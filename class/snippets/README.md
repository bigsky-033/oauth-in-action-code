# Classroom protocol snippets

Run `./gradlew :class:snippets:run` or
`org.oauthinaction.classroomsnippets.SnippetDemo.main()` in IntelliJ. This prints
an authorization URL and locally validated ID/UserInfo claims; it opens no server
and uses no ports. The original directory contained fragments, not an application.

The eight Java files map to the original numbered snippets:

1. ClientServerInfo: matching registration and endpoints.
2. AuthorizationRequest: state, URL construction, registration/scope validation.
3. TokenRequest: state check and Basic/form code exchange.
4. FetchResource: bearer HTTP request and stored-token lookup.
5. JwtExample: RS256 access token with resource audience.
6. UserRecords: identity records and the configured OpenID scope set.
7. IdTokenExample: conditional issuance and explicit claim/nonce validation.
8. UserInfoExample: scoped claims, false-value handling and subject matching.

Runnable consent/code lifecycle and HTTP response handling live in ../complete.
These snippets reuse only its encoding, token-file and JDK RSA helpers through
a Gradle dependency. JWT signing/verification is visible in that module's Jwt.java;
there is no OAuth/security framework. The malformed original ID-token fragment is
repaired, algorithm selection is fixed to RS256, and false claims are preserved.
`./gradlew check` tests requests/state, token lookup, distinct JWT audiences and
UserInfo filtering/subject checks. Original source history is recorded at the root.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
