# Independent review — w3-campaigns-email-editor repair-3

PASS

Diff reviewed: `32a68311594e611aaa8eaa0803bc72bba7d735a9...fca55fc7ffe503e67de2e65f3b969442ee4f0a20`
(79a2224 fix, fca55fc test), worktree `w3-campaigns-email-editor-r3`,
branch `migration/wave-3/campaigns-email-editor-repair3`.

## Rubric 1 — root cause (compare.mjs/capture.mjs clock freeze)

**PASS.** `tools/migration-verify/compare.mjs:281` and
`context/migration-oracle/symfony-to-spring-vue/capture/capture.mjs:144` both call
`page.clock.setFixedTime(FIXED_TIME)` on every run. Independently reproduced with a fresh
Playwright probe (scratchpad, real `page.reload()`, 900ms poll): frozen clock →
`performance.getEntriesByType("navigation")` stays `length:0` before and after reload;
unfrozen clock → 1 entry, `type:"reload"`. Legacy `performance.navigation.type` is
`undefined` under the frozen clock in this Chrome 152 build (unusable either way). Matches
the worker's own probes (`evidence/repair-3/01-clock-freeze-probe.txt`,
`04-frozen-detail-probe.txt`, `05-legacy-navigation-api-probe.txt`) exactly.

## Rubric 2 — `isReloadOfAnAlreadyVisitedEntry()` mechanism

**(a) Survives vue-router's own writes — PASS.** Read `node_modules/vue-router` 5.3.1 source
directly: `beforeUnloadListener` (pagehide) does
`history.replaceState(assign({}, history.state, {scroll:...}), "")` — merges, doesn't
overwrite, so the marker survives. `push()`'s new-entry `changeLocation` builds state purely
from `buildState()` (no marker key) — a genuinely fresh entry never inherits it. Verified live
with a second scratchpad probe: marker false→true across a first visit, true again after a
real reload, false on a fresh `goto` to a new URL, true again on `goBack` to the original entry.

**(b) Correct on real reload / fresh goto / back-forward — PASS**, same probe. `routes.ts:24-28`
documents that this app never calls `router.push`/`router.replace` in-app (every transition is
a real document GET), so vue-router's own `replace()` helper — which *would* carry a marker from
`history.state` onto a *different*, newly-replaced entry (`vue-router.js` `replace()`:
`assign({}, history.state, buildState(...), data, {position})`, and `buildState` never clears a
foreign key) — is dead code here. **Low, undisclosed:** the doc comment at
`scrollRestoration.ts:150-172` states plainly that "a genuinely fresh top-level navigation... gets
a brand-new state object... with no trace of anything written on a previous visit," which is only
true for the `push()`/first-load path, not for `router.replace()`. If this app ever adopts
`router.replace()` (e.g. an auth redirect or query-only replace), the marker could leak onto an
unrelated entry and, combined with a same-href match in the sessionStorage fallback, wrongly
restore a stale scroll position. Not blocking today (no `router.replace`/`.push` call exists
anywhere in `frontend/src`), but should be an acknowledged deviation, matching the standard this
same slice's `review-repair-2.md` set for an analogous narrow gap.

**(c) No clock dependence, timers, named constants — PASS. Guarded try/catch — FAIL.**
`scrollRestoration.ts:185-190` has no `try`/`catch` around `history.state` access or the
`history.replaceState()` call, unlike every other browser-API touchpoint in this same file
(`readStorage`/`writeStorage`/`clearStorage`, `scrollRestoration.ts:84-106`, all guarded) and
unlike the function it replaces (`isReloadNavigation()`, which caught and defaulted to `false`).
`history.replaceState` can throw (Chrome enforces a ~100-calls/10s history-mutation limit;
sandboxed/cross-origin-restricted frames can also reject it); an uncaught throw here propagates
out of `scrollBehavior` and is only caught by vue-router's own generic
`.catch(err => triggerError(...))` (`vue-router.js:1490`), which `console.error`s it rather than
degrading gracefully the way the rest of this module does. The corroborating unit test
(`"is false (never throws) when performance.getEntriesByType is unavailable"`) was deleted
outright, with no replacement asserting the new function's own robustness. **Fix:** wrap the body
in `try { ... } catch { return false; }`, matching `isReloadNavigation()`'s and the file's own
established defensive convention, and add a test that stubs `history.replaceState` to throw and
asserts `isReloadOfAnAlreadyVisitedEntry()` returns `false` without throwing.

