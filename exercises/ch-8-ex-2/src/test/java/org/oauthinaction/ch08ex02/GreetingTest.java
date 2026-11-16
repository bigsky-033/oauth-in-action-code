package org.oauthinaction.ch08ex02;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.oauthinaction.ch08ex02.completed.CompletedResource;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GreetingTest {
    @TempDir Path directory;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    WebServer authorization, resource, client;
    TokenStore tokens;
    URI callback;
    @BeforeEach void start() throws Exception {
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        callback = URI.create("http://localhost:" + port + "/callback");
        tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear(); tokens.put("greeting-token", "oauth-client-1", "greeting");
        authorization = WebServer.start(0, "authorization", new AuthorizationServer(callback, tokens));
        resource = WebServer.start(0, "resource", new CompletedResource(new TokenStore(directory.resolve("tokens.json"))));
        client = WebServer.start(port, "client", new Client());
    }
    @AfterEach void stop() { if (client != null) client.close(); if (resource != null) resource.close(); if (authorization != null) authorization.close(); }
    @Test void allLanguagesReturnJsonAndContentProtectionHeaders() throws Exception {
        var expected = Map.of("en", "Hello World", "de", "Hallo Welt", "it", "Ciao Mondo", "fr", "Bonjour monde", "es", "Hola mundo");
        for (var language : expected.entrySet()) {
            var response = greeting(resource.uri(), language.getKey(), "greeting-token");
            assertEquals(200, response.statusCode()); assertEquals(language.getValue(), json.readTree(response.body()).path("greeting").asText());
            assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
            assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElseThrow());
            assertEquals("1; mode=block", response.headers().firstValue("X-XSS-Protection").orElseThrow());
        }
    }
    @Test void missingAndUnknownTokensReturn401RatherThanHanging() throws Exception {
        assertEquals(401, greeting(resource.uri(), "en", null).statusCode());
        assertEquals(401, greeting(resource.uri(), "en", "unknown").statusCode());
    }
    @Test void resourceUsesBookHeaderAndQueryTokenTransport() throws Exception {
        assertEquals(200, get(resource.uri().resolve("/helloWorld?language=en&access_token=greeting-token"), null).statusCode());
        assertEquals(401, get(resource.uri().resolve("/helloWorld?language=en&access_token=greeting-token"), "Bearer wrong").statusCode());
    }
    @Test void corsAllowsBrowserBearerRequestsButStarterDoesNot() throws Exception {
        var preflight = HttpRequest.newBuilder(resource.uri().resolve("/helloWorld?language=en"))
                .header("Origin", "http://localhost:9000").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        var response = http.send(preflight, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode()); assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
        var direct = HttpRequest.newBuilder(resource.uri().resolve("/helloWorld?language=en"))
                .header("Origin", "http://localhost:9000").header("Authorization", "Bearer greeting-token").build();
        assertTrue(http.send(direct, HttpResponse.BodyHandlers.ofString()).headers().firstValue("Access-Control-Allow-Origin").isPresent());
        try (var starter = WebServer.start(0, "resource", new StarterResource(tokens))) {
            var without = HttpRequest.newBuilder(starter.uri().resolve("/helloWorld?language=en"))
                    .header("Origin", "http://localhost:9000").header("Authorization", "Bearer greeting-token").build();
            assertTrue(http.send(without, HttpResponse.BodyHandlers.ofString()).headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        }
    }
    @Test void implicitBrowserFlowReturnsFragmentAndServesCallback() throws Exception {
        var consent = get(authorization.uri().resolve("/authorize?" + Forms.encode(Map.of("client_id", "oauth-client-1",
                "redirect_uri", callback.toString(), "response_type", "token", "scope", "greeting", "state", "test-state"))), null);
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"),
                "approve", "yes", "scope_greeting", "yes"), null);
        var values = Forms.query(URI.create("http://localhost/?" + location(approved).getRawFragment()));
        assertEquals("test-state", values.get("state")); assertEquals("greeting", values.get("scope"));
        assertEquals(200, greeting(resource.uri(), "en", values.get("access_token")).statusCode());
        assertEquals(200, get(client.uri().resolve("/callback"), null).statusCode());
    }
    HttpResponse<String> greeting(URI base, String language, String token) throws Exception {
        return get(base.resolve("/helloWorld?" + Forms.encode(Map.of("language", language))), token == null ? null : "Bearer " + token);
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
