package org.oauthinaction.ch13ex01;
import org.oauthinaction.ch13ex01.completed.CompletedResource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;
@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(TokenStore tokens) { super(tokens); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @Override public ResponseEntity<?> userInfo(String auth, Map<String, String> parameters) {
        // TODO: require an openid token, resolve its user, and return claims allowed by scope.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: implement UserInfo"));
    }
}
