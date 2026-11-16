package org.oauthinaction.ch13ex01;
import org.oauthinaction.ch13ex01.completed.CompletedClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource, URI callback) { super(authorization, resource, callback); }
    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))); }
    @Override protected boolean starter() { return true; }
    @Override protected JsonNode validateIdToken(String token, HttpSession session) {
        // TODO: validate signature, issuer, audience, times and the requested nonce before saving identity.
        return null;
    }
    @Override public ResponseEntity<?> userInfo(HttpSession session) {
        // TODO: call UserInfo with the access token and match its sub to the ID token.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: call UserInfo"));
    }
}
