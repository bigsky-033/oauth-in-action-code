# Chapter 10, exercise 1: PKCE

Run `./gradlew :exercises:ch-10-ex-1:run` or IntelliJ's
`org.oauthinaction.ch10ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize, approve and fetch the resource. Stop other
exercises on ports 9000–9002 first.

`CompletedClient.pkceParameters` generates/stores a 64-character verifier and sends
the base64url SHA-256 challenge with method S256. Callback sends the verifier to
the token endpoint. `CompletedAuthorizationServer.validPkce` compares it with the
challenge/method bound to the code. Both `plain` and `S256` are visible; invalid,
missing or unsupported values reject the exchange, consuming the attempted code.
`Pkce.challenge` shows the JDK SHA-256/base64url transformation directly.

`StarterExercise.main()` / `runStarter` omits the challenge and verifier verification
in `StarterClient`/`StarterAuthorizationServer`. Complete these two short TODOs.
As in the original solution, PKCE is checked only when requested; it is not made
mandatory for legacy no-challenge requests. This independent baseline does not
silently include chapter 9 exercise 1's redirect-binding check.

Supporting client scopes are aligned with the AS's foo/bar registration. The
resource keeps its original API surface but uses shared-file lookup because this
AS has no introspection endpoint; see chapter 9's supporting-source repairs. Individual entry points: authorization variant first, resource and client variant. Storage: `${java.io.tmpdir}/oauth-in-action/ch-10-ex-1/tokens.json`. `./gradlew check` verifies the RFC S256
vector, plain/S256 success, invalid verifier/method, replay, starter omissions,
full client flow and existing code/refresh/auth/scope behavior.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
