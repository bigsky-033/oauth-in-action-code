# Chapter 12, exercise 2: registration management

Run `./gradlew :exercises:ch-12-ex-2:run` or IntelliJ's
`org.oauthinaction.ch12ex02.completed.CompletedExercise.main()`. Authorize at
http://localhost:9000/, then read/update/delete the registration using the page's
controls. Authorizing after deletion registers a new client. Stop other exercises
on ports 9000–9002 first.

Registration returns a management URI and registration bearer token, distinct from
the client secret used at the token endpoint. The client's `/read_client`,
`/update_client` and `/unregister_client` call GET/PUT/DELETE there. AS checks that
bearer, preserves issued IDs/secrets/management fields on metadata replacement,
and removes all of that client's tokens on deletion. The client clears its local
registration/token state too. Missing/wrong/unknown management credentials produce
401/403/404 respectively. The URI uses the actual request origin, so temporary
test ports and normal standalone launchers both work.

`StarterExercise.main()` / `runStarter` leaves these management operations as
explicit TODOs on client and AS, replacing the original starter's undefined
variables with compilable 501 handlers. Registration and code flow already work.
The original credential-regeneration/update bug is repaired without hiding the
metadata processing. One fixed foo/bar client is seeded as in this source variant;
the normal client still registers its own credentials dynamically.

Individual entry points: authorization variant, `ProtectedResource`, client variant. Storage: `${java.io.tmpdir}/oauth-in-action/ch-12-ex-2/tokens.json`. The same
shared-file resource repair and registration metadata/auth-name fixes as exercise 1
apply. `./gradlew check` covers the
full lifecycle, auth/status codes, immutable credentials, deletion invalidation,
browser re-registration, starter gaps and earlier registration/code-flow cases.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
