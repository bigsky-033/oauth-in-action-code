package org.oauthinaction.classroomsnippets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.oauthinaction.classroomcomplete.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SnippetsTest {
    @TempDir Path directory;
    @Test void requestsExposeEncodingStateAndBasicAuthentication() {
        var attempt = AuthorizationRequest.begin("foo bar");
        var query = Forms.query(attempt.location());
        AuthorizationRequest.validate(query, Set.of("foo", "bar"));
        assertEquals(attempt.state(), query.get("state")); assertEquals("foo bar", query.get("scope"));
        var invalid = new HashMap<>(query); invalid.put("redirect_uri", "https://attacker.example/");
        assertThrows(IllegalArgumentException.class, () -> AuthorizationRequest.validate(invalid, Set.of("foo", "bar")));
        assertThrows(IllegalArgumentException.class, () -> TokenRequest.exchange("code", attempt.state(), "wrong"));
        var request = TokenRequest.exchange("code", attempt.state(), attempt.state());
        assertEquals("POST", request.method()); assertEquals(Forms.basic(ClientServerInfo.CLIENT_ID, ClientServerInfo.CLIENT_SECRET), request.headers().firstValue("Authorization").orElseThrow());
    }
    @Test void bearerLookupAndUnknownTokensRemainSeparateFromIdentity() {
        var tokens = new TokenStore(directory.resolve("tokens.json")); tokens.clear();
        tokens.save("known", new TokenStore.Token("client", List.of("foo"), "bob", false));
        assertNotNull(FetchResource.lookup(tokens, "bEaReR known", Map.of()));
        assertNotNull(FetchResource.lookup(tokens, null, Map.of("access_token", "known")));
        assertNull(FetchResource.lookup(tokens, "Bearer unknown", Map.of()));
        assertEquals("Bearer known", FetchResource.request("known").headers().firstValue("Authorization").orElseThrow());
    }
    @Test void signedAccessAndIdTokensHaveDifferentAudiences() {
        String access = JwtExample.issue("subject"); assertEquals("http://localhost:9002/", Jwt.verify(access).path("aud").asText());
        assertThrows(IllegalArgumentException.class, () -> IdTokenExample.validate(access, null));
        assertNull(IdTokenExample.issue(Set.of("email"), "subject", null));
        String id = IdTokenExample.issue(Set.of("openid"), "subject", "nonce");
        assertEquals("subject", IdTokenExample.validate(id, "nonce").path("sub").asText());
        assertThrows(IllegalArgumentException.class, () -> IdTokenExample.validate(id, "wrong"));
        assertThrows(IllegalArgumentException.class, () -> IdTokenExample.validate(id + "tamper", "nonce"));
    }
    @Test void userInfoKeepsFalseClaimsAndMustMatchIdSubject() {
        var user = UserRecords.USERS.get("bob"); var info = UserInfoExample.claims(Set.of("openid", "email"), user);
        assertEquals(false, info.get("email_verified")); assertFalse(info.containsKey("name"));
        UserInfoExample.matchSubject((String) user.get("sub"), info);
        assertThrows(IllegalArgumentException.class, () -> UserInfoExample.matchSubject("other", info));
        assertThrows(IllegalArgumentException.class, () -> UserInfoExample.claims(Set.of("email"), user));
        assertThrows(NoSuchElementException.class, () -> UserInfoExample.claims(Set.of("openid"), null));
    }
}
