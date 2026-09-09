# Wave-3 verifier report — dimension: e2e (round 3)

- Wave SHA: `60296f16250c6ebd25f7b4435c795268772fb6be`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e` (detached at wave SHA, clean before and after — verified via `git status --short` / `git diff --stat`, no tracked-file changes)
- Stack under test: `https://localhost:19101` (compose project `kivvi-int`, edge http 19100) — pre-started by the orchestrator; not started or stopped by this verifier
- Journeys in scope: automations, settings, campaigns-email-editor
- Behaviours in scope: B26 (automations), B06/B32 (settings), B27/B28 (campaigns-email-editor), B01 rows for automations/settings/campaigns/email-editor
- Deviations in scope: DEV-4 (contract dimension only — not exercised here), DEV-7 (contract dimension only), DEV-12 (visual dimension only, settings step 10 = 404 document — not exercised by `settings.spec.ts`)

## Dimension status: **FAIL**, bound to wave SHA `60296f16250c6ebd25f7b4435c795268772fb6be`

The three wave-3 journeys themselves are green (parity) in every run. The dimension is FAILED because the mandatory `navigation.spec.ts --workers=1` × 5 regression-guard requirement was violated once: run 1 of 5 produced a third failure — `app shell › theme toggle survives a reload` — in addition to the two accepted (popups, import sidebar). The round-3 note is explicit that "exactly popups+import" may fail in every run and "any other failure in any run is a regression." This is that case: an intermittent (1/5, ~20%) regression in the round-3 theme/sidebar-preference keepalive-POST repair, not an accepted deviation (no DEV id covers it).

## Per-journey verdicts

| Journey | Spec(s) | Verdict | Evidence |
| --- | --- | --- | --- |
| automations | `automations.spec.ts` (×2 full-file runs) | **parity** — 4/4 passed both runs | `e2e/automations-run1.log`, `e2e/automations-run2.log` |
| settings | `settings.spec.ts` (×2 full-file runs) | **parity** — 12/12 passed both runs | `e2e/settings-run1.log`, `e2e/settings-run2.log` |
| campaigns-email-editor | `lists.spec.ts -g campaigns`, `editors.spec.ts -g "email editor"` | **parity** — 1/1 and 2/2 passed | `e2e/lists-campaigns.log`, `e2e/editors-email-editor.log` |

## Regression guard finding (shell-wide, not scoped to one wave-3 journey)

| Guard | Runs | Result | Verdict |
| --- | --- | --- | --- |
| `navigation.spec.ts --workers=1` (5×, required by round-3 note) | 5 | Run 1: 3 failed (popups, import, **theme toggle survives a reload**) / 11 passed. Runs 2–5: 2 failed (popups, import only) / 12 passed each. | **regression** — `theme toggle survives a reload` is not in the accepted failure set (popups/import sidebar only); it appeared in 1 of 5 runs (intermittent race in the round-3 fetch-keepalive preference-persistence repair: after `page.reload()` the reloaded document still shows `data-theme="light"` instead of the just-set `"dark"`) |

Accepted failures every run (wave-4 routes still `EmptyPageView`, expected per packet): `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page`.

## Other regression guards (all pass, all runs)

| Guard | Result |
| --- | --- |
| `public.spec.ts` | 5/5 passed |
| `events.spec.ts --workers=1` | 4/4 passed |
| `customers.spec.ts` | 4/4 passed |
| `lists.spec.ts -g feeds` | 2/2 passed |

## Performance budgets (`performance.mjs`, budget.json)

All four budgets met against `https://localhost:19101` (frontend built fresh in this checkout — see setup table):

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| initialJsGzipBytes | 88,682 | 307,200 | yes |
| lcpMillis | 176 | 2000 | yes |
| ttiMillis | 22.3 | 2500 | yes |
| collectP95Millis | 7.07 | 500 | yes |

Evidence: `e2e/performance.log`

## Every command run

