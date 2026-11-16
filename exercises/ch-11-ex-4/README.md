# Chapter 11, exercise 4: token introspection

Run `./gradlew :exercises:ch-11-ex-4:run` or IntelliJ's
`org.oauthinaction.ch11ex04.completed.CompletedExercise.main()`. Authorize and
fetch the resource at http://localhost:9000/. Stop other exercises on ports 9000–9002.

The AS issues opaque tokens again. `CompletedResource.inspect` sends each token
to `/introspect` with the protected resource's own HTTP Basic credentials. The
resource only accepts `active=true`; it has no token-file access. The AS authenticates
that caller, looks up the record and returns scope/client/subject/user metadata,
or `active=false`. Unknown tokens are not a server error. No cache obscures changes.

`StarterExercise.main()` / `runStarter` leaves TODOs in the AS's introspection
handler (501) and the resource's HTTP call/result check (currently rejects tokens).
Code/consent/refresh and user/scope records remain working supporting behavior.
Individual entry points: authorization variant first, resource variant, `Client`.
Storage: `${java.io.tmpdir}/oauth-in-action/ch-11-ex-4/tokens.json` is AS-only.

`./gradlew check` verifies real
HTTP introspection, resource authentication, metadata/active/inactive behavior,
immediate invalidation after AS storage reset, starter gaps and complete
code/refresh/auth/scope/transport behavior.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
