# Verifier report — wave wave-2, dimension e2e

Wave SHA: `22d7fcb6380723728a33fc21fda22a92594a2e88` (checkout HEAD confirmed detached at this SHA, clean working tree).
Stack: https://localhost:19101 (compose project kivvi-int) — reachable (HTTP 200) throughout.

## Dimension status: **FAIL**

## Per-journey verdicts

- **event-stream**: **regression** (no deviation id covers it). Kill-criterion K6 requires `events.spec.ts` at 4/4 in all three consecutive runs; observed 4/4, 4/4, 2/4 across runs 1–3 (run 1 also had 1 failure). Root-cause read (evidence only, not a fix): `#event-stream .event-row` count assertions of exactly 30 (lines 12 and 41) are not resilient to the same file's other tests (`a collected event…`, `a replayed event…`) concurrently POSTing to `/collect` and streaming a live row into the same shared Mercure topic while Playwright's `fullyParallel: true` runs all four tests in the file at once. `MAX_ROWS` in the ported composable (`frontend/src/composables/useEventStream.ts:24`) is 80 — identical to the pre-migration `assets/controllers/event-stream.ts:9` (`MAX_ROWS = 80`) — so this is not a behavioural delta introduced by the port; it is a pre-existing race in the (unchanged) spec file that the wave's default parallel execution exposes. It still fails the packet's literal 4/4×3 requirement with no deviation id on file, so it is reported as FAIL/regression, not accepted-deviation.
- **customers**: **parity**. `customers.spec.ts` 4/4 passed.

## Test list and results per run

| Command | Result |
|---|---|
| `events.spec.ts` run 1 | 3/4 passed — FAIL (`a collected event reaches the stream as a rendered row`: expected 30 rows, got 31) |
| `events.spec.ts` run 2 | 4/4 passed |
| `events.spec.ts` run 3 | 2/4 passed — FAIL (`renders thirty rows…`: 31 rows; `a collected event…`: 31 rows) |
| `customers.spec.ts` | 4/4 passed |
| `public.spec.ts` | 5/5 passed |
| `lists.spec.ts -g "product feeds"` | 2/2 passed |
| `navigation.spec.ts -g "sidebar collapse\|theme toggle"` | 2/2 passed |
| `navigation.spec.ts -g 'sidebar entry "events" opens its page\|sidebar entry "customers" opens its page'` | 2/2 passed |

## Performance (DEV-8, `budget.json` vs `performance.mjs --base https://localhost:19101`)

| Metric | Value | Budget | Pass |
|---|---|---|---|
| initialJsGzipBytes | 73,403 B | 307,200 B | yes |
| lcpMillis | 232 | 2000 | yes |
| ttiMillis | 35.3 | 2500 | yes |
| collectP95Millis (50 requests) | 11.5 | 500 | yes |

All budgets within threshold; command exit code 0.

## Commands run (cwd, exit code)

1. `npm ci` — cwd `tests/e2e` — exit 0
2. `npm ci` — cwd `tools/migration-verify` — exit 0
3. `npm ci` — cwd `frontend` — exit 0
4. `npm run build` — cwd `frontend` — exit 0 (vue-tsc --noEmit && vite build)
5. `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts` ×3 — cwd `tests/e2e` — exit 1, 0, 1
6. `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` — cwd `tests/e2e` — exit 0
7. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` — cwd `tests/e2e` — exit 0
8. `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g "product feeds"` — cwd `tests/e2e` — exit 0
9. `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse|theme toggle"` — cwd `tests/e2e` — exit 0
10. `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g 'sidebar entry "events" opens its page|sidebar entry "customers" opens its page'` — cwd `tests/e2e` — exit 0
11. `node tools/migration-verify/performance.mjs --base https://localhost:19101` — cwd repo root of checkout — exit 0

System Chrome used throughout (`channel: "chrome"`, `/usr/bin/google-chrome-stable`), headless, per `playwright.config.ts`.

## Evidence paths

- `waves/wave-2/e2e/events-run-1/output.log`, `report.json`, `test-results/` (failed-test screenshot + trace.zip + error-context.md)
- `waves/wave-2/e2e/events-run-2/output.log`, `report.json`, `test-results/`
- `waves/wave-2/e2e/events-run-3/output.log`, `report.json`, `test-results/` (2 failed-test screenshots + traces + error-context.md)
- `waves/wave-2/e2e/customers/output.log`, `report.json`, `test-results/`
- `waves/wave-2/e2e/regression/public-output.log`, `.json`
- `waves/wave-2/e2e/regression/lists-feeds-output.log`, `.json`
- `waves/wave-2/e2e/regression/nav-collapse-theme-output.log`, `.json`
- `waves/wave-2/e2e/regression/nav-entries-output.log`, `.json`
- `waves/wave-2/e2e/performance/output.log`
