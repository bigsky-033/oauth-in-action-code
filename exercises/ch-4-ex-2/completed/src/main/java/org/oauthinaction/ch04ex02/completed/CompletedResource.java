package org.oauthinaction.ch04ex02.completed;

import org.oauthinaction.ch04ex02.TokenStore;
import org.oauthinaction.ch04ex02.WebServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class CompletedResource {
    private final TokenStore tokens;
    private final List<String> words = new ArrayList<>();
    public CompletedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource(TokenStore.local())); }

    @GetMapping("/words")
    public synchronized ResponseEntity<?> read(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @RequestParam Map<String, String> parameters) {
        var token = access(auth, parameters);
        if (token == null) return unauthorized();
        if (!allowed(token, "read")) return insufficient("read");
        return ResponseEntity.ok(Map.of("words", String.join(" ", words), "timestamp", System.currentTimeMillis()));
    }
    @PostMapping("/words")
    public synchronized ResponseEntity<?> write(@RequestHeader(value = "Authorization", required = false) String auth,
                                               @RequestParam Map<String, String> parameters) {
        var token = access(auth, parameters);
        if (token == null) return unauthorized();
        if (!allowed(token, "write")) return insufficient("write");
        String word = parameters.get("word");
        if (word != null && !word.isEmpty()) words.add(word);
        return ResponseEntity.status(201).build();
    }
    @DeleteMapping("/words")
    public synchronized ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                                @RequestParam Map<String, String> parameters) {
        var token = access(auth, parameters);
        if (token == null) return unauthorized();
        if (!allowed(token, "delete")) return insufficient("delete");
        if (!words.isEmpty()) words.removeLast();
        return ResponseEntity.noContent().build();
    }
    protected boolean allowed(TokenStore.Token token, String required) { return token.scope().contains(required); }
    private TokenStore.Token access(String auth, Map<String, String> parameters) {
        String value = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)
                ? auth.substring(7) : parameters.get("access_token");
        return tokens.access(value);
    }
    private static ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build(); }
    private static ResponseEntity<?> insufficient(String scope) {
        return ResponseEntity.status(403).header("WWW-Authenticate",
                "Bearer realm=\"localhost:9002\", error=\"insufficient_scope\", scope=\"" + scope + "\"").build();
    }
}
