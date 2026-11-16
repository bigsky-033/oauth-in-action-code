# Complete OAuth example

Run `./gradlew :example:run` or IntelliJ's
`org.oauthinaction.examplemain.Exercise.main()`, then open http://localhost:9000/.
Authorize as a demo user and select scopes, then explore resource, words, produce,
favorites, refresh, revocation, UserInfo and password-grant actions. Stop other
examples on ports 9000–9002 first. Individual entry points are `AuthorizationServer`,
`ProtectedResource` and `Client`. `ImplicitClient.main()` is an alternate browser
client: start the two servers individually, then that client instead of `Client`.

The source's fixed client/server scopes disagree. Java uses an explicit study
scope set covering the APIs shown on the page; consent still controls what each
token can do. Protocol checks stay in these files. AS supports code/implicit/
refresh/client-credentials/Carol-password grants, registration management,
introspection, revocation and scoped UserInfo. Resource uses real authenticated
HTTP introspection. User-bearing grants include the book's RS256 ID token, verified
by the web client. Word operations distinguish 401/403; produce filters categories;
favorites follow the selected user. Separate native-client material is migrated
in its existing directory.

The fixed teaching key is local/namespaced; token storage is AS-only at
`${java.io.tmpdir}/oauth-in-action/example/tokens.json`, reset on AS startup. The same user-shape, refresh scope, false-claim, undefined-variable and registration
metadata fixes documented in chapter 6 exercise 5 apply. Pages are plain local
HTML/JS/CSS. `./gradlew check` covers the
full protocol server, dynamic management, ID/UserInfo claims and client proxy routes.

Original JavaScript/template history and migration decisions: [migration record](../MIGRATION.md).
