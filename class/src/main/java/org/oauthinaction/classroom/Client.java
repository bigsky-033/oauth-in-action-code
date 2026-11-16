package org.oauthinaction.classroom;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public final class Client {
    public static void main(String[] args) { WebServer.start(9000, "client", new Client()); }
    // These are the workshop's starting TODOs, not completed OAuth handlers.
    @GetMapping({"/authorize", "/callback", "/fetch_resource", "/userinfo"})
    public org.springframework.http.ResponseEntity<?> unfinished() {
        return org.springframework.http.ResponseEntity.status(501).body(Map.of("error", "Implement this classroom client handler"));
    }
    @GetMapping("/status") public Map<String, Object> status() {
        var status = new LinkedHashMap<String, Object>(); status.put("access_token", null); status.put("refresh_token", null); status.put("scope", null); return status;
    }
}
