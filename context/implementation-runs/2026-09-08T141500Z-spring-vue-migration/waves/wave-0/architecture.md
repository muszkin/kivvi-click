# Verifier verdict — wave-0, dimension architecture

- Wave SHA: `3b17c07e23354e493edbab572e4090f69d2c0c71` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-arch`, `git rev-parse HEAD` confirmed equal; checkout left unmodified).
- Journeys: login.
- **Dimension status: FAIL** (bound to `3b17c07e23354e493edbab572e4090f69d2c0c71`).
- **Journey verdict — login: regression.** Reason: one wave-0 rule (`Test policy fail-on-warning`) has no enforcing tooling in the codebase and no deviation record — FAIL condition per `migration-waves.md` architecture row and the plan's zero-change rule (every difference is parity / accepted-deviation / regression, no fourth verdict).

## Rule → tooling table (rules-translated.md rows with Wave = wave-0)

| Rule | Enforcing tooling found | Command | Result |
| --- | --- | --- | --- |
| Static typing gate (PHPStan 5) → `-Werror`-equivalent + `vue-tsc --noEmit` strict | `backend/pom.xml:146-153` `maven-compiler-plugin` `-Xlint:all -Werror`; `frontend/tsconfig.json` `strict:true` + `noUncheckedIndexedAccess`/`noImplicitOverride`/`noImplicitReturns`, `frontend/package.json` `typecheck` script | `npm run typecheck` | PASS (exit 0) |
| Formatting `@Symfony` → Spotless google-java-format / Prettier | `backend/pom.xml:155-168` `spotless-maven-plugin` + `google-java-format 1.36.1`; `frontend/package.json` `format` script, `prettier 3.8.4` devDependency | `./mvnw -q spotless:check` | PASS (exit 0) |
| Test policy fail-on-warning → JUnit WARN-as-failure via Logback appender assertion + Vitest fails on unhandled errors | **none found.** No `ch.qos.logback`/`Appender` reference, no `@ExtendWith`, no `junit-platform.properties`, no `junit.jupiter.execution.parallel` setting anywhere under `backend/src` or `backend/pom.xml`. No custom Vitest `setupFiles`/`onUnhandledError` config beyond framework defaults. No `DEV-n` entry in the plan's Accepted deviations table covers this rule. | — | **FAIL — rule has no equivalent and no deviation record** |
| Layering convention (controller→application→domain→infrastructure, no cycles) → ArchUnit | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`: 3 ArchUnit rules (`domain` not→`web`, `infrastructure` only from `infrastructure`/`application`, no package cycle) | `./mvnw -q test -Dtest=ArchitectureTest` | PASS (3 tests, 0 failures, 0 errors) |

(Rows for Mercure-topic, row-markup, and formatting-server-side rules are wave-2/wave-1/N-A and out of scope here; storybook row is "Out of scope for parity".)

## Global structural constraints (plan "Global implementation constraints" + packet)

| Constraint | Check | Result |
| --- | --- | --- |
| No touch to old-stack/protected paths vs approved base `8d3fc32` | `git diff --stat 8d3fc320354604b641b44a3043a070f279e6d491..HEAD -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | empty output — PASS |
| Pinned versions match plan "Technology decisions" table | manual diff of `backend/pom.xml` (Spring Boot 4.1.1, Java 25, ArchUnit 1.5.0) and `frontend/package.json` (vue 3.5.42, vue-router 5.3.1, pinia 4.0.3, vite 8.2.2, @vitejs/plugin-vue 6.0.8, vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, vue-i18n 11.4.10, typescript 6.0.3, prettier 3.8.4) against plan lines 195-208 | all match — PASS |
| Flyway contains only `V1__baseline.sql` | `find . -path "*/db/migration/*" -not -path "*/target/*"` in `backend/` | one file, `V1__baseline.sql` — PASS |

## Commands run (cwd, exit code)

1. `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` then `./mvnw -q test -Dtest=ArchitectureTest` — cwd `backend/` — exit 0 (3 tests, 0 failures/errors)
2. `./mvnw -q spotless:check` — cwd `backend/` — exit 0
3. `npm ci` — cwd `frontend/` — exit 0
4. `npm run lint` — cwd `frontend/` — exit 0
5. `npm run typecheck` — cwd `frontend/` — exit 0
6. `git diff --stat 8d3fc320354604b641b44a3043a070f279e6d491..HEAD -- <protected paths>` — cwd repo root of checkout — exit 0, empty output

## Evidence paths

- `waves/wave-0/architecture/backend-archunit.log`, `waves/wave-0/architecture/backend-archunit-surefire.txt`
- `waves/wave-0/architecture/backend-spotless.log`
- `waves/wave-0/architecture/frontend-npm-ci.log`, `frontend-lint.log`, `frontend-typecheck.log`
- `waves/wave-0/architecture/protected-paths-diff.log`
- `waves/wave-0/architecture/structural-checks.log` (Flyway check, version-pin comparison, and the full negative-evidence search for the missing "Test policy fail-on-warning" tooling)

## Note for repair

The `ArchitectureTest` class itself only implements the layering rule (its own Javadoc scopes it to "the wave-0 rows ... layering rules"); it does not attempt the WARN-as-failure test policy. A repair should either add a Logback `ListAppender`-based JUnit assertion (base test class or extension) enforcing WARN-as-failure on the backend, and confirm/add the equivalent Vitest unhandled-error behavior on the frontend, or the plan should record an explicit `DEV-n` deviation for this rule with owner and reason. Until one of those exists, this dimension stays FAIL independent of any other dimension's result.
