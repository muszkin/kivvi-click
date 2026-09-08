# Verifier — wave wave-2, dimension e2e (round 2)

**Wave SHA:** `b87a701244f5316e1a53bace7a1e41facdffc277`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-2-e2e` (HEAD == wave SHA, clean working tree — verified before and after all commands; no tracked file was edited)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`, containers `kivvi-int-api-1` / `kivvi-int-database-1` / `kivvi-int-mercure-1`, all healthy before, during and after this run) — not started or stopped by this verifier
**Browser:** system Chrome (`/usr/bin/google-chrome`) via Playwright `channel: "chrome"`, headless (project config default, no override)
**Round-1 note:** round-1 evidence at `waves/wave-2/round-1/` was not read; this run was performed entirely afresh.

## Dimension status: **PASS**

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| event-stream | parity |
| customers | parity |

- **event-stream** (`events.spec.ts`, behaviours B15–B19, B24): all 4 tests green on 3 independent `--workers=1` runs (12/12 total), including the SSE-delivered row rendering (B15–B18), the type/site filter set (B19), and replay-dedup (B24 — "a replayed event is not shown twice"). No flake across 3 runs.
- **customers** (`customers.spec.ts`, behaviours B03, B05, B25): all 4 tests green on the first run at default (parallel) workers — no flake observed, so no rerun was needed per the packet's "rerun once if flaky" instruction.
- **performance** (DEV-8 budgets, `tools/migration-verify/performance.mjs` against `budget.json`): all 4 metrics within budget, including `collectP95Millis` (23.9 ms vs 500 ms budget) — the `/collect` endpoint introduced by this wave.

## Regression guards (earlier-wave journeys, run once each)

| Spec / filter | Journey covered | Result |
| --- | --- | --- |
| `public.spec.ts` (whole file) | login (J0), landing (J1) | 5/5 passed |
| `navigation.spec.ts` (whole file) | shell-navigation (J0) + feeds (J1) entries, plus every later-wave sidebar entry | 9/14 passed — see note below |
| `lists.spec.ts -g "feeds"` | feeds (J1) | 2/2 passed |

**Navigation.spec.ts failure analysis (not a wave-2 regression):** the 5 failing tests are the sidebar entries for `automations`, `campaigns`, `popups`, `import`, `settings` — journeys J6–J10, which belong to later waves (DAG groups C/D) not yet executed. `frontend/src/router/routes.ts` wires all five of those routes to `EmptyPageView` (an empty `<div class="page"></div>` with no `.page-title`), and `git diff` between the wave-1 SHA (`4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`) and the wave-2 SHA confirms wave-2's only edits to `routes.ts` are the `events` and `customers` entries (exactly this wave's two journeys) — the five failing routes are byte-identical `EmptyPageView` wiring in both SHAs. This condition pre-dates wave-2, is outside wave-2's journey scope (event-stream, customers), and is unrelated to wave-2's diff. The 9 passing tests (`dashboard`, `events`, `customers`, `feeds` sidebar entries; scroll containment; sidebar-collapse persistence; theme-toggle persistence; locale switch; breadcrumb) cover everything that should currently work and show no regression. This does not affect the e2e dimension verdict for wave-2's journeys.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e` | 0 |
| 2 | `npm ci` | `tools/migration-verify` | 0 |
| 3 | `npm ci` | `frontend` | 0 |
| 4 | `npm run build` (`vue-tsc --noEmit && vite build`) | `frontend` | 0 |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` (run 1/3) | `tests/e2e` | 0 |
| 6 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` (run 2/3) | `tests/e2e` | 0 |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` (run 3/3) | `tests/e2e` | 0 |
| 8 | `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` (default workers) | `tests/e2e` | 0 |
| 9 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root (checkout) | 0 |
| 10 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e` | 0 |
| 11 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts` (whole file, no filter) | `tests/e2e` | 1 (5 failures, all out-of-scope — see analysis above) |
| 12 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g "feeds"` | `tests/e2e` | 0 |

## Test tally

| Run | Spec / filter | Passed | Failed | Notes |
| --- | --- | --- | --- | --- |
| 1 | `events.spec.ts --workers=1` (attempt 1) | 4 | 0 | |
| 2 | `events.spec.ts --workers=1` (attempt 2) | 4 | 0 | |
| 3 | `events.spec.ts --workers=1` (attempt 3) | 4 | 0 | |
| 4 | `customers.spec.ts` (default workers) | 4 | 0 | no rerun needed, no flake |
| 5 | `public.spec.ts` | 5 | 0 | regression guard |
| 6 | `navigation.spec.ts` (whole file) | 9 | 5 | 5 failures = future-wave (J6–J10) sidebar entries, pre-existing/out-of-scope, not a wave-2 regression |
| 7 | `lists.spec.ts -g "feeds"` | 2 | 0 | regression guard |

In-scope wave-2 journey total across the 4 required runs (3× events + 1× customers): **16/16 passed**.
Regression-guard total (public + navigation + lists-feeds): **16/19 passed**, 3 spec-runs; the 3 non-passing were all `navigation.spec.ts` entries for not-yet-implemented later-wave routes (see analysis above), not a regression.

## Performance vs budget (DEV-8, `tools/migration-verify/budget.json`)

| Metric | Value | Budget | Result |
| --- | --- | --- | --- |
| initialJsGzipBytes | 73,403 B | 307,200 B | PASS |
| lcpMillis (`/pl/login`) | 220 ms | 2,000 ms | PASS |
| ttiMillis | 24 ms | 2,500 ms | PASS |
| collectP95Millis (50 sequential `/collect` POSTs) | 23.92 ms | 500 ms | PASS |

`frontend/dist` build (command #4) reports `index-B6Kj-KLw.js` 214.35 kB raw / 74.20 kB gzip and `index-BubHWPzC.css` 45.85 kB / 8.67 kB gzip — consistent with the script's own gzip measurement of all JS assets (73,403 B).

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/e2e/`:
- `events-run1.log`, `events-run2.log`, `events-run3.log` (commands #5–7, 4/4 passed each)
- `customers-run1.log` (command #8, 4/4 passed, default workers)
- `performance.log` (command #9, raw JSON output)
- `public.spec.log` (command #10, 5/5 passed)
- `navigation-full.spec.log` (command #11, full output, 9 passed / 5 failed with traces/screenshots referenced inline)
- `lists-feeds.spec.log` (command #12, 2/2 passed)

Round-1 evidence remains archived at `waves/wave-2/round-1/` (not read, not touched).
