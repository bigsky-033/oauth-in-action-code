package org.oauthinaction.examplesolution0;
import org.oauthinaction.examplechapter8.AuthorizationServer;
import org.oauthinaction.examplechapter8.TokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ResponseTest {
    @TempDir Path directory;
    @Test void threeSolutionsDifferInMimeAndHeadersButAllEncodeUnknownInput() throws Exception {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear(); tokens.put("token", "oauth-client-1", "greeting");
        try (var as = WebServer.start(0, "authorization", new AuthorizationServer(URI.create("http://localhost/callback"), tokens))) {
            Object[] variants = {new ProtectedResource0a(as.uri()), new ProtectedResource0b(as.uri()), new ProtectedResource0c(as.uri())};
            var http = HttpClient.newHttpClient();
            for (Object variant : variants) try (var resource = WebServer.start(0, "resource", variant)) {
                var uri = resource.uri().resolve("/helloWorld?language=%3Cscript%3E");
                var result = http.send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer token").build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, result.statusCode()); assertTrue(result.body().contains("%3Cscript%3E"));
                assertTrue(result.headers().firstValue("Content-Type").orElseThrow().startsWith(variant instanceof ProtectedResource0a ? "text/html" : "application/json"));
                assertEquals(variant instanceof ProtectedResource0c, result.headers().firstValue("X-Content-Type-Options").isPresent());
                assertEquals(401, http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
                assertEquals(200, http.send(HttpRequest.newBuilder(resource.uri()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            }
        }
    }
}
