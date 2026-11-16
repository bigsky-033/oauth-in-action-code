package org.oauthinaction.ch11ex05;
import org.oauthinaction.ch11ex05.completed.CompletedClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource, URI callback) { super(authorization, resource, callback); }
    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))); }
    @Override protected boolean starter() { return true; }
    @Override public ResponseEntity<?> revoke(HttpSession session) {
        // TODO: call /revoke with client authentication and clear the local tokens after success.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: implement client revocation request"));
    }
}
