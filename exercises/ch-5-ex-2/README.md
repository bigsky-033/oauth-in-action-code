# Chapter 5, exercise 2: issuing and processing refresh tokens

Run `./gradlew :exercises:ch-5-ex-2:run` or IntelliJ's
`org.oauthinaction.ch05ex02.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize and approve, then choose **Refresh token**.
The access token changes while the refresh token remains usable. Fetch the resource
with the new access token. Stop other exercises on ports 9000–9002 first.

Read `CompletedAuthorizationServer.createRefreshToken` and `refresh`. The former
creates a separate refresh record bound to the client. The latter authenticates
the caller, looks up that record and issues a new access token. A refresh token
cannot access the resource. Scope processing is reserved for exercise 3.

`StarterExercise.main()` / `runStarter` has working code flow but leaves refresh
issuance and processing unfinished in `StarterAuthorizationServer`. Implement its
two overrides. Refresh requests return 501 until completed; no refresh token is
issued by its code branch. Supporting code is inherited to keep these TODOs focused.

The client's `/refresh` route uses form-body client credentials as in the original;
code exchange uses HTTP Basic. State is session-local and consumed once; errors
carry state. Missing/unknown credentials or tokens return explicit JSON errors.
Token-endpoint redirect-URI checking remains absent as in the chapter.

Separate processes: run `CompletedAuthorizationServer`/`StarterAuthorizationServer`
first, then `Client` and `ProtectedResource`. Tokens share
`${java.io.tmpdir}/oauth-in-action/ch-5-ex-2/tokens.json`, reset on authorization
startup. Static HTML/JS/CSS is local and resolved from the classpath, not the cwd. Run `./gradlew check` to verify all modules, including
code flow, refresh reuse, token/client separation, explicit browser refresh,
negative authentication, starter gaps, shared storage and asset isolation.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
