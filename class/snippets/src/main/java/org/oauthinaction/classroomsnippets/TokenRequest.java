package org.oauthinaction.classroomsnippets;
import org.oauthinaction.classroomcomplete.Forms;
import java.net.http.HttpRequest;
import java.util.Map;
/** Snippet 3: check and consume session state BEFORE building the code exchange. */
public final class TokenRequest {
    public static HttpRequest exchange(String code, String expectedState, String returnedState) {
        if (expectedState == null || !expectedState.equals(returnedState)) throw new IllegalArgumentException("State did not match");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Missing code");
        String body = Forms.encode(Map.of("grant_type", "authorization_code", "code", code,
                "redirect_uri", ClientServerInfo.CALLBACK.toString()));
        return HttpRequest.newBuilder(ClientServerInfo.TOKEN)
                .header("Authorization", Forms.basic(ClientServerInfo.CLIENT_ID, ClientServerInfo.CLIENT_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
    // Send with HttpClient; only save the returned access token after checking HTTP status.
    // Server authentication, one-use code removal and issuance are in ../complete/AuthorizationServer.token.
}
