package org.oauthinaction.classroomsnippets;
import org.oauthinaction.classroomcomplete.Forms;
import java.net.URI;
import java.util.*;
/** Snippet 2: keep state in the browser's session, not a process-wide variable. */
public final class AuthorizationRequest {
    public record Attempt(String state, URI location) {}
    public static Attempt begin(String scope) {
        String state = Forms.random();
        var parameters = Map.of("response_type", "code", "client_id", ClientServerInfo.CLIENT_ID,
                "redirect_uri", ClientServerInfo.CALLBACK.toString(), "scope", scope, "state", state);
        return new Attempt(state, URI.create(ClientServerInfo.AUTHORIZE + "?" + Forms.encode(parameters)));
    }
    public static void validate(Map<String, String> query, Set<String> allowedScopes) {
        if (!ClientServerInfo.CLIENT_ID.equals(query.get("client_id"))) throw new IllegalArgumentException("Unknown client");
        // Reject locally before redirecting to an untrusted URI, even for errors.
        if (!ClientServerInfo.CALLBACK.toString().equals(query.get("redirect_uri"))) throw new IllegalArgumentException("Invalid redirect URI");
        if (!"code".equals(query.get("response_type"))) throw new IllegalArgumentException("unsupported_response_type");
        if (!allowedScopes.containsAll(org.oauthinaction.classroomcomplete.TokenStore.scopes(query.get("scope")))) throw new IllegalArgumentException("invalid_scope");
    }
    // The runnable server's saved consent request/code lifecycle is in ../complete/AuthorizationServer.
}
