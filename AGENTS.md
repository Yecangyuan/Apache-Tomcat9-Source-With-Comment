# Repository Guidelines

## Project Structure & Module Organization
Source under `java/` mirrors the runtime modules (`org/apache/catalina`, `org/apache/coyote`, etc.), while reusable add-ons live in `modules/`. Scripts and launchers are in `bin/`, baseline configs in `conf/`, and shipping webapps plus docs under `webapps/`. Integration fixtures and servlet samples for automated tests live in `test/`. Keep personal overrides in `build.properties`; generated content should stay confined to `output/` so the tree remains clean.

## Build, Test, and Development Commands
- `ant` — default deploy target; compiles everything, assembles a runnable distro in `output/build/`.
- `ant release` — reproduces the ASF release layout; requires the toolchain declared in `build.properties.release`.
- `ant test` — executes the full connector matrix (NIO, NIO2, APR) and drops reports under `output/build/logs/`.
- `ant ide-intellij` / `ant ide-eclipse` / `ant ide-netbeans` — generates project metadata after checkout or branch switches.
Always create a `build.properties` overriding `base.path` so dependency downloads do not pollute the source tree.

## Coding Style & Naming Conventions
Follow the loose but consistent rules in `CONTRIBUTING.md`: four-space indentation for Java, two for XML, spaces instead of tabs, braces on the same line, and a 100-character limit for Java (80 for docs). Package and class names stay in the existing namespace layout; new tests should use the `Test*.java` or `Tester*Performance.java` patterns already defined in the Ant filters. Respect `.editorconfig` defaults before submitting patches.

## Testing Guidelines
JUnit lives under `test/org/...`; helper webapps sit in `test/webapp-*` and are auto-deployed during `ant test`. Limit runs with `test.entry=<FQN>` or pattern-based `test.name` in `build.properties`; exclude slow suites with `test.exclude` or toggle connector runs via `execute.test.nio*`. Set `test.threads` to control parallelism, `test.accesslog=true` for HTTP traces, and `test.cobertura=true` plus `test.threads=1` when collecting coverage (`output/coverage/`). Keep APR binaries reachable via `test.apr.loc` if you enable APR connector testing.

## Commit & Pull Request Guidelines
As outlined in `CONTRIBUTING.md`, GitHub pull requests are preferred, with Bugzilla-attached patches or dev-list emails as fallbacks. Align commit messages with upstream practice: lead with the affected area (`catalina`, `webapps/docs`, etc.), keep the first line imperative, and reference the Bugzilla ID when applicable (e.g., `BZ 67890 - Coyote: tighten HTTP/2 flow control`). Every PR should explain motivation, include reproduction or testing notes, and update docs or configuration defaults when behavior changes. Stay responsive to reviewer feedback and rerun `ant`/`ant test` after each revision so reviewers can focus on the code instead of build hygiene.
