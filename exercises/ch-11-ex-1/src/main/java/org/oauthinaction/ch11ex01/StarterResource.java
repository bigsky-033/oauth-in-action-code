package org.oauthinaction.ch11ex01;
import org.oauthinaction.ch11ex01.completed.CompletedResource;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RestController;
@RestController
public final class StarterResource extends CompletedResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource()); }
    @Override protected JsonNode accepted(String token) {
        // TODO: parse the JWT, then validate issuer, audience, issued-at and expiry.
        return null;
    }
}
