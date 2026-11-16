package org.oauthinaction.ch04ex01;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public final class StarterResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource()); }
    @CrossOrigin
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> parameters) {
        // TODO: extract bearer token (header, body, then query), look it up in
        // TokenStore.local(), and return data only for a matching access token.
        return ResponseEntity.status(501).body(Map.of("error", "Exercise TODO: extract and verify the access token"));
    }
}
