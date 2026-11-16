# Chapter 13, exercise 1: OpenID Connect

Run `./gradlew :exercises:ch-13-ex-1:run` or IntelliJ's
`org.oauthinaction.ch13ex01.completed.CompletedExercise.main()`. At
http://localhost:9000/, authorize as Alice/Bob, grant `openid` and desired claim
scopes, then inspect ID claims and choose **Get UserInfo**. Stop other exercises
on ports 9000–9002 first.

The AS only issues an RS256 ID token when `openid` is granted. Its audience is the
client and subject is the selected user's stable ID; the access token stays opaque
and is used at the resource's `/userinfo`. `CompletedClient.validateIdToken`
checks signature, issuer, audience (string or array), issued-at, expiry and nonce.
The Java client supplies a nonce to make the server's existing optional nonce path
observable. UserInfo subject must match the validated ID token before display.

`CompletedResource.userInfo` requires a valid token and openid scope, resolves
the user, and filters profile/email claims by granted scopes. False values such
as Bob's email_verified are preserved (the original accidentally omitted them).
Phone/address scopes add nothing because the fixtures contain no such claims.
This source variant has code flow only: no refresh grant is silently added.

`StarterExercise.main()` / `runStarter` leaves three identity gaps: ID issuance in
AS, ID validation/UserInfo call in client, and UserInfo endpoint in resource.
Normal OAuth access remains separate from accepted identity. Complete the short
starter overrides using the adjacent completed classes as references.

Individual entry points: authorization variant, resource variant, client variant. Storage: `${java.io.tmpdir}/oauth-in-action/ch-13-ex-1/tokens.json`, with separate
resource readers. The original key is a namespaced local fixture; `./gradlew check` covers full OIDC flow,
conditional issuance, scoped/false claims, signature/claim/nonce rejection,
UserInfo subject mismatch and 401/403/404 cases, starters, state and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
