# Wave-1 verifier — dimension: architecture (round 2)

**Status: PASS**, bound to wave SHA `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`.
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-architecture` (HEAD confirmed == wave SHA, read-only, no edits made).

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| landing | parity | web/application/fixtures/domain layering intact; ArchUnit + ESLint clean |
| feeds | parity | web/application/fixtures/domain layering intact; ArchUnit + ESLint clean |
| scheduler-heartbeat | parity | scheduling code confined to `infrastructure/scheduling`; DEV-6 (single process) unaffected — not an architecture-dimension concern |

## Rule → tooling table (rows tagged `wave-0` or `wave-1` in `architecture/rules-translated.md`)

| Rule | Tooling | Result |
| --- | --- | --- |
| Static typing gate (PHPStan 5) → `-Werror`/`-Xlint:all` + `vue-tsc --noEmit` strict | `maven-compiler-plugin` (`-Xlint:all -Werror`, `backend/pom.xml`); `frontend/tsconfig.json` (`strict`, `noUncheckedIndexedAccess`, etc.) | enforced, 0 violations (`mvnw verify` exit 0 compiles cleanly; `npm run typecheck` exit 0) |
| Formatting `@Symfony` → Spotless (google-java-format) / Prettier | `spotless-maven-plugin` bound to `verify` phase (`check-formatting` execution, `backend/pom.xml:181-199`); `prettier 3.8.4` | enforced, 0 violations (`mvnw verify` exit 0; `npm run format:check` exit 0) |
| Test policy fail-on-warning | `FailOnWarnLogExtension` auto-registered via `junit-platform.properties` (all `click.kivvi.*` WARN+ fails a test); Vitest `dangerouslyIgnoreUnhandledErrors: false` in `vite.config.ts` | enforced, present and exercised (`mvnw verify` runs the extension on every test, exit 0) |
| Layering: `web`↛`domain`, `infrastructure` only from `application`, no cycles | `ArchitectureTest.java` (ArchUnit 1.5.0, `@AnalyzeClasses`-equivalent `ClassFileImporter`) | enforced, 0 violations (`mvnw test -Dtest=ArchitectureTest` exit 0) |
| **wave-1: SPA `format.ts` is the only importer of `Intl.NumberFormat`** | `frontend/eslint.config.js`: `no-restricted-imports` (pattern `*Intl*`), `no-restricted-globals` (`Intl`), `no-restricted-syntax` (`Intl.*` member expr, `.toLocaleString()`, `.toFixed()`) — exception scoped to `files: ["src/format.ts"]` | enforced. Positive: `npm run lint` exit 0 (checkout has no `src/format.ts` yet and zero `Intl.` references outside it — rule holds vacuously; numbers are formatted server-side via `domain/Format.java`, matching the rule's stated rationale). Negative probe (temp copy outside checkout, `<scratchpad>/eslint-negative-probe`): a file using `Intl.NumberFormat` directly fails lint (exit 1, `no-restricted-globals` + `no-restricted-syntax`); the identical code moved into `src/format.ts` passes (exit 0) — see `architecture/eslint-negative-probe.md` |

No row for this wave lacks tooling; no deviation was needed.

## Layering for new wave-1 code

`backend/src/main/java/click/kivvi/`: scheduling code lives under `infrastructure/scheduling/` (`HeartbeatJob`, `HeartbeatTrigger`, `HeartbeatLockHistory`, `JdbcHeartbeatLockHistory`) plus `infrastructure/config/SchedulingConfig`. Landing/feeds are split across `web/{LandingController,FeedsController,dto/FeedsResponse}`, `application/{LandingView,LandingViewService,FeedsViewService}`, `fixtures/{LandingFixtures,FeedsFixtures}`, `domain/{RouteTable,Format,...}` — no new top-level package introduced. Full listing: `architecture/layering.log`.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 0 |
| `./mvnw -q verify` (spotless bound to `verify`) | `backend/` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` | `frontend/` | 0 |
| `npm run typecheck` | `frontend/` | 0 |
| `npm run format:check` | `frontend/` | 0 |
| `npm run lint` (negative probe: `Intl.NumberFormat` outside `format.ts`) | `<scratchpad>/eslint-negative-probe/` | 1 (expected FAIL) |
| `npm run lint` (exception probe: same code inside `src/format.ts`) | `<scratchpad>/eslint-negative-probe/` | 0 (expected PASS) |
| `git diff --quiet 8d3fc320354604b641b44a3043a070f279e6d491 -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | checkout root | 0 (empty diff) |
| `cmp` × 5 (`frontend/src/styles/*.css` vs `/home/muszkin/work/kivvi-click/assets/styles/*.css`) | checkout root | 0 each (byte-identical) |

## Global constraints

- Empty diff vs base `8d3fc320354604b641b44a3043a070f279e6d491` under every listed old-stack/protected path: **confirmed** (`git diff --quiet` exit 0). Full diff vs base touches only `backend/`, `frontend/`, `compose.next.yaml`, `compose.next.prod.yaml`, `.github/workflows/next-build.yml`, `.gitignore`, `mercure/Caddyfile`, `tools/migration-verify/` — 167 files, insertions only, no deletions.
- Flyway: only `V1__baseline.sql` present under `backend/src/main/resources/db/migration/`. **Confirmed.**
- Five `frontend/src/styles/*.css` files byte-identical (`cmp`) to `/home/muszkin/work/kivvi-click/assets/styles/*.css`. **Confirmed**, all 5.
- Pinned versions match the plan's Technology decisions table: Spring Boot 4.1.1, Java 25 (Temurin 25.0.4.1+1 confirmed via `java -version`), ArchUnit 1.5.0, ShedLock 7.10.0 (`shedlock-spring` + `shedlock-provider-jdbc-template`, Apache-2.0), Mercure `v0.24.2`, Vue 3.5.42, vue-router 5.3.1, Pinia 4.0.3, vue-i18n 11.4.10, Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, TypeScript 6.0.3, Prettier 3.8.4. **All confirmed, no mismatches.**

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/architecture/`:
`archunit-test.log`, `mvnw-verify.log`, `frontend-npm-ci.log`, `frontend-lint.log`, `frontend-typecheck.log`, `frontend-format-check.log`, `eslint-negative-probe.md`, `global-constraints.log`, `layering.log`.

## Verdict rationale

No rule in `rules-translated.md` tagged `wave-0`/`wave-1` is violated; none lacks tooling. All commands ran green (`mvnw verify` includes ArchUnit as part of `test`, plus Spotless-check on `verify`; `ArchitectureTest` run standalone confirms 0 layering violations). The wave-1-specific `format.ts` rule has working, verified enforcement (positive clean run + negative probe that correctly fails + exception-path probe that correctly passes). All global constraints (empty diff on protected paths, single Flyway migration, byte-identical CSS, pinned versions) hold. Round-1 failed only the `integration` dimension (missing `LandingApiIT`, since added at `backend/src/test/java/click/kivvi/LandingApiIT.java`) — outside this dimension's scope and not re-litigated here.
