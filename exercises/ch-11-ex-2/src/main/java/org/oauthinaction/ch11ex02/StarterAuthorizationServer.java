package org.oauthinaction.ch11ex02;
import org.oauthinaction.ch11ex02.completed.CompletedAuthorizationServer;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override protected String issueAccessToken(String clientId, String scope, String user) {
        // TODO: sign the JWT with HS256 instead of returning an unsigned JWT.
        return TokenJwt.unsigned(claimsForUser(user));
    }
}
