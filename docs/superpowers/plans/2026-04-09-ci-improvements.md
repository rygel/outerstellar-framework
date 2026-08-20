# CI/CD & Build Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix five CI/CD and build issues identified during repository analysis: a missing Maven profile, CodeQL coverage gap on PRs, missing JaCoCo enforcement, double compilation on release, and undocumented Kotlin version lock.

**Architecture:** All changes are isolated to CI/CD workflow files and the root `pom.xml`. No production source code is touched. Tasks are independent and can be implemented in any order.

**Tech Stack:** GitHub Actions, Maven, JaCoCo, CodeQL, YAML

---

### Task 1: Add `fast` Maven profile to `pom.xml`

**Files:**
- Modify: `pom.xml:376-429` (profiles section)

The `codeql.yml` workflow runs `mvn compile -Pfast -q` but no `fast` profile exists in `pom.xml`. This means Maven silently ignores the unknown profile and runs with full configuration. The profile should skip tests and detekt so CodeQL can compile the code quickly without quality-gate overhead.

- [ ] **Step 1: Add the `fast` profile inside the `<profiles>` block in `pom.xml`**

Open `pom.xml`. After line 429 (the closing `</profile>` of the `release` profile, before `</profiles>`), add:

```xml
        <profile>
            <id>fast</id>
            <properties>
                <maven.test.skip>true</maven.test.skip>
                <detekt.skip>true</detekt.skip>
            </properties>
        </profile>
```

The final `<profiles>` block should look like:

```xml
    <profiles>
        <profile>
            <id>release</id>
            <!-- ... existing release profile content ... -->
        </profile>
        <profile>
            <id>fast</id>
            <properties>
                <maven.test.skip>true</maven.test.skip>
                <detekt.skip>true</detekt.skip>
            </properties>
        </profile>
    </profiles>
```

- [ ] **Step 2: Verify the profile is recognized**

```bash
mvn help:all-profiles -q
```

Expected output includes a line like:
```
Profile Id: fast (Active: false , Source: pom)
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "ci: add fast Maven profile for CodeQL compilation"
```

---

### Task 2: Add push and PR triggers to CodeQL workflow

**Files:**
- Modify: `.github/workflows/codeql.yml:3-5` (on trigger block)

Currently CodeQL only runs on a weekly schedule. PRs get no CodeQL feedback before merge. Adding `push` and `pull_request` triggers closes this gap. Keep the weekly schedule for the periodic sweep.

- [ ] **Step 1: Replace the `on:` block in `.github/workflows/codeql.yml`**

Replace:
```yaml
on:
  schedule:
    - cron: '0 6 * * 1'
```

With:
```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
  schedule:
    - cron: '0 6 * * 1'
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/codeql.yml
git commit -m "ci: run CodeQL on push and pull requests, not just weekly"
```

---

### Task 3: Add JaCoCo coverage enforcement to `pom.xml`

**Files:**
- Modify: `pom.xml:313-332` (jacoco-maven-plugin executions inside `pluginManagement`)

JaCoCo already generates reports but enforces no minimum. Adding a `check` execution makes coverage regressions fail the build. 75% line coverage is the target — reasonable for a framework library.

Note: the `check` goal must be added to the `pluginManagement` section (not just the `<plugins>` section) so child modules inherit it. The `<plugins>` section at the bottom already references `jacoco-maven-plugin` without extra config, which is enough to activate the inherited `pluginManagement` executions.

- [ ] **Step 1: Add a `check` execution to the JaCoCo plugin in `pluginManagement` in `pom.xml`**

The existing JaCoCo block (lines 313–332) looks like:

```xml
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>${jacoco.version}</version>
                    <executions>
                        <execution>
                            <id>prepare-agent</id>
                            <goals>
                                <goal>prepare-agent</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>report</id>
                            <phase>test</phase>
                            <goals>
                                <goal>report</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
```

Replace it with:

