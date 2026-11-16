# Implementation journal

Historical checkpoints recorded during the migration. Plans and next steps below
refer to their original checkpoint; [MIGRATION.md](../MIGRATION.md) is authoritative
for final status.

## Current checkpoint

Continuation scope: migrate the ENTIRE inventory without asking the user to
authorize each chapter. A verified chapter is an internal checkpoint, not the end
of the task. Keep advancing to the next unfinished exercise after root checks pass.

Chapter 4 exercise 1 plan: implement bearer extraction/lookup as the resource-server
solution; keep the starter gap in that server. Supporting authorization server must
retain code/implicit/client-credentials/password/refresh grant paths, selectable
users, and empty registered scope. Tests first: all grant paths, code replay,
client authentication, denial/state, header/body/query bearer transport and header
precedence, missing/unknown/refresh token rejection, CORS preflight, starter 501,
and static-resource isolation. Fix original undefined `query`/`clientId` and user
shape bugs explicitly rather than carry JavaScript runtime errors into Java.

Chapter 4 exercise 1 PASSED root checks: 14 tests (42 repository total), covering
the code flow, all five grant paths, bearer transports/precedence, CORS, and starter.
Next exercise 2: word API GET/POST/DELETE requires read/write/delete respectively.
Tests must distinguish 401 (no token) from 403 (missing scope), cover independent
scope combinations, stateful add/delete including empty list, browser proxy routes,
and a starter that authenticates tokens but intentionally omits scope enforcement.

Chapter 4 exercise 2 PASSED root checks: 12 tests (54 repository total).
Next exercise 3: `/produce` filters fruit/veggies/meats independently, returning
empty arrays for absent scopes (200, not 403). Verify all eight scope combinations,
401 without a valid token, the full consent/client flow with narrowed scope, and
the starter's deliberately unfiltered data. Keep supporting grant tests/behavior.

Chapter 3 exercise 1: `GRADLE_USER_HOME=/tmp/oauth-java-gradle ./gradlew check`
passed with 12 HTTP/storage integration tests, 0 failures. Root consistency passed
for all 42 inventory entries and the first module. Tests run with an alternate
working directory; independently constructed token-file readers observe writes
and resets. Browser JavaScript syntax checks and `git diff --check` passed.
User confirmed IntelliJ launch and browser loading work (2026-11-16). This is a
startup/usability confirmation only: the user has not yet studied the book or
checked intended OAuth behavior. No specific exercise/variant coverage was stated.
Protocol verification remains the implementation's automated-test evidence; do
not treat this feedback as an independent review of protocol correctness.

Chapter 3 exercise 2: the same root command passed with 16 tests in this module
(28 total across both modules), 0 failures. Added automatic refresh/retry, invalid
refresh recovery, scope preservation, client authentication on refresh, rejection
of refresh tokens at the resource server, and a starter with only refresh unfinished.
Root consistency passed with both modules. Java seeds the demo refresh token
synchronously; the initial invalid access token is preserved. Repeated small helpers
keep exercises independently readable; there is no shared OAuth framework.

Next: chapter 4 exercise 1. Its lesson is bearer extraction and token lookup in
the resource server; the starter gap moves FROM the client TO the resource server.
IMPORTANT: its supporting authorization server has additional implicit, refresh,
client-credentials, and password branches and user data. Do not blindly copy chapter
3 and claim equivalent coverage. Inventory those branches and decide what must be
ported with explicit documentation/tests before implementing. Then migrate chapter
4 exercises 2–4 (word scopes, produce filtering, user favorites) individually.

Packaged startup verification PASSED: both exercises ran their complete code flows
from temporary working directories, each with the combined launcher AND three
separate JVMs (4 successful smoke cases). Exercise 2 also passed automatic refresh
in both modes. Command: `python3 tools/smoke_launchers.py` after both `installDist`
tasks; the equivalent reproducible root task is `./gradlew smokeLaunchers`.

All original code remains available. Final JavaScript removal is deferred until
the full migration is reviewed. Basic IntelliJ/browser startup is user-confirmed;
detailed visual and learning-flow review remains outstanding. This is a verified
chapter 3 checkpoint, not completion of the full rewrite.

### Chapter 4 progress

Exercise 3 passed root checks: 12 tests (66 total). All eight category combinations
and consent narrowing are verified. Exercise 4 plan: select Alice/Bob on consent,
carry the stable user key through the code/token file, and return that user's
favorites; missing/other users return Unknown/empty arrays, with 401 for invalid
bearer tokens. Test both users, unknown/client-only tokens, token reuse across
separate readers, browser flow, and the starter's deliberate always-Unknown result.
The original compares a user object to a string: fix the representation, preserving
intended user selection, and document it. Favorites are not scope-filtered in source.

