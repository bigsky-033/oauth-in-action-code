package org.oauthinaction.ch05ex02;
import org.oauthinaction.ch05ex02.completed.CompletedAuthorizationServer;
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
    @Override protected String createRefreshToken(String clientId) {
        // TODO: generate, save and return a refresh token bound to this client.
        return null;
    }
    @Override protected ResponseEntity<?> refresh(String clientId, String refreshToken) {
        // TODO: find the refresh token, check its client, and issue a fresh access token.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: process refresh tokens"));
    }
}
