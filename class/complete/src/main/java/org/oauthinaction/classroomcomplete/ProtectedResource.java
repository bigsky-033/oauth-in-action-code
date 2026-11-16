package org.oauthinaction.classroomcomplete;
import org.oauthinaction.classroomcomplete.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class ProtectedResource {
    private final TokenStore tokens;
    private final Map<String, Map<String, Object>> users = Map.of(
            "alice", Map.of("sub", "9XE3-JI34-00132A", "name", "Alice", "preferred_username", "alice", "email", "alice.wonderland@example.com", "email_verified", true),
            "bob", Map.of("sub", "1ZT5-OE63-57383B", "name", "Bob", "preferred_username", "bob", "email", "bob.loblob@example.net", "email_verified", false),
            "carol", Map.of("sub", "F5Q1-L6LGG-959FS", "name", "Carol", "preferred_username", "carol", "email", "carol.lewis@example.net", "email_verified", true));
    public ProtectedResource(TokenStore tokens) { this.tokens = tokens; }
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource(TokenStore.local())); }
    @PostMapping("/resource")
    public ResponseEntity<?> resource(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        return access(auth, parameters) == null ? ResponseEntity.status(401).build() : ResponseEntity.ok(Map.of("name", "Protected Resource", "description", "This data has been protected by OAuth 2.0"));
    }
    @RequestMapping(value = "/userinfo", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> userInfo(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam Map<String, String> parameters) {
        var token = access(auth, parameters); if (token == null) return ResponseEntity.status(401).build();
        if (!token.scope().contains("openid")) return ResponseEntity.status(403).build();
        var user = token.user() == null ? null : users.get(token.user()); if (user == null) return ResponseEntity.notFound().build();
        var result = new LinkedHashMap<String, Object>(); result.put("sub", user.get("sub"));
        if (token.scope().contains("profile")) for (String claim : List.of("name", "preferred_username")) result.put(claim, user.get(claim));
        if (token.scope().contains("email")) for (String claim : List.of("email", "email_verified")) result.put(claim, user.get(claim));
        return ResponseEntity.ok(result);
    }
    private TokenStore.Token access(String auth, Map<String, String> parameters) {
        return tokens.access(auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7) : parameters.get("access_token"));
    }
}
