package org.oauthinaction.ch06ex04;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationCodeFlowTest {
    @TempDir Path directory;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient browser = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private WebServer client;
    private WebServer authorization;
    private WebServer resource;
    private URI callback;

    @BeforeEach
    void start() throws Exception {
        // Reserve a callback port before constructing the registered client. The other
        // two servers ask the OS for ports directly; tests never occupy 9000–9002.
        int clientPort;
        try (var socket = new ServerSocket(0)) { clientPort = socket.getLocalPort(); }
        callback = URI.create("http://localhost:" + clientPort + "/callback");
        var tokens = new TokenStore(directory.resolve("tokens.json"));
        tokens.clear();
        authorization = WebServer.start(0, "authorization", new AuthorizationServer(callback, tokens));
        resource = WebServer.start(0, "resource", new ProtectedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(clientPort, "client",
                new CodeClient(authorization.uri(), resource.uri(), callback));
    }

    @AfterEach
    void stop() {
        if (client != null) client.close();
        if (resource != null) resource.close();
        if (authorization != null) authorization.close();
    }

    @Test
    void browserCanAuthorizeExchangeCodeAndFetchResource() throws Exception {
        var authorize = get(client.uri().resolve("/authorize"));
        assertEquals(302, authorize.statusCode());
        var consent = get(location(authorize));
        assertEquals(302, consent.statusCode());
        var requestId = requestId(location(consent));
        var details = get(authorization.uri().resolve("/request?request_id=" + requestId));
        assertEquals("oauth-client-1", json.readTree(details.body()).path("client_id").asText());
        var approved = post(authorization.uri().resolve("/approve"),
                Map.of("request_id", requestId, "approve", "yes"), null);
        var callbackResponse = get(location(approved));
        assertEquals(302, callbackResponse.statusCode());
        var status = json.readTree(get(client.uri().resolve("/status")).body());
        assertFalse(status.path("access_token").asText().isBlank());
        var data = get(client.uri().resolve("/fetch_resource"));
        assertEquals(200, data.statusCode());
        assertEquals("Protected Resource", json.readTree(data.body()).path("resource").path("name").asText());
        assertEquals(400, get(location(approved)).statusCode(), "callback state is single-use");
    }

    @Test
    void wrongOrAbsentStateIsRejected() throws Exception {
        get(client.uri().resolve("/authorize"));
        assertEquals(400, get(client.uri().resolve("/callback?state=wrong&code=unused")).statusCode());
        assertEquals(400, get(client.uri().resolve("/callback?code=unused")).statusCode());
        assertTrue(json.readTree(get(client.uri().resolve("/status")).body()).path("access_token").isNull());
    }

    @Test
    void denialReturnsAnErrorAndNoToken() throws Exception {
        var start = get(client.uri().resolve("/authorize"));
        var consent = get(location(start));
        var denied = post(authorization.uri().resolve("/approve"),
                Map.of("request_id", requestId(location(consent))), null);
        assertEquals("access_denied", Forms.query(location(denied)).get("error"));
        var returned = get(location(denied));
        assertEquals(302, returned.statusCode());
        assertTrue(location(returned).toString().contains("error=access_denied"));
        assertTrue(json.readTree(get(client.uri().resolve("/status")).body()).path("access_token").isNull());
    }

    @Test
    void invalidClientAndRedirectAreNeverRedirected() throws Exception {
        var invalidClient = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of(
                "client_id", "unknown", "redirect_uri", callback.toString(), "response_type", "code"))));
        assertEquals(400, invalidClient.statusCode());
        assertTrue(invalidClient.headers().firstValue("Location").isEmpty());
        var invalidRedirect = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of(
                "client_id", "oauth-client-1", "redirect_uri", "https://example.invalid", "response_type", "code"))));
        assertEquals(400, invalidRedirect.statusCode());
        assertTrue(invalidRedirect.headers().firstValue("Location").isEmpty());
    }

    @Test
    void tokenEndpointAuthenticatesClientsAndRejectsCodeReplay() throws Exception {
        String code = issueCode();
        var form = Map.of("grant_type", "authorization_code", "code", code, "redirect_uri", callback.toString());
        var bad = post(authorization.uri().resolve("/token"), form, Forms.basic("oauth-client-1", "wrong"));
        assertEquals(401, bad.statusCode());
        var issued = post(authorization.uri().resolve("/token"), form, credentials());
        assertEquals(200, issued.statusCode());
        assertEquals("no-store", issued.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("Bearer", json.readTree(issued.body()).path("token_type").asText());
        var replay = post(authorization.uri().resolve("/token"), form, credentials());
        assertEquals(400, replay.statusCode());
        assertEquals("invalid_grant", json.readTree(replay.body()).path("error").asText());
    }

    @Test
    void tokenEndpointAlsoAcceptsBodyCredentialsButRejectsAmbiguityAndMalformedBasic() throws Exception {
        var body = new java.util.HashMap<>(Map.of("grant_type", "authorization_code", "code", issueCode(),
                "redirect_uri", callback.toString(), "client_id", "oauth-client-1", "client_secret", "oauth-client-secret-1"));
        assertEquals(401, post(authorization.uri().resolve("/token"), body, credentials()).statusCode());
        assertEquals(401, post(authorization.uri().resolve("/token"), body, "Basic %%%").statusCode());
        assertEquals(200, post(authorization.uri().resolve("/token"), body, null).statusCode());
        var unsupported = post(authorization.uri().resolve("/token"), Map.of("grant_type", "unknown"), credentials());
        assertEquals("unsupported_grant_type", json.readTree(unsupported.body()).path("error").asText());
    }

    @Test
    void resourceRejectsMissingOrUnknownTokensAndSupportsOriginalTransportVariants() throws Exception {
        assertEquals(401, post(resource.uri().resolve("/resource"), Map.of(), null).statusCode());
        assertEquals(401, post(resource.uri().resolve("/resource"), Map.of(), "Bearer unknown").statusCode());
        var issued = post(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code",
                "code", issueCode(), "redirect_uri", callback.toString()), credentials());
        String token = json.readTree(issued.body()).path("access_token").asText();
        assertEquals(200, post(resource.uri().resolve("/resource"), Map.of(), "Bearer " + token).statusCode());
        assertEquals(200, post(resource.uri().resolve("/resource"), Map.of("access_token", token), null).statusCode());
        assertEquals(200, post(resource.uri().resolve("/resource?access_token=" + token), Map.of(), null).statusCode());
    }

    @Test
    void staticPagesAreAvailableAndIsolatedByServerRole() throws Exception {
        for (var server : new WebServer[]{client, authorization, resource}) {
            var page = get(server.uri());
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("<!doctype html>"));
            assertEquals(200, get(server.uri().resolve("/style.css")).statusCode());
        }
        assertEquals(200, get(authorization.uri().resolve("/approve.html")).statusCode());
        assertEquals(404, get(client.uri().resolve("/approve.html")).statusCode());
        assertEquals(404, get(resource.uri().resolve("/approve.html")).statusCode());
    }

    @Test
    void consentRequestCanOnlyBeSubmittedOnce() throws Exception {
        var consent = get(authorizeUri(Map.of()));
        var form = Map.of("request_id", requestId(location(consent)), "approve", "yes");
        assertEquals(302, post(authorization.uri().resolve("/approve"), form, null).statusCode());
        assertEquals(400, post(authorization.uri().resolve("/approve"), form, null).statusCode());
    }

    @Test
    void separateReadersSeeTokenWritesAndStartupReset() {
        var writer = new TokenStore(directory.resolve("independent.json"));
        var reader = new TokenStore(directory.resolve("independent.json"));
        assertFalse(reader.contains("token"));
        writer.put("token", "oauth-client-1", "foo");
        assertTrue(reader.contains("token"));
        writer.clear();
        assertFalse(reader.contains("token"));
    }

    @Test
    void refreshCanBeReusedButCannotBeUsedAsAnAccessToken() throws Exception {
        var issued = post(authorization.uri().resolve("/token"), Map.of("grant_type", "authorization_code",
                "code", issueCode(), "redirect_uri", callback.toString()), credentials());
        var before = json.readTree(issued.body());
        String refresh = before.path("refresh_token").asText();
        assertFalse(refresh.isBlank());
        assertEquals(401, post(resource.uri().resolve("/resource"), Map.of(), "Bearer " + refresh).statusCode());
        var form = Map.of("grant_type", "refresh_token", "refresh_token", refresh);
        for (int attempt = 0; attempt < 2; attempt++) {
            var result = post(authorization.uri().resolve("/token"), form, credentials());
            assertEquals(200, result.statusCode());
            var after = json.readTree(result.body());
            assertEquals(refresh, after.path("refresh_token").asText());
            assertNotEquals(before.path("access_token"), after.path("access_token"));
        }
        assertEquals(401, post(authorization.uri().resolve("/token"), form, Forms.basic("other", "secret")).statusCode());
        assertEquals(400, post(authorization.uri().resolve("/token"), Map.of("grant_type", "refresh_token", "refresh_token", "unknown"), credentials()).statusCode());
    }

    @Test
    void browserCanExplicitlyRefreshUsingBodyCredentials() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"))));
        var approval = post(authorization.uri().resolve("/approve"), Map.of("request_id", requestId(location(consent)), "approve", "yes"), null);
        assertEquals(302, get(location(approval)).statusCode());
        var before = json.readTree(get(client.uri().resolve("/status")).body());
        assertEquals(302, get(client.uri().resolve("/refresh")).statusCode());
        var after = json.readTree(get(client.uri().resolve("/status")).body());
        assertNotEquals(before.path("access_token"), after.path("access_token"));
        assertEquals(before.path("refresh_token"), after.path("refresh_token"));
        assertEquals(200, get(client.uri().resolve("/fetch_resource")).statusCode());
    }

    @Test
    void authorizationAndConsentRejectUnregisteredScopes() throws Exception {
        assertEquals("invalid_scope", Forms.query(location(get(authorizeUri(Map.of("scope", "unknown"))))).get("error"));
        var consent = get(authorizeUri(Map.of("scope", "foo")));
        var rejected = post(authorization.uri().resolve("/approve"), Map.of("request_id", requestId(location(consent)),
                "approve", "yes", "scope_unknown", "yes"), null);
        assertEquals("invalid_scope", Forms.query(location(rejected)).get("error"));
    }

    @Test
    void grantedScopeReachesResourceAndRefreshCanOnlyRetainOrNarrowIt() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"))));
        var approval = post(authorization.uri().resolve("/approve"), Map.of("request_id", requestId(location(consent)),
                "approve", "yes", "scope_foo", "yes"), null);
        assertEquals(302, get(location(approval)).statusCode());
        var status = json.readTree(get(client.uri().resolve("/status")).body());
        assertEquals("foo", status.path("scope").asText());
        assertEquals("foo", json.readTree(get(client.uri().resolve("/fetch_resource")).body()).path("scope").get(0).asText());
        String refresh = status.path("refresh_token").asText();
        var retained = post(authorization.uri().resolve("/token"), Map.of("grant_type", "refresh_token", "refresh_token", refresh), credentials());
        assertEquals("foo", json.readTree(retained.body()).path("scope").asText());
        var enlarged = post(authorization.uri().resolve("/token"), Map.of("grant_type", "refresh_token", "refresh_token", refresh, "scope", "foo bar"), credentials());
        assertEquals(400, enlarged.statusCode());
        assertEquals("invalid_scope", json.readTree(enlarged.body()).path("error").asText());
        var narrowed = post(authorization.uri().resolve("/token"), Map.of("grant_type", "refresh_token", "refresh_token", refresh, "scope", ""), credentials());
        assertEquals(200, narrowed.statusCode());
        assertEquals("", json.readTree(narrowed.body()).path("scope").asText());
    }

    @Test
    void implicitResponseUsesFragmentWithoutCodeExchangeOrRefreshToken() throws Exception {
        var consent = get(authorizeUri(Map.of("response_type", "token", "scope", "foo bar", "state", "browser-state")));
        var approval = post(authorization.uri().resolve("/approve"), Map.of("request_id", requestId(location(consent)),
                "approve", "yes", "scope_foo", "yes"), null);
        URI target = location(approval);
        assertNull(target.getRawQuery());
        var fragment = Forms.query(URI.create("http://localhost/?" + target.getRawFragment()));
        assertEquals("browser-state", fragment.get("state"));
        assertEquals("foo", fragment.get("scope"));
        assertEquals("Bearer", fragment.get("token_type"));
        assertFalse(fragment.containsKey("refresh_token"));
        assertEquals(200, post(resource.uri().resolve("/resource"), Map.of(), "Bearer " + fragment.get("access_token")).statusCode());
    }

    @Test
    void staticClientServesCallback() throws Exception {
        try (var staticClient = WebServer.start(0, "client", new Client())) {
            assertEquals(200, get(staticClient.uri().resolve("/callback")).statusCode());
            assertTrue(get(staticClient.uri().resolve("/client.js")).body().contains("parseCallback"));
        }
    }

    private String issueCode() throws Exception {
        var consent = get(authorizeUri(Map.of("state", "test-state")));
        var approved = post(authorization.uri().resolve("/approve"),
                Map.of("request_id", requestId(location(consent)), "approve", "yes"), null);
        return Forms.query(location(approved)).get("code");
    }

    private URI authorizeUri(Map<String, String> extra) {
        var query = new java.util.HashMap<>(Map.of("client_id", "oauth-client-1",
                "redirect_uri", callback.toString(), "response_type", "code"));
        query.putAll(extra);
        return authorization.uri().resolve("/authorize?" + Forms.encode(query));
    }

    private static String requestId(URI uri) { return Forms.query(uri).get("request_id"); }
    private static String credentials() { return Forms.basic("oauth-client-1", "oauth-client-secret-1"); }
    private static URI location(HttpResponse<?> response) {
        return response.uri().resolve(response.headers().firstValue("Location").orElseThrow());
    }
    private HttpResponse<String> get(URI uri) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(URI uri, Map<String, String> body, String auth) throws Exception {
        var builder = HttpRequest.newBuilder(uri).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(body)));
        if (auth != null) builder.header("Authorization", auth);
        return browser.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
