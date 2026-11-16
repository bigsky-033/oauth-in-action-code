package org.oauthinaction.ch08ex03;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public final class AuthorizationServer {
    private static final String ISSUER = "http://localhost:9001/";
    private final TokenStore tokens;
    private final Map<String, Map<String, Object>> clients = new ConcurrentHashMap<>();
    private final Map<String, Pending> requests = new ConcurrentHashMap<>();
    private final Map<String, Grant> codes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> users = Map.of(
            "alice", Map.of("sub", "9XE3-JI34-00132A", "preferred_username", "alice", "name", "Alice", "email", "alice.wonderland@example.com", "email_verified", true),
            "bob", Map.of("sub", "1ZT5-OE63-57383B", "preferred_username", "bob", "name", "Bob", "email", "bob.loblob@example.net", "email_verified", false),
            "carol", Map.of("sub", "F5Q1-L6LGG-959FS", "preferred_username", "carol", "name", "Carol", "email", "carol.lewis@example.net", "email_verified", true));

    public AuthorizationServer(URI callback, TokenStore tokens) {
        this.tokens = tokens;
        seed("oauth-client-1", "oauth-client-secret-1", callback, "greeting");
        seed("oauth-client-2", "oauth-client-secret-1", callback, "bar");
        seed("native-client-1", "oauth-native-secret-1", callback, "openid profile email phone address");
    }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new AuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    private void seed(String id, String secret, URI callback, String scope) {
        clients.put(id, new LinkedHashMap<>(Map.of("client_id", id, "client_secret", secret,
                "redirect_uris", List.of(callback.toString()), "scope", scope, "token_endpoint_auth_method", "client_secret_basic",
                "grant_types", List.of("authorization_code", "implicit", "refresh_token", "client_credentials", "password"), "response_types", List.of("code", "token"))));
    }
    @GetMapping("/status") public Map<String, Object> status() { return Map.of("clients", clients.values(), "users", users.keySet()); }
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
                "client_id", pending.clientId(), "scope", pending.scope(), "users", users.keySet()));
    }
    @PostMapping(value = "/approve", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> approve(@RequestParam Map<String, String> form) {
        var pending = requests.remove(form.getOrDefault("request_id", ""));
        if (pending == null) return error(400, "No matching authorization request");
        if (!form.containsKey("approve")) return callback(pending, withState("error", "access_denied", pending.state()));
        String user = form.getOrDefault("user", "alice");
        if (!users.containsKey(user)) return error(400, "Unknown user");
        var scope = form.keySet().stream().filter(k -> k.startsWith("scope_")).map(k -> k.substring(6)).sorted().toList();
        var client = clients.get(pending.clientId());
        if (client == null) return error(400, "Unknown client");
        if (!TokenStore.scopes((String) client.get("scope")).containsAll(scope)) return callback(pending, withState("error", "invalid_scope", pending.state()));
        if (pending.type().equals("token")) {
            var response = issue(pending.clientId(), scope, user, pending.nonce(), false);
            if (pending.state() != null) response.put("state", pending.state());
            return callback(pending, response);
        }
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
            case "client_credentials" -> {
                var scope = TokenStore.scopes(form.get("scope"));
                if (!TokenStore.scopes((String) client.get("scope")).containsAll(scope)) return error(400, "invalid_scope");
                response = issue(clientId, scope, null, null, false);
            }
            case "password" -> {
                if (!"clewis".equals(form.get("username")) || !"user password!".equals(form.get("password"))) return error(401, "invalid_grant");
                var scope = TokenStore.scopes(form.get("scope"));
                if (!TokenStore.scopes((String) client.get("scope")).containsAll(scope)) return error(400, "invalid_scope");
                response = issue(clientId, scope, "carol", null, false);
            }
            default -> { return error(400, "unsupported_grant_type"); }
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache").body(response);
    }
    private Map<String, String> issue(String clientId, List<String> scope, String user, String nonce, boolean refresh) {
        String access = Forms.random(); tokens.save(access, new TokenStore.Token(clientId, scope, user, false));
        var response = new LinkedHashMap<>(Map.of("access_token", access, "token_type", "Bearer", "scope", String.join(" ", scope)));
        if (refresh) {
            String value = Forms.random(); tokens.save(value, new TokenStore.Token(clientId, scope, user, true)); response.put("refresh_token", value);
        }
        // The source emits an ID token whenever it has a user; access tokens stay opaque.
        if (user != null) {
            long now = Instant.now().getEpochSecond();
            var claims = new LinkedHashMap<String, Object>(Map.of("iss", ISSUER, "sub", users.get(user).get("sub"), "aud", clientId, "iat", now, "exp", now + 300));
            if (nonce != null && !nonce.isBlank()) claims.put("nonce", nonce);
            response.put("id_token", Jwt.sign(claims));
        }
        return response;
    }
    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestHeader(value = "Authorization", required = false) String header, @RequestParam Map<String, String> form) {
        String clientId = authenticate(header, form); if (clientId == null) return invalidClient();
        tokens.revoke(form.get("token"), clientId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/introspect")
    public ResponseEntity<?> introspect(@RequestHeader(value = "Authorization", required = false) String header, @RequestParam Map<String, String> form) {
        if (!Forms.basic("protected-resource-1", "protected-resource-secret-1").equals(header)) return invalidClient();
        var token = tokens.access(form.get("token"));
        if (token == null) return ResponseEntity.ok(Map.of("active", false));
        var result = new LinkedHashMap<String, Object>(Map.of("active", true, "iss", ISSUER, "scope", String.join(" ", token.scope()), "client_id", token.clientId()));
        if (token.user() != null) { result.put("sub", users.get(token.user()).get("sub")); result.put("username", token.user()); }
        return ResponseEntity.ok(result);
    }
    @RequestMapping(value = "/userinfo", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> userInfo(@RequestHeader(value = "Authorization", required = false) String header, @RequestParam Map<String, String> parameters) {
        String value = header != null && header.startsWith("Bearer ") ? header.substring(7) : parameters.get("access_token");
        var token = tokens.access(value);
        if (token == null) return ResponseEntity.status(401).build();
        if (!token.scope().contains("openid")) return ResponseEntity.status(403).build();
        var user = token.user() == null ? null : users.get(token.user());
        if (user == null) return ResponseEntity.notFound().build();
        var claims = new LinkedHashMap<String, Object>(); claims.put("sub", user.get("sub"));
        if (token.scope().contains("profile")) for (String key : List.of("name", "preferred_username")) claims.put(key, user.get(key));
        if (token.scope().contains("email")) for (String key : List.of("email", "email_verified")) claims.put(key, user.get(key));
        // No phone/address fixture exists, so those scopes add no fabricated claims.
        return ResponseEntity.ok(claims);
    }
    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> metadata) {
        String error = metadataError(metadata); if (error != null) return error(400, error);
        String id = Forms.random(); var registration = normalize(metadata);
        registration.put("client_id", id); registration.put("client_id_created_at", Instant.now().getEpochSecond());
        registration.put("client_secret_expires_at", 0); registration.put("registration_access_token", Forms.random());
        registration.put("registration_client_uri", org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUriString());
        if (!registration.get("token_endpoint_auth_method").equals("none")) registration.put("client_secret", Forms.random());
        clients.put(id, registration); return ResponseEntity.status(201).body(registration);
    }
    @GetMapping("/register/{id}")
    public ResponseEntity<?> readRegistration(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String auth) {
        int status = managementStatus(id, auth); return status == 200 ? ResponseEntity.ok(clients.get(id)) : ResponseEntity.status(status).build();
    }
    @PutMapping(value = "/register/{id}", consumes = "application/json")
    public ResponseEntity<?> updateRegistration(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String auth,
                                                @RequestBody Map<String, Object> metadata) {
        int status = managementStatus(id, auth); if (status != 200) return ResponseEntity.status(status).build();
        var current = clients.get(id);
        if (!id.equals(metadata.get("client_id")) || metadata.containsKey("client_secret") && !Objects.equals(metadata.get("client_secret"), current.get("client_secret"))) return error(400, "invalid_client_metadata");
        String error = metadataError(metadata); if (error != null) return error(400, error);
        var updated = normalize(metadata);
        for (String field : List.of("client_id", "client_secret", "client_id_created_at", "client_secret_expires_at", "registration_access_token", "registration_client_uri")) {
            if (current.containsKey(field)) updated.put(field, current.get(field));
        }
        clients.put(id, updated); return ResponseEntity.ok(updated);
    }
    @DeleteMapping("/register/{id}")
    public ResponseEntity<?> deleteRegistration(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String auth) {
        int status = managementStatus(id, auth); if (status != 200) return ResponseEntity.status(status).build();
        clients.remove(id); tokens.removeClient(id);
        return ResponseEntity.noContent().build();
    }
    private int managementStatus(String id, String auth) {
        var client = clients.get(id); if (client == null) return 404;
        if (auth == null || !auth.startsWith("Bearer ")) return 401;
        return auth.substring(7).equals(client.get("registration_access_token")) ? 200 : 403;
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
