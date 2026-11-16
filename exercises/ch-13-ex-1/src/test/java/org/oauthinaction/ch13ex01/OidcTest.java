package org.oauthinaction.ch13ex01;
import org.oauthinaction.ch13ex01.completed.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OidcTest {
    @TempDir Path directory;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    WebServer authorization, resource, client;
    URI callback;
    TokenStore tokens;
    @BeforeEach void start() throws Exception {
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        callback = URI.create("http://localhost:" + port + "/callback");
        tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        authorization = WebServer.start(0, "authorization", new CompletedAuthorizationServer(callback, tokens));
        resource = WebServer.start(0, "resource", new CompletedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(port, "client", new CompletedClient(authorization.uri(), resource.uri(), callback));
    }
    @AfterEach void stop() { if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close(); }
    @Test void idTokenAndScopedUserInfoDescribeTheSameSelectedUser() throws Exception {
        authorize("bob", List.of("openid", "email"));
        var status = json.readTree(get(client.uri().resolve("/status"), null).body());
        assertEquals("1ZT5-OE63-57383B", status.path("id_token").path("sub").asText());
        var response = get(client.uri().resolve("/userinfo"), null); assertEquals(200, response.statusCode());
        var info = json.readTree(response.body()).path("userInfo");
        assertEquals(status.path("id_token").path("sub"), info.path("sub")); assertEquals("bob.loblob@example.net", info.path("email").asText());
        assertTrue(info.has("email_verified")); assertFalse(info.path("email_verified").asBoolean()); assertFalse(info.has("name"));
        assertEquals(200, get(client.uri().resolve("/fetch_resource"), null).statusCode());
    }
    @Test void noIdTokenIsIssuedWithoutOpenidAndUserInfoRejectsIt() throws Exception {
        authorize("alice", List.of("email"));
        var status = json.readTree(get(client.uri().resolve("/status"), null).body()); assertTrue(status.path("id_token").isNull());
        assertEquals(403, get(resource.uri().resolve("/userinfo"), "Bearer " + status.path("access_token").asText()).statusCode());
        assertEquals(401, get(resource.uri().resolve("/userinfo"), "Bearer unknown").statusCode());
        tokens.save("no-user", new TokenStore.Token("oauth-client-1", List.of("openid"), null, false));
        assertEquals(404, get(resource.uri().resolve("/userinfo"), "Bearer no-user").statusCode());
    }
    @Test void clientRejectsInvalidSignatureClaimsAndNonce() {
        var verifier = new Verifier(authorization.uri(), resource.uri(), callback);
        long now = java.time.Instant.now().getEpochSecond();
        var claims = new HashMap<String, Object>(Map.of("iss", "http://localhost:9001/", "aud", "oauth-client-1", "sub", "subject", "iat", now, "exp", now + 300, "nonce", "expected"));
        var session = new MockHttpSession(); session.setAttribute("nonce", "expected");
        assertEquals("subject", verifier.check(Jwt.sign(claims), session).path("sub").asText());
        for (String field : List.of("iss", "aud", "iat", "exp", "nonce")) {
            var bad = new HashMap<>(claims); bad.put(field, field.equals("iat") ? now + 100 : field.equals("exp") ? now - 1 : "wrong");
            var attempt = new MockHttpSession(); attempt.setAttribute("nonce", "expected");
            assertThrows(IllegalArgumentException.class, () -> verifier.check(Jwt.sign(bad), attempt));
        }
        assertThrows(IllegalArgumentException.class, () -> verifier.check(Jwt.sign(claims) + "tamper", new MockHttpSession()));
        assertThrows(IllegalArgumentException.class, () -> verifier.check(null, new MockHttpSession()));
    }
    @Test void userInfoSubjectMismatchIsRejectedByClient() throws Exception {
        authorize("alice", List.of("openid", "email"));
        var status = json.readTree(get(client.uri().resolve("/status"), null).body());
        String access = status.path("access_token").asText();
        tokens.save(access, new TokenStore.Token("oauth-client-1", List.of("openid", "email"), "bob", false));
        assertEquals(400, get(client.uri().resolve("/userinfo"), null).statusCode());
    }
    @Test void starterGapsAreLimitedToIdentityFeatures() throws Exception {
        try (var starterServer = WebServer.start(0, "authorization", new StarterAuthorizationServer(callback, new TokenStore(directory.resolve("starter.json"))));
             var starterResource = WebServer.start(0, "resource", new StarterResource(tokens));
             var starterClient = WebServer.start(0, "client", new StarterClient(authorization.uri(), resource.uri(), callback))) {
            var consent = get(starterServer.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", "oauth-client-1", "redirect_uri", callback.toString(), "response_type", "code", "scope", "openid"))), null);
            var approved = form(starterServer.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_openid", "yes"), null);
            var token = form(starterServer.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", Forms.query(location(approved)).get("code")), Forms.basic("oauth-client-1", "oauth-client-secret-1"));
            assertEquals(200, token.statusCode()); assertFalse(json.readTree(token.body()).has("id_token"));
            assertEquals(501, get(starterClient.uri().resolve("/userinfo"), null).statusCode());
            assertEquals(501, get(starterResource.uri().resolve("/userinfo"), null).statusCode());
        }
    }
    @Test void unknownConsentUserIsRejectedBeforeIssuingAnIdToken() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"), null)), null);
        String id = Forms.query(location(consent)).get("request_id");
        var response = form(authorization.uri().resolve("/approve"), Map.of("request_id", id, "approve", "yes", "user", "unknown", "scope_openid", "yes"), null);
        assertEquals(400, response.statusCode());
        assertTrue(json.readTree(response.body()).has("error"));
    }
    @Test void stateAndStaticAssetIsolationStillWork() throws Exception {
        assertEquals(400, get(client.uri().resolve("/callback?state=wrong&code=unused"), null).statusCode());
        assertEquals(200, get(client.uri(), null).statusCode()); assertEquals(200, get(client.uri().resolve("/style.css"), null).statusCode());
        assertEquals(404, get(client.uri().resolve("/approve.html"), null).statusCode());
    }
    void authorize(String user, List<String> scopes) throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"), null)), null);
        var form = new HashMap<String, String>(Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "user", user));
        for (String scope : scopes) form.put("scope_" + scope, "yes");
        assertEquals(302, get(location(form(authorization.uri().resolve("/approve"), form, null)), null).statusCode());
    }
    static class Verifier extends CompletedClient {
        Verifier(URI auth, URI resource, URI callback) { super(auth, resource, callback); }
        JsonNode check(String token, HttpSession session) { return validateIdToken(token, session); }
    }
    URI location(HttpResponse<?> r) { return r.uri().resolve(r.headers().firstValue("Location").orElseThrow()); }
    HttpResponse<String> get(URI uri, String auth) throws Exception { return send("GET", uri, null, auth); }
    HttpResponse<String> form(URI uri, Map<String, String> form, String auth) throws Exception { return send("POST", uri, Forms.encode(form), auth); }
    HttpResponse<String> send(String method, URI uri, String body, String auth) throws Exception {
        var b = HttpRequest.newBuilder(uri).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) b.header("Content-Type", "application/x-www-form-urlencoded"); if (auth != null) b.header("Authorization", auth);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
