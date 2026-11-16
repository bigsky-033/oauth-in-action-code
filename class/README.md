# Classroom: starting stage

Run `./gradlew :class:run` or `org.oauthinaction.classroom.Exercise.main()` and
open http://localhost:9000/. Stop other exercises on ports 9000–9002 first.
Individual entry points: `Client`, `AuthorizationServer`, `ProtectedResource`.

This is the workshop's initial scaffold: the client registry is empty. Client
authorize/callback/resource/UserInfo and authorization-server authorize/approve/token
handlers are TODOs. They return 501 with an explanation instead of an empty 200.
Resource POST /resource is also a TODO; /userinfo retains the original 401 guard.
No OAuth flow is claimed here. Continue with `day2-start`, then `complete`; those
are separate modules so edits and classpaths remain isolated.

Plain HTML/CSS pages work from any directory. `./gradlew check` verifies pages,
assets, the empty registry and intentional unfinished/unauthorized responses.

Original JavaScript/template history and migration decisions: [migration record](../MIGRATION.md).
