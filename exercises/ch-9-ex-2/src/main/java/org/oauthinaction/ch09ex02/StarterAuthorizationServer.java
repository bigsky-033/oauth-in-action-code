package org.oauthinaction.ch09ex02;
import org.oauthinaction.ch09ex02.completed.CompletedAuthorizationServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;

@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    private final URI callback;
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); this.callback = callback; }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override protected ResponseEntity<?> invalidRequestedScope(String state) {
        // TODO: return a local invalid_scope error instead of redirecting this request.
        return Forms.redirect(callback, state == null ? Map.of("error", "invalid_scope") : Map.of("error", "invalid_scope", "state", state));
    }
}
