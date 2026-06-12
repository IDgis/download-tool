# Migration Design: Play 2.4.6 / Gradle 2.12 / Java 8 → Play 3.0 / Gradle 9 / Java 21

**Date:** 2026-06-10  
**Status:** Approved  
**Approach:** Strict sequential stages (Option A) with a minimal smoke test added in Stage 1

---

## 1. Current State & Constraints

### The build is already broken

`jcenter()` was shut down in February 2024. Two build-time dependencies resolve only from jcenter:
- `org.ajoberstar:gradle-git:1.3.2` (git-based versioning)
- `com.bmuschko:gradle-docker-plugin:3.0.7`

Stage 1 must fix these before anything else can proceed.

### The Gradle Software Model is gone

The `model {}` / `components {}` / `play {}` DSL in `build.gradle` was removed in Gradle 5. The replacement is the community `org.gradle.playframework` plugin, which uses a `plugins {}` block and a conventional `play {}` extension. This is the central work of Stage 1.

### No web module test coverage

The `web` module has no automated tests. Every Play API change (Stages 2–8) is verified only by build success and a single smoke test added in Stage 1. A couple of integration tests exist in `downloader/src/integration` — those cover the downloader module only.

### `provisioning-registration` is a cross-cutting dependency

`nl.idgis.sys:provisioning-registration` is an IDgis-owned library that uses Play internals directly: `play.libs.F.Promise`, `play.Configuration`, `ApplicationLifecycle`. It requires three coordinated releases aligned with Play migration stages (see Section 5).

### Docker base images

Both `web` and `downloader` Dockerfiles reference `azul/zulu-openjdk:8`. These update at the Java upgrade stages (4, 6, 9).

---

## 2. Stage-by-Stage Breakdown

Each stage lives on its own branch (`migration/stage-N-description`), verified with `./gradlew build` before merging. The `## Current State` block in `CLAUDE.md` is updated on each merge.

---

### Stage 1 — Gradle 2.12 → 8.x `(Java 8, Play 2.4.6)`

The most complex stage. Beyond upgrading the wrapper, the entire Software Model must be replaced.

| Item | Change |
|---|---|
| `gradle-wrapper.properties` | `2.12` → `8.8` |
| `jcenter()` in root and `buildSrc` | → `mavenCentral()` |
| `org.ajoberstar:gradle-git:1.3.2` | → `org.ajoberstar.grgit:grgit-gradle:4.x` |
| `com.bmuschko:gradle-docker-plugin:3.0.7` | → `9.x` |
| Software Model `model {}` / `play {}` block | → `org.gradle.playframework` plugin (`0.14+`) |
| `compile` / `testCompile` configs | → `implementation` / `testImplementation` |
| `configurations.integrationCompile` | → `configurations.integrationImplementation` |
| `testClassesDir` on `integrationTest` task | → `testClassesDirs` |
| `sourceSets.integration.output.classesDir` | → `.classesDirs` |
| `buildSrc/build.gradle` `compile` config | → `implementation` |
| Smoke test | Add one route test to `web` using `Helpers.running()` |

Play version stays at 2.4.6. All application code is untouched.

---

### Stage 2 — Play 2.4.6 → 2.5 → 2.6 `(Gradle 8, Java 8)`

Two micro-commits on one branch. Play 2.5 is an intermediate step where `F.Promise` is deprecated but still compiles. Play 2.6 removes it.

**Micro-commit 1: Play 2.4.6 → 2.5**
- Update platform to `play: '2.5.x'`, `scala: '2.12'` (switch to 2.12 now; it is required from 2.6 onwards)
- Update `play-java-ws` and `webjars-play` versions and `_2.12` suffixes
- Build must pass; `F.Promise` shows deprecation warnings, not errors

**Micro-commit 2: Play 2.5 → 2.6** (largest code change in the project)
- `play.libs.F.Promise<Result>` → `CompletionStage<Result>` in `DownloadForm`, `Metadata`, `MetadataProvider`
- `Form.form(X.class)` static call → inject `FormFactory`, use `formFactory.form(X.class)` in `DownloadForm`
- `Routes.javascriptRouter(...)` → `JavaScriptReverseRouter.create(...)` in `DownloadForm`
- `play.http.DefaultHttpErrorHandler` constructor signature changes — adjust `ErrorHandler`
- `play.api.UsefulException` in `error.scala.html` → `play.api.http.HttpErrorInfo`
- Update `provisioning-registration` to `2.0.x` (remove `F.Promise` from stop hook → `CompletableFuture.completedFuture(null)`)

---

