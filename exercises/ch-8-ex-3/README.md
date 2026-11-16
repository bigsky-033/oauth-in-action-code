# Chapter 8, exercise 3: the transport-security header

Run `./gradlew :exercises:ch-8-ex-3:run` or IntelliJ's
`org.oauthinaction.ch08ex03.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize and fetch a greeting. Stop other exercises
on ports 9000–9002 first.

`CompletedResource.response` adds `Strict-Transport-Security: max-age=31536000`
alongside the previous content headers, JSON response and CORS support. The
otherwise complete `StarterResource` leaves only HSTS out; add the header there.
Run `StarterExercise.main()` / `runStarter` to compare response headers.

As in the original, these teaching servers still listen on HTTP. An HSTS response
header does not create TLS, and a browser only learns HSTS over HTTPS. This exercise
verifies the header being emitted, not an HTTPS deployment. The original's legacy
X-XSS-Protection header is retained for book comparison.

The supporting client/server, ancillary routes, fixture scopes and explicit bug
fixes are the same as exercise 1. Individual entry points: `AuthorizationServer`
first, then resource variant and `Client`. Storage:
`${java.io.tmpdir}/oauth-in-action/ch-8-ex-3/tokens.json`. Run `./gradlew check` for HSTS/starter differences,
content headers/languages, token errors/precedence and full client flow/revocation.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
