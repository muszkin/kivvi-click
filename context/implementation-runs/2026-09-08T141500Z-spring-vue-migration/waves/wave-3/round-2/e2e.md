# Wave-3 verifier — dimension e2e (round 2)

- Wave SHA: `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e` (detached at wave SHA, confirmed clean, untouched)
- Stack: `https://localhost:19101`, compose project `kivvi-int` (`compose.next.yaml`), started by the orchestrator, healthy throughout, not started/stopped by this verifier
- Node: v26.8.1 (host). `npm ci` run in `tests/e2e/`, `frontend/`, `tools/migration-verify/` of this checkout only.
- Frontend built once for `performance.mjs` (`frontend/dist` did not exist): `npm run build` in `frontend/` → `vue-tsc --noEmit && vite build`, succeeded, `dist/assets/index-*.js` 279.57 kB / 89.47 kB gzip.

## Dimension status: **FAIL**

Reason: the wave-3 journeys' own specs (automations, settings, campaigns, email editor) are fully green, but the mandated `navigation.spec.ts --workers=1` regression guard shows an intermittent regression beyond the two accepted failures (popups/import). Across 20 sequential (`--workers=1`) executions of the full spec run during this verification (2 recorded as formal evidence + 18 diagnostic repeats used to characterize a suspicious first-pass result), 5 runs (25%) produced one extra failure beyond the accepted "popups"/"import" pair:

- `theme toggle survives a reload` — failed 3 of 20 runs (`data-theme` reverts to `"light"` after `page.reload()` instead of staying `"dark"`)
- `sidebar collapse survives a reload` — failed 1 of 20 runs (same reload-persistence family)
- `sidebar entry "feeds" opens its page` — failed 1 of 20 runs (unrelated navigation entry, transient)

The two officially-logged runs (evidence 07/08 below) happened to land on the clean 2-failure outcome, but the packet's own instruction ("run twice... list every failure by name") is a floor, not a ceiling, for finding an intermittent fault; a diagnostic re-run of the same unchanged spec against the same unchanged stack surfaced the fault reproducibly enough (5/20) to rule out one-off harness noise. This is a regression: no deviation id covers reload-triggered theme/sidebar state loss, and DEV-4/DEV-7/DEV-12 (the only deviations in scope for this wave) do not apply to `navigation.spec.ts`. Raw logs and one full trace/screenshot pair are under `e2e/navigation-flake-investigation/`.

## Per-journey verdicts

| Journey | Verdict | Evidence |
| --- | --- | --- |
| automations (B26) | **parity** — `automations.spec.ts`, 4/4 tests green, 2/2 runs | `e2e/01-automations-run1.txt`, `e2e/02-automations-run2.txt` |
| settings (B06, B32) | **parity** — `settings.spec.ts`, 12/12 tests green, 2/2 runs (8 tabs + tracker snippet + DNS + webhook + notification matrix); DEV-12 (settings step 10 = 404 document) is a contract/visual-dimension deviation, not exercised by these specs | `e2e/03-settings-run1.txt`, `e2e/04-settings-run2.txt` |
| campaigns-email-editor (B27, B28) | **parity** — `lists.spec.ts -g campaigns` 1/1 green; `editors.spec.ts -g "email editor"` 2/2 green; DEV-7 (SPA makes no POST …/blocks) is a contract-dimension deviation, not exercised by these specs | `e2e/05-lists-campaigns.txt`, `e2e/06-editors-email-editor.txt` |

## Regression guards (earlier waves — journeys not in scope this wave)

| Guard | Verdict | Evidence |
| --- | --- | --- |
| navigation (shell: scrollBehavior, reload, locale toggle) | **regression** (intermittent) | `e2e/07-navigation-run1-workers1.txt`, `e2e/08-navigation-run2-workers1.txt` (both clean at 2/14 failed — accepted), `e2e/navigation-flake-investigation/` (18 further runs, 5/20 total show the extra failure; trace+screenshot for one occurrence preserved under `test-results-run-7/`) |
| public / login | **parity** — `public.spec.ts` 5/5 green | `e2e/09-public.txt` |
| events (event-stream) | **parity** — `events.spec.ts --workers=1` 4/4 green | `e2e/10-events-workers1.txt` |
| customers | **parity** — `customers.spec.ts` 4/4 green | `e2e/11-customers.txt` |
| feeds | **parity** — `lists.spec.ts -g feeds` 2/2 green | `e2e/12-lists-feeds.txt` |
| performance (DEV-8 budgets) | **parity** — all 4 budgets met (run twice, both green) | `e2e/13-performance.txt` (+ ad-hoc console run at start of session, identical verdict) |

## Commands run (cwd for every `playwright`/`npm` command: `tests/e2e/` inside the checkout above; `performance.mjs` and `npm ci`/`npm run build` under `frontend/` and `tools/migration-verify/` as noted)

