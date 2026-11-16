package org.oauthinaction.ch06ex02;
import org.oauthinaction.ch06ex02.completed.CompletedAuthorizationServer;
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
    @Override protected ResponseEntity<?> clientCredentials(String clientId, String scope) {
        // TODO: validate scope, save an access token for the client and return it without a refresh token.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: implement client credentials grant"));
    }
}
