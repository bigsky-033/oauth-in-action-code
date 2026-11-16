# Chapter 6, exercise 4: complete browser client

Run `./gradlew :exercises:ch-6-ex-4:run` or IntelliJ's
`org.oauthinaction.ch06ex04.Exercise.main()`. Open http://localhost:9000/, authorize,
approve selected scopes, then fetch the resource. Stop other exercises using
ports 9000–9002 first. This original variant has no unfinished exercise/solution
split, so all Java is under `src/main/java`.

Java's `Client` serves static HTML for `/` and `/callback`. Browser `client.js`
constructs the implicit request, validates state and reads the token fragment,
then calls the resource directly. The Java server's implicit branch returns
access_token/token_type/scope/state with no code exchange or refresh token.
The fragment is browser-only data, not a Java request parameter. The supporting
code and refresh branches remain available.

This uses the same explicit sessionStorage/crypto/URLSearchParams approach and
error-state handling as exercise 1. The original's weak browser randomness,
ad hoc parsing and refresh scope-loss are corrected and documented there.
Token-endpoint redirect validation remains absent, matching this chapter.

Individual entry points: `AuthorizationServer` first, then `ProtectedResource`
and `Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-6-ex-4/tokens.json`,
reset on authorization startup. Assets are local/static; Run `./gradlew check` for fragment, scope, callback, resource and supporting-grant
tests. Optional developer check: `node tools/test_browser_clients.mjs` tests the
actual fragment parser (Node is not needed to run the Java exercise).

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
