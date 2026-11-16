package org.oauthinaction.classroomsnippets;
import com.fasterxml.jackson.databind.JsonNode;
import org.oauthinaction.classroomcomplete.Jwt;
import java.time.Instant;
import java.util.*;
/** Snippet 7: an ID token's audience is the CLIENT, not the resource server. */
public final class IdTokenExample {
    public static String issue(Set<String> scopes, String subject, String nonce) {
        if (!scopes.contains("openid")) return null;
        long now = Instant.now().getEpochSecond();
        var claims = new LinkedHashMap<String, Object>(Map.of("iss", "http://localhost:9001/", "sub", subject,
                "aud", ClientServerInfo.CLIENT_ID, "iat", now, "exp", now + 300));
        if (nonce != null) claims.put("nonce", nonce);
        return Jwt.sign(claims);
    }
    public static JsonNode validate(String token, String expectedNonce) {
        JsonNode claims = Jwt.verify(token); // Fixed RS256 algorithm, not the untrusted header's choice.
        var audience = claims.path("aud");
        boolean matches = audience.isTextual() && ClientServerInfo.CLIENT_ID.equals(audience.asText());
        if (audience.isArray()) for (var value : audience) matches |= ClientServerInfo.CLIENT_ID.equals(value.asText());
        long now = Instant.now().getEpochSecond();
        if (!"http://localhost:9001/".equals(claims.path("iss").asText()) || !matches
                || !claims.path("iat").isIntegralNumber() || !claims.path("exp").isIntegralNumber()
                || claims.path("iat").asLong() > now || claims.path("exp").asLong() < now)
            throw new IllegalArgumentException("Invalid issuer, audience or times");
        if (expectedNonce != null && !expectedNonce.equals(claims.path("nonce").asText())) throw new IllegalArgumentException("Invalid nonce");
        return claims;
    }
}
