package org.oauthinaction.ch06ex02.completed;
import org.oauthinaction.ch06ex02.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
public class CompletedClient extends ClientSupport {
    public CompletedClient(URI authorization, URI resource) { super(authorization, resource); }
    public static void main(String[] args) {
        WebServer.start(9000, "client", new CompletedClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/")));
    }
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(HttpSession session) {
        // Direct token request: there is no resource owner, state, or consent redirect.
        // requestToken sends these fields with HTTP Basic client authentication.
        return requestToken(Map.of("grant_type", "client_credentials", "scope", "foo bar"), session);
    }
}
