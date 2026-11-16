package org.oauthinaction.ch09ex01;
import org.oauthinaction.ch09ex01.completed.CompletedAuthorizationServer;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override protected boolean validRedirect(String expected, String received) {
        // TODO: require the token request redirect URI to equal the one bound to the code.
        return true;
    }
}
