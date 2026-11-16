package org.oauthinaction.ch12ex02;
import org.oauthinaction.ch12ex02.completed.CompletedAuthorizationServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) { var tokens = TokenStore.local(); tokens.clear(); WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)); }
    @Override public ResponseEntity<?> readRegistration(String id, String auth) { return todo(); }
    @Override public ResponseEntity<?> updateRegistration(String id, String auth, Map<String, Object> metadata) { return todo(); }
    @Override public ResponseEntity<?> deleteRegistration(String id, String auth) { return todo(); }
    private ResponseEntity<?> todo() {
        // TODO: authenticate the registration bearer, preserve issued fields on update, and delete client tokens on removal.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: complete registration management"));
    }
}
