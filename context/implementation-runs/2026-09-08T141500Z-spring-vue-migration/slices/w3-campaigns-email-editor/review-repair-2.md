# Independent review — w3-campaigns-email-editor repair-2 + rebase-1

FAIL

Diff reviewed: mechanism `4530eb2..3157a5f` (worktree, branch `migration/wave-3/campaigns-email-editor`,
HEAD `3157a5f042264de4b9dee26678a658d6f41b35df`); scope `2ca5f07..3157a5f`. `review-repair-1.md`'s
back/forward regression is genuinely fixed. The mechanism, tests, rebase and hygiene all pass. This
fails on a single evidence gap: the rebase's own regression guard for the two conflict-resolved
files was never run against the final rebased HEAD.

## Part 1 — repair-2 mechanism

1. **PASS.** `router/index.ts` wires vue-router's own `scrollBehavior` option (no app-level
   `history.scrollRestoration` write anywhere in the diff). Verified all four line citations in
   `scrollRestoration.ts:150-172`'s doc comment directly against this worktree's
   `node_modules/vue-router/dist/vue-router.js` — exact matches: line 1162
   (`if (isBrowser && options.scrollBehavior ...) history.scrollRestoration = "manual"`), lines
   76-77 (`popstate`+`pagehide` listeners registered unconditionally in `createWebHistory`), line
   1429 (`popstate` handler calls `saveScrollPosition(...)` into its own store), lines 1487-1490
   (`handleScroll` resolves `savedPosition` from that store or `history.state.scroll`, then calls
   `scrollBehavior`). Also confirmed `scrollToPosition` (`devtools-CN5uWJaH.js:323`) only branches
   on `"el" in position`; since this module's return values are always `{left, top}`, it always
   falls through to `window.scrollTo` — `.main-scroll` is never touched, matching native parity.
   Live popstate proof matches report: `evidence/repair-2-gates/00b-isolated-popstate-harness-probe.txt`
   (scrollY 1000→0 push→1000 back→0 forward, using vue-router's own global build, no docker).

2. **PASS**, with one undisclosed narrow deviation (Minor). Independently reproduced the
   `history.state.scroll`-does-not-survive-reload claim with a standalone Playwright probe (static
   page, `pagehide`→`replaceState`, scroll, reload, read `history.state`) against **both** browsers
   named in the task: bundled Chromium 151.0.7922.34 → `state:null` after reload; `channel:"chrome"`
   Google Chrome 152.0.7977.82 → also `state:null`. With `history.scrollRestoration="manual"` set
   first (vue-router's actual runtime behaviour), both browsers land the reload at `scrollX:0,
   scrollY:0` — confirms the fallback is a genuine necessity, not a misdiagnosis, on the exact
   compare-oracle browser (Chrome 152) as well as the bundled one.
   Fallback narrowness (`scrollRestoration.ts:174-190`): only reached when `savedPosition` is
   falsy (`:183`); `consumeScrollPositionForReload` (`:119-133`) calls `clearStorage()`
   unconditionally before the href check, so it is always consumed, matched or not; `pagehide`
   never fires for an in-app `pushState`/`popstate` navigation (only real document teardown), so
   the fallback cannot fire on push/popstate — confirmed by the "never touches the fallback ...
   (popstate)" unit test and the isolated harness proof. **Minor, undisclosed:** the fallback keys
   only on href equality, not navigation *type*. A user who fully leaves the SPA (firing
   `pagehide`, saving the entry) and then makes a **fresh top-level navigation to the identical
   href** (retyped URL, bookmark, "duplicate tab", clicking back into browser history) — not an F5
   reload — is indistinguishable from a real reload to this mechanism and gets the stale scroll
   restored, whereas the old MPA always starts such a navigation at the top. Neither
   `scrollRestoration.ts` nor `worker-report.md` mentions this case (`performance.getEntriesByType
   ("navigation")[0].type` could gate the fallback to `"reload"` only if this needs closing).
   Likelihood is low (requires a full teardown-then-identical-URL top-level nav within the same
   tab session) and arguably user-friendly rather than wrong — not blocking, but should be an
   explicit, acknowledged deviation rather than a silent one.

3. **PASS.** 13 tests in `frontend/test/unit/scrollRestoration.spec.ts` (verified count and content
   directly). Independently mutation-tested in an isolated scratch copy (source+tests+config
   copied out, `node_modules` symlinked back — no tracked file was edited):
   - `REQUIRED_STABLE_FRAMES` 12→1 (`scrollRestoration.ts:20`): **3 tests fail**, matches the
     report's claim exactly.
   - Deleting `await waitForStableLayout();` from the primary `if (savedPosition)` branch
     (`scrollRestoration.ts:179-182`): **1 test fails** (`scrollBehavior > waits for the layout to
     settle before resolving a saved position on an overflowing page`), matches the report.
   - Bypassing the href guard in `consumeScrollPositionForReload` (`:127-129`, always returning the
     saved value): **2 tests fail**. All three mutants are caught; repair-1's "the wait is
     untestable" gap (`review-repair-1.md` rubric 2) is resolved.

4. **Gap found — see Disposition.** Evidence for the packet's own live-proof/guard list matches the
   report: reload 347/347 (`00a-live-app-scroll-probe.txt`), push→0, non-overflow→0, visual guards
   0 regressions for campaigns-email-editor (incl. step 6 mobile `0.000%`), login, customers,
   event-stream (`evidence/repair-2-gates/1[0-3]-*.txt`). `14-e2e-navigation.txt` (run against the
   **pre-rebase** candidate `d0412c2`) correctly shows exactly the 4 expected failures
   (automations/popups/import/settings — automations not yet integrated on this branch at that
   point). But per this review's own brief, that test needed to be re-run **after** rebase-1, once
   automations *is* integrated, to prove the "automations" sidebar entry flips to passing and only
   settings/popups/import remain red — see Part 2/3.

## Part 2 — rebase-1

**PASS** (mechanically), but see the cross-cutting gap below. `git range-diff b87a701..4530eb2
2ca5f07..3157a5f` and `b87a701..d0412c2 2ca5f07..3157a5f` (actual pre-rebase HEAD) both show only
commit 1 (`41dd5cb`→`49f21bb`) with a diff, confined to the two import lines in
`frontend/src/router/routes.ts` and the added `on("go-automation", ...)` block in
`frontend/src/composables/useIntents.ts`; commits 2-5 are byte-identical (`=`), matching
`worker-report.md`'s Rebase-1 section verbatim. No duplicate route `name`s or `on(...)` handlers
(checked `routes.ts`/`useIntents.ts` directly); route order preserved
(dashboard/events/customers/**automations**/campaigns/popups/feeds/import/settings, sidebar
order); import lines alphabetical. `git diff 2ca5f07 3157a5f --stat` touches only
campaigns/email-editor-owned files plus exactly those two shared files (+ `scrollRestoration.ts`,
`main.ts`, `router/index.ts`) — nothing else owned by automations/settings.

## Part 3 — scope / hygiene / report accuracy

**PASS.** No CSS, no old-stack `templates/`/Symfony imports (only doc comments citing the Twig
files each Vue component was ported from — provenance, not a dependency). Commits `616abbb`,
`ebc6fd6`, `3157a5f`: Conventional Commits, English, no trailers, no AI/co-author mentions
(checked full commit bodies). Re-ran independently from a clean `npm ci`:
`test` 98/98, `test:integration` 69/69, `lint` 0 errors / 2 pre-existing `FeedCard.vue` warnings,
`typecheck` clean, `format:check` clean — all match the report exactly.
`./mvnw -q test` (backend) exit 0, no errors, matching the report's claim.

## Findings

- **Major (blocking).** `evidence/rebase-1-gates/` re-runs `lists.spec.ts -g campaigns`,
  `editors.spec.ts -g "email editor"` and `automations.spec.ts` (the packet-specified "union
  regression guard") but **never re-runs `navigation.spec.ts`** against the final rebased HEAD
  `3157a5f`. This review's brief explicitly requires it: "automations is now integrated on this
  branch — its sidebar test must pass." The only navigation.spec.ts run in evidence
  (`evidence/repair-2-gates/14-e2e-navigation.txt`) predates the rebase and correctly shows
  automations still failing (expected then, not now). Static check strongly suggests it now
  passes: `routes.ts:68` wires the `automations` route to `AutomationsView` (not `EmptyPageView`),
  and `AutomationsView.vue` renders `PageHead :title="t('automations.title')"`, where
  `i18n/messages/automations.pl.ts:12` sets `title: "Reguły i automatyzacje"` — the exact string
  `navigation.spec.ts:15` expects. But this is unproven at runtime, and `routes.ts` is precisely
  the file the rebase had to hand-resolve, making this the most relevant regression guard for the
  merge itself, not a redundant one. Fix: `cd tests/e2e && npx playwright test navigation.spec.ts
  --workers=1` against the rebased stack, confirm exactly 3 failures (settings/popups/import) with
  the "automations" case passing, save under `evidence/repair-2-gates/` or a new `rebase-1-gates`
  entry, and append the result to `worker-report.md`'s Rebase-1 gate table. No code change expected
  unless the run surfaces a real failure.
- **Minor (non-blocking).** Document or close the same-href/typed-navigation fallback gap
  described in Part 1.2 — either an explicit accepted-deviation note near
  `scrollRestoration.ts:150` (and in the worker report), or gate the fallback with
  `performance.getEntriesByType("navigation")[0]?.type === "reload"`.

## Disposition

Reject on evidence completeness alone: the mechanism, mutation coverage, rebase union and hygiene
all independently verified as sound (including two claims I re-derived from scratch — the
vue-router source citations and the Chrome-151/Chrome-152 `history.state.scroll` reload probe).
The one required check this review's own brief called out by name — `navigation.spec.ts` against
the post-rebase, automations-integrated HEAD — was never run. Close with the single evidence
addition above (and optionally the Minor note); no further mechanism changes are expected.
