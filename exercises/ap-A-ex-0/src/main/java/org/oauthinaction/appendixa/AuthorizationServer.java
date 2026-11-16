package org.oauthinaction.appendixa;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public final class AuthorizationServer {
    public static void main(String[] args) { WebServer.start(9001, "authorization", new AuthorizationServer()); }
    @GetMapping("/status") public Map<String, Object> status() {
        return Map.of("clients", List.of(Map.of("client_id", "oauth-client-1", "client_secret", "oauth-client-secret-1", "redirect_uris", List.of("http://localhost:9000/callback"), "scope", "foo bar")));
    }
}
