# Working on the Java study repository

Read `MIGRATION.md` before changing an exercise. It is the durable checklist and handoff.

- Preserve `example/`, `exercises/`, `class/`, and chapter-specific behavior.
- Java 21, Gradle, Spring MVC, plain HTML/JavaScript/CSS. No Spring Security,
  template engines, Lombok, database server, or general-purpose OAuth framework.
- Keep OAuth checks in the chapter code. Prefer explicit constructors, records,
  and short methods. Comments explain protocol decisions, not Java syntax.
- The migration covers all 42 directories. Superseded JS/templates/Cordova files
  are removed; original source is in the reference Git commit in MIGRATION.md.
- Use unique packages and classpath resource paths per exercise. Never rely on
  IntelliJ's working directory. All launchers must work from the root.
- Default ports: client 9000, authorization server 9001, resource server 9002.
  Run one exercise at a time. Tests use ephemeral ports and isolated storage.
- Preserve deliberate teaching vulnerabilities; document differences from the
  original. Do not silently replace historical flows with newer protocols.
- Before implementing an exercise, record its behavior and verification cases.
  Complete and verify it before beginning the next.
- After each exercise run `./gradlew check` for ALL migrated modules, including
  the repository consistency check. Update evidence and next steps in MIGRATION.md.
- Do not mark IDE or visual checks complete unless actually performed. Automated
  HTTP tests and packaged launch checks are separate evidence.

## Resuming

1. Read the current checkpoint and inventory in `MIGRATION.md`.
2. Inspect `git status`; preserve existing work.
3. Read docs/RUNNING.md for main classes and task paths.
4. Follow the current user request; migration completion is not a reason to
   silently rewrite another chapter. Update verification evidence after changes.
