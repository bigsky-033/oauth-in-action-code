package org.oauthinaction.ch10ex01;
import org.oauthinaction.ch10ex01.completed.CompletedAuthorizationServer;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override protected boolean validPkce(String challenge, String method, String verifier) {
        // TODO: check plain/S256 verifier against the challenge bound to this code.
        return true;
    }
}
