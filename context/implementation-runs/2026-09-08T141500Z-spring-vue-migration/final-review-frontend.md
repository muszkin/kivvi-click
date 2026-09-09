PASS

# Final combined review — frontend (`frontend/**`)

Scope: `git diff $(git merge-base main migration/spring-vue)...dae1696 -- frontend` on
`/home/muszkin/work/kivvi-click-wt/verify-final-integration` (HEAD `dae1696`, merge-base
`8d3fc32`). Entire `frontend/` tree is new (212 files, ~26.4k lines, 0 deletions). Gates run:
`npm ci`, `npm run test -- --run` (169/169 pass), `npm run test:integration -- --run` (152/152
pass), `npm run lint` (0 errors, 12 warnings), `npm run typecheck` (0 errors), `npm run build`
(332.79 kB / 102.80 kB gzip JS — within the DEV-8 300 kB budget). `node_modules`/`dist` removed
after; `git status --porcelain` clean.

No material defect blocks cutover. Findings below are maintainability/robustness gaps for
follow-up, ordered by severity.

## Findings

1. **[Medium] No error handling on any view's data load.** Every view (`DashboardView.vue:88-94`,
   `ImportView.vue:62-73`, `CustomerView.vue`, `EmailEditorView.vue`, `CampaignsView.vue`,
   `PopupEditorView.vue`, `PopupsView.vue`, `CustomersView.vue`, `AutomationsView.vue`,
   `EventsView.vue`, `AutomationEditorView.vue`, `FeedsView.vue`, `LandingView.vue`,
   `SettingsView.vue` — 15 occurrences of the same pattern) does `if (!response.ok) return;` with
   nothing else: on a non-2xx response the page stays blank forever (`v-if="payload"` never turns
   true), no error message, no retry. A network-level `fetch`/`response.json()` rejection is only
   caught by Vue's default lifecycle error handler (`console.error`) since no `app.config.
   errorHandler` is registered in `main.ts`, and no test exercises this path — it would trip the
   fail-on-warning test policy (`test/setup.ts`) the day someone adds one. The old server-rendered
   stack had no client-fetch failure mode to preserve here, so this is a new gap, not a parity
   violation. Fix: a small shared composable (`useApiPayload` or similar) that sets an `error` ref
   on non-OK/rejection and a minimal `<Callout tone="bad">` fallback in each view.

2. **[Medium] Double-click race on `toggle-sidebar`.** `useIntents.ts:26-29` computes
   `next = shell.sidebar === "expanded" ? "collapsed" : "expanded"` synchronously at click time,
   but `stores/shell.ts:98-111`'s `setSidebar` only writes `this.sidebar` *after* its `await
   fetch(...)` resolves (the wave-4 "await-before-DOM" ordering fix). Two clicks inside one RTT
   both read the same pre-toggle `shell.sidebar` and both compute the same target, so the second
   click does not toggle back — verified with a throwaway Vitest probe (mounted, mutated nothing,
   removed after): two rapid `setSidebar` calls both resolved to `"collapsed"` instead of
   `"collapsed"` then `"expanded"`. The old stack's own `assets/app.ts` updated the DOM
   synchronously before firing its (un-awaited) POST, so it never had this race — it was
   introduced by the ordering fix. Low real-world odds (needs a genuine double-click inside one
   RTT) and self-corrects on the next click, but worth a follow-up: read/write an optimistic
   pending-target ref instead of `shell.sidebar` when computing `next`, or disable the control
   while a toggle is in flight.

3. **[Low-Medium] `npm run lint` is not warning-clean.** 12 `vue/multiline-html-element-content-
   newline` warnings, all on the `{{ " " }}` whitespace-preservation blocks (e.g.
   `DashboardView.vue:221-228`, `FeedCard.vue:164-169`, `HookRow.vue:45-46`, `StepRun.vue:142-144`,
   `ApiTab.vue:78-80`). These blocks are deliberately written without line breaks to avoid
   reintroducing the whitespace text-node bug the pattern exists to fix; an `eslint --fix` or an
   IDE's vue-formatter would silently "clean up" them back into a visual regression. Every other
   pure-formatting rule was explicitly turned off in `eslint.config.js:77-83`; this one was missed.
   Fix: add it to that same off-list, or add a file/line-scoped `eslint-disable` with a one-line
   reason at each site.

4. **[Low-Medium] `test/**` is excluded from type-checking.** `tsconfig.json:24-29`'s `include`
   only lists `src/**`; `npm run typecheck`/`vue-tsc --noEmit` (also run inside `npm run build`)
   never processes the ~9.7k-line `test/` tree, so a type error in a spec is invisible to every
   static-analysis gate in the plan's verifier contract. No current spec relies on `any`/`@ts-
   ignore` so nothing is hidden today, but the gap is real. Fix: add `test/**/*.ts` to `include` (or
   a second `tsconfig.test.json` wired into CI).

5. **[Low] Comment density.** `router/scrollRestoration.ts` is 53% comment lines (147/274) —
   largely a "repair-N found X empirically" debugging narrative rather than durable "why"
   documentation; `stores/shell.ts` (31%) and `useIntents.ts` (21%) carry similar historical
   narration. The underlying engineering is sound and some of it is genuinely load-bearing
   (documented browser quirks), but this is well past "minimal and purposeful" — recommend moving
   the repair-history trail into commit messages/an ADR and keeping only the durable rationale
   inline.

6. **[Low] Duplicated page-head markup.** `AutomationEditorView.vue:103-114`,
   `EmailEditorView.vue:80-91`, `PopupEditorView.vue:191-202` and `CustomerView.vue:92-102` each
   hand-roll `page-head > h1.page-title + p.page-sub(v-html)` instead of reusing
   `PageHead.vue` (already used by the other 9 views), because `PageHead.vue` has no slot for the
   back-button each editor prepends before the title. Four near-identical copies risk drifting
   (e.g. an aria fix landing in three of the four). Fix: add a `#before-title` slot to
   `PageHead.vue` and switch these four views to it.