| # | Command | cwd | Exit | Notes |
| --- | --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e/` | 0 | dependency setup |
| 2 | `npm ci` | `frontend/` | 0 | dependency setup |
| 3 | `npm ci` | `tools/migration-verify/` | 0 | dependency setup |
| 4 | `npm run build` | `frontend/` | 0 | produces `frontend/dist/` for performance.mjs; `vue-tsc --noEmit && vite build`, 229 modules, no TS errors |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e/` | 0 | run 1/2 — 4 passed |
| 6 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e/` | 0 | run 2/2 — 4 passed |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e/` | 0 | run 1/2 — 12 passed |
| 8 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e/` | 0 | run 2/2 — 12 passed |
| 9 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g campaigns` | `tests/e2e/` | 0 | 1 passed |
| 10 | `E2E_BASE_URL=https://localhost:19101 npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e/` | 0 | 2 passed |
| 11 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 | run 1/5 — 11 passed, 3 failed (popups, import, **theme toggle survives a reload**) |
| 12 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 | run 2/5 — 12 passed, 2 failed (popups, import) |
| 13 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 | run 3/5 — 12 passed, 2 failed (popups, import) |
| 14 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 | run 4/5 — 12 passed, 2 failed (popups, import) |
| 15 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 | run 5/5 — 12 passed, 2 failed (popups, import) |
| 16 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e/` | 0 | 5 passed |
| 17 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` | `tests/e2e/` | 0 | 4 passed |
| 18 | `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` | `tests/e2e/` | 0 | 4 passed |
| 19 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g feeds` | `tests/e2e/` | 0 | 2 passed |
| 20 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root of checkout | 0 | all 4 budgets pass |

(exit code 1 on the `navigation.spec.ts` runs reflects the two accepted expected failures every time — Playwright exits non-zero on any failed test, expected or not; run 1 additionally carried the unexpected/regression failure)

## Run-by-run pass/fail counts

| Spec / run | Passed | Failed | Failed test names |
| --- | --- | --- | --- |
| automations run 1 | 4 | 0 | — |
| automations run 2 | 4 | 0 | — |
| settings run 1 | 12 | 0 | — |
| settings run 2 | 12 | 0 | — |
| lists -g campaigns | 1 | 0 | — |
| editors -g "email editor" | 2 | 0 | — |
| navigation run 1/5 | 11 | 3 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page`, `theme toggle survives a reload` |
| navigation run 2/5 | 12 | 2 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page` |
| navigation run 3/5 | 12 | 2 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page` |
| navigation run 4/5 | 12 | 2 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page` |
| navigation run 5/5 | 12 | 2 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page` |
| public | 5 | 0 | — |
| events --workers=1 | 4 | 0 | — |
| customers | 4 | 0 | — |
| lists -g feeds | 2 | 0 | — |

## Evidence paths (all under this wave's e2e evidence dir)

- `automations-run1.log`, `automations-run2.log`
- `settings-run1.log`, `settings-run2.log`
- `lists-campaigns.log`
- `editors-email-editor.log`
- `navigation-run1.log` (contains the full stack trace for the `theme toggle survives a reload` failure: expected `data-theme="dark"` after `page.reload()`, received `"light"`)
- `navigation-run2.log` … `navigation-run5.log`
- `public.log`
- `events.log`
- `customers.log`
- `lists-feeds.log`
- `performance.log`

## Notes for the orchestrator

- The `theme toggle survives a reload` failure is a race, not a deterministic break: 4 of 5 runs were clean at exactly the accepted two failures. Given the round-3 repair note explicitly targeted this exact race (fetch keepalive on the theme/sidebar preference POST so it "survives an immediate reload"), and the packet's own acceptance test was "run it 5× and confirm exactly popups+import fail every time," an intermittent recurrence in 1 of 5 runs is evidence the fix narrows but does not close the race window between the reload and the preference POST completing. This verifier does not attempt a root-cause fix (read-only, contract-bound); recommend a further round-3 repair (e.g., await the preference POST before navigating/reloading, or a `visibilitychange`/`pagehide` flush) and a re-run of this exact 5× gate.
- No other unexpected failures were observed anywhere else in the suite (journey specs run twice each, four independent regression-guard specs, and the performance budget check all green).
- Frontend was built fresh in this checkout (`npm run build` in `frontend/`) solely to produce `dist/assets/*.js` for `performance.mjs`'s gzip-bytes budget; no tracked files were touched by the build (output is gitignored `frontend/dist/`).
- No files outside the packet's evidence dir and this summary were written; the detached checkout remains clean (verified via `git status --short` / `git diff --stat` before and after the run).
