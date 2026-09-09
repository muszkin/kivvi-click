PASS

Independent review of `71d884d..60445eb` (frontend/src/stores/shell.ts + shellStore.spec.ts only).
Read every changed line, ran the real gates myself, and injected both required mutations in place
(reverted; `git status --porcelain` clean at end, only untracked `evidence/`).

## Rubric

**1. Reorder — PASS**, shell.ts:98-111 (setTheme) / 113-124+138 (setSidebar). Both now `await fetch(...)`
inside `try`, apply `this.theme`/`document.documentElement.dataset.theme` (and for setSidebar, `.sidebar`
+ `document.documentElement.dataset.sidebar`) only after the `try`/`catch` block; `catch {}` has a comment
only, no `console.*` anywhere in the file (`grep console.` → 0 hits). `keepalive: true`, method, headers,
body byte-identical to parent (`git show 71d884d:...shell.ts` diffed by eye) and to the contract evidence
(`compare/shell-navigation-contract/.../http.jsonl` step 11/13/16: `requestBody`/`body` `{"state":"collapsed"}`
etc., 0 regressions).
**Risk (concrete, not hypothetical):** `useIntents.ts:26-28` computes `toggle-sidebar`'s target from
`shell.sidebar` synchronously at click time: `const next = shell.sidebar === "expanded" ? "collapsed" :
"expanded"`. Since `shell.sidebar` no longer flips until the POST resolves, two rapid clicks inside the RTT
window both read the *same* pre-flip value and send the *same* `next` twice — the second click does not
toggle back as a user would expect from an instant UI, it just re-sends the first click's target. Also,
`AppShell.vue:28` binds `.app`'s `data-sidebar` reactively to `shell.sidebar`, so the sidebar's whole
collapse/expand animation is now gated on one local RTT instead of firing instantly (the theme-toggle
*icon* is unaffected — `Topbar.vue:24-25` computes `themeIcon`/`themePayload` once from `initialTheme` at
mount, non-reactive by pre-existing design, confirmed by the surrounding comment). This is exactly the
"micro-timing" trade-off the packet pre-authorized ("no verifier dimension can observe it, no DEV row is
needed") — not a blocker, but flagging it as a residual, untested interaction risk for the next slice.

**2. `<html data-sidebar` sync — PASS, with one factual correction.** The old stack does **not** put
`data-sidebar` on `<html>` at all: `templates/layout/base.html.twig:13` only sets `data-theme` on
`<html>`; `data-sidebar` lives exclusively on `.app` (`templates/layout/app.html.twig:16`,
`assets/app.ts:44-46` writes `app.dataset.sidebar`, never `document.documentElement`). The rubric's premise
("confirm the old stack keeps data-sidebar on html") does not hold — the worker's in-code comment
("mirrors the old stack's own assets/controllers/shell.ts") overstates the parity for this specific
attribute. What *is* true and is the real justification: the **new** stack's `SpaDocument.inject`
(`backend/.../domain/SpaDocument.java:35-42`, called from `SpaDocumentService`, not `SpaDocumentController`
itself as shell.ts:129's comment says — minor mis-attribution, nit) stamps `data-sidebar` on `<html>` at
SSR, a new-stack-only architectural choice; `03-components.css`'s selectors (`[data-sidebar="collapsed"]
.nav-label` etc., confirmed byte-identical to `assets/styles/03-components.css`) are unscoped and match
`<html>` too. Confirmed via `git show 71d884d:frontend/src/stores/shell.ts` that `setTheme` already wrote
`document.documentElement.dataset.theme` while `setSidebar` never touched `document.documentElement` —
genuine pre-existing asymmetry, not introduced by this diff. The one-line fix (shell.ts:138) is complete:
initial state comes from SSR (`SpaDocument.inject` stamps it on every render, incl. reload), and the
fetch-then-apply ordering (item 1) now updates `<html>` on every toggle in both directions, verified by
mutation (below) and by the 0-regression shell-navigation visual re-run.
**Evidence gap (low severity):** the specific claim "reproduced identically on the unmodified parent
commit" (via a `git stash`-based revert, described as a throwaway/uncommitted step) has no distinct saved
compare.mjs artifact — only the "first run of the actual repair-2 candidate before the html-sync line was
added" is preserved (`evidence/repair-2/compare/shell-navigation-visual-retry/`, 7 regressions, steps
13-14, matches the report's description exactly). This is consistent with the claim but doesn't
independently prove the parent-revert step specifically ran; the code archaeology above independently
confirms the bug pre-dates repair-2 regardless.

**3. Tests — PASS, mutation-verified myself.** `shellStore.spec.ts` has all four required cases: deferred
promise unchanged-before/changed-after resolve for both actions (lines ~82-133), changed-on-rejection with
no console output for both (lines ~135-178). I mutated the file in place and reverted (`git diff` empty,
`git checkout --` confirmed clean):
- Reverted setTheme's DOM/state update to *before* the await → `B08 ... only after the POST resolves`
  failed (`expected 'light', received 'dark'`), 1/9 selected tests failed.
- Removed `document.documentElement.dataset.sidebar = state;` → exactly 3 tests failed for the right
  reason (`expected 'collapsed', received 'expanded'`), matching the worker's claim precisely.

**4. Evidence — PASS.** Independently re-checked (not re-run) the worker's saved logs:
`load-runs/{baseline,run-1..20}.txt` all "14 passed", 0 failures — timestamps span ~08:23:55→08:27:13
(≈3 min for 21×~8.7s runs), consistent with real sequential execution, not fabricated. `other-specs/`:
dashboard×3 = 6/6 each, events×3 = 4/4 each, public 5/5, settings 12/12, customers 4/4.
`compare-login-contract.txt`/`compare-login-visual.txt`/`compare-shell-navigation-contract.txt`: 0
regressions each. `compare-shell-navigation-visual.txt` (final): 0 regressions — its report.md steps 13/14
(the collapse/theme-toggle steps) are present and green, confirming the visual dimension does exercise
those steps. `performance.txt`: all 4 budgets pass (`collectP95Millis` 6.45/500ms etc.).

**5. Commit hygiene — PASS.** `git log -1 --format=%B 60445eb` → single line, `fix: report a preference as
applied only after the server committed it (#shell)`, Conventional Commits, English, no trailers, no AI
mention. `git show --stat` → only `frontend/src/stores/shell.ts` and
`frontend/test/integration/shellStore.spec.ts` changed.

**6. Report accuracy — PASS, reproduced.** In `frontend/`: `npm ci` → 259 packages, 0 vulnerabilities.
`npm run test -- --run` → **164/164** unit. `npm run test:integration -- --run` → **131/131** integration.
`npm run lint` → 0 errors, 12 pre-existing warnings, none in `shell.ts`/`shellStore.spec.ts` (all in
unrelated `.vue` files). `npm run typecheck` → clean. `npm run format:check` → clean. All numbers match
the worker report exactly.

## Overall

No blocking defects. The one substantive risk (rapid double-click on sidebar toggle can send a redundant
identical POST instead of toggling back, and the collapse animation now waits one local RTT) is real but
explicitly pre-authorized by the packet and not something any current gate exercises — worth a follow-up
note, not a repair. The rubric's own premise about the old stack's `<html data-sidebar` was wrong; the
fix itself is correct and necessary for the *new* stack's own SSR behavior, independently verified via
`SpaDocument.java` and the pre-diff `git show` of `setTheme`/`setSidebar`. Minor nit: shell.ts:129's
comment attributes the SSR stamping to `SpaDocumentController`; it is actually done in
`domain/SpaDocument.inject`, called from `SpaDocumentService` — worth a one-word fix next time this file
is touched, not worth a repair round on its own.
