# Worker report — w3-shell-preferences (wave-3 repair round 2: preference POST lost on reload)

**Candidate SHA:** `60296f16250c6ebd25f7b4435c795268772fb6be`
**Parent SHA:** `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`
**Branch:** `migration/wave-3/shell-preferences`
**Worktree:** `/home/muszkin/work/kivvi-click-wt/w3-shell-preferences`

```
$ git log --oneline c74e3b8..HEAD
60296f1 fix: keep preference POSTs alive across an immediate reload (#shell)
```

Identity guard passed at start (`git rev-parse HEAD` == parent SHA, `git status --porcelain` empty).
Working tree is clean after the commit. The compose stack was torn down (`down -v`) and the
`kivvi-w-shell-api` image was removed.

## Root cause

`useIntents.ts`'s `set-theme` and `toggle-sidebar` handlers call `void shell.setTheme(...)` /
`void shell.setSidebar(...)` — deliberately not awaited, because the DOM update inside each store
action must happen synchronously, before the network request even starts (matching the old
stack's `assets/controllers/shell.ts`). Both actions then `await fetch("/preferences/...", {...})`
with a plain (non-`keepalive`) request. Per the Fetch standard, a normal `fetch` started from a
page that then unloads (a `page.reload()` issued immediately after the click, as
`navigation.spec.ts`'s "theme toggle/sidebar collapse survives a reload" tests do) can be aborted
by the browser before the request ever leaves the network stack. Under host load — more contention
for the event loop and the network layer between the click and the reload — this was lost outright
often enough to flake: wave-3 round-2's `navigation.spec.ts --workers=1` run showed "theme toggle"
failing 3/20 and "sidebar collapse" 1/20 (`waves/wave-3/round-2/e2e.md` +
`e2e/navigation-flake-investigation/`). The locale toggle needed no fix: it is a plain `<a href>`
real navigation (see the settings journey's own `Topbar.vue`/`localeHref.ts`), never a `fetch`, so
there is no request to lose.

## Fix

Added `keepalive: true` to both preference `fetch` calls in `frontend/src/stores/shell.ts`
(`setTheme` → `POST /preferences/theme`, `setSidebar` → `POST /preferences/sidebar`).
`keepalive: true` only changes the browser's own lifecycle handling of the request (making it
survive page unload, the same mechanism `navigator.sendBeacon` uses internally); it does not
change the request itself — method, headers and body are byte-identical to before, and the body is
a one-field JSON object, far under the keepalive 64 KB budget. The DOM-update-then-POST order was
not touched: `this.theme = theme; document.documentElement.dataset.theme = theme;` (and
`this.sidebar = state;`) still run before the `fetch` call in both actions, exactly as before and
as the oracle recorded.

No change was needed in `useIntents.ts` (the `void`-fired dispatch pattern is correct and
unchanged) or in a `frontend/src/api/*` client wrapper (none exists in this codebase — verified via
a repo-wide search before touching anything).

## Files changed

- `frontend/src/stores/shell.ts` (edited) — `keepalive: true` added to both preference POSTs, with
  a comment explaining the race and why the fix is request-shape-neutral.
- `frontend/test/integration/shellStore.spec.ts` (edited) — two new cases
  (`B08`/`B10` "…'s POST survives an immediate reload (keepalive: true)") asserting
  `expect.objectContaining({ keepalive: true })` on both preference POSTs; the two pre-existing
  cases were left untouched (they already assert method/body).
- `frontend/test/unit/useIntents.spec.ts` (new) — mounts a harness component that calls
  `useIntents()` and exercises the *real* dispatch path (`document.addEventListener("click", …)`,
  `data-action` delegation) rather than calling the store method directly, clicking
  `[data-action="set-theme"]`/`[data-action="toggle-sidebar"]` elements attached to
  `document.body` and asserting the resulting `fetch` call carries `keepalive: true`.

## Mutation proof

Manually removed `keepalive: true` from both `fetch` calls in `shell.ts`, re-ran the four new/
updated tests: all 4 failed (`useIntents.spec.ts`'s 2 cases and `shellStore.spec.ts`'s 2 new
cases), the 3 unrelated tests in the same files stayed green. Restored the fix, re-ran: all 7 pass.
This is the "mutation must fail" requirement, proved directly rather than via an automated
mutation-testing tool (none is configured in this codebase).

## Gate table

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` | `backend/` | 0 | `evidence/gates/backend-test.log` (no backend files touched; re-run for completeness) |
| 2 | `./mvnw -q verify` | `backend/` | 0 | `evidence/gates/backend-verify.log` |
| 3 | `npm run test -- --run` | `frontend/` | 0 | `evidence/gates/frontend-unit-test.log` — 128/128 (incl. the new `useIntents.spec.ts`, 2 cases) |
| 4 | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/gates/frontend-integration-test.log` — 91/91 (incl. `shellStore.spec.ts`'s 2 new cases) |
| 5 | `npm run lint` | `frontend/` | 0 | `evidence/gates/frontend-lint.log` — 0 errors, 6 warnings (all pre-existing `vue/multiline-html-element-content-newline`, none in the files touched here) |
| 6 | `npm run typecheck` | `frontend/` | 0 | `evidence/gates/frontend-typecheck.log` |
| 7 | `npm run format:check` | `frontend/` | 0 | `evidence/gates/frontend-format-check.log` (clean on the first pass — no reformatting needed) |
| 8 | `npm run build` | `frontend/` | 0 | `evidence/gates/frontend-build.log` |
| 9 | `compare.mjs --journey login --dimension contract` | repo root | 0 | `evidence/compare-login-contract.txt` + `.../compare/login-contract/login/report.md` — **0 regressions** (proves the `/preferences/theme`/`/preferences/sidebar` request bytes are unchanged) |
| 10 | `compare.mjs --journey shell-navigation --dimension contract` | repo root | 0 | `evidence/compare-shell-navigation-contract.txt` — **0 regressions** |
| 11 | `compare.mjs --journey login --dimension visual` (guard) | repo root | 0 | `evidence/compare-login-visual.txt` — **0 regressions** |
| 12 | `E2E_BASE_URL=... npx playwright test navigation.spec.ts --workers=1` × 10, under simulated load | `tests/e2e/` | 1 each (expected) | `evidence/load-runs/run-{1..10}.txt` — see the 10-run table below |
| 13 | `E2E_BASE_URL=... npx playwright test public.spec.ts` | `tests/e2e/` | 0 | `evidence/e2e-public.txt` — 5/5 |

### The 10-run load proof

Simulated load: `npm run build` looped continuously in `frontend/` (a fresh `vue-tsc --noEmit && vite build` cycle started again the instant the previous one finished; each cycle pins a CPU core for ~1–2 s) for the full duration of all 10 runs, started before run 1 and stopped only after run 10 completed. A baseline run (`evidence/nav-baseline.txt`, no load) was taken first and already showed the expected shape.

| Run | Exit | `sidebar entry "popups"` | `sidebar entry "import"` | `theme toggle survives a reload` | `sidebar collapse survives a reload` | Total |
| --- | --- | --- | --- | --- | --- | --- |
| baseline | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 1 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 2 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 3 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 4 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 5 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 6 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 7 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 8 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 9 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 10 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |

**10/10 runs show exactly the two accepted failures** (`popups`/`import` — these two routes still
mount `EmptyPageView` in this wave; their journeys land their own page bodies in wave-4, unrelated
to this repair) **and nothing else.** Zero flakes on "theme toggle survives a reload" or "sidebar
collapse survives a reload" across all 10 runs plus the baseline (11 runs total, 22 assertions of
the two target tests, all green).

## Out of scope / not touched

- `frontend/src/api/*` — no client wrapper exists in this codebase (confirmed via
  `find frontend/src/api`); nothing to change there.
- Locale toggle — no fix needed; it's a real `<a href>` navigation (see the settings journey's
  `Topbar.vue`), not a `fetch`, so it cannot lose a request to a reload race.
- No old-stack paths, no other journey's files, no push/merge.
