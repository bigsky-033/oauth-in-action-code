# Java migration record

## Current checkpoint

Migration complete (2026-11-16): all 42 original inventory directories have Java
modules. All 400 tests and 80 packaged launch cases passed on the cleaned tree.
Superseded runtime/scaffolding files are removed. No migration item remains pending.

## Agreed design

Java 21, Gradle 8.14.3 (wrapper SHA-256 pinned), Spring Boot 3.5.16 web dependency
platform for Spring MVC/embedded HTTP/JSON. No Spring Security, template engine,
Lombok, external database or general OAuth framework. Plain local HTML/JS/CSS.
Import the root in IntelliJ Community and run ordinary main methods; packages and
static paths are unique per module. [Run index](docs/RUNNING.md).

The existing chapter/example/classroom organization and completed solutions are
preserved. Each chapter shows its own protocol decisions. Small helpers are
repeated when that keeps an exercise independently readable; related chapter 8
solutions depend on the baseline, and classroom snippets reuse the complete-stage
encoding/storage/RSA helpers. Dependencies are explicit Gradle project edges.

Default ports are 9000/9001/9002; run one example at a time. Session-local browser
state is single-use. Assets never depend on cwd. A small atomic JSON token file
replaces the source NoSQL file, with one AS writer and independent readers; later
lessons use real HTTP introspection where the source does. Demo state resets on
AS startup. Tests use temporary ports/files and alternate working directories.

Cordova clients are Java desktop/loopback examples using the system browser,
retaining fixture credentials and grant behavior. No framework silently adds PKCE
or removes historical implicit/password flows. Classroom starting/day2 stages
retain their TODOs; snippets compile and demonstrate fragments without a fake web app.

## Checklist and verification gates

- [x] Inspect every original starter/solution and record its intended behavior.
- [x] Specify chapter-specific positive/negative verification before implementation.
- [x] Implement and verify each directory before moving to the next.
- [x] Root Gradle import structure, ordinary mains and isolated resource namespaces.
- [x] All numbered exercises, appendix, complete examples and chapter variants.
- [x] Native client, classroom stages and eight protocol snippets.
- [x] Root-wide tests and repository consistency after every implementation step.
- [x] Final consistency review and root/module run instructions.
- [x] Packaged default/starter/alternative launches from temporary directories.
- [x] Remove superseded Node/templates/Cordova files after verified replacement.
- [x] Final root checks and recorded evidence.

For future changes: inspect the lesson and original reference, define the behavior
and verification first, implement the smallest explicit change, run the affected
HTTP checks and the entire root `check`, then update this record. Preserve teaching
weaknesses instead of applying unrelated production hardening.

## Inventory

Each verified row has a build file, explicit settings registration, unique package,
README, executable main and tests. Web modules also have plain static HTML/CSS/JS;
class/snippets is intentionally a console module. Verification is automated
protocol/build evidence, not a claim that every browser/IDE interaction was manual.

| Path | Status |
| --- | --- |
| exercises/ch-3-ex-1 | verified |
| exercises/ch-3-ex-2 | verified |
| exercises/ch-4-ex-1 | verified |
| exercises/ch-4-ex-2 | verified |
| exercises/ch-4-ex-3 | verified |
| exercises/ch-4-ex-4 | verified |
| exercises/ch-5-ex-1 | verified |
| exercises/ch-5-ex-2 | verified |
| exercises/ch-5-ex-3 | verified |
| exercises/ch-6-ex-1 | verified |
| exercises/ch-6-ex-2 | verified |
| exercises/ch-6-ex-3 | verified |
| exercises/ch-6-ex-4 | verified |
| exercises/ch-6-ex-5 | verified |
| exercises/ch-7-ex-1 | verified |
| exercises/ch-8-ex-1 | verified |
| exercises/ch-8-ex-2 | verified |
| exercises/ch-8-ex-3 | verified |
| exercises/ch-9-ex-1 | verified |
| exercises/ch-9-ex-2 | verified |
| exercises/ch-10-ex-1 | verified |
| exercises/ch-11-ex-1 | verified |
| exercises/ch-11-ex-2 | verified |
| exercises/ch-11-ex-3 | verified |
| exercises/ch-11-ex-4 | verified |
| exercises/ch-11-ex-5 | verified |
| exercises/ch-12-ex-1 | verified |
| exercises/ch-12-ex-2 | verified |
| exercises/ch-13-ex-1 | verified |
| exercises/ch-15-ex-1 | verified |
| exercises/ap-A-ex-0 | verified |
| example | verified |
| example/chapter8 | verified |
| example/chapter8/solution0 | verified |
| example/chapter8/solution1 | verified |
| example/chapter8/solution2 | verified |
| example/chapter10 | verified |
| example/native-client | verified |
| class | verified |
| class/day2-start | verified |
| class/complete | verified |
| class/snippets | verified |

