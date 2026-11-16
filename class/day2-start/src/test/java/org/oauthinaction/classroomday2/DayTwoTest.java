package org.oauthinaction.classroomday2;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DayTwoTest {
    @TempDir Path directory;
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    final ObjectMapper json = new ObjectMapper();
    @Test void oauthWorksWhileIdentityIsStillUnfinished() throws Exception {
        int port; try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        var callback = URI.create("http://localhost:" + port + "/callback");
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        try (var as = WebServer.start(0, "authorization", new AuthorizationServer(callback, tokens));
             var rs = WebServer.start(0, "resource", new ProtectedResource(new TokenStore(directory.resolve("tokens.json"))));
             var client = WebServer.start(port, "client", new Client(as.uri(), rs.uri(), callback))) {
            assertEquals(401, send(rs.uri().resolve("/resource"), "", null).statusCode());
            assertEquals(401, send(rs.uri().resolve("/userinfo"), null, null).statusCode());
            var consent = send(location(send(client.uri().resolve("/authorize"), null, null)), null, null);
            var approved = send(as.uri().resolve("/approve"), Forms.encode(Map.of("request_id", Forms.query(location(consent)).get("request_id"), "approve", "yes", "scope_foo", "yes", "user", "bob")), null);
            assertEquals(302, send(location(approved), null, null).statusCode());
            var status = json.readTree(send(client.uri().resolve("/status"), null, null).body());
            assertEquals("foo", status.path("scope").asText()); assertTrue(status.path("id_token").isNull());
            assertEquals(200, send(client.uri().resolve("/fetch_resource"), null, null).statusCode());
            assertEquals(501, send(client.uri().resolve("/userinfo"), null, null).statusCode());
            assertEquals(501, send(rs.uri().resolve("/userinfo"), null, "Bearer " + status.path("access_token").asText()).statusCode());
            assertEquals(400, send(location(approved), null, null).statusCode());
            assertEquals(200, send(client.uri().resolve("/style.css"), null, null).statusCode());
            assertEquals(400, send(as.uri().resolve("/token"), Forms.encode(Map.of("grant_type", "authorization_code", "code", Forms.query(location(approved)).get("code"))), Forms.basic("oauth-client-1", "oauth-client-secret-1")).statusCode());
            assertEquals(401, send(as.uri().resolve("/token"), "grant_type=authorization_code", "Basic broken").statusCode());
        }
    }
    URI location(HttpResponse<?> r) { return r.uri().resolve(r.headers().firstValue("Location").orElseThrow()); }
    HttpResponse<String> send(URI uri, String body, String auth) throws Exception {
        var b = HttpRequest.newBuilder(uri);
        if (body != null) b.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        if (auth != null) b.header("Authorization", auth);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
