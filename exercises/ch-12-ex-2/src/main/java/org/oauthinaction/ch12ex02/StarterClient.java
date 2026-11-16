package org.oauthinaction.ch12ex02;
import org.oauthinaction.ch12ex02.completed.CompletedClient;
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
    @Override public ResponseEntity<?> readClient(HttpSession session) { return todo(); }
    @Override public ResponseEntity<?> updateClient(String name, HttpSession session) { return todo(); }
    @Override public ResponseEntity<?> unregisterClient(HttpSession session) { return todo(); }
    private ResponseEntity<?> todo() {
        // TODO: call the management URI with its registration bearer for GET/PUT/DELETE.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: manage this registration"));
    }
}
