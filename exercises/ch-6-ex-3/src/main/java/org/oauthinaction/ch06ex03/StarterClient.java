package org.oauthinaction.ch06ex03;
import org.oauthinaction.ch06ex03.completed.CompletedClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;

@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource) { super(authorization, resource); }
    public static void main(String[] args) {
        WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/")));
    }
    @Override protected boolean starter() { return true; }
    @Override public ResponseEntity<?> usernamePassword(Map<String, String> form, HttpSession session) {
        // TODO: send grant_type=password with the form's resource owner credentials and requested scope.
        return error(501, "Exercise TODO: implement resource owner password request");
    }
}
