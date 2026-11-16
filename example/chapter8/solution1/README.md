# Chapter 8 solution 1: CORS

Run `./gradlew :example:chapter8:solution1:run` or
`org.oauthinaction.examplesolution1.Solution.main()`. Open http://localhost:9000/.
Individual `ProtectedResource2.main()` can replace the baseline resource while
chapter8's AS and `ImplicitClient` are running. Stop other examples on ports 9000–9002.

This solution retains JSON, encoded unknown input and content headers, and permits
cross-origin Bearer requests through explicit method-level CORS. Its response
policy is local; the Gradle dependency supplies only related baseline servers and
HTTP introspection/greeting helpers. Static assets have their own namespace; token
storage is the parent chapter8 baseline's file. `./gradlew check` verifies real
preflight/response headers, JSON/content handling and invalid-token rejection.

Original JavaScript/template history and migration decisions: [migration record](../../../MIGRATION.md).
