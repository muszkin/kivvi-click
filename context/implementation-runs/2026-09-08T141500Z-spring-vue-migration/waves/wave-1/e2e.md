# Verifier — wave wave-1, dimension e2e (round 2)

**Wave SHA:** `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-1-e2e` (HEAD == wave SHA, clean working tree — verified before any command)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`, containers `kivvi-int-api-1`/`kivvi-int-database-1`/`kivvi-int-mercure-1`, all healthy) — not started/stopped by this verifier
**Browser:** system Chrome (`/usr/bin/google-chrome`) via Playwright `channel: "chrome"`, headless (project config default, no override)

## Dimension status: **PASS**

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| landing | parity |
| feeds | parity |
| scheduler-heartbeat | parity |

- landing: all 5 tests in `public.spec.ts` pass (3 `landing` describe + 2 `login` describe, unchanged from wave-0 walking skeleton).
- feeds: both tests in `lists.spec.ts`'s `product feeds` describe block pass; the feeds sidebar entry (`navigation.spec.ts`) opens its page.
- scheduler-heartbeat: no browser spec (static journey per packet). B33 evidence is the `Scheduler heartbeat tick.` INFO line from `HeartbeatJob` in the `api` container log (1 tick observed in the current log window — see `heartbeat-tick.log`).

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e` | 0 |
| 2 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts --reporter=list` | `tests/e2e` | 0 |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g "product feeds" --reporter=list` | `tests/e2e` | 0 |
| 4 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse\|theme toggle" --reporter=list` | `tests/e2e` | 0 |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g 'sidebar entry "feeds" opens its page' --reporter=list` | `tests/e2e` | 0 |
| 6 | `docker compose -p kivvi-int logs api \| grep -i heartbeat` | (host) | 0 |
| 7 | `npm ci` | `tools/migration-verify` | 0 |
| 8 | `npm ci` | `frontend` | 0 |
| 9 | `npm run build` (`vue-tsc --noEmit && vite build`) | `frontend` | 0 |
| 10 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root (checkout) | 0 |

## Test list and results

**public.spec.ts (invocation #2, full file, 5/5 expected):**
- `landing › hero, preview frame, features, steps and pricing` — PASS (1.2s)
- `landing › the demo button lands in the panel` — PASS (1.3s)
- `landing › English landing keeps the same structure` — PASS (1.1s)
- `login › signs in and shows the identity in the sidebar` — PASS (1.4s)
- `login › the browser blocks a malformed address before it is sent` — PASS (1.5s)
- Result: **5 passed**

**lists.spec.ts -g "product feeds" (invocation #3):**
- `product feeds › sources, connected feeds and the failing one` — PASS (761ms)
- `product feeds › the matching diagnostic explains the coverage` — PASS (797ms)
- Result: **2 passed**

**navigation.spec.ts -g "sidebar collapse|theme toggle" (invocation #4):**
- `app shell › sidebar collapse survives a reload` — PASS (624ms)
- `app shell › theme toggle survives a reload` — PASS (604ms)
- Result: **2 passed**

**navigation.spec.ts -g 'sidebar entry "feeds" opens its page' (invocation #5, exact title):**
- `app shell › sidebar entry "feeds" opens its page` — PASS (573ms)
- Result: **1 passed**

**Heartbeat evidence (invocation #6):**
- `2026-09-08T20:12:39.279Z  INFO 1 --- [kivvi-click] [eat-scheduler-1] c.k.i.scheduling.HeartbeatJob : Scheduler heartbeat tick.` — B33 tick observed.

All in-scope tests: **10/10 passed**, 0 failures.

## Performance vs budget (DEV-8, `tools/migration-verify/budget.json`)

| Metric | Value | Budget | Result |
| --- | --- | --- | --- |
| initialJsGzipBytes | 69,006 B | 307,200 B | PASS |
| lcpMillis (`/pl/login`) | 160 ms | 2,000 ms | PASS |
| ttiMillis | 14.7 ms | 2,500 ms | PASS |
| collectP95Millis | skipped — `/collect` not introduced until wave-2 (script's own note) | 500 ms | N/A, not in scope |

`frontend/dist` build (invocation #9) also independently reports 196.94 kB raw / 69.75 kB gzip for `index-*.js`, consistent with the script's gzip measurement across all JS assets (69,006 B). No budget exceeded; no deviation id needed beyond DEV-8 itself (which governs the budget mechanism, not an overage).

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/e2e/`:
- `npm-ci-e2e.log`
- `public.spec.log` (invocation #2, full run, 5 passed)
- `lists.spec.log` (invocation #3, product feeds, 2 passed)
- `navigation-collapse-theme.spec.log` (invocation #4, 2 passed)
- `navigation-feeds-nav.spec.log` (invocation #5, 1 passed)
- `heartbeat-tick.log` (invocation #6, B33 evidence)
- `npm-ci-migration-verify.log`
- `npm-ci-frontend.log`
- `npm-build-frontend.log` (invocation #9)
- `performance.log` (invocation #10, raw JSON output)

Round-1 evidence remains archived at `waves/wave-1/round-1/e2e/` (untouched).
