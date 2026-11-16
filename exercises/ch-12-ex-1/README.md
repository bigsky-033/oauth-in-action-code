# Chapter 12, exercise 1: dynamic client registration

Run `./gradlew :exercises:ch-12-ex-1:run` or IntelliJ's
`org.oauthinaction.ch12ex01.completed.CompletedExercise.main()`. At
http://localhost:9000/, authorize: the initially unregistered client first POSTs
JSON metadata, saves issued credentials, then begins normal code/consent flow.
Stop other exercises on ports 9000–9002 first.

`CompletedClient.register` and the AS's `register`, `metadataError`, `normalize`
show the protocol directly. Defaults are code flow and client_secret_basic; public
clients use `none` and receive no secret. The book's secret_basic/secret_post aliases
are accepted and normalized. Client IDs/secrets are newly generated, and registration
is reused for the client session. Only code and refresh grants exist here.

`StarterExercise.main()` / `runStarter` leaves client and AS registration as 501
TODOs. Code exchange and resource access are already implemented. Java fixes the
original authentication-name/secret-generation and missing redirect-property bugs.
The client explicitly registers refresh permission so its refresh action works.
Management endpoints are introduced in the next exercise, not hidden in this one.

The original resource expects an introspection endpoint absent from this AS. Java
keeps its resource APIs but uses the shared token file instead. Individual entry
points: authorization variant first, `ProtectedResource`, client variant. Storage:
`${java.io.tmpdir}/oauth-in-action/ch-12-ex-1/tokens.json`. Registrations are in
memory, reset on AS restart.

`./gradlew check` covers empty registry, metadata defaults/invalid values, public
and secret clients, unique IDs, dynamic browser flow/reuse/refresh, client/code
binding, replay, starter gaps and classpath assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
