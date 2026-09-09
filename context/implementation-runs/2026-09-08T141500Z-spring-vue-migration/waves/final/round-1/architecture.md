# Architecture verifier — wave FINAL (all journeys, round 1)

**Dimension status: PASS**, bound to wave SHA `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-final-architecture` (detached at the wave SHA,
untouched at the end of this run — `git status --porcelain` empty, `HEAD` unchanged). No Docker
stack was started or stopped by this verifier.

Every row of the oracle's `architecture/rules-translated.md` has a named ArchUnit test / ESLint
rule / build gate, and every one was proven to actually fire by adding a throwaway negative probe,
running the exact gate command, observing the failure, then deleting the probe and re-confirming a
clean tree. Full transcript: [`architecture/rules-proof.md`](architecture/rules-proof.md).

All of the plan's "Global implementation constraints" checked for this dimension pass, with two
findings recorded (neither is a rule violation or a FAIL condition) — full transcript:
[`architecture/global-constraints.md`](architecture/global-constraints.md).

## Per-journey verdicts (13/13 — all `parity`)

The architecture dimension checks whole-codebase boundary rules (ArchUnit layering/Mercure-topic
rules, ESLint restricted-import/global/syntax/class rules), not journey-specific behaviour, so a
single global pass/fail applies to every journey sharing this codebase and SHA. All 13 are
`parity`:

| Journey | Verdict |
| --- | --- |
| login | parity |
| landing | parity |
| feeds | parity |
| scheduler-heartbeat | parity |
| event-stream | parity |
| customers | parity |
| automations | parity |
| settings | parity |
| campaigns-email-editor | parity |
| popups-widget-editor | parity |
| import-wizard | parity |
| dashboard | parity |
| shell-navigation | parity |

No `regression` verdicts. No `accepted-deviation` verdicts were needed for this dimension — the
oracle's `rules-translated.md` states "No source rule lacks an equivalent, so no deviation is
needed for this file", and that holds: every row has a proven equivalent on the target stack.

## Rule-by-rule proof (summary — see rules-proof.md for full transcripts)

