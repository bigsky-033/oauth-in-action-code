package org.oauthinaction.examplemain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.oauthinaction.examplemain.Forms;
import org.oauthinaction.examplemain.WebServer;
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
public class Client {
    private static final String CLIENT_ID = "oauth-client-1";
    private static final String CLIENT_SECRET = "oauth-client-secret-1";
    private final URI authorizationServer;
    private final URI resourceServer;
    private final URI redirectUri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public Client(URI authorizationServer, URI resourceServer, URI redirectUri) {
        this.authorizationServer = authorizationServer;
        this.resourceServer = resourceServer;
        this.redirectUri = redirectUri;
    }

    public static void main(String[] args) {
        WebServer.start(9000, "client", new Client(URI.create("http://localhost:9001/"),
                URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback")));
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        var status = new LinkedHashMap<String, Object>();
        status.put("starter", false);
        status.put("access_token", session.getAttribute("access_token"));
        status.put("scope", session.getAttribute("scope"));
        status.put("id_token", session.getAttribute("id_token"));
        status.put("refresh_token", session.getAttribute("refresh_token"));
        return status;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(HttpSession session) {
        session.removeAttribute("access_token");
        session.removeAttribute("refresh_token");
        session.removeAttribute("scope");
        session.removeAttribute("id_token");
        String state = Forms.random();
        session.setAttribute("state", state);
        return Forms.redirect(authorizationServer.resolve("/authorize"), Map.of(
                "response_type", "code", "client_id", CLIENT_ID,
                "redirect_uri", redirectUri.toString(), "state", state, "scope", "foo bar openid profile email phone address read write delete fruit veggies meats movies foods music"));
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
            if (token.hasNonNull("id_token")) {
                var claims = Jwt.verify(token.path("id_token").asText());
                long now = java.time.Instant.now().getEpochSecond();
                if (!"http://localhost:9001/".equals(claims.path("iss").asText()) || !CLIENT_ID.equals(claims.path("aud").asText())
                        || claims.path("iat").asLong(Long.MAX_VALUE) > now || claims.path("exp").asLong(0) < now) return error(400, "Invalid ID token claims");
                session.setAttribute("id_token", claims);
            }
            session.setAttribute("access_token", token.path("access_token").asText());
            session.setAttribute("scope", token.path("scope").asText());
            if (token.hasNonNull("refresh_token")) session.setAttribute("refresh_token", token.path("refresh_token").asText());
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IllegalArgumentException e) {
            return error(400, "Invalid ID token signature");
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
                        "refresh_token", refresh, "client_id", CLIENT_ID, "client_secret", CLIENT_SECRET))))
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

    @GetMapping("/revoke")
    public ResponseEntity<?> revokePage() { return Forms.redirect(URI.create("/revoke.html"), Map.of()); }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return error(400, "Missing access token");
        var request = HttpRequest.newBuilder(authorizationServer.resolve("/revoke")).timeout(Duration.ofSeconds(5))
                .header("Authorization", Forms.basic(CLIENT_ID, CLIENT_SECRET)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("token", token)))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return error(response.statusCode(), "Unable to revoke token");
            session.removeAttribute("access_token"); session.removeAttribute("refresh_token"); session.removeAttribute("scope"); session.removeAttribute("id_token");
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) { return error(502, "Cannot reach revocation endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Revocation interrupted"); }
    }

    @GetMapping("/userinfo")
    public ResponseEntity<?> userInfo(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return error(400, "Missing access token");
        var request = HttpRequest.newBuilder(authorizationServer.resolve("/userinfo")).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(response.statusCode(), "Unable to fetch user information");
            var result = new LinkedHashMap<String, Object>(); result.put("userInfo", json.readTree(response.body())); result.put("id_token", session.getAttribute("id_token"));
            return ResponseEntity.ok(result);
        } catch (IOException e) { return error(502, "Cannot reach UserInfo endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "UserInfo request interrupted"); }
    }

    @GetMapping("/username_password")
    public ResponseEntity<?> passwordPage() { return Forms.redirect(URI.create("/username_password.html"), Map.of()); }

    @PostMapping("/username_password")
    public ResponseEntity<?> password(@RequestParam Map<String, String> form, HttpSession session) {
        var request = HttpRequest.newBuilder(authorizationServer.resolve("/token")).timeout(Duration.ofSeconds(5))
                .header("Authorization", Forms.basic(CLIENT_ID, CLIENT_SECRET)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("grant_type", "password", "username", form.getOrDefault("username", ""), "password", form.getOrDefault("password", ""))))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(response.statusCode(), "Unable to get password-grant token");
            var token = json.readTree(response.body()); session.setAttribute("access_token", token.path("access_token").asText()); session.setAttribute("scope", token.path("scope").asText());
            session.removeAttribute("refresh_token"); session.removeAttribute("id_token");
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) { return error(502, "Cannot reach token endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Password request interrupted"); }
    }

    @GetMapping("/words") public ResponseEntity<?> wordsPage() { return Forms.redirect(URI.create("/"), Map.of()); }
    @GetMapping("/get_words") public ResponseEntity<?> words(HttpSession session) { return callApi("GET", "/words", Map.of(), session); }
    @GetMapping("/add_word") public ResponseEntity<?> addWord(@RequestParam(defaultValue = "") String word, HttpSession session) { return callApi("POST", "/words", Map.of("word", word), session); }
    @GetMapping("/delete_word") public ResponseEntity<?> deleteWord(HttpSession session) { return callApi("DELETE", "/words", Map.of(), session); }
    @GetMapping("/produce") public ResponseEntity<?> produce(HttpSession session) { return callApi("GET", "/produce", Map.of(), session); }
    @GetMapping("/favorites") public ResponseEntity<?> favorites(HttpSession session) { return callApi("GET", "/favorites", Map.of(), session); }

    private ResponseEntity<?> callApi(String method, String path, Map<String, String> form, HttpSession session) {
        String token = (String) session.getAttribute("access_token"); if (token == null) return error(400, "Missing access token");
        var request = HttpRequest.newBuilder(resourceServer.resolve(path)).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/x-www-form-urlencoded")
                .method(method, form.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(Forms.encode(form))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return error(response.statusCode(), "Resource rejected this operation");
            return response.body().isBlank() ? ResponseEntity.status(response.statusCode()).build() : ResponseEntity.status(response.statusCode()).body(json.readTree(response.body()));
        } catch (IOException e) { return error(502, "Cannot reach/read resource"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Resource request interrupted"); }
    }

    private static ResponseEntity<?> error(int status, String error) {
        return ResponseEntity.status(status).body(Map.of("error", error));
    }
}
