# Chapter 10 standalone example: plain PKCE

Run `./gradlew :example:chapter10:run` or IntelliJ's
`org.oauthinaction.examplechapter10.Exercise.main()`. Open http://localhost:9000/.
This is the full example with its original plain verifier/challenge check added.
For S256, compare `exercises/ch-10-ex-1`. Stop other examples on ports 9000–9002.

The client generates/stores a verifier, sends it as the challenge, then returns it
at code exchange. AS binds it to the saved request/code and rejects missing/wrong
values; attempted codes are consumed. Unlike the original, the AS does not trust
a copied challenge hidden in the consent form. Typo errors become invalid_grant.
No-challenge compatibility and the alternate implicit client remain as in source.

Full resource/word/produce/favorites, refresh, revocation, UserInfo, password,
registration and introspection behavior and explicit study scopes are the same as
the main example, with its documented supporting-source fixes. Individual entry
points: `AuthorizationServer`, `ProtectedResource`, `Client` or `ImplicitClient`. Storage: `${java.io.tmpdir}/oauth-in-action/chapter10/tokens.json`. `./gradlew check` covers plain PKCE, replay,
the complete browser flow and the full-example server/client API cases.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
