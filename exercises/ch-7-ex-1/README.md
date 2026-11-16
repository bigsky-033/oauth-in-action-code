# Chapter 7, exercise 1: dynamically registering a native client

Run `./gradlew :exercises:ch-7-ex-1:run` or IntelliJ's
`org.oauthinaction.ch07ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize and approve, then fetch the resource. The desktop
client registers itself before its first authorization, retaining its issued ID
and secret for the current session. Stop other exercises on ports 9000–9002 first.

Read `CompletedDesktopClient.register`: POST JSON metadata including name,
loopback redirect URI, scope, grants, response type and authentication method to
`/register`; then use the response's credentials for authorization/code exchange.
The original starts registration on page load; Java performs it lazily on the first
authorization action so a request cannot race unfinished registration.

`StarterExercise.main()` / `runStarter` leaves only dynamic registration unfinished
in `StarterDesktopClient.register` (501). The rest of the native flow is inherited.
The Cordova/custom-scheme client is replaced with the same Java process + local
HTML interface + loopback callback approach as chapter 6 exercise 5.

The supporting authorization/resource endpoints, teaching RSA key, explicit bug
fixes, storage reset and HTTP introspection behavior remain as documented in
chapter 6 exercise 5. All are kept and tested, not replaced by framework OAuth
handlers. Individual entry points: `AuthorizationServer` first, then
`ProtectedResource` and `CompletedDesktopClient`/`StarterDesktopClient`. Storage:
`${java.io.tmpdir}/oauth-in-action/ch-7-ex-1/tokens.json`.

Run `./gradlew check` for registration/session reuse, complete native flow,
starter gap, management lifecycle, introspection/revocation, grant paths,
ID signatures/nonce, scoped UserInfo, resource scope/user behavior and assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
