package org.oauthinaction.ch09ex02;

import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Forms {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Forms() {}

    public static String encode(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> escape(entry.getKey()) + "=" + escape(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static Map<String, String> query(URI uri) {
        var values = new LinkedHashMap<String, String>();
        if (uri.getRawQuery() != null) {
            for (String part : uri.getRawQuery().split("&")) {
                String[] pair = part.split("=", 2);
                values.put(unescape(pair[0]), pair.length == 2 ? unescape(pair[1]) : "");
            }
        }
        return values;
    }

    public static String basic(String clientId, String secret) {
        // OAuth applies form encoding to EACH credential before constructing Basic.
        String credentials = escape(clientId) + ":" + escape(secret);
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public static String unescape(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static String random() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static ResponseEntity<Void> redirect(URI target, Map<String, String> query) {
        String separator = target.getRawQuery() == null ? "?" : "&";
        URI location = query.isEmpty() ? target : URI.create(target + separator + encode(query));
        return ResponseEntity.status(302).location(location).build();
    }

    private static String escape(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
