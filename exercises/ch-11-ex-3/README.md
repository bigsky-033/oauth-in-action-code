# Chapter 11, exercise 3: RSA-signed JWTs

Run `./gradlew :exercises:ch-11-ex-3:run` or IntelliJ's
`org.oauthinaction.ch11ex03.completed.CompletedExercise.main()`. Authorize and
fetch the resource at http://localhost:9000/. Stop other exercises on ports 9000–9002.

`TokenJwt.create` signs header/payload using JDK SHA256withRSA and the source's
private teaching key. `parse` pins RS256, constructs the public key from JWK n/e,
verifies the signature and only then returns claims. The resource checks issuer,
audience and times. The fixed JWK is a namespaced classpath fixture so separately
launched processes use the same key. No OAuth/security framework hides these steps.

`StarterExercise.main()` / `runStarter` retains unsigned issuance and claim-only
acceptance in its two starter overrides. Complete RSA signing and verification.
The tests compare those deliberate omissions with completed rejection of unsigned,
payload-tampered and signature-tampered tokens. Refresh preserves JWT format and
selected subject/scope, fixing the original opaque refresh-access inconsistency.

Individual entry points: authorization variant, resource variant, `Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-11-ex-3/tokens.json`; the resource
uses public-key verification rather than token-file lookup. `./gradlew check` verifies signed flow, tampering, alg=none,
claim checks, starter weaknesses, code/refresh/auth/scope and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
