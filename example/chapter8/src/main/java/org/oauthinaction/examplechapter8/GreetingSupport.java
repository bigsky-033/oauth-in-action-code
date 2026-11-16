package org.oauthinaction.examplechapter8;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
public class GreetingSupport {
    private final URI authorization;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    public GreetingSupport(URI authorization) { this.authorization = authorization; }
    public static String greeting(String language) {
        return Map.of("en", "Hello World", "de", "Hallo Welt", "it", "Ciao Mondo", "fr", "Bonjour monde", "es", "Hola mundo").getOrDefault(language, "Error, invalid language: " + language);
    }
    protected JsonNode inspect(String auth, Map<String, String> parameters) {
        String token = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7) : parameters.get("access_token");
        if (token == null || token.isBlank()) return null;
        // This resource has no access to token storage: it asks the authorization server over HTTP.
        var request = HttpRequest.newBuilder(authorization.resolve("/introspect")).timeout(Duration.ofSeconds(5))
                .header("Authorization", Forms.basic("protected-resource-1", "protected-resource-secret-1"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Forms.encode(Map.of("token", token)))).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var body = json.readTree(response.body()); return body.path("active").asBoolean() ? body : null;
        } catch (java.io.IOException e) { return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }
}
