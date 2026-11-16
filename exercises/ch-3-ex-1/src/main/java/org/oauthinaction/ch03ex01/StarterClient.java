package org.oauthinaction.ch03ex01;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public final class StarterClient {
    public static void main(String[] args) { WebServer.start(9000, "client", new StarterClient()); }

    @GetMapping("/status")
    public Map<String, Object> status() {
        var status = new LinkedHashMap<String, Object>();
        status.put("starter", true);
        status.put("access_token", null);
        status.put("scope", null);
        return status;
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize() {
        // TODO: configure client credentials, remember state, and redirect to /authorize.
        return unfinished("Build the authorization request");
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback() {
        // TODO: validate state, exchange the code with Basic authentication, save the token.
        return unfinished("Exchange the authorization code");
    }

    @GetMapping("/fetch_resource")
    public ResponseEntity<?> fetchResource() {
        // TODO: call POST /resource with Authorization: Bearer <access_token>.
        return unfinished("Call the protected resource");
    }

    private static ResponseEntity<?> unfinished(String step) {
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: " + step));
    }
}
