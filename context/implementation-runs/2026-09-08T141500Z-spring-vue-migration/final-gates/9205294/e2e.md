# Verifier summary — final gate on frozen feature SHA `92052947a6238ecf4ee177f4e72493122315ce32`, dimension: e2e

**Dimension status: PASS**, bound to SHA `92052947a6238ecf4ee177f4e72493122315ce32`
(short `9205294`). Checkout: `/home/muszkin/work/kivvi-click-wt/verify-final-e2e`, detached,
clean throughout — `git status --porcelain` empty before and after the run;
`git rev-parse HEAD` = `92052947a6238ecf4ee177f4e72493122315ce32` confirmed at start and at
the end.

Read-only: the `kivvi-int` compose stack (https://localhost:19101) was neither started nor
stopped — confirmed healthy (`api`, `database`, `mercure` all "healthy") before the run and left
untouched. Ran ALONE on the host for the Playwright/performance work: no other verifier and no
Maven build ran concurrently with any Playwright invocation (`ps aux | grep mvnw` empty
immediately before the sequence started). Every Playwright invocation ran strictly sequentially,
one file (or one repeat) per invocation — never combined `-g` filters, per the packet's warning
that Playwright keeps only the last `-g` on the command line.

## Procedure followed

Read `waves/final/verifier-e2e.md` (procedure/format reference — this run's own packet, per the
task, uses `final-gates/9205294/e2e/` and `final-gates/9205294/e2e.md` instead of the
`waves/final/` paths that file names), `common-journey-rules.md`, and the plan's "Verifier
contract" section (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md` line 448) for
the e2e dimension's command and threshold: run the journeys' Playwright specs from `tests/e2e`
(`E2E_BASE_URL` pointed at the stack) plus `performance.mjs` budgets; FAIL if any step fails or a
budget is exceeded without a deviation.

Deviations checked: `tools/migration-verify/deviations.json` on this SHA has zero entries with
`"dimension": "e2e"` — confirmed by direct inspection. The only change to that file between the
prior all-green e2e SHA (`dae1696`, wave `final` round 2) and this SHA is a note-only
reconciliation on DEV-12 (visual/contract dimension, unrelated to e2e). No e2e-dimension
deviation applies; none was needed.

## Load-average gate (before starting Playwright)

Bounded wait, polling `/proc/loadavg` every 30 s, capped at 15 minutes, per the task instruction.
Started at **load1 = 32.85** (13:20:52 UTC). The host is shared with many long-running,
unrelated tenants (several persistent Java/Gradle/Kafka/Codex/Claude processes with multi-hour
to multi-day uptimes — confirmed via `ps -eo pid,pcpu,etime,cmd`); this project's own stack
containers were idle throughout (`docker stats`: 0.00–0.03 % CPU on `kivvi-int-api-1`,
`-database-1`, `-mercure-1`). Load fluctuated with an unrelated burst up to 98.95 mid-wait, then
settled; at the 900 s bound it was **load1 = 21.93** — still above the 10 threshold, so the wait
ended on **TIMEOUT**, not on the load condition. Per the task's "bounded, up to 15 min" wording,
Playwright started after the bound elapsed rather than waiting indefinitely for a value this
chronically-loaded host was not trending to reach. This is recorded as context for the (zero)
failures below; the single-retry contingency was never triggered because nothing failed.
Full poll trace: `final-gates/9205294/e2e/load-wait.log`.

## Setup (once, before any Playwright invocation)

| Step | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `frontend/` | 0 |
| `npm run build` (`vue-tsc --noEmit && vite build`) — built once, before any Playwright run, for `performance.mjs`'s JS-budget check | `frontend/` | 0 |
| `npm ci` | `tests/e2e/` | 0 |
| system Chrome present (`/usr/bin/google-chrome`, `/usr/bin/google-chrome-stable`) — confirmed, no install needed | — | — |

## Result: every test in every file passed, zero failures, no re-run needed

122 Playwright tests executed across 14 invocations (10 spec files, `navigation.spec.ts` counted
five times); every one passed on the first attempt. No single-test flake occurred despite the
elevated host load noted above, so the "re-run once, report load/runners" contingency in the
packet was never triggered.

## Journeys and specs — every test that ran (all green)

| # | File | Workers | Tests | Result |
| --- | --- | --- | --- | --- |
| 1 | `public.spec.ts` | 5 | 5 | 5/5 passed |
| 2 | `lists.spec.ts` | 5 | 5 | 5/5 passed |
| 3 | `editors.spec.ts` | 6 | 6 | 6/6 passed |
| 4 | `customers.spec.ts` | 4 | 4 | 4/4 passed |
| 5 | `automations.spec.ts` | 4 | 4 | 4/4 passed |
| 6 | `settings.spec.ts` | 8 | 12 | 12/12 passed |
| 7 | `import.spec.ts` | 6 | 6 | 6/6 passed |
| 8 | `events.spec.ts` | 1 (`--workers=1`) | 4 | 4/4 passed |
| 9 | `dashboard.spec.ts` | 1 (`--workers=1`) | 6 | 6/6 passed |
| 10 | `navigation.spec.ts` run 1/5 | 1 (`--workers=1`) | 14 | 14/14 passed |
| 11 | `navigation.spec.ts` run 2/5 | 1 (`--workers=1`) | 14 | 14/14 passed |
| 12 | `navigation.spec.ts` run 3/5 | 1 (`--workers=1`) | 14 | 14/14 passed |
| 13 | `navigation.spec.ts` run 4/5 | 1 (`--workers=1`) | 14 | 14/14 passed |
| 14 | `navigation.spec.ts` run 5/5 | 1 (`--workers=1`) | 14 | 14/14 passed |

**Total: 122/122 Playwright tests passed. 0 failures. 0 flakes. 0 re-runs needed.**

`node tools/migration-verify/performance.mjs --base https://localhost:19101` — exit 0, all four
DEV-8 budgets met:

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| `initialJsGzipBytes` | 101 990 B | 307 200 B | yes |
| `lcpMillis` | 216 ms | 2000 ms | yes |
| `ttiMillis` | 18.1 ms | 2500 ms | yes |
| `collectP95Millis` | 6.64 ms | 500 ms | yes |

These figures are identical (bundle size) or consistent within normal run-to-run variance
(timings) with the prior all-green e2e run on `dae1696` (wave `final` round 2): confirmed by
`git diff dae1696..9205294 --stat` — the only changes between that SHA and this one touch
`backend/` (forwarded-header trust config + a new backend test), compose/prod-isolation files,
CI workflow, and `context/` docs; nothing under `frontend/`, `tests/e2e/`, or the SPA build
inputs changed, so identical Playwright and bundle-size results were expected and observed.

## Per-journey verdicts (13; scheduler-heartbeat N/A)

| Journey | Specs | Verdict | Note |
| --- | --- | --- | --- |
| login | `public.spec.ts`, `navigation.spec.ts` | parity | `public.spec.ts` #1 "signs in and shows the identity in the sidebar", #2 "the browser blocks a malformed address before it is sent"; `navigation.spec.ts` #11 "sidebar collapse survives a reload", #12 "theme toggle survives a reload" — all green in every one of the 5 navigation runs |
| landing | `public.spec.ts` | parity | landing describe block (3 tests: hero/preview/features/steps/pricing, English structure, demo button) — all green |
| feeds | `lists.spec.ts` | parity | "product feeds" describe block, both tests green |
| scheduler-heartbeat | backend `HeartbeatSchedulerIT` + db.json shedlock delta (contract) | **NOT_APPLICABLE** | no browser spec exists for this journey by design — owned by the backend/contract dimensions, not e2e |
| event-stream | `events.spec.ts --workers=1` | parity | 4/4 |
| customers | `customers.spec.ts` | parity | 4/4 |
| automations | `automations.spec.ts` | parity | 4/4 |
| settings | `settings.spec.ts` | parity | 12/12 (8 tab sub-tests + 4 content tests) |
| campaigns-email-editor | `lists.spec.ts` (campaigns), `editors.spec.ts` (email editor) | parity | "campaigns" describe in lists, "email editor" describe in editors (2 tests) — all green |
| popups-widget-editor | `lists.spec.ts` (widgets), `editors.spec.ts` (popup editor) | parity | "widgets" describe in lists (2 tests), "popup editor" describe in editors (4 tests) — all green |
| import-wizard | `import.spec.ts` | parity | 6/6 |
| dashboard | `dashboard.spec.ts --workers=1` | parity | 6/6 |
| shell-navigation | `navigation.spec.ts --workers=1` (14/14, FIVE times) | parity | 14/14 on every one of 5 independent runs, no flake, despite the elevated host load recorded above |

No journey required an accepted-deviation id for the e2e dimension; deviations.json carries zero
`"dimension": "e2e"` rows on this SHA, and none were needed since nothing failed.

## Commands run (this checkout, cwd as shown)

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| — | `npm ci` | `frontend/` | 0 |
| — | `npm run build` (once, before any Playwright invocation) | `frontend/` | 0 |
| — | `npm ci` | `tests/e2e/` | 0 |
| 1 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/public.spec.ts` | `tests/e2e/` | 0 |
| 2 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/lists.spec.ts` | `tests/e2e/` | 0 |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/editors.spec.ts` | `tests/e2e/` | 0 |
| 4 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/customers.spec.ts` | `tests/e2e/` | 0 |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/automations.spec.ts` | `tests/e2e/` | 0 |
| 6 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/settings.spec.ts` | `tests/e2e/` | 0 |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/import.spec.ts` | `tests/e2e/` | 0 |
| 8 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/events.spec.ts --workers=1` | `tests/e2e/` | 0 |
| 9 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/dashboard.spec.ts --workers=1` | `tests/e2e/` | 0 |
| 10–14 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/navigation.spec.ts --workers=1` (×5) | `tests/e2e/` | 0 (all five) |
| 15 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | (checkout root) | 0 |

No test failed, so the "re-run the affected file once and report both load and runner state"
contingency was never exercised.

## Evidence paths

- `final-gates/9205294/e2e/logs/01-public.log` … `07-import.log` — one log per whole-file run.
- `final-gates/9205294/e2e/logs/08-events.log`, `09-dashboard.log` — `--workers=1` runs.
- `final-gates/9205294/e2e/logs/10-navigation-run1.log` … `run5.log` — the five independent
  `navigation.spec.ts --workers=1` runs, 14/14 each.
- `final-gates/9205294/e2e/logs/11-performance.log` — full `performance.mjs` JSON output and
  exit code.
- `final-gates/9205294/e2e/load-wait.log` — the bounded load-average poll trace (start 32.85,
  peak 98.95, end-of-bound 21.93 at 900 s / TIMEOUT).
- This file: `final-gates/9205294/e2e.md`.

## Cleanup

`tests/e2e/node_modules`, `frontend/node_modules`, `frontend/dist` and `tests/e2e/test-results`
(the last never existed — no test failed, so Playwright never wrote a trace/screenshot artifact)
removed from this checkout after the run. Verified empty/absent below.
