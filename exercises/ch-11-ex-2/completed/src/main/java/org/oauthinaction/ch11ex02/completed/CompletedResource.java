package org.oauthinaction.ch11ex02.completed;
import org.oauthinaction.ch11ex02.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin
public class CompletedResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new CompletedResource()); }
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String header,
                                      @RequestParam Map<String, String> parameters) {
        String token = header != null && header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7) : parameters.get("access_token");
        if (accepted(token) == null) return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer").build();
        return ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    protected JsonNode accepted(String token) {
        if (token == null) return null;
        try {
            var claims = TokenJwt.parse(token);
            return TokenJwt.validClaims(claims, java.time.Instant.now().getEpochSecond()) ? claims : null;
        } catch (IllegalArgumentException e) { return null; }
    }
}
