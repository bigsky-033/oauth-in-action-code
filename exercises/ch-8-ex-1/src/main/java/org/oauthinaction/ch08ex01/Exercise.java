package org.oauthinaction.ch08ex01;
import java.net.URI;
import java.util.ArrayList;

/** Launches the desktop Java client and its two protocol servers for study. */
public final class Exercise {
    public static void start(boolean completed) {
        var servers = new ArrayList<WebServer>();
        try {
            var tokens = TokenStore.local(); tokens.clear();
            servers.add(WebServer.start(9001, "authorization", new AuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)));
            servers.add(WebServer.start(9002, "resource", completed ? new org.oauthinaction.ch08ex01.completed.CompletedResource(tokens) : new StarterResource(tokens)));
            servers.add(WebServer.start(9000, "client", new Client(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))));
        } catch (RuntimeException e) { for (var server : servers.reversed()) server.close(); throw e; }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { for (var server : servers.reversed()) server.close(); }));
        System.out.println("Chapter 8 exercise 1: desktop Java client ready at http://localhost:9000/");
    }
}
