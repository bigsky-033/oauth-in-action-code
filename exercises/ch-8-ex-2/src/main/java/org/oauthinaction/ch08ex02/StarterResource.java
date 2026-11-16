package org.oauthinaction.ch08ex02;
import org.oauthinaction.ch08ex02.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class StarterResource {
    private final TokenStore tokens;
    public StarterResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @GetMapping("/helloWorld")
    public ResponseEntity<?> hello(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestParam Map<String, String> parameters) {
        String token = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7) : parameters.get("access_token");
        if (tokens.access(token) == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        return response(parameters.getOrDefault("language", ""));
    }
    protected ResponseEntity<?> response(String language) {
        String greeting = greetings().get(language);
        if (greeting == null) greeting = "Error, invalid language: " + Forms.encode(Map.of("language", language)).substring("language=".length());
        return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff").header("X-XSS-Protection", "1; mode=block")
                .body(Map.of("greeting", greeting));
    }
    protected static Map<String, String> greetings() { return Map.of("en", "Hello World", "de", "Hallo Welt", "it", "Ciao Mondo", "fr", "Bonjour monde", "es", "Hola mundo"); }
}
