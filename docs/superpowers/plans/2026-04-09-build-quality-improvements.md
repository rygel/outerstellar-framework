# Build Quality and Code Safety Improvements

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix ten identified quality gaps: Surefire timeouts, enforcer rules, jvmTarget duplication, PluginManager race condition, I18nService atomic locale change, publish-central test gate, README version staleness, and CONTRIBUTING.md.

**Architecture:** Build changes go in `pom.xml` and child module pom files. Code safety changes go in source + test files. CI changes go in `.github/workflows/`. Documentation changes go in root Markdown files. All work is on a new branch off `develop`.

**Tech Stack:** Maven, Kotlin, JUnit Jupiter, GitHub Actions

---

### Task 1: Create feature branch

**Files:** none (git only)

- [ ] **Step 1: Create and switch to new branch**

```bash
git checkout develop
git pull origin develop
git checkout -b chore/build-quality-improvements
```

- [ ] **Step 2: Verify clean state**

```bash
git status
```

Expected: `nothing to commit, working tree clean`

- [ ] **Step 3: Commit (none — branch creation only)**

No commit needed for this task.

---

### Task 2: Add Surefire timeout and JaCoCo argLine to parent pom

**Files:**
- Modify: `pom.xml:284-286` (maven-surefire-plugin in pluginManagement)

The Surefire plugin has no configuration. A hung test blocks CI forever. `forkedProcessTimeoutInSeconds=300` kills the forked JVM after 5 minutes. `@{argLine}` (late binding — note the `@`, not `$`) ensures JaCoCo's agent string is passed to the forked JVM even though `argLine` is set during the `initialize` phase.

- [ ] **Step 1: Replace the bare Surefire declaration in `pom.xml` pluginManagement**

Find (around line 284):
```xml
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven.surefire.plugin.version}</version>
                </plugin>
```

Replace with:
```xml
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven.surefire.plugin.version}</version>
                    <configuration>
                        <forkedProcessTimeoutInSeconds>300</forkedProcessTimeoutInSeconds>
                        <argLine>@{argLine}</argLine>
                    </configuration>
                </plugin>
```

- [ ] **Step 2: Verify build still passes**

```bash
mvn test -T 4 -q
```

Expected: `BUILD SUCCESS` — all tests still pass.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "ci: add Surefire timeout and JaCoCo argLine to parent pom"
```

---

### Task 3: Add enforcer rules — requireReleaseDeps and banDynamicVersions

**Files:**
- Modify: `pom.xml:234-244` (quality-guards enforcer execution in pluginManagement)

`requireReleaseDeps` prevents SNAPSHOT dependencies from appearing in a release build. `banDynamicVersions` prevents version ranges like `[1.0,2.0)`. Both are already in the `quality-guards` execution — we just add them to its `<rules>` block.

- [ ] **Step 1: Add rules to the quality-guards enforcer execution in `pom.xml`**

Find the `quality-guards` execution (around line 234):
```xml
                        <execution>
                            <id>quality-guards</id>
                            <goals><goal>enforce</goal></goals>
                            <configuration>
                                <rules>
                                    <dependencyConvergence/>
                                    <requirePluginVersions/>
                                </rules>
                            </configuration>
                        </execution>
```

Replace with:
```xml
                        <execution>
                            <id>quality-guards</id>
                            <goals><goal>enforce</goal></goals>
                            <configuration>
                                <rules>
                                    <dependencyConvergence/>
                                    <requirePluginVersions/>
                                    <requireReleaseDeps/>
                                    <banDynamicVersions/>
                                </rules>
                            </configuration>
                        </execution>
```

- [ ] **Step 2: Verify build passes with new rules**

```bash
mvn validate -q
```

Expected: `BUILD SUCCESS` — no SNAPSHOT deps and no dynamic versions in this project.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "ci: add requireReleaseDeps and banDynamicVersions enforcer rules"
```

---

### Task 4: Consolidate Kotlin jvmTarget into parent pluginManagement

**Files:**
- Modify: `pom.xml:198-201` (kotlin-maven-plugin in pluginManagement)
- Modify: `outerstellar-i18n/pom.xml:39-41`
- Modify: `outerstellar-theme/pom.xml:47-49`
- Modify: `outerstellar-plugin/pom.xml:48-50`

Three child modules each declare `<configuration><jvmTarget>21</jvmTarget></configuration>` inside their `kotlin-maven-plugin`. Moving it to the parent's `pluginManagement` removes the duplication. Children keep only their `<executions>`; Maven merges in the parent's `<configuration>`.

