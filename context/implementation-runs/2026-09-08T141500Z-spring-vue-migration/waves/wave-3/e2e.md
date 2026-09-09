# Wave-3 verifier — dimension e2e (round 4, isolated re-run)

- Wave SHA: `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e` (detached at wave SHA, clean tree, untouched)
- Stack: `https://localhost:19101` (compose project `kivvi-int`, edge http `19100`) — pre-existing, not started/stopped by this run; confirmed `kivvi-int-api-1`, `kivvi-int-database-1`, `kivvi-int-mercure-1` all `Up ... (healthy)` before testing, `GET /` → 200
- Isolation: this is the isolated re-run mandated by common-journey-rules.md "Cohort sequencing" — e2e ran alone, no other verifier active on this host during the run
- Journeys of this wave: automations, settings, campaigns-email-editor
- Earlier-wave journeys exercised here as regression guards only: shell/navigation (scrollBehavior + locale toggle, this wave's shell changes), event-stream, landing/login, customers, feeds

## Dimension status: PASS

Bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

All in-scope journey specs green. `navigation.spec.ts --workers=1` run ten times, strictly sequentially, alone on the host: exactly the popups and import-sidebar tests failed in every one of the 10 runs (wave-4 routes are still `EmptyPageView`, expected per packet), zero other failures, zero flakes — the round-4 backend repair (`SessionRequestSerializationFilter`) holds under isolation. All performance budgets met.

## Per-journey verdicts

| Journey | Verdict | Evidence |
| --- | --- | --- |
| automations | parity | `automations.log` — 4/4 passed |
| settings | parity | `settings.log` — 12/12 passed |
| campaigns-email-editor | parity | `lists-campaigns.log` (1/1 passed) + `editors-email-editor.log` (2/2 passed) |

No deviation IDs invoked on this dimension (DEV-4/DEV-7/DEV-12 in scope per packet did not surface as e2e-spec failures or skips in this round).

## Regression guards (earlier-wave journeys, not scored as this wave's journeys)

| Guard | Spec | Verdict | Evidence |
| --- | --- | --- | --- |
| shell/navigation (this wave's shell changes: scrollBehavior + locale toggle) | `navigation.spec.ts --workers=1` × 10 | holds — 0 unexpected failures across 10 runs | `navigation-run-01.log` … `navigation-run-10.log` |
| event-stream | `events.spec.ts --workers=1` | holds — 4/4 passed | `events.log` |
| landing/login | `public.spec.ts` | holds — 5/5 passed | `public.log` |
| customers | `customers.spec.ts` | holds — 4/4 passed | `customers.log` |
| feeds | `lists.spec.ts -g feeds` | holds — 2/2 passed | `lists-feeds.log` |

## Every Playwright run — pass/fail counts and named failures

| # | Command | Exit | Passed | Failed | Named failures |
| --- | --- | --- | --- | --- | --- |
| navigation run 1 | `npx playwright test navigation.spec.ts --workers=1` | 1 | 12 | 2 | `sidebar entry "popups" opens its page`, `sidebar entry "import" opens its page` |
| navigation run 2 | same | 1 | 12 | 2 | same two |
| navigation run 3 | same | 1 | 12 | 2 | same two |
| navigation run 4 | same | 1 | 12 | 2 | same two |
| navigation run 5 | same | 1 | 12 | 2 | same two |
| navigation run 6 | same | 1 | 12 | 2 | same two |
| navigation run 7 | same | 1 | 12 | 2 | same two |
| navigation run 8 | same | 1 | 12 | 2 | same two |
| navigation run 9 | same | 1 | 12 | 2 | same two |
| navigation run 10 | same | 1 | 12 | 2 | same two |
| automations | `npx playwright test automations.spec.ts` | 0 | 4 | 0 | — |
| settings | `npx playwright test settings.spec.ts` | 0 | 12 | 0 | — |
| lists (campaigns) | `npx playwright test lists.spec.ts -g campaigns` | 0 | 1 | 0 | — |
| editors (email editor) | `npx playwright test editors.spec.ts -g "email editor"` | 0 | 2 | 0 | — |
| public | `npx playwright test public.spec.ts` | 0 | 5 | 0 | — |
| events | `npx playwright test events.spec.ts --workers=1` | 0 | 4 | 0 | — |
| customers | `npx playwright test customers.spec.ts` | 0 | 4 | 0 | — |
| lists (feeds) | `npx playwright test lists.spec.ts -g feeds` | 0 | 2 | 0 | — |

Every navigation run failed on exactly and only: `[chrome] › specs/navigation.spec.ts:26:13 › app shell › sidebar entry "popups" opens its page` and `[chrome] › specs/navigation.spec.ts:26:13 › app shell › sidebar entry "import" opens its page` (both: `expect(locator('.page-title')).toContainText(...)` timeout — page never renders because the route is still `EmptyPageView`). No other test failed in any of the 10 runs. This matches the packet's expected-failure set exactly, so all 10 runs are non-regressions.

## Performance (`performance.mjs`)

`node tools/migration-verify/performance.mjs --base https://localhost:19101` — exit 0, all four budgets met:

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| initialJsGzipBytes | 88682 | 307200 | true |
| lcpMillis | 160 | 2000 | true |
| ttiMillis | 14.5 | 2500 | true |
| collectP95Millis | 5.05 | 500 | true |

`frontend/dist/assets` was already present in the checkout (built after checkout time, `index-CtRfNzA4.js` + `index-BubHWPzC.css`), so no build was run before the Playwright runs (per packet: build only if `frontend/dist` is missing).

## Commands (cwd, exit code)

Setup:
- `cd /home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e/tests/e2e && npm ci` — exit 0 (3 packages, 0 vulnerabilities)

Playwright (cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e/tests/e2e`, `E2E_BASE_URL=https://localhost:19101`, strictly sequential, one invocation per filter):
1. `npx playwright test navigation.spec.ts --workers=1` × 10 — exit 1 each (2 expected failures each)
2. `npx playwright test automations.spec.ts` — exit 0
3. `npx playwright test settings.spec.ts` — exit 0
4. `npx playwright test lists.spec.ts -g campaigns` — exit 0
5. `npx playwright test editors.spec.ts -g "email editor"` — exit 0
6. `npx playwright test public.spec.ts` — exit 0
7. `npx playwright test events.spec.ts --workers=1` — exit 0
8. `npx playwright test customers.spec.ts` — exit 0
9. `npx playwright test lists.spec.ts -g feeds` — exit 0

Performance (cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-3-e2e`):
10. `node tools/migration-verify/performance.mjs --base https://localhost:19101` — exit 0

## Evidence paths

All under `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/e2e/`:

- `navigation-run-01.log` … `navigation-run-10.log`
- `automations.log`
- `settings.log`
- `lists-campaigns.log`
- `editors-email-editor.log`
- `public.log`
- `events.log`
- `customers.log`
- `lists-feeds.log`
- `performance.log`

Playwright trace/screenshot attachments for the two expected `navigation.spec.ts` failures remain in the checkout's own `tests/e2e/test-results/` (not copied into the run-dir evidence path — packet did not ask for artifact copying beyond logs, and the checkout is not to be otherwise edited).

## Notes / anomalies

- No unexpected failures anywhere. No flakes across 10 identical navigation.spec.ts runs (0/10 anomalous).
- Round-1..3 evidence under `waves/wave-3/round-1..3/` and the parallel round-4 evidence under `waves/wave-3/round-4-parallel/` were not read, per instruction.
- Stack was not started or stopped by this verifier.
