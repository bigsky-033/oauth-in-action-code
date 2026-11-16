package org.oauthinaction.ch06ex02;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.oauthinaction.ch06ex02.completed.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClientCredentialsTest {
    @TempDir Path directory;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    WebServer authorization, resource, client;
    @BeforeEach void start() {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        authorization = WebServer.start(0, "authorization", new CompletedAuthorizationServer(URI.create("http://localhost/callback"), tokens));
        resource = WebServer.start(0, "resource", new ProtectedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(0, "client", new CompletedClient(authorization.uri(), resource.uri()));
    }
    @AfterEach void stop() {
        if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close();
    }
    @Test void clientGetsTokenWithoutConsentAndCanUseIt() throws Exception {
        var result = get(client.uri().resolve("/authorize"));
        assertEquals(302, result.statusCode());
        assertEquals("/", result.headers().firstValue("Location").orElseThrow());
        var status = json.readTree(get(client.uri().resolve("/status")).body());
        assertEquals("foo bar", status.path("scope").asText());
        assertTrue(status.path("refresh_token").isNull());
        assertEquals(200, get(client.uri().resolve("/fetch_resource")).statusCode());
    }
    @Test void tokenHasRequestedScopeAndNoRefreshToken() throws Exception {
        var result = post(authorization.uri().resolve("/token"), Map.of("grant_type", "client_credentials", "scope", "foo"), credentials());
        assertEquals(200, result.statusCode());
        var token = json.readTree(result.body());
        assertEquals("foo", token.path("scope").asText());
        assertFalse(token.has("refresh_token"));
        assertEquals("no-store", result.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals(200, post(resource.uri().resolve("/resource"), Map.of(), "Bearer " + token.path("access_token").asText()).statusCode());
    }
    @Test void invalidClientScopeAndGrantAreRejected() throws Exception {
        var form = Map.of("grant_type", "client_credentials", "scope", "foo");
        assertEquals(401, post(authorization.uri().resolve("/token"), form, null).statusCode());
        assertEquals(401, post(authorization.uri().resolve("/token"), form, Forms.basic("oauth-client-1", "wrong")).statusCode());
        assertEquals(400, post(authorization.uri().resolve("/token"), Map.of("grant_type", "client_credentials", "scope", "unknown"), credentials()).statusCode());
        assertEquals(400, post(authorization.uri().resolve("/token"), Map.of("grant_type", "unknown"), credentials()).statusCode());
    }
    @Test void bodyAuthenticationIsSupportedButMixedMethodsAreRejected() throws Exception {
        var form = Map.of("grant_type", "client_credentials", "client_id", "oauth-client-1", "client_secret", "oauth-client-secret-1");
        assertEquals(200, post(authorization.uri().resolve("/token"), form, null).statusCode());
        assertEquals(401, post(authorization.uri().resolve("/token"), form, credentials()).statusCode());
    }
    @Test void supportingCodeAndRefreshPathsStillWork() throws Exception {
        var consent = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", "oauth-client-1",
                "redirect_uri", "http://localhost/callback", "response_type", "code", "state", "test"))));
        String id = Forms.query(URI.create(consent.headers().firstValue("Location").orElseThrow())).get("request_id");
        var approved = post(authorization.uri().resolve("/approve"), Map.of("request_id", id, "approve", "yes"), null);
        String code = Forms.query(URI.create(approved.headers().firstValue("Location").orElseThrow())).get("code");
        var issued = post(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), credentials());
        assertEquals(200, issued.statusCode());
        String refresh = json.readTree(issued.body()).path("refresh_token").asText();
        assertFalse(refresh.isBlank());
        assertEquals(200, post(authorization.uri().resolve("/token"), Map.of("grant_type", "refresh_token", "refresh_token", refresh), credentials()).statusCode());
        assertEquals(400, post(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code", "code", code), credentials()).statusCode());
    }
    @Test void startersLeaveOnlyClientCredentialsUnfinished() throws Exception {
        try (var starterClient = WebServer.start(0, "client", new StarterClient(authorization.uri(), resource.uri()));
             var starterServer = WebServer.start(0, "authorization", new StarterAuthorizationServer(URI.create("http://localhost/callback"),
                     new TokenStore(directory.resolve("starter.json"))))) {
            assertEquals(501, get(starterClient.uri().resolve("/authorize")).statusCode());
            assertEquals(501, post(starterServer.uri().resolve("/token"), Map.of("grant_type", "client_credentials"), credentials()).statusCode());
        }
    }
    @Test void resourcesAndStaticPagesAreIsolated() throws Exception {
        assertEquals(401, post(resource.uri().resolve("/resource"), Map.of(), "Bearer unknown").statusCode());
        for (var server : new WebServer[]{authorization, resource, client}) {
            assertEquals(200, get(server.uri()).statusCode()); assertEquals(200, get(server.uri().resolve("/style.css")).statusCode());
        }
        assertEquals(404, get(client.uri().resolve("/approve.html")).statusCode());
    }
    HttpResponse<String> get(URI uri) throws Exception { return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString()); }
    HttpResponse<String> post(URI uri, Map<String, String> form, String auth) throws Exception {
        var request = HttpRequest.newBuilder(uri).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(form)));
        if (auth != null) request.header("Authorization", auth);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    String credentials() { return Forms.basic("oauth-client-1", "oauth-client-secret-1"); }
}