The maven-plugin module (`outerstellar-i18n-validator-maven-plugin`) is Java-only — no Kotlin plugin, no change needed.

- [ ] **Step 1: Add jvmTarget to the kotlin-maven-plugin in root `pom.xml` pluginManagement**

Find (around line 198):
```xml
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>
                    <version>${kotlin.version}</version>
                </plugin>
```

Replace with:
```xml
                <plugin>
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-plugin</artifactId>
                    <version>${kotlin.version}</version>
                    <configuration>
                        <jvmTarget>21</jvmTarget>
                    </configuration>
                </plugin>
```

- [ ] **Step 2: Remove the `<configuration>` block from `outerstellar-i18n/pom.xml`**

Find:
```xml
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <configuration>
                    <jvmTarget>21</jvmTarget>
                </configuration>
                <executions>
```

Replace with:
```xml
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <executions>
```

- [ ] **Step 3: Same removal in `outerstellar-theme/pom.xml`**

Same find/replace as Step 2 (identical block).

- [ ] **Step 4: Same removal in `outerstellar-plugin/pom.xml`**

Same find/replace as Step 2 (identical block).

- [ ] **Step 5: Verify Kotlin compiles correctly with jvmTarget from parent**

```bash
mvn compile -T 4 -q
```

Expected: `BUILD SUCCESS`. If jvmTarget is missing, you'd see a Kotlin compiler warning or error about unsupported JVM target.

- [ ] **Step 6: Commit**

```bash
git add pom.xml outerstellar-i18n/pom.xml outerstellar-theme/pom.xml outerstellar-plugin/pom.xml
git commit -m "build: consolidate Kotlin jvmTarget=21 into parent pluginManagement"
```

---

### Task 5: Fix PluginManager.initialized — replace @Volatile Boolean with AtomicBoolean

**Files:**
- Modify: `outerstellar-plugin/src/main/kotlin/io/github/rygel/outerstellar/plugin/PluginManager.kt`

`@Volatile Boolean` provides visibility (writes are immediately visible) but not atomicity. `AtomicBoolean` makes the intent explicit and enables future use of `compareAndSet` if needed. All reads/writes must be updated: `initialized = true` → `initialized.set(true)`, reads → `initialized.get()`.

- [ ] **Step 1: Write a failing test that demonstrates the gap**

Add to `outerstellar-plugin/src/test/kotlin/io/github/rygel/outerstellar/plugin/PluginManagerTest.kt` (inside the `PluginManagerTest` class, before the closing `}`):

```kotlin
    @Test
    fun `isInitialized is visible across threads after discoverAndInitialize`() {
        val manager = PluginManager.create(SharedPlugin::class.java)
        val latch = java.util.concurrent.CountDownLatch(1)
        var seenFromOtherThread = false

        manager.discoverAndInitialize()

        val thread = Thread {
            latch.await()
            seenFromOtherThread = manager.isInitialized()
        }
        thread.start()
        latch.countDown()
        thread.join(1000)

        assertTrue(seenFromOtherThread, "isInitialized() must be visible across threads")
    }
```

- [ ] **Step 2: Run the test to confirm it currently passes (visibility works with @Volatile too)**

```bash
mvn test -pl outerstellar-plugin -Dtest=PluginManagerTest#isInitialized* -q
```

Expected: PASS (the test documents the required behavior, not a regression).

- [ ] **Step 3: Replace the field declaration in `PluginManager.kt`**

Add import at the top of `PluginManager.kt` (after the existing imports):
```kotlin
import java.util.concurrent.atomic.AtomicBoolean
```

Replace:
```kotlin
    @Volatile
    private var initialized = false
```

With:
```kotlin
    private val initialized = AtomicBoolean(false)
```

- [ ] **Step 4: Update all read and write sites in `PluginManager.kt`**

In `discoverAndInitialize()`, replace:
```kotlin
        initialized = true
```
With:
```kotlin
        initialized.set(true)
```

In `reload()`, replace:
```kotlin
        if (initialized) {
```
With:
```kotlin
        if (initialized.get()) {
```

In `isInitialized()`, replace:
```kotlin
    fun isInitialized(): Boolean = initialized
```
With:
```kotlin
    fun isInitialized(): Boolean = initialized.get()
```

In `withPlugin()`, replace:
```kotlin
        check(initialized) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
```
With:
```kotlin
        check(initialized.get()) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
```

In `withEachPlugin()`, replace:
```kotlin
        check(initialized) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
```
With:
```kotlin
        check(initialized.get()) { "PluginManager has not been initialized. Call discoverAndInitialize() first." }
```

- [ ] **Step 5: Run all plugin tests**

