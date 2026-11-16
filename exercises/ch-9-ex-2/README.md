# Chapter 9, exercise 2: local authorization-request errors

Run `./gradlew :exercises:ch-9-ex-2:run` or IntelliJ's
`org.oauthinaction.ch09ex02.completed.CompletedExercise.main()`. Normal code flow
starts at http://localhost:9000/. Stop other exercises on ports 9000–9002 first.

`CompletedAuthorizationServer.invalidRequestedScope` returns a local 400 JSON
`invalid_scope` error without redirecting the browser. `StarterExercise.main()` /
`runStarter` retains the original redirect behavior in that one override. Request
an unregistered scope directly at `/authorize` to compare them; valid `foo bar`
requests still show consent.

The original solution accidentally compares scopes to a string and refers to an
undefined redirect during consent errors. Java compares lists and uses the stored
authorization request. This exercise is an independent book baseline: it does not
include exercise 1's token-endpoint redirect binding. Its supporting scope/refresh
flow and fixes to the mismatched client scope/absent introspection dependency are
the same as exercise 1, using direct shared-file resource token lookup.

Individual entry points: authorization variant first, `ProtectedResource`, `Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-9-ex-2/tokens.json`. Static assets
are local; `./gradlew check` covers local-vs-redirect errors,
valid compound scopes, consent errors/state, complete code/refresh/auth behavior,
replay, starter differences, storage and resources.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
