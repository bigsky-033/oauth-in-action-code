package org.oauthinaction.ch15ex01;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

/** The book's PoP request format, using JDK RSA primitives and explicit JWS framing. */
public final class Proof {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private Proof() {}
    public static Map<String, Object> generateKey() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
            var pair = generator.generateKeyPair(); var pub = (RSAPublicKey) pair.getPublic(); var privateKey = (RSAPrivateKey) pair.getPrivate();
            return Map.of("kty", "RSA", "kid", Forms.random(), "n", number(pub.getModulus()), "e", number(pub.getPublicExponent()), "d", number(privateKey.getPrivateExponent()));
        } catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    public static Map<String, Object> publicKey(Map<String, Object> key) {
        return Map.of("kty", key.get("kty"), "kid", key.get("kid"), "n", key.get("n"), "e", key.get("e"));
    }
    public static String sign(Map<String, Object> key, Map<String, Object> claims) {
        try {
            String input = ENCODER.encodeToString(JSON.writeValueAsBytes(Map.of("typ", "PoP", "alg", "RS256", "kid", key.get("kid")))) + "." + ENCODER.encodeToString(JSON.writeValueAsBytes(claims));
            var signer = Signature.getInstance("SHA256withRSA"); signer.initSign(KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(integer(key, "n"), integer(key, "d"))));
            signer.update(input.getBytes(StandardCharsets.US_ASCII)); return input + "." + ENCODER.encodeToString(signer.sign());
        } catch (Exception e) { throw new IllegalArgumentException("Cannot sign proof", e); }
    }
    public static JsonNode payload(String proof) {
        try {
            String[] parts = proof.split("\\.", -1); if (parts.length != 3) throw new IllegalArgumentException("Malformed proof");
            return JSON.readTree(DECODER.decode(parts[1]));
        } catch (Exception e) { throw new IllegalArgumentException("Malformed proof", e); }
    }
    public static boolean verify(String proof, Map<String, Object> key) {
        try {
            String[] parts = proof.split("\\.", -1); if (parts.length != 3) return false;
            var header = JSON.readTree(DECODER.decode(parts[0])); if (!"RS256".equals(header.path("alg").asText()) || !"PoP".equals(header.path("typ").asText())) return false;
            var verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(integer(key, "n"), integer(key, "e"))));
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII)); return verifier.verify(DECODER.decode(parts[2]));
        } catch (Exception e) { return false; }
    }
    private static BigInteger integer(Map<String, Object> key, String field) { return new BigInteger(1, DECODER.decode((String) key.get(field))); }
    private static String number(BigInteger value) {
        byte[] bytes = value.toByteArray(); if (bytes.length > 1 && bytes[0] == 0) bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        return ENCODER.encodeToString(bytes);
    }
}
