package org.oauthinaction.classroomsnippets;
import java.net.URI;
/** Snippet 1: client registration and endpoint configuration must agree. */
public final class ClientServerInfo {
    public static final String CLIENT_ID = "oauth-client-1";
    public static final String CLIENT_SECRET = "oauth-client-secret-1";
    public static final URI CALLBACK = URI.create("http://localhost:9000/callback");
    public static final URI AUTHORIZE = URI.create("http://localhost:9001/authorize");
    public static final URI TOKEN = URI.create("http://localhost:9001/token");
    public static final URI RESOURCE = URI.create("http://localhost:9002/resource");
    public static final URI USERINFO = URI.create("http://localhost:9002/userinfo");
    public static final String OAUTH_SCOPE = "foo bar";
    public static final String OPENID_SCOPE = "openid profile email phone address";
}
