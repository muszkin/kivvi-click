---
project: kivvi-click
checked_at: 2026-06-25T21:34:41+02:00
health_status: healthy
context_type: brownfield
language_family: php
stack_assessment_available: true
checks_run:
  - lockfile
  - dependency_audit
  - outdated_deps
  - test_runner
  - ci_cd
  - configuration
audit_findings:
  critical: 0
  high: 0
  moderate: 0
  low: 0
test_runner_detected: true
ci_provider: github-actions
recommended_fixes: 1
---

## Dependency Health

### Lockfile

Status: present (`composer.lock`, `yarn.lock`)
Package manager: Composer for PHP; Yarn 4 for JS assets.

### Security Audit

Tool: `composer audit` and `yarn npm audit --all --recursive --json`
Summary: 0 CRITICAL, 0 HIGH, 0 MODERATE, 0 LOW
Direct vs transitive: no findings.

### Outdated Dependencies

Packages with major version gaps: 0

Composer reported only semver-safe direct updates. Yarn audit returned no findings.

## Test Suite

Test runner: PHPUnit
Tests found: 4 tests
Test execution: passing via `docker compose exec php composer test`

Configuration: `phpunit.dist.xml`
Framework: PHPUnit 13.1.10 from `composer.json`

Current canonical command:

```bash
docker compose exec php composer test
```

That script creates the test database if missing, builds TypeScript assets for AssetMapper, then runs PHPUnit. Latest verification passed with 4 tests and 10 assertions.

## CI/CD

Provider: GitHub Actions
Configuration: `.github/workflows/docker-build.yml`

| Stage | Status | Notes |
|---|---|---|
| Lint | - | intentionally local-only for now |
| Test | - | intentionally local-only for now |
| Build | yes | production Docker image builds on push to `main` |
| Type check | - | intentionally local-only for now |
| Security | - | intentionally local-only for now |

The current workflow is deliberately narrow: it validates that the production image can be built from `Dockerfile` target `frankenphp_prod` after each push to `main`. Local validation remains the source of truth for tests, PHPStan, formatting, and audits until the CI scope is expanded.

## Configuration

### High severity

None.

### Medium severity

None.

### Low severity

- **Prettier configuration file** - Prettier is installed and `yarn format` exists, but no `.prettierrc*` or `prettier.config.*` was detected. Fix: add one if frontend formatting choices diverge from Prettier defaults.

## Stack Assessment Cross-Reference

Stack assessment: `context/foundation/stack-assessment.md`
Agent readiness (from stack-assess): ready

| Quality Gate Gap | Health-Check Finding | Status |
|---|---|---|
| PHP type safety is partial | PHPStan and PHPStan Symfony are configured and passing locally through `docker compose exec php composer phpstan`. | Mitigated |
| TypeScript assets need explicit type-check gate | `tsconfig.json`, `typescript`, and `yarn typecheck` are configured and passing. | Mitigated |
| PHPUnit discoverability | `phpunit.dist.xml` exists, and `composer test` prepares dependencies before running PHPUnit. | Mitigated |
| CI/CD not detected | GitHub Actions now builds the production Docker image on push to `main`. | Mitigated |
| FrankenPHP needs explicit steering | `AGENTS.md`, `CLAUDE.md`, `Dockerfile`, and `compose.yaml` all document Docker-first/FrankenPHP assumptions. | Mitigated |

## Recommended Fixes

### Fix before agent work (Category A)

None.

### Addressed in upcoming lessons (Category B)

#### Expand CI validation

**Lesson**: Infrastructure and deployment setup.
**What you'll do there**: Add formatting, PHPStan, PHPUnit, TypeScript type checking, and security audits to pull request CI when the project is ready for broader automated gates.

## Summary

Health status: healthy

The project has a solid foundation: lockfiles are present, instruction files are strong, Composer/Yarn audit tooling runs cleanly, PHPUnit is installed and passing, PHPStan is configured and passing, TypeScript has an explicit strict configuration, a sanitized `.env.example` exists, and GitHub Actions builds the production Docker image on push to `main`.
