# Contributing

## Prerequisites

- Java 21
- Maven 3.9+
- Kotlin 2.0.x (see [Kotlin version policy](#kotlin-version-policy) before updating)

## Building

Full build with tests:

```bash
mvn verify -T 4
```

Quick compile and test cycle (skips detekt):

```bash
mvn verify -T 4 -Pfast
```

Always run the full build locally before pushing. Do not rely on CI to catch failures.

## Branch naming

Use a descriptive prefix:

| Prefix | When to use |
|---|---|
| `feat/` | New feature |
| `fix/` | Bug fix |
| `refactor/` | Refactoring without behavior change |
| `chore/` | Maintenance, dependency updates |
| `ci/` | CI/CD changes |
| `docs/` | Documentation only |

Never push directly to `develop` or `main`.

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add support for X
fix: correct null handling in Y
ci: pin action SHA for security
chore: update dependency Z
docs: document configuration options
build: adjust compiler flags
test: add coverage for edge case
refactor: extract shared utility
```

Keep the first line under 72 characters. Add a body if context is needed.

## PR checklist

Before opening a pull request:

- [ ] `mvn verify -T 4` passes locally with no failures
- [ ] No detekt suppressions or threshold increases added to silence linter warnings — fix the code instead
- [ ] Branch is based on `develop`, not `main`
- [ ] PR targets `develop`

Releases to `main` are handled by the maintainer. Do not target `main` in your PR.

## Kotlin version policy

Kotlin is intentionally pinned to **2.0.x** for API stability. Dependabot suppressions are in place to prevent automatic bumps. Do not update the Kotlin version unless the maintainer explicitly changes this policy.

## Release process

Releases are cut by the maintainer. Contributors do not need to bump versions, update changelogs, or create release PRs.
