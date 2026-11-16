package org.oauthinaction.ch06ex03;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.io.IOException;
import java.util.*;

/** HTTP transport and token display shared by this exercise's starter and answer. */
public class ClientSupport {
    private final URI authorization;
    private final URI resource;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    public ClientSupport(URI authorization, URI resource) { this.authorization = authorization; this.resource = resource; }
    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        var result = new LinkedHashMap<String, Object>();
        result.put("starter", starter()); result.put("access_token", session.getAttribute("access_token"));
        result.put("scope", session.getAttribute("scope")); result.put("refresh_token", session.getAttribute("refresh_token"));
        return result;
    }
    protected boolean starter() { return false; }
    protected ResponseEntity<?> requestToken(Map<String, String> form, HttpSession session) {
        session.removeAttribute("access_token"); session.removeAttribute("scope"); session.removeAttribute("refresh_token");
        var request = HttpRequest.newBuilder(authorization.resolve("/token")).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", Forms.basic("oauth-client-1", "oauth-client-secret-1"))
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(form))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(response.statusCode(), "Token endpoint: " + response.body());
            var token = json.readTree(response.body());
            session.setAttribute("access_token", token.path("access_token").asText());
            session.setAttribute("scope", token.path("scope").asText());
            if (token.hasNonNull("refresh_token")) session.setAttribute("refresh_token", token.path("refresh_token").asText());
            return Forms.redirect(URI.create("/"), Map.of());
        } catch (IOException e) { return error(502, "Cannot reach/read the token endpoint"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Token request interrupted"); }
    }
    @GetMapping("/fetch_resource")
    public ResponseEntity<?> fetchResource(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return error(400, "Missing access token");
        var request = HttpRequest.newBuilder(resource.resolve("/resource")).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.noBody()).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return error(response.statusCode(), "Resource rejected the token");
            return ResponseEntity.ok(json.readTree(response.body()));
        } catch (IOException e) { return error(502, "Cannot reach/read the resource"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return error(503, "Resource request interrupted"); }
    }
    protected static ResponseEntity<?> error(int status, String message) { return ResponseEntity.status(status).body(Map.of("error", message)); }
}
