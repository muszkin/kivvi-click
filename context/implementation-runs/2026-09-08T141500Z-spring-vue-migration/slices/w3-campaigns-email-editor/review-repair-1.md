# Independent review — w3-campaigns-email-editor repair-1

FAIL

Diff reviewed: `4829252f08bf5375c35d195d41096a214b8df19c...4530eb231fa4457829c68319a9e18104cc65267d`.
Finding A from `review.md` (step 6 mobile reload regression) is genuinely fixed — see rubric 4 —
but the fix itself introduces a new, undisclosed, untested regression (rubric 1), which is why
this repair still fails.

## Rubric 1 — Mechanism

- (a) SSE/live-content starvation — **PASS**. `frontend/src/styles/03-components.css:8-32`:
  `.app{height:100vh}`, `.main{min-height:0}`, `.main-scroll{flex:1;overflow-y:auto}` — the event
  stream's live-prepended rows grow inside `.main-scroll`, never `document.documentElement`, so
  `layoutSignature()` (`scrollRestoration.ts:73`, keyed on `documentElement.scrollWidth/Height`)
  cannot be starved by that growth. Confirmed by evidence gate 13 (event-stream visual, 0
  regressions). No "cardiogram canvas" exists (grep: it's a comment word only in `DashboardView.vue:5`).
- (b) vue-router interference — **FAIL, Major**. `router/index.ts` sets no `scrollBehavior` option,
  so vue-router itself never touches `history.scrollRestoration` (verified in
  `node_modules/vue-router/dist/vue-router.js:1162`: it only does so `if (options.scrollBehavior)`).
  `scrollRestoration.ts:143` sets `history.scrollRestoration = "manual"` **unconditionally and
  globally**, and only wires `pagehide` (`:144`) — never `popstate`. `pagehide` does not fire on a
  same-document `pushState`/`popstate` navigation, so this removes the browser's own native
  scroll-restoration on in-app **back/forward** with nothing put in its place. Verified empirically
  with an isolated Playwright probe (no docker stack, static `file://` page, pushState + `goBack()`):
  with `scrollRestoration` left at its default `"auto"` (pre-repair-1 state), a same-document back
  navigation restores `window.scrollY` natively (1000→1000); with `"manual"` (this patch's effect),
  it does not (stays at 200). This is a real, journey-agnostic regression across the *entire* app,
  not just campaigns — the old MPA's own "back" is always a hard reload with correct native
  restoration, so this is a fidelity regression too, not just an internal one. The docstring's claim
  at `scrollRestoration.ts:137-140` ("Client-side (`push`/`pop`) navigations are untouched") is
  incorrect: the JS restore-once branch doesn't re-run for them, but the native browser mechanism
  they relied on is disabled with no replacement. Fix: add a `popstate` listener that saves/restores
  like `pagehide` does, or (preferred, idiomatic) give the router a real `scrollBehavior(to, from,
  savedPosition)` option returning `savedPosition ?? {left:0, top:0}` — vue-router then manages
  `history.scrollRestoration` and per-entry save/restore itself, including for reload if paired with
  the same hydration-wait logic already written here.
- (c) keyed storage — **PASS**. `location.href` is the full URL incl. query/hash;
  `consumeScrollPosition` (`:59-68`) calls `clearStorage()` unconditionally before checking
  `saved.href === href`, so a stale/mismatched entry is always discarded, never left behind.
- (d) window vs `.main-scroll` — **PASS**. Restoring only `window.scrollTo` is correct: native
  browser scroll-restoration (the mechanism this replaces) only ever restores the top-level
  document/viewport scroll offset, never an arbitrary descendant scrollable element — verified from
  the HTML Standard's scroll-restoration-mode text, which is scoped to session-history-entry ↔
  document, not to elements. `.main-scroll` was never natively restored either, on the MPA or the
  SPA, so not restoring it here is not a regression.
- (e) SSR/hydration — N/A, confirmed no SSR path exists.
- (f) magic numbers / early returns / swallowed errors — **PASS**. `REQUIRED_STABLE_FRAMES = 12`
  (`:89`) is a named, commented constant; no bare `setTimeout`. Early returns at `:60` (`!raw`) and
  `:150` (`!saved`). `sessionStorage` access is guarded in `readStorage`/`writeStorage`/`clearStorage`
  (`:12-33`) with an explicit, documented "losing the saved position is harmless" rationale — an
  intentional degrade, not a silent swallow.

## Rubric 2 — Tests

10 unit tests exist in `frontend/test/unit/scrollRestoration.spec.ts` (4 + 6), all pass under
`npm run test -- --run` (verified: 89/89 total). Mutation-tested both required behaviours:
- Bypassing the href check in `consumeScrollPosition` (`return saved.href===href?...` → unconditional
  return) **fails 2 tests** as expected — good coverage for (c).
- **Gap (Major):** stubbing out the stability wait entirely — deleting
  `await waitForStableLayout();` at `scrollRestoration.ts:150`, or reducing
  `REQUIRED_STABLE_FRAMES` from 12 to 1 — **passes all 89 tests unchanged**. jsdom's
  `document.documentElement.scrollWidth/scrollHeight` never changes between polls, so
  `stubImmediateAnimationFrame` (`spec.ts:17-22`) makes every candidate frame-count trivially
  "stable" on the very first check; the suite cannot detect a regression to the exact timing
  constant the worker's own report calls "the direct fix" for the original bug. Add a test that
  fakes `layoutSignature()`'s output (e.g. stub `document.documentElement` width getters to change
  for N frames then settle) and asserts `scrollTo` is not called before settling.

## Rubric 3 — Scope

**PASS.** `git diff --name-status` shows exactly `frontend/src/main.ts` (M),
`frontend/src/router/scrollRestoration.ts` (A), `frontend/test/unit/scrollRestoration.spec.ts` (A).
No CSS, no `compare.mjs`, no campaigns product code.

## Rubric 4 — Evidence

**PASS.** `evidence/repair-1-gates/00-live-scroll-probe.txt`: 347→347 at 390×844 (matches worker
report table, review.md's 346 vs this run's 347 is a 1px capture-time difference, immaterial), 0/0
on two non-overflow pages. `compare-visual-campaigns/campaigns-email-editor/report.md`: step 6
`screenshotMobile` now `0.000% (0/329160)`, was `2.961%`. Guard journeys `login`/`customers`/
`event-stream` all 0 regressions (`11/12/13-*.txt`).

## Rubric 5 — Commit hygiene

**PASS.** `4530eb2 fix: replace native reload scroll restoration with a hydration-aware manual one
(#campaigns-email-editor)` — Conventional Commits, English, no trailers, no AI mention. Consistent
with the two prior commits' `(#campaigns-email-editor)` convention already accepted in `review.md`.

## Rubric 6 — Report accuracy

Re-ran independently: `npm ci` clean; `npm run test -- --run` 89/89; `npm run test:integration --
--run` 55/55; `npm run lint` 0 errors/2 pre-existing `FeedCard.vue` warnings; `npm run typecheck`
clean; `npm run format:check` clean; `./mvnw -q test` exit 0. All match the worker's gate table.
**One inaccuracy**: the report's mechanism section states "Same-document (`push`/`pop`) navigations
are untouched" — true only for this module's own code path, false for the browser's native
scroll-restoration those navigations previously relied on (see rubric 1b). The report did not run
or mention any back/forward test, so this side effect went undetected and unreported.

## Disposition

Reject repair-1. The reload regression (finding A) is genuinely resolved and evidenced. But fixing
it by unconditionally flipping `history.scrollRestoration` to `"manual"` at global bootstrap, with
only a `pagehide` hook and no `popstate` handling, silently removes native scroll memory from every
in-app browser back/forward navigation in the whole SPA — verified with an isolated probe, not
covered by any test in this diff or the existing e2e suite, and mischaracterized as a non-issue in
the report. Needs another pass: add real `popstate` save/restore (or switch to a router
`scrollBehavior` option) before this can land, plus a unit test that can actually detect a
regression to the 12-frame stability requirement.
