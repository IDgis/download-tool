# Stage 1: Gradle 2.12 → 8.x Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Gradle 2.12 and its broken Software Model build with a Gradle 8.8 build using the `org.gradle.playframework` plugin, fixing the broken `jcenter()` dependencies in the process.

**Architecture:** The Software Model (`model {}` / `components {}` / `play {}` DSL) was removed in Gradle 5. Every top-level structural change in `build.gradle` happens here — all later stages touch only Play API version numbers and application source. `buildSrc` is a separate Gradle build; it needs the same jcenter and configuration-name fixes as the root.

**Tech Stack:** Gradle 8.8, `org.gradle.playframework:0.14`, `org.ajoberstar.grgit:grgit-gradle:4.1.1`, `com.bmuschko:gradle-docker-plugin:9.4.0`, Play 2.4.6 (unchanged), Java 8

---

### Before you start

- Branch: `migration/stage-1-gradle-8` (cut from `develop` — this repo follows git-flow; `develop` is the integration branch, `master` is release-only)
- Never commit automatically — per project rules
- Verify command after all tasks: `./gradlew build`

---

### Task 1: Create branch and update the Gradle wrapper

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Create the branch**

```bash
git checkout -b migration/stage-1-gradle-8
```

- [ ] **Step 2: Update the wrapper distribution URL**

In `gradle/wrapper/gradle-wrapper.properties`, replace the single `distributionUrl` line:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
```

The full file should look like:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
```

- [ ] **Step 3: Run the wrapper to download Gradle 8.8**

```bash
./gradlew --version
```

Expected: `Gradle 8.8` in output (download will happen on first run).

---

### Task 2: Fix the root buildscript — jcenter, plugin versions

**Files:**
- Modify: `build.gradle` (buildscript block and imports only)

The current buildscript uses `jcenter()` (shut down Feb 2024) and outdated plugins. This task replaces all three.

- [ ] **Step 1: Replace the `buildscript {}` block**

In `build.gradle`, replace the entire `buildscript { ... }` block (lines 1–9) with:

```groovy
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'org.ajoberstar.grgit:grgit-gradle:4.1.1'
        classpath 'com.bmuschko:gradle-docker-plugin:9.4.0'
        classpath 'org.gradle.playframework:org.gradle.playframework.gradle.plugin:0.14'
    }
}
```

- [ ] **Step 2: Update the imports**

Replace the import block (lines 11–14) with:

```groovy
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import org.gradle.plugins.ide.eclipse.model.SourceFolder
```

(Remove `import org.ajoberstar.grgit.Grgit` — grgit 4.x exposes the repo as a `grgit` project property when the plugin is applied.)

- [ ] **Step 3: Update the version-detection block**

Replace these two lines:

```groovy
def repo = Grgit.open(project.file('.'))
def describe = repo.describe()
```

with:

```groovy
def describe = grgit.describe()
```

The `apply plugin: 'org.ajoberstar.grgit'` line stays as-is.

---

### Task 3: Fix Java subproject configuration names

**Files:**
- Modify: `build.gradle` (the `configure([...])` block for Java projects)

Gradle 8 removed the `compile` and `testCompile` configurations. This task updates the entire `configure([project(':dao'), ...])` block.

- [ ] **Step 1: Replace the `configure([...])` block**

Find the `configure([project(':dao'), project(':domain'), project(':queue'), project(':downloader')])` block and replace it in full:

