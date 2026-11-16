package org.oauthinaction.classroom;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public final class AuthorizationServer {
    public static void main(String[] args) { WebServer.start(9001, "authorization", new AuthorizationServer()); }
    @GetMapping("/authorize") public org.springframework.http.ResponseEntity<?> authorize() { return unfinished(); }
    @PostMapping({"/approve", "/token"}) public org.springframework.http.ResponseEntity<?> unfinished() {
        return org.springframework.http.ResponseEntity.status(501).body(Map.of("error", "Implement this classroom authorization handler"));
    }
    @GetMapping("/status") public Map<String, Object> status() {
        return Map.of("clients", List.of());
    }
}
