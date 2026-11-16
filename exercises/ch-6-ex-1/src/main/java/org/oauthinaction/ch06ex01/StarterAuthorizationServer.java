package org.oauthinaction.ch06ex01;
import org.oauthinaction.ch06ex01.completed.CompletedAuthorizationServer;
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
    @Override protected ResponseEntity<?> implicit(String scope, String state) {
        // TODO: save an access token, then return token/type/scope/state in the redirect fragment.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: issue an implicit token response"));
    }
}
