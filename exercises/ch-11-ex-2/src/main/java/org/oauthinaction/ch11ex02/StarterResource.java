package org.oauthinaction.ch11ex02;
import org.oauthinaction.ch11ex02.completed.CompletedResource;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RestController;
@RestController
public final class StarterResource extends CompletedResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource()); }
    @Override protected JsonNode accepted(String token) {
        // TODO: verify the HS256 signature BEFORE accepting these claims.
        if (token == null) return null;
        try {
            var claims = TokenJwt.parseWithoutVerification(token);
            return TokenJwt.validClaims(claims, java.time.Instant.now().getEpochSecond()) ? claims : null;
        } catch (IllegalArgumentException e) { return null; }
    }
}
