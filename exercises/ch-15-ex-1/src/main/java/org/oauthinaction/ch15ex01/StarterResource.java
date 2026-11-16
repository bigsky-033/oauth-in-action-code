package org.oauthinaction.ch15ex01;
import org.oauthinaction.ch15ex01.completed.CompletedResource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
@RestController
public final class StarterResource extends CompletedResource {
    public StarterResource(URI authorization) { super(authorization); }
    public static void main(String[] args) { WebServer.start(9002, "resource", new StarterResource(URI.create("http://localhost:9001/"))); }
    @Override protected boolean accepted(String proof, HttpServletRequest incoming) {
        // TODO: introspect the bound access token, verify its proof key/signature and request components.
        return false;
    }
}
