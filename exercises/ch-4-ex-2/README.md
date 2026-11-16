# Chapter 4, exercise 2: word API scopes

Run `./gradlew :exercises:ch-4-ex-2:run`, or IntelliJ's
`org.oauthinaction.ch04ex02.completed.CompletedExercise.main()`. Open
http://localhost:9000/, get a token, select scopes on consent, then read/add/delete
words. Uncheck `write` to see add fail while read succeeds. Stop other exercises
using ports 9000–9002 first.

`CompletedResource` demonstrates separate scope checks for GET (`read`), POST
(`write`), and DELETE (`delete`) on `/words`. Missing/unknown token is 401; a valid
token lacking the operation's scope is 403 with an `insufficient_scope` challenge.
POST adds a word and returns 201; DELETE removes the last word (including safely
handling an empty list) and returns 204. Words live in memory until restart.

`StarterExercise.main()` / `runStarter` intentionally authenticates tokens but
omits scope enforcement, matching the book's starter weakness. Complete
`StarterResource.allowed`; the supporting operations are inherited so the TODO
stays focused on the lesson. This deliberate gap is labeled in the starter code.

The client keeps `/words`, `/get_words`, `/add_word`, and `/delete_word` routes,
with a static page showing JSON/results instead of server templates. Client state
is session-local. The supporting authorization server retains code, implicit,
refresh, client-credentials and password grants; registered scopes are
`read write delete`. User selection and refresh preserve user/scope, correcting
the original undefined variables and record-shape bugs. Token-endpoint redirect
validation remains absent as in the source. See exercise 1 for shared simplifications.

Separate processes: run `AuthorizationServer` first, then `CompletedResource`
(or `StarterResource`) and `Client`. Shared file:
`${java.io.tmpdir}/oauth-in-action/ch-4-ex-2/tokens.json`, reset by authorization
server startup. HTML/JS/CSS is served from namespaced classpath assets; no Node,
CDN or template engine is required.

Run `./gradlew check` from the root. Tests verify authorization/errors/replay,
independent scope enforcement, word mutation and empty deletion, browser proxy
flow, starter weakness, shared storage and static isolation on temporary ports.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
