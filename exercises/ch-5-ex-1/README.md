# Chapter 5, exercise 1: building the authorization server

Run `./gradlew :exercises:ch-5-ex-1:run` or IntelliJ's
`org.oauthinaction.ch05ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize, approve, and fetch the resource. Stop other
exercises using ports 9000–9002 first.

The TODOs are now in `StarterAuthorizationServer`: register a client, handle
authorization/consent, authenticate token requests, consume a code once and return
an access token. Run `StarterExercise.main()` / Gradle `runStarter` to try the
starter; these three handlers return 501 until implemented. The solution is in
`completed/CompletedAuthorizationServer`. Supporting `Client` and
`ProtectedResource` already work.

This exercise intentionally implements only authorization-code grants, without
refresh tokens or scope processing (the next two exercises add those). State is
remembered per client session and consumed once; errors include state. Client
registration/redirects and HTTP Basic/body authentication are checked explicitly.
Token-endpoint redirect-URI checking is not added ahead of its later lesson.

Individual entry points: `CompletedAuthorizationServer`/`StarterAuthorizationServer`
first, then `Client` and `ProtectedResource`. Tokens share
`${java.io.tmpdir}/oauth-in-action/ch-5-ex-1/tokens.json`; authorization startup
clears it, and atomic file replacement supports separate JVMs. Pages are static
HTML with local JS/CSS.

Run `./gradlew check` at the root for code-flow, denial/state, registration,
authentication, code replay, token transport, starter, storage and asset tests.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
