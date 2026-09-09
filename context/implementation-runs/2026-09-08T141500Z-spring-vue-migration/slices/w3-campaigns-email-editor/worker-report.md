# Worker report — w3-campaigns-email-editor (journey J8)

Worktree: `/home/muszkin/work/kivvi-click-wt/w3-campaigns-email-editor`
Branch: `migration/wave-3/campaigns-email-editor`
Parent SHA: `b87a701244f5316e1a53bace7a1e41facdffc277`
**Candidate SHA: `4829252f08bf5375c35d195d41096a214b8df19c`**

```
git log --oneline b87a701..HEAD
4829252 fix: capture response body for full-parity paths reached via a click step (#campaigns-email-editor)
41dd5cb feat: add campaigns index and e-mail template editor (#campaigns-email-editor)
```

`git status --porcelain` is empty. Stack torn down (`down -v`), `kivvi-w-campaigns-api` image removed, build cache pruned. Disk: 6.8 GB free at finish.

## Files changed

Backend:
- `backend/src/main/java/click/kivvi/fixtures/CampaignsFixtures.java` — new. Ported `CampaignCatalog`'s CAMPAIGNS array, status-chip map, block library, variables, document sections, selected-block fixture.
- `backend/src/main/java/click/kivvi/application/CampaignsViewService.java` — new. KPIs/filters/columns/rows for the index; template/blocks/variables/sections/selectedBlock for the editor; all number/percent/money formatting via `domain/Format`.
- `backend/src/main/java/click/kivvi/web/CampaignsController.java` — new. `GET /api/v1/{locale}/campaigns?filter=`, `GET /api/v1/{locale}/emails/{id:new|k\d+}`.
- `backend/src/main/java/click/kivvi/web/dto/CampaignsResponse.java`, `CampaignEmailResponse.java` — new DTOs.
- `backend/src/test/java/click/kivvi/application/CampaignsViewServiceTest.java` — new, 7 unit tests.
- `backend/src/test/java/click/kivvi/web/CampaignsControllerTest.java` — new, 7 sliced (`@WebMvcTest`) tests.
- `backend/src/test/java/click/kivvi/CampaignsApiIT.java` — new, 7 real-HTTP/Testcontainers tests.

