package org.oauthinaction.ch03ex01;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public final class ProtectedResource {
    private final TokenStore tokens;

    public ProtectedResource(TokenStore tokens) { this.tokens = tokens; }

    public static void main(String[] args) {
        WebServer.start(9002, "resource", new ProtectedResource(TokenStore.local()));
    }

    @CrossOrigin
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> parameters) {
        String token = parameters.get("access_token");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorization.substring(7);
        }
        // Body/query transport is retained for the book; the Java client uses a header.
        if (!tokens.contains(token)) {
            return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build();
        }
        return ResponseEntity.ok(Map.of("name", "Protected Resource",
                "description", "This data has been protected by OAuth 2.0"));
    }
}
