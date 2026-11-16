# Chapter 11, exercise 2: HMAC-signed JWTs

Run `./gradlew :exercises:ch-11-ex-2:run` or IntelliJ's
`org.oauthinaction.ch11ex02.completed.CompletedExercise.main()`. Authorize and
fetch the resource at http://localhost:9000/. Stop other exercises on ports 9000–9002.

`TokenJwt.create` signs the compact header/payload with JDK `HmacSHA256` and the
book's `shared token secret!`. `parse` requires HS256 and compares the HMAC before
returning claims; the resource then checks issuer/audience/time. Expected algorithm
is pinned rather than copied from an untrusted header. Access tokens remain JWTs
after refresh and carry the selected user's stable subject.

`StarterExercise.main()` / `runStarter` leaves unsigned issuance in
`StarterAuthorizationServer` and claim-only acceptance in `StarterResource`.
Complete signing and verification. The starter deliberately accepts forged
unsigned claims, while the completed resource rejects them and payload tampering.
Tests make this difference explicit. Shared-secret fixtures are visible for study;
no Spring Security or JWT framework hides the checks.

Individual entry points: authorization variant, resource variant, `Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-11-ex-2/tokens.json`; the resource
validates JWTs independently of storage. web assets are
local/static. `./gradlew check` covers signed flow/refresh, tampering, alg=none,
claim validation including array audiences, starter weaknesses, auth, replay,
scope behavior, shared records and classpath assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
