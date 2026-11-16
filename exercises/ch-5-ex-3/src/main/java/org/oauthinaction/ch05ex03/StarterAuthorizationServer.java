package org.oauthinaction.ch05ex03;
import org.oauthinaction.ch05ex03.completed.CompletedAuthorizationServer;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.*;

@RestController
public final class StarterAuthorizationServer extends CompletedAuthorizationServer {
    public StarterAuthorizationServer(URI callback, TokenStore tokens) { super(callback, tokens); }
    public static void main(String[] args) {
        var tokens = TokenStore.local(); tokens.clear();
        WebServer.start(9001, "authorization", new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens));
    }
    @Override protected boolean validScopes(List<String> scope) {
        // TODO: validate requested/selected scopes against the client registration.
        return true;
    }
    @Override protected List<String> grantedScopes(Map<String, String> form) {
        // TODO: collect checked scopes, store them with the code and both token types.
        return List.of();
    }
    @Override protected boolean includeScopes() {
        // TODO: return the granted scope in token responses.
        return false;
    }
}
