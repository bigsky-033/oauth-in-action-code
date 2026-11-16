# Chapter 6, exercise 5: desktop Java client

Run `./gradlew :exercises:ch-6-ex-5:run` or IntelliJ's
`org.oauthinaction.ch06ex05.Exercise.main()`, then open http://localhost:9000/.
Get a token, choose a demo user/scopes, approve, and fetch the resource. Stop other
exercises on ports 9000–9002 first. This complete example has no starter TODO split.

The Cordova app is replaced by an independently runnable `DesktopClient` Java
process with a local HTML interface. It uses the system browser and a loopback
callback (`http://localhost:9000/callback`) instead of a phone-only custom scheme.
The native fixture credentials remain `native-client-1` / `oauth-native-secret-1`;
code exchange sends them in the form body as in the book. State is session-local
and single-use. This example retains its original confidential-client fixture;
it does not silently add PKCE or change the grant while changing platform.

`AuthorizationServer` keeps the original supporting endpoints and grant paths:
code, implicit, refresh, client credentials, Carol's password grant, `/revoke`,
resource-authenticated `/introspect`, JSON `/register` with GET/PUT/DELETE management,
and scoped GET/POST `/userinfo`. User-bearing grants also issue the book's RS256
ID token; access tokens remain opaque. `Jwt` shows compact JWS framing around JDK
RSA primitives; the original fixed teaching key is a namespaced classpath fixture.
The desktop client does not use ID tokens for login in this chapter.

`ProtectedResource` uses real HTTP introspection, not a shared token-file shortcut.
It keeps `/resource`, scoped GET/POST/DELETE `/words`, filtered `/produce`, and
user-bound `/favorites`. Introspection returns a stable subject and username key,
resolving the original user-object/string inconsistency. False `email_verified`
is included rather than accidentally dropped. Phone/address scopes add no claims
because the source fixtures contain none.

Other explicit repairs: undefined client variables; registration authentication
method names (`client_secret_basic`/`client_secret_post`, accepting the book's
`secret_basic`/`secret_post` aliases); secret issuance and update bookkeeping;
refresh user/scope retention; controlled errors for malformed auth/metadata. The
source allowed both Basic and body secret authentication and this example retains
that. Registration management uses a separate bearer credential. Scope on refresh
may be retained/narrowed, never enlarged. Token-endpoint redirect checking remains
absent until the later validation lesson.

Individual entry points: `AuthorizationServer` first, then `ProtectedResource` and
`DesktopClient`. Only the authorization server reads/writes
`${java.io.tmpdir}/oauth-in-action/ch-6-ex-5/tokens.json`, reset on startup. Registrations, pending requests and codes are in memory. Assets are local static
HTML/JS/CSS.

Run `./gradlew check`. Tests cover loopback code/state, active/inactive authenticated
introspection, revocation, registration lifecycle/public clients, all grant paths,
ID signatures/nonce, scoped UserInfo including false values, resource scopes/user
selection and assets. Tests use temporary ports/files; actual desktop/browser UI
interaction remains a separate manual check.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
