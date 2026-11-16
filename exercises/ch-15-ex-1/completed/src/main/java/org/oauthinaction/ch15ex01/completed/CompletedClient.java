package org.oauthinaction.ch15ex01.completed;
import org.oauthinaction.ch15ex01.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.oauthinaction.ch15ex01.Forms;
import org.oauthinaction.ch15ex01.WebServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CompletedClient {
    private static final String CLIENT_ID = "oauth-client-1";
    private static final String CLIENT_SECRET = "oauth-client-secret-1";
    private final URI authorizationServer;
    private final URI resourceServer;
    private final URI redirectUri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public CompletedClient(URI authorizationServer, URI resourceServer, URI redirectUri) {
        this.authorizationServer = authorizationServer;
        this.resourceServer = resourceServer;
        this.redirectUri = redirectUri;
    }

    public static void main(String[] args) {
        WebServer.start(9000, "client", new CompletedClient(URI.create("http://localhost:9001/"),
                URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback")));
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        var status = new LinkedHashMap<String, Object>();
        status.put("starter", starter());
        status.put("key", session.getAttribute("key"));
        status.put("access_token", session.getAttribute("access_token"));
        status.put("scope", session.getAttribute("scope"));
        status.put("refresh_token", session.getAttribute("refresh_token"));
        return status;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(HttpSession session) {
        session.removeAttribute("access_token");
        session.removeAttribute("refresh_token");
        session.removeAttribute("scope");
        session.removeAttribute("key");
        String state = Forms.random();
        session.setAttribute("state", state);
        return Forms.redirect(authorizationServer.resolve("/authorize"), Map.of(
                "response_type", "code", "client_id", CLIENT_ID,
                "redirect_uri", redirectUri.toString(), "state", state, "scope", "foo"));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam Map<String, String> query, HttpSession session) {
        String expectedState = (String) session.getAttribute("state");
        session.removeAttribute("state");
        // The browser must return the state belonging to THIS authorization attempt.
        if (expectedState == null || !expectedState.equals(query.get("state"))) return error(400, "State value did not match");
        if (query.containsKey("error")) return Forms.redirect(URI.create("/"), Map.of("error", query.get("error")));
        String code = query.get("code");
        if (code == null || code.isBlank()) return error(400, "Missing authorization code");

        var request = HttpRequest.newBuilder(authorizationServer.resolve("/token"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", Forms.basic(CLIENT_ID, CLIENT_SECRET))
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of(
                        "grant_type", "authorization_code", "code", code, "redirect_uri", redirectUri.toString()))))
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(502, "Token endpoint returned " + response.statusCode());
            JsonNode token = json.readTree(response.body());
            session.setAttribute("key", json.convertValue(token.path("access_token_key"), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}));
            session.setAttribute("access_token", token.path("access_token").asText());
            session.setAttribute("scope", token.path("scope").asText());
            if (token.hasNonNull("refresh_token")) session.setAttribute("refresh_token", token.path("refresh_token").asText());
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) {
            return error(502, "Cannot reach or read the token endpoint");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error(503, "Token request interrupted");
        }
    }

    @GetMapping("/fetch_resource")
    public ResponseEntity<?> fetchResource(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return error(400, "Missing Access Token");
        @SuppressWarnings("unchecked") var key = (Map<String, Object>) session.getAttribute("key");
        if (key == null) return error(400, "Missing proof key");
        String proof = Proof.sign(key, Map.of("at", token, "ts", java.time.Instant.now().getEpochSecond(),
                "m", "POST", "u", resourceServer.getAuthority(), "p", "/resource"));
        var request = HttpRequest.newBuilder(resourceServer.resolve("/resource"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "PoP " + proof)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                session.removeAttribute("access_token");
                return error(response.statusCode(), "Resource server returned " + response.statusCode());
            }
            return ResponseEntity.ok(json.readTree(response.body()));
        } catch (IOException e) {
            return error(502, "Cannot reach or read the protected resource");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error(503, "Resource request interrupted");
        }
    }

    protected boolean starter() { return false; }

    private static ResponseEntity<?> error(int status, String error) {
        return ResponseEntity.status(status).body(Map.of("error", error));
    }
}