7. **[Low] `{{ " " }}` whitespace hack is undocumented at its 15 call sites** (`LandingView.vue`,
   `PopupsView.vue`, `Cardiogram.vue`, `FeedsView.vue`, `WorkspaceCard.vue`, `AddSlot.vue`,
   `TimelineItem.vue`, `SettingsNav.vue`, `BlockLibrary.vue`, `EmailEnvelope.vue`,
   `EditorShell.vue`, `PriceCard.vue`, `popups.pl.ts`). The only explanation lives in
   `common-journey-rules.md`, an implementation-run artifact that will not ship with the
   repository; `frontend/README.md` says nothing about it. To a future contributor this reads as
   dead markup. Fix: one line in `frontend/README.md`'s markup-porting section, referenced (or
   briefly repeated) at each site.

8. **[Info] Architecture-boundary ESLint rules are declared three times.**
   `eslint.config.js` repeats the same `Intl`/Mercure-topic `no-restricted-syntax` selector array
   at lines ~40-67, ~126-165 and ~230-260, because the rule's array replaces rather than merges
   across cascading config blocks. A future edit to one copy without the other two silently drops
   coverage for the files that block doesn't touch. Fix: hoist the shared selector array to a
   `const` and spread it into all three blocks.

## Accepted as-is

- All 18 `v-html` sites traced to their source: developer-authored i18n strings (`landingPage.*`,
  `auth.headline`, `popups.triggerSummary`, `import.rules.callout` — no interpolation params) or
  backend fixture/content payloads (`EmailDocument.section.body`, `RbBlock.body`,
  `SimulationCard.item.value`, `ToggleRow.label`, `Callout.text`, `ValidationList.check.label`),
  never reflected user input. `Icon.vue`'s v-html is keyed off the static `ICONS` map
  (`lib/icons.ts`) — unknown names render nothing. `i18n/index.ts`'s `warnHtmlMessage: false` is
  scoped to the same static catalogues. `EventRow.vue` renders every live Mercure-event field
  (reachable via the public, unauthenticated `/collect`) through plain `{{ }}` interpolation, not
  v-html — no new XSS surface from that public input.
- `highlight.ts` HTML-escapes before wrapping spans (verified: escape-then-wrap ordering), used
  only on the fixed tracker snippet in `SitesTab.vue` — safe, and matches the old PHP filter's
  documented quirk (single-quoted strings never highlighted) intentionally.
- `Dropzone.vue`'s unguarded `fetch`/redirect and `useEventStream.ts`'s unguarded `JSON.parse` in
  `onmessage` reproduce the old stack's `assets/controllers/{upload,event-stream}.ts` byte-for-
  byte (diffed both) — not new regressions. The old event-stream controller's `innerHTML` sink is
  in fact removed by this migration (rows now render through Vue text interpolation, DEV-3).
- `useEditorDrag.ts` (DEV-7) correctly drops the dead `.../blocks` POST and the `innerHTML` swap
  it used to drive — a net security simplification.
- Google Fonts loading (`index.html`) is byte-identical to `templates/layout/base.html.twig`.
- PL/EN i18n key parity is 100% across all 11 message-module pairs plus the base catalogues
  (verified programmatically) — no missing-key/fallback-locale surprises.
- `router/localeHref.ts`'s `defaultParams` handling, `Modal.vue`'s focus-trap/ESC/Tab-cycle/
  restore-focus, `Cardiogram.vue`'s listener/interval/MutationObserver cleanup, and `NavItem.vue`'s
  `aria-current`/`data-active` all check out against the oracle markup and behave correctly.
- `Modal.vue`'s hardcoded `aria-label="Zamknij"` (never localized, even in EN) is byte-identical to
  `templates/components/organisms/modal.html.twig:21` — a pre-existing gap, not introduced here.
- Bundle is 102.80 kB gzip vs. the 300 kB DEV-8 budget; single JS chunk (no route-level code
  splitting), which is a reasonable choice given every navigation is a full document reload by
  design (never `router.push`), so splitting would buy little.
- Test suite: 169 unit + 152 integration tests, all green; global fail-on-warning policy
  (`test/setup.ts`) restricted to first-party stack frames; no `setTimeout`/sleep-based waits found
  (one legitimate `vi.advanceTimersByTime` use); mutation-tested two assertions and both are
  load-bearing: (a) integration — reverting `stores/shell.ts`'s await-before-DOM ordering broke
  `shellStore.spec.ts`'s "applies ... only after the POST resolves" test; (b) unit — removing the
  `data-paused` guard in `useEventStream.ts` broke `EventStream.spec.ts`'s pause test. Both
  mutations reverted, `git status --porcelain` confirmed clean after each.
- Minor, harmless: jsdom prints "Not implemented: Window's scrollTo()" during
  `test:integration` (stack originates in jsdom internals, not first-party code, so it doesn't
  trip the fail-on-warning policy) — cosmetic only.