```bash
mvn test -pl outerstellar-plugin -q
```

Expected: `Tests run: 29, Failures: 0, Errors: 0` (28 existing + 1 new).

- [ ] **Step 6: Commit**

```bash
git add outerstellar-plugin/src/main/kotlin/io/github/rygel/outerstellar/plugin/PluginManager.kt
git add outerstellar-plugin/src/test/kotlin/io/github/rygel/outerstellar/plugin/PluginManagerTest.kt
git commit -m "fix: replace @Volatile Boolean with AtomicBoolean in PluginManager"
```

---

### Task 6: Fix I18nService.setLocale() — make locale change atomic with @Synchronized

**Files:**
- Modify: `outerstellar-i18n/src/main/kotlin/io/github/rygel/outerstellar/i18n/I18nService.kt:50-54`
- Modify: `outerstellar-i18n/src/test/kotlin/io/github/rygel/outerstellar/i18n/I18nServiceTest.kt`

`setLocale()` performs three steps: write new locale, clear cache, notify listeners. Without synchronization, a concurrent `setLocale()` call could interleave these steps — one thread writes locale A while another has already cleared the cache, causing the first thread's listeners to be notified with an inconsistent state. `@Synchronized` makes the entire method body a critical section.

`currentLocale` is already `@Volatile` (visibility guaranteed) and the cache and listeners are already concurrent collections, so only `setLocale()` itself needs the lock.

- [ ] **Step 1: Write a failing concurrent test**

Add to `I18nServiceTest.kt` (inside the `I18nServiceTest` class, before the closing `}`):

```kotlin
    @Test
    fun `setLocale is safe when called concurrently from multiple threads`() {
        val service = I18nService.create("messages")
        val locales = listOf(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN)
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
        val latch = java.util.concurrent.CountDownLatch(1)
        val threads = (1..8).map { i ->
            Thread {
                latch.await()
                repeat(50) {
                    try {
                        service.setLocale(locales[i % locales.size])
                        service.translate("any.key")
                    } catch (e: Throwable) {
                        errors.add(e)
                    }
                }
            }
        }
        threads.forEach { it.start() }
        latch.countDown()
        threads.forEach { it.join(5000) }

        assertTrue(errors.isEmpty(), "Concurrent setLocale must not throw: ${errors.firstOrNull()}")
    }
```

- [ ] **Step 2: Run the test to see if it currently passes or exposes a race**

```bash
mvn test -pl outerstellar-i18n -Dtest=I18nServiceTest#setLocale* -q
```

Note the result — whether it passes or fails depends on timing. The fix prevents any possibility of failure.

- [ ] **Step 3: Add `@Synchronized` to `setLocale()` in `I18nService.kt`**

Replace:
```kotlin
    fun setLocale(locale: Locale) {
        currentLocale = locale
        bundleCache.clear()
        listeners.forEach { it.updateTexts() }
    }
```

With:
```kotlin
    @Synchronized
    fun setLocale(locale: Locale) {
        currentLocale = locale
        bundleCache.clear()
        listeners.forEach { it.updateTexts() }
    }
```

No import needed — `@Synchronized` is a Kotlin stdlib annotation.

- [ ] **Step 4: Run all i18n tests**

```bash
mvn test -pl outerstellar-i18n -q
```

Expected: `Tests run: 21, Failures: 0, Errors: 0` (20 existing + 1 new).

- [ ] **Step 5: Commit**

```bash
git add outerstellar-i18n/src/main/kotlin/io/github/rygel/outerstellar/i18n/I18nService.kt
git add outerstellar-i18n/src/test/kotlin/io/github/rygel/outerstellar/i18n/I18nServiceTest.kt
git commit -m "fix: make I18nService.setLocale atomic with @Synchronized"
```

---

### Task 7: Fix publish-central.yml — run tests before deploying to Maven Central

**Files:**
- Modify: `.github/workflows/publish-central.yml:39-44`

Currently the workflow runs `mvn deploy -Prelease -DskipTests`, publishing to Maven Central without ever running tests. A broken release can be published. The fix is a single combined `mvn clean deploy -Prelease` — the full Maven lifecycle runs (compile → test → verify → package → sign → deploy) in one pass.

- [ ] **Step 1: Replace the publish step in `.github/workflows/publish-central.yml`**

