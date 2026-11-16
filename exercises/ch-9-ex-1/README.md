# Chapter 9, exercise 1: binding the redirect URI at token exchange

Run `./gradlew :exercises:ch-9-ex-1:run` or IntelliJ's
`org.oauthinaction.ch09ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize and fetch the resource. Stop other exercises
on ports 9000–9002 first.

`CompletedAuthorizationServer.validRedirect` compares the redirect URI in the
token request to the one stored with the code. Missing/different values produce
`invalid_grant`; the attempted code is consumed, so retrying it with a corrected
URI also fails. `StarterExercise.main()` / `runStarter` deliberately omits that
check in `StarterAuthorizationServer.validRedirect`. Complete that short TODO.

The original supporting client requests OIDC scopes from an AS that only registers
foo/bar. Java requests `foo bar`, making this chapter's flow work. Its resource
also tries to introspect against an endpoint absent from the AS. Java keeps the
same resource APIs (`/resource`, `/words`, `/produce`, `/favorites`) but looks up
the existing shared token file. The focus stays on redirect binding, not adding a
new authorization-server endpoint. User-specific APIs return Unknown where this
chapter's tokens have no user; word/produce permissions remain scope-dependent.

Code/refresh scope handling and explicit error state are retained from chapter 5.
The client exposes the supported authorize/refresh/resource actions; copied
generic UI actions for endpoints absent from this AS are not presented as working
features. Their implementations are retained in the full-server examples.

Individual entry points: authorization variant first, then `ProtectedResource`
and `Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-9-ex-1/tokens.json`. `./gradlew check` verifies
matching/missing/different redirects, attempted-code replay, starter weakness,
full code/refresh flow, authentication/scope errors, token transports and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