### Stage 3 — Play 2.6 → 2.7 → 2.8 `(Gradle 8, Java 8)`

**Micro-commit 1: Play 2.6 → 2.7**
- Verify build passes cleanly; mostly deprecation warnings
- `play.Configuration` still compiles (deprecated, not removed until 2.9)

**Micro-commit 2: Play 2.7 → 2.8** (second major code change)
- `WebJarAssets` **removed** → replace with `org.webjars.play.WebJarsUtil` in all five controllers
- All seven Twirl templates pass `webJarAssets: WebJarAssets` as first argument → replace with `webJarsUtil: WebJarsUtil` and update all calls from `webJarAssets.locate(...)` to `webJarsUtil.locate(...)`
- `response().setHeader(...)` / `response().setContentType(...)` → `Result.withHeader(...)` / `Result.as(...)` in `DownloadResult` and `DownloadRaster`
- Update `webjars-play` dependency to `org.webjars.play:webjars-play_2.12:2.8.x`

---

### Stage 4 — Java 8 → 11 `(Gradle 8, Play 2.8)`

Primarily infrastructure; no Play API changes.

- Play platform: `java: '1.8'` → `java: '11'`
- Dockerfiles (`web`, `downloader`): `azul/zulu-openjdk:8` → `azul/zulu-openjdk:11`
- CI workflow: `java-version: 8` → `java-version: 11`
- Verify `javax.xml` and `javax.inject` remain available (they are in Java 11)
- Spring JDBC 5.3.39 and PostgreSQL driver 42.7.7 are both Java 11 compatible — no changes needed

---

### Stage 5 — Play 2.8 → 2.9 `(Gradle 8, Java 11)`

Play 2.9 removes all APIs deprecated since 2.6. Second large code-change stage.

- `play.Configuration` **removed** → inject `com.typesafe.config.Config` directly in all constructors: `DownloadForm`, `DownloadResult`, `DownloadRaster`, `MetadataProvider`, `ZooKeeper`, `Cache`
- `play.Logger` / `play.Logger.ALogger` **removed** → `org.slf4j.LoggerFactory.getLogger(X.class)` throughout (`DownloadRaster` already uses SLF4J directly — use it as the pattern)
- Scala 2.13 required — update platform declaration and all `_2.12` dependency suffixes to `_2.13`
- `injectedRoutesGenerator` is the only available mode (already enabled — no change)
- Update `provisioning-registration` to `3.0.x` (`play.Configuration` → `Config`, `play.Logger` → SLF4J)

---

### Stage 6 — Java 11 → 17 `(Gradle 8, Play 2.9)`

- Dockerfiles: `azul/zulu-openjdk:11` → `azul/zulu-openjdk:17`
- CI: `java-version: 11` → `java-version: 17`
- Verify no `--add-opens` flags needed for Spring JDBC and PostgreSQL driver at Java 17 strong-encapsulation boundary (5.3.x / 42.7.x are both clean)
- Test `commons-cache:0.0.15` specifically at this step (see Risk R4)
- No Play API changes

---

### Stage 7 — Gradle 8 → 9 `(Java 17, Play 2.9)`

- Update `gradle-wrapper.properties` to Gradle 9.x
- Update `org.gradle.playframework` to latest (may be required for Gradle 9 compatibility)
- Address any Gradle 9 deprecation warnings accumulated across previous stages (configuration resolution, convention plugin API)

---

### Stage 8 — Play 2.9 → 3.0 `(Gradle 9, Java 17)`

The most uncertain stage. Play 3.0 renamed the entire package namespace.

- All `com.typesafe.play.*` imports → `org.playframework.*`
- `play-java-ws`, `webjars-play`, `play-java-jdbc` group IDs change accordingly
- Pekko replaces Akka internally (mostly invisible from the Java API, but configuration keys in `application.conf` change)
- Drop the Typesafe ivy layout repository — Play 3.x artifacts are on Maven Central
- Update `provisioning-registration` to `4.0.x` (namespace change)
- **Verify `org.gradle.playframework` plugin Play 3.0 support before starting** (see Risk R1)

---

### Stage 9 — Java 17 → 21 `(Gradle 9, Play 3.0)`

- Dockerfiles: `azul/zulu-openjdk:17` → `azul/zulu-openjdk:21`
- CI: `java-version: 17` → `java-version: 21`
- No additional API changes; Security Manager was already fully removed in Java 17
- Virtual threads available as an optional follow-up enhancement

---

## 3. Risk Register

### R1 — `org.gradle.playframework` plugin and Play 3.0 — HIGH

