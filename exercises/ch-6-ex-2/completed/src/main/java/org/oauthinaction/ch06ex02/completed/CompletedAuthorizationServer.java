package org.oauthinaction.ch06ex02.completed;

import org.oauthinaction.ch06ex02.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CompletedAuthorizationServer {
    private static final String CLIENT_ID = "oauth-client-1";
    private static final String CLIENT_SECRET = "oauth-client-secret-1";
    private static final List<String> SCOPES = List.of("foo", "bar");
    private final URI redirectUri;
    private final TokenStore tokens;
    private final Map<String, AuthorizationRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, Grant> codes = new ConcurrentHashMap<>();

    public CompletedAuthorizationServer(URI redirectUri, TokenStore tokens) {
        this.redirectUri = redirectUri;
        this.tokens = tokens;
    }

    public static void main(String[] args) {
        var tokens = TokenStore.local();
        tokens.clear();
        WebServer.start(9001, "authorization",
                new CompletedAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("client_id", CLIENT_ID, "redirect_uri", redirectUri.toString(), "scope", SCOPES);
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam Map<String, String> query) {
        if (!CLIENT_ID.equals(query.get("client_id"))) return error(400, "Unknown client");
        // Never send a browser to an unregistered URI, including on errors.
        if (!redirectUri.toString().equals(query.get("redirect_uri"))) return error(400, "Invalid redirect URI");

        String state = query.get("state");
        if (!"code".equals(query.get("response_type"))) return authorizationError("unsupported_response_type", state);
        List<String> requestedScopes = scopes(query.get("scope"));
        if (!validScopes(requestedScopes)) return authorizationError("invalid_scope", state);

        String requestId = Forms.random();
        requests.put(requestId, new AuthorizationRequest(state, requestedScopes));
        return Forms.redirect(URI.create("/approve.html"), Map.of("request_id", requestId));
    }

    @GetMapping("/request")
    public ResponseEntity<?> request(@RequestParam String request_id) {
        AuthorizationRequest request = requests.get(request_id);
        if (request == null) return error(400, "No matching authorization request");
        return ResponseEntity.ok(Map.of("client_id", CLIENT_ID, "scope", request.scopes()));
    }

    @PostMapping(value = "/approve", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> approve(@RequestParam Map<String, String> form) {
        AuthorizationRequest request = requests.remove(form.getOrDefault("request_id", ""));
        if (request == null) return error(400, "No matching authorization request");
        if (!form.containsKey("approve")) return authorizationError("access_denied", request.state());

        List<String> grantedScopes = grantedScopes(form);
        if (!validScopes(grantedScopes)) return authorizationError("invalid_scope", request.state());

        String code = Forms.random();
        codes.put(code, new Grant(CLIENT_ID, String.join(" ", grantedScopes)));
        return Forms.redirect(redirectUri, withState("code", code, request.state()));
    }

    @PostMapping(value = "/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> token(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> form) {
        String clientId = form.get("client_id");
        String secret = form.get("client_secret");
        if (authorization != null) {
            if (clientId != null || secret != null) return invalidClient();
            try {
                if (!authorization.regionMatches(true, 0, "Basic ", 0, 6)) return invalidClient();
                String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
                String[] pair = decoded.split(":", 2);
                if (pair.length != 2) return invalidClient();
                clientId = Forms.unescape(pair[0]);
                secret = Forms.unescape(pair[1]);
            } catch (IllegalArgumentException e) {
                return invalidClient();
            }
        }
        if (!CLIENT_ID.equals(clientId) || !CLIENT_SECRET.equals(secret)) return invalidClient();
        if ("client_credentials".equals(form.get("grant_type"))) return clientCredentials(clientId, form.get("scope"));
        if ("refresh_token".equals(form.get("grant_type"))) return refresh(clientId, form.get("refresh_token"), form.get("scope"));
        if (!"authorization_code".equals(form.get("grant_type"))) return error(400, "unsupported_grant_type");

        // Removing before issuing makes each authorization code usable at most once.
        Grant grant = codes.remove(form.getOrDefault("code", ""));
        if (grant == null || !grant.clientId().equals(clientId)) return error(400, "invalid_grant");

        String token = Forms.random();
        tokens.put(token, clientId, grant.scope());
        String refresh = createRefreshToken(clientId, grant.scope());
        var response = new java.util.LinkedHashMap<String, String>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        if (includeScopes()) response.put("scope", grant.scope());
        if (refresh != null) response.put("refresh_token", refresh);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache").body(response);
    }

    protected String createRefreshToken(String clientId, String scope) {
        String refresh = Forms.random();
        tokens.save(refresh, new TokenStore.Token(clientId, TokenStore.scopes(scope), null, true));
        return refresh;
    }

    protected ResponseEntity<?> refresh(String clientId, String refreshToken, String requestedScope) {
        var stored = tokens.refresh(refreshToken);
        if (stored == null || !stored.clientId().equals(clientId)) return error(400, "invalid_grant");
        var scope = requestedScope == null ? stored.scope() : TokenStore.scopes(requestedScope);
        if (!stored.scope().containsAll(scope)) return error(400, "invalid_scope");
        String access = Forms.random();
        tokens.put(access, clientId, String.join(" ", scope));
        var response = new java.util.LinkedHashMap<String, String>();
        response.put("access_token", access); response.put("token_type", "Bearer"); response.put("refresh_token", refreshToken);
        if (includeScopes()) response.put("scope", String.join(" ", scope));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").header(HttpHeaders.PRAGMA, "no-cache").body(response);
    }

    protected ResponseEntity<?> clientCredentials(String clientId, String requestedScope) {
        var scope = TokenStore.scopes(requestedScope);
        if (!validScopes(scope)) return error(400, "invalid_scope");
        String access = Forms.random();
        tokens.put(access, clientId, String.join(" ", scope));
        // Client credentials represent the client itself, so no refresh token is issued.
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of("access_token", access, "token_type", "Bearer", "scope", String.join(" ", scope)));
    }

    protected boolean validScopes(List<String> scope) { return SCOPES.containsAll(scope); }
    protected List<String> grantedScopes(Map<String, String> form) {
        return form.keySet().stream().filter(k -> k.startsWith("scope_")).map(k -> k.substring(6)).sorted().toList();
    }
    protected boolean includeScopes() { return true; }

    private ResponseEntity<?> authorizationError(String error, String state) {
        return Forms.redirect(redirectUri, withState("error", error, state));
    }

    private static Map<String, String> withState(String key, String value, String state) {
        return state == null ? Map.of(key, value) : Map.of(key, value, "state", state);
    }

    private static List<String> scopes(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.trim().split("\\s+"));
    }

    private static ResponseEntity<?> error(int status, String error) {
        return ResponseEntity.status(status).body(Map.of("error", error));
    }

    private static ResponseEntity<?> invalidClient() {
        return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"token\"")
                .body(Map.of("error", "invalid_client"));
    }

    private record AuthorizationRequest(String state, List<String> scopes) {}
    private record Grant(String clientId, String scope) {}
}
