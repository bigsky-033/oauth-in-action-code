package org.oauthinaction.ch04ex03;
import org.oauthinaction.ch04ex03.completed.CompletedResource;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(TokenStore tokens) { super(tokens); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @Override protected boolean allowed(TokenStore.Token token, String category) {
        // TODO: include a category only when its scope is present on the token.
        return true;
    }
}
