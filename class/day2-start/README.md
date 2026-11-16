# Classroom: start of day two

Run `./gradlew :class:day2-start:run` or
`org.oauthinaction.classroomday2.Exercise.main()`; open http://localhost:9000/.
One exercise at a time uses ports 9000–9002. Individual entry points:
`AuthorizationServer`, `ProtectedResource`, `Client`.

The OAuth authorization-code flow works: foo/bar consent, single-use state/code,
Basic client authentication and bearer resource access. User identity is the next
lesson: client /userinfo returns 501; resource /userinfo requires a valid token
then returns 501. There is no ID token, refresh grant or completed login yet.
The source's empty identity fixture remains unfinished; Java stores only the selected user key with the grant.

The authorization server resets its JSON token file under
`${java.io.tmpdir}/oauth-in-action/day2-start/` at startup. Separate JVMs
share that file. Token-endpoint redirect binding is intentionally absent, matching
this stage. Session state replaces the original process-wide browser state.

`./gradlew check` verifies the real code/callback/resource flow, state/code replay,
malformed authentication, assets and identity TODOs. Compare `../complete` for
OpenID Connect and `../snippets` for individual protocol fragments.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