Chapter 4 exercise 4 passed root checks: 12 tests (78 total). All chapter 4 exercises
are now verified. Chapter 5 exercise 1 plan: unfinished registration/authorize/
approve/token handlers belong to the authorization server; supporting client and
resource server are complete. Verify full code flow, deny, invalid client/redirect,
Basic/body auth ambiguity, replay, unknown grant, starter 501, and absence of refresh
and scope features (introduced in the next exercises). No user picker is needed.

Chapter 5 exercise 1 passed root checks: 11 tests (89 total). Exercise 2 plan: issue
refresh tokens alongside code tokens and implement the refresh grant. Verify reusable
refresh tokens, new access values, no access with refresh credentials, invalid refresh,
client binding, body authentication used by the /refresh client route, and starter
code flow working while refresh issuance/processing remains TODO. No scopes yet.

Chapter 5 exercise 2 passed root checks: 13 tests (102 total). Exercise 3 plan:
register foo/bar scopes, validate authorization and consent selections, bind granted
scopes to access/refresh records and return them. Implement the book's refresh-scope
bonus explicitly (retain or narrow, never enlarge). Tests: invalid requested/selected
scope, narrowed consent reflected at resource, retained/narrowed refresh scope,
rejected enlargement, starter omission, and all existing code/refresh error cases.

Chapter 5 exercise 3 passed root checks: 15 tests (117 total). Chapter 5 is verified.
Chapter 6 exercise 1 plan: add implicit response generation in the authorization
server and migrate the static browser client. Fragment parsing/state/token storage
must stay in browser JS (fragments are not HTTP parameters). Tests: access token in
fragment with state/scope/type and no refresh, denial, narrowed scope, resource CORS,
callback page serving; keep existing code/refresh paths and test them separately.
Starter only leaves implicit issuance unfinished. Browser state uses sessionStorage
and crypto random values, with a small separately testable fragment parser.

Chapter 6 exercise 1 passed root checks: 16 tests (133 total). Optional Node browser
parser checks passed (encoding, missing/wrong state, errors, token presence/type).
Chapter 6 exercise 2 plan: client credentials without browser redirects/consent;
TODOs in client request and server grant branch. Verify scoped authenticated token
issuance with NO refresh, invalid credentials/scopes, client session/resource flow,
body/basic authentication, starter gaps, plus retained server code/refresh paths.

Chapter 6 exercise 2 passed root checks: 7 tests (140 total). Exercise 3 plan:
password grant with client authentication and the book's three user/password
fixtures. Verify all users, wrong/missing credentials, invalid scope, returned
refresh tokens, form/client/resource flow, starter client/server TODOs, and retained
code/refresh server paths. Unlike exercise 2, no client-credentials branch is added.

Chapter 6 exercise 3 passed root checks: 8 tests (148 total). Exercise 4 has no
starter/completed split in the original: retain the complete browser implicit
client and code/implicit/refresh server. Tests cover fragment flow, narrowed scope,
callback serving, denial and supporting code/refresh. Reuse optional browser-parser
checks. All Java stays in src/main; do not invent exercise TODOs for this variant.

Chapter 6 exercise 4 passed root checks: 16 tests (164 total); optional browser
parser tests pass for both browser variants. Exercise 5 is a complete native-client
example with no original TODO split. Replace Cordova/custom-scheme callbacks with
an independently runnable Java desktop process using a loopback callback + local
HTML interface and system browser. Preserve its fixture native-client-1 credentials.
Supporting server inventory: code/implicit/client-credentials/password/refresh,
RS256 ID tokens, revocation, resource-authenticated introspection, dynamic client
registration GET/PUT/DELETE, scoped UserInfo; resource uses HTTP introspection and
has /resource, /words, /produce, /favorites. Tests first: loopback code flow/state,
introspection (active/inactive/auth), revocation, registration lifecycle/auth/invalid
metadata/deletion, subject/scope selection, all grant branches, ID signature/nonce,
resource scope/subject behavior, and native callback page/assets. Fix original
undefined variables, inconsistent metadata auth names, broken registration updates,
user object/key mismatch and false email_verified omission explicitly.

