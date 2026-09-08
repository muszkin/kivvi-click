# Verifier summary — wave-1, dimension e2e

Wave SHA: `9427fdb1d92e4e606e67471638031d1bc0fb73d4` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-1-e2e`, HEAD matches, working tree clean). Stack: `https://localhost:19101` (compose project `kivvi-int`), all three containers `Up ... (healthy)`.

## Dimension status: PASS

## Per-journey verdicts

- **landing** (B22 landing part, B01 landing row) — parity. `public.spec.ts` full file: 5/5 passed.
- **feeds** (B01 feeds row, B30) — parity. `lists.spec.ts -g "product feeds"`: 2/2 passed.
- **scheduler-heartbeat** (B33; B20 out of scope for e2e, owned by integration) — parity. No browser spec (static journey per packet); evidence is the `HeartbeatJob` log line below.
- Wave-0 regression checks (not re-scored, run as guard per packet) — parity. `navigation.spec.ts -g "sidebar collapse|theme toggle"`: 2/2 passed. `navigation.spec.ts -g 'sidebar entry "feeds"'`: 1/1 passed (title confirmed verbatim in spec: `sidebar entry "feeds" opens its page`).

No regressions, no accepted-deviation invocations needed — every in-scope test passed.

## Commands run

Chrome: system `google-chrome`/`google-chrome-stable` (`/usr/bin/google-chrome`), headless via Playwright `channel: "chrome"` config in `playwright.config.ts`.

| Command | cwd | Exit |
|---|---|---|
| `npm ci` | `tests/e2e` | 0 |
| `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e` | 0 (5 passed) |
| `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g "product feeds"` | `tests/e2e` | 0 (2 passed) |
| `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse\|theme toggle"` | `tests/e2e` | 0 (2 passed) |
| `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g 'sidebar entry "feeds"'` | `tests/e2e` | 0 (1 passed) |
| `docker compose -p kivvi-int logs api` | (n/a, docker CLI) | 0 |
| `npm ci` | `tools/migration-verify` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run build` | `frontend/` | 0 |
| `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root of checkout | 0 |

## Scheduler heartbeat evidence (B33)

`docker compose -p kivvi-int logs api` contains:
```
api-1  | 2026-09-08T19:50:55.261Z  INFO 1 --- [kivvi-click] [eat-scheduler-1] c.k.i.scheduling.HeartbeatJob            : Scheduler heartbeat tick.
```
Matches oracle `behaviours.json` B33 text verbatim ("Scheduler heartbeat tick."). Full log saved at `e2e/scheduler-heartbeat-api.log`.

## Performance vs budget (DEV-8, `tools/migration-verify/budget.json`)

| Metric | Value | Budget | Result |
|---|---|---|---|
| initialJsGzipBytes | 69,006 B | 307,200 B | PASS |
| lcpMillis | 164 | 2000 | PASS |
| ttiMillis | 15 | 2500 | PASS |
| collectP95Millis | skipped | 500 | n/a — script self-notes `/collect does not exist yet (introduced in wave-2)`; not an in-scope endpoint this wave, no budget breach |

Frontend build: `vue-tsc --noEmit && vite build` succeeded, 196.94 kB JS / 69.75 kB gzip single chunk, 45.85 kB CSS.

## Evidence paths (all under `waves/wave-1/e2e/`)

- `public.log`, `public-report/`, `public-artifacts/`
- `lists-feeds.log`, `lists-feeds-report/`, `lists-feeds-artifacts/`
- `navigation-regression.log`, `navigation-regression-report/`, `navigation-regression-artifacts/`
- `navigation-feeds-entry.log`, `navigation-feeds-entry-report/`, `navigation-feeds-entry-artifacts/`
- `scheduler-heartbeat-api.log`
- `tools-migration-verify-npm-ci.log`
- `frontend-npm-ci.log`, `frontend-build.log`
- `performance.log`
