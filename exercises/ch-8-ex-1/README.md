# Chapter 8, exercise 1: protecting response content

Run `./gradlew :exercises:ch-8-ex-1:run` or IntelliJ's
`org.oauthinaction.ch08ex01.completed.CompletedExercise.main()`. Open
http://localhost:9000/, authorize for `greeting`, choose a language, then get the
greeting. Stop other exercises on ports 9000–9002 first.

`CompletedResource` serves GET `/helloWorld?language=en` as JSON, adds
`X-Content-Type-Options: nosniff` and the book's `X-XSS-Protection` header, and
encodes unknown-language input. `StarterExercise.main()` / `runStarter` deliberately
retains the original reflected-HTML response without those headers. Implement
`StarterResource.response`. Browser output uses textContent, so inspect resource
responses to compare MIME/header behavior directly.

The original hangs on invalid tokens; Java returns an explicit 401. This exercise
does not enable CORS; the next does. Valid-token lookup uses the shared file as in
the source. The client uses code flow and proxies the greeting, validates any
returned ID token's signature/issuer/audience/time, and retains the supporting
refresh/revocation/UserInfo/password routes. Its original password handler never
responded; Java completes it with a result/error. Default client scope is greeting,
so UserInfo requires obtaining a separately scoped token, just as in the source.

The full supporting authorization endpoints, registration, introspection and
teaching RSA key are preserved from the native full-server variant, with original
chapter 8 fixture scopes. Scope/user record and registration bug fixes are those
documented in chapter 6 exercise 5. The unused dynamic-registration helper is
covered by the actual dynamic client in chapter 7 and later registration exercises;
this fixed-registration client does not register on startup.

Individual entry points: `AuthorizationServer` first, `CompletedResource` or
`StarterResource`, then `Client`. Storage:
`${java.io.tmpdir}/oauth-in-action/ch-8-ex-1/tokens.json`. Run `./gradlew check` for language/JSON/header tests, reflected HTML-like input,
invalid tokens, complete client flow/revocation, ancillary pages and token precedence.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
