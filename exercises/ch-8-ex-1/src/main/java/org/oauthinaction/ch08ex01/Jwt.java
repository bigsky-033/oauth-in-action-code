package org.oauthinaction.ch08ex01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/** JWS framing is visible here; RSA signing/verification uses the JDK primitives. */
public final class Jwt {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private Jwt() {}
    public static String sign(Map<String, Object> claims) {
        try {
            JsonNode key = key();
            String input = ENCODER.encodeToString(JSON.writeValueAsBytes(Map.of("typ", "JWT", "alg", "RS256", "kid", "authserver")))
                    + "." + ENCODER.encodeToString(JSON.writeValueAsBytes(claims));
            var signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(number(key, "n"), number(key, "d"))));
            signer.update(input.getBytes(StandardCharsets.US_ASCII));
            return input + "." + ENCODER.encodeToString(signer.sign());
        } catch (Exception e) { throw new IllegalStateException("Cannot sign teaching ID token", e); }
    }
    public static JsonNode verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Malformed JWT");
            var header = JSON.readTree(DECODER.decode(parts[0]));
            if (!"RS256".equals(header.path("alg").asText())) throw new IllegalArgumentException("Unexpected algorithm");
            JsonNode key = key();
            var verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(number(key, "n"), number(key, "e"))));
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(DECODER.decode(parts[2]))) throw new IllegalArgumentException("Invalid signature");
            return JSON.readTree(DECODER.decode(parts[1]));
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Invalid JWT", e); }
    }
    private static JsonNode key() throws java.io.IOException {
        try (var input = Jwt.class.getResourceAsStream("/keys/ch08ex01/authserver.json")) {
            if (input == null) throw new java.io.IOException("Missing teaching RSA key");
            return JSON.readTree(input);
        }
    }
    private static BigInteger number(JsonNode key, String name) { return new BigInteger(1, DECODER.decode(key.path(name).asText())); }
}
