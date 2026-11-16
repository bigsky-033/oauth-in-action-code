package org.oauthinaction.classroomday2;
import org.oauthinaction.classroomday2.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class ProtectedResource {
    private final TokenStore tokens;
    public ProtectedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource(TokenStore.local())); }
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        return access(auth, parameters) == null ? ResponseEntity.status(401).build() : ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    @RequestMapping(value = "/userinfo", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> userInfo(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = access(auth, parameters); if (token == null) return ResponseEntity.status(401).build();
        // TODO: resolve the user and return claims allowed by the granted scopes.
        return ResponseEntity.status(501).body(Map.of("error", "Classroom TODO: implement UserInfo"));
    }
    private TokenStore.Token access(String auth, Map<String, String> parameters) {
        return tokens.access(auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7) : parameters.get("access_token"));
    }
}
