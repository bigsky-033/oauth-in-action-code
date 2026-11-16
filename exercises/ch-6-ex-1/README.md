# Chapter 6, exercise 1: implicit grant

Run `./gradlew :exercises:ch-6-ex-1:run` or IntelliJ's
`org.oauthinaction.ch06ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize, approve some scopes, and fetch the resource.
Stop other exercises on ports 9000–9002 first.

Here the client runs in the browser. Java's `Client` serves the static index for
`/` and `/callback`; browser `client.js` sends `response_type=token` and parses
the fragment using `parseCallback`. URL fragments are not sent over HTTP. The
authorization server's `implicit` method saves an opaque token and redirects with
access_token/token_type/scope/state in the fragment, without code exchange or a
refresh token. The browser calls the resource directly; its CORS support permits it.

`StarterExercise.main()` / `runStarter` leaves only implicit issuance unfinished in
`StarterAuthorizationServer.implicit` (501). Supporting code and refresh grants
remain implemented, as in the original. No password/client-credentials branches
are added ahead of their exercises.

Browser state is generated using `crypto.getRandomValues`, stored in sessionStorage
and consumed on callback. The parser checks state before accepting a token, handles
errors, and removes the fragment from the address bar. This replaces the original's
weak Math.random and ad hoc splitting without moving browser protocol work to Java.
For consistency with the original supporting server, error callbacks can use query
parameters; the client validates their state too. This is the book's historical
implicit lesson, not a silent conversion to another grant.

Separate entry points: authorization server first, `ProtectedResource`, then
`Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-6-ex-1/tokens.json`, reset
on authorization startup. All assets are local/static. Run `./gradlew check`; tests verify implicit fragment/type/state/scope, no refresh,
resource access, callback page, starter gap, and supporting code/refresh behavior.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