Replace:
```yaml
      - name: Publish to Maven Central
        run: mvn deploy -Prelease -DskipTests
        env:
          CENTRAL_USERNAME: ${{ secrets.CENTRAL_USERNAME }}
          CENTRAL_PASSWORD: ${{ secrets.CENTRAL_PASSWORD }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

With:
```yaml
      - name: Publish to Maven Central
        run: mvn clean deploy -Prelease
        env:
          CENTRAL_USERNAME: ${{ secrets.CENTRAL_USERNAME }}
          CENTRAL_PASSWORD: ${{ secrets.CENTRAL_PASSWORD }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/publish-central.yml
git commit -m "ci: run full test suite before deploying to Maven Central"
```

---

### Task 8: Fix README version examples (1.0.10 → 1.0.14)

**Files:**
- Modify: `README.md:41,48` (version numbers in dependency examples)

The README shows `1.0.10` in the installation examples. Current version is `1.0.14`.

- [ ] **Step 1: Update both version references in `README.md`**

Replace both occurrences of `<version>1.0.10</version>` with `<version>1.0.14</version>`.

There are exactly two (lines 41 and 48). Use replace-all since both should become 1.0.14.

- [ ] **Step 2: Verify the README is correct**

Read the updated README to confirm both occurrences changed.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: update README version examples to 1.0.14"
```

---

### Task 9: Create CONTRIBUTING.md

**Files:**
- Create: `CONTRIBUTING.md`

- [ ] **Step 1: Create `CONTRIBUTING.md` in the repository root**

```markdown
# Contributing

## Prerequisites

- JDK 21+
- Maven 3.9+
- Git

## Building locally

```bash
mvn clean verify -T 4
```

This compiles, runs all tests, and runs all quality checks (Detekt, JaCoCo coverage, enforcer rules).

## Branch naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/<description>` | `feat/add-plural-support` |
| Bug fix | `fix/<description>` | `fix/bundle-cache-leak` |
| Refactor | `refactor/<description>` | `refactor/extract-injector` |
| CI / build | `chore/<description>` | `chore/update-actions` |

## Pull request checklist

- [ ] Branch targets `develop`, not `main`
- [ ] `mvn clean verify -T 4` passes locally before pushing
- [ ] New public API has a test
- [ ] Line coverage stays at or above 75% per module (`mvn verify` fails otherwise)
- [ ] Detekt reports zero issues (`mvn verify` fails otherwise)
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `type: description`

## Kotlin version policy

The framework targets Kotlin 2.0.x intentionally. All semver levels are suppressed in Dependabot. Kotlin upgrades are manual and deliberate. Do not bump `kotlin.version` in `pom.xml` without a documented reason.

## Release process

Releases are managed by maintainers:

1. Bump `<revision>` in `pom.xml` on `develop`
2. Open a PR from `develop` → `main`
3. Merge triggers automatic GitHub Packages publish
4. Maven Central publish is a separate manual workflow dispatch
```

- [ ] **Step 2: Commit**

```bash
git add CONTRIBUTING.md
git commit -m "docs: add CONTRIBUTING.md with build, branch, and PR guidelines"
```

---

### Task 10: Run full verify and open PR

**Files:** none

- [ ] **Step 1: Run the full build to confirm everything passes**

```bash
mvn clean verify -T 4 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` — all 7 modules succeed.

- [ ] **Step 2: Check for merge conflicts with develop**

```bash
git fetch origin
git merge origin/develop --no-commit --no-ff 2>&1 | head -5
git merge --abort
```

Expected: `Already up to date` or only fast-forward (no CONFLICT lines). If there are conflicts, resolve them before opening the PR.

- [ ] **Step 3: Push and open PR**

```bash
git push -u origin chore/build-quality-improvements
gh pr create \
  --base develop \
  --title "chore: build quality, code safety, and doc improvements" \
  --body "$(cat <<'EOF'
## Summary

- **Surefire timeout** — 300s per forked JVM; `@{argLine}` for correct JaCoCo agent injection
- **Enforcer rules** — `requireReleaseDeps` and `banDynamicVersions` prevent SNAPSHOT deps and version ranges in releases
- **jvmTarget consolidation** — removed from 3 child module pom files; lives in parent pluginManagement once
- **PluginManager.initialized** — replaced `@Volatile Boolean` with `AtomicBoolean` for explicit memory semantics
- **I18nService.setLocale()** — `@Synchronized` makes locale change + cache clear + listener notification atomic
- **publish-central.yml** — `mvn clean deploy -Prelease` runs tests before deploying (was `-DskipTests`)
- **README** — version examples updated from `1.0.10` to `1.0.14`
- **CONTRIBUTING.md** — branch naming, PR checklist, Kotlin version policy, release process

## Test plan

- [ ] `mvn clean verify -T 4` passes locally (all 7 modules)
- [ ] New concurrent tests pass for `PluginManager` and `I18nService`
- [ ] CI passes on this PR

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```
