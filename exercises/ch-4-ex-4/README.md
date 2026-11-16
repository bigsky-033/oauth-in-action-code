# Chapter 4, exercise 4: user-specific favorites

Run `./gradlew :exercises:ch-4-ex-4:run` or IntelliJ's
`org.oauthinaction.ch04ex04.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize as Alice or Bob, then choose **Get favorites**.
Authorize again with the other user and compare the result. Stop other exercises
on ports 9000–9002 first. The consent user picker is a teaching fixture, not login.

`CompletedResource.forUser` chooses data using the user associated with the
access token, not a user supplied in the resource request. A valid token for an
unknown/no user returns `Unknown` with empty arrays; an invalid token returns 401.
This exercise does not scope-filter favorites: its lesson is binding data to the
user who approved the token. The original code compared a user object to a string;
Java consistently stores the selected user key to make the intended behavior work.

`StarterExercise.main()` / `runStarter` still returns Unknown for every valid
token. Implement `StarterResource.forUser`. Other request/lookup behavior is
inherited, keeping the TODO focused on this exercise.

Separate processes: `AuthorizationServer` first, then `Client` and
`CompletedResource`/`StarterResource`. Shared storage is
`${java.io.tmpdir}/oauth-in-action/ch-4-ex-4/tokens.json`. Supporting grants,
static-page approach, session state, original bug fixes and absent token-endpoint
redirect checking are the same as exercise 1, with `movies foods music` scopes. Run `./gradlew check` for all modules; tests check Alice/Bob/unknown, client-only tokens, the full client flow, the starter
gap, authorization errors/replay, shared storage, and classpath assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
