package org.oauthinaction.ch04ex04;
import org.oauthinaction.ch04ex04.completed.CompletedResource;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(TokenStore tokens) { super(tokens); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @Override protected Map<String, Object> forUser(String user) {
        // TODO: select Alice's or Bob's favorites using the token's user key.
        return unknown();
    }
}
