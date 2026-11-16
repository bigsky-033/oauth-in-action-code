package org.oauthinaction.appendixa;
public final class ProtectedResource {
    public static void main(String[] args) { WebServer.start(9002, "resource", new ProtectedResource()); }
}
