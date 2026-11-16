# Chapter 6, exercise 2: client credentials

Run `./gradlew :exercises:ch-6-ex-2:run` or IntelliJ's
`org.oauthinaction.ch06ex02.completed.CompletedExercise.main()`. Open
http://localhost:9000/ and get a token; no consent screen appears. Fetch the
resource. Stop other exercises on ports 9000–9002 first.

`CompletedClient.authorize` requests `grant_type=client_credentials` with scope
`foo bar`; `ClientSupport.requestToken` shows the HTTP Basic authentication and
form encoding. `CompletedAuthorizationServer.clientCredentials` validates scope
and issues an access token for the client itself, without a refresh token. There
is no user, callback or state in this flow. Supporting code/refresh server paths
are retained for comparison and have explicit tests.

`StarterExercise.main()` / `runStarter` leaves TODOs in `StarterClient.authorize`
and `StarterAuthorizationServer.clientCredentials`; both return 501. Run each
starter or completed server/client independently to work on one side at a time.
Start the authorization server first; `ProtectedResource` is already complete.

Storage: `${java.io.tmpdir}/oauth-in-action/ch-6-ex-2/tokens.json`, reset on
authorization startup. Scopes survive code/refresh records (fixing the original
scope-loss bug); the original supporting code branch had no registered redirect
URI, so Java supplies the usual callback to keep that branch usable. This does not
affect client credentials. Static HTML/JS/CSS replaces templates. Original JS is
retained. `./gradlew check` tests scoped/no-refresh issuance, authentication/errors,
client/resource flow, starter gaps, supporting code/refresh, and static isolation.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
