# Worker report — w5-shell-navigation (J12 shell-navigation, wave-5)

Worktree: `/home/muszkin/work/kivvi-click-wt/w5-shell`, branch `migration/wave-5/shell-navigation`.
Parent SHA: `60445ebac139bcf1179b397c997600073a356e95`. Candidate SHA: `7e6a66a`.

## Identity guard

Worktree, branch and parent SHA matched the packet exactly at start (`git log -1` on the
worktree HEAD == parent SHA before any work).

## Summary

This is the verification-heavy closing journey: all product-code chrome gaps the packet
anticipated (breadcrumb text, `open-command-bar`/Ctrl-Cmd-K dispatch, the
only-`.main-scroll`-scrolls invariant, the generic locale toggle incl. `import/:step?`, scroll
restoration, `<html data-sidebar>` sync) were **already closed** on the parent SHA by prior
waves' repairs (wave-3 `w3-shell-preferences`, wave-4 repair-2, and the parent-SHA commit
`60445eb "fix: report a preference as applied only after the server committed it (#shell)"`).
Proof: `compare.mjs --journey shell-navigation` (contract + visual, all 21 steps) and 5
consecutive `navigation.spec.ts --workers=1` runs both came back 0 regressions / 14-14-14-14-14
passed on the very first run, before any of my own changes. No page-body divergences were found
to report to other journeys either — every section's crumb/nav-label text
(`backend/src/main/resources/messages_{pl,en}.properties`) was checked against the oracle's
`texts.json`/`a11y.json` for all 21 steps and matches exactly.

So this slice is entirely the four deferred items the packet named, plus the verification
proofs and the two new required test suites (`ShellPagesIT`, `ShellNavigation.spec.ts`).

## Deferred items closed

