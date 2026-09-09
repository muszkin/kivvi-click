# Slice packet w5-shell-navigation — wave-5, journey J12 "shell-navigation" (single, verification-heavy)

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w5-shell · branch `migration/wave-5/shell-navigation` · parent SHA <PARENT-SHA> (feature HEAD after wave-4). **Lease:** compose project `kivvi-w-shell`, HTTP_PORT=19140, HTTPS_PORT=19141. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can move between every panel section and keep sidebar, theme and locale choices across reloads.
**Oracle:** journeys/shell-navigation/steps/1..21 (every sidebar entry opens its page; only `.main-scroll` scrolls; sidebar collapse, theme toggle and locale switch survive a reload; breadcrumb names the section; step 21 is a DEV-12 404 document). Behaviours: B01 (ALL rows), B02, B21.
**Old-stack sources:** `templates/layouts/*.html.twig`, organisms `sidebar`, `topbar`, `modal`, `assets/controllers/shell.ts` (sidebar/theme/locale persistence, `open-command-bar` intent, Ctrl/Cmd+K), `assets/controllers/modal.ts`, `src/Controller/PreferencesController.php`, `src/Panel/Navigation*.php`, `tests/PanelPagesTest.php` (the 25-URL smoke), `tests/e2e/specs/navigation.spec.ts`.

## In scope
- No new pages. Close every residual chrome gap the full oracle reveals: breadcrumb text per section, `open-command-bar` intent + keyboard dispatch (Ctrl/Cmd+K) with the same `Modal` markup, the only-`.main-scroll`-scrolls invariant on every route (measure `window.scrollY` stays 0 while `.main-scroll` scrolls, on desktop and mobile), the generic locale toggle (route `meta.defaultParams`, produced by settings repair-1) for every route incl. `import/:step?`, the shell scroll-restoration (produced by campaigns repair-1) on every route.
- Backend: `ShellPagesIT` — `@ParameterizedTest` over the 25 old-stack URLs from `tests/PanelPagesTest.php` asserting the SPA document is 200 and the `GET /api/v1/{locale}/shell?route=<name>` payload carries the resolved `currentSection`/crumb marker; 404 for unknown locale/ids exactly as the old `PanelPagesTest` expects.
- Frontend: `test/integration/ShellNavigation.spec.ts` — real router through every route in `routes.ts`, asserting active nav item, crumb, `aria-current`, and that the shell store persists sidebar/theme across a simulated reload (store re-hydration from the API payload).
- Tests: e2e `navigation.spec.ts` (whole file, all 14 tests green — from this wave on none of the section routes is an `EmptyPageView`), `public.spec.ts` regression guard; compare.mjs contract+visual for `shell-navigation` (21 steps, 0 regressions; DEV-4, DEV-12 step 21) and, as guards, for `login` and `dashboard`.

## Out of scope
Any page body change (owned by the journey that produced it) — if a page body diverges from its oracle, report it as a gap for that journey; do not fix it here.

## Deviations in scope
DEV-4, DEV-12 (step 21).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19141 npx playwright test navigation.spec.ts` (whole file), then `public.spec.ts`.

## Parallel-safety obligations
Single journey in its wave — no parallel workers. Touch only: `backend/src/test/**/ShellPages*`, `backend/src/main/java/click/kivvi/{web/ShellController.java,application/ShellViewService.java,application/NavigationCatalog.java}` (only for a proven crumb/section gap), `frontend/src/{App.vue,main.ts,router/**,stores/shell.ts,composables/useIntents.ts,components/organisms/{Sidebar,Topbar,Modal}.vue,components/molecules/NavItem.vue}`, `frontend/src/i18n/messages/shell.*.ts`, `frontend/test/**/*shell*`, `frontend/test/**/*navigation*`, your report/evidence.
