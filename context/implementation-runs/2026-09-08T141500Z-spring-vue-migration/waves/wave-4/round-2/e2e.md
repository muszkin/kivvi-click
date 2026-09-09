# Wave-4 verifier — dimension: e2e (round 2)

**Wave SHA:** `71d884d2ed96a7a7caad18147e76570c9e113d4d`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-4-e2e` (detached at the wave SHA; confirmed clean, `git rev-parse HEAD` matched, `git status` clean, before any command ran — HEAD commit: `test: isolate the traversal IT from the shared /tmp (#import-wizard)`, consistent with the round-2 note)
**Journeys:** popups-widget-editor, import-wizard, dashboard
**Stack:** `https://localhost:19101` (compose project `kivvi-int`; not started/stopped by this verifier — confirmed `Up …(healthy)` for api/database/mercure throughout, `curl -sk https://localhost:19101/` → 200)

**Dimension status: FAIL**, bound to `71d884d2ed96a7a7caad18147e76570c9e113d4d`

Cause: the wave-4 mandatory regression guard `navigation.spec.ts --workers=1`, required to pass 14/14 on **every** one of 5 sequential runs with "ANY failure is a regression", failed on 2 of the 5 runs (run 4 and run 5), each with exactly one failing test — a shell-level preference-persistence race, not a fluke of this verifier's environment (see "Root-cause diagnosis" below). All three wave-4 journeys' own specs, all regression-guard specs for earlier waves, and the performance budgets are otherwise green.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e` | 0 |
| 2 | `npm ci` | `tools/migration-verify` | 0 |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts --workers=1` (run 1/5) | `tests/e2e` | 0 |
| 4 | same (run 2/5) | `tests/e2e` | 0 |
| 5 | same (run 3/5) | `tests/e2e` | 0 |
| 6 | same (run 4/5) | `tests/e2e` | **1** |
| 7 | same (run 5/5) | `tests/e2e` | **1** |
| 8 | `E2E_BASE_URL=https://localhost:19101 npx playwright test dashboard.spec.ts --workers=1` (run 1/2) | `tests/e2e` | 0 |
| 9 | same (run 2/2) | `tests/e2e` | 0 |
| 10 | `E2E_BASE_URL=https://localhost:19101 npx playwright test import.spec.ts` (run 1/2) | `tests/e2e` | 0 |
| 11 | same (run 2/2) | `tests/e2e` | 0 |
| 12 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g widgets` | `tests/e2e` | 0 |
| 13 | `E2E_BASE_URL=https://localhost:19101 npx playwright test editors.spec.ts -g "popup editor"` | `tests/e2e` | 0 |
| 14 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` | `tests/e2e` | 0 |
| 15 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` (invalidated — see process note) | `tests/e2e` | 0 |
| 16 | `E2E_BASE_URL=https://localhost:19101 npx playwright test events.spec.ts --workers=1` (clean re-run, counted) | `tests/e2e` | 0 |
| 17 | `E2E_BASE_URL=https://localhost:19101 npx playwright test customers.spec.ts` | `tests/e2e` | 0 |
| 18 | `E2E_BASE_URL=https://localhost:19101 npx playwright test automations.spec.ts` | `tests/e2e` | 0 |
| 19 | `E2E_BASE_URL=https://localhost:19101 npx playwright test settings.spec.ts` | `tests/e2e` | 0 |
| 20 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g campaigns` | `tests/e2e` | 0 |
| 21 | `E2E_BASE_URL=https://localhost:19101 npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e` | 0 |
| 22 | `E2E_BASE_URL=https://localhost:19101 npx playwright test lists.spec.ts -g feeds` | `tests/e2e` | 0 |
| 23 | `npm ci` | `frontend` | 0 |
| 24 | `npm run build` (needed by performance.mjs for `frontend/dist/assets`) | `frontend` | 0 |
| 25 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | (repo root of checkout) | 0 |

**Process note (self-reported deviation from "strictly sequentially"):** command #15 (`events.spec.ts --workers=1`) was launched concurrently with #14 (`public.spec.ts`) by mistake — both were dispatched in one batch. Both passed (5/5 and 4/4), but since `events.spec.ts` asserts exact row counts on a shared Mercure topic, that result was treated as unreliable evidence and discarded; #16 is a clean, isolated re-run (also 4/4) and is the one counted toward the verdict. Every other command in the table ran strictly one at a time, waited to completion before the next started.

## Per-run results

### navigation.spec.ts --workers=1 (5 required runs — must be 14/14 every time)

| Run | Passed | Failed | Failing test |
| --- | --- | --- | --- |
| 1 | 14 | 0 | — |
| 2 | 14 | 0 | — |
| 3 | 14 | 0 | — |
| 4 | 13 | 1 | `app shell › theme toggle survives a reload` |
| 5 | 13 | 1 | `app shell › sidebar collapse survives a reload` |

Both failures are the identical shape: `expect(locator).toHaveAttribute(...)` on the post-`page.reload()` assertion sees the pre-toggle default value instead of the persisted one (run 4: `data-theme` on `<html>` read back `"light"` instead of `"dark"`; run 5: `data-sidebar` on `.app` read back `"expanded"` instead of `"collapsed"`). Logs: `e2e/logs/navigation-run{1..5}.log`.

### dashboard.spec.ts --workers=1 (2 runs)
6/6 both runs. Logs: `e2e/logs/dashboard-run{1,2}.log`.

### import.spec.ts (2 runs, incl. real file upload)
6/6 both runs. Logs: `e2e/logs/import-run{1,2}.log`.

### Journey specs (single run each)
- `lists.spec.ts -g widgets`: 2/2 — `e2e/logs/lists-widgets.log`
- `editors.spec.ts -g "popup editor"`: 4/4 — `e2e/logs/editors-popup.log`

### Regression guards (earlier waves, single run each)
- `public.spec.ts`: 5/5 (incl. the DEV-11-closed login steps) — `e2e/logs/public.log`
- `events.spec.ts --workers=1`: 4/4 (clean re-run) — `e2e/logs/events.log`
- `customers.spec.ts`: 4/4 — `e2e/logs/customers.log`
- `automations.spec.ts`: 4/4 — `e2e/logs/automations.log`
- `settings.spec.ts`: 12/12 — `e2e/logs/settings.log`
- `lists.spec.ts -g campaigns`: 1/1 — `e2e/logs/lists-campaigns.log`
- `editors.spec.ts -g "email editor"`: 2/2 — `e2e/logs/editors-email.log`
- `lists.spec.ts -g feeds`: 2/2 — `e2e/logs/lists-feeds.log`

### performance.mjs (DEV-8 budgets)
All 4 metrics pass, comfortably inside budget — `e2e/logs/performance.log`:

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| initialJsGzipBytes | 101,976 | 307,200 | yes |
| lcpMillis | 160 | 2,000 | yes |
| ttiMillis | 14.2 | 2,500 | yes |
| collectP95Millis | 5.75 | 500 | yes |

## Root-cause diagnosis of the navigation.spec.ts regression (diagnostic only — this verifier does not fix)

Checked host load at the time of the failing runs to rule out CPU contention as required by the common-journey-rules "cohort sequencing" note (a run overlapping a CPU-heavy verifier is inconclusive and must be re-run in isolation before ruling): `uptime` load average was `3.03 2.10 2.39` on a 16-core host, `top` showed ~98% idle, and no Maven/Testcontainers/other verifier process was running — this verifier ran alone against the stack as instructed. The failures are therefore not attributable to concurrent CPU-heavy work; they were re-observed after the corrective isolation described above for a different spec, and no heavy build ran during any of the 5 navigation runs.

Reading the implementation: `frontend/src/stores/shell.ts` `setTheme`/`setSidebar` update the DOM/state synchronously, then fire `fetch(..., { keepalive: true })` **without the caller awaiting it** (`frontend/src/composables/useIntents.ts` intents are void-fired on purpose, per the in-code comment referencing a wave-3 repair, w3-shell-preferences). `backend/.../infrastructure/session/SessionRequestSerializationFilter.java` serializes requests carrying the same session cookie so a reload's `GET` blocks until an in-flight `POST` on that session finishes (PHP-session-lock parity) — but that lock only helps once the `POST` has reached the server and entered the filter chain. Because the `POST` is fired-and-forgotten from the click handler and `page.reload()` follows almost immediately (the test only awaits the synchronous DOM assertion, not the fetch), there remains a genuine client-side race window: if the reload's document/`shell` `GET` reaches the server before the still-in-flight `POST` does, the lock has nothing to serialize against and the `GET` reads the pre-toggle session value. `keepalive: true` (the wave-3 fix) only stops the browser from **aborting** the POST on unload; it does not make the reload wait for it. This matches the observed 2/5 failure rate under real host/network jitter (this is a busy shared host — `df -h /` shows 94% disk in use with ~40 unrelated containers running) with no CPU starvation involved. No deviation id in `tools/migration-verify/deviations.json` covers this behaviour, and it is not in the packet's "Deviations in scope" list, so it is ruled a regression, not an accepted deviation.

## Verdicts

| Journey / guard | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor (B29) | parity | `lists.spec.ts -g widgets` 2/2, `editors.spec.ts -g "popup editor"` 4/4, both clean single runs |
| import-wizard (B31) | parity | `import.spec.ts` 6/6 × 2 runs, incl. real file upload |
| dashboard (B23) | parity | `dashboard.spec.ts --workers=1` 6/6 × 2 runs |
| app-shell regression guard (`navigation.spec.ts`, wave-4 mandatory 5× gate) | **regression** | 3/5 clean, 2/5 with one failure each (theme-toggle and sidebar-collapse persistence-after-reload); see root-cause diagnosis above |
| earlier-wave regression guards (public, events, customers, automations, settings, campaigns, email editor, feeds) | parity | all green, see per-run results |
| performance budgets (DEV-8) | parity | all 4 metrics inside budget |

**Dimension status: FAIL**, bound to wave SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`. The failure is scoped to the app shell's preference-persistence mechanism (shared infrastructure touched by every route, including all three wave-4 journeys), not to any single journey's own behaviour — but the packet's wave-4 note makes this specific gate an unconditional, zero-tolerance blocker ("no accepted failures"), so it fails the dimension regardless of the three journeys' own clean specs.

## Evidence paths

- All command logs: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/e2e/logs/*.log`
- This summary: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/e2e.md`
- Playwright failure artifacts (screenshots/traces/error-context, retained in the checkout, not copied into the run dir per the "write only to the evidence dir" instruction — paths recorded here for traceability): `/home/muszkin/work/kivvi-click-wt/verify-wave-4-e2e/tests/e2e/test-results/navigation-app-shell-theme-toggle-survives-a-reload-chrome/` and `.../navigation-app-shell-sidebar-collapse-survives-a-reload-chrome/` (both deleted along with `node_modules`/`dist` per the packet's disk-cleanup instruction after this report was written; the console log excerpts above are the durable record).
