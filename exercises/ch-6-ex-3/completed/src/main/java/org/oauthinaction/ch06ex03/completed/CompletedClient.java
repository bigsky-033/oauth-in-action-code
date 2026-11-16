package org.oauthinaction.ch06ex03.completed;
import org.oauthinaction.ch06ex03.*;
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
        return Forms.redirect(URI.create("/username_password.html"), Map.of());
    }
    @PostMapping("/username_password")
    public ResponseEntity<?> usernamePassword(@RequestParam Map<String, String> form, HttpSession session) {
        // Resource owner credentials are sent to the token endpoint with client authentication.
        return requestToken(Map.of("grant_type", "password", "scope", "foo bar",
                "username", form.getOrDefault("username", ""), "password", form.getOrDefault("password", "")), session);
    }
}
