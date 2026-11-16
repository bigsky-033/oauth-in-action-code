package org.oauthinaction.ch11ex04;
import org.oauthinaction.ch11ex04.completed.CompletedAuthorizationServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override public ResponseEntity<?> introspect(String header, Map<String, String> form) {
        // TODO: authenticate the resource, look up the token and return active metadata.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: implement introspection"));
    }
}
