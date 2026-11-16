package org.oauthinaction.ch04ex02;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.oauthinaction.ch04ex02.completed.CompletedResource;

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
        resource = WebServer.start(0, "resource", new CompletedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(clientPort, "client",
                new Client(authorization.uri(), resource.uri(), callback));
    }

    @AfterEach
    void stop() {
        if (client != null) client.close();
        if (resource != null) resource.close();
        if (authorization != null) authorization.close();
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
    void invalidScopesAndUnsupportedResponseTypesReturnProtocolErrors() throws Exception {
        var response = get(authorizeUri(Map.of("scope", "not-registered")));
        assertEquals("invalid_scope", Forms.query(location(response)).get("error"));
        response = get(authorizeUri(Map.of("response_type", "id_token")));
        assertEquals("unsupported_response_type", Forms.query(location(response)).get("error"));
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
    void wordScopesAreCheckedIndependently() throws Exception {
        for (String method : new String[]{"GET", "POST", "DELETE"}) {
            assertEquals(401, wordRequest(method, null, Map.of()).statusCode());
            assertEquals(401, wordRequest(method, "unknown", Map.of()).statusCode());
        }
        var store = new TokenStore(directory.resolve("tokens.json"));
        store.put("read-only", "oauth-client-1", "read");
        store.put("write-only", "oauth-client-1", "write");
        store.put("delete-only", "oauth-client-1", "delete");
        assertEquals(200, wordRequest("GET", "read-only", Map.of()).statusCode());
        assertEquals(201, wordRequest("POST", "write-only", Map.of("word", "hello")).statusCode());
        assertEquals(403, wordRequest("GET", "write-only", Map.of()).statusCode());
        var forbidden = wordRequest("POST", "read-only", Map.of("word", "forbidden"));
        assertEquals(403, forbidden.statusCode());
        assertTrue(forbidden.headers().firstValue("WWW-Authenticate").orElseThrow().contains("scope=\"write\""));
        assertEquals(403, wordRequest("DELETE", "read-only", Map.of()).statusCode());
        assertEquals("hello", json.readTree(wordRequest("GET", "read-only", Map.of()).body()).path("words").asText());
        assertEquals(204, wordRequest("DELETE", "delete-only", Map.of()).statusCode());
        assertEquals(204, wordRequest("DELETE", "delete-only", Map.of()).statusCode());
        assertEquals("", json.readTree(wordRequest("GET", "read-only", Map.of()).body()).path("words").asText());
    }

    @Test
    void browserCanAuthorizeThenAddReadAndDeleteWords() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"))));
        var approved = post(authorization.uri().resolve("/approve"), Map.of("request_id", requestId(location(consent)),
                "approve", "yes", "scope_read", "yes", "scope_write", "yes", "scope_delete", "yes"), null);
        assertEquals(302, get(location(approved)).statusCode());
        assertEquals(201, get(client.uri().resolve("/add_word?word=hello")).statusCode());
        var result = get(client.uri().resolve("/get_words"));
        assertEquals(200, result.statusCode());
        assertEquals("hello", json.readTree(result.body()).path("words").asText());
        assertTrue(json.readTree(result.body()).path("timestamp").asLong() > 0);
        assertEquals(204, get(client.uri().resolve("/delete_word")).statusCode());
    }

    @Test
    void starterDemonstratesMissingScopeChecksButStillRequiresAToken() throws Exception {
        var store = new TokenStore(directory.resolve("tokens.json"));
        store.put("no-scope", "oauth-client-1", "");
        try (var starter = WebServer.start(0, "resource", new StarterResource(store))) {
            var unauthenticated = get(starter.uri().resolve("/words"));
            assertEquals(401, unauthenticated.statusCode());
            var response = browser.send(HttpRequest.newBuilder(starter.uri().resolve("/words"))
                    .header("Authorization", "Bearer no-scope").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        }
        assertEquals(403, wordRequest("GET", "no-scope", Map.of()).statusCode());
    }

    private HttpResponse<String> wordRequest(String method, String token, Map<String, String> form) throws Exception {
        var builder = HttpRequest.newBuilder(resource.uri().resolve("/words"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method(method, form.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(Forms.encode(form)));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return browser.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
