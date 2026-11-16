# Chapter 4, exercise 1: protecting a resource

Run `./gradlew :exercises:ch-4-ex-1:run` from the root, or run
`org.oauthinaction.ch04ex01.completed.CompletedExercise.main()` in IntelliJ.
Open http://localhost:9000/, obtain a token, approve access, then fetch the resource.
Run one exercise at a time (ports 9000–9002).

The starter is `StarterExercise.main()` / Gradle `runStarter`. The client and
authorization server work, but the resource endpoint returns HTTP 501 until you
complete `StarterResource.resource`. The answer is `completed/CompletedResource`:
extract the bearer token, look up its record, and return data or HTTP 401.

Individual entry points: `Client`, `AuthorizationServer`, and `CompletedResource`
(or `StarterResource`). Start the authorization server first; it resets the shared
`${java.io.tmpdir}/oauth-in-action/ch-4-ex-1/tokens.json` file. The resource server
only reads this file; refresh records never qualify as access tokens.

The supporting authorization server retains all original grant branches: code,
implicit (token in fragment), client credentials, Carol's password credentials
(`clewis` / `user password!`), and refresh. No scopes are registered in this exercise.
The normal client demonstrates code flow and displays access/refresh tokens. It
does not automatically refresh here. Consent uses selectable demonstration users,
not login. State is session-local and consumed once.

The original has undefined `query`/`clientId` references in supporting branches,
a missing client redirect property, inconsistent user shapes, and refresh records
that lose their user/scope. Java uses explicit records and preserves user/scope
on refresh. Password/invalid-refresh failures return JSON `invalid_grant` (400). Errors return state. Token-endpoint redirect-URI checking remains absent as in the
chapter; later validation lessons are not silently added. HTML is static and uses
same-origin JSON requests. All assets are local.

Verification: `./gradlew check` runs all migrated modules. This exercise tests the
whole code flow, every supporting grant, invalid registration/credentials, replay,
bearer transports and precedence, refresh-token rejection, CORS preflight, starter
behavior, shared-file reset and static-role isolation on temporary ports/files.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
