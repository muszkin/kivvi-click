# Verifier report — wave wave-0, dimension architecture (round 3)

Wave SHA: `887af4543c5a51255735f1ddfc0d8860d11551c0`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-arch` (HEAD confirmed == wave SHA)

## Dimension status: PASS

## Journey verdict

| Journey | Verdict |
| --- | --- |
| login | parity |

## Rule → tooling table (wave-0 rows of architecture/rules-translated.md)

| Rule | Tooling located | Result |
| --- | --- | --- |
| Static typing gate (PHPStan 5 → `-Werror`-equivalent) | `backend/pom.xml` maven-compiler-plugin: `-Xlint:all -Werror`; `frontend/package.json` `typecheck` = `vue-tsc --noEmit` | PASS — `mvn verify` compiles clean; `npm run typecheck` exit 0 |
| Formatting `@Symfony` | `backend/pom.xml` spotless-maven-plugin (google-java-format 1.36.1), execution `check-formatting` bound to `verify` phase; `frontend/package.json` `format:check` = `prettier --check .` | PASS — confirmed bound and executing (see below); `npm run format:check` exit 0 |
| Test policy fail-on-warning | Backend: `FailOnWarnLogExtension` (`backend/src/test/java/click/kivvi/testsupport/`), auto-registered via `backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` + `junit.jupiter.extensions.autodetection.enabled=true` in `junit-platform.properties`. Frontend: `frontend/test/setup.ts` wired via `vite.config.ts` `test.setupFiles`, plus `dangerouslyIgnoreUnhandledErrors: false` | PASS — registration files present and correctly wired; extension exercised by 66 unit + 11 IT tests, all green |
| Layering convention (controller → application → domain → infrastructure) | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` (ArchUnit 1.5.0, `@AnalyzeClasses`-equivalent `ClassFileImporter`): domain-not-depends-on-web, infrastructure-only-from-application, no top-level package cycle | PASS — `mvn test -Dtest=ArchitectureTest` → 3/3 green |

Spotless binding confirmed by full (non-`-q`) verify log line 251-252: `[INFO] --- spotless:3.10.2:check (check-formatting) @ kivvi-click ---` / `Spotless.Java is keeping 42 files clean`, followed by `BUILD SUCCESS`.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 0 |
| `./mvnw -q verify` | `backend/` | 0 (66 unit tests, 11 IT tests — Testcontainers Postgres 18 — all green; spotless:check clean) |
| `./mvnw verify` (non-quiet rerun, to confirm spotless binding in log) | `backend/` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` (eslint .) | `frontend/` | 0 |
| `npm run typecheck` (vue-tsc --noEmit) | `frontend/` | 0 |
| `npm run format:check` (prettier --check .) | `frontend/` | 0 |
| `git diff --stat 8d3fc320354604b641b44a3043a070f279e6d491 -- src/ templates/ assets/ translations/ migrations/ tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | repo root | 0, empty output |
| `git diff --stat 8d3fc320354604b641b44a3043a070f279e6d491 -- tests/*.php tests/**/*.php` | repo root | 0, empty output |
| `cmp` × 5 on `frontend/src/styles/*.css` vs `assets/styles/*.css` | repo root | 0, all identical |
| `./mvnw dependency:tree` (grep junit/testcontainers) | `backend/` | 0 |

## Global constraints

- **Empty diff vs base `8d3fc320354604b641b44a3043a070f279e6d491`** under all guarded legacy paths (`src/`, `templates/`, `assets/`, `translations/`, `migrations/`, `tests/**/*.php`, `tests/e2e/specs/`, `composer.*`, `compose.yaml`, `compose.override.yaml`, `compose.prod.yaml`, `Dockerfile`, `frankenphp/`, `config/`, `public/`): confirmed empty. Full repo diff vs base is 123 files / +14009/-0 (insertions only — new `backend/`, `frontend/`, `tools/migration-verify/` trees), consistent with a read-only legacy stack.
- **Pinned versions match the plan's Technology decisions table** (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`): Spring Boot 4.1.1, Java 25 (Temurin), ArchUnit 1.5.0, JUnit 6.0.3 (resolved via BOM, confirmed by `dependency:tree`), Testcontainers 2.0.5, Maven wrapper 3.9.11, Vue 3.5.42, vue-router 5.3.1, Pinia 4.0.3, Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, vue-i18n 11.4.10, TypeScript 6.0.3, Prettier 3.8.4, Node 26.8.1 (host) — all match.
- **Flyway has only `V1__baseline.sql`**: confirmed, single file in `backend/src/main/resources/db/migration/`.
- **All five `frontend/src/styles/*.css` byte-identical to `assets/styles/`**: `cmp` confirmed for `01-tokens.css`, `02-base.css`, `03-components.css`, `04-patterns.css`, `app.css` (the sixth legacy file, `storybook.css`, is dev-only/out-of-scope per rules-translated.md's last row and is not expected in `frontend/src/styles/`).

No wave-0 rule violation, no missing tooling, no global constraint violation. Round-1/round-2 failures (unit/integration coverage) are outside this dimension's scope; the architecture-relevant repairs referenced in code comments (Repair-1 session wiring, Repair-2 Vitest unhandled-errors, Repair-3 spotless verify binding) are present and verified working in this checkout.

## Evidence paths

- `waves/wave-0/architecture/mvn-test-ArchitectureTest.log`
- `waves/wave-0/architecture/mvn-verify.log`
- `waves/wave-0/architecture/npm-ci.log`
- `waves/wave-0/architecture/npm-lint.log`
- `waves/wave-0/architecture/npm-typecheck.log`
- `waves/wave-0/architecture/npm-format-check.log`
- `waves/wave-0/architecture/global-constraints.log`
