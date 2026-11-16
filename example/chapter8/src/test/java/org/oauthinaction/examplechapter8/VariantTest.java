package org.oauthinaction.examplechapter8;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class VariantTest {
    @TempDir Path directory;
    @Test void baselineVariantsKeepTheirDifferentRawResponsesAndRequireActiveTokens() throws Exception {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear(); tokens.put("token", "oauth-client-1", "greeting");
        try (var as = WebServer.start(0, "authorization", new AuthorizationServer(URI.create("http://localhost/callback"), tokens))) {
            Object[] variants = {new ProtectedResource0(as.uri()), new ProtectedResource1(as.uri()), new ProtectedResource2(as.uri()), new ProtectedResource3(as.uri())};
            var http = HttpClient.newHttpClient();
            for (Object variant : variants) try (var server = WebServer.start(0, "resource", variant)) {
                var uri = server.uri().resolve("/helloWorld?" + Forms.encode(Map.of("language", "<script>alert(1)</script>")));
                var result = http.send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer token").header("Origin", "http://localhost:9000").build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, result.statusCode()); assertTrue(result.body().contains("<script>"));
                assertTrue(result.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
                assertTrue(result.headers().firstValue("X-Content-Type-Options").isEmpty());
                if (variant instanceof ProtectedResource1) assertTrue(result.headers().firstValue("Content-Type").isEmpty());
                else assertTrue(result.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
                assertEquals(401, http.send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer unknown").build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            }
        }
    }
    @Test void clientCanDisplayATextGreetingAndImplicitCallbackIsServed() throws Exception {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        var callback = URI.create("http://localhost:" + port + "/callback");
        try (var as = WebServer.start(0, "authorization", new AuthorizationServer(callback, tokens));
             var rs = WebServer.start(0, "resource", new ProtectedResource0(as.uri()));
             var client = WebServer.start(port, "client", new Client(as.uri(), rs.uri(), callback));
             var implicit = WebServer.start(0, "implicit", new ImplicitClient())) {
            var http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            var first = http.send(HttpRequest.newBuilder(client.uri().resolve("/authorize")).build(), HttpResponse.BodyHandlers.ofString());
            var consent = http.send(HttpRequest.newBuilder(URI.create(first.headers().firstValue("Location").orElseThrow())).build(), HttpResponse.BodyHandlers.ofString());
            String id = Forms.query(URI.create(consent.headers().firstValue("Location").orElseThrow())).get("request_id");
            var approved = http.send(HttpRequest.newBuilder(as.uri().resolve("/approve")).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("request_id", id, "approve", "yes", "scope_greeting", "yes")))).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(302, http.send(HttpRequest.newBuilder(URI.create(approved.headers().firstValue("Location").orElseThrow())).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            var greeting = http.send(HttpRequest.newBuilder(client.uri().resolve("/fetch_resource?language=de")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, greeting.statusCode()); assertTrue(greeting.body().contains("Hallo Welt"));
            assertEquals(200, http.send(HttpRequest.newBuilder(implicit.uri().resolve("/callback")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
}
