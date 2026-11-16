# Chapter 4, exercise 3: filtering produce by scope

Run `./gradlew :exercises:ch-4-ex-3:run` or IntelliJ's
`org.oauthinaction.ch04ex03.completed.CompletedExercise.main()`. Open
http://localhost:9000/, request a token, uncheck some categories on the consent
page, then choose **Get produce**. Stop other exercises on ports 9000–9002 first.

`CompletedResource.produce` requires a valid bearer token, then filters fruit,
veggies, and meats independently. A missing category scope gives an empty array,
not a 403 response. Compare this to the operation-level scope checks in exercise 2.
The client proxies GET `/produce` and shows the result as JSON in a static page.

`StarterExercise.main()` / `runStarter` retains the original starter's weakness:
it authenticates tokens but returns all categories regardless of scope. Implement
`StarterResource.allowed`. Supporting behavior is inherited to keep the TODO small.

Separate processes: `AuthorizationServer` first, then `Client` and
`CompletedResource`/`StarterResource`. Tokens share
`${java.io.tmpdir}/oauth-in-action/ch-4-ex-3/tokens.json`, reset at authorization
server startup. Code, implicit, refresh, client-credentials, and Carol's password
grant remain present. Registered/requested scopes are `fruit veggies meats`. The same fixes to undefined JS variables, user record shape, refresh scope/user,
and error state as in exercise 1 apply. Token-endpoint redirect checking remains
absent, matching this chapter. No template engine, external database, CDN or Node
is required.

Run `./gradlew check`. Tests exercise all eight category-scope combinations,
missing/unknown tokens, consent narrowing through the client, the starter's
unfiltered behavior, authorization error cases, replay, shared storage and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
