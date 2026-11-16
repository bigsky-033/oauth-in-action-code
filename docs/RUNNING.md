# Run any directory from the root

Import the repository root as a Gradle project in IntelliJ Community, and choose
JDK 21 for both Project SDK and Gradle JVM. Wait for Gradle import to finish.
Open the main class listed below and use its gutter Run button. The fully qualified
packages are different in every module, so equal simple names do not conflict.
For a Gradle configuration, choose the complete task path shown below.

Only one web example can use the book's fixed ports at a time: client 9000,
authorization server 9001, resource 9002. Stop all processes from the previous
example before running another. A combined main starts all three roles. The
snippets main prints to the console and uses no ports. Static files and default
storage paths are independent of IntelliJ's working directory.

`run` selects the completed solution. Where available, `runStarter` selects the
unfinished exercise; its ordinary `StarterExercise.main()` is in the same module.
Starter TODOs either return an explicit 501 or retain the weakness being taught,
as described in that directory's README. Editing the completed source is optional.

Each row links to lesson-specific instructions, separate-server mains, scope/user
fixtures, storage and tests. For example, the Gradle task `:exercises:ch-3-ex-1:run`
means `./gradlew :exercises:ch-3-ex-1:run` at the root.

| Directory / lesson | Gradle task | IntelliJ main class | Starter task suffix |
| --- | --- | --- | --- |
| [Chapter 3, exercise 1: authorization-code client](../exercises/ch-3-ex-1/README.md) | `:exercises:ch-3-ex-1:run` | `org.oauthinaction.ch03ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 3, exercise 2: refreshing an access token](../exercises/ch-3-ex-2/README.md) | `:exercises:ch-3-ex-2:run` | `org.oauthinaction.ch03ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 4, exercise 1: protecting a resource](../exercises/ch-4-ex-1/README.md) | `:exercises:ch-4-ex-1:run` | `org.oauthinaction.ch04ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 4, exercise 2: word API scopes](../exercises/ch-4-ex-2/README.md) | `:exercises:ch-4-ex-2:run` | `org.oauthinaction.ch04ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 4, exercise 3: filtering produce by scope](../exercises/ch-4-ex-3/README.md) | `:exercises:ch-4-ex-3:run` | `org.oauthinaction.ch04ex03.completed.CompletedExercise` | `runStarter` |
| [Chapter 4, exercise 4: user-specific favorites](../exercises/ch-4-ex-4/README.md) | `:exercises:ch-4-ex-4:run` | `org.oauthinaction.ch04ex04.completed.CompletedExercise` | `runStarter` |
| [Chapter 5, exercise 1: building the authorization server](../exercises/ch-5-ex-1/README.md) | `:exercises:ch-5-ex-1:run` | `org.oauthinaction.ch05ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 5, exercise 2: issuing and processing refresh tokens](../exercises/ch-5-ex-2/README.md) | `:exercises:ch-5-ex-2:run` | `org.oauthinaction.ch05ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 5, exercise 3: carrying granted scopes through the protocol](../exercises/ch-5-ex-3/README.md) | `:exercises:ch-5-ex-3:run` | `org.oauthinaction.ch05ex03.completed.CompletedExercise` | `runStarter` |
| [Chapter 6, exercise 1: implicit grant](../exercises/ch-6-ex-1/README.md) | `:exercises:ch-6-ex-1:run` | `org.oauthinaction.ch06ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 6, exercise 2: client credentials](../exercises/ch-6-ex-2/README.md) | `:exercises:ch-6-ex-2:run` | `org.oauthinaction.ch06ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 6, exercise 3: resource owner password grant](../exercises/ch-6-ex-3/README.md) | `:exercises:ch-6-ex-3:run` | `org.oauthinaction.ch06ex03.completed.CompletedExercise` | `runStarter` |
| [Chapter 6, exercise 4: complete browser client](../exercises/ch-6-ex-4/README.md) | `:exercises:ch-6-ex-4:run` | `org.oauthinaction.ch06ex04.Exercise` | — |
| [Chapter 6, exercise 5: desktop Java client](../exercises/ch-6-ex-5/README.md) | `:exercises:ch-6-ex-5:run` | `org.oauthinaction.ch06ex05.Exercise` | — |
| [Chapter 7, exercise 1: dynamically registering a native client](../exercises/ch-7-ex-1/README.md) | `:exercises:ch-7-ex-1:run` | `org.oauthinaction.ch07ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 8, exercise 1: protecting response content](../exercises/ch-8-ex-1/README.md) | `:exercises:ch-8-ex-1:run` | `org.oauthinaction.ch08ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 8, exercise 2: cross-origin resource requests](../exercises/ch-8-ex-2/README.md) | `:exercises:ch-8-ex-2:run` | `org.oauthinaction.ch08ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 8, exercise 3: the transport-security header](../exercises/ch-8-ex-3/README.md) | `:exercises:ch-8-ex-3:run` | `org.oauthinaction.ch08ex03.completed.CompletedExercise` | `runStarter` |
| [Chapter 9, exercise 1: binding the redirect URI at token exchange](../exercises/ch-9-ex-1/README.md) | `:exercises:ch-9-ex-1:run` | `org.oauthinaction.ch09ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 9, exercise 2: local authorization-request errors](../exercises/ch-9-ex-2/README.md) | `:exercises:ch-9-ex-2:run` | `org.oauthinaction.ch09ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 10, exercise 1: PKCE](../exercises/ch-10-ex-1/README.md) | `:exercises:ch-10-ex-1:run` | `org.oauthinaction.ch10ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 11, exercise 1: unsigned JWT format and claims](../exercises/ch-11-ex-1/README.md) | `:exercises:ch-11-ex-1:run` | `org.oauthinaction.ch11ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 11, exercise 2: HMAC-signed JWTs](../exercises/ch-11-ex-2/README.md) | `:exercises:ch-11-ex-2:run` | `org.oauthinaction.ch11ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 11, exercise 3: RSA-signed JWTs](../exercises/ch-11-ex-3/README.md) | `:exercises:ch-11-ex-3:run` | `org.oauthinaction.ch11ex03.completed.CompletedExercise` | `runStarter` |
| [Chapter 11, exercise 4: token introspection](../exercises/ch-11-ex-4/README.md) | `:exercises:ch-11-ex-4:run` | `org.oauthinaction.ch11ex04.completed.CompletedExercise` | `runStarter` |
| [Chapter 11, exercise 5: token revocation](../exercises/ch-11-ex-5/README.md) | `:exercises:ch-11-ex-5:run` | `org.oauthinaction.ch11ex05.completed.CompletedExercise` | `runStarter` |
| [Chapter 12, exercise 1: dynamic client registration](../exercises/ch-12-ex-1/README.md) | `:exercises:ch-12-ex-1:run` | `org.oauthinaction.ch12ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 12, exercise 2: registration management](../exercises/ch-12-ex-2/README.md) | `:exercises:ch-12-ex-2:run` | `org.oauthinaction.ch12ex02.completed.CompletedExercise` | `runStarter` |
| [Chapter 13, exercise 1: OpenID Connect](../exercises/ch-13-ex-1/README.md) | `:exercises:ch-13-ex-1:run` | `org.oauthinaction.ch13ex01.completed.CompletedExercise` | `runStarter` |
| [Chapter 15, exercise 1: proof-of-possession tokens](../exercises/ch-15-ex-1/README.md) | `:exercises:ch-15-ex-1:run` | `org.oauthinaction.ch15ex01.completed.CompletedExercise` | `runStarter` |
| [Appendix A, exercise 0: HTTP scaffolding](../exercises/ap-A-ex-0/README.md) | `:exercises:ap-A-ex-0:run` | `org.oauthinaction.appendixa.Exercise` | — |
| [Complete OAuth example](../example/README.md) | `:example:run` | `org.oauthinaction.examplemain.Exercise` | — |
| [Chapter 8 baseline variants](../example/chapter8/README.md) | `:example:chapter8:run` | `org.oauthinaction.examplechapter8.Exercise` | — |
| [Chapter 8 solution 0: response content](../example/chapter8/solution0/README.md) | `:example:chapter8:solution0:run` | `org.oauthinaction.examplesolution0.Solution` | — |
| [Chapter 8 solution 1: CORS](../example/chapter8/solution1/README.md) | `:example:chapter8:solution1:run` | `org.oauthinaction.examplesolution1.Solution` | — |
| [Chapter 8 solution 2: HSTS response header](../example/chapter8/solution2/README.md) | `:example:chapter8:solution2:run` | `org.oauthinaction.examplesolution2.Solution` | — |
| [Chapter 10 standalone example: plain PKCE](../example/chapter10/README.md) | `:example:chapter10:run` | `org.oauthinaction.examplechapter10.Exercise` | — |
| [Standalone native client](../example/native-client/README.md) | `:example:native-client:run` | `org.oauthinaction.examplenative.Exercise` | — |
| [Classroom: starting stage](../class/README.md) | `:class:run` | `org.oauthinaction.classroom.Exercise` | — |
| [Classroom: start of day two](../class/day2-start/README.md) | `:class:day2-start:run` | `org.oauthinaction.classroomday2.Exercise` | — |
| [Classroom: completed OpenID Connect flow](../class/complete/README.md) | `:class:complete:run` | `org.oauthinaction.classroomcomplete.Exercise` | — |
| [Classroom protocol snippets](../class/snippets/README.md) | `:class:snippets:run` | `org.oauthinaction.classroomsnippets.SnippetDemo` | — |

## Separate servers and alternative clients

Each web module also exposes ordinary mains for the three roles. Use exactly one
client and one resource variant with an authorization server from the same module.
The authorization server starts first and resets demo tokens; restarting it makes
existing access tokens inactive. It is the only writer of the small JSON token
file under `${java.io.tmpdir}/oauth-in-action/`; resource JVMs either read the file
or make the lesson's HTTP introspection call. Tests always use isolated files.

The main example and standalone chapters 8/10 have `ImplicitClient.main()` as an
alternative to the web client, on port 9000. Chapter 8 resource variants and solution
0a/0b/0c have their own mains; see those READMEs. Native clients use an ordinary
browser and localhost callback, replacing Cordova's custom phone URI.

## Verification

`./gradlew check` runs all modules' tests and repository consistency checks.
Python 3 is needed for the latter. Tests bind temporary ports and cover real HTTP
protocol exchanges plus chapter-specific rejection cases. No Node runtime is
needed to run or test the Java applications. Optional browser development checks
use `node tools/test_browser_clients.mjs`.

`./gradlew smokeLaunchers` builds installed distributions, runs every default and
starter main from a temporary directory, checks local pages/assets, runs alternate
chapter 8 and implicit variants, and exercises chapter 3 code/refresh exchanges
across three JVMs. It requires free ports 9000–9002 and refuses occupied ports.
First use of the Gradle wrapper downloads its pinned distribution and dependencies.
