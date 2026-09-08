# Wave-2 verifier — dimension: architecture (round 2)

**Status: PASS**, bound to wave SHA `b87a701244f5316e1a53bace7a1e41facdffc277`.
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture` (HEAD confirmed == wave SHA via `git rev-parse HEAD`, read-only throughout — every throwaway probe file was deleted; `git status --porcelain` is empty at the end of this run).

Verified independently: no worker reports or review files were read for this run.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| event-stream | parity | New code (`application/tracking/EventIngestionService`, `infrastructure/mercure/*`, `infrastructure/tracking/*`, `domain/tracking/*`, `web/CollectController`, `web/EventsController`) sits entirely inside the existing `web`/`application`/`domain`/`infrastructure` layering; both wave-2 rules (Mercure-publisher access boundary, `/accounts/` topic-literal ownership) hold and were proven to fire on a negative case |
| customers | parity | New code (`application/CustomersViewService`, `web/CustomersController`, `web/dto/Customer*Response`) sits inside the existing layering; no wave-2-tagged rule in `rules-translated.md` is specific to this journey beyond the shared layering/no-cycle rules, which hold |

Neither journey needed a deviation for this dimension.

## Wave-2 rule → tooling table (rows tagged `wave-2` in `architecture/rules-translated.md`)

| Rule | Tooling (as declared in `rules-translated.md`) | Enforcing test/rule found in checkout | Negative-case proof |
| --- | --- | --- | --- |
| Mercure topic built only server-side | ArchUnit; ESLint (`no-restricted-syntax` on `/accounts/` literals) | Backend: `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` (restricts `MercurePublisher`/`HttpMercurePublisher` access to `application.tracking..` and `infrastructure.mercure..`) and `ArchitectureTest.accountsTopicLiteralExistsOnlyInEventStreamTopic` (plain source scan: the `"/accounts/` string literal may exist only in `domain/tracking/EventStreamTopic.java`). Frontend: `frontend/eslint.config.js`, the `files: ["src/**/*.{ts,vue}"]` block's `no-restricted-syntax` rule with `Literal[value=/\/accounts\//]` and `TemplateElement[value.raw=/\/accounts\//]` selectors | **Backend boundary rule**: added `backend/src/main/java/click/kivvi/web/TmpArchViolation.java` calling `publisher.publish(...)` on an injected `MercurePublisher` from the `web` package → `./mvnw test -Dtest=ArchitectureTest` failed (exit 1), both `infrastructureIsOnlyUsedFromApplication` and `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` reported the violation naming `TmpArchViolation.triggerViolation()`. **Backend literal-scan rule**: replaced the file with a class holding `static final String TOPIC = "/accounts/1/events";` → `accountsTopicLiteralExistsOnlyInEventStreamTopic` failed (exit 1), asserting the literal now appears in two files instead of one. **Frontend**: added `frontend/src/tmp-arch-violation.ts` exporting `` `/accounts/1/events` `` as a template literal → `npx eslint src/tmp-arch-violation.ts` failed (exit 1, `no-restricted-syntax`). All three probe files were deleted immediately after their run; `git status --porcelain` confirmed clean before the next probe and at the end |
| Row markup in one place | ESLint rule `vue/no-restricted-class` for `event-row` outside `EventRow.vue` | `frontend/eslint.config.js`, the `files: ["src/**/*.vue"], ignores: ["src/components/molecules/EventRow.vue"]` block's `vue/no-restricted-class: ["error", "event-row"]`. (Backend side is structurally out of scope: `CollectApiIT`'s DEV-3 test already proves the hub payload is JSON, never HTML — the backend never emits a competing `.event-row` markup to police; `ArchitectureTest`'s class Javadoc records this as "transitively" enforced by the ESLint rule alone) | Added `frontend/src/components/molecules/TmpArchViolation.vue` with `<div class="event-row">` → `npx eslint src/components/molecules/TmpArchViolation.vue` failed (exit 1, `'event-row' class is not allowed` / `vue/no-restricted-class`). File deleted immediately after; `git status --porcelain` confirmed clean |

Both wave-2 rows have working enforcement; neither lacks tooling; no deviation was needed for this dimension.

## Regression check on earlier-wave rows (regression guards only, per round-2 note — not re-probed for negative cases, only confirmed still green)

| Rule (wave) | Tooling | Result this round |
| --- | --- | --- |
| Layering: `web`↛`domain`, `infrastructure` only from `application`, no cycles (wave-0) | `ArchitectureTest.domainDoesNotDependOnWeb`, `.infrastructureIsOnlyUsedFromApplication`, `.topLevelPackagesFormNoCycle` | held — part of the same `./mvnw test -Dtest=ArchitectureTest` run (5/5 pass at baseline and after cleanup) |
| SPA `format.ts` is the only importer of `Intl.NumberFormat` (wave-1) | `frontend/eslint.config.js`: `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl` | held — `npm run lint` baseline and final-clean runs both exit 0 with the same 2 pre-existing formatting warnings in `FeedCard.vue` (unrelated to any boundary rule) |

`Static typing gate`, `Formatting @Symfony`, and `Test policy fail-on-warning` (wave-0) are compiler/Spotless/JUnit-extension concerns, not ArchUnit/ESLint boundary rules, and are out of this dimension's scope (covered by `unit`/`static analysis` gates elsewhere); `Storybook dev-only` is tagged "Out of scope for parity" in the table itself.

## Global constraints checked (architecture-adjacent, no drift since wave-1's assembled SHA `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`)

