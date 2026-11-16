package org.oauthinaction.ch15ex01;
import org.oauthinaction.ch15ex01.completed.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProofTest {
    @TempDir Path directory;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    WebServer authorization, resource, client;
    URI callback;
    @BeforeEach void start() throws Exception {
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        callback = URI.create("http://localhost:" + port + "/callback");
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        authorization = WebServer.start(0, "authorization", new CompletedAuthorizationServer(callback, tokens));
        resource = WebServer.start(0, "resource", new CompletedResource(authorization.uri()));
        client = WebServer.start(port, "client", new CompletedClient(authorization.uri(), resource.uri(), callback));
    }
    @AfterEach void stop() { if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close(); }
    @Test void clientSignsProofAndResourceValidatesItThroughIntrospection() throws Exception {
        authorizeClient();
        assertEquals(200, get(client.uri().resolve("/fetch_resource")).statusCode());
        var status = json.readTree(get(client.uri().resolve("/status")).body()); assertTrue(status.path("key").has("d"));
        var response = post(authorization.uri().resolve("/introspect"), Map.of("token", status.path("access_token").asText()), resourceAuth());
        assertTrue(json.readTree(response.body()).path("active").asBoolean());
        assertFalse(json.readTree(response.body()).path("access_token_key").has("d"));
        assertFalse(java.nio.file.Files.readString(directory.resolve("tokens.json")).contains("\"d\""));
    }
    @Test void wrongKeyTokenAndBoundRequestComponentsFail() throws Exception {
        var issued = issue();
        String token = issued.path("access_token").asText();
        var key = json.convertValue(issued.path("access_token_key"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        var claims = new HashMap<String, Object>(Map.of("at", token, "ts", java.time.Instant.now().getEpochSecond(), "m", "POST", "u", resource.uri().getAuthority(), "p", "/resource"));
        assertEquals(200, access("PoP " + Proof.sign(key, claims)).statusCode());
        assertEquals(401, access("Bearer " + token).statusCode());
        assertEquals(401, access("PoP " + Proof.sign(Proof.generateKey(), claims)).statusCode());
        for (String field : List.of("at", "m", "u", "p")) {
            var bad = new HashMap<>(claims); bad.put(field, "wrong");
            assertEquals(401, access("PoP " + Proof.sign(key, bad)).statusCode());
        }
        assertEquals(401, access("PoP " + Proof.sign(key, claims) + "tamper").statusCode());
        assertEquals(401, access(null).statusCode());
    }
    @Test void eachAccessTokenGetsItsOwnKeyAndInactiveTokensFail() throws Exception {
        var first = issue(); var second = issue();
        assertNotEquals(first.path("access_token_key").path("n"), second.path("access_token_key").path("n"));
        assertEquals(401, post(authorization.uri().resolve("/introspect"), Map.of("token", first.path("access_token").asText()), null).statusCode());
        assertFalse(json.readTree(post(authorization.uri().resolve("/introspect"), Map.of("token", "unknown"), resourceAuth()).body()).path("active").asBoolean());
        var key = json.convertValue(first.path("access_token_key"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        new TokenStore(directory.resolve("tokens.json")).clear();
        assertEquals(401, access("PoP " + Proof.sign(key, Map.of("at", first.path("access_token").asText()))).statusCode());
    }
    @Test void historicalOptionalBindingsRemainOptionalAndNoReplayCacheIsInvented() throws Exception {
        var issued = issue(); var key = json.convertValue(issued.path("access_token_key"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        String proof = Proof.sign(key, Map.of("at", issued.path("access_token").asText(), "ts", 1));
        assertEquals(200, access("PoP " + proof).statusCode()); assertEquals(200, access("PoP " + proof).statusCode());
    }
    @Test void allStarterGapsAndStateErrorsAreExplicit() throws Exception {
        assertEquals(400, get(client.uri().resolve("/callback?state=wrong&code=unused")).statusCode());
        try (var starterServer = WebServer.start(0, "authorization", new StarterAuthorizationServer(callback, new TokenStore(directory.resolve("starter.json"))));
             var starterResource = WebServer.start(0, "resource", new StarterResource(authorization.uri()));
             var starterClient = WebServer.start(0, "client", new StarterClient(authorization.uri(), resource.uri(), callback))) {
            String code = code(starterServer.uri());
            assertEquals(501, post(starterServer.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), clientAuth()).statusCode());
            assertEquals(501, get(starterClient.uri().resolve("/fetch_resource")).statusCode());
            assertEquals(401, post(starterResource.uri().resolve("/resource"), Map.of(), "PoP anything").statusCode());
        }
        assertEquals(200, get(client.uri()).statusCode()); assertEquals(404, get(client.uri().resolve("/approve.html")).statusCode());
    }
    JsonNode issue() throws Exception {
        var response = post(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code(authorization.uri())), clientAuth());
        assertEquals(200, response.statusCode()); var result = json.readTree(response.body()); assertEquals("PoP", result.path("token_type").asText()); return result;
    }
    String code(URI server) throws Exception {
        var consent = get(server.resolve("/authorize?" + Forms.encode(Map.of("client_id", "oauth-client-1", "redirect_uri", callback.toString(), "response_type", "code", "scope", "foo"))));
        var approved = post(server.resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes"), null);
        return Forms.query(location(approved)).get("code");
    }
    void authorizeClient() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"))));
        var approved = post(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes"), null);
        assertEquals(302, get(location(approved)).statusCode());
    }
    String clientAuth() { return Forms.basic("oauth-client-1", "oauth-client-secret-1"); }
    String resourceAuth() { return Forms.basic("protected-resource-1", "protected-resource-secret-1"); }
    HttpResponse<String> access(String auth) throws Exception { return post(resource.uri().resolve("/resource"), Map.of(), auth); }
    URI location(HttpResponse<?> r) { return r.uri().resolve(r.headers().firstValue("Location").orElseThrow()); }
    HttpResponse<String> get(URI uri) throws Exception { return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString()); }
    HttpResponse<String> post(URI uri, Map<String, String> body, String auth) throws Exception {
        var b = HttpRequest.newBuilder(uri).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(Forms.encode(body)));
        if (auth != null) b.header("Authorization", auth); return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
