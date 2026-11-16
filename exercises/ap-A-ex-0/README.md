# Appendix A, exercise 0: HTTP scaffolding

Run `./gradlew :exercises:ap-A-ex-0:run` or IntelliJ's
`org.oauthinaction.appendixa.Exercise.main()`. Open http://localhost:9000/.
`Client`, `AuthorizationServer`, and `ProtectedResource` also have individual
entry points on ports 9000–9002. Stop other examples using those ports first.

This preserves the original appendix's scaffolding stage: three running servers,
static pages, client registration data and null token placeholders. There are no
OAuth authorization/token/resource handlers yet; those routes intentionally return
404 rather than pretending a flow is implemented. No token database is required.

Run `./gradlew check` for root pages/assets, status data and absent protocol-route
checks on temporary ports. All assets are local/classpath-based.

Original JavaScript/template history and migration decisions: [migration record](../../MIGRATION.md).
