package org.oauthinaction.examplechapter10;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Java serves the page; the implicit OAuth client actually runs in the browser. */
@Controller
public final class ImplicitClient {
    public static void main(String[] args) { WebServer.start(9000, "implicit", new ImplicitClient()); }
    @GetMapping({"/", "/callback"})
    public String page() { return "forward:/index.html"; }
}
