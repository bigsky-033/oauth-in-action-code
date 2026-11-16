package org.oauthinaction.ch05ex01;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public final class StarterAuthorizationServer {
    public static void main(String[] args) {
        TokenStore.local().clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer());
    }
    @GetMapping("/status")
    public Map<String, Object> status() { return Map.of("starter", true, "clients", java.util.List.of()); }
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam Map<String, String> query) {
        // TODO: register the client, validate its redirect, and save a consent request.
        return unfinished("Handle the authorization request");
    }
    @PostMapping("/approve")
    public ResponseEntity<?> approve(@RequestParam Map<String, String> form) {
        // TODO: consume the consent request and return a code or access_denied.
        return unfinished("Handle consent");
    }
    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestHeader(value = "Authorization", required = false) String header,
                                   @RequestParam Map<String, String> form) {
        // TODO: authenticate the client, consume the code, and save/return an access token.
        return unfinished("Exchange the authorization code");
    }
    private static ResponseEntity<?> unfinished(String step) { return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: " + step)); }
}
