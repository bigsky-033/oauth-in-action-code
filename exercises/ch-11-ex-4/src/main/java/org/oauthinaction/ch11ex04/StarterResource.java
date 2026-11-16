package org.oauthinaction.ch11ex04;
import org.oauthinaction.ch11ex04.completed.CompletedResource;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(URI authorization) { super(authorization); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(URI.create("http://localhost:9001/"))); }
    @Override protected JsonNode inspect(String token) {
        // TODO: authenticate to /introspect, send the token, and require active=true.
        return null;
    }
}
