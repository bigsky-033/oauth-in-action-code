# Chapter 8 solution 0: response content

Run `./gradlew :example:chapter8:solution0:run` or
`org.oauthinaction.examplesolution0.Solution.main()`. This starts supporting servers
and variant 0c. Open http://localhost:9000/. Stop other examples on ports 9000–9002.

Individual resource entry points preserve the three source variants:
`ProtectedResource0a` encodes unknown input but still returns HTML;
`ProtectedResource0b` returns JSON; `ProtectedResource0c` adds nosniff and the
book's X-XSS-Protection header. All still validate via HTTP introspection. For an
individual resource run, start chapter8's `AuthorizationServer` and `Client` too.

This small module depends on the related chapter8 baseline for those supporting
servers, HTTP helper, key and token store; response-policy code is local and
resources have a separate classpath namespace. It uses the baseline's
`${java.io.tmpdir}/oauth-in-action/chapter8/tokens.json` storage. `./gradlew check`
tests MIME/encoding/header differences, invalid tokens and local asset loading.

Original JavaScript/template history and migration decisions: [migration record](../../../MIGRATION.md).