## Rubric 3 — tests

**PASS.** `scrollRestoration.spec.ts`: 4 new `describe("isReloadOfAnAlreadyVisitedEntry")` tests —
mutation-checked by inspection: deleting the `history.replaceState(...)` write breaks the
"marks it" assertion; always-`false` breaks the "second time" assertion. New
`ScrollRestoration.spec.ts` (99 lines) imports the real `@/router` singleton and calls
`router.options.scrollBehavior` directly — proves `router/index.ts` wiring, not just the helper.
`CampaignsApiIT.java`: `newEmailDocumentRenders200` added (`/pl/emails/new`, was missing),
`campaignsPageRenders200`/`knownEmailDocumentRenders200` relabelled `B01/B27`/`B01/B28`, matching
`AutomationsApiIT`'s convention and closing exactly the gap `waves/wave-3/round-1/integration.md:97-100`
named. Frontend `CampaignsView.spec.ts`/`EmailEditorView.spec.ts` titles relabelled `B01/B27`,
`B01/B28` (×2, including `/emails/new`).

## Rubric 4 — evidence

**PASS**, all independently re-checked against files under `evidence/repair-3-gates/`:
`compare-visual-campaigns/campaigns-email-editor/report.md` — 0 regressions, step 6
`screenshotMobile` 0.000% (0/329160), was 5.286% at the pre-fix SHA per
`waves/wave-3/round-1/visual.md:24`. `08`/`09` — login/customers 0 regressions.
`11-playwright-editors.txt` — 2/2 passed. `12-playwright-navigation.txt` — 12 passed / 2 failed
(popups, import — `EmptyPageView` placeholders, unrelated to this diff). All logs present.

## Rubric 5 — scope/hygiene

**PASS.** `git diff --stat` touches only `scrollRestoration.ts`, `CampaignsApiIT.java`, and four
test files — no `compare.mjs`, no CSS/`.vue`, no `router/index.ts`. Two commits, Conventional
Commits, English bodies, no trailers, no AI/co-author mentions (checked full commit messages).

## Rubric 6 — report accuracy (independently re-run)

**PASS**, exact match to `worker-report.md`'s gate table:
- `npm ci` clean; `npm run test -- --run` → 126/126, 21 files.
- `npm run test:integration -- --run` → 86/86, 15 files.
- `npm run lint` → 0 errors, 6 pre-existing warnings (`HookRow.vue`, `FeedCard.vue`, `ApiTab.vue`).
- `npm run typecheck` → clean. `npm run format:check` → clean.
- `JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify` → exit 0; `TEST-click.kivvi.CampaignsApiIT.xml`
  confirms `tests="8" errors="0" failures="0"` (was 7); no failure/error lines in any
  surefire/failsafe report.
- `git status --porcelain` empty in the worktree throughout, and in this reviewer's own
  workspace at the end of this review.

## Disposition

PASS. The R-C root-cause fix, R-A B01 traceability, and R-B wiring test all hold up under
independent reproduction (two from-scratch scratchpad probes reached the same conclusions as the
worker's own evidence). One concrete, fixable gap: `isReloadOfAnAlreadyVisitedEntry()`
(`scrollRestoration.ts:185-190`) lacks the try/catch guard the rubric asks for and the file's own
convention establishes elsewhere — Medium, non-blocking, recommend closing in a fast follow-up.
One Low/informational note: the doc comment's "no trace of a previous visit" claim doesn't hold
for `router.replace()`, currently unreachable dead code per `routes.ts:24-28` — worth an explicit
acknowledgement if in-app `router.push`/`.replace()` is ever introduced.
