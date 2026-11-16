# Chapter 8, exercise 2: cross-origin resource requests

Run `./gradlew :exercises:ch-8-ex-2:run` or IntelliJ's
`org.oauthinaction.ch08ex02.completed.CompletedExercise.main()`. Open
http://localhost:9000/, obtain an implicit token and fetch the greeting directly
from port 9002. Stop other exercises on ports 9000–9002 first.

The client is static browser HTML/JS, not a server proxy. `CompletedResource`
adds `@CrossOrigin` to the protected greeting endpoint so the browser's Bearer
header is permitted by preflight and the response is readable. Token validation,
JSON MIME, escaped unknown input and the previous exercise's content headers stay
explicit. `StarterExercise.main()` / `runStarter` uses the otherwise identical
`StarterResource` without CORS: HTTP calls work, but browser cross-origin reads
do not. Add CORS to that method to complete the exercise.

The implicit token stays in the fragment, is parsed/validated in browser JS, and
is stored with per-tab session state. Java only serves `/` and `/callback`. The
supporting full authorization server retains the previous example's endpoints,
fixture scopes and documented bug fixes. Resource token lookup uses the shared
file. Individual entry points: authorization server first, resource variant, then
`Client`. Storage: `${java.io.tmpdir}/oauth-in-action/ch-8-ex-2/tokens.json`.

Run `./gradlew check` for CORS preflight/response and starter differences, implicit
flow, callback, JSON/language behavior, 401 and token precedence. Optional browser
parser checks: `node tools/test_browser_clients.mjs`.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
