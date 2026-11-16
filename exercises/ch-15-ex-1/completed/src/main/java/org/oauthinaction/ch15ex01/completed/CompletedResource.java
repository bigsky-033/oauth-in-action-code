package org.oauthinaction.ch15ex01.completed;
import org.oauthinaction.ch15ex01.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> resource(HttpServletRequest request, @RequestHeader(value = "Authorization", required = false) String header,
                                      @RequestParam Map<String, String> parameters) {
        String proof = header != null && header.regionMatches(true, 0, "PoP ", 0, 4) ? header.substring(4) : parameters.get("pop_access_token");
        if (proof == null || !accepted(proof, request)) return ResponseEntity.status(401).header("WWW-Authenticate", "PoP").build();
        return ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    protected boolean accepted(String proof, HttpServletRequest incoming) {
        try {
            // Unverified payload is used ONLY to locate the bound public key.
            var payload = Proof.payload(proof); String access = payload.path("at").asText();
            if (access.isBlank()) return false;
            var request = HttpRequest.newBuilder(authorization.resolve("/introspect")).timeout(Duration.ofSeconds(5))
                    .header("Authorization", Forms.basic("protected-resource-1", "protected-resource-secret-1"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("token", access)))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString()); if (response.statusCode() != 200) return false;
            var metadata = json.readTree(response.body()); if (!metadata.path("active").asBoolean()) return false;
            var key = json.convertValue(metadata.path("access_token_key"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if (!Proof.verify(proof, key)) return false;
            String authority = incoming.getServerName() + ":" + incoming.getServerPort();
            return (!payload.has("m") || payload.path("m").asText().equals(incoming.getMethod()))
                    && (!payload.has("u") || payload.path("u").asText().equals(authority))
                    && (!payload.has("p") || payload.path("p").asText().equals(incoming.getRequestURI()));
        } catch (java.io.IOException | IllegalArgumentException e) { return false; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
}
