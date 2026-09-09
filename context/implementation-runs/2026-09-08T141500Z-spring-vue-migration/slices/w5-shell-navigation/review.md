PASS

Independent review of `git diff 60445ebac139bcf1179b397c997600073a356e95...7e6a66a` on
`migration/wave-5/shell-navigation` (worktree `w5-shell`). 2 commits, 5 files: `ShellPagesIT.java`
(new), `frontend/src/router/scrollRestoration.ts`, `ShellNavigation.spec.ts` (new),
`frontend/test/unit/scrollRestoration.spec.ts`, `frontend/test/unit/useIntents.spec.ts`. Worker's
own framing ("verification-heavy journey, chrome already closed on parent SHA") matches what the
diff actually contains — checked, not just accepted.

## Rubric findings

1. Scope — PASS. All 5 touched files fall under the packet's touch-list (`backend/src/test/**/ShellPages*`,
   `frontend/src/router/**`, `frontend/test/**/*navigation*`). `frontend/test/unit/scrollRestoration.spec.ts`
   and `frontend/test/unit/useIntents.spec.ts` don't match the generic `*shell*`/`*navigation*` test
   patterns by filename, but packet.md:17,19 name these exact files for the two deferred repairs —
   authorized by name, not a violation. No page-body files, no old-stack paths (comments cite old
   files for provenance only), no ledger files committed (`git status --porcelain` empty; worker
   report/evidence live only under the main-checkout run dir, never on the branch).

2. ShellPagesIT (backend/src/test/java/click/kivvi/ShellPagesIT.java:76-104) — PASS. All 25 URLs
   from `tests/Controller/PanelPagesTest.php::pages()` are reproduced verbatim, same order, none
   missing/altered (diffed line by line). `everyPageDocumentRenders200` (line 109) asserts 200 +
   `<html`; `everyPanelPageResolvesItsShellSection` (line 121) asserts `currentSection`/`crumb` for
   the 23 shell-bearing rows. 404s: `/de/dashboard` direct (line 152); unknown customer id
   (`CustomersApiIT.java:96` B05/DEV-12), unknown settings tab (`SettingsApiIT.java:227` B06/DEV-12)
   and out-of-range import step (`ImportApiIT.java:85` B31/DEV-12) verified present in their owning
   journeys' `*ApiIT` files, as the class doc comment (lines 45-52) claims — not duplicated, but not
   missing either. Real HTTP (`TestRestTemplate`, `RANDOM_PORT`) + Testcontainers (`postgres:18-alpine`).

3. ShellNavigation.spec.ts (frontend/test/integration/ShellNavigation.spec.ts) — PASS. Mounts the
   real router → `AppLayout` → `useShellStore` → `AppShell`/`Sidebar`/`Topbar` chain (line 389-402),
   fetch stubbed only at the shell-API boundary — not prop-fed. `PANEL_ROUTES` (line 360) covers 18
   URLs spanning all 16 non-public route names in `routes.ts` (18 total minus `home`/`login`, which
   use `PublicLayout`/`AuthLayout` and carry no shell — correctly excluded). Each row asserts
   `aria-current="page"`/`data-active` on the active item AND `undefined`/`"false"` on every other
   nav item (line 438-443), plus crumb text. Persistence: three tests (line 451-524) prove sidebar/theme
   re-hydrate from the API payload on a simulated reload, both via a full `AppLayout` mount and via
   the store directly.

4. Deferred items — PASS.
   - try/catch: `scrollRestoration.ts:206-215` wraps the body, defaults to `false` on throw.
   - Test: `frontend/test/unit/scrollRestoration.spec.ts:534-551` stubs `history.replaceState` to
     throw a `DOMException` and asserts `false` — verified as load-bearing (mutation below).
   - `router.replace()` doc comment: `scrollRestoration.ts:171-180` — checked the claim "this app
     never calls `router.replace()` anywhere"; grepped `frontend/src` — true, zero call sites.
   - Citation fix: `useIntents.spec.ts` doc comment now cites `assets/app.ts` (not
     `assets/controllers/shell.ts`) for the POST-ordering pattern — confirmed correct by reading the
     old-stack file (`assets/controllers/shell.ts` only forwards `transitionend`→`resize`).
   - `<html data-sidebar>` decision (kept, not removed; `frontend/src/stores/shell.ts` — unchanged by
     this diff, decided in an earlier round): old `templates/layout/app.html.twig:16` stamps
     `data-sidebar` only on `.app`; new `SpaDocument.java` stamps `<html>` too, and `stores/shell.ts`'s
     `setSidebar` keeps it synced. Rationale (ancestor-agnostic frozen CSS selector
     `[data-sidebar="collapsed"] .nav-label`; no `.app` in the SPA's static `index.html` before Vue
     mounts, so `<html>` prevents a pre-hydration flash) is sound and matches the frozen CSS the
     worker cannot edit. Checked observability: `compare/shell-navigation/report.md` shows 0
     regressions across all 21 steps on `visual.aria`/`visual.texts`/screenshots (0.000-0.008% pixel
     diff, steps 11-16 included — exactly where the pre-fix desync previously broke). Not observable
     in any verified dimension → accept with rationale recorded, per the rubric's own instruction.

