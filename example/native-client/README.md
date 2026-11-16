# Standalone native client

Run `./gradlew :example:native-client:run` or
`org.oauthinaction.examplenative.Exercise.main()` in IntelliJ. Open
http://localhost:9000/; one exercise may use ports 9000–9002 at a time.

The Cordova shell becomes a Java desktop client with plain local HTML and a
system-browser loopback callback. `DesktopClient` sends native-client-1 /
oauth-native-secret-1 in the token form, checks single-use session state, and
requests the original OpenID/profile/email/address/phone scopes. ID tokens are
issued by the supporting server but this client does not use them to log in.
This retains the book's confidential native fixture; PKCE is taught separately.

The local authorization/resource servers retain the full main example grant and
API set. Their native fixture also allows foo/bar for the supporting grant tests.
Separate entry points: `AuthorizationServer`, `ProtectedResource`, `DesktopClient`.
The authorization server resets `${java.io.tmpdir}/oauth-in-action/native-client/tokens.json`
on startup. No phone SDK or Node runtime is needed.

`./gradlew check` verifies real loopback code/state exchanges, authenticated HTTP
introspection, refresh, revocation, all grant paths, registration, signed ID tokens,
scoped UserInfo and resource APIs. Browser/IDE interaction is a manual check.
See the root migration record for original-source history and explicit repairs.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