## Behavior and source repairs

Read each module README for exact endpoints, grants, fixtures, starter gaps and
negative cases. The implementations preserve the active source variant rather
than accumulating every later chapter's validation into earlier chapters.

- Chapters 3–5: code/refresh, bearer extraction, word scopes, produce filtering,
  selected-user favorites and AS steps. Chapter 3.2 retains a seeded invalid access
  token and valid refresh token to make retry observable. Chapter 5.1 is code-only.
- Chapters 6–7: implicit, client credentials, password, native loopback and native
  registration. Full supporting servers keep introspection, revocation,
  registration management and scoped UserInfo where those endpoints existed.
- Chapter 8: reflected HTML/MIME/content headers, CORS and HSTS. Baselines retain
  their teaching weaknesses; the separate solutions expose each response change.
  HSTS headers over local HTTP are tested; these launchers do not configure TLS.
- Chapters 9–10: redirect binding, scope error behavior and PKCE. The exercise
  supports S256/plain; standalone chapter10 preserves plain PKCE. Optional PKCE
  remains optional as in the source; challenge binding uses the saved request.
- Chapter 11: unsigned, HMAC and RSA JWTs, authenticated introspection and access
  revocation. Algorithm choice is pinned to the variant. Refresh access tokens
  keep the same format and scope/user instead of the source's incompatible output.
- Chapter 12: dynamic metadata/registration management, canonical auth names with
  book aliases, stable credentials, correct returned management URI including port.
  Malformed types/URIs/grants return errors instead of runtime exceptions.
- Chapter 13 and classroom complete: conditional ID token, signature/issuer/
  audience/time/nonce checks and scoped UserInfo with subject matching. Opaque
  access tokens, no added refresh grant; false email_verified is retained.
- Chapter 15: the book's historical RSA PoP token/key/method/authority/path binding.
  Private key stays out of storage/introspection. Timestamp/replay/header/query
  checks remain absent and some bindings remain optional, documented and tested.
  This is the book's PoP format, not modern DPoP.
- Appendix and classroom beginning: runnable scaffolds. Classroom unfinished
  handlers return 501 rather than an empty success; appendix missing routes stay
  absent. Day2 OAuth works while identity handlers remain explicit TODOs.

Other repaired source defects include undefined variables, incompatible user
object/string shapes, client/AS scope mismatches, false-value filtering, lost
refresh scope/user, registration secret regeneration and malformed fragment code.
The full example uses an explicit study scope union so its visible APIs can work.
Where a simple AS lacked the introspection endpoint its generic resource called,
the module uses shared-file lookup and documents that repair. Invalid-token paths
return 401 rather than hanging. The chapter8 client accepts both raw greeting text
and JSON. These are practical repairs, not a claim of production hardening.

## Verification evidence

- `./gradlew check`: 400 tests, zero failures or skipped tests; all 42 modules and repository
  consistency passed after source cleanup. Tests exercise actual HTTP exchanges,
  storage handoff and chapter-specific rejection cases. Repeated root checks at
  every checkpoint are recorded in [implementation journal](docs/MIGRATION_LOG.md).
- Browser JavaScript syntax: 123 scripts passed `node --check`. Optional implicit
  parser tests passed for all six implicit clients: state, encoding, error and
  token-type rejection.
- `./gradlew smokeLaunchers`: all 80 installed launch cases passed again after
  cleanup, from temporary
  working directories: 42 default/console examples, 28 starters, eight alternative
  resource/implicit combinations and two separate-JVM code/refresh flows.
- User confirmed initial IntelliJ/browser startup (2026-11-16), explicitly without
  validating book behavior. No claim is made of manual IDE/visual review for every
  module. Automated protocol evidence is separate from that feedback.

## Original-source reference and cleanup

Original reference commit: `8d837ee48a42358fab220e911c4004530ca40e43`.
Use `git show <commit>:<original/path>` to compare a lesson. Superseded server JS,
underscore templates, Node manifests and Cordova shell assets were removed after
verification. [Removal manifest](docs/REMOVED_ORIGINALS.txt) lists every old path.
611 superseded files were removed; the original license is retained.
Java classpath fixtures and new browser scripts remain part of the study examples.
