package org.oauthinaction.ch12ex02.completed;
import org.oauthinaction.ch12ex02.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.oauthinaction.ch12ex02.Forms;
import org.oauthinaction.ch12ex02.WebServer;
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

    protected boolean starter() { return false; }

    protected ResponseEntity<?> register(HttpSession session) {
        try {
            var metadata = Map.of("client_name", "OAuth in Action Dynamic Test Client", "redirect_uris", java.util.List.of(redirectUri.toString()),
                    "scope", "foo bar", "grant_types", java.util.List.of("authorization_code", "refresh_token"),
                    "response_types", java.util.List.of("code"), "token_endpoint_auth_method", "client_secret_basic");
            var request = HttpRequest.newBuilder(authorizationServer.resolve("/register")).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(metadata))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) return error(502, "Client registration failed: " + response.statusCode());
            var registration = json.readTree(response.body());
            session.setAttribute("client_id", registration.path("client_id").asText());
            session.setAttribute("client_secret", registration.path("client_secret").asText());
            session.setAttribute("client", registration);
            return null;
        } catch (IOException e) { return error(502, "Cannot reach/read registration endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Registration interrupted"); }
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        var status = new LinkedHashMap<String, Object>();
        status.put("starter", starter());
        status.put("client_id", session.getAttribute("client_id"));
        status.put("client", session.getAttribute("client"));
        status.put("access_token", session.getAttribute("access_token"));
        status.put("scope", session.getAttribute("scope"));
        status.put("refresh_token", session.getAttribute("refresh_token"));
        return status;
    }

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(HttpSession session) {
        if (session.getAttribute("client_id") == null) {
            var failure = register(session);
            if (failure != null) return failure;
        }
        session.removeAttribute("access_token");
        session.removeAttribute("refresh_token");
        session.removeAttribute("scope");
        String state = Forms.random();
        session.setAttribute("state", state);
        return Forms.redirect(authorizationServer.resolve("/authorize"), Map.of(
                "response_type", "code", "client_id", (String) session.getAttribute("client_id"),
                "redirect_uri", redirectUri.toString(), "state", state, "scope", "foo bar"));
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
                                .header("Authorization", Forms.basic((String) session.getAttribute("client_id"), (String) session.getAttribute("client_secret")))
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of(
                        "grant_type", "authorization_code", "code", code, "redirect_uri", redirectUri.toString()))))
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(502, "Token endpoint returned " + response.statusCode());
            JsonNode token = json.readTree(response.body());
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

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        String refresh = (String) session.getAttribute("refresh_token");
        if (refresh == null) return error(400, "Missing refresh token");
        // This client demonstrates form-body client authentication on refresh.
        var request = HttpRequest.newBuilder(authorizationServer.resolve("/token"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("grant_type", "refresh_token",
                        "refresh_token", refresh, "client_id", (String) session.getAttribute("client_id"), "client_secret", (String) session.getAttribute("client_secret")))))
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(400, "Unable to refresh token");
            var token = json.readTree(response.body());
            session.setAttribute("access_token", token.path("access_token").asText());
            if (token.hasNonNull("refresh_token")) session.setAttribute("refresh_token", token.path("refresh_token").asText());
            if (token.hasNonNull("scope")) session.setAttribute("scope", token.path("scope").asText());
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) { return error(502, "Cannot reach the token endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Refresh interrupted"); }
    }

    @GetMapping("/fetch_resource")
    public ResponseEntity<?> fetchResource(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return error(400, "Missing Access Token");
        var request = HttpRequest.newBuilder(resourceServer.resolve("/resource"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token)
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

    @GetMapping("/read_client")
    public ResponseEntity<?> readClient(HttpSession session) { return manage("GET", null, session); }

    @PostMapping("/update_client")
    public ResponseEntity<?> updateClient(@RequestParam(defaultValue = "") String client_name, HttpSession session) {
        JsonNode registration = (JsonNode) session.getAttribute("client");
        if (registration == null) return error(400, "Register the client first");
        var metadata = new LinkedHashMap<String, Object>();
        for (String field : java.util.List.of("client_id", "client_secret", "redirect_uris", "grant_types", "response_types", "token_endpoint_auth_method", "scope", "client_uri", "logo_uri")) {
            if (registration.has(field)) metadata.put(field, json.convertValue(registration.get(field), Object.class));
        }
        metadata.put("client_name", client_name);
        return manage("PUT", metadata, session);
    }

    @GetMapping("/unregister_client")
    public ResponseEntity<?> unregisterClient(HttpSession session) { return manage("DELETE", null, session); }

    private ResponseEntity<?> manage(String method, Map<String, Object> metadata, HttpSession session) {
        JsonNode registration = (JsonNode) session.getAttribute("client");
        if (registration == null) return error(400, "Register the client first");
        try {
            var request = HttpRequest.newBuilder(URI.create(registration.path("registration_client_uri").asText())).timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + registration.path("registration_access_token").asText())
                    .header("Content-Type", "application/json").method(method, metadata == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(metadata))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return error(response.statusCode(), "Unable to manage registration");
            if (method.equals("DELETE")) {
                for (String attribute : java.util.List.of("client", "client_id", "client_secret", "access_token", "refresh_token", "scope", "state")) session.removeAttribute(attribute);
                return Forms.redirect(URI.create("/"), Map.of());
            }
            var updated = json.readTree(response.body()); session.setAttribute("client", updated);
            return method.equals("GET") ? ResponseEntity.ok(updated) : Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) { return error(502, "Cannot reach/read registration management endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Registration request interrupted"); }
    }

    private static ResponseEntity<?> error(int status, String error) {
        return ResponseEntity.status(status).body(Map.of("error", error));
    }
}
