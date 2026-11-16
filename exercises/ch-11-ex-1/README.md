# Chapter 11, exercise 1: unsigned JWT format and claims

Run `./gradlew :exercises:ch-11-ex-1:run` or IntelliJ's
`org.oauthinaction.ch11ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize as a selected user and fetch the resource.
Stop other exercises on ports 9000–9002 first.

`CompletedAuthorizationServer.issueAccessToken` builds issuer, subject, audience,
issued-at, expiry and unique-ID claims. `TokenJwt.create` encodes the header and
payload with `alg=none` and an empty signature. `CompletedResource.accepted` parses
it and checks issuer/audience/time without looking up token storage. This exercise
deliberately has no integrity protection: anyone can construct valid-looking claims.
The next two exercises add HMAC and RSA signatures; tests preserve this distinction.

`StarterExercise.main()` / `runStarter` leaves issuance opaque in
`StarterAuthorizationServer` and parsing/claim validation unfinished in
`StarterResource`. Both need completion before the starter's flow can fetch data.
The original selected-user subject mismatch is fixed with a stable user key.
Refresh-issued access tokens retain the JWT format and subject/scope, fixing the
original refresh branch's incompatible opaque access-token output.

Individual entry points: authorization variant first, resource variant, `Client`. Code/refresh records share `${java.io.tmpdir}/oauth-in-action/ch-11-ex-1/tokens.json`; the resource does not read it. Static assets are local; `./gradlew check` verifies full code/refresh flow, subject/JTI, issuer/audience
(including arrays), issued-at/expiry, malformed tokens, deliberately forgeable
unsigned claims, the starter parser gap, auth errors, replay and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