| # | Command | cwd | Exit | Result | Evidence |
| --- | --- | --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e/` | 0 | 3 packages installed | — |
| 2 | `npm ci` | `frontend/` | 0 | 259 packages installed | — |
| 3 | `npm run build` | `frontend/` | 0 | `dist/` built | — |
| 4 | `npm ci` | `tools/migration-verify/` | 0 | 2 packages installed | — |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/automations.spec.ts` (run 1) | `tests/e2e/` | 0 | 4 passed | `e2e/01-automations-run1.txt` |
| 6 | same (run 2) | `tests/e2e/` | 0 | 4 passed | `e2e/02-automations-run2.txt` |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/settings.spec.ts` (run 1) | `tests/e2e/` | 0 | 12 passed | `e2e/03-settings-run1.txt` |
| 8 | same (run 2) | `tests/e2e/` | 0 | 12 passed | `e2e/04-settings-run2.txt` |
| 9 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/lists.spec.ts -g campaigns` | `tests/e2e/` | 0 | 1 passed | `e2e/05-lists-campaigns.txt` |
| 10 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/editors.spec.ts -g "email editor"` | `tests/e2e/` | 0 | 2 passed | `e2e/06-editors-email-editor.txt` |
| 11 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/navigation.spec.ts --workers=1` (formal run 1) | `tests/e2e/` | 1 | 12 passed, 2 failed (popups, import — accepted) | `e2e/07-navigation-run1-workers1.txt` |
| 12 | same (formal run 2) | `tests/e2e/` | 1 | 12 passed, 2 failed (popups, import — accepted) | `e2e/08-navigation-run2-workers1.txt` |
| 13 | same, ×4 diagnostic repeats | `tests/e2e/` | 1 (×4) | 2 runs clean (2 failed, accepted); 1 run 3 failed (+ theme toggle); 1 run 4 failed (+ sidebar collapse, + theme toggle) | inline in session log; not separately filed |
| 14 | same, ×6 diagnostic repeats (background batch) | `tests/e2e/` | 1 (×6) | 5 runs clean; 1 run 3 failed (+ "feeds" sidebar entry) | `e2e/navigation-flake-investigation/full-log.txt` |
| 15 | same, ×8 diagnostic repeats (background batch, evidence-preserving) | `tests/e2e/` | 1 (×8) | 7 runs clean; 1 run 3 failed (+ theme toggle), trace+screenshot preserved | `e2e/navigation-flake-investigation/run-1.txt` … `run-8.txt`, `test-results-run-7/` |
| 16 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/public.spec.ts` | `tests/e2e/` | 0 | 5 passed | `e2e/09-public.txt` |
| 17 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/events.spec.ts --workers=1` | `tests/e2e/` | 0 | 4 passed | `e2e/10-events-workers1.txt` |
| 18 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/customers.spec.ts` | `tests/e2e/` | 0 | 4 passed | `e2e/11-customers.txt` |
| 19 | `E2E_BASE_URL=https://localhost:19101 npx playwright test specs/lists.spec.ts -g feeds` | `tests/e2e/` | 0 | 2 passed | `e2e/12-lists-feeds.txt` |
| 20 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` (×2) | repo root of checkout | 0 (×2) | all 4 budgets pass both times | `e2e/13-performance.txt` |

## Totals

| Suite | Tests | Pass | Fail |
| --- | --- | --- | --- |
| automations.spec.ts (×2) | 4 each | 8 | 0 |
| settings.spec.ts (×2) | 12 each | 24 | 0 |
| lists.spec.ts -g campaigns | 1 | 1 | 0 |
| editors.spec.ts -g "email editor" | 2 | 2 | 0 |
| navigation.spec.ts --workers=1 (20 total runs, 14 tests each) | 14 each | avg 12.25/14 | 2/14 baseline (accepted), extra failure in 5/20 runs |
| public.spec.ts | 5 | 5 | 0 |
| events.spec.ts --workers=1 | 4 | 4 | 0 |
| customers.spec.ts | 4 | 4 | 0 |
| lists.spec.ts -g feeds | 2 | 2 | 0 |
| performance.mjs (×2) | 4 budgets each | 8 | 0 |

## Evidence paths

- `e2e/01-automations-run1.txt` … `e2e/13-performance.txt` — raw Playwright/Node output for every command above.
- `e2e/navigation-flake-investigation/` — 18 extra `navigation.spec.ts --workers=1` runs used to characterize the intermittent regression, plus `test-results-run-7/` (screenshot + trace.zip + error-context.md for one `theme toggle survives a reload` failure).
- No files outside `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/e2e/` and this summary file were written by this verifier. No tracked file in the checkout was edited (`git status` confirmed clean before and after).
