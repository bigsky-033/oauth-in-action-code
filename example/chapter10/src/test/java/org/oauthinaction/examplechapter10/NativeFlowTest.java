package org.oauthinaction.examplechapter10;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeFlowTest {
    @TempDir Path directory;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    WebServer authorization, resource, client;
    URI callback;
    @BeforeEach void start() throws Exception {
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        callback = URI.create("http://localhost:" + port + "/callback");
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        authorization = WebServer.start(0, "authorization", new AuthorizationServer(callback, tokens));
        resource = WebServer.start(0, "resource", new ProtectedResource(authorization.uri()));
        client = WebServer.start(port, "client", new Client(authorization.uri(), resource.uri(), callback));
    }
    @AfterEach void stop() { if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close(); }
    @Test void webCodeFlowUsesRealHttpIntrospection() throws Exception {
        var start = get(client.uri().resolve("/authorize"), null);
        var consent = get(location(start), null);
        String id = Forms.query(location(consent)).get("request_id");
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", id, "approve", "yes", "user", "bob", "scope_foo", "yes"), null);
        assertEquals(302, get(location(approved), null).statusCode());
        var status = json.readTree(get(client.uri().resolve("/status"), null).body());
        assertEquals("foo", status.path("scope").asText());
        assertEquals(200, get(client.uri().resolve("/fetch_resource"), null).statusCode());
        assertEquals(400, get(location(approved), null).statusCode());
    }
    @Test void fullClientProxyRoutesAndIdentityWorkTogether() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"), null)), null);
        var body = new HashMap<String, String>(Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "user", "bob"));
        for (String scope : List.of("openid", "email", "profile", "read", "write", "delete", "fruit")) body.put("scope_" + scope, "yes");
        assertEquals(302, get(location(form(authorization.uri().resolve("/approve"), body, null)), null).statusCode());
        assertEquals(201, get(client.uri().resolve("/add_word?word=hello"), null).statusCode());
        assertEquals("hello", json.readTree(get(client.uri().resolve("/get_words"), null).body()).path("words").asText());
        assertEquals(204, get(client.uri().resolve("/delete_word"), null).statusCode());
        assertEquals(3, json.readTree(get(client.uri().resolve("/produce"), null).body()).path("fruit").size());
        assertEquals("Bob", json.readTree(get(client.uri().resolve("/favorites"), null).body()).path("user").asText());
        assertFalse(json.readTree(get(client.uri().resolve("/userinfo"), null).body()).path("userInfo").path("email_verified").asBoolean());
        assertEquals(302, form(client.uri().resolve("/revoke"), Map.of(), null).statusCode());
        assertEquals(400, get(client.uri().resolve("/fetch_resource"), null).statusCode());
        try (var implicit = WebServer.start(0, "implicit", new ImplicitClient())) { assertEquals(200, get(implicit.uri().resolve("/callback"), null).statusCode()); }
    }
    @Test void plainPkceIsBoundToTheCodeAndMissingOrWrongVerifierFails() throws Exception {
        for (String verifier : List.of("", "wrong", "expected")) {
            var consent = get(URI.create(authorize("native-client-1", "foo", "code", "state") + "&code_challenge=expected"), null);
            var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes"), null);
            String code = Forms.query(location(approved)).get("code");
            var body = new HashMap<String, String>(Map.of("grant_type", "authorization_code", "code", code));
            if (!verifier.isEmpty()) body.put("code_verifier", verifier);
            assertEquals(verifier.equals("expected") ? 200 : 400, token(body).statusCode());
            assertEquals(400, token(body).statusCode());
        }
        var clientRequest = get(client.uri().resolve("/authorize"), null);
        assertFalse(Forms.query(location(clientRequest)).get("code_challenge").isBlank());
    }
    @Test void stateAndInvalidRedirectsAreRejected() throws Exception {
        get(client.uri().resolve("/authorize"), null);
        assertEquals(400, get(client.uri().resolve("/callback?state=wrong&code=unused"), null).statusCode());
        var result = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", "native-client-1",
                "redirect_uri", "https://invalid.example/callback", "response_type", "code"))), null);
        assertEquals(400, result.statusCode()); assertTrue(result.headers().firstValue("Location").isEmpty());
    }
    @Test void introspectionNeedsResourceCredentialsAndRevocationMakesTokenInactive() throws Exception {
        var issued = token(Map.of("grant_type", "client_credentials", "scope", "foo"));
        String access = json.readTree(issued.body()).path("access_token").asText();
        assertEquals(401, form(authorization.uri().resolve("/introspect"), Map.of("token", access), null).statusCode());
        var inspected = inspect(access);
        assertTrue(json.readTree(inspected.body()).path("active").asBoolean());
        assertFalse(json.readTree(inspect("unknown").body()).path("active").asBoolean());
        assertEquals(204, form(authorization.uri().resolve("/revoke"), Map.of("token", access), nativeAuth()).statusCode());
        assertFalse(json.readTree(inspect(access).body()).path("active").asBoolean());
        assertEquals(401, form(resource.uri().resolve("/resource"), Map.of(), "Bearer " + access).statusCode());
        assertEquals(204, form(authorization.uri().resolve("/revoke"), Map.of("token", "unknown"), nativeAuth()).statusCode());
    }
    @Test void registrationLifecycleValidatesMetadataAndManagementBearer() throws Exception {
        for (var bad : List.of(Map.<String, Object>of("scope", 42), Map.<String, Object>of("grant_types", List.of(42)), Map.<String, Object>of("response_types", List.of(false)))) {
            var metadata = new HashMap<String, Object>(bad); metadata.put("redirect_uris", List.of(callback.toString()));
            assertEquals(400, jsonRequest("POST", authorization.uri().resolve("/register"), metadata, null).statusCode());
        }
        assertEquals(400, get(authorization.uri().resolve("/authorize?client_id=native-client-1"), null).statusCode());
        assertEquals(400, jsonRequest("POST", authorization.uri().resolve("/register"), Map.of(), null).statusCode());
        assertEquals(400, jsonRequest("POST", authorization.uri().resolve("/register"), Map.of("redirect_uris", List.of(callback.toString()),
                "token_endpoint_auth_method", "unknown"), null).statusCode());
        var created = jsonRequest("POST", authorization.uri().resolve("/register"), Map.of("redirect_uris", List.of(callback.toString()),
                "client_name", "study", "scope", "foo", "token_endpoint_auth_method", "client_secret_post"), null);
        assertEquals(201, created.statusCode());
        var registration = json.readTree(created.body());
        String id = registration.path("client_id").asText();
        String management = "Bearer " + registration.path("registration_access_token").asText();
        URI uri = URI.create(registration.path("registration_client_uri").asText());
        assertEquals(authorization.uri().getPort(), uri.getPort());
        assertEquals(401, get(uri, null).statusCode()); assertEquals(403, get(uri, "Bearer wrong").statusCode());
        assertEquals(200, get(uri, management).statusCode());
        var updated = jsonRequest("PUT", uri, Map.of("client_id", id, "redirect_uris", List.of(callback.toString()),
                "client_name", "renamed", "scope", "foo", "token_endpoint_auth_method", "client_secret_post"), management);
        assertEquals(200, updated.statusCode()); assertEquals("renamed", json.readTree(updated.body()).path("client_name").asText());
        assertEquals(registration.path("client_secret"), json.readTree(updated.body()).path("client_secret"));
        assertEquals(204, jsonRequest("DELETE", uri, null, management).statusCode()); assertEquals(404, get(uri, management).statusCode());
    }
    @Test void publicRegistrationUsesNoSecretAndCanExchangeItsOwnCode() throws Exception {
        var created = jsonRequest("POST", authorization.uri().resolve("/register"), Map.of("redirect_uris", List.of(callback.toString()),
                "scope", "foo", "token_endpoint_auth_method", "none"), null);
        var registration = json.readTree(created.body()); assertFalse(registration.has("client_secret"));
        String id = registration.path("client_id").asText();
        String code = code(id, "foo", "alice", "nonce");
        var issued = form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code,
                "client_id", id, "redirect_uri", callback.toString()), null);
        assertEquals(200, issued.statusCode());
        assertEquals(400, form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code, "client_id", id), null).statusCode());
    }
    @Test void idTokenSignatureNonceAndScopedUserInfoAreCorrect() throws Exception {
        String code = code("oauth-client-1", "openid email", "bob", "book-nonce");
        var issued = form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), Forms.basic("oauth-client-1", "oauth-client-secret-1"));
        var body = json.readTree(issued.body());
        var claims = Jwt.verify(body.path("id_token").asText());
        assertEquals("book-nonce", claims.path("nonce").asText()); assertEquals("oauth-client-1", claims.path("aud").asText());
        assertEquals("1ZT5-OE63-57383B", claims.path("sub").asText());
        var userInfo = get(authorization.uri().resolve("/userinfo"), "Bearer " + body.path("access_token").asText());
        assertEquals(200, userInfo.statusCode());
        var user = json.readTree(userInfo.body()); assertEquals("bob.loblob@example.net", user.path("email").asText());
        assertTrue(user.has("email_verified")); assertFalse(user.path("email_verified").asBoolean()); assertFalse(user.has("name"));
        assertThrows(IllegalArgumentException.class, () -> Jwt.verify(body.path("id_token").asText() + "tamper"));
    }
    @Test void implicitPasswordAndRefreshPathsWorkAndScopeCannotBeEnlarged() throws Exception {
        var consent = get(authorize("native-client-1", "foo", "token", "state"), null);
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"),
                "approve", "yes", "scope_foo", "yes", "user", "alice"), null);
        var fragment = Forms.query(URI.create("http://localhost/?" + location(approved).getRawFragment()));
        assertEquals("state", fragment.get("state")); assertFalse(fragment.containsKey("refresh_token"));
        var password = token(Map.of("grant_type", "password", "username", "clewis", "password", "user password!", "scope", "foo"));
        assertEquals(200, password.statusCode());
        assertEquals(401, token(Map.of("grant_type", "password", "username", "clewis", "password", "wrong")).statusCode());
        String refresh = json.readTree(form(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code",
                "code", code("native-client-1", "foo", "alice", "")), nativeAuth()).body()).path("refresh_token").asText();
        assertEquals(200, token(Map.of("grant_type", "refresh_token", "refresh_token", refresh)).statusCode());
        assertEquals(400, token(Map.of("grant_type", "refresh_token", "refresh_token", refresh, "scope", "foo bar")).statusCode());
        assertEquals(401, form(resource.uri().resolve("/resource"), Map.of(), "Bearer " + refresh).statusCode());
    }
    @Test void resourceScopesAndUserSelectionUseIntrospection() throws Exception {
        var store = new TokenStore(directory.resolve("tokens.json"));
        store.save("read-fruit-bob", new TokenStore.Token("native-client-1", List.of("read", "fruit"), "bob", false));
        assertEquals(200, get(resource.uri().resolve("/words"), "Bearer read-fruit-bob").statusCode());
        assertEquals(403, form(resource.uri().resolve("/words"), Map.of("word", "no"), "Bearer read-fruit-bob").statusCode());
        var produce = json.readTree(get(resource.uri().resolve("/produce"), "Bearer read-fruit-bob").body());
        assertEquals(3, produce.path("fruit").size()); assertEquals(0, produce.path("meats").size());
        assertEquals("Bob", json.readTree(get(resource.uri().resolve("/favorites"), "Bearer read-fruit-bob").body()).path("user").asText());
    }
    @Test void metadataAndUnknownBearerFailuresAreControlled() throws Exception {
        assertEquals(401, get(authorization.uri().resolve("/userinfo"), "Bearer unknown").statusCode());
        String access = json.readTree(token(Map.of("grant_type", "client_credentials", "scope", "foo")).body()).path("access_token").asText();
        assertEquals(403, get(authorization.uri().resolve("/userinfo"), "Bearer " + access).statusCode());
        assertEquals(401, form(authorization.uri().resolve("/token"), Map.of("grant_type", "client_credentials"), "Basic %%%").statusCode());
        assertEquals(400, token(Map.of("grant_type", "client_credentials", "scope", "unknown")).statusCode());
        assertEquals(200, get(client.uri(), null).statusCode()); assertEquals(200, get(client.uri().resolve("/style.css"), null).statusCode());
        assertEquals(404, get(client.uri().resolve("/approve.html"), null).statusCode());
    }
    URI authorize(String id, String scope, String type, String state) { return authorization.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", id, "redirect_uri", callback.toString(), "scope", scope, "response_type", type, "state", state))); }
    String code(String clientId, String scope, String user, String nonce) throws Exception {
        URI uri = URI.create(authorize(clientId, scope, "code", "state") + "&nonce=" + nonce);
        var consent = get(uri, null); String id = Forms.query(location(consent)).get("request_id");
        var body = new HashMap<String, String>(Map.of("request_id", id, "approve", "yes", "user", user));
        for (String s : TokenStore.scopes(scope)) body.put("scope_" + s, "yes");
        return Forms.query(location(form(authorization.uri().resolve("/approve"), body, null))).get("code");
    }
    String nativeAuth() { return Forms.basic("native-client-1", "oauth-native-secret-1"); }
    HttpResponse<String> token(Map<String, String> form) throws Exception { return form(authorization.uri().resolve("/token"), form, nativeAuth()); }
    HttpResponse<String> inspect(String token) throws Exception { return form(authorization.uri().resolve("/introspect"), Map.of("token", token), Forms.basic("protected-resource-1", "protected-resource-secret-1")); }
    URI location(HttpResponse<?> response) { return response.uri().resolve(response.headers().firstValue("Location").orElseThrow()); }
    HttpResponse<String> get(URI uri, String auth) throws Exception { return send("GET", uri, null, null, auth); }
    HttpResponse<String> form(URI uri, Map<String, String> body, String auth) throws Exception { return send("POST", uri, Forms.encode(body), "application/x-www-form-urlencoded", auth); }
    HttpResponse<String> jsonRequest(String method, URI uri, Map<String, Object> body, String auth) throws Exception { return send(method, uri, body == null ? null : json.writeValueAsString(body), "application/json", auth); }
    HttpResponse<String> send(String method, URI uri, String body, String type, String auth) throws Exception {
        var request = HttpRequest.newBuilder(uri).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (type != null) request.header("Content-Type", type); if (auth != null) request.header("Authorization", auth);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
