# Chapter 3, exercise 1: authorization-code client

## Run from the repository root

```sh
./gradlew :exercises:ch-3-ex-1:run
```

Open http://localhost:9000/. Choose **Get OAuth token**, approve the request,
then choose **Get protected resource**. Denying consent returns an error to the
client. Stop the running exercise before starting another on the same ports.

In IntelliJ Community, import the ROOT as a Gradle project, select JDK 21 for
both project SDK and Gradle JVM, then run `CompletedExercise.main()` using the
gutter button. This starts all three servers. No Spring IDE integration is needed.

Run `StarterExercise.main()` (or `./gradlew :exercises:ch-3-ex-1:runStarter`)
for the exercise version. It compiles and serves pages, but its three TODO routes
return HTTP 501 until implemented. Look at `completed/` for the answer.

For separate processes, run the `main` methods in `AuthorizationServer`,
`ProtectedResource`, and `CompletedClient` (or `StarterClient`). Start the
authorization server first: it clears the token file on startup, as in the book.

## Read the flow

1. `CompletedClient.authorize`: remember a random state and redirect the browser.
2. `AuthorizationServer.authorize` / `approve`: check registration, collect consent,
   save a code, and redirect back with code and state.
3. `CompletedClient.callback`: compare state, then POST the code to `/token` using
   HTTP Basic client authentication. The token exchange is Java-to-Java HTTP;
   the browser never performs it.
4. `AuthorizationServer.token`: authenticate the client, consume the code once,
   and save/return an opaque access token.
5. `CompletedClient.fetchResource`: send the token in a Bearer header.
6. `ProtectedResource.resource`: look up the token before returning data.

The HTML is static. Small browser scripts display same-origin JSON using
`textContent`. No template engine, frontend build, CDN, or Node installation is
needed for the Java version. Java only serves assets under `src/main/resources/web/ch03ex01/`.

## State and intentional simplifications

Client state/tokens live in an HTTP session rather than process-wide variables,
so different browsers do not overwrite each other's authorization state. Each
server role uses its own cookie name because cookies are not isolated by port.
One authorization attempt per client session is supported.

Authorization requests and codes live in memory. The shared NoSQL file becomes
`${java.io.tmpdir}/oauth-in-action/ch-3-ex-1/tokens.json`. The authorization server
is the only writer; the resource server reads the same file even in a separate JVM.
Atomic replacement keeps readers from seeing partially written JSON. There is no
database service, login system, token expiry, or refresh flow in this exercise.

The Java server preserves this chapter's lack of redirect-URI validation at the
TOKEN endpoint. It validates redirect URIs at the authorization endpoint, but does
not add later chapters' checks here. Body/query access-token transport and the
simple consent model are also retained for comparison with the book.

Small differences from the original: denial/error redirects include state, the
client checks it on all callbacks, and consumes it once. Invalid Basic credentials
produce a controlled error. Unknown response types fail before showing consent.
Protocol JSON field names and the key `/authorize`, `/callback`, `/token`,
`/approve`, and `/resource` routes remain visible.

## Verify

```sh
./gradlew check
```

This runs real HTTP integration tests on temporary ports and files, plus root
inventory/module/dependency/resource checks. HTML and asset responses are tested;
visual rendering and IntelliJ's actual UI require a manual check.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
