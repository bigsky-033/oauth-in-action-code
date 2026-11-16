package org.oauthinaction.ch12ex01.completed;
import org.oauthinaction.ch12ex01.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CompletedAuthorizationServer {
    private static final String ISSUER = "http://localhost:9001/";
    private final TokenStore tokens;
    private final Map<String, Map<String, Object>> clients = new ConcurrentHashMap<>();
    private final Map<String, Pending> requests = new ConcurrentHashMap<>();
    private final Map<String, Grant> codes = new ConcurrentHashMap<>();
    public CompletedAuthorizationServer(URI callback, TokenStore tokens) {
        this.tokens = tokens;

    }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new CompletedAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @GetMapping("/status") public Map<String, Object> status() { return Map.of("clients", clients.values()); }
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam Map<String, String> query) {
        var client = clients.get(query.getOrDefault("client_id", ""));
        if (client == null) return error(400, "Unknown client");
        String redirect = query.get("redirect_uri");
        if (redirect == null || !strings(client.get("redirect_uris")).contains(redirect)) return error(400, "Invalid redirect URI");
        String type = query.getOrDefault("response_type", "");
        var pending = new Pending((String) client.get("client_id"), URI.create(redirect), type, query.get("state"),
                TokenStore.scopes(query.get("scope")), query.get("nonce"));
        if (!strings(client.get("response_types")).contains(type)) return callback(pending, withState("error", "unsupported_response_type", pending.state()));
        if (!TokenStore.scopes((String) client.get("scope")).containsAll(pending.scope())) return callback(pending, withState("error", "invalid_scope", pending.state()));
        String id = Forms.random(); requests.put(id, pending);
        return Forms.redirect(URI.create("/approve.html"), Map.of("request_id", id));
    }
    @GetMapping("/request")
    public ResponseEntity<?> request(@RequestParam String request_id) {
        var pending = requests.get(request_id);
        return pending == null ? error(400, "No matching authorization request") : ResponseEntity.ok(Map.of(
                "client_id", pending.clientId(), "scope", pending.scope()));
    }
    @PostMapping(value = "/approve", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> approve(@RequestParam Map<String, String> form) {
        var pending = requests.remove(form.getOrDefault("request_id", ""));
        if (pending == null) return error(400, "No matching authorization request");
        if (!form.containsKey("approve")) return callback(pending, withState("error", "access_denied", pending.state()));
        String user = form.getOrDefault("user", "alice");
        var scope = form.keySet().stream().filter(k -> k.startsWith("scope_")).map(k -> k.substring(6)).sorted().toList();
        var client = clients.get(pending.clientId());
        if (client == null) return error(400, "Unknown client");
        if (!TokenStore.scopes((String) client.get("scope")).containsAll(scope)) return callback(pending, withState("error", "invalid_scope", pending.state()));
        String code = Forms.random(); codes.put(code, new Grant(pending.clientId(), scope, user, pending.nonce()));
        return callback(pending, withState("code", code, pending.state()));
    }
    @PostMapping(value = "/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> token(@RequestHeader(value = "Authorization", required = false) String header,
                                   @RequestParam Map<String, String> form) {
        String clientId = authenticate(header, form);
        if (clientId == null) return invalidClient();
        var client = clients.get(clientId);
        String type = form.getOrDefault("grant_type", "");
        if (!strings(client.get("grant_types")).contains(type)) return error(400, "unsupported_grant_type");
        Map<String, String> response;
        switch (type) {
            case "authorization_code" -> {
                var grant = codes.remove(form.getOrDefault("code", ""));
                if (grant == null || !clientId.equals(grant.clientId())) return error(400, "invalid_grant");
                // The book introduces token-endpoint redirect binding in a later lesson.
                response = issue(clientId, grant.scope(), grant.user(), grant.nonce(), true);
            }
            case "refresh_token" -> {
                var grant = tokens.refresh(form.get("refresh_token"));
                if (grant == null || !clientId.equals(grant.clientId())) return error(400, "invalid_grant");
                var scope = form.containsKey("scope") ? TokenStore.scopes(form.get("scope")) : grant.scope();
                if (!grant.scope().containsAll(scope)) return error(400, "invalid_scope");
                response = issue(clientId, scope, grant.user(), null, false);
                response.put("refresh_token", form.get("refresh_token"));
            }
            default -> { return error(400, "unsupported_grant_type"); }
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache").body(response);
    }
    private Map<String, String> issue(String clientId, List<String> scope, String user, String nonce, boolean refresh) {
        String access = Forms.random(); tokens.save(access, new TokenStore.Token(clientId, scope, null, false));
        var response = new LinkedHashMap<>(Map.of("access_token", access, "token_type", "Bearer", "scope", String.join(" ", scope)));
        if (refresh) {
            String value = Forms.random(); tokens.save(value, new TokenStore.Token(clientId, scope, null, true)); response.put("refresh_token", value);
        }
        return response;
    }
    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> metadata) {
        String error = metadataError(metadata); if (error != null) return error(400, error);
        String id = Forms.random(); var registration = normalize(metadata);
        registration.put("client_id", id); registration.put("client_id_created_at", Instant.now().getEpochSecond());
        registration.put("client_secret_expires_at", 0);
        if (!registration.get("token_endpoint_auth_method").equals("none")) registration.put("client_secret", Forms.random());
        clients.put(id, registration); return ResponseEntity.status(201).body(registration);
    }
    private static String metadataError(Map<String, Object> metadata) {
        for (String field : List.of("scope", "client_name", "client_uri", "logo_uri", "token_endpoint_auth_method")) {
            if (metadata.containsKey(field) && !(metadata.get(field) instanceof String)) return "invalid_client_metadata";
        }
        for (String field : List.of("grant_types", "response_types")) {
            if (metadata.containsKey(field) && (!(metadata.get(field) instanceof List<?> values)
                    || values.stream().anyMatch(value -> !(value instanceof String)))) return "invalid_client_metadata";
        }
        if (!(metadata.get("redirect_uris") instanceof List<?> uris) || uris.isEmpty()) return "invalid_redirect_uri";
        for (Object uri : uris) {
            try { if (!(uri instanceof String text) || !URI.create(text).isAbsolute() || URI.create(text).getFragment() != null) return "invalid_redirect_uri"; }
            catch (IllegalArgumentException e) { return "invalid_redirect_uri"; }
        }
        String method = String.valueOf(metadata.getOrDefault("token_endpoint_auth_method", "client_secret_basic"));
        if (!List.of("client_secret_basic", "client_secret_post", "none", "secret_basic", "secret_post").contains(method)) return "invalid_client_metadata";
        for (String field : List.of("grant_types", "response_types")) {
            if (metadata.containsKey(field) && !(metadata.get(field) instanceof List<?>)) return "invalid_client_metadata";
        }
        if (!List.of("authorization_code", "refresh_token").containsAll(strings(metadata.getOrDefault("grant_types", List.of("authorization_code"))))) return "invalid_client_metadata";
        if (!List.of("code").containsAll(strings(metadata.getOrDefault("response_types", List.of("code"))))) return "invalid_client_metadata";
        return null;
    }
    private static Map<String, Object> normalize(Map<String, Object> metadata) {
        var result = new LinkedHashMap<String, Object>();
        for (String field : List.of("redirect_uris", "client_name", "client_uri", "logo_uri", "scope")) if (metadata.containsKey(field)) result.put(field, metadata.get(field));
        result.putIfAbsent("scope", "");
        String method = String.valueOf(metadata.getOrDefault("token_endpoint_auth_method", "client_secret_basic"));
        result.put("token_endpoint_auth_method", method.equals("secret_basic") ? "client_secret_basic" : method.equals("secret_post") ? "client_secret_post" : method);
        var grants = new ArrayList<>(strings(metadata.getOrDefault("grant_types", List.of("authorization_code"))));
        var responses = new ArrayList<>(strings(metadata.getOrDefault("response_types", grants.contains("authorization_code") ? List.of("code") : List.of())));
        if (responses.contains("code") && !grants.contains("authorization_code")) grants.add("authorization_code");
        if (grants.contains("authorization_code") && !responses.contains("code")) responses.add("code");
        result.put("grant_types", grants); result.put("response_types", responses); return result;
    }
    private String authenticate(String header, Map<String, String> form) {
        String id = form.get("client_id"), secret = form.get("client_secret");
        if (header != null) {
            if (id != null || secret != null || !header.regionMatches(true, 0, "Basic ", 0, 6)) return null;
            try {
                String[] pair = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8).split(":", 2);
                if (pair.length != 2) return null; id = Forms.unescape(pair[0]); secret = Forms.unescape(pair[1]);
            } catch (IllegalArgumentException e) { return null; }
        }
        var client = id == null ? null : clients.get(id); if (client == null) return null;
        if (client.get("token_endpoint_auth_method").equals("none")) return secret == null && header == null ? id : null;
        return Objects.equals(secret, client.get("client_secret")) ? id : null;
    }
    private static List<String> strings(Object value) {
        return value instanceof List<?> list ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList() : List.of();
    }
    private ResponseEntity<?> callback(Pending pending, Map<String, String> values) {
        return pending.type().equals("token") ? ResponseEntity.status(302).location(URI.create(pending.redirect() + "#" + Forms.encode(values))).build()
                : Forms.redirect(pending.redirect(), values);
    }
    private static Map<String, String> withState(String key, String value, String state) { return state == null ? Map.of(key, value) : Map.of(key, value, "state", state); }
    private static ResponseEntity<?> invalidClient() { return ResponseEntity.status(401).header("WWW-Authenticate", "Basic realm=\"oauth\"").body(Map.of("error", "invalid_client")); }
    private static ResponseEntity<?> error(int status, String message) { return ResponseEntity.status(status).body(Map.of("error", message)); }
    private record Pending(String clientId, URI redirect, String type, String state, List<String> scope, String nonce) {}
    private record Grant(String clientId, List<String> scope, String user, String nonce) {}
}
