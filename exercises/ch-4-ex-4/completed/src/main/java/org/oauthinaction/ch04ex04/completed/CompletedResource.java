package org.oauthinaction.ch04ex04.completed;

import org.oauthinaction.ch04ex04.TokenStore;
import org.oauthinaction.ch04ex04.WebServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class CompletedResource {
    private final TokenStore tokens;
    public CompletedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource(TokenStore.local())); }
    @GetMapping("/favorites")
    public ResponseEntity<?> favorites(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestParam Map<String, String> parameters) {
        String bearer = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)
                ? auth.substring(7) : parameters.get("access_token");
        var token = tokens.access(bearer);
        if (token == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        return ResponseEntity.ok(forUser(token.user()));
    }
    protected Map<String, Object> forUser(String user) {
        // The consent screen's user key is bound to the code, then to the token.
        if ("alice".equals(user)) return Map.of("user", "Alice", "favorites", Map.of(
                "movies", List.of("The Multidmensional Vector", "Space Fights", "Jewelry Boss"),
                "foods", List.of("bacon", "pizza", "bacon pizza"),
                "music", List.of("techno", "industrial", "alternative")));
        if ("bob".equals(user)) return Map.of("user", "Bob", "favorites", Map.of(
                "movies", List.of("An Unrequited Love", "Several Shades of Turquoise", "Think Of The Children"),
                "foods", List.of("bacon", "kale", "gravel"),
                "music", List.of("baroque", "ukulele", "baroque ukulele")));
        return unknown();
    }
    protected static Map<String, Object> unknown() {
        return Map.of("user", "Unknown", "favorites", Map.of("movies", List.of(), "foods", List.of(), "music", List.of()));
    }
}
