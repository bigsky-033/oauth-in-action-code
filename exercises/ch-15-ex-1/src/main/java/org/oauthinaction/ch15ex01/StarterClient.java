package org.oauthinaction.ch15ex01;
import org.oauthinaction.ch15ex01.completed.CompletedClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource, URI callback) { super(authorization, resource, callback); }
    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))); }
    @Override protected boolean starter() { return true; }
    @Override public ResponseEntity<?> fetchResource(HttpSession session) {
        // TODO: sign at/ts/m/u/p with the delivered private JWK and send Authorization: PoP <JWS>.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: send a signed proof"));
    }
}
