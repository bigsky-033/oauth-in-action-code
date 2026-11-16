package org.oauthinaction.ch15ex01;
import org.oauthinaction.ch15ex01.completed.CompletedAuthorizationServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) { var tokens = TokenStore.local(); tokens.clear(); WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)); }
    @Override protected ResponseEntity<?> issueToken(String clientId, String scope) {
        // TODO: generate per-token RSA keys, save the public key and deliver the private key to the client.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: issue a key-bound access token"));
    }
}
