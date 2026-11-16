package org.oauthinaction.ch07ex01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@RestController
@CrossOrigin
public final class ProtectedResource {
    private final URI authorization;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final List<String> words = new ArrayList<>();
    public ProtectedResource(URI authorization) { this.authorization = authorization; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource(URI.create("http://localhost:9001/"))); }

    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = inspect(auth, parameters);
        return token == null ? unauthorized() : ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    @RequestMapping(value = "/words", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
    public synchronized ResponseEntity<?> words(HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = inspect(auth, parameters); if (token == null) return unauthorized();
        String required = switch (request.getMethod()) { case "GET" -> "read"; case "POST" -> "write"; default -> "delete"; };
        if (!scopes(token).contains(required)) return ResponseEntity.status(403).header("WWW-Authenticate", "Bearer error=\"insufficient_scope\", scope=\"" + required + "\"").build();
        switch (request.getMethod()) {
            case "GET": return ResponseEntity.ok(Map.of("words", String.join(" ", words), "timestamp", System.currentTimeMillis()));
            case "POST":
                if (!parameters.getOrDefault("word", "").isEmpty()) words.add(parameters.get("word"));
                return ResponseEntity.status(201).build();
            default:
                if (!words.isEmpty()) words.removeLast();
                return ResponseEntity.noContent().build();
        }
    }
    @GetMapping("/produce")
    public ResponseEntity<?> produce(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = inspect(auth, parameters); if (token == null) return unauthorized();
        var scope = scopes(token);
        return ResponseEntity.ok(Map.of("fruit", scope.contains("fruit") ? List.of("apple", "banana", "kiwi") : List.of(),
                "veggies", scope.contains("veggies") ? List.of("lettuce", "onion", "potato") : List.of(),
                "meats", scope.contains("meats") ? List.of("bacon", "steak", "chicken breast") : List.of()));
    }
    @GetMapping("/favorites")
    public ResponseEntity<?> favorites(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = inspect(auth, parameters); if (token == null) return unauthorized();
        return ResponseEntity.ok(forUser(token.path("username").asText()));
    }
    private Map<String, Object> forUser(String user) {
        if ("alice".equals(user)) return Map.of("user", "Alice", "favorites", Map.of(
                "movies", List.of("The Multidmensional Vector", "Space Fights", "Jewelry Boss"), "foods", List.of("bacon", "pizza", "bacon pizza"), "music", List.of("techno", "industrial", "alternative")));
        if ("bob".equals(user)) return Map.of("user", "Bob", "favorites", Map.of(
                "movies", List.of("An Unrequited Love", "Several Shades of Turquoise", "Think Of The Children"), "foods", List.of("bacon", "kale", "gravel"), "music", List.of("baroque", "ukulele", "baroque ukulele")));
        return Map.of("user", "Unknown", "favorites", Map.of("movies", List.of(), "foods", List.of(), "music", List.of()));
    }
    private JsonNode inspect(String auth, Map<String, String> parameters) {
        String token = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7) : parameters.get("access_token");
        if (token == null || token.isBlank()) return null;
        // This resource has no access to token storage: it asks the authorization server over HTTP.
        var request = HttpRequest.newBuilder(authorization.resolve("/introspect")).timeout(Duration.ofSeconds(5))
                .header("Authorization", Forms.basic("protected-resource-1", "protected-resource-secret-1"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("token", token)))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var body = json.readTree(response.body()); return body.path("active").asBoolean() ? body : null;
        } catch (java.io.IOException e) { return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }
    private static List<String> scopes(JsonNode token) { return TokenStore.scopes(token.path("scope").asText()); }
    private static ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build(); }
}
