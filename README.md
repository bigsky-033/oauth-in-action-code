# OAuth 2 in Action — Java 21 study edition

The whole repository uses Java 21, Gradle, Spring MVC and plain HTML/JavaScript/CSS.
OAuth requests, token handling and validation are explicit chapter code. There is
no Spring Security, template engine, frontend build or database server.

Open this root directory in IntelliJ Community as a **Gradle** project with JDK 21
for the Project SDK and Gradle JVM. Start with:

```sh
./gradlew :exercises:ch-3-ex-1:run
```

Open http://localhost:9000/. In IntelliJ, run
`org.oauthinaction.ch03ex01.completed.CompletedExercise.main()` using the gutter
Run button. `StarterExercise.main()` or the `runStarter` task selects the exercise
with its teaching gaps. Stop the previous example before switching: the book
reuses client/authorization/resource ports **9000/9001/9002**.

[Run every directory](docs/RUNNING.md) lists all 42 modules, their Gradle tasks,
IntelliJ mains and starter variants. The original `exercises/`, `example/`,
`class/`, chapter and `completed/` organization is preserved. Each module has its
own instructions, Java package and static-resource namespace. Code and assets work
from the repository root or another working directory.

```sh
./gradlew check           # all tests and repository consistency (requires Python 3)
./gradlew smokeLaunchers  # installed launchers; requires free ports 9000–9002
```

First use downloads Gradle and dependencies. Java examples need no Node/npm,
Cordova or phone SDK. Native examples use a Java client with a browser loopback
callback. The snippets directory is a runnable console demonstration.

These are book exercises: historical grants, deliberately incomplete starters and
chapter-specific vulnerabilities are retained and explained locally. They are
learning code. Token state resets when an authorization server starts. Read each
lesson's README for behavior and intentional simplifications.

[MIGRATION.md](MIGRATION.md) records the checklist, verification and source fixes.
The original JavaScript/templates are available in Git at commit
`8d837ee48a42358fab220e911c4004530ca40e43`; for example:

```sh
git show 8d837ee48a42358fab220e911c4004530ca40e43:exercises/ch-3-ex-1/client.js
```

This repository accompanies [OAuth 2 in Action](https://www.manning.com/books/oauth-2-in-action)
by Justin Richer and Antonio Sanso. The original license is in [LICENSE.txt](LICENSE.txt).
