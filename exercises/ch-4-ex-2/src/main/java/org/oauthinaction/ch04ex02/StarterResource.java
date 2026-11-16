package org.oauthinaction.ch04ex02;
import org.oauthinaction.ch04ex02.completed.CompletedResource;
import org.springframework.web.bind.annotation.RestController;

/** The starter authenticates tokens but deliberately omits per-operation scopes. */
@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(TokenStore tokens) { super(tokens); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @Override protected boolean allowed(TokenStore.Token token, String required) {
        // TODO: require the operation's scope; a valid token alone is insufficient.
        return true;
    }
}
