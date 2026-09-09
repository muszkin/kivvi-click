# Wave-3 verifier report — dimension e2e (round 1)

**Wave SHA:** `32a68311594e611aaa8eaa0803bc72bba7d735a9`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e` (detached at wave SHA, verified clean, untouched)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`), already running, healthy (`api`, `database`, `mercure` all `Up ... (healthy)`); not started or stopped by this verifier.

## Dimension status: **PASS**

All journey specs for this wave (automations, settings, campaigns-email-editor) pass green on
both required runs. Regression guards for earlier-wave journeys and the wave-3 shell changes
(router `scrollBehavior`, locale toggle via `route.meta.defaultParams`) pass, with exactly the
two pre-authorized `navigation.spec.ts` failures (popups, import — wave-4 routes still
`EmptyPageView`) and no other failure. Performance budgets (DEV-8) all pass.

## Per-journey verdicts

| Journey | Verdict | Evidence |
| --- | --- | --- |
| automations | parity | `e2e/run-01-automations.txt`, `e2e/run-02-automations.txt` — 4/4 passed both runs |
| settings | parity | `e2e/run-03-settings.txt`, `e2e/run-04-settings.txt` — 12/12 passed both runs |
| campaigns-email-editor | parity | `e2e/run-05-lists-campaigns.txt` (1/1 passed), `e2e/run-06-editors-email.txt` (2/2 passed) |

No accepted-deviation or regression verdicts were needed for this dimension's own journeys.
DEV-4, DEV-7, DEV-12 are recorded in `tools/migration-verify/deviations.json` with
`"dimension": "contract"` (DEV-8 is `"dimension": "performance"`) — they govern the contract
and performance dimensions' masks, not e2e's pass/fail; none of them manifested as an e2e
assertion failure in this run.

## Regression guards (earlier-wave journeys + wave-3 shell changes)

| Guard | Verdict | Notes |
| --- | --- | --- |
| navigation (shell: scroll restoration, locale toggle) | pass (2 pre-authorized failures) | `sidebar entry "popups"` and `sidebar entry "import"` fail both runs — identical failure set both times, matching the packet's explicit exception (wave-4 routes still `EmptyPageView`). No other test in the file failed. |
| events (event-stream) | pass | 4/4, `--workers=1` |
| public (landing/login) | pass | 5/5 |
| customers | pass | 4/4 |
| lists -g feeds (product feeds) | pass | 2/2 |
| performance.mjs budgets (DEV-8) | pass | all 4 budgets under threshold (see table below) |

## Runs table

| # | Command | cwd | Exit | Result |
| --- | --- | --- | --- | --- |
| 1 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e` | 0 | 4 passed, 0 failed |
| 2 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e` | 0 | 4 passed, 0 failed |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e` | 0 | 12 passed, 0 failed |
| 4 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e` | 0 | 12 passed, 0 failed |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g campaigns` | `tests/e2e` | 0 | 1 passed, 0 failed |
| 6 | `E2E_BASE_URL=https://localhost:19101 npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e` | 0 | 2 passed, 0 failed |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e` | 1 | 12 passed, 2 failed (popups, import — expected) |
| 8 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e` | 1 | 12 passed, 2 failed (popups, import — expected, identical set) |
| 9 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e` | 0 | 5 passed, 0 failed |
| 10 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` | `tests/e2e` | 0 | 4 passed, 0 failed |
| 11 | `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` | `tests/e2e` | 0 | 4 passed, 0 failed |
| 12 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g feeds` | `tests/e2e` | 0 | 2 passed, 0 failed |
| 13 | `npm ci` | `tests/e2e` | 0 | dependencies installed (3 packages; playwright/playwright-core/@playwright already vendored) |
| 13b | `npm ci` | `frontend` | 0 | 259 packages installed |
| 13c | `npm run build` (`vue-tsc --noEmit && vite build`) | `frontend` | 0 | built `dist/assets` (needed by performance.mjs's JS-budget check) |
| 14 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root of checkout | 0 | all 4 budgets pass (see below) |

### Performance budgets (run 14, budget.json)

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| initialJsGzipBytes | 88,666 B | 307,200 B | yes |
| lcpMillis | 156 ms | 2000 ms | yes |
| ttiMillis | 19 ms | 2500 ms | yes |
| collectP95Millis | 8.74 ms | 500 ms | yes |

## Evidence paths

All under `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/e2e/`:

- `run-01-automations.txt`, `run-02-automations.txt`
- `run-03-settings.txt`, `run-04-settings.txt`
- `run-05-lists-campaigns.txt`
- `run-06-editors-email.txt`
- `run-07-navigation.txt`, `run-08-navigation.txt`
- `run-09-public.txt`
- `run-10-events.txt`
- `run-11-customers.txt`
- `run-12-lists-feeds.txt`
- `run-13-frontend-build.txt`
- `run-14-performance.txt`

## Notes for the orchestrator

- Specs under `tests/e2e/specs/` were confirmed unchanged (`git status --porcelain tests/e2e/`
  empty) before and after this run — the oracle's contract held.
- This run generated `/collect` traffic (`events.spec.ts`, `performance.mjs`'s `collectP95`
  probe) against the shared `kivvi-int` stack. Per the plan's contract-dimension isolation rule
  (common-journey-rules.md and this wave's note), the contract dimension must run alone on this
  stack — sequence it after this e2e run (and after visual) finishes, or its `db.json` deltas
  will be contaminated by this traffic.
- Stack was left running (not started/stopped by this verifier), per instructions.
- `frontend/dist/` and `frontend/node_modules/`, and `tests/e2e/node_modules/`, were created in
  this verifier's own checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e`) only, to
  satisfy `npm ci` / `npm run build` prerequisites for `performance.mjs`. No tracked file was
  edited.