5. Evidence — PASS. `compare-shell-contract.log`/`compare-shell-visual.log`: 0 regressions, 21
   steps (step 21 `accepted-deviation(DEV-12)`, status-only, correct). Guards: `compare/login`,
   `compare/dashboard` visual — 0 regressions each. `e2e/navigation-runs/run-{1..5}.log`: 14/14 every
   run, `--workers=1`. `performance.log`: JS 101990/307200 B, LCP 172/2000 ms, TTI 14.8/2500 ms,
   `/collect` p95 5.7/500 ms — all `pass: true`. Gate logs present for every row of the worker's gate
   table.

6. Behaviour → test map — PASS, and independently mutation-tested (not just read):
   - Backend: mutated `ShellViewService.java:51` (`crumb = "MUTATED"`), ran `ShellPagesIT` alone →
     24/52 failures at `ShellPagesIT.java:126` (`everyPanelPageResolvesItsShellSection`), exactly the
     B02 assertion. Reverted (`git checkout`), tree clean.
   - Frontend: mutated `NavItem.vue:23` (`:aria-current="undefined"`), ran `ShellNavigation.spec.ts`
     alone → 18/21 failures at `ShellNavigation.spec.ts:209` (the B02 `aria-current` assertion).
     Reverted, tree clean.
   B01/B21 mapped by name in the worker report to `ShellPagesIT`/pre-existing `routes.spec.ts` and
   `navigation.spec.ts` respectively — consistent with what exists in the repo.

7. Commit hygiene — PASS. Both commits (`2b5ecb2`, `7e6a66a`) are Conventional Commits
   (`fix:`/`test:`), English, descriptive bodies, no trailers, no AI/co-author mentions.

8. Report accuracy — PASS, independently re-run on candidate SHA `7e6a66a`:
   - `./mvnw -q verify` (JAVA_HOME=jdk-25): exit 0. `target/surefire-reports`: 289 unit tests, 0
     fail/err. `target/failsafe-reports`: 142 IT, 0 fail/err, 1 skipped (unrelated
     `SessionRequestSerializationRedProofIT`, pre-existing gate, not this journey).
     `ShellPagesIT`: 52/52. `spotless:check`: exit 0.
   - `npm run test -- --run`: 165 tests, 26 files, 0 fail — matches report exactly.
   - `npm run test:integration -- --run`: 152 tests, 21 files, 0 fail — matches report exactly.
   - `npm run lint`: 0 errors, 12 pre-existing warnings, all in untouched files
     (`StepRun.vue`, `HookRow.vue`, `FeedCard.vue`, `ApiTab.vue`, `DashboardView.vue`).
   - `npm run typecheck`: clean. `npm run format:check`: clean.
   All four numbers (289/142/165/152) match the worker report's gate table exactly.

## Minor notes (non-blocking)

- `frontend/test/unit/scrollRestoration.spec.ts` and `useIntents.spec.ts` sit outside the packet's
  generic `frontend/test/**/*shell*`/`*navigation*` glob but are individually named by the packet's
  deferred-item bullets — worth tightening the packet's touch-list wording for future waves so this
  doesn't need reviewer judgment.
- `compare.mjs`'s contract and visual runs overwrote each other's `shell-navigation/report.md` (both
  used the same `--out`); the contract 0-regression result survives only in stdout
  (`compare-shell-contract.log`), not as a separate per-row file. Documented honestly by the worker;
  not re-run given the disk budget — acceptable, but future packets should pass distinct `--out`
  dirs per dimension.
- `PreferencesController.java`'s class doc comment is now stale ("flips immediately") after
  `60445eb`'s await-then-apply change; correctly flagged by the worker as out of this packet's
  backend touch-list rather than fixed out of scope — a real, small doc-hygiene debt for another
  packet to close.

Verification housekeeping: `backend/target` and `frontend/node_modules`/`dist` deleted after each
run; two throwaway mutations reverted via `git checkout`; `git status --porcelain` empty at the end
of this review.