The community plugin has no guaranteed Play 3.0 support. At the time of writing it covers Play 2.4–2.8 reliably; Play 3.0 support is partial and untested in production.

**Mitigation:** Before starting Stage 8, inspect the plugin's release notes and open issues on GitHub. If Play 3.0 is unsupported, the fallback is executing `sbt dist` from a Gradle `exec` task and consuming the output artifact — preserving the Gradle outer shell without requiring a full build system migration.

### R2 — Typesafe/Lightbend Maven repositories — MEDIUM

`repo.typesafe.com/typesafe/maven-releases` and the ivy layout equivalent are referenced in the `web` build. Some artifacts were migrated to `repo.lightbend.com`; Play 2.6+ artifacts are on Maven Central directly. These repos may return 404s for certain versions.

**Mitigation:** Verify per stage which repository serves the target Play version. From Stage 2 onward, prefer Maven Central. Drop the ivy repo entirely at Stage 8.

### R3 — `provisioning-registration` blocking each Play stage — MEDIUM

The library requires three coordinated releases. If a release is not ready when a stage starts, the stage is blocked.

**Mitigation:** Prepare each library update as a parallel track ahead of the dependent stage (see Section 5).

### R4 — `nl.idgis.commons:commons-cache:0.0.15` on Java 17 — LOW/UNKNOWN

A private IDgis library used in `downloader`. If it uses internal JDK APIs it may fail at the Java 17 strong-encapsulation boundary.

**Mitigation:** Run `./gradlew :downloader:build` in isolation at Stage 6 and watch for `InaccessibleObjectException`. Update the library if needed.

### R5 — `com.dinstone:beanstalkc:2.2.0` on newer JVMs — LOW

An old Beanstalk client from ~2016. Unlikely to hit JVM encapsulation issues (uses raw sockets), but untested on Java 17/21.

**Mitigation:** Flag for explicit testing at Stage 6. A newer version or replacement (`spring-beanstalkd`) exists if needed.

### R6 — No `web` test coverage — MEDIUM (ongoing)

A broken controller method or Twirl template could go undetected until manual testing.

**Mitigation:** The smoke test added in Stage 1 is the minimum floor. Adding one controller test per stage while touching that controller costs little and raises the regression signal considerably.

---

## 4. Scala Version Coupling

Play ties its binary-compatible artifact suffixes to Scala. All `_2.11` and `_2.12` suffixes in `build.gradle` (`webjars-play`, `play-java-ws`, `play-java-jdbc`) must be updated at the appropriate stage.

| Play version | Scala version | Dependency suffix | Switch at |
|---|---|---|---|
| 2.4.6 | 2.11 | `_2.11` | — |
| 2.5–2.8 | 2.12 | `_2.12` | Stage 2, micro-commit 1 |
| 2.9 | 2.13 | `_2.13` | Stage 5 |
| 3.0 | 2.13 / Scala 3 | `_2.13` | Stage 8 |

---

## 5. `provisioning-registration` Release Coordination

Three coordinated releases are required. Each should be prepared and published before its dependent stage starts.

| Library version | Aligned with | Key changes |
|---|---|---|
| `2.0.x` | Stage 2 (Play 2.6) | `F.Promise` → `CompletableFuture` in stop hook |
| `3.0.x` | Stage 5 (Play 2.9) | `play.Configuration` → `com.typesafe.config.Config`, `play.Logger` → SLF4J |
| `4.0.x` | Stage 8 (Play 3.0) | `com.typesafe.play` → `org.playframework` namespace |

---

## 6. Docker & CI Updates

### Dockerfile base images

| Stage | Image tag |
|---|---|
| Stage 4 | `azul/zulu-openjdk:11` |
| Stage 6 | `azul/zulu-openjdk:17` |
| Stage 9 | `azul/zulu-openjdk:21` |

Both `web` and `downloader` Dockerfiles update at each of these stages.

### CI workflow (`build-project.yml`)

The `java-version` field updates at Stages 4, 6, and 9. The workflow currently triggers only on PRs to `develop` — this is fine for the migration branch strategy.

---

## 7. Branch Strategy

Each stage uses a dedicated branch named `migration/stage-N-description`, merged via PR. On each merge, update the `## Current State` block in `CLAUDE.md` to reflect the new versions so the project file always reflects where the migration stands.

Example branch names:
- `migration/stage-1-gradle-8`
- `migration/stage-2-play-26`
- `migration/stage-3-play-28`
- `migration/stage-4-java-11`
- `migration/stage-5-play-29`
- `migration/stage-6-java-17`
- `migration/stage-7-gradle-9`
- `migration/stage-8-play-30`
- `migration/stage-9-java-21`
