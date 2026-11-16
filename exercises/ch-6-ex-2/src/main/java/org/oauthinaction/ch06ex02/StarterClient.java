package org.oauthinaction.ch06ex02;
import org.oauthinaction.ch06ex02.completed.CompletedClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource) { super(authorization, resource); }
    public static void main(String[] args) {
        WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/")));
    }
    @Override protected boolean starter() { return true; }
    @Override public ResponseEntity<?> authorize(HttpSession session) {
        // TODO: request a client_credentials token with the requested scope.
        return error(501, "Exercise TODO: implement client credentials request");
    }
}
