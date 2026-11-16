package org.oauthinaction.ch03ex02;

import jakarta.servlet.http.HttpSession;
import org.oauthinaction.ch03ex02.completed.CompletedClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/** Authorization-code behavior is already provided; this exercise adds refresh. */
@RestController
public final class StarterClient extends CompletedClient {
    public StarterClient() {
        this(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"),
                URI.create("http://localhost:9000/callback"));
    }

    public StarterClient(URI authorizationServer, URI resourceServer, URI redirectUri) {
        super(authorizationServer, resourceServer, redirectUri);
    }

    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient()); }

    @Override protected boolean isStarter() { return true; }

    @Override
    protected ResponseEntity<?> refreshAccessToken(HttpSession session) {
        // TODO: POST grant_type=refresh_token with the stored refresh_token and
        // Basic client authentication. Save the new access token and retry the resource.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: refresh the access token"));
    }
}