```xml
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>${jacoco.version}</version>
                    <executions>
                        <execution>
                            <id>prepare-agent</id>
                            <goals>
                                <goal>prepare-agent</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>report</id>
                            <phase>test</phase>
                            <goals>
                                <goal>report</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>jacoco-check</id>
                            <goals>
                                <goal>check</goal>
                            </goals>
                            <configuration>
                                <rules>
                                    <rule>
                                        <element>BUNDLE</element>
                                        <limits>
                                            <limit>
                                                <counter>LINE</counter>
                                                <value>COVEREDRATIO</value>
                                                <minimum>0.75</minimum>
                                            </limit>
                                        </limits>
                                    </rule>
                                </rules>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
```

- [ ] **Step 2: Run tests locally to confirm the check passes**

```bash
mvn test -T 4
```

Expected: BUILD SUCCESS. If coverage is below 75% for any module, you'll see a JaCoCo violation. Fix by adjusting the threshold or improving tests — do NOT suppress the check.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "ci: enforce 75% line coverage minimum via JaCoCo"
```

---

### Task 4: Fix double compilation in `publish.yml`

**Files:**
- Modify: `.github/workflows/publish.yml:68-76` (Build and test + Publish steps)

The current flow runs `mvn clean verify` then `mvn deploy -DskipTests`. These are two separate Maven invocations, so Maven compiles all sources twice. Combining them into a single `mvn clean deploy` runs the full lifecycle (compile → test → verify → package → deploy) once.

The `setup-java` step already configures the GitHub Packages server credentials, so `deploy` will work with a single invocation.

- [ ] **Step 1: Replace the two Maven steps in `.github/workflows/publish.yml` with one**

Replace these two steps:
```yaml
      - name: Build and test
        if: steps.check.outputs.exists == 'false'
        run: mvn clean verify

      - name: Publish to GitHub Packages
        if: steps.check.outputs.exists == 'false'
        run: mvn deploy -DskipTests
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

With a single step:
```yaml
      - name: Build, test, and publish to GitHub Packages
        if: steps.check.outputs.exists == 'false'
        run: mvn clean deploy
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/publish.yml
git commit -m "ci: eliminate double compilation in release workflow"
```

---

### Task 5: Document Kotlin version lock in `dependabot.yml`

**Files:**
- Modify: `.github/dependabot.yml:9-11` (ignore block)

The Kotlin dependency is fully ignored by Dependabot with no explanation. Future maintainers will wonder why. Add a comment.

- [ ] **Step 1: Add a comment above the Kotlin ignore entry in `.github/dependabot.yml`**

Replace:
```yaml
    ignore:
      - dependency-name: "org.jetbrains.kotlin:*"
        update-types: ["version-update:semver-major", "version-update:semver-minor", "version-update:semver-patch"]
```

With:
```yaml
    ignore:
      # Kotlin is pinned to 2.0.x intentionally: the framework targets Kotlin 2.0
      # to ensure forward compatibility. Update manually when the policy changes.
      - dependency-name: "org.jetbrains.kotlin:*"
        update-types: ["version-update:semver-major", "version-update:semver-minor", "version-update:semver-patch"]
```

- [ ] **Step 2: Commit**

```bash
git add .github/dependabot.yml
git commit -m "chore: document intentional Kotlin version lock in Dependabot config"
```

---

### Task 6: Open PR

- [ ] **Step 1: Push the branch and open a PR targeting `develop`**

```bash
git push -u origin chore/semgrep-replace-codeql-on-pr
gh pr create \
  --base develop \
  --title "ci: CodeQL on PRs, fast profile, JaCoCo enforcement, release fix" \
  --body "$(cat <<'EOF'
## Summary
- Add `fast` Maven profile so CodeQL build step is explicit and correct
- Run CodeQL on every push/PR (was weekly-schedule only)
- Enforce 75% JaCoCo line coverage minimum — build fails on regression
- Combine release publish into single `mvn clean deploy` (eliminates double compile)
- Document intentional Kotlin 2.0 version lock in Dependabot config

## Test plan
- [ ] `mvn test -T 4` passes locally with coverage check
- [ ] `mvn help:all-profiles` shows `fast` profile listed
- [ ] CodeQL action triggers on this PR
EOF
)"
```
