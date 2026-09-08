# Verifier report — wave wave-0, dimension architecture (round 2)

Wave SHA: `6a53642c9c5ec9c256625854e6670f035e0292be` · Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-arch` (HEAD confirmed == wave SHA). Journeys: login.

## Dimension status: PASS

## Journey verdict

| Journey | Verdict |
| --- | --- |
| login | parity |

## Rule → tooling → result (wave-0 rows of `architecture/rules-translated.md`)

| Rule | Enforcing tooling | Command | Result |
| --- | --- | --- | --- |
| Static typing gate (PHPStan 5 → `-Werror`-equivalent) | `maven-compiler-plugin` `-Xlint:all -Werror` (backend/pom.xml, default `compile`/`test-compile` lifecycle binding — no `<executions>` needed, these are core-lifecycle goals); `vue-tsc --noEmit` (frontend `npm run typecheck`) | `./mvnw -q clean compile test-compile`; `npm run typecheck` | PASS — both exit 0, clean compile with all lint categories as errors |
| Formatting (`@Symfony` → Spotless/Prettier) | `spotless-maven-plugin` 3.10.2 + `google-java-format` 1.36.1 (backend); `prettier` 3.8.4 (frontend, existing config, no dedicated `.prettierrc` — house defaults) | `./mvnw -q spotless:check` | PASS — exit 0, current tree already conforms. Caveat: `spotless-maven-plugin` has no `<executions>` binding `check` to `compile`/`test`/`verify` (confirmed empirically: `mvn test` and `mvn verify -Dmaven.test.skip=true` never invoke a spotless goal), and `next-build.yml` never calls `spotless:check` despite its step comment claiming to; frontend has no `prettier --check`/format script or CI step either. Formatting is real and currently clean but only self-enforced by this verifier round, not by CI. Not a FAIL (rule not violated, tool present and functional per the packet's own command list), but worth a follow-up ticket. |
| Test policy fail-on-warning | Backend: `FailOnWarnLogExtension`, auto-registered for every test via `META-INF/services/org.junit.jupiter.api.extension.Extension` + `junit.jupiter.extensions.autodetection.enabled=true` in `junit-platform.properties` (no per-class `@ExtendWith` needed); asserts on `click.kivvi.*` WARN+ and unexpected `System.err`. Frontend: `frontend/test/setup.ts`, wired via `vite.config.ts` `test.setupFiles`; wraps `console.warn`/`console.error` (first-party-stack filtered) and sets `config.global.config.warnHandler` for Vue runtime warnings; unhandled errors covered by Vitest's own default (`dangerouslyIgnoreUnhandledErrors: false`, explicit in config) | Static self-tests: `FailOnWarnLogExtensionTest` (backend, 6 cases) and the frontend's own unit suite exercise the detection logic. Dynamic proof (this round, throwaway, never committed): a scratch copy of `backend/` with one extra test doing `LOG.warn(...)` via `click.kivvi.testsupport.Throwaway` → **failed** with the expected `AssertionError`, with zero `@ExtendWith` on the class (proves auto-detection, not just the logic). A scratch copy of `frontend/` with a `src/throwaway-warn.ts` calling `console.warn` from a first-party stack frame, imported by a throwaway spec → **failed** with the expected `test/setup.ts` error. Both throwaway copies deleted after the run; the checkout was never touched. | PASS — wiring proven end-to-end on both sides |
| Layering (controller → application → domain → infrastructure) | ArchUnit 1.5.0, `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`: `domain` may not depend on `web`; `infrastructure` only reachable from `infrastructure`/`application`; no cycles among `click.kivvi.(*)..` slices. Matches Surefire's default `**/*Test.java` include pattern, so it also runs automatically under plain `mvn test` (confirmed: `target/surefire-reports/click.kivvi.architecture.ArchitectureTest.txt` present after an unfiltered `mvn test` run), not only when explicitly selected. | `./mvnw -q test -Dtest=ArchitectureTest` | PASS — 3/3 tests green (0 failures/errors), both filtered and as part of the full suite |

No wave-0 row in `rules-translated.md` lacks an equivalent (the file's own header states this), and no wave-0 architecture deviation exists in `tools/migration-verify/deviations.json` (its entries are all `visual`/`contract`, none `architecture`) — consistent with none being needed.

## Global constraints

- **No diff vs base** `8d3fc320354604b641b44a3043a070f279e6d491` under the frozen old-stack paths (`src/`, `templates/`, `assets/`, `translations/`, `migrations/`, `tests/**/*.php`, `tests/e2e/specs/`, `composer.*`, `compose.yaml`, `compose.override.yaml`, `compose.prod.yaml`, `Dockerfile`, `frankenphp/`, `config/`, `public/`): `git diff --stat` between base and wave SHA over exactly those pathspecs → empty output. PASS.
- **Pinned versions match the plan's Technology decisions table**: Spring Boot 4.1.1, Java 25 (Temurin 25.0.4.1+1, `java -version` confirmed), ArchUnit 1.5.0, Maven wrapper 3.9.11 (`.mvn/wrapper/maven-wrapper.properties`), Vue 3.5.42, vue-router 5.3.1, Pinia 4.0.3, Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, vue-i18n 11.4.10, TypeScript 6.0.3, Prettier 3.8.4, Node 26.8.1 (host `node --version`) — all exact matches in `backend/pom.xml` and `frontend/package.json`. PASS.
- **Flyway has only `V1__baseline.sql`**: `find backend -iname "*.sql"` under `db/migration` → exactly one file. PASS.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 0 |
| `./mvnw -q spotless:check` | `backend/` | 0 |
| `./mvnw -q clean compile test-compile` | `backend/` | 0 |
| `./mvnw -q test` (unfiltered, to confirm ArchitectureTest runs in the default suite) | `backend/` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` | `frontend/` | 0 |
| `npm run typecheck` | `frontend/` | 0 |
| `git diff --stat 8d3fc32...6a53642 -- <frozen paths>` | repo root of checkout | 0 (empty diff) |
| Throwaway backend warn-detection test (scratch copy, deleted after) | `/tmp/.../scratchpad/backend-throwaway` | 1 (expected failure, proves wiring) |
| Throwaway frontend warn-detection test (scratch copy, deleted after) | `/tmp/.../scratchpad/frontend-throwaway` | 1 (expected failure, proves wiring) |

JAVA_HOME used for all `mvnw` invocations: `/home/muszkin/.cache/kivvi-toolchains/jdk-25`.

## Evidence paths

- `waves/wave-0/architecture/backend-commands.log`
- `waves/wave-0/architecture/frontend-commands.log`
- `waves/wave-0/architecture/commands-summary.txt`
- `waves/wave-0/architecture/pom.xml.snapshot`
- `waves/wave-0/architecture/ArchitectureTest.java.snapshot`
- `waves/wave-0/architecture/ArchitectureTest.surefire.txt`
- `waves/wave-0/architecture/FailOnWarnLogExtension.java.snapshot`
- `waves/wave-0/architecture/FailOnWarnLogExtensionTest.java.snapshot`
- `waves/wave-0/architecture/junit-platform.properties.snapshot`
- `waves/wave-0/architecture/junit-services-extension.snapshot`
- `waves/wave-0/architecture/vite.config.ts.snapshot`
- `waves/wave-0/architecture/setup.ts.snapshot`
- `waves/wave-0/architecture/eslint.config.js.snapshot`
