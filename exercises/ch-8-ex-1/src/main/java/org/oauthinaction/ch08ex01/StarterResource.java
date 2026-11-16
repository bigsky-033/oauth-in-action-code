package org.oauthinaction.ch08ex01;
import org.oauthinaction.ch08ex01.completed.CompletedResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(TokenStore tokens) { super(tokens); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(TokenStore.local())); }
    @Override protected ResponseEntity<?> response(String language) {
        // TODO: return JSON with content protection headers and encoded reflected input.
        // This intentionally retains the original starter's reflected-HTML weakness.
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(greetings().getOrDefault(language, "Error, invalid language: " + language));
    }
}
