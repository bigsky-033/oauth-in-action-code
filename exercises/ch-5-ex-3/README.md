# Chapter 5, exercise 3: carrying granted scopes through the protocol

Run `./gradlew :exercises:ch-5-ex-3:run` or IntelliJ's
`org.oauthinaction.ch05ex03.completed.CompletedExercise.main()`. Open
http://localhost:9000/, request `foo bar`, uncheck one scope at consent, then fetch
the resource. The token response/status and resource JSON show the granted scope.
The **Refresh token** action retains it. Stop other exercises on ports 9000–9002.

Read `CompletedAuthorizationServer.authorize`, `grantedScopes`, `token` and
`refresh`: registered scopes bound what may be requested/approved; the code binds
the selection to access/refresh records. Refresh keeps the grant's scope unless
the request narrows it. This implements the book's refresh-scope bonus: an attempt
to enlarge scope returns `invalid_scope`. Narrowing one access token does not
change the underlying reusable refresh grant.

`StarterExercise.main()` / `runStarter` deliberately omits scope processing while
leaving code/refresh flow intact. Implement the three short overrides in
`StarterAuthorizationServer`; the answer remains readable in `completed/`.
The resource endpoint returns `{resource, scope}` as in the original exercise.

Separate processes: authorization server first, then `Client` and
`ProtectedResource`. Shared file:
`${java.io.tmpdir}/oauth-in-action/ch-5-ex-3/tokens.json`, reset on authorization
startup. State is session-local, error callbacks carry state, and assets are static
HTML/JS/CSS. Token-endpoint redirect checking is
still absent as in this chapter. The original refresh scope-loss bug is resolved
by explicitly retaining/narrowing the saved grant.

Run `./gradlew check` for the entire repository. Tests cover authorization/consent
scope validation, scope at the resource, refresh retention/narrowing/enlargement,
the starter omission, code flow/errors/replay, authentication, token separation,
storage reset and classpath assets on temporary ports/files.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
