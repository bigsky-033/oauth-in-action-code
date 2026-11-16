package org.oauthinaction.ch12ex01;
import org.oauthinaction.ch12ex01.completed.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationTest {
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
        resource = WebServer.start(0, "resource", new ProtectedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(port, "client", new CompletedClient(authorization.uri(), resource.uri(), callback));
    }
    @AfterEach void stop() { if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close(); }
    @Test void registrationDefaultsAndClientSessionFlowWork() throws Exception {
        assertEquals(0, json.readTree(get(authorization.uri().resolve("/status"), null).body()).path("clients").size());
        authorizeClient();
        var status = json.readTree(get(client.uri().resolve("/status"), null).body());
        assertFalse(status.path("client_id").asText().isBlank()); assertEquals("foo", status.path("scope").asText());
        assertEquals(200, get(client.uri().resolve("/fetch_resource"), null).statusCode());
        assertEquals(302, get(client.uri().resolve("/refresh"), null).statusCode());
        String id = status.path("client_id").asText();
        get(client.uri().resolve("/authorize"), null);
        assertEquals(id, json.readTree(get(client.uri().resolve("/status"), null).body()).path("client_id").asText());
    }
    @Test void registrationDefaultsAndSecretsAreExplicit() throws Exception {
        var first = register(Map.of()); var second = register(Map.of());
        assertEquals("client_secret_basic", first.path("token_endpoint_auth_method").asText());
        assertFalse(first.path("client_secret").asText().isBlank()); assertNotEquals(first.path("client_id"), second.path("client_id"));
        assertEquals("authorization_code", first.path("grant_types").get(0).asText());
        assertEquals("code", first.path("response_types").get(0).asText());
        var publicClient = register(Map.of("token_endpoint_auth_method", "none")); assertFalse(publicClient.has("client_secret"));
        String code = code(publicClient.path("client_id").asText());
        var issued = form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code, "client_id", publicClient.path("client_id").asText()), null);
        assertEquals(200, issued.statusCode());
    }
    @Test void metadataRedirectAndGrantValidationRejectInvalidInput() throws Exception {
        assertEquals(400, jsonRequest("POST", authorization.uri().resolve("/register"), Map.of(), null).statusCode());
        for (var extra : List.<Map<String, Object>>of(Map.of("token_endpoint_auth_method", "unknown"), Map.of("grant_types", List.of("password")),
                Map.of("response_types", List.of("token")), Map.of("scope", 42), Map.of("grant_types", List.of(42)), Map.of("response_types", List.of(false)), Map.of("redirect_uris", List.of("http://localhost/callback#fragment")))) {
            var metadata = new HashMap<String, Object>(Map.of("redirect_uris", List.of(callback.toString()))); metadata.putAll(extra);
            assertEquals(400, jsonRequest("POST", authorization.uri().resolve("/register"), metadata, null).statusCode());
        }
    }
    @Test void authenticationClientBindingAndCodeReplayAreChecked() throws Exception {
        var first = register(Map.of());
        var second = register(Map.of());
        String code = code(first.path("client_id").asText());
        assertEquals(401, form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), null).statusCode());
        String otherAuth = Forms.basic(second.path("client_id").asText(), second.path("client_secret").asText());
        assertEquals(400, form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), otherAuth).statusCode());
        String auth = Forms.basic(first.path("client_id").asText(), first.path("client_secret").asText());
        assertEquals(400, form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), auth).statusCode());
    }
    @Test void starterRegistrationGapsReturn501AndAssetsRemainIsolated() throws Exception {
        try (var starterServer = WebServer.start(0, "authorization", new StarterAuthorizationServer(callback, new TokenStore(directory.resolve("starter.json"))));
             var starterClient = WebServer.start(0, "client", new StarterClient(authorization.uri(), resource.uri(), callback))) {
            assertEquals(501, jsonRequest("POST", starterServer.uri().resolve("/register"), Map.of("redirect_uris", List.of(callback.toString())), null).statusCode());
            assertEquals(501, get(starterClient.uri().resolve("/authorize"), null).statusCode());
        }
        assertEquals(200, get(client.uri(), null).statusCode()); assertEquals(200, get(client.uri().resolve("/style.css"), null).statusCode());
        assertEquals(404, get(client.uri().resolve("/approve.html"), null).statusCode());
    }
    JsonNode register(Map<String, Object> extra) throws Exception {
        var metadata = new HashMap<String, Object>(Map.of("redirect_uris", List.of(callback.toString()), "scope", "foo bar")); metadata.putAll(extra);
        var response = jsonRequest("POST", authorization.uri().resolve("/register"), metadata, null); assertEquals(201, response.statusCode()); return json.readTree(response.body());
    }
    String code(String id) throws Exception {
        var consent = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", id, "redirect_uri", callback.toString(), "response_type", "code", "scope", "foo"))), null);
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes"), null);
        return Forms.query(location(approved)).get("code");
    }
    void authorizeClient() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"), null)), null);
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes"), null);
        assertEquals(302, get(location(approved), null).statusCode());
    }
    URI location(HttpResponse<?> r) { return r.uri().resolve(r.headers().firstValue("Location").orElseThrow()); }
    HttpResponse<String> get(URI uri, String auth) throws Exception { return send("GET", uri, null, null, auth); }
    HttpResponse<String> form(URI uri, Map<String, String> body, String auth) throws Exception { return send("POST", uri, Forms.encode(body), "application/x-www-form-urlencoded", auth); }
    HttpResponse<String> jsonRequest(String method, URI uri, Map<String, Object> body, String auth) throws Exception { return send(method, uri, body == null ? null : json.writeValueAsString(body), "application/json", auth); }
    HttpResponse<String> send(String method, URI uri, String body, String type, String auth) throws Exception {
        var b = HttpRequest.newBuilder(uri).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (type != null) b.header("Content-Type", type); if (auth != null) b.header("Authorization", auth);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
