# Stage 7: Gradle 8 → 9 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the Gradle wrapper from 8.x to 9.x, addressing any deprecation warnings accumulated since Stage 1 and verifying that `org.gradle.playframework` is compatible with Gradle 9.

**Architecture:** Gradle 9 is a major version with breaking changes to the configuration resolution API and convention plugin API. Deprecation warnings from earlier Gradle 8 builds are errors in Gradle 9. The `org.gradle.playframework` plugin may need an update for Gradle 9 compatibility — check the plugin's release history before starting. No Play API or application source changes.

**Tech Stack:** Gradle 9.x, Play 2.9.x, Java 17

---

### Before you start

- Prerequisite: Stage 6 complete (`./gradlew build` passing on `master`)
- Branch: `migration/stage-7-gradle-9` (cut from `master`)
- **Check first:** Inspect `org.gradle.playframework` GitHub releases to confirm Gradle 9 support before updating the wrapper.
- Verify command: `./gradlew build`

---

### Task 1: Create branch and check plugin compatibility

- [ ] **Step 1: Create the branch**

```bash
git checkout -b migration/stage-7-gradle-9
```

- [ ] **Step 2: Check `org.gradle.playframework` release notes**

Before changing the wrapper version, verify that `org.gradle.playframework` has a release compatible with Gradle 9. Look at the plugin's GitHub releases page (`https://github.com/gradle/playframework`).

If no Gradle 9 compatible release exists, this stage is blocked until the plugin supports it. The fallback documented in the spec is to execute `sbt dist` from a Gradle `exec` task — this is a last resort and a separate decision requiring review.

If a compatible plugin version is available, note it and proceed.

---

### Task 2: Collect deprecation warnings from the current Gradle 8 build

**Files:** none (read-only step)

- [ ] **Step 1: Run the build with deprecation warnings as errors**

This surfaces all warnings that will become errors in Gradle 9:

```bash
./gradlew build --warning-mode all 2>&1 | grep -i "deprecat"
```

Read the output carefully. Common Gradle 8→9 deprecations:
- Configuration resolution at configuration time (accessing `configurations.x.files` during configuration)
- Convention plugin property API (`.convention(...)` vs direct assignment)
- `testClassesDirs` / `classesDirs` property style

Note down each distinct deprecation pattern for Task 3.

---

### Task 3: Fix deprecation warnings before updating the wrapper

**Files:**
- Modify: `build.gradle` (as needed per warnings found in Task 2)

This task is open-ended — fix what `--warning-mode all` reported. Common patterns:

**Configuration resolution during configuration phase:**

If you see `Configuration 'integrationImplementation' was resolved during configuration time`, wrap the resolution in `afterEvaluate`:
```groovy
afterEvaluate {
    // move the configuration resolution here if needed
}
```

Or use lazy resolution APIs: `configurations.integrationImplementation.incoming.resolutionResult` instead of `.files`.

**Deprecated `testClassesDirs` property API (if Gradle 9 changes it again):**

Verify the exact Gradle 9 property name in the Gradle 9 release notes.

**Play plugin compatibility issues:**

If `org.gradle.playframework` emits deprecation warnings related to the Software Component API, update to the newer plugin version found in Task 1.

- [ ] **Step 1: Apply each fix found in Task 2**

Make targeted fixes. Run `./gradlew build --warning-mode all` after each fix to verify it is resolved.

---

### Task 4: Update the Gradle wrapper

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Update `distributionUrl`**

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip
```

(Use the latest stable Gradle 9 release — check `https://gradle.org/releases/` for the latest version number.)

- [ ] **Step 2: Update `org.gradle.playframework` in `build.gradle` if a new version is needed**

In `buildscript { dependencies { ... } }`:

```groovy
classpath 'org.gradle.playframework:org.gradle.playframework.gradle.plugin:X.Y.Z'
```

Replace `X.Y.Z` with the Gradle 9 compatible version found in Task 1.

---

### Task 5: Verify the full build under Gradle 9

- [ ] **Step 1: Download Gradle 9 and run the build**

```bash
./gradlew --version
./gradlew build
```

Expected: `Gradle 9.x` version output, then `BUILD SUCCESSFUL` with zero deprecation warnings.

If there are new errors after the wrapper update:
- Read each error carefully — Gradle 9 errors are usually self-explanatory about which deprecated API was used
- Fix the specific API call identified in the error
- Do not proceed to Stage 8 until the build is clean

- [ ] **Step 2: Update CLAUDE.md**

In `CLAUDE.md`, update the `## Current State` block:

```markdown
## Current State
- Stage: 7 (complete)
- Play: 2.9.x | Gradle: 9.x | Java: 17
```