Frontend:
- `frontend/src/views/CampaignsView.vue`, `EmailEditorView.vue` — new pages.
- `frontend/src/components/organisms/EditorShell.vue`, `BlockLibrary.vue`, `EmailDocument.vue`, `EmailEnvelope.vue`, `EmailInspector.vue`, `EmailVariables.vue` — new.
- `frontend/src/components/molecules/CouponCode.vue` — new.
- `frontend/src/composables/useEditorDrag.ts` — new (drag/drop, DEV-7: no network call on drop).
- `frontend/src/composables/useIntents.ts` — added `go-email` and `copy-variable` intents only (both were missing; mirrors `assets/app.ts`'s own intents of the same name).
- `frontend/src/router/routes.ts` — swapped `EmptyPageView` → `CampaignsView`/`EmailEditorView` for the `campaigns`/`email_new`/`email_edit` routes; added the two imports. No other line touched.
- `frontend/src/i18n/messages/campaigns.pl.ts`, `campaigns.en.ts` — new, single top-level key `campaigns`.
- `frontend/test/unit/campaignsComponents.spec.ts`, `emailDocument.spec.ts` — new, 5 tests total.
- `frontend/test/integration/CampaignsView.spec.ts`, `EmailEditorView.spec.ts` — new, 12 tests total.

Shared tooling (see "Deviations / tooling fix" below):
- `tools/migration-verify/compare.mjs` — one narrow, verified bug fix (response-body capture for full-parity paths reached via a `click` step). No other line touched.

Not touched: any file outside this list, including `EmptyPageView.vue`, `PageHead.vue`, `Field.vue`, old-stack paths, the oracle dir, other workers' files.

## Behaviour → test map

| Behaviour | Unit test | Integration test |
| --- | --- | --- |
| B27 (KPIs, 4 filters, 5 rows, last row "—") | `CampaignsViewServiceTest.kpisMatchTheOracle`, `.filtersMarkTheRequestedOneActive`, `.rowsAreIdenticalRegardlessOfFilter`, `.rowsMatchTheOracle` (backend); `campaignsComponents.spec.ts` n/a (covered via view test below) | `CampaignsControllerTest.campaignsPayloadShape`, `.filterQueryMarksTheRequestedChipActive`, `.unsupportedLocaleCampaignsIsNotFound` (sliced HTTP); `CampaignsApiIT.campaignsPageRenders200`, `.campaignsPayloadMatchesTheOracleThroughTheRealHttpLayer`, `.unsupportedLocaleCampaignsIsNotFoundThroughTheRealHttpLayer` (real HTTP + Testcontainers); `CampaignsView.spec.ts` (5 tests, real router + i18n + stubbed fetch) |
| B28 (library/envelope/document/inspector; white doc under dark theme) | `CampaignsViewServiceTest.knownTemplateMatchesTheOracle`, `.unknownIdFallsBackToTheBlankTemplate`, `.editorPayloadShapeIsConstantAcrossIds` (backend); `campaignsComponents.spec.ts` (BlockLibrary 10-button count, CouponCode) (frontend); `emailDocument.spec.ts` (2 tests: computed `.ee-doc` background = `rgb(255, 255, 255)` under `data-theme=dark` AND `=light`, real CSS cascade via jsdom, not mocked) | `CampaignsControllerTest.knownEmailPayloadShape`, `.newEmailPayloadShape`, `.unmatchedEmailIdShapeIsNotFound`, `.unsupportedLocaleEmailIsNotFound`; `CampaignsApiIT.knownEmailDocumentRenders200`, `.unseededButShapeValidEmailDocumentStillRenders200`, `.knownEmailPayloadMatchesTheOracleThroughTheRealHttpLayer`, `.newEmailPayloadMatchesTheOracleThroughTheRealHttpLayer`; `EmailEditorView.spec.ts` (7 tests: library count, document sections, inspector `hero_title` value, blank-draft route, and a dedicated DEV-7 test asserting `dragstart`+`drop` never calls `fetch`) |
| B01 (page renders 200) | — (already owned by wave-0's `SpaDocumentControllerTest`/`RouteTableTest`, which already parametrize `/pl/campaigns`, `/pl/emails/new`, `/pl/emails/k1`; not touched) | `CampaignsApiIT.campaignsPageRenders200`, `.knownEmailDocumentRenders200` add real-HTTP-layer coverage for this journey's own routes specifically |

Every behaviour in scope (B27, B28) has both a named unit test and a real-HTTP/real-router integration test.

## Gate table

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| RED | stack up + compare.mjs contract + both Playwright specs, against pre-implementation worktree | worktree root / tests/e2e | (RED, as required) | `evidence/red-contract.txt`, `evidence/red-lists-campaigns.txt`, `evidence/red-editors-email-editor.txt`, `evidence/red-contract/` |
| 1a | `./mvnw -o test` | `backend/` | 0 (184 tests, 0 failures) | `evidence/gate-backend-unit-test.txt` |
| 1b | `npm run test -- --run` | `frontend/` | 0 (79 tests, 16 files) | `evidence/gate-frontend-unit-test.txt` |
| 2a | `./mvnw -o verify` | `backend/` | 0 (184 + 46 tests, spotless clean) | `evidence/gate-backend-verify.txt` |
| 2b | `npm run test:integration -- --run` | `frontend/` | 0 (55 tests, 11 files) | `evidence/gate-frontend-integration-test.txt` |
| 3a | ArchUnit (`ArchitectureTest`, part of gate 1a) | `backend/` | 0 (5 rules, 0 violations) | `evidence/gate-backend-unit-test.txt` |
| 3b | `npm run lint` | `frontend/` | 0 (0 errors; 2 pre-existing warnings in `FeedCard.vue`, not mine) | `evidence/gate-frontend-lint.txt` |
| 3c | `npm run typecheck` | `frontend/` | 0 | `evidence/gate-frontend-typecheck.txt` |
| 4a | `./mvnw -o spotless:check` (part of gate 2a's `verify`) | `backend/` | 0 | `evidence/gate-backend-verify.txt` |
| 4b | `npm run build` | `frontend/` | 0 (228 kB / 77.7 kB gzip JS) | `evidence/gate-frontend-build.txt` |
| 4c | `npm run format:check` | `frontend/` | 0 | `evidence/gate-frontend-format-check.txt` |
| 5a | `compare.mjs --dimension contract` | worktree root | 0 regressions | `evidence/contract/campaigns-email-editor/report.md`, `evidence/gate-contract-final.txt` |
| 5b | `compare.mjs --dimension visual` | worktree root | **1 regression** (see finding below) | `evidence/visual/campaigns-email-editor/report.md`, `evidence/gate-visual-final.txt` |
| 6a | `npx playwright test lists.spec.ts -g "campaigns"` | `tests/e2e/` | 0 (1 passed) | `evidence/gate-e2e-lists-campaigns.txt` |
| 6b | `npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e/` | 0 (2 passed) | `evidence/gate-e2e-editors-email-editor.txt` |
| 7 | `performance.mjs --base https://localhost:19081` | worktree root | 0 (all 4 budgets pass: 76 935 B / 307 200 B JS, LCP 160 ms, TTI 15 ms, `/collect` p95 6.7 ms) | `evidence/gate-performance.txt` |

Sonar: NOT_APPLICABLE (no config). Introduced dependencies: none (only reused existing `pinia`/`vue`/`vue-i18n`/`vue-router`, existing Spring Boot/Testcontainers deps). Diff grepped for secrets: none found.

## Deviations used

- **DEV-7** (in scope): `useEditorDrag.ts` sets `text/plain` on `dragstart` and calls `preventDefault()` on `drop` over `.ee-canvas-wrap`, but never issues any request — reproduces the old stack's dead `POST .../blocks` call (404 today, R6) by making no call at all. Verified with a dedicated integration test (`EmailEditorView.spec.ts`, "dragging a library block onto the canvas never issues a network request (DEV-7)") that dispatches real `dragstart`/`drop` events and asserts the `fetch` mock's call count is unchanged.
- **DEV-4**: document requests compared by method/path/status/final-URL only (harness-standard, not journey-specific).

## Tooling fix (shared `compare.mjs`)

While proving the contract gate I found that `compare.mjs`'s passive `page.on("response", ...)` listener never captured the response body (only the request body, into `entry.requestBody`), while `compareHttpEntry`'s DEV-4 full-parity deep-check for `/preferences/*` reads `entry.body` (the response body). Every prior journey's oracle scenario never actually clicked `[data-action="set-theme"]` mid-journey (only `campaigns-email-editor` and the not-yet-run `shell-navigation` do), so this gap was never exercised before. It produced two false "regressions" at steps 5 and 7 (`POST /preferences/theme` body `undefined` vs oracle's `{"theme":"dark"}`/`{"theme":"light"}`) even though the live endpoint verifiably returns the correct body (confirmed with a manual `curl -X POST .../preferences/theme -d '{"theme":"dark"}'` → `{"theme":"dark"}`, exactly matching the oracle).

Fix (commit `4829252`): in the passive listener, when the path is one of DEV-4's full-parity patterns, `await response.text()` into `entry.body` the same way the explicit `s.post` step branch already did, wrapped in try/catch (some response types can't be read as text). This is additive only — it can only turn a previously-`undefined` body into a real one for the 6 full-parity path patterns already listed in `FULL_PARITY_PATTERNS`; every other response (assets, `/api/v1/**`) is untouched. Re-ran the contract dimension after the fix: 0 regressions.

I also attempted (and reverted) a second, broader fix: resetting `window.scrollTo(0, 0)` before every screenshot, to address the visual-dimension finding below. That change unexpectedly broke steps 1 and 5–7 (new text/aria mismatches, larger pixel diffs) — I don't fully understand why, and reverted it immediately rather than risk destabilizing the shared verifier for other journeys. Left uninvestigated for whoever owns `compare.mjs` next; see "Visual dimension finding" below for the narrower, still-open issue it was meant to fix.

## Visual dimension finding (not resolved — needs orchestrator decision)

`compare.mjs --dimension visual` reports 1 regression out of 40 checks (8 steps × {url, texts, aria, screenshotDesktop, screenshotMobile}): step 6 (`reload`, after the theme was set to dark in step 5; the reload's own note says "a reload flips it to 'light'" but `waitAttr` waits for `dark`), `screenshotMobile` only, 2.961% pixels differ (9746/329160), reproduced identically across two separate runs (fully deterministic, not flaky).

Evidence it is **not** a content/DOM defect:
- `texts` and `aria` are exact-parity on **all 8 steps**, including step 6.
- `screenshotDesktop` is exact-parity (0.000%) on step 6 and 5 of the other 7 steps (steps 1–3 show a pre-existing ≤0.009% cursor/caret antialiasing difference, well under the 0.5% threshold, on the campaigns list page — unrelated).
- `screenshotMobile` is exact-parity (0.000%) on the other 7 of 8 steps, including steps 5 and 7 which sandwich the failing reload step.
- Visual inspection of both images (`evidence/visual/campaigns-email-editor/steps/6/mobile.png` vs the oracle's own `context/migration-oracle/.../steps/6/mobile.png`) shows **the same content, shifted horizontally** — the oracle's capture is scrolled further right (topbar shows the full breadcrumb, sidebar toggle chevron out of view) than the candidate's (chevron visible, less scrolled).

Root cause: the `.email-editor` 3-pane CSS grid (`240px 1fr` under 1300px width, unchanged/byte-identical from the old design system) has an intrinsic minimum width that exceeds the 390px mobile viewport, so the page is horizontally scrollable there. `page.reload()` triggers Chromium's native scroll-restoration, which tries to re-apply the pre-reload scroll offset — but a small (sub-pixel-to-few-pixel) difference in the two stacks' rendered content width is enough for the restored offset to land at a visibly different position, even though the content at every scroll position is identical between the two. This is a property of the (untouched) CSS grid and the browser's own reload behavior, not something my Vue components or the API payload control.

I could not find a fix within my authorized scope: CSS is off-limits (byte-identical, never edit), and a page-level "scroll to (0,0) on mount" would make my page diverge from the old stack's own reload behavior in the opposite direction (the old stack does not reset scroll either — the oracle capture itself landed non-zero). My one attempt at a shared-tool-level fix (normalize scroll before every screenshot) broke other steps for reasons I didn't chase down further, and I reverted it rather than ship something I couldn't explain.

Recommendation: this needs a decision from whoever owns `compare.mjs`/the deviation table (Piotr Mucha per the plan) — either (a) a new accepted deviation masking `screenshotMobile` for reload steps journey-wide, or (b) a properly-investigated scroll-normalization fix in `compare.mjs` (my reverted attempt is not it). I did not add anything to `deviations.json` myself since this isn't covered by any existing DEV id in the plan and I have no authority to mint a new one.

## Everything else

Everything else in scope (B27, B28, DEV-7, all named gates) is done and green. Nothing else was left incomplete.

## Repair-1

Review verdict (`review.md`): FAIL on finding A — step 6 (reload) mobile screenshot, 2.961% pixels
differ, real regression: Chromium's native scroll restoration on `page.reload()` reads the
document's scrollable width *before* the SPA finishes hydrating (the pre-hydration shell, ~563px
wide) and clamps the restored offset against that width instead of the final rendered width
(~736px), landing at `scrollX=173` instead of the pre-reload `346`. The old MPA never had this
problem: its single server response already contains the full-width HTML, so its own native
restoration always sees the true width. Repair packet (`repair-1.md`, R1-A) authorized editing
`frontend/src/main.ts` (and, if strictly needed, `App.vue`/the router setup file) plus their tests
to fix this with a generic, journey-agnostic manual scroll-restoration mechanism.

**Candidate SHA: `4530eb231fa4457829c68319a9e18104cc65267d`**

```
git log --oneline b87a701..HEAD
4530eb2 fix: replace native reload scroll restoration with a hydration-aware manual one (#campaigns-email-editor)
4829252 fix: capture response body for full-parity paths reached via a click step (#campaigns-email-editor)
41dd5cb feat: add campaigns index and e-mail template editor (#campaigns-email-editor)
```

`git status --porcelain` is empty. Stack torn down (`down -v`), `kivvi-w-campaigns-api` image
removed, build cache pruned. Disk: 5.6 GB free at finish.

### Mechanism

New file `frontend/src/router/scrollRestoration.ts`, wired from `frontend/src/main.ts`
(`installScrollRestoration(router)`, called once at bootstrap, right after `app.use(router)`,
before `app.mount(...)`):

1. `history.scrollRestoration = "manual"` — disables the browser's own (mistimed) restoration
   entirely.
2. `window.addEventListener("pagehide", saveScrollPosition)` — on every full teardown of the page
   (reload, navigating to a different URL, closing the tab), persists
   `{ href: location.href, x: window.scrollX, y: window.scrollY }` to `sessionStorage` under one
   fixed key (a single slot; each `pagehide` overwrites whatever the previous one wrote).
3. Once `router.isReady()` resolves, `consumeScrollPosition(location.href)` reads and clears that
   slot, returning the saved `{x,y}` only if its `href` matches the current page's — a mismatch
   (a fresh navigation to a different URL) or nothing saved both short-circuit immediately, no
   further work. This ordering matters: the common case (most navigations restore nothing) never
   pays any waiting cost at all.
4. Only when there **is** a same-URL position to restore: `await nextTick()` (flush Vue's own
   pending synchronous DOM updates), then `await waitForStableLayout()` — polls
   `document.documentElement.scrollWidth`/`scrollHeight` once per `requestAnimationFrame` and
   waits for **12 consecutive identical readings** (capped at 90 frames ≈ 1.5s) before considering
   the layout final, then calls `window.scrollTo(x, y)`.

The "12 consecutive frames" number is not arbitrary window-dressing — it is the direct fix for a
subtlety the repair packet's own wording (`nextTick` + a single `requestAnimationFrame`) does not
by itself cover, found while building and proving this fix: every view in this app fetches its
payload asynchronously in `onMounted`, and the document's scrollable size reads as "unchanged" for
1-2 frames purely because nothing has rendered yet while the fetch is in flight — not because
layout has actually reached its final size. A short stability requirement (2 frames, matching the
packet's literal `nextTick`+one `rAF` phrasing) re-introduces the exact bug this module exists to
fix, just from manual code instead of the browser's native mechanism — confirmed by direct
instrumentation (`console.debug` traces, removed from the final diff) showing the 2-frame version
call `scrollTo(347, 0)` while the document was still only 564px wide, landing at the same clamped
174 as before. Requiring 12 consecutive stable frames comfortably outlasts that fetch's local
round-trip; the 90-frame cap keeps a page whose content could grow forever (none currently do at
`document.documentElement` scope — the event stream's own growth happens inside `.main-scroll`,
which the app shell keeps at a fixed size) from hanging the restore indefinitely.

Same-document (`push`/`pop`) navigations are untouched: `router.isReady()` resolves once per full
page load, so the whole `.then()` chain runs exactly once per load — never again for a later
in-app route change.

### Measured scrollX before/after (live probe against the candidate stack)

| Scenario | Viewport | Before reload | After reload | Note |
| --- | --- | --- | --- | --- |
| `/pl/emails/k1`, theme-toggle clicked (scrolls the overflowing topbar into view) | 390×844 (mobile) | 347 | **347** | matches; was 174 (clamped) before this fix, confirmed via the same probe run against `4829252` |
| `/pl/campaigns`, never scrolled | 1440×900 (desktop) | 0 (no overflow) | **0** | no false restore |
| `/pl/dashboard`, never scrolled | 390×844 (mobile) | 0 | **0** | no false restore |

Full probe output: `evidence/repair-1-gates/00-live-scroll-probe.txt`. (The probe scripts
themselves were temporary, run from `tests/e2e/` and deleted afterward — not part of the diff;
`git status --porcelain` was empty before every gate run below.)

### Files changed (repair-1 only)

- `frontend/src/router/scrollRestoration.ts` — new. `saveScrollPosition`, `consumeScrollPosition`,
  `waitForStableLayout`, `installScrollRestoration` (all exported for testability).
- `frontend/src/main.ts` — one import + one call (`installScrollRestoration(router)`), placed
  before `app.mount(...)`.
- `frontend/test/unit/scrollRestoration.spec.ts` — new, 10 tests: save/consume round-trip, consumes
  (clears) on read, ignores a mismatched href, no-op when nothing saved, switches off native
  restoration, saves on `pagehide`, restores once after the router is ready and the layout settles,
  never restores when nothing saved, never restores on a mismatched href, consumes exactly once
  (no double-apply on a subsequent check).

No other file touched — `App.vue` and the router setup (`router/index.ts`) were not needed.

### Gate table (candidate SHA `4530eb2`, logs under `evidence/repair-1-gates/`)

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `./mvnw -o test` | `backend/` | 0 (184 tests) | `01-backend-test.txt` |
| 2 | `./mvnw -o verify` | `backend/` | 0 (184+46 tests, spotless clean) | `02-backend-verify.txt` |
| 3 | `npm run test -- --run` | `frontend/` | 0 (89 tests, +10 from repair-1) | `03-frontend-test.txt` |
| 4 | `npm run test:integration -- --run` | `frontend/` | 0 (55 tests, unchanged) | `04-frontend-integration.txt` |
| 5 | `npm run lint` | `frontend/` | 0 (0 errors; 2 pre-existing `FeedCard.vue` warnings, not mine) | `05-frontend-lint.txt` |
| 6 | `npm run typecheck` | `frontend/` | 0 | `06-frontend-typecheck.txt` |
| 7 | `npm run format:check` | `frontend/` | 0 | `07-frontend-format-check.txt` |
| 8 | `npm run build` | `frontend/` | 0 (229 kB / 78.1 kB gzip JS) | `08-frontend-build.txt` |
| 9 | `compare.mjs --journey campaigns-email-editor --dimension contract` | worktree root | 0 regressions | `09-compare-contract-campaigns.txt` |
| 10 | `compare.mjs --journey campaigns-email-editor --dimension visual` | worktree root | **0 regressions** (step 6 mobile now 0.000%, was 2.961%) | `10-compare-visual-campaigns.txt` + `compare-visual-campaigns/campaigns-email-editor/report.md` |
| 11 | `compare.mjs --journey login --dimension visual` | worktree root | 0 regressions | `11-compare-visual-login.txt` |
| 12 | `compare.mjs --journey customers --dimension visual` | worktree root | 0 regressions | `12-compare-visual-customers.txt` |
| 13 | `compare.mjs --journey event-stream --dimension visual` | worktree root | 0 regressions (rules out the live-Mercure-growth concern) | `13-compare-visual-eventstream.txt` |
| 14 | `npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 4 failed / 10 passed — **expected**: `automations`, `popups`, `import`, `settings` sidebar-entry tests fail because those routes are still `EmptyPageView` on this branch (not yet merged from their own wave-3/4 slices); `feeds`, `campaigns`, `dashboard`, `events`, `customers`, "only `.main-scroll` scrolls", "sidebar collapse survives a reload", "theme toggle survives a reload", locale switch and breadcrumb all pass — including two other `page.reload()`-driven tests, confirming this fix does not disturb sidebar/theme persistence | `14-e2e-navigation.txt` |
| 15 | `npx playwright test public.spec.ts` | `tests/e2e/` | 0 (5 passed, incl. a real login flow) | `15-e2e-public.txt` |
| 16 | `npx playwright test events.spec.ts --workers=1` | `tests/e2e/` | 0 (4 passed) | `16-e2e-events.txt` |
| 17 | `npx playwright test lists.spec.ts -g "campaigns"` | `tests/e2e/` | 0 (1 passed) | `17-e2e-lists-campaigns.txt` |
| 18 | `npx playwright test editors.spec.ts -g "email editor"` | `tests/e2e/` | 0 (2 passed) | `18-e2e-editors-email-editor.txt` |
| 19 | `performance.mjs --base https://localhost:19081` | worktree root | 0 (4/4 budgets pass: 77 347 B JS, LCP 224 ms, TTI 27.8 ms, `/collect` p95 23.5 ms) | `19-performance.txt` |

Note on gate 14: running the full file with default parallelism (`fullyParallel: true`, no
`--workers=1`) once produced a spurious extra failure on the `feeds` test (a 30s timeout waiting
for a nav-item locator that was never touched by this fix) under heavy host contention (this
machine runs many unrelated Docker workloads); re-running that single test in isolation passed in
568ms, and the full file with `--workers=1` reproduced cleanly as exactly the 4 expected failures
listed above with no flake. The serial run is what is saved as evidence.

No deviation row was added to `deviations.json` or the plan — the fix reaches parity, so none was
needed.

### Disposition

Finding A from `review.md` is resolved: `compare.mjs --dimension visual` for
`campaigns-email-editor` is now 0 regressions end to end (40/40 checks), and the three guard
journeys (`login`, `customers`, `event-stream`) confirm the fix does not disturb any other page's
scroll/reload behaviour. Nothing else from the review was reopened (B and C were already
"confirmed"/passing, not blockers).
