package org.oauthinaction.appendixa;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public final class Client {
    public static void main(String[] args) { WebServer.start(9000, "client", new Client()); }
    @GetMapping("/status") public Map<String, Object> status() {
        var status = new LinkedHashMap<String, Object>(); status.put("access_token", null); status.put("refresh_token", null); status.put("scope", null); return status;
    }
}
