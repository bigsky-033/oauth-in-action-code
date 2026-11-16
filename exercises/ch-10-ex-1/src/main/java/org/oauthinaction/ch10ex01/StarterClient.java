package org.oauthinaction.ch10ex01;
import org.oauthinaction.ch10ex01.completed.CompletedClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.Map;
@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient(URI authorization, URI resource, URI callback) { super(authorization, resource, callback); }
    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))); }
    @Override protected boolean starter() { return true; }
    @Override protected Map<String, String> pkceParameters(HttpSession session) {
        // TODO: create/store a verifier and send its S256 challenge. Callback sends the stored verifier.
        return Map.of();
    }
}
