package org.oauthinaction.examplesolution1;
import org.oauthinaction.examplechapter8.AuthorizationServer;
import org.oauthinaction.examplechapter8.TokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class CorsTest {
    @TempDir Path directory;
    @Test void corsPreflightAndJsonResponsePermitBrowserBearerRequests() throws Exception {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear(); tokens.put("token", "oauth-client-1", "greeting");
        try (var as = WebServer.start(0, "authorization", new AuthorizationServer(URI.create("http://localhost/callback"), tokens));
             var rs = WebServer.start(0, "resource", new ProtectedResource2(as.uri()))) {
            var http = HttpClient.newHttpClient(); var uri = rs.uri().resolve("/helloWorld?language=%3Cscript%3E");
            var preflight = HttpRequest.newBuilder(uri).header("Origin", "http://localhost:9000").header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "Authorization").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
            var checked = http.send(preflight, HttpResponse.BodyHandlers.ofString()); assertEquals(200, checked.statusCode());
            assertTrue(checked.headers().firstValue("Access-Control-Allow-Origin").isPresent());
            var result = http.send(HttpRequest.newBuilder(uri).header("Origin", "http://localhost:9000").header("Authorization", "Bearer token").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, result.statusCode()); assertTrue(result.body().contains("%3Cscript%3E"));
            assertTrue(result.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
            assertEquals("nosniff", result.headers().firstValue("X-Content-Type-Options").orElseThrow());
            assertTrue(result.headers().firstValue("Access-Control-Allow-Origin").isPresent());
            assertEquals(401, http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
}
