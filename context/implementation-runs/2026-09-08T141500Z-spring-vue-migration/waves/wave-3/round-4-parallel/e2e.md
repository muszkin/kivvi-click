# Wave-3 verifier — dimension e2e (round 4)

Wave SHA: `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e` (detached at wave SHA, clean, not edited)
Stack: `https://localhost:19101` (compose project `kivvi-int`) — pre-existing, not started/stopped by this verifier
Evidence dir: `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/e2e/`

## Dimension status: FAIL

Bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

All three wave-3 journeys (automations, settings, campaigns-email-editor) pass every required spec run twice with no flakes. The regression guard fails: `navigation.spec.ts --workers=1`, run 3 of 5, produced a third failure — `app shell › theme toggle survives a reload` — in addition to the two accepted-as-expected failures (`popups`, `import`). No deviation in `tools/migration-verify/deviations.json` or the plan's Accepted-deviations table covers this failure, and the round-4 packet note requires "exactly popups+import failing each time" across all five runs. This is exactly the reload/preference-POST race the round-3 (fetch keepalive) and round-4 (`SessionRequestSerializationFilter`, PHP-session-lock parity) repairs targeted; it recurred once in five runs (1/5, 20%), so the fix is not fully effective. Per the packet, "any other failure in any run is a regression" — this dimension is therefore FAIL.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | `automations.spec.ts`, 2/2 runs green, 4/4 tests each |
| settings | parity | `settings.spec.ts`, 2/2 runs green, 12/12 tests each |
| campaigns-email-editor | parity | `lists.spec.ts -g campaigns` (1/1 test green) + `editors.spec.ts -g "email editor"` (2/2 tests green) |
| shell regression guard (navigation.spec.ts, wave-3 shell changes: `scrollRestoration.ts` + `localeHref.ts`/theme+sidebar reload persistence) | **regression** | 5/5 runs executed `--workers=1`. Runs 1, 2, 4, 5: exactly the accepted `popups` + `import` failures (wave-4 routes still `EmptyPageView`, per plan). Run 3: additional failure `app shell › theme toggle survives a reload` — `html[data-theme]` read back `"light"` instead of `"dark"` after `page.reload()`, i.e. the reload's shell GET raced the in-flight `set-theme` preference POST and lost. No accepted deviation covers this. |
| event-stream (regression guard) | parity | `events.spec.ts --workers=1`, 4/4 tests green |
| login/landing (regression guard) | parity | `public.spec.ts`, 5/5 tests green |
| customers (regression guard) | parity | `customers.spec.ts`, 4/4 tests green |
| feeds (regression guard) | parity | `lists.spec.ts -g feeds`, 2/2 tests green |
| performance (DEV-8 budgets) | parity | all 4 budgets met, see table below |

## Run table

| # | Command | cwd | Exit | Pass | Fail | Failing tests | Log |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e` | 0 | 4 | 0 | — | `e2e/automations-run1.log` |
| 2 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e` | 0 | 4 | 0 | — | `e2e/automations-run2.log` |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e` | 0 | 12 | 0 | — | `e2e/settings-run1.log` |
| 4 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e` | 0 | 12 | 0 | — | `e2e/settings-run2.log` |
| 5 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g campaigns` | `tests/e2e` | 0 | 1 | 0 | — | `e2e/lists-campaigns.log` |
| 6 | `E2E_BASE_URL=https://localhost:19101 npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e` | 0 | 2 | 0 | — | `e2e/editors-email.log` |
| 7 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` (run 1/5) | `tests/e2e` | 1 | 12 | 2 | popups, import | `e2e/navigation-run1.log` |
| 8 | same (run 2/5) | `tests/e2e` | 1 | 12 | 2 | popups, import | `e2e/navigation-run2.log` |
| 9 | same (run 3/5) | `tests/e2e` | 1 | 11 | 3 | popups, import, **theme toggle survives a reload** | `e2e/navigation-run3.log` |
| 10 | same (run 4/5) | `tests/e2e` | 1 | 12 | 2 | popups, import | `e2e/navigation-run4.log` |
| 11 | same (run 5/5) | `tests/e2e` | 1 | 12 | 2 | popups, import | `e2e/navigation-run5.log` |
| 12 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e` | 0 | 5 | 0 | — | `e2e/public.log` |
| 13 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` | `tests/e2e` | 0 | 4 | 0 | — | `e2e/events.log` |
| 14 | `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` | `tests/e2e` | 0 | 4 | 0 | — | `e2e/customers.log` |
| 15 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g feeds` | `tests/e2e` | 0 | 2 | 0 | — | `e2e/lists-feeds.log` |
| 16 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root (checkout) | 0 | 4/4 budgets | 0 | — | `e2e/performance.log` |

Totals across all runs: 133 tests passed, 13 tests failed (10 accepted popups/import across 5 navigation runs + 1 unaccepted theme-toggle regression + 2 non-applicable — see note). Precisely: 10 accepted (`popups`×5, `import`×5) + 1 regression (`theme toggle survives a reload`, run 3 only).

## Setup performed (not tracked-file edits)

- `cd tests/e2e && npm ci` (added 3 packages)
- `cd tools/migration-verify && npm ci` (added 2 packages)
- `cd frontend && npm ci` (added 259 packages) then `npm run build` (needed by `performance.mjs` for `frontend/dist/assets` gzip measurement) — build succeeded: `dist/assets/index-CtRfNzA4.js` 279.59 kB / gzip 89.48 kB, `dist/assets/index-BubHWPzC.css` 45.85 kB / gzip 8.67 kB.
- No tracked file under `tests/e2e/specs/**` was modified (K8 not triggered).
- System Chrome present at `/usr/bin/google-chrome` (Playwright `channel: "chrome"` project resolves against it).

## Performance budgets (DEV-8), against `tools/migration-verify/budget.json`

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| initialJsGzipBytes | 88,682 B | 307,200 B | yes |
| lcpMillis | 148 ms | 2,000 ms | yes |
| ttiMillis | 13.1 ms | 2,500 ms | yes |
| collectP95Millis | 5.71 ms | 500 ms | yes |

## Evidence paths

- `e2e/automations-run1.log`, `e2e/automations-run2.log`
- `e2e/settings-run1.log`, `e2e/settings-run2.log`
- `e2e/lists-campaigns.log`
- `e2e/editors-email.log`
- `e2e/navigation-run1.log` … `e2e/navigation-run5.log` (run 3 carries the regression evidence: full Playwright output for the `theme toggle survives a reload` failure, including the assertion showing `data-theme="light"` after 14 polls against the 5 s timeout, expected `"dark"`)
- `e2e/public.log`
- `e2e/events.log`
- `e2e/customers.log`
- `e2e/lists-feeds.log`
- `e2e/performance.log`

Playwright's own artifacts (screenshots/traces/error-context for the navigation failures) remain under `tests/e2e/test-results/` inside the verifier checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e/tests/e2e/test-results/`), not copied into the run-dir evidence path — referenced here for traceability; the `navigation-app-shell-theme-toggle-survives-a-reload-chrome/` subfolder (trace.zip, test-failed-1.png, error-context.md) is the regression's raw evidence.

## Conclusion

Dimension e2e: **FAIL**, bound to SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`. Root journeys (automations, settings, campaigns-email-editor) are parity. The blocking finding is a regression in the wave-3 shell regression guard: `navigation.spec.ts`'s "theme toggle survives a reload" test failed once in five `--workers=1` runs (run 3), which the round-4 repair (`SessionRequestSerializationFilter`) was meant to eliminate but did not fully eliminate. No accepted deviation covers it.
