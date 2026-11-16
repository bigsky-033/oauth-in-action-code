package org.oauthinaction.classroomsnippets;
import java.util.Map;
/** Snippet 6: identity fixtures and the OpenID scope set in ClientServerInfo. */
public final class UserRecords {
    public static final Map<String, Map<String, Object>> USERS = Map.of(
        "alice", Map.of("sub", "9XE3-JI34-00132A", "preferred_username", "alice", "name", "Alice",
                "email", "alice.wonderland@example.com", "email_verified", true),
        "bob", Map.of("sub", "1ZT5-OE63-57383B", "preferred_username", "bob", "name", "Bob",
                "email", "bob.loblob@example.net", "email_verified", false));
}
