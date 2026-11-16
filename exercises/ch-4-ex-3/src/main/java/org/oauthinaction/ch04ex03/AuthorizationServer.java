package org.oauthinaction.ch04ex03;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public final class AuthorizationServer {
    private static final String CLIENT_ID = "oauth-client-1";
    private static final String SECRET = "oauth-client-secret-1";
    private static final List<String> SCOPES = List.of("fruit", "veggies", "meats");
    private static final List<String> USERS = List.of("alice", "bob", "carol");
    private final URI redirectUri;
    private final TokenStore tokens;
    private final Map<String, AuthorizationRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, Grant> codes = new ConcurrentHashMap<>();

    public AuthorizationServer(URI redirectUri, TokenStore tokens) {
        this.redirectUri = redirectUri;
        this.tokens = tokens;
    }
    public static void main(String[] args) {
        var tokens = TokenStore.local();
        tokens.clear();
        WebServer.start(9001, "authorization", new AuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("client_id", CLIENT_ID, "redirect_uri", redirectUri.toString(), "scope", SCOPES, "users", USERS);
    }
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam Map<String, String> query) {
        if (!CLIENT_ID.equals(query.get("client_id"))) return error(400, "Unknown client");
        // Validate the destination before using it, including for errors.
        if (!redirectUri.toString().equals(query.get("redirect_uri"))) return error(400, "Invalid redirect URI");
        String state = query.get("state");
        String responseType = query.get("response_type");
        if (!List.of("code", "token").contains(responseType == null ? "" : responseType)) {
            return Forms.redirect(redirectUri, withState("error", "unsupported_response_type", state));
        }
        List<String> scope = TokenStore.scopes(query.get("scope"));
        if (!SCOPES.containsAll(scope)) return Forms.redirect(redirectUri, withState("error", "invalid_scope", state));
        String id = Forms.random();
        requests.put(id, new AuthorizationRequest(responseType, state, scope));
        return Forms.redirect(URI.create("/approve.html"), Map.of("request_id", id));
    }
    @GetMapping("/request")
    public ResponseEntity<?> request(@RequestParam String request_id) {
        var request = requests.get(request_id);
        return request == null ? error(400, "No matching authorization request")
                : ResponseEntity.ok(Map.of("client_id", CLIENT_ID, "scope", request.scope(), "users", USERS));
    }
    @PostMapping(value = "/approve", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> approve(@RequestParam Map<String, String> form) {
        var request = requests.remove(form.getOrDefault("request_id", ""));
        if (request == null) return error(400, "No matching authorization request");
        if (!form.containsKey("approve")) return callback(request, withState("error", "access_denied", request.state()));
        String user = form.getOrDefault("user", "alice");
        if (!USERS.contains(user)) return error(400, "Unknown user");
        List<String> scope = form.keySet().stream().filter(k -> k.startsWith("scope_"))
                .map(k -> k.substring(6)).sorted().toList();
        if (!SCOPES.containsAll(scope)) return callback(request, withState("error", "invalid_scope", request.state()));
        if (request.responseType().equals("token")) {
            var response = issue(CLIENT_ID, scope, user, false);
            if (request.state() != null) response.put("state", request.state());
            return callback(request, response);
        }
        String code = Forms.random();
        codes.put(code, new Grant(CLIENT_ID, scope, user));
        return callback(request, withState("code", code, request.state()));
    }
    @PostMapping(value = "/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> token(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> form) {
        String clientId = authenticate(authorization, form);
        if (clientId == null) return ResponseEntity.status(401)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"token\"").body(Map.of("error", "invalid_client"));
        String type = form.getOrDefault("grant_type", "");
        Map<String, String> response;
        switch (type) {
            case "authorization_code" -> {
                // Consume once. Token-endpoint redirect checking is a later lesson.
                Grant grant = codes.remove(form.getOrDefault("code", ""));
                if (grant == null || !grant.clientId().equals(clientId)) return error(400, "invalid_grant");
                response = issue(clientId, grant.scope(), grant.user(), true);
            }
            case "refresh_token" -> {
                String refreshToken = form.get("refresh_token");
                var stored = tokens.refresh(refreshToken);
                if (stored == null || !stored.clientId().equals(clientId)) return error(400, "invalid_grant");
                response = issue(clientId, stored.scope(), stored.user(), false);
                response.put("refresh_token", refreshToken);
            }
            case "client_credentials" -> {
                var scope = TokenStore.scopes(form.get("scope"));
                if (!SCOPES.containsAll(scope)) return error(400, "invalid_scope");
                response = issue(clientId, scope, null, false);
            }
            case "password" -> {
                // Only Carol has username/password credentials in the book's fixture.
                if (!"clewis".equals(form.get("username")) || !"user password!".equals(form.get("password"))) {
                    return error(400, "invalid_grant");
                }
                var scope = TokenStore.scopes(form.get("scope"));
                if (!SCOPES.containsAll(scope)) return error(400, "invalid_scope");
                response = issue(clientId, scope, "carol", false);
            }
            default -> { return error(400, "unsupported_grant_type"); }
        }
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache").body(response);
    }
    private Map<String, String> issue(String clientId, List<String> scope, String user, boolean refresh) {
        String token = Forms.random();
        tokens.save(token, new TokenStore.Token(clientId, scope, user, false));
        var response = new LinkedHashMap<>(Map.of("access_token", token, "token_type", "Bearer", "scope", String.join(" ", scope)));
        if (refresh) {
            String refreshToken = Forms.random();
            tokens.save(refreshToken, new TokenStore.Token(clientId, scope, user, true));
            response.put("refresh_token", refreshToken);
        }
        return response;
    }
    private String authenticate(String header, Map<String, String> form) {
        String id = form.get("client_id"), secret = form.get("client_secret");
        if (header != null) {
            if (id != null || secret != null || !header.regionMatches(true, 0, "Basic ", 0, 6)) return null;
            try {
                String[] parts = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8).split(":", 2);
                if (parts.length != 2) return null;
                id = Forms.unescape(parts[0]); secret = Forms.unescape(parts[1]);
            } catch (IllegalArgumentException e) { return null; }
        }
        return CLIENT_ID.equals(id) && SECRET.equals(secret) ? id : null;
    }
    private ResponseEntity<?> callback(AuthorizationRequest request, Map<String, String> values) {
        // Fragment parameters belong to the browser and never reach the callback server.
        if (request.responseType().equals("token")) {
            return ResponseEntity.status(302).location(URI.create(redirectUri + "#" + Forms.encode(values))).build();
        }
        return Forms.redirect(redirectUri, values);
    }
    private static Map<String, String> withState(String key, String value, String state) {
        return state == null ? Map.of(key, value) : Map.of(key, value, "state", state);
    }
    private static ResponseEntity<?> error(int status, String error) { return ResponseEntity.status(status).body(Map.of("error", error)); }
    private record AuthorizationRequest(String responseType, String state, List<String> scope) {}
    private record Grant(String clientId, List<String> scope, String user) {}
}
