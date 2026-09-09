# Verifier summary — wave FINAL (all journeys, round 2), dimension: e2e

**Dimension status: PASS**, bound to wave SHA `dae169614a52532c130bd34435994d6a914165c0`
(checkout `/home/muszkin/work/kivvi-click-wt/verify-final-e2e`, detached, clean throughout —
`git status --porcelain` empty; confirmed `git log -1` = `dae1696` before any run).

Scope executed exactly as instructed by `waves/final/verifier-e2e.md` + `common-journey-rules.md`
+ plan section "Verifier contract" (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`).
Read-only: stack `kivvi-int` (https://localhost:19101) was not started or stopped, no tracked
file was modified, no worker reports / review files / round-1 evidence were read. Ran ALONE on
the host — no other verifier active concurrently; `frontend/dist` was built once, before any
Playwright invocation, and deleted immediately after `performance.mjs` ran (see Disk section).
Every Playwright invocation ran strictly sequentially, one file (or one repeat) per invocation;
no two invocations ran at once.

## Round-2 scope note

Round 2 follows the shell-navigation label repair, commit `dae1696 test: label B02/B03/B21 unit
coverage and pin the main-scroll invariant (#shell-navigation)` — a test-only change
(`backend/src/test/java/click/kivvi/application/ShellViewServiceTest.java`,
`frontend/test/unit/AppShell.spec.ts`, `frontend/test/unit/Topbar.spec.ts`). None of those files
are `tests/e2e/specs/*`, so the e2e dimension is a from-scratch full-suite run against the
round-2 SHA rather than a delta re-check; `navigation.spec.ts` (the shell-navigation journey's
spec) was run the full FIVE times as instructed, given the round-1→round-2 history around
shell-navigation.

## Result: every test in every file passed, zero failures, no re-run needed

122 Playwright tests executed across 14 invocations (10 spec files, `navigation.spec.ts` counted
five times); every one passed on the first attempt. No single-test flake occurred, so the
"re-run once, report host load" contingency in the packet was never triggered.

## Journeys and specs — every test that ran (all green)

| # | File | -g filter | Workers | Tests | Result |
| --- | --- | --- | --- | --- | --- |
| 1 | `public.spec.ts` | (whole file) | 5 | 5 | 5/5 passed |
| 2 | `lists.spec.ts` | (whole file) | 5 | 5 | 5/5 passed |
| 3 | `editors.spec.ts` | (whole file) | 6 | 6 | 6/6 passed |
| 4 | `customers.spec.ts` | (whole file) | 4 | 4 | 4/4 passed |
| 5 | `automations.spec.ts` | (whole file) | 4 | 4 | 4/4 passed |
| 6 | `settings.spec.ts` | (whole file) | 8 | 12 | 12/12 passed |
| 7 | `import.spec.ts` | (whole file) | 6 | 6 | 6/6 passed |
| 8 | `events.spec.ts` | (whole file) | 1 (`--workers=1`) | 4 | 4/4 passed |
| 9 | `dashboard.spec.ts` | (whole file) | 1 (`--workers=1`) | 6 | 6/6 passed |
| 10 | `navigation.spec.ts` run 1/5 | (whole file) | 1 (`--workers=1`) | 14 | 14/14 passed |
| 11 | `navigation.spec.ts` run 2/5 | (whole file) | 1 (`--workers=1`) | 14 | 14/14 passed |
| 12 | `navigation.spec.ts` run 3/5 | (whole file) | 1 (`--workers=1`) | 14 | 14/14 passed |
| 13 | `navigation.spec.ts` run 4/5 | (whole file) | 1 (`--workers=1`) | 14 | 14/14 passed |
| 14 | `navigation.spec.ts` run 5/5 | (whole file) | 1 (`--workers=1`) | 14 | 14/14 passed |

**Total: 122/122 Playwright tests passed. 0 failures. 0 flakes.**

`node tools/migration-verify/performance.mjs --base https://localhost:19101` — exit 0, all four
DEV-8 budgets met:

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| `initialJsGzipBytes` | 101 990 B | 307 200 B | yes |
| `lcpMillis` | 156 ms | 2000 ms | yes |
| `ttiMillis` | 14.6 ms | 2500 ms | yes |
| `collectP95Millis` | 6.36 ms | 500 ms | yes |

## Per-journey verdicts (13)

| Journey | Specs (per packet) | Verdict | Note |
| --- | --- | --- | --- |
| login | `public.spec.ts -g login`, `navigation.spec.ts -g "sidebar collapse"`, `-g "theme toggle"` | parity | covered by the whole-file runs: `public.spec.ts` #5 "signs in and shows the identity in the sidebar", #6 "the browser blocks a malformed address before it is sent"; `navigation.spec.ts` #11 "sidebar collapse survives a reload", #12 "theme toggle survives a reload" — all green in every one of the 5 navigation runs |
| landing | `public.spec.ts` (whole file) | parity | `public.spec.ts` #1–3: hero/preview/features/steps/pricing, English landing structure, demo button lands in panel — all green |
| feeds | `lists.spec.ts -g feeds` | parity | `lists.spec.ts` "product feeds" describe block, both tests green |
| scheduler-heartbeat | backend `HeartbeatSchedulerIT` + db.json shedlock delta (contract) | **NOT_APPLICABLE** | no browser spec exists for this journey — it has no Playwright coverage by design (backend/contract dimensions own it) |
| event-stream | `events.spec.ts --workers=1` | parity | 4/4 |
| customers | `customers.spec.ts` | parity | 4/4 |
| automations | `automations.spec.ts` | parity | 4/4 |
| settings | `settings.spec.ts` | parity | 12/12 (8 tab sub-tests + 4 content tests) |
| campaigns-email-editor | `lists.spec.ts -g campaigns`, `editors.spec.ts -g "email editor"` | parity | `lists.spec.ts` "campaigns" describe, `editors.spec.ts` "email editor" describe (2 tests) — all green |
| popups-widget-editor | `lists.spec.ts -g widgets`, `editors.spec.ts -g "popup editor"` | parity | `lists.spec.ts` "widgets" describe (2 tests), `editors.spec.ts` "popup editor" describe (4 tests) — all green |
| import-wizard | `import.spec.ts` | parity | 6/6 |
| dashboard | `dashboard.spec.ts --workers=1` | parity | 6/6 |
| shell-navigation | `navigation.spec.ts --workers=1` (14/14, FIVE times) | parity | 14/14 on every one of 5 independent runs, no flake |

No journey required an accepted-deviation id for the e2e dimension; none of DEV-1..DEV-13 change
Playwright pass/fail behaviour (DEV-8 governs `performance.mjs` only, which also passed cleanly).

## Commands run (this checkout, cwd as shown)

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| — | `npm ci` | `frontend/` | 0 |
| — | `npm run build` (`vue-tsc --noEmit && vite build`) — run once, before any Playwright invocation | `frontend/` | 0 |
| — | `rm -rf frontend/node_modules` (immediately after build; not needed again) | `frontend/` (parent) | n/a |
| — | `npm ci` | `tests/e2e/` | 0 |
| — | `npm ci` | `tools/migration-verify/` | 0 |
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

Environment snapshot at start (informational, no re-run was needed since nothing failed):
`/proc/loadavg` = `8.80 18.73 258.30 1/4947 1677003`; `docker ps | grep -ci runner` = `3` (three
unrelated `github-runners-*` containers on the host, not test runners for this suite).

## Evidence paths

- `waves/final/e2e/logs/01-public.log` … `07-import.log` — one log per whole-file run.
- `waves/final/e2e/logs/08-events.log`, `09-dashboard.log` — `--workers=1` runs.
- `waves/final/e2e/logs/10-navigation-run1.log` … `run5.log` — the five independent
  `navigation.spec.ts --workers=1` runs, 14/14 each.
- `waves/final/e2e/logs/11-performance.log` — full `performance.mjs` JSON output and exit code.
- This file: `waves/final/e2e.md`.

## Disk

`frontend/node_modules` deleted right after `npm run build` completed (not needed again —
`performance.mjs` reads `frontend/dist/assets` directly from disk, not through the package).
After `performance.mjs` ran, `tests/e2e/node_modules`, `tools/migration-verify/node_modules`,
`frontend/dist` and `tests/e2e/test-results` were deleted from this checkout, per the packet's
disk instruction — confirmed empty/absent below.
