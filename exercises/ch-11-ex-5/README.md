# Chapter 11, exercise 5: token revocation

Run `./gradlew :exercises:ch-11-ex-5:run` or IntelliJ's
`org.oauthinaction.ch11ex05.completed.CompletedExercise.main()`. Authorize and
fetch data at http://localhost:9000/, then revoke the access token. Stop other
exercises on ports 9000–9002 first.

`CompletedClient.revoke` authenticates to the revocation endpoint and clears its
local token state after success. The AS reuses explicit client authentication and
removes only a matching access token belonging to that client. Unknown/already
revoked/other-owner tokens all produce the same 204 result. This book exercise
revokes access tokens, not refresh grants; an existing refresh grant can still
issue a new access token.

`StarterExercise.main()` / `runStarter` leaves revocation TODOs on client and AS
(501). Resource introspection is already implemented. The original resource calls
an introspection endpoint missing from this exercise's AS; Java retains the previous
exercise's endpoint so the main flow and immediate revocation effect work.

Individual entry points: authorization variant, `ProtectedResource`, client variant. Only AS reads `${java.io.tmpdir}/oauth-in-action/ch-11-ex-5/tokens.json`; the resource
uses HTTP introspection. `./gradlew check` covers owner/authentication, idempotence,
refresh retention, immediate resource rejection, client state clearing, starters,
introspection and existing grant/error/transport behavior.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
