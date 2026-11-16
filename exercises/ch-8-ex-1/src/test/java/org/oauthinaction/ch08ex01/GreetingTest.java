package org.oauthinaction.ch08ex01;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.oauthinaction.ch08ex01.completed.CompletedResource;
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
        client = WebServer.start(port, "client", new Client(authorization.uri(), resource.uri(), callback));
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
    @Test void reflectedHtmlIsEncodedInJsonWhileStarterRetainsTheTeachingWeakness() throws Exception {
        String input = "<script>alert(1)</script>";
        var safe = greeting(resource.uri(), input, "greeting-token");
        assertTrue(json.readTree(safe.body()).path("greeting").asText().contains("%3Cscript%3E"));
        try (var starter = WebServer.start(0, "resource", new StarterResource(tokens))) {
            var unsafe = greeting(starter.uri(), input, "greeting-token");
            assertTrue(unsafe.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
            assertTrue(unsafe.body().contains(input)); assertTrue(unsafe.headers().firstValue("X-Content-Type-Options").isEmpty());
        }
    }
    @Test void missingAndUnknownTokensReturn401RatherThanHanging() throws Exception {
        assertEquals(401, greeting(resource.uri(), "en", null).statusCode());
        assertEquals(401, greeting(resource.uri(), "en", "unknown").statusCode());
    }
    @Test void codeFlowClientCanChooseLanguageAndRevokeItsToken() throws Exception {
        authorizeClient();
        var response = get(client.uri().resolve("/fetch_resource?language=de"), null);
        assertEquals("Hallo Welt", json.readTree(response.body()).path("greeting").asText());
        assertEquals(302, form(client.uri().resolve("/revoke"), Map.of(), null).statusCode());
        assertEquals(400, get(client.uri().resolve("/fetch_resource"), null).statusCode());
    }
    @Test void stateErrorsAndSupportingClientPagesAreExplicit() throws Exception {
        assertEquals(400, get(client.uri().resolve("/callback?state=wrong&code=unused"), null).statusCode());
        assertEquals(302, get(client.uri().resolve("/username_password"), null).statusCode());
        assertEquals(200, get(client.uri().resolve("/username_password.html"), null).statusCode());
        assertEquals(302, get(client.uri().resolve("/revoke"), null).statusCode());
        assertEquals(200, get(client.uri(), null).statusCode()); assertEquals(404, get(client.uri().resolve("/approve.html"), null).statusCode());
    }
    @Test void resourceUsesBookHeaderAndQueryTokenTransport() throws Exception {
        assertEquals(200, get(resource.uri().resolve("/helloWorld?language=en&access_token=greeting-token"), null).statusCode());
        assertEquals(401, get(resource.uri().resolve("/helloWorld?language=en&access_token=greeting-token"), "Bearer wrong").statusCode());
    }
    void authorizeClient() throws Exception {
        var consent = get(location(get(client.uri().resolve("/authorize"), null)), null);
        var approved = form(authorization.uri().resolve("/approve"), Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_greeting", "yes", "user", "alice"), null);
        assertEquals(302, get(location(approved), null).statusCode());
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
