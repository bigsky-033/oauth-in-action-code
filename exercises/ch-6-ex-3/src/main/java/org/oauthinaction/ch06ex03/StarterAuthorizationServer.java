package org.oauthinaction.ch06ex03;
import org.oauthinaction.ch06ex03.completed.CompletedAuthorizationServer;
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
    @Override protected ResponseEntity<?> password(String clientId, String username, String password, String scope) {
        // TODO: validate the resource owner and requested scope, then issue access and refresh tokens.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: implement password grant"));
    }
}
