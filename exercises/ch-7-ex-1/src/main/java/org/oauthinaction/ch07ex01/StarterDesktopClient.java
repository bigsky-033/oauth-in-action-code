package org.oauthinaction.ch07ex01;
import org.oauthinaction.ch07ex01.completed.CompletedDesktopClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.Map;

@RestController
public final class StarterDesktopClient extends CompletedDesktopClient {
    public StarterDesktopClient(URI authorization, URI resource, URI callback) { super(authorization, resource, callback); }
    public static void main(String[] args) {
        WebServer.start(9000, "client", new StarterDesktopClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback")));
    }
    @Override protected boolean starter() { return true; }
    @Override protected ResponseEntity<?> register(HttpSession session) {
        // TODO: POST registration metadata and retain the issued client_id/client_secret.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: dynamically register the native client"));
    }
}
