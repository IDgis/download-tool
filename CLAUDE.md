# Migration Project Rules

## Current State
- Stage: 4 (complete)
- Play: 2.8.22 | Gradle: 8.8 | Java: 11
- **The entire remaining migration (Stages 5-9) is BLOCKED on one root cause, not five separate ones.** The `org.gradle.playframework` build plugin (all published versions through the latest, `0.16.0`) has no support for Play 2.9 — confirmed by decompiling the plugin, not just its docs. This isn't only Stage 5's problem: checked directly (2026-08-12) whether Stage 6 (Java 17) or Stage 7 (Gradle 9) could proceed independently, since neither touches Play version — both are transitively blocked too. Play's own documentation caps Play 2.8.x at Java SE 8-11 (Java 17 needs Play 2.9), and Gradle 9.0 itself requires JVM 17 minimum just to launch its daemon (this project has no toolchain configured, so that JDK also ends up running compilation and tests). So Stage 4 (Java 11, complete) is the last reachable milestone until the Play-plugin question resolves — no other stage is worth attempting in the meantime. See the blocker banner at the top of `docs/superpowers/plans/2026-06-12-stage-5-play-29.md` for full details and options.

## Target Stack
- Gradle: 8.x (intermediate) → 9.x (final)
- Java: 8 (current) → 17 (intermediate) → 21 (final)
- Play: 2.4.6 → 2.5 → 2.6 → 2.7 → 2.8 → 3.0

## Stage Order
1. Gradle 2.12 → 8.x           (Java 8, Play 2.4.6)
2. Play 2.4.6 → 2.5 → 2.6      (Gradle 8, Java 8)
3. Play 2.6 → 2.7 → 2.8        (Gradle 8, Java 8)
4. Java 8 → 11                 (Gradle 8, Play 2.8 — minimum for Play 2.9)
5. Play 2.8 → 2.9              (Gradle 8, Java 11)
6. Java 11 → 17                (Gradle 8, Play 2.9)
7. Gradle 8 → 9                (Java 17, Play 2.9)
8. Play 2.9 → 3.0              (Gradle 9, Java 17)
9. Java 17 → 21

## Boundaries
- NEVER commit to git automatically
- NEVER read .env, .env.*, *.secret, or any secrets files
- NEVER run destructive commands (drop, delete, truncate) without explicit confirmation
- Only modify files relevant to the current migration stage
- Follow stage order strictly. Verify `./gradlew build` after each stage. Update `## Current State` on each merge.

## Editing Files
- This repo has mixed line endings — many files use Windows CRLF. Never use Python (or similar scripts) for bulk find/replace edits: Python's default text-mode file I/O silently normalizes CRLF to LF, which flips every line of a file in `git diff` even when only a few lines actually changed. Use the Edit tool instead.

## Build Verification
- Always verify with: `./gradlew build` after changes
- Report errors before proceeding to next step
- **On a Java-version stage (4, 6, 9), `./gradlew build` succeeding proves nothing by itself.** Root cause, confirmed by decompiling `gradle-playframework-0.14`: `javaVersion` in the `play { platform { ... } }` block is consumed by exactly one place in the whole plugin — `PlayIdeaPlugin.getTargetJavaVersion(...)`, which only feeds generated IntelliJ `.iml` files. It is never wired into `sourceCompatibility`/`targetCompatibility` (grepped every class in the plugin jar, zero matches) or a Gradle toolchain, and this project's own `build.gradle` doesn't set those either, for any subproject. With nothing connecting `javaVersion` to actual compilation, `javac`/Zinc silently fall back to compiling at whatever bytecode level is native to whichever JDK is on `JAVA_HOME` — no error, no warning, regardless of the declared setting. Confirmed empirically during Stage 4 (2026-08-12): the identical `build.gradle` produced class-file major version 55 (Java 11) under a JDK 11 `JAVA_HOME`, and major version 52 (Java 8) under Zulu 8 — same command, `BUILD SUCCESSFUL` both times. (Runtime testing won't catch this either — Java bytecode is forward-compatible, so even Java-8-targeted classes run fine on the Docker image's Java 11 JRE; the mismatch is invisible except in the `.class` file header itself.) **Before trusting a Java-version-stage build as verified, explicitly point `JAVA_HOME` at the target JDK first**, and ideally spot-check the actual output bytecode version (`javap -v <a .class file> | grep "major version"`) rather than trusting a green build or a green Docker test alone. CI is unaffected by this gap — `.github/workflows/build-project.yml` pins the JDK explicitly via `setup-java@v4`, so the officially merged build is genuinely compiled at the right level regardless of any local `JAVA_HOME` drift.

## Docs
- Migration spec: `docs/superpowers/specs/2026-06-10-migration-design.md`
- Stage plans: `docs/superpowers/plans/`