Chapter 6 exercise 5 passed root checks: 9 tests (173 total), including all full
supporting-server paths and real resource-to-AS HTTP introspection. Chapter 6 is
verified. Chapter 7 exercise 1 plan: dynamic registration for the desktop client
before its first authorization (instead of blank native client credentials). Test
new IDs/secrets, per-session registration reuse, registration metadata and complete
code/resource flow; starter returns 501 only for registration. Preserve all server
and resource tests from the previous full native variant. Use loopback redirect.

Chapter 7 exercise 1 passed root checks: 10 tests (183 total), including dynamic
native registration/reuse and all full-server behavior. Chapter 8 exercise 1 plan:
GET /helloWorld must return JSON with nosniff/X-XSS-Protection instead of HTML/string
reflection; starter deliberately retains reflected HTML. Verify all five languages,
unknown/HTML-like input encoding, explicit MIME/header differences, missing token
401 (fix original hanging request), whole client greeting flow, ancillary client
revocation/UserInfo/password routes, and supporting server grant paths. AS first
client has greeting scope; native fixture has OIDC scopes, matching original.

Chapter 8 exercise 1 passed root checks: 6 tests (189 total). Exercise 2 plan:
static implicit browser client calls GET /helloWorld across origins, with Bearer
Authorization triggering preflight. Completed resource enables CORS; starter is
otherwise the same protected JSON endpoint without CORS. Verify preflight and
simple response headers, no-CORS starter behavior, implicit fragment/resource flow,
callback page and existing JSON/401/transport behavior. Test the actual JS parser.

Chapter 8 exercise 2 passed root checks: 5 tests (194 total); browser parser checks
passed. Exercise 3 plan: add Strict-Transport-Security: max-age=31536000 to the
already JSON/content-header/CORS response. Test presence and starter absence,
all greeting languages, token handling and full proxy-client flow. The original
still listens on HTTP; preserve that setup and explain that browsers only apply
HSTS learned over HTTPS, rather than claiming the header creates TLS.

Chapter 8 exercise 3 passed root checks: 6 tests (200 total). Chapter 9 exercise 1
plan: bind the token exchange redirect_uri to the authorization request, rejecting
missing/mismatched values and consuming the attempted code. Starter deliberately
omits that binding. Tests: matching/missing/different redirect, replay after failure,
whole client flow, existing scope/refresh/auth errors. Supporting source defects:
client requests OIDC scopes while its AS registers foo/bar; resource introspects
an endpoint absent from this AS. Java requests foo/bar and uses shared-file token
lookup for the same resource APIs. Unsupported copied client menu routes will be
explicitly labeled instead of hanging; the full implementations remain in their
relevant chapters. This repairs main flow without inventing a different grant.

Chapter 9 exercise 1 passed root checks: 16 tests (216 total). Exercise 2 plan:
invalid requested scope is a local HTTP 400 error without redirect, compared with
the starter's redirect response. Verify normal multi-scope requests still work,
forged consent scope handling uses the stored callback, and state is preserved in
other responses. Fix the original solution's string-vs-array scope comparison and
undefined request redirect reference. Keep this independent baseline's lack of
exercise 1 token-endpoint binding explicit rather than silently adding it.

Chapter 9 exercise 2 passed root checks: 15 tests (231 total). Chapter 10 exercise 1
plan: client generates/stores a PKCE verifier and S256 challenge; server binds the
challenge/method to the code and checks verifier at exchange. Test RFC S256 vector,
full S256 flow, plain support, wrong/missing verifier, unknown method, replay after
failed verification, no-challenge compatibility (source only checks when supplied),
and explicit starter omissions on each side. Preserve independent source baseline:
PKCE does not silently add chapter 9's token-endpoint redirect check.

Chapter 10 exercise 1 passed root checks: 16 tests (247 total), including both
starter omissions explicitly. Chapter 11 exercise 1 plan: unsigned JWT access
tokens (alg=none), user subject, issuer/audience/iat/exp/jti; resource validates
claims without storage lookup. Verify full code/resource flow, claims and array
audiences, expired/future/wrong issuer/audience/malformed rejection, deliberately
forgeable unsigned claims, opaque starter token and unfinished resource parser.
Keep refresh token format/user/scope consistent rather than the original's opaque
refresh-access bug. Do not add signatures until the following exercises.

Chapter 11 exercise 1 passed root checks: 16 tests (263 total), with both starter
gaps explicitly covered. Root chapter-label checks now prevent copied launch
messages from drifting. Exercise 2 plan: HS256 HMAC using JDK Mac and the source's
shared secret, verify before accepting claims, pin expected algorithm (do not trust
the unverified header). Tests: valid signed flow, payload/signature tampering,
alg=none rejection, signed bad claims and array audiences, starter unsigned issuer
and omitted verifier. Keep the claim-only starter weakness visible and tested.

