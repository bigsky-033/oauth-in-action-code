# Classroom: completed OpenID Connect flow

Run `./gradlew :class:complete:run` or
`org.oauthinaction.classroomcomplete.Exercise.main()`. Open http://localhost:9000/,
authorize as Alice, Bob or Carol with openid and desired claim scopes, then get UserInfo.
Stop other examples on ports 9000–9002. Separate entry points: `AuthorizationServer`,
`ProtectedResource`, `Client`.

Read Client.authorize/callback, AuthorizationServer.authorize/approve/token, then
Client.validateIdToken/userInfo and ProtectedResource.userInfo. Code/state are
single-use; confidential client exchange uses Basic authentication. Access tokens
are opaque; the source's commented JWT access-token experiment is in ../snippets.

ID tokens are signed with RS256 using the local teaching fixture. The client checks
signature, issuer, string/array audience, issue/expiry times and the supplied nonce.
UserInfo lives at the resource server (9002), requires openid, filters profile/email
claims by scope and must match the validated subject. Bob's false email_verified
is preserved. Phone/address scopes have no claims in the source fixtures. There
is no refresh grant in this stage. Token-endpoint redirect binding remains absent
as in the classroom source; chapter 9 teaches that check.

The authorization server resets its JSON file under
`${java.io.tmpdir}/oauth-in-action/complete/` at startup; independent resource
processes read it. `./gradlew check` verifies the full flow and identity rejection
cases over HTTP, with isolated ports/files and working directories.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
