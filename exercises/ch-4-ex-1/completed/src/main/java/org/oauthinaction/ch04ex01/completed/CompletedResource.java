package org.oauthinaction.ch04ex01.completed;

import org.oauthinaction.ch04ex01.TokenStore;
import org.oauthinaction.ch04ex01.WebServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public final class CompletedResource {
    private final TokenStore tokens;
    public CompletedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource(TokenStore.local())); }

    @CrossOrigin
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> parameters) {
        String token = parameters.get("access_token");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7);
        }
        // Header takes precedence. The book also demonstrates body/query transport.
        if (tokens.access(token) == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        return ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
}
