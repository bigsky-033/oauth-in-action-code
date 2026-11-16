package org.oauthinaction.ch03ex01;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Starts three independent HTTP servers in one JVM for IntelliJ's Run button. */
public final class Exercise {
    private Exercise() {}

    public static void start(Object client) {
        var servers = new ArrayList<WebServer>();
        try {
            var tokens = TokenStore.local();
            tokens.clear();
            servers.add(WebServer.start(9001, "authorization",
                    new AuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)));
            servers.add(WebServer.start(9002, "resource", new ProtectedResource(tokens)));
            servers.add(WebServer.start(9000, "client", client));
        } catch (RuntimeException e) {
            close(servers);
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> close(servers)));
        System.out.println("Chapter 3 exercise 1: open http://localhost:9000/");
    }

    private static void close(List<WebServer> servers) {
        for (WebServer server : servers.reversed()) server.close();
    }
}
