package org.oauthinaction.ch11ex04.completed;
import org.oauthinaction.ch11ex04.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

@RestController
@CrossOrigin
public class CompletedResource {
    private final URI authorization;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    public CompletedResource(URI authorization) { this.authorization = authorization; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource(URI.create("http://localhost:9001/"))); }
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String header, @RequestParam Map<String, String> parameters) {
        String token = header != null && header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7) : parameters.get("access_token");
        if (token == null || inspect(token) == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        return ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    protected JsonNode inspect(String token) {
        var request = HttpRequest.newBuilder(authorization.resolve("/introspect")).timeout(Duration.ofSeconds(5))
                .header("Authorization", Forms.basic("protected-resource-1", "protected-resource-secret-1"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("token", token)))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var data = json.readTree(response.body()); return data.path("active").asBoolean() ? data : null;
        } catch (java.io.IOException e) { return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }
}
