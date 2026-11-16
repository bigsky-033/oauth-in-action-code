package org.oauthinaction.ch11ex02;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HS256 framing and verification; HMAC itself is supplied by the JDK. */
public final class TokenJwt {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SECRET = "shared token secret!";
    private TokenJwt() {}
    public static String create(Map<String, Object> claims) {
        String input = input(Map.of("typ", "JWT", "alg", "HS256"), claims);
        return input + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature(input));
    }
    public static String unsigned(Map<String, Object> claims) { return input(Map.of("typ", "JWT", "alg", "none"), claims) + "."; }
    public static JsonNode parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Malformed JWT");
            var decoder = Base64.getUrlDecoder();
            if (!"HS256".equals(JSON.readTree(decoder.decode(parts[0])).path("alg").asText())) throw new IllegalArgumentException("Expected HS256");
            byte[] expected = signature(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, decoder.decode(parts[2]))) throw new IllegalArgumentException("Invalid HMAC");
            return parseWithoutVerification(token);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid signed JWT", e); }
    }
    public static JsonNode parseWithoutVerification(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Malformed JWT");
            return JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception e) { throw new IllegalArgumentException("Malformed JWT", e); }
    }
    private static String input(Map<String, Object> header, Map<String, Object> claims) {
        try {
            var encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(JSON.writeValueAsBytes(header)) + "." + encoder.encodeToString(JSON.writeValueAsBytes(claims));
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Cannot encode JWT", e); }
    }
    private static byte[] signature(String input) {
        try {
            var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("JDK must provide HmacSHA256", e); }
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
