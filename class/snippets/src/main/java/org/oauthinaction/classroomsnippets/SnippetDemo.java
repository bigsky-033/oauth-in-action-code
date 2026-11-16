package org.oauthinaction.classroomsnippets;
import java.util.Set;
public final class SnippetDemo {
    public static void main(String[] args) {
        var attempt = AuthorizationRequest.begin(ClientServerInfo.OAUTH_SCOPE);
        System.out.println("Authorization URL: " + attempt.location());
        var user = UserRecords.USERS.get("bob");
        String token = IdTokenExample.issue(Set.of("openid", "email"), (String) user.get("sub"), "study-nonce");
        System.out.println("Validated ID claims: " + IdTokenExample.validate(token, "study-nonce"));
        System.out.println("Scoped UserInfo: " + UserInfoExample.claims(Set.of("openid", "email"), user));
    }
}