- `git diff --quiet 4f74907..b87a701 -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` — exit 0, empty: no old-stack/protected path touched by wave-2.
- Full diff wave-1→wave-2: 81 files changed, 6177 insertions(+), 23 deletions(-), entirely under `backend/src`, `frontend/src`, `frontend/test`, `tools/migration-verify/`.
- `find backend/src/main/resources/db/migration -type f` → only `V1__baseline.sql`, unchanged from wave-1: no new Flyway migration, no schema layering drift.
- `git diff 4f74907..b87a701 -- backend/pom.xml frontend/package.json` — exit 0, empty: no dependency/version pin changed since wave-1 (already confirmed against the plan's Technology decisions table in `wave-1/architecture.md`).
- New backend main-source files for this wave all sit under the four existing top-level packages (`web`, `application`, `domain`, `infrastructure`) or `fixtures/`; no new top-level package introduced (full list: `architecture/layering.log`). `EventIngestionService` (in `application.tracking`) is confirmed the sole application-layer caller of `MercurePublisher.publish(...)` in the diff (`grep -rn "\.publish(" backend/src/main/java/click/kivvi/` finds one call site outside `infrastructure/mercure/`, and the only other `MercurePublisher` reference outside that package is a Javadoc mention in `EventDedupLedger.java`, not a real dependency).

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture` | 0 (`b87a701244f5316e1a53bace7a1e41facdffc277`, matches packet) |
| `git status --porcelain` (pre-run) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture` | 0, empty |
| `./mvnw -q test -Dtest=ArchitectureTest` (baseline) | `backend/` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` (baseline) | `frontend/` | 0 (2 unrelated pre-existing warnings, 0 errors) |
| add `web/TmpArchViolation.java` (publisher-boundary probe); `./mvnw test -Dtest=ArchitectureTest` | `backend/` | 1 (expected FAIL — 2 tests failed naming the probe) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `web/TmpArchViolation.java` (topic-literal probe); `./mvnw test -Dtest=ArchitectureTest` | `backend/` | 1 (expected FAIL — `accountsTopicLiteralExistsOnlyInEventStreamTopic`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| `./mvnw -q test -Dtest=ArchitectureTest` (final clean re-check) | `backend/` | 0 |
| add `frontend/src/tmp-arch-violation.ts` (accounts-literal probe); `npx eslint src/tmp-arch-violation.ts` | `frontend/` | 1 (expected FAIL — `no-restricted-syntax`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `frontend/src/components/molecules/TmpArchViolation.vue` (event-row probe); `npx eslint src/components/molecules/TmpArchViolation.vue` | `frontend/` | 1 (expected FAIL — `vue/no-restricted-class`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| `npm run lint` (final clean re-check) | `frontend/` | 0 (same 2 unrelated warnings, 0 errors) |
| `git diff --quiet 4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a..b87a701244f5316e1a53bace7a1e41facdffc277 -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | checkout root | 0 (empty diff) |
| `git diff 4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a..b87a701244f5316e1a53bace7a1e41facdffc277 -- backend/pom.xml frontend/package.json` | checkout root | 0 (empty diff) |
| `git status --porcelain` (post-run, final) | checkout root | 0, empty |

Environment: `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25` (`java -version` → `openjdk version "25.0.4.1" 2026-08-18 LTS`); host Node `v26.8.1`/npm `11.19.0`; `npm ci` run only inside `frontend/` of this checkout, per packet instructions. No Docker stack was started or stopped by this run; the running stack at `https://localhost:19101` was not needed for this dimension (ArchUnit/ESLint are static checks) and was not touched.

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/architecture/`:

- `archunit-baseline.log` — baseline `ArchitectureTest` run, 0 violations
- `archunit-negative-mercure-boundary.log` — negative probe, publisher-access boundary rule, exit 1 with violation detail
- `archunit-negative-topic-literal.log` — negative probe, `/accounts/` literal scan rule, exit 1 with violation detail
- `archunit-final-clean.log` — re-run after both backend probes were deleted, 0 violations
- `frontend-lint-baseline.log` — baseline `npm run lint`, 0 errors
- `eslint-negative-accounts-literal.log` — negative probe, `no-restricted-syntax` `/accounts/` rule, exit 1
- `eslint-negative-event-row.log` — negative probe, `vue/no-restricted-class` `event-row` rule, exit 1
- `frontend-lint-final-clean.log` — re-run after both frontend probes were deleted, 0 errors
- `global-constraints.log` — protected-path diff, Flyway migration list, dependency-pin diff
- `layering.log` — full list of backend main-source files touched by wave-2 and the layering observation

## Verdict rationale

Both wave-2 rows of `rules-translated.md` ("Mercure topic built only server-side", "Row markup in one place") have real, working enforcement — an ArchUnit access-boundary rule, an ArchUnit/plain-source literal scan, and two ESLint rules — and each was independently proven to fire by introducing a throwaway violation, observing the expected exit-1 failure with the expected rule/message, then deleting the probe and confirming a clean `git status --porcelain` and a green re-run. The carried-over wave-0 layering rules and wave-1 `Intl`-ban rule still hold with 0 violations/errors. No rule in the table lacks tooling for this wave; no deviation was required. No old-stack or protected path was touched since wave-1's assembled SHA, no new Flyway migration was added, and no dependency pin drifted. The checkout's `git status --porcelain` is empty at the end of this run — no tracked-file change was left behind.
