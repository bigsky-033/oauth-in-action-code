package org.oauthinaction.examplechapter8;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;
/** Baseline variant 3: reflected HTML without the corresponding solution's protections. */
@RestController
public final class ProtectedResource3 extends GreetingSupport {
    public ProtectedResource3(URI authorization) { super(authorization); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource3(URI.create("http://localhost:9001/"))); }
    @GetMapping("/helloWorld")
    public ResponseEntity<?> hello(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        if (inspect(auth, parameters) == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(greeting(parameters.getOrDefault("language", "")));
    }
}
