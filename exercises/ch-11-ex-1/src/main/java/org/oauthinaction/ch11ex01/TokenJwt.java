package org.oauthinaction.ch11ex01;
import com.fasterxml.jackson.databind.*;
import java.util.*;

/** Unsigned JWTs are deliberately forgeable: this first lesson teaches format/claims. */
public final class TokenJwt {
    private static final ObjectMapper JSON = new ObjectMapper();
    private TokenJwt() {}
    public static String create(Map<String, Object> claims) {
        try {
            var encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(JSON.writeValueAsBytes(Map.of("typ", "JWT", "alg", "none"))) + "."
                    + encoder.encodeToString(JSON.writeValueAsBytes(claims)) + ".";
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Cannot encode JWT", e); }
    }
    public static JsonNode parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !parts[2].isEmpty()) throw new IllegalArgumentException("Expected unsigned JWT");
            var decoder = Base64.getUrlDecoder();
            if (!"none".equals(JSON.readTree(decoder.decode(parts[0])).path("alg").asText())) throw new IllegalArgumentException("Expected alg=none");
            return JSON.readTree(decoder.decode(parts[1]));
        } catch (Exception e) { throw new IllegalArgumentException("Malformed unsigned JWT", e); }
    }
    public static boolean validClaims(JsonNode claims, long now) {
        if (!"http://localhost:9001/".equals(claims.path("iss").asText())) return false;
        var audience = claims.path("aud");
        boolean accepted = audience.isTextual() && audience.asText().equals("http://localhost:9002/");
        if (audience.isArray()) for (var entry : audience) accepted |= entry.asText().equals("http://localhost:9002/");
        return accepted && claims.path("iat").isIntegralNumber() && claims.path("exp").isIntegralNumber()
                && claims.path("iat").asLong() <= now && claims.path("exp").asLong() >= now;
    }
}