| `rules-translated.md` row | Tooling | Baseline | Negative proven |
| --- | --- | --- | --- |
| 1. Static typing gate | `javac -Xlint:all -Werror` (backend); `vue-tsc --noEmit` (frontend) | green | yes, both sides |
| 2. Formatting `@Symfony` | Spotless/google-java-format (backend); Prettier (frontend) | green | yes, both sides |
| 3. Test policy fail-on-warning | `FailOnWarnLogExtension` (backend, autodetected); `test/setup.ts` console/Vue-warn interception + Vitest `dangerouslyIgnoreUnhandledErrors: false` (frontend) | green | yes, backend (unit-level + end-to-end) and frontend (console.warn + unhandled rejection) |
| 4. Layering convention | `ArchitectureTest` (ArchUnit 1.5.0), 3 rules | green (4/4 tests) | yes, all 3 rules |
| 5. Mercure topic server-side only | `ArchitectureTest` (2 rules, backend); ESLint `no-restricted-syntax` `/accounts/` (frontend) | green | yes, all 3 |
| 6. Row markup in one place | ESLint `vue/no-restricted-class` + `no-restricted-syntax` "event-row" (frontend) | green | yes, both selectors |
| 7. Formatting decided server-side (`Intl` ban) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` (frontend) | green (no `Intl` usage exists yet) | yes, all 3 rules |
| 8. Storybook dev-only | Vite build (no `stories/` exists) | vacuously satisfied | n/a — nothing to violate, matches DEV-10 |

## Global constraints — outcome table

| Constraint | Verdict | Detail |
| --- | --- | --- |
| Layering (domain/application/infrastructure/web) | PASS | ArchUnit rules above; **finding**: a 5th package `fixtures/` exists as a DAG leaf under `domain` (not a rule violation) and is Java classes rather than the JSON resources the plan's decision ledger described — unrecorded plan drift, not an architecture-rule violation |
| Flyway baseline unchanged since V1 | PASS | Only `V1__baseline.sql` exists; single creation commit, never modified |
| No Redis/RabbitMQ/other backing services in compose.next*.yaml | PASS | Only `api`, `mercure` (`dunglas/mercure:v0.24.2`), `database` (`postgres:18-alpine`) in both compose files |
| Pinned versions vs Target stack table | PASS | Zero drift across every checked dependency (Spring Boot 4.1.1, Java/Temurin 25.0.4.1+1, ArchUnit 1.5.0, ShedLock 7.10.0, Maven wrapper 3.9.11, Testcontainers 2.0.5, JUnit 6.0.3, Mercure 0.24.2, Postgres 18, Vue 3.5.42, vue-router 5.3.1, Pinia 4.0.3, vue-i18n 11.4.10, Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, TypeScript 6.0.3, Prettier 3.8.4) |
| No `Intl` outside `format.ts` | PASS | Zero usages anywhere; `format.ts` doesn't exist yet (unused exemption, not a violation) |
| `/accounts/` literal only in `EventStreamTopic` | PASS | ArchUnit source scan + ESLint rule, both proven |
| `event-row` only in the three allowed files | PASS | `EventRow.vue`, `ListCard.vue`, `RecentImports.vue` only |
| i18n PL-first, EN present for every key | PASS | 427/427 keys parity across 10 journey message modules + base catalogue |
| No protected old-stack path modified since `5b806ac` | PASS* | `compose.yaml` gained a `restart: unless-stopped` line — the pre-run R17 incident fix bundled into the plan-approval commit (`33b3f8d`, before the run started), not a journey-worker edit |

## Commands run (cwd, exit code)

All commands ran inside `/home/muszkin/work/kivvi-click-wt/verify-final-architecture` (or its
`backend`/`frontend` subdirectories as noted); full output for each is in `rules-proof.md` and
`global-constraints.md`.

| Command | cwd | Exit |
| --- | --- | --- |
| `java -version` | backend | n/a (info) |
| `./mvnw -v` | backend | 0 |
| `./mvnw -q -o test -Dtest=ArchitectureTest` (baseline) | backend | 0 |
| `./mvnw -q -o test -Dtest=ArchitectureTest#domainDoesNotDependOnWeb` (probe A) | backend | 1 |
| `./mvnw -q -o test -Dtest=ArchitectureTest#infrastructureIsOnlyUsedFromApplication` (probe B, field-only) | backend | 0 (see finding) |
| `./mvnw -q -o test -Dtest=ArchitectureTest#infrastructureIsOnlyUsedFromApplication` (probe B, method-call) | backend | 1 |
| `./mvnw -q -o test -Dtest=ArchitectureTest#topLevelPackagesFormNoCycle` (probe C) | backend | 1 |
| `./mvnw -q -o test -Dtest=ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` (probe D) | backend | 1 |
| `./mvnw -q -o test -Dtest=ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic` (probe E) | backend | 1 |
| `./mvnw -q -o test -Dtest=ArchitectureTest` (restored) | backend | 0 |
| `./mvnw -q -o compile` (compiler -Werror probe) | backend | 1 |
| `./mvnw -q -o spotless:check` (Spotless probe) | backend | 1 |
| `./mvnw -q -o test -Dtest=ProbeFailOnWarnEndToEndTest` | backend | 1 |
| `npm ci` | frontend | 0 |
| `npm run lint` (baseline) | frontend | 0 (0 errors, 12 unrelated pre-existing formatting warnings) |
| `npx eslint src/ProbeArchitecture.ts` (Intl/accounts/event-row combined probe) | frontend | 1 |
| `npx eslint src/components/molecules/ProbeEventRow.vue` (event-row template probe) | frontend | 1 |
| `npx vue-tsc --noEmit` (baseline) | frontend | 0 |
| `npx vue-tsc --noEmit` (type-error probe) | frontend | 2 |
| `npx vue-tsc --noEmit` (restored) | frontend | 0 |
| `npx prettier --check src/router/localeHref.ts` (formatting probe) | frontend | 1 |
| `npx vitest run test/unit/probeFailOnWarn.spec.ts` (console.warn, inline in spec — control) | frontend | 0 |
| `npx vitest run test/unit/probeFailOnWarn.spec.ts` (console.warn, through `src/`) | frontend | 1 |
| `npx vitest run test/unit/probeUnhandledRejection.spec.ts` | frontend | 1 |
| `node <scratchpad>/i18n-key-diff.mjs` | n/a (script imports checkout files by absolute path) | 0, 427/427 parity |
| `git diff --stat 5b806ac..HEAD -- <protected paths>` | repo root | 0, one file (`compose.yaml`, pre-run) |
| `git log --format=... -- backend/src/main/resources/db/migration/V1__baseline.sql` | repo root | 0, single creation commit |

## Evidence paths

- [`architecture/rules-proof.md`](architecture/rules-proof.md) — full per-rule proof transcripts
- [`architecture/global-constraints.md`](architecture/global-constraints.md) — full global-constraint transcripts
- This file: `architecture.md`

## Disk hygiene

`backend/target` (2.4M) and `frontend/node_modules` (161M) deleted immediately after use;
checkout left clean (`git status --porcelain` empty, `HEAD` still `7e6a66a`).

**Note:** host free disk (`df -h /`) was already at 2.9G — under the plan's K7 3GB floor — at the
very first check, before this dimension did any work (other dimensions' worktrees run
concurrently on the same host per the packet's sibling `verifier-*.md` files). It fluctuated
between 2.4G and 2.9G over the course of this run and stood at 2.7G at the end. This verifier's
own footprint (161M frontend + 2.4M backend, both deleted) was not the cause and was cleaned up
immediately per the packet's instruction; the pre-existing sub-3GB condition is a host-level K7
signal worth the run owner's attention, not something this dimension's checks could resolve or
that blocked completing the architecture verification.
