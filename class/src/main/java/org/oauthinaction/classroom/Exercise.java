package org.oauthinaction.classroom;
import java.util.ArrayList;
public final class Exercise {
    public static void main(String[] args) {
        var servers = new ArrayList<WebServer>();
        try {
            servers.add(WebServer.start(9001, "authorization", new AuthorizationServer()));
            servers.add(WebServer.start(9002, "resource", new ProtectedResource()));
            servers.add(WebServer.start(9000, "client", new Client()));
        } catch (RuntimeException e) { for (var server : servers.reversed()) server.close(); throw e; }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { for (var server : servers.reversed()) server.close(); }));
        System.out.println("Classroom starting scaffold: http://localhost:9000/");
    }
}