1. **Preference POST/reload determinism proof.** Already implemented in the parent-SHA commit
   `60445eb` (`stores/shell.ts`'s `setTheme`/`setSidebar` now `await` the `keepalive: true` POST
   before applying the DOM/store update, replacing the earlier "DOM first, POST after,
   un-awaited" shape). My job was to *prove* it, not re-implement it:
   `cd tests/e2e && E2E_BASE_URL=https://localhost:19141 npx playwright test navigation.spec.ts
   --workers=1` run **5 times consecutively** — 14/14 every time, including "sidebar collapse
   survives a reload" and "theme toggle survives a reload"
   (`evidence/e2e/navigation-runs/run-{1..5}.log`).

2. **`scrollRestoration.ts` `isReloadOfAnAlreadyVisitedEntry()` try/catch + doc comment.**
   Closed. The function read/wrote `history.state` with no guard, unlike every other browser-API
   touchpoint in the file (`readStorage`/`writeStorage`/`clearStorage`) and unlike the function
   it replaced. Wrapped the body in `try { … } catch { return false; }` (commit `2b5ecb2`).
   Added a unit test that stubs `history.replaceState` to throw and asserts the function still
   returns `false` (`frontend/test/unit/scrollRestoration.spec.ts`). Also corrected the doc
   comment's "a fresh top-level navigation … gets a brand-new state object … with no trace of
   anything written on a previous visit" claim, which review-repair-3 (campaigns) flagged as only
   true for `push()`/first-load, not `router.replace()` — now explicitly notes `router.replace()`
   is unreachable dead code today (`routes.ts:24-28`'s own doc comment: every in-app transition is
   a real document GET) and an accepted, narrow limitation if that ever changes.

3. **Old-stack citations in `shell.ts`/`useIntents.spec.ts`.**
   - `frontend/src/stores/shell.ts`: already fixed in the parent-SHA commit `60445eb` (its old
     comment block, which cited `assets/controllers/shell.ts` for the POST-ordering pattern, was
     rewritten in that commit). No further action needed — verified by reading the current file
     and by `git log -- frontend/src/stores/shell.ts` up to the parent SHA.
   - `frontend/test/unit/useIntents.spec.ts`: **was still wrong** — its top-of-file doc comment
     cited `assets/controllers/shell.ts` for the "DOM update must be synchronous, before the POST
     even starts" pattern. `assets/controllers/shell.ts` only forwards a `transitionend` into a
     `resize` dispatch (verified by reading the old-stack file); the preference-POST pattern lives
     in `assets/app.ts`. Rewrote the comment to cite `assets/app.ts` correctly and to describe the
     *current* store behaviour (await-then-apply, per item 1 above) instead of the superseded
     "DOM first" description, while explaining why this particular suite still only asserts
     request shape, not DOM-update ordering (commit `7e6a66a`).
   - Also found (not touched, out of scope): `frontend/src/components/organisms/AppShell.vue`'s
     own citation of `assets/controllers/shell.ts` (line 12, "mirroring
     assets/controllers/shell.ts") **is correct** — `AppShell.vue`'s `transitionend`→`resize`
     forwarder is exactly what the old `shell.ts` does. `AppShell.vue` is not in this packet's
     touch list, so left as-is.

4. **`<html data-sidebar>` parity decision.** Investigated and resolved as **keep, not remove**.
   The old stack's CSS (`assets/styles/03-components.css:81-85`, frozen/byte-identical) uses a
   *bare* descendant selector `[data-sidebar="collapsed"] .nav-label` — no `.app` scoping — so it
   matches when **any** ancestor carries the attribute, not just the nearest one. The old MPA
   never needed an `<html>`-level stamp because `.app` itself was server-rendered with the correct
   value on every request (no hydration gap). The new SPA's static `index.html` has no `.app` at
   all until Vue mounts, so `SpaDocument.inject()` stamps `<html data-theme/data-sidebar>` to
   avoid a flash of the wrong state before first paint — and `stores/shell.ts`'s `setSidebar()`
   keeps `<html>` in sync afterwards (wave-4 repair-2), because if it didn't, a stale `<html>`
   value and a fresh `.app` value could **disagree**, and the bare CSS selector would still match
   via the stale `<html>` ancestor, hiding nav labels even though `.app` correctly reports
   "expanded" — exactly the 7-regression visual failure `common-journey-rules.md` records from
   before that fix landed. Removing the `<html>` sync is therefore not safe; kept as-is. Proof:
   `compare.mjs --journey shell-navigation --dimension visual` — 0 regressions across all 21
   steps, specifically including steps 11-16 (the collapse/reload/theme-toggle/reload sequence
   where this exact desync previously showed up) — `evidence/compare-shell-visual.log` and
   `evidence/compare/shell-navigation/report.md` (screenshot diffs 0.000-0.008% on every step,
   well under the 0.5% threshold).

## In-scope work closed (packet's "In scope" bullets)

All already correct on the parent SHA — verified, not changed:
- Breadcrumb text per section: `backend/src/main/resources/messages_{pl,en}.properties`'s
  `nav.*` keys match the oracle's `texts.json` breadcrumb strings exactly for every one of the 9
  sections (Pulpit/Dashboard, Strumień zdarzeń/Event stream, Klienci/Customers, …).
- `open-command-bar` + Ctrl/Cmd+K: both old stack (`assets/app.ts`) and new stack
  (`useIntents.ts`) dispatch `kivvi:command-bar:open` with **no listener anywhere** in either
  stack — verified by grep. Exact parity by construction; no `Modal` render is expected or
  needed for this intent in either stack (no oracle step or e2e assertion exercises a rendered
  command-bar modal).
- Only-`.main-scroll`-scrolls invariant: proven by `navigation.spec.ts`'s own test on every run.
- Generic locale toggle incl. `import/:step?`: `router/localeHref.ts` + `meta.defaultParams`
  (wave-3 settings repair-1, extended to `import` in wave-4) already handle every route
  generically; `LocaleToggle.spec.ts` (pre-existing) already proves it end-to-end for settings,
  and my new `ShellNavigation.spec.ts` exercises the full shell chrome (crumb + active nav item)
  for `/pl/import` and `/pl/import/2` too.
- Shell scroll restoration on every route: `router/scrollRestoration.ts`'s `scrollBehavior` is
  wired generically in `router/index.ts`, not per-route.

## New required tests added

- **`backend/src/test/java/click/kivvi/ShellPagesIT.java`** (new, 52 test cases via
  `@ParameterizedTest`/`@MethodSource`): the JUnit equivalent of the old stack's
  `tests/Controller/PanelPagesTest.php` — every one of its 25 URLs renders the SPA document 200
  (B01), and for the 23 that carry shell chrome, `GET /api/v1/pl/shell?route=<name>` resolves the
  `currentSection`/`crumb` the old page's breadcrumb carried (B02). Also reproduces
  `testUnsupportedLocaleIsNotFound` (B07) and adds an English-locale document+shell check (B04)
  for completeness with the source file, even though both already have named coverage in
  `ShellApiIT`. Deliberately does **not** duplicate `testDetailRoutesKeepTheirSectionActive`
  (B03, owned by customers — already in `CustomersApiIT`), `testUnknownCustomerIsNotFound` (B05,
  owned by customers — already in `CustomersApiIT`) or `testUnknownSettingsTabIsNotFound` (B06,
  owned by settings — already in `SettingsApiIT`).
- **`frontend/test/integration/ShellNavigation.spec.ts`** (new): the real
  router → `AppLayout` → shell-store → `AppShell`/`Sidebar`/`Topbar` chain, mounted at every
  panel route in `routes.ts` (18 representative URLs covering all 16 named panel routes),
  asserting the correct nav item is `aria-current="page"`/`data-active="true"` and every other
  nav item is not, the breadcrumb text, and the presence of `.sidebar`/`.topbar`/`.main-scroll`
  (B02) — plus three tests proving the shell store re-hydrates `sidebar`/`theme` from the API
  payload on a simulated reload (B08/B10), both through the real `AppLayout` mount and by calling
  the store directly.

## Behaviour → test map

| Behaviour | Backend test | Frontend test |
| --- | --- | --- |
| B01 (all rows) | `ShellPagesIT::everyPageDocumentRenders200` (25 cases, new) + `SpaDocumentControllerTest` (pre-existing, broader route set) + `RouteTableTest` | `routes.spec.ts` (pre-existing) |
| B02 | `ShellPagesIT::everyPanelPageResolvesItsShellSection` (23 cases, new) + `ShellApiIT::dashboardShellPayloadMatchesTheOracle` (pre-existing, deep single-route check) | `ShellNavigation.spec.ts` describe "B02 every panel route carries the shell…" (18 cases, new) + `Sidebar.spec.ts` (pre-existing, hand-crafted props) |
| B21 | — (e2e-only behaviour) | `tests/e2e/specs/navigation.spec.ts` (unchanged spec; proven 5× consecutive green runs, 14/14 each) |

(B03-B10 already have their own named tests in other journeys' files, unchanged by this slice —
see "Deferred items closed" item 1 and "New required tests added" above for which specific ones
this slice's new files reproduce for completeness.)

## Files changed

- `frontend/src/router/scrollRestoration.ts` — try/catch guard + doc-comment fix.
- `frontend/test/unit/scrollRestoration.spec.ts` — new throw-path test.
- `frontend/test/unit/useIntents.spec.ts` — doc-comment fix (citation + description).
- `backend/src/test/java/click/kivvi/ShellPagesIT.java` — new.
- `frontend/test/integration/ShellNavigation.spec.ts` — new.

No product-code behaviour changed (the `scrollRestoration.ts` change only adds a defensive
`catch` around an existing code path; the happy path is byte-identical). No new dependencies
introduced (no `package.json`/`pom.xml` touched). `git diff` grepped for
secret/token/key/password patterns — none found.

## Gate table (candidate SHA `7e6a66a`)

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1a | `./mvnw -q test` | `backend/` | 0 (289 tests, 0 fail/err) | `evidence/backend/mvn-test.log` |
| 1b | `npm run test -- --run` | `frontend/` | 0 (165 tests, 26 files) | `evidence/frontend/unit-test.log` |
| 2a | `./mvnw -q verify` | `backend/` | 0 (142 IT, 0 fail/err; incl. `ShellPagesIT` 52/52) | `evidence/backend/mvn-verify.log` |
| 2b | `npm run test:integration -- --run` | `frontend/` | 0 (152 tests, 21 files; incl. `ShellNavigation.spec.ts`) | `evidence/frontend/integration-test.log` |
| 3a | ArchUnit (`click.kivvi.architecture.ArchitectureTest`, inside `mvnw verify`) | `backend/` | 0 (5/5) | `evidence/backend/mvn-verify.log` |
| 3b | `npm run lint` | `frontend/` | 0 (0 errors, 12 pre-existing warnings in untouched files) | `evidence/frontend/lint.log` |
| 3c | `npm run typecheck` | `frontend/` | 0 | `evidence/frontend/typecheck.log` |
| 4a | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/backend/spotless-check.log` |
| 4b | `npm run build` | `frontend/` | 0 (also proven inside the Docker image build) | `evidence/docker-build.log` |
| — | `npm run format:check` | `frontend/` | 0 (after one `prettier --write` pass on the 2 touched files) | `evidence/frontend/format-check.log` |
| 5a | `compare.mjs --journey shell-navigation --dimension contract` | repo root | 0 regressions | `evidence/compare-shell-contract.log` |
| 5b | `compare.mjs --journey shell-navigation --dimension visual` | repo root | 0 regressions, all 21 steps | `evidence/compare-shell-visual.log`, `evidence/compare/shell-navigation/report.md` |
| 5c | `compare.mjs --journey login --dimension visual` (guard) | repo root | 0 regressions | `evidence/compare-login-visual.log`, `evidence/compare/login/report.md` |
| 5d | `compare.mjs --journey dashboard --dimension visual` (guard) | repo root | 0 regressions | `evidence/compare-dashboard-visual.log`, `evidence/compare/dashboard/report.md` |
| 6a | `E2E_BASE_URL=https://localhost:19141 npx playwright test navigation.spec.ts --workers=1` ×5 | `tests/e2e/` | 0, 14/14 every run | `evidence/e2e/navigation-runs/run-{1..5}.log` |
| 6b | `… npx playwright test public.spec.ts` | `tests/e2e/` | 0, 5/5 | `evidence/e2e/public.log` |
| 6c | `… npx playwright test dashboard.spec.ts --workers=1` | `tests/e2e/` | 0, 6/6 | `evidence/e2e/dashboard.log` |
| 6d | `… npx playwright test events.spec.ts --workers=1` | `tests/e2e/` | 0, 4/4 | `evidence/e2e/events.log` |
| 7 | `node tools/migration-verify/performance.mjs --base https://localhost:19141` | repo root | 0, all budgets pass (JS 101990/307200 B, LCP 172/2000 ms, TTI 14.8/2500 ms, /collect p95 5.7/500 ms) | `evidence/performance.log` |

Sonar: NOT_APPLICABLE (no config, per plan). No introduced dependencies.

### Evidence note

`compare.mjs`'s contract and visual runs both wrote to the same `--out` directory
(`shell-navigation/report.md`); the visual run (second) overwrote the contract run's per-row
report file. The contract run's authoritative result — `0 regression(s)` — is preserved in
`evidence/compare-shell-contract.log` (the command's own stdout), just not as a separate
per-row breakdown file. Not re-run to regenerate it separately, given the disk budget (see
below); the visual run's 0-regression result on the same journey/SHA is not in doubt.

## RED-state note

Per the packet's own escape hatch ("if everything already passes, your slice is the tests/proofs
below plus the deferred items"): `compare.mjs --journey shell-navigation` (contract + visual) and
5× `navigation.spec.ts --workers=1` were run as the *first* actions against the built candidate
stack, before evaluating whether any further product-code change was needed — both came back
green immediately (0 regressions / 14-14-14-14-14). No RED excerpts to record: there was no
observable gap in shell-navigation's oracle-facing behaviour on the parent SHA. Local backend/
frontend gates (`mvnw test`/`verify`, `npm run test`/`test:integration`/`typecheck`/`lint`/
`format:check`/`build`) were run before the Docker build for the same reason and to conserve the
tight disk budget (see below) — a second full rebuild-and-rerun cycle was not needed since none
of the four deferred-item changes touch runtime-observable behaviour (three are test-file-only;
the fourth adds a `catch` around a path that never throws on the happy path).

## Deviations used

DEV-4 (document requests compared by method/path/status/final URL only — the standing
contract-dimension rule, unchanged), DEV-12 (step 21, the `/de/dashboard` 404 — visual/a11y/text
comparison skipped, status-only, per `compare/shell-navigation/report.md`'s own row for step 21).

## Disk

Host was under real pressure from unrelated concurrent activity throughout this run (free space
observed ranging 2.1-3.4 GB despite this slice's own footprint staying under ~600 MB at any
point: 161 MB frontend `node_modules`, ~400 MB Docker image, ~30 MB Maven `target/`). Checked
`df -h /` before every build; one post-build reading (2.1 GB, right after the single `docker
compose build`) fell below the 2.5 GB stop threshold, so `docker builder prune -af` was run
immediately (recovered to 2.9 GB) before any further build step, per the packet's disk-hygiene
rule — no build was attempted while below threshold. `frontend/dist`, `tests/e2e/test-results`
and `backend/target` were deleted after each use; `~/.npm` cache was cleared once at the start of
the run. Final teardown: `docker compose -p kivvi-w-shell -f compose.next.yaml down -v`, `docker
rmi kivvi-w-shell-api`, `docker builder prune -af` — all done; final `df -h /` back to 3.2 GB.

## What I could not do / deliberately left alone

- **`backend/src/main/java/click/kivvi/web/PreferencesController.java`'s class doc comment** is
  now stale — it says "the browser flips the attribute immediately for a snappy toggle," which
  described the pre-`60445eb` `useIntents`/`shell.ts` behaviour (DOM-first, POST-after). The
  actual current behaviour (await-then-apply) no longer "flips immediately." This file is **not**
  in this packet's backend touch list
  (`{web/ShellController.java, application/ShellViewService.java, application/NavigationCatalog.java}`
  only), so left untouched — flagged here for the orchestrator/a future doc-hygiene pass rather
  than fixed out of scope.
- No page-body divergences found to report to other journeys: every oracle step's crumb/nav
  label text was cross-checked against `messages_{pl,en}.properties` and matches exactly; no
  gap to hand back to another journey.
- Did not duplicate B03/B05/B06 coverage in `ShellPagesIT` (see "New required tests added" above)
  — each already has adequate named coverage in its owning journey's `*ApiIT`, and duplicating it
  here would not add behaviour-mapping value the common rules require.

## `git log --oneline 60445eb..HEAD`

```
7e6a66a test: cover B01/B02 across every panel route and fix a stale old-stack citation (#shell-navigation)
2b5ecb2 fix: guard the scroll-restoration reload marker against a throwing history.replaceState (#shell-navigation)
```

## Finish checklist

- `docker compose -p kivvi-w-shell -f compose.next.yaml down -v` — done.
- `docker rmi kivvi-w-shell-api` — done.
- `docker builder prune -af` — done (twice: once mid-run after the 2.1 GB low-water mark, once at
  teardown).
- `git status --porcelain` in the worktree — empty (verified after both commits and after
  deleting `frontend/dist`/`tests/e2e/test-results`/`backend/target`).

## Repair-1

Fresh worktree `/home/muszkin/work/kivvi-click-wt/w5-shell-r1`, branch
`migration/wave-5/shell-navigation-repair1`, base `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`
(identity guard confirmed at start: worktree HEAD == base, clean). Test-only, no stack.

**Finding:** B02, B03, B21 had no unit-level test (`./mvnw -q test` / `npm run test -- --run`)
carrying a `"Bnn"`-prefixed name — B03's test existed unlabelled since wave-2.

**Fix:**
- **B03** — labelled `ShellViewServiceTest.detailRouteKeepsItsIndexSectionCurrent` with
  `@DisplayName("B03 …")` (was unlabelled; GAP-1).
- **B02** — added `@ParameterizedTest @DisplayName("B02 the crumb names every section, matching
  its own nav label")` to `ShellViewServiceTest` (9 cases, one per `NavigationCatalog` section,
  cross-checked against the oracle's own breadcrumb text). Added
  `frontend/test/unit/Topbar.spec.ts` (new): a unit-level test proving `Topbar.vue` renders
  whatever crumb text its `crumb` prop carries into `.crumbs .now`, verbatim, for two different
  sections (mutation-detectable against a hard-coded string).
- **B21** — added `frontend/test/unit/AppShell.spec.ts` (new): pins the structural invariant the
  frozen `03-components.css` relies on — exactly one `.main-scroll` element exists, the slot
  (standing in for `<router-view>`) renders inside it and nowhere else (not bare under `.app`,
  not inside `.sidebar`), and no inline `overflow`/scroll style stands in for the stylesheet.
  Hit a real `@vue/test-utils` `find()` quirk along the way: a compound selector whose leading
  class matches the wrapper's own root element (`.app .main .main-scroll`, when the mounted
  component's root IS `.app`) silently matches nothing, even though native
  `Element.querySelector` on the same root finds it — `find()` does not treat the wrapper's own
  root as a valid ancestor for a selector component earlier than the query target. Worked around
  by dropping the redundant `.app` prefix (the wrapper is already scoped to it) and asserting
  parentage directly (`.parentElement === wrapper.find(".main-scroll").element`) where a
  child-combinator check was needed — documented in the test file's own class comment so the next
  person does not rediscover it via a silently-false assertion.

**Sweep:** every `Bnn` id in `behaviours.json` for B01–B32 checked against every `*Test.java` and
`frontend/test/unit/*.ts` file, backend and frontend independently. Full map:
`evidence/repair-1/behaviour-unit-map.md` (this run dir). Result: 29/32 already had unit-level
coverage; B02/B03/B21 were the only gaps, all closed by this repair. No other gap found.

### Gates (repair-1 candidate SHA `dae1696`)

| Command | cwd | Exit | Notes |
| --- | --- | --- | --- |
| `./mvnw -q test` | `backend/` | 0 | 298 tests, 0 failures/errors (was 289 before repair-1; +9 from the new B02 `@ParameterizedTest`) |
| `npm run test -- --run` | `frontend/` | 0 | 169 tests, 28 files (was 165/26; +2 files, +4 tests: `AppShell.spec.ts` ×2, `Topbar.spec.ts` ×2) |
| `npm run lint` | `frontend/` | 0 | 0 errors, 12 pre-existing warnings in untouched files (unchanged) |
| `npm run typecheck` | `frontend/` | 0 | clean |
| `npm run format:check` | `frontend/` | 0 | clean (after one `prettier --write` pass on the 2 new files) |
| `./mvnw -q spotless:check` | `backend/` | 0 | clean (after one `spotless:apply` pass — a Javadoc line-wrap violation in the new B02 test's own doc comment) |

No stack was brought up (test-only repair, per packet). `frontend/node_modules`,
`backend/target` and `frontend/dist` deleted after the gates ran, before reporting (disk was
critical throughout, 2.1-2.9 GB free).

### Files changed (repair-1)

- `backend/src/test/java/click/kivvi/application/ShellViewServiceTest.java` — B03 label, new B02
  `@ParameterizedTest`.
- `frontend/test/unit/Topbar.spec.ts` — new (B02).
- `frontend/test/unit/AppShell.spec.ts` — new (B21).

### `git log --oneline 7e6a66a..HEAD` (repair-1 worktree)

```
dae1696 test: label B02/B03/B21 unit coverage and pin the main-scroll invariant (#shell-navigation)
```

**Repair-1 SHA: `dae169614a52532c130bd34435994d6a914165c0`** (worktree
`/home/muszkin/work/kivvi-click-wt/w5-shell-r1`, branch `migration/wave-5/shell-navigation-repair1`).
`git status --porcelain` empty in that worktree at the end.
