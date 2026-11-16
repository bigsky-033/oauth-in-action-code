package org.oauthinaction.classroomsnippets;
import org.oauthinaction.classroomcomplete.Forms;
import org.oauthinaction.classroomcomplete.Jwt;
import java.time.Instant;
import java.util.*;
/** Snippet 5: signed JWT ACCESS token, whose audience is the resource server. */
public final class JwtExample {
    public static String issue(String subject) {
        long now = Instant.now().getEpochSecond();
        var claims = new LinkedHashMap<String, Object>();
        claims.put("iss", "http://localhost:9001/"); claims.put("sub", subject);
        claims.put("aud", "http://localhost:9002/"); claims.put("iat", now);
        claims.put("exp", now + 300); claims.put("jti", Forms.random());
        return Jwt.sign(claims); // See ../complete/Jwt for JWS framing and JDK RSA primitives.
    }
}
