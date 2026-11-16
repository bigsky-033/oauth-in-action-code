package org.oauthinaction.examplesolution0;
import org.oauthinaction.examplechapter8.*;
import java.net.URI;
import java.util.ArrayList;
public final class Solution {
    public static void main(String[] args) {
        var servers = new ArrayList<WebServer>();
        try {
            var tokens = TokenStore.local(); tokens.clear();
            servers.add(WebServer.start(9001, "authorization", new AuthorizationServer(URI.create("http://localhost:9000/callback"), tokens)));
            servers.add(WebServer.start(9002, "resource", new ProtectedResource0c(URI.create("http://localhost:9001/"))));
            servers.add(WebServer.start(9000, "client", new Client(URI.create("http://localhost:9001/"), URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback"))));
        } catch (RuntimeException e) { for (var server : servers.reversed()) server.close(); throw e; }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { for (var server : servers.reversed()) server.close(); }));
        System.out.println("Chapter 8 solution 0c: http://localhost:9000/");
    }
}
