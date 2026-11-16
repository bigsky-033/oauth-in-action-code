package org.oauthinaction.classroom;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
class ScaffoldTest {
    @Test void allRolesServeIndependentPagesAndLocalAssets() throws Exception {
        try (var client = WebServer.start(0, "client", new Client()); var as = WebServer.start(0, "authorization", new AuthorizationServer()); var rs = WebServer.start(0, "resource", new ProtectedResource())) {
            var http = HttpClient.newHttpClient();
            for (var server : new WebServer[]{client, as, rs}) {
                var response = http.send(HttpRequest.newBuilder(server.uri()).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode()); assertTrue(response.body().contains("scaffold"));
                assertEquals(200, http.send(HttpRequest.newBuilder(server.uri().resolve("/style.css")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            }
            assertTrue(http.send(HttpRequest.newBuilder(client.uri().resolve("/status")).build(), HttpResponse.BodyHandlers.ofString()).body().contains("\"access_token\":null"));
            assertTrue(http.send(HttpRequest.newBuilder(as.uri().resolve("/status")).build(), HttpResponse.BodyHandlers.ofString()).body().contains("\"clients\":[]"));
            assertEquals(501, http.send(HttpRequest.newBuilder(client.uri().resolve("/authorize")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401, http.send(HttpRequest.newBuilder(rs.uri().resolve("/userinfo")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(501, http.send(HttpRequest.newBuilder(as.uri().resolve("/authorize")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(501, http.send(HttpRequest.newBuilder(rs.uri().resolve("/resource")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
}
