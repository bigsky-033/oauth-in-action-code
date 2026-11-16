package org.oauthinaction.classroomsnippets;
import org.oauthinaction.classroomcomplete.TokenStore;
import java.net.http.HttpRequest;
import java.util.Map;
/** Snippet 4: bearer extraction is distinct from finding a valid stored token. */
public final class FetchResource {
    public static HttpRequest request(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Missing access token");
        return HttpRequest.newBuilder(ClientServerInfo.RESOURCE).header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
    }
    public static TokenStore.Token lookup(TokenStore tokens, String authorization, Map<String, String> parameters) {
        String value = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7) : parameters.get("access_token");
        return tokens.access(value); // null means the endpoint must return 401.
    }
}
