package org.oauthinaction.ch06ex04;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Java serves the page; the implicit OAuth client actually runs in the browser. */
@Controller
public final class Client {
    public static void main(String[] args) { WebServer.start(9000, "client", new Client()); }
    @GetMapping({"/", "/callback"})
    public String page() { return "forward:/index.html"; }
}