Chapter 11 exercise 2 passed root checks: 16 tests (279 total). Exercise 3 plan:
RS256 JWT issuance with the book's RSA key and public-key verification. Preserve
claim validation and refresh format. Test valid signed flow, tampering, unsigned
rejection, wrong algorithm/signature, invalid claims, and the unsigned/claim-only
starter weaknesses. Use JDK Signature/KeyFactory, not Spring Security.

Chapter 11 exercise 3 passed root checks: 16 tests (295 total). Exercise 4 plan:
opaque token issuance plus resource-authenticated HTTP introspection. Resource
must not read token storage. Test active/inactive results, client/scope/subject
metadata, missing/wrong resource credentials, real resource network flow, immediate
invalidation after storage reset, and 501 introspection/unfinished-resource starters.
Preserve code/refresh/user/scope behavior without JWT generation in this variant.

Chapter 11 exercise 4 passed root checks: 16 tests (311 total). Exercise 5 plan:
authenticated owner-bound access-token revocation, idempotent unknown-token success,
client POST /revoke and local state clearing. Verify invalid client, other-owner
record retention, immediate resource failure, refresh grant retention and starters
on both AS/client. Retain introspection from exercise 4 as a documented repair:
original exercise 5 resource calls /introspect but its AS omitted that endpoint.
The book's revocation operation is access-token-only, not refresh-grant revocation.

Chapter 11 exercise 5 passed root checks: 17 tests (328 total). Chapter 12 exercise
1 plan: initially empty registry, JSON dynamic client registration and client-side
registration before authorization; code/refresh only (no unrelated full-server
features). Verify metadata defaults/validation, secret/no-secret clients, new IDs,
code/client binding, complete dynamic browser flow/reuse, scope/auth/replay errors,
and client/AS registration TODOs. Repair canonical auth names/secret-generation bug
and request refresh permission explicitly. Use shared-file resource lookup because
original resource introspection calls an AS endpoint absent in this exercise.

Chapter 12 exercise 1 passed root checks: 5 tests (333 total). Exercise 2 plan:
management bearer credentials distinct from token client authentication; read,
replace client name/metadata while preserving issued identifiers/secrets, delete
registration and its tokens, clear local state and allow re-registration. Tests:
401/403/404, immutable ID/secret rejection, full management browser routes, token
invalidation on deletion, and explicit starter management gaps. Registration URI
must use the actual request origin so temporary-port tests and local launch both work.

Chapter 12 exercise 2 passed root checks: 7 tests (340 total). Chapter 13 exercise 1
plan: code flow with OpenID scope, conditional RS256 ID token issuance and client
signature/issuer/audience/time validation, resource-server UserInfo scoped claims.
Verify Bob's false email_verified survives, ID/UserInfo subject agreement, no ID
without openid, invalid signature/claims/nonce, UserInfo 401/403/404, whole browser
flow and three starter gaps (issuance, validation/UserInfo call, UserInfo endpoint).
Use the server's existing optional nonce path in the Java client so it is observable.
This variant has no refresh grant; access tokens remain opaque.

Chapter 13 passed root checks: 6 tests (346 total). Chapter 15 exercise 1 plan:
per-access-token RSA keypair, private JWK delivered to client, public key only in
AS storage/introspection, signed PoP header binding access token/method/authority/
path. Verify full flow, active introspection and no private exponent exposure,
wrong key/signature/token/method/authority/path rejection, Bearer-only rejection,
unknown tokens and all three starter TODOs. Preserve the book's historical PoP
format (not modern DPoP): timestamp/replay/header/query validation remains absent
and method/authority/path bindings are optional in that source, documented as such.
Use JDK RSA primitives; no external key generation dependency or OAuth framework.

Chapter 15 passed root checks: 5 tests (351 total). All numbered chapter exercises
are verified; do not stop here. Appendix A plan: preserve the original three-server
scaffolding with registered client/token placeholders and static pages, but no OAuth
handlers. Verify root pages/assets/status and intentional 404 for unimplemented
protocol endpoints. No token database or invented flow is needed for the appendix.
After that migrate example/, chapter8 variants/solutions, chapter10, native-client,
and all classroom stages/snippets, then full launch/source cleanup review.

