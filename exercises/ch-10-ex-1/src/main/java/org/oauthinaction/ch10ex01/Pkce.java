package org.oauthinaction.ch10ex01;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
public final class Pkce {
    private Pkce() {}
    public static String challenge(String verifier) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("JDK must provide SHA-256", e); }
    }
}
