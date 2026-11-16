# Chapter 6, exercise 3: resource owner password grant

Run `./gradlew :exercises:ch-6-ex-3:run` or IntelliJ's
`org.oauthinaction.ch06ex03.completed.CompletedExercise.main()`. Open
http://localhost:9000/, get a token, submit the demo credentials, then fetch the
resource. Stop other exercises on ports 9000–9002 first.

`CompletedClient.usernamePassword` builds the password-grant form.
`ClientSupport.requestToken` sends it with HTTP Basic client authentication.
`CompletedAuthorizationServer.password` separately checks user credentials and
scope before issuing access and refresh tokens. Client credentials are not the
resource owner's credentials. The book's fixtures are Alice / `password`, Bob /
`this is my secret password`, and Carol / `user password!`. No consent redirect
or state is needed for this grant.

`StarterExercise.main()` / `runStarter` leaves just the password client request
and authorization grant processing as 501 TODOs in `StarterClient` and
`StarterAuthorizationServer`. Existing code/refresh server branches remain; no
client-credentials branch is introduced into this exercise.

Individual entry points: authorization server first, `ProtectedResource`, and
`CompletedClient`/`StarterClient`. Storage:
`${java.io.tmpdir}/oauth-in-action/ch-6-ex-3/tokens.json`, reset on authorization
startup. Java retains scope across refresh, fixes the supporting missing redirect
registration, and uses static local HTML/JS/CSS. This keeps
the historical password-grant lesson visible rather than replacing it with a
different flow. Run `./gradlew check` for form/client/resource behavior, all users,
invalid credentials/scopes, token authentication, starter gaps, supporting grants
and static assets on temporary ports/files.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
