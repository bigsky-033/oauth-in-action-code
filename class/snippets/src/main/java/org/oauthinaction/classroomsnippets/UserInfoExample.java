package org.oauthinaction.classroomsnippets;
import java.util.*;
/** Snippet 8: invoke after bearer validation; the caller maps missing user to 404. */
public final class UserInfoExample {
    public static Map<String, Object> claims(Set<String> scopes, Map<String, Object> user) {
        if (!scopes.contains("openid")) throw new IllegalArgumentException("openid required (403)");
        if (user == null) throw new NoSuchElementException("Unknown user (404)");
        var result = new LinkedHashMap<String, Object>(); result.put("sub", user.get("sub"));
        if (scopes.contains("profile")) copy(user, result, List.of("name", "family_name", "given_name", "middle_name", "nickname", "preferred_username", "profile", "picture", "website", "gender", "birthdate", "zoneinfo", "locale", "updated_at"));
        if (scopes.contains("email")) copy(user, result, List.of("email", "email_verified"));
        if (scopes.contains("address")) copy(user, result, List.of("address"));
        if (scopes.contains("phone")) copy(user, result, List.of("phone_number", "phone_number_verified"));
        return result;
    }
    private static void copy(Map<String, Object> user, Map<String, Object> out, List<String> names) {
        for (String name : names) if (user.get(name) != null) out.put(name, user.get(name)); // false is a real value.
    }
    public static void matchSubject(String validatedIdSubject, Map<String, Object> userInfo) {
        if (validatedIdSubject == null || !validatedIdSubject.equals(userInfo.get("sub"))) throw new IllegalArgumentException("UserInfo subject mismatch");
    }
    // See ../complete/Client.userInfo for the actual bearer HTTP call and response handling.
}