```groovy
configure([
    project(':dao'),
    project(':domain'),
    project(':queue'),
    project(':downloader')]) {

    apply plugin: 'java'
    apply plugin: 'eclipse'

    sourceSets {
        integration {
            java {
                srcDir file('src/integration/java')
            }
            resources {
                srcDir file('src/integration/resources')
            }
            compileClasspath = sourceSets.main.output + configurations.integrationImplementation
            runtimeClasspath = output + compileClasspath
        }
    }

    configurations {
        integrationImplementation {
            extendsFrom implementation, testImplementation
        }
    }

    task integrationTest(type: Test, group: 'Verification', description: 'Runs the integration tests.') {
        testClassesDirs = sourceSets.integration.output.classesDirs
        classpath = sourceSets.integration.runtimeClasspath
    }

    dependencies {
        implementation 'org.slf4j:slf4j-api:1.7.14'
        implementation 'org.slf4j:slf4j-log4j12:1.7.14'

        testImplementation 'junit:junit:4.12'
        testImplementation 'org.easymock:easymock:3.4'
    }

    jar {
        manifest {
            attributes("Implementation-Title": project.name)
            if (project.version) {
                attributes("Implementation-Version": project.version)
            }
        }
    }
}
```

Key changes: `compile` → `implementation`, `testCompile` → `testImplementation`, `integrationCompile` → `integrationImplementation`, `testClassesDir` → `testClassesDirs`, `.classesDir` → `.classesDirs`.

---

### Task 4: Replace the Software Model with `org.gradle.playframework`

**Files:**
- Modify: `build.gradle` (the `project(':web')` block)

The Software Model's `model { components { play { ... } } }` DSL was removed in Gradle 5. The replacement is the community `org.gradle.playframework` plugin with a conventional `play {}` extension.

- [ ] **Step 1: Replace the `project(':web')` block**

Find the entire `project(':web') { ... }` block and replace it with:

```groovy
project(':web') {

    apply plugin: 'org.gradle.playframework'
    apply plugin: DockerRemoteConfig
    apply plugin: 'eclipse'

    play {
        platform {
            playVersion = '2.4.6'
            scalaVersion = '2.11'
            javaVersion = JavaVersion.VERSION_1_8
        }
        injectedRoutesGenerator = true
    }

    repositories {
        maven {
            name "typesafe-maven-release"
            url "https://repo.typesafe.com/typesafe/maven-releases"
        }
        ivy {
            name "typesafe-ivy-release"
            url "https://repo.typesafe.com/typesafe/ivy-releases"
            layout "ivy"
        }
    }

    task copyTar(type: Copy) {
        dependsOn 'playBinaryTarDist'
        from tarTree("${project.buildDir}/distributions/playBinary.tar")
        into "${project.buildDir}/docker"
    }

    task createDockerfile(type: Dockerfile) {
        dependsOn copyTar
        destFile = project.file('build/docker/Dockerfile')
        from 'azul/zulu-openjdk:8'
        copyFile 'playBinary', '/opt'
        runCommand 'chmod u+x /opt/bin/playBinary'
        exposePort 9000
        defaultCommand '/opt/bin/playBinary'
    }

    task buildImage(type: DockerBuildImage) {
        dependsOn createDockerfile
        inputDir = project.file('build/docker')
        tag = "idgis/${rootProject.name}_${project.name}:${project.version}"
    }

    eclipse {
        classpath {
            plusConfigurations += [ configurations.play ]
            plusConfigurations += [ configurations.playTest ]

            file {
                beforeMerged { classpath ->
                    classpath.entries += [
                        new SourceFolder("app", null)]
                }
            }
        }
    }

    dependencies {
        play 'org.webjars:webjars-play_2.11:2.4.0-2'
        play 'org.webjars:dojo:1.17.2'
        play 'com.typesafe.play:play-java-ws_2.11:2.4.6'
        play 'com.typesafe.play:play-java-jdbc_2.11:2.4.6'

        play project(':domain')
        play project(':dao')
        play project(':queue')
        play 'org.webjars:bootstrap:3.3.6'
        play ("nl.idgis.sys:provisioning-registration:1.1.6-SNAPSHOT") {
            exclude module: "ch.qos.logback"
            exclude module: "logback-classic"
        }

        play 'org.springframework:spring-jdbc:4.2.5.RELEASE'
    }
}
```

**Note on Twirl default imports:** The old model DSL had `sources { twirlTemplates { defaultImports = TwirlImports.JAVA } }`. With `org.gradle.playframework` 0.14, Twirl configuration is done via a `twirl {}` block if needed. If the build fails with Twirl import errors, add inside the `project(':web')` block:

