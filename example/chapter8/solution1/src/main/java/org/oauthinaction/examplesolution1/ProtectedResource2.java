package org.oauthinaction.examplesolution1;
import org.oauthinaction.examplechapter8.GreetingSupport;
import org.oauthinaction.examplechapter8.Forms;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;
@RestController
public final class ProtectedResource2 extends GreetingSupport {
    public ProtectedResource2(URI authorization) { super(authorization); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource2(URI.create("http://localhost:9001/"))); }
    @CrossOrigin
    @GetMapping("/helloWorld")
    public ResponseEntity<?> hello(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        if (inspect(auth, parameters) == null) return ResponseEntity.status(401).build();
        String language = parameters.getOrDefault("language", "");
        String greeting = List.of("en", "de", "it", "fr", "es").contains(language) ? greeting(language)
                : "Error, invalid language: " + Forms.encode(Map.of("language", language)).substring(9);
        return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff").header("X-XSS-Protection", "1; mode=block").body(Map.of("greeting", greeting));
    }
}
