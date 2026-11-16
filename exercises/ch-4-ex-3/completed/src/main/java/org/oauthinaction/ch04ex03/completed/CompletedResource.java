package org.oauthinaction.ch04ex03.completed;

import org.oauthinaction.ch04ex03.TokenStore;
import org.oauthinaction.ch04ex03.WebServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class CompletedResource {
    private final TokenStore tokens;
    public CompletedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource(TokenStore.local())); }
    @GetMapping("/produce")
    public ResponseEntity<?> produce(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestParam Map<String, String> parameters) {
        String bearer = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)
                ? auth.substring(7) : parameters.get("access_token");
        var token = tokens.access(bearer);
        if (token == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        // A valid token receives a response; scopes determine WHICH fields have data.
        return ResponseEntity.ok(Map.of(
                "fruit", allowed(token, "fruit") ? List.of("apple", "banana", "kiwi") : List.of(),
                "veggies", allowed(token, "veggies") ? List.of("lettuce", "onion", "potato") : List.of(),
                "meats", allowed(token, "meats") ? List.of("bacon", "steak", "chicken breast") : List.of()));
    }
    protected boolean allowed(TokenStore.Token token, String category) { return token.scope().contains(category); }
}