```groovy
twirl {
    defaultImports = "JAVA"
}
```

---

### Task 5: Fix `buildSrc`

**Files:**
- Modify: `buildSrc/build.gradle`

`buildSrc` is a standalone Gradle build. It has the same `jcenter()` and `compile` problems.

- [ ] **Step 1: Replace `buildSrc/build.gradle`**

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.bmuschko:gradle-docker-plugin:9.4.0'
}
```

---

### Task 6: Add smoke test for the web module

**Files:**
- Create: `web/test/resources/application.conf`
- Create: `web/test/SmokeTest.java`

The `web` module has no automated tests. This minimal test proves the built Play application can start and route a request — the regression floor for all later stages.

- [ ] **Step 1: Create the test application configuration**

Create `web/test/resources/application.conf`:

```hocon
download.client = "overijssel"
metadata.url = "http://test.example.com/"
metadata.confidential-path = "test"
metadata.data-public-value = "public"
db.default.driver = "org.h2.Driver"
db.default.url = "jdbc:h2:mem:test;MODE=PostgreSQL"
db.default.username = "sa"
db.default.password = ""
beanstalk.host = "localhost"
beanstalk.queue = "test"
cache.path = "/tmp"
download.access = "extern"
download.trusted.header = "X-Trusted"
download.url.prefix = "http://localhost"
deployment.acceptance = "false"
display.stats.analytics = "false"
play.server.pidfile.path = /dev/null
```

- [ ] **Step 2: Add H2 as a test dependency**

In `build.gradle`, inside `project(':web') { dependencies { ... } }`, add:

```groovy
playTest 'com.h2database:h2:2.2.220'
```

- [ ] **Step 3: Create the smoke test**

Create `web/test/SmokeTest.java`:

```java
import org.junit.Test;
import play.test.Helpers;

import static org.junit.Assert.assertNotEquals;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

public class SmokeTest {
    @Test
    public void helpPageDoesNotCrash() {
        Helpers.running(Helpers.fakeApplication(), () -> {
            play.mvc.Result result = Helpers.route(
                Helpers.fakeRequest("GET", "/help")
            );
            assertNotEquals(INTERNAL_SERVER_ERROR, result.status());
        });
    }
}
```

- [ ] **Step 4: Run the test in isolation to verify it compiles**

```bash
./gradlew :web:test
```

Expected: `BUILD SUCCESSFUL` with `SmokeTest > helpPageDoesNotCrash PASSED`.

If the test fails because `ZooKeeper` tries to connect (it shouldn't — it only connects when `zooKeeper.hosts` is configured, which the test conf omits), add `"zooKeeper.hosts" -> null` to `fakeApplication()` additional config.

---

### Task 7: Verify the full build and update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Run the full build**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. All subprojects compile and tests pass. If there are Gradle API deprecation warnings (not errors), note them for Stage 7.

- [ ] **Step 2: Investigate failures before proceeding**

If the build fails:
- `Could not resolve org.gradle.playframework`: The classpath declaration in `buildscript {}` is wrong. Check the artifact ID in Gradle Plugin Portal.
- `TwirlImports not found`: Add `twirl { defaultImports = "JAVA" }` inside `project(':web')`.
- `playBinaryTarDist task not found`: The dist task name in `org.gradle.playframework` may differ. Run `./gradlew :web:tasks` and find the correct tar dist task name.
- `Dockerfile class not found`: Verify the `com.bmuschko:gradle-docker-plugin:9.4.0` `Dockerfile` task type — it may have been renamed. Check the plugin changelog.

Do not proceed to Stage 2 until `./gradlew build` succeeds.

- [ ] **Step 3: Update `CLAUDE.md` Current State**

In `CLAUDE.md`, update the `## Current State` block:

```markdown
## Current State
- Stage: 1 (complete)
- Play: 2.4.6 | Gradle: 8.8 | Java: 8
```
