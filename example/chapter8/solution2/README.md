# Chapter 8 solution 2: HSTS response header

Run `./gradlew :example:chapter8:solution2:run` or
`org.oauthinaction.examplesolution2.Solution.main()`. Open http://localhost:9000/.
Individual `ProtectedResource3.main()` can replace the baseline resource while
chapter8's AS/client run. Stop other examples on ports 9000–9002 first.

The resource keeps authenticated introspection, JSON/content headers, encoded
unknown input and CORS, then adds `Strict-Transport-Security: max-age=31536000`.
As in the book, the teaching launcher still uses HTTP: the header does not create
TLS and a browser only learns HSTS over HTTPS. Tests verify header emission.

Supporting chapter8 servers/HTTP helpers are a Gradle dependency; response policy
and static namespace are local. Token storage remains the parent chapter8 file.
`./gradlew check` verifies HSTS, CORS preflight/response, content and invalid tokens.

Original JavaScript/template history and migration decisions: [migration record](../../../MIGRATION.md).
