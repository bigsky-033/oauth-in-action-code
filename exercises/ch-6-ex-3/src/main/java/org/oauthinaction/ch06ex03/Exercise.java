package org.oauthinaction.ch06ex03;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Starts three independent HTTP servers in one JVM for IntelliJ's Run button. */
public final class Exercise {
    private Exercise() {}

    public static void start(boolean completed) {
        var servers = new ArrayList<WebServer>();
        try {
            var tokens = TokenStore.local();
            tokens.clear();
            servers.add(WebServer.start(9001, "authorization",
                    completed ? new org.oauthinaction.ch06ex03.completed.CompletedAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens) : new StarterAuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)));
            servers.add(WebServer.start(9002, "resource", new ProtectedResource(tokens)));
            servers.add(WebServer.start(9000, "client", completed ? new org.oauthinaction.ch06ex03.completed.CompletedClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/")) : new StarterClient(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"))));
        } catch (RuntimeException e) {
            close(servers);
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> close(servers)));
        System.out.println("Chapter 6 exercise 3: open http://localhost:9000/");
    }

    private static void close(List<WebServer> servers) {
        for (WebServer server : servers.reversed()) server.close();
    }
}