Appendix scaffold passed root checks: 1 test (352 total). All exercise directories
are now covered. Main example plan: full supporting AS and HTTP-introspected
resource APIs, full web client proxy routes (resource/words/produce/favorites,
refresh/revoke/UserInfo/password), and alternate implicit browser launcher. Repair
source client/AS scope mismatch using an explicit study scope set covering exposed
APIs. Tests: all full-server cases plus browser word/produce/favorites/UserInfo and
revocation routes, implicit page/flow and assets; no protocol framework extraction.

Main example passed root checks: 10 tests (362 total), including all full client
proxy APIs and the alternate implicit callback page. Chapter 8 examples plan:
preserve four separately runnable resource variants and their introspection dependency.
0/2/3 retain reflected HTML; 1 writes a raw response and retains other resource APIs.
Solutions 0a/0b/0c progressively encode input, use JSON, add content headers;
solution1 adds CORS; solution2 adds HSTS. Test exact response formats/header/CORS
and token failures, plus proxy client handling text responses without crashing.
Related solution modules may reuse the chapter8 baseline servers/transport helpers
through a Gradle dependency; response-policy code remains local to each solution.

Chapter 8 baseline variants passed root checks: 2 tests (364 total). Solution0
plan: independently runnable 0a encoded HTML, 0b JSON, 0c JSON plus content headers;
all use authenticated introspection from the parent baseline. Test exact MIME,
encoded unknown input, missing-token 401 and header differences, with own static
resource namespace and one combined default launcher (0c).

Chapter 8 solution0 passed root checks: 1 test covering three variants (365 total).
Solution1 plan: JSON/content headers plus CORS on resource variant 2. Verify bearer
preflight/simple response, encoded JSON, invalid-token 401 and own assets while
reusing the already verified parent chapter servers/transport.

Chapter 8 solution1 passed root checks: 1 test (366 total). Solution2 plan: same
protected JSON/CORS response plus HSTS max-age=31536000. Verify header and existing
preflight/token behavior; document that HTTP teaching launchers do not create TLS.

Chapter 8 solution2 passed root checks: 1 test (367 total). Chapter10 example plan:
preserve its full-example API set and plain PKCE (not the S256 exercise variant).
Bind the challenge from the saved authorization request rather than a tamperable
consent hidden field. Verify full flow, wrong/missing verifier and attempted-code
replay plus the full-example tests. Normalize the original typo error to invalid_grant.

Chapter10 passed root checks: 11 tests (378 total). Standalone native plan: replace
Cordova with the verified desktop loopback design, preserve native credentials and
its OpenID/profile/email/address/phone request. Supply local supporting servers so
the directory runs alone. Verify code/state, introspection, refresh, resource APIs,
registration and signed ID issuance using the native integration suite.

Standalone native passed root checks: 9 tests (387 total). Classroom initial plan:
preserve empty registry and unfinished client/AS/resource handlers as explicit 501
responses; original UserInfo guard remains 401 because extraction is unfinished.
Verify distinct pages/assets, empty registry, TODO statuses and UserInfo rejection.

Classroom scaffold passed root checks (388 total). Day2 plan: working code/state/
Basic exchange and shared-file resource access with foo/bar; preserve identity
TODOs as 501 after the existing resource bearer guard (401 without a token).
Verify full browser flow, replay, malformed auth, assets and both UserInfo TODOs.
No ID-token issuance, refresh grant or user fixture is invented for this stage.

Day2 passed root checks (389 total). Classroom complete plan: code-only OIDC with
Alice/Bob/Carol, opaque access tokens and conditional RS256 ID token. Verify signature/
issuer/audience/time/nonce, scoped UserInfo (false values retained), subject match,
missing identity/scope/token and assets. Commented JWT-access-token experiments
remain snippets, not active flow. Reuse the chapter13 explicit design consistently.

Classroom complete passed root checks: 5 tests (394 total). Snippets plan: eight
compilable Java counterparts plus a local console demonstration, no invented
HTTP application. Verify state/encoding/Basic, bearer lookup, access-vs-ID token
audience and signature/nonce validation, scoped/false UserInfo claims and subject
agreement. Reuse only complete-stage transport/storage/JDK crypto helpers.

All 42 directories now have verified Java modules: 398 tests passed root checks.
Final consistency review: normalize copied page/launcher labels; consistently reject
wrong-type registration metadata; derive management URLs from the actual request
origin (including test ports); reject unknown identity users with 400. Verify each
copy through its HTTP suite before final source cleanup and packaged launches.
