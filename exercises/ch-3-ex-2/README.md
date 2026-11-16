# Chapter 3, exercise 2: refreshing an access token

```sh
./gradlew :exercises:ch-3-ex-2:run
```

Open http://localhost:9000/ and choose **Get protected resource** FIRST. The
initial access token is deliberately invalid. The client receives HTTP 401,
exchanges the preloaded refresh token at `/token`, and redirects to retry the
resource with its new access token. No consent interaction is needed for refresh.
Choose **Get OAuth token** to walk through the authorization-code flow with scope
`foo` instead. Stop other exercises using ports 9000–9002 before running this one.

In IntelliJ Community, import the root Gradle project with Java 21 and run
`org.oauthinaction.ch03ex02.completed.CompletedExercise.main()`. Run
`org.oauthinaction.ch03ex02.StarterExercise.main()` (or the Gradle `runStarter`
task) for the exercise version. Its only unfinished step is refresh: it returns
HTTP 501 after the rejected resource call. The unchanged authorization-code
behavior is inherited from `CompletedClient`; implement the short overridden
`refreshAccessToken` method in `StarterClient`.

Individual processes can run the `main` methods in `AuthorizationServer`,
`ProtectedResource`, and `CompletedClient`/`StarterClient`. Start the authorization
server first; it resets token storage and seeds the demonstration refresh token.

## Read the lesson

- `CompletedClient.fetchResource` sends the access token. On HTTP 401 it calls
  `refreshAccessToken`, which authenticates the client using HTTP Basic and posts
  `grant_type=refresh_token`. It stores the response and retries once.
- `AuthorizationServer.token` handles code exchange and refresh as two explicit
  branches. Refresh verifies that the stored refresh token belongs to the client.
- `TokenStore` distinguishes refresh tokens from access tokens; a refresh token
  cannot be used to call the protected resource.
- `CompletedClient.authorize` requests scope `foo`; consent and status pages show
  granted scopes. Tokens and state are kept in the client's HTTP session.

## Differences and simplifications

As in the book, the refresh token is a preloaded demonstration credential:
`j2r3oj32r23rmasd98uhjrk2o3i`. The invalid initial access token is
`987tghjkiu6trfghjuytrghj`. These are teaching fixtures, not newly issued tokens.
Java seeds the refresh token immediately rather than after a five-second timer.
The authorization-code branch does not issue refresh tokens in this exercise;
the client retains its demonstration refresh token.

The original refresh response omitted scope. Java returns the stored `foo bar`
scope explicitly so its meaning is visible, and keeps the same refresh token.
Unknown refresh tokens receive JSON `invalid_grant` with HTTP 400. The client
only attempts refresh for HTTP 401 and limits automatic retries to one, avoiding
an endless redirect loop if a resource keeps rejecting tokens.

No expiry timer is added: invalid initial credentials exercise recovery
deterministically. Authorization requests/codes stay in memory; token records
use `${java.io.tmpdir}/oauth-in-action/ch-3-ex-2/tokens.json`, with a single writer
and atomic file replacement. The chapter's missing token-endpoint redirect-URI
validation is retained and documented, as in exercise 1. Error callbacks include
state, and Java consumes that state once.

All web pages are static HTML with local CSS and small JavaScript files. Java
serves only namespaced classpath assets, independently of the working directory. Small infrastructure classes are intentionally repeated between exercises to keep
each lesson independently readable; no shared OAuth framework controls the flow.

## Verify

Run `./gradlew check` from the root. Integration tests cover code flow, refresh
and retry, invalid refresh recovery, client authentication, scope, token separation,
starter behavior, file storage, and isolated static resources. Tests use temporary
ports/storage and a different working directory. IntelliJ UI and visual browser
checks remain manual.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
