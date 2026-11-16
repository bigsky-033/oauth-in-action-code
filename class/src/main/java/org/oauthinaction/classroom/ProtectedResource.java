package org.oauthinaction.classroom;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public final class ProtectedResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource()); }
    @CrossOrigin @PostMapping("/resource") public ResponseEntity<?> resource() {
        return ResponseEntity.status(501).body(Map.of("error", "Implement bearer extraction and resource access"));
    }
    // The original extractor never attached a token, so its UserInfo guard rejected every request.
    @RequestMapping(value="/userinfo", method={RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> userInfo() { return ResponseEntity.status(401).build(); }
}
