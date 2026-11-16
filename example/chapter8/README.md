# Chapter 8 baseline variants

Run `./gradlew :example:chapter8:run` or
`org.oauthinaction.examplechapter8.Exercise.main()`. It starts the full supporting
AS, resource variant 0 and greeting web client. Open http://localhost:9000/.
For other variants, start `AuthorizationServer`, then exactly one of
`ProtectedResource0`/`1`/`2`/`3`, then `Client` or alternate `ImplicitClient`.
All use ports 9000–9002; stop the previous example first.

Variants 0, 2 and 3 retain reflected HTML and missing protections as the original
teaching baselines. Variant 1 writes a raw servlet response without Content-Type
and retains resource/word/produce/favorites APIs. All validate tokens through real
authenticated introspection. Invalid tokens return 401 instead of hanging.
The greeting client handles both text and JSON responses, fixing the original
generic client's JSON parse failure for these baselines, and displays text safely.
Do not mistake the browser proxy's JSON wrapper for protection at the resource.

The solutions remain in solution0/solution1/solution2, with their own Java entry
points and response-policy code. They reuse this chapter's supporting servers and
HTTP helper through Gradle rather than copy another OAuth implementation. This
keeps comparison focused on MIME, content headers, CORS and HSTS. Supporting AS
fixes are those documented in the complete example; the chapter client uses greeting
scope. Storage: `${java.io.tmpdir}/oauth-in-action/chapter8/tokens.json`.

`./gradlew check` verifies each baseline's raw format/MIME/header differences,
active-token requirement, text greeting client flow and implicit callback assets.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
