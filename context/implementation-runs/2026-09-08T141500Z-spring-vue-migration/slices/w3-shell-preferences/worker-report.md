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

## Repair-1

**Candidate SHA:** `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
**Parent SHA:** `60296f16250c6ebd25f7b4435c795268772fb6be`
**Branch:** `migration/wave-3/shell-preferences-repair1`
**Worktree:** `/home/muszkin/work/kivvi-click-wt/w3-shell-preferences-r1`

```
$ git log --oneline 60296f1..HEAD
11d3cc4 fix: serialize requests per session like PHP's session lock (#shell)
```

Identity guard passed at start (`git rev-parse HEAD` == parent SHA, `git status --porcelain`
empty). Working tree is clean after the commit (only the untracked `evidence/` directory remains,
same convention as the original slice). The compose stack was torn down (`down -v`) and the
`kivvi-w-shell-api` image was removed.

### Root cause

`keepalive: true` (repair round 2) fixed the client half of the race — the `POST
/preferences/theme` now always reaches the server — but not the server half: Spring Session JDBC
has no per-session lock. Two concurrent requests to the same session each `SELECT` their own copy
from `spring_session`/`spring_session_attributes` independently; if a reload's `GET
/api/v1/{locale}/shell` lands while an in-flight `POST /preferences/theme` has not yet reached
`SessionRepositoryFilter`'s own `finally` (where it writes the session back), the `GET` reads the
session as it was *before* that `POST`. PHP's native session handler avoids this by locking the
session file for the whole request, so the reload's read always blocked until the write finished —
parity the migration had not reproduced on the server side.

### Fix

Added `click.kivvi.infrastructure.session.SessionRequestSerializationFilter`, a plain
`OncePerRequestFilter` that acquires a per-session `java.util.concurrent.locks.ReentrantLock`
before calling `chain.doFilter(...)` and releases it in `finally`, serializing every request that
carries the same session cookie. It is registered (`SessionRequestSerializationConfig`) at
`Ordered.HIGHEST_PRECEDENCE + 10` — ten below Spring Boot's `SessionRepositoryFilter` default
(`HIGHEST_PRECEDENCE + 50`, `SessionProperties.Servlet#filterOrder`) — so it wraps *outside* it:
on the way in its `doFilterInternal` runs first and takes the lock before `SessionRepositoryFilter`
even starts; on the way out, `chain.doFilter(...)` does not return here until
`SessionRepositoryFilter`'s own `finally` (the session commit) has already run, so the lock is
never released before that commit completes. Locks live in a bounded, leak-free
`SessionLockRegistry`: a `ConcurrentHashMap<String, Entry>` where `Entry` pairs the
`ReentrantLock` with an `AtomicInteger` holder count, incremented in `acquire`/decremented in
`release`, with the entry removed from the map the instant the count reaches zero —
`ConcurrentHashMap.compute`/`computeIfPresent` make each of those operations atomic per key, so a
concurrent acquire and release on the same session id can never race into a leaked or
double-evicted entry. Lock acquisition is bounded by `kivvi.session-lock.timeout-ms` (named
constant `SessionRequestSerializationFilter.DEFAULT_LOCK_TIMEOUT_MS = 30_000`); exceeding it
responds `503 Service Unavailable` instead of ever deadlocking. A `kivvi.session-lock.enabled`
flag (default `true`) exists solely so the RED-proof test below can turn the fix off without a
second code path.

**Non-obvious finding, worth flagging explicitly:** the packet's suggested session-id resolution —
`request.getRequestedSessionId()` / `request.getSession(false)` — does not actually work in this
application. Both are answered by the servlet container's own (Tomcat) session support, which
only recognizes its own cookie name (`JSESSIONID` by default); this app's session cookie is named
`SESSION` (`server.servlet.session.cookie.name`), and since this filter runs *ahead* of Spring
Session's own filter, the request has not yet been wrapped by
`SessionRepositoryRequestWrapper` either — there is no Spring-Session-aware `getSession()` to defer
to at this point in the chain. Both APIs therefore silently returned `null` for every request,
making the filter a permanent no-op — a bug the unit tests did not catch, because they built
requests with `MockHttpServletRequest#setRequestedSessionId(...)` directly rather than a real
cookie. Fixed by reading the `SESSION` cookie directly off `request.getCookies()` (framework- and
wrapper-agnostic) and using its raw value verbatim as the lock key — it only needs to be a stable,
unique-per-session string, not the decoded `spring_session.session_id`. This is documented in the
filter's class Javadoc so the next reader does not repeat the same dead end.

A second bug found and fixed during the RED-proof cycle itself: the test-only POST-delay hook
(`kivvi.testing.preferences-theme-post-delay-ms`) originally slept *before* acquiring the lock,
so the delayed `POST` held no lock while sleeping and a concurrent `GET` raced in and won
immediately — the exact opposite of the intended effect. Moved the delay to inside the locked
section (after `tryLock` succeeds, before `chain.doFilter`), which is also the version that ships:
see `SessionRequestSerializationFilter.doFilterInternal`.

DOM-update-then-POST order and `keepalive: true` in `frontend/src/stores/shell.ts` were not
touched — this repair is backend-only.

### Filter order proof

`SessionRequestSerializationIT#sessionLockFilterWrapsOutsideSessionRepositoryFilter` autowires the
live `FilterRegistrationBean<SessionRequestSerializationFilter>` and the live `SessionProperties`
bean and asserts `lockFilterOrder < sessionRepositoryFilterOrder` directly against the running
context — not a hard-coded pair of constants on both sides:

| Filter | Order | Source |
| --- | --- | --- |
| `SessionRequestSerializationFilter` | `Ordered.HIGHEST_PRECEDENCE + 10` = `-2147483638` | `SessionRequestSerializationConfig.FILTER_ORDER` |
| `SessionRepositoryFilter` | `Ordered.HIGHEST_PRECEDENCE + 50` = `-2147483598` | `SessionProperties.Servlet#filterOrder` (Spring Boot default, read live from the context) |

Test passed — see `evidence/repair-1-gates/mvn-verify.log`.

### RED-then-GREEN proof

`SessionRequestSerializationRedProofIT` (kept `@Disabled` in the committed suite) is the same
scenario as the GREEN regression test, run with `kivvi.session-lock.enabled=false` and the same
`kivvi.testing.preferences-theme-post-delay-ms=400` delay. Manually enabled once to capture RED,
then restored to `@Disabled` before the final commit:

- **RED** (`kivvi.session-lock.enabled=false`): `withoutTheLockAReloadObservesTheStaleTheme`
  **failed** — `expected: "dark", but was: "light"` — reproducing the exact pre-fix race.
  Captured in `evidence/repair-1/red-proof.log`.
- **GREEN** (default, lock enabled): `SessionRequestSerializationIT
  #concurrentReloadSeesTheCommittedThemeNotTheStaleOne` **passed** with the identical fixture —
  see `evidence/repair-1-gates/mvn-verify.log`.

### Tests added

- `SessionLockRegistryTest` (unit, no Spring context) — same session id always returns the same
  `ReentrantLock` (and two threads holding it never overlap, proven by tracking max observed
  concurrency = 1); different session ids get independent locks and run fully concurrently
  (proven with a two-thread rendezvous latch that would time out if they shared a lock); an
  entry is evicted the instant its last holder releases it, and survives while any holder remains.
- `SessionRequestSerializationFilterTest` (unit, servlet mocks, real `SESSION` cookies — never
  `setRequestedSessionId`) — session-less requests pass through untouched; a successful request
  releases the lock and evicts the registry entry; `kivvi.session-lock.enabled=false` still lets
  requests through without ever touching the registry; exceeding the timeout responds `503` and
  never invokes the rest of the chain; the test-only POST delay only ever applies to `POST
  /preferences/theme`, never to other requests.
- `SessionRequestSerializationIT` (integration, Testcontainers Postgres, real HTTP) — the filter
  order proof above, and the GREEN concurrent-reload proof.
- `SessionRequestSerializationRedProofIT` (integration, `@Disabled`) — the RED proof, kept in the
  tree and reproducible on demand rather than only described in prose.

### Gate table

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` | `backend/` | 0 | `evidence/repair-1-gates/mvn-test.log` — 236/236 |
| 2 | `./mvnw -q verify` | `backend/` | 0 | `evidence/repair-1-gates/mvn-verify.log` — 305 run, 0 failures, 0 errors, 1 skipped (the RED-proof IT, by design); ArchUnit 5/5; spotless clean |
| 3 | `npm run test -- --run` | `frontend/` | 0 | `evidence/repair-1-gates/npm-test.log` — 128/128 (unchanged, no frontend files touched) |
| 4 | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/repair-1-gates/npm-test-integration.log` — 91/91 |
| 5 | `npm run lint` | `frontend/` | 0 | `evidence/repair-1-gates/npm-lint.log` — 0 errors, 6 pre-existing warnings unrelated to this repair |
| 6 | `npm run typecheck` | `frontend/` | 0 | `evidence/repair-1-gates/npm-typecheck.log` |
| 7 | `npm run format:check` | `frontend/` | 0 | `evidence/repair-1-gates/npm-format-check.log` |
| 8 | `npm run build` | `frontend/` | 0 | `evidence/repair-1-gates/npm-build.log` |
| 9 | `docker compose -p kivvi-w-shell -f compose.next.yaml up -d --build --wait` | repo root | 0 | `evidence/repair-1-gates/compose-up.log` — all three containers healthy |
| 10 | `compare.mjs --journey login --dimension contract` | repo root | 0 | `evidence/repair-1/compare-login-contract.txt` + `.../compare/login-contract/login/report.md` — **0 regressions** |
| 11 | `compare.mjs --journey shell-navigation --dimension contract` | repo root | 0 | `evidence/repair-1/compare-shell-navigation-contract.txt` — **0 regressions** |
| 12 | `performance.mjs --base https://localhost:19141` | repo root | 0 | `evidence/repair-1/performance.txt` — `collectP95Millis` 7.18 ms / 500 ms budget (unaffected by the lock), all four budgets pass |
| 13 | `E2E_BASE_URL=https://localhost:19141 npx playwright test navigation.spec.ts --workers=1` × 20, under simulated load | `tests/e2e/` | 1 each (expected) | `evidence/repair-1/load-runs/run-{1..20}.txt` + `baseline.txt` — see the 20-run table below |
| 14 | `... playwright test events.spec.ts --workers=1` × 3 | `tests/e2e/` | 0 each | `evidence/repair-1/other-specs/events-{1,2,3}.txt` — 6/6 each run (SSE + `/collect` unaffected by the new filter) |
| 15 | `... playwright test public.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-1/other-specs/public.txt` — 5/5 |
| 16 | `... playwright test settings.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-1/other-specs/settings.txt` — 12/12 |
| 17 | `... playwright test customers.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-1/other-specs/customers.txt` — 4/4 |
| 18 | `docker compose -p kivvi-w-shell -f compose.next.yaml down -v` + `docker rmi kivvi-w-shell-api` | repo root | 0 | `evidence/repair-1-gates/compose-down.log` — containers/volumes/network removed, image untagged and deleted |

### The 20-run load proof

Simulated load: `npm run build` looped continuously in `frontend/` (each cycle a fresh `vue-tsc
--noEmit && vite build`) for the full duration of all 20 runs, started before run 1 and stopped
only after run 20 completed. A baseline run (no load) was taken first.

| Run | Exit | `sidebar entry "popups"` | `sidebar entry "import"` | `theme toggle survives a reload` | `sidebar collapse survives a reload` | Total |
| --- | --- | --- | --- | --- | --- | --- |
| baseline | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed |
| 1–20 | 1 | ✗ fail | ✗ fail | ✓ pass | ✓ pass | 12 passed / 2 failed (identical on every run) |

**All 20 runs plus the baseline show exactly the two accepted failures** (`popups`/`import` — same
wave-4 gap as before, unrelated to this repair) **and nothing else.** Zero flakes on "theme toggle
survives a reload" or "sidebar collapse survives a reload" across 21 runs (baseline + 20), 42
assertions of the two target tests, all green — up from the round-3 evidence of 1/5 failing under
load before this fix.

### Out of scope / not touched (repair-1)

- `frontend/` — no files changed; `keepalive: true` and the DOM-update-then-POST order from the
  original slice and repair round 2 are untouched.
- No old-stack paths, no other journey's files, no push/merge.
- `evidence/` is left untracked in the worktree (not committed), matching the original slice's
  convention — only `backend/src/main/java/click/kivvi/infrastructure/session/*`,
  `backend/src/test/java/click/kivvi/infrastructure/session/*` and the two new
  `backend/src/test/java/click/kivvi/SessionRequestSerialization*IT.java` files were committed.

## Repair-2

**Candidate SHA:** `60445ebac139bcf1179b397c997600073a356e95`
**Parent SHA:** `71d884d2ed96a7a7caad18147e76570c9e113d4d`
**Branch:** `migration/wave-4/shell-preferences-repair2`
**Worktree:** `/home/muszkin/work/kivvi-click-wt/w3-shell-preferences-r2`

```
$ git log --oneline 71d884d..HEAD
60445eb fix: report a preference as applied only after the server committed it (#shell)
```

Identity guard passed at start (worktree was already provisioned at the expected base SHA/branch,
clean tree). Working tree is clean after the commit (only the untracked `evidence/` directory
remains). The compose stack was torn down (`down -v`) and the `kivvi-w-shell-api` image was
removed.

### Mechanism

Repair-1's per-session lock only orders requests that have already *arrived* at the server; it
cannot fix arrival *order* itself. A `keepalive: true` fetch is scheduled by the browser at its
lowest priority. On the wave-3 dashboard (light) this lost to nothing under isolated load, so
repair-1's fix looked complete; on the wave-4 dashboard (SSE subscription, canvas cardiogram, KPI
payloads — real weight now) an isolated `navigation.spec.ts --workers=1` run showed runs 4 and 5
failing "theme toggle survives a reload" and "sidebar collapse survives a reload": the reload's own
navigation and its assets were prioritised over the still-queued `keepalive` POST, so the POST
could reach the server *after* the reload's `GET /api/v1/{locale}/shell` — an arrival-order race no
amount of server-side locking can fix.

### Fix

In `frontend/src/stores/shell.ts`, both `setTheme` and `setSidebar` now `await` the
`/preferences/*` POST (wrapped in `try`/`catch`, so a rejected/aborted request still applies the
update — no console output, ever) **before** applying the DOM attribute and store state, not
after. This makes the causal order explicit: `data-theme`/`data-sidebar` cannot flip until the
fetch settles, so a reload issued any time after that point observes the committed value by
construction, regardless of the request's own dispatch priority. The request itself
(method/headers/body/`keepalive: true`) is untouched — contract guards below confirm the wire
bytes are unchanged. `useIntents.ts` needed no change: its `void`-fired dispatch pattern is still
correct, since the ordering guarantee now lives entirely inside the two store actions it calls.

### A second, pre-existing bug found and fixed en route (still inside the authorized file)

The packet's own proof list required 0 visual regressions on `shell-navigation` — first run showed
**7 regressions**, all on steps 13–14 (the sidebar-toggle/theme-toggle steps): `.nav-label`s (the
whole sidebar's text) rendered `display:none` even though `.app[data-sidebar]` had already reached
`"expanded"`. Before assuming this was caused by the await-reordering above, I built the candidate
from `71d884d` **with the await-reordering fix reverted** (the very same source tree, `git stash`)
and re-ran the identical `compare.mjs --dimension visual` check: **the same 7 regressions
reproduced identically on the unmodified parent commit.** This proved the bug pre-dates repair-2
entirely and is not caused by the RTT-before-DOM-update change.

Root cause (confirmed via a throwaway Playwright repro script, not committed): `.app`'s
`data-sidebar` attribute is kept reactive by `AppShell.vue`'s own template binding, but
`document.documentElement` (`<html>`)'s `data-sidebar` — stamped once by `SpaDocumentController`
at SSR time — was **never updated again on the client**; only `setTheme` had ever kept `<html>` in
sync (`document.documentElement.dataset.theme = theme`), `setSidebar` never did the equivalent.
`03-components.css`'s collapse rule is `[data-sidebar="collapsed"] .nav-label { display: none; }`
— a bare, unscoped attribute selector (frozen, byte-identical to the old stack's own
`assets/styles/03-components.css` — confirmed with `diff`, so the CSS itself is correctly out of
scope and was never touched) that matches *any* ancestor carrying that attribute, `<html>`
included. Sequence: reload while collapsed (journey step 12, `<html>` re-stamped `"collapsed"` by
SSR) → plain click back to `"expanded"` (step 13, `.app` updates, `<html>` does not) → the
selector still matches on the stale `<html>`, so labels stay hidden even though `.app` already
correctly reports `"expanded"`. Fixed by adding the missing
`document.documentElement.dataset.sidebar = state;` line to `setSidebar`, mirroring the pattern
`setTheme` already had — a one-line, in-scope, in-file fix, not a CSS change. Re-ran
`compare.mjs --dimension visual` against the rebuilt candidate: **0 regressions.** Unit tests
extended to assert `document.documentElement.dataset.sidebar` alongside the store state in all
four `setSidebar` cases (updates-and-posts, deferred-resolve, deferred-reject); mutation-proved
(see below) by removing the new line and confirming three tests fail for the right reason, then
restoring.

### Tests

`frontend/test/integration/shellStore.spec.ts` — four new cases: `setTheme`/`setSidebar` apply the
DOM attribute and store state only after a deferred fetch promise resolves (attribute/state
unchanged immediately after calling the action, changed only after the promise is resolved and
awaited); `setTheme`/`setSidebar` still apply the update when the POST rejects, and never call
`console.error`. The existing `setSidebar`-related cases were extended to also assert
`document.documentElement.dataset.sidebar` (previously only `shell.sidebar` was checked, which is
exactly how the pre-existing `<html>` bug above went unnoticed by the existing suite).
`frontend/test/unit/useIntents.spec.ts` — unchanged; its keepalive/dispatch-shape assertions are
unaffected by the reordering (the `fetch` call itself still happens synchronously inside the
click handler, before the first `await`).

**Mutation proof (manual, no automated mutation tool configured in this codebase):**
1. Reverted `setTheme` to apply the DOM/state update *before* the POST (old order) → the new
   `B08 setTheme applies the DOM attribute and store state only after the POST resolves` test
   failed (`expected 'light', received 'dark'`). Restored, re-ran: green.
2. Same for `setSidebar` → `B10 setSidebar applies the store state only after the POST resolves`
   failed (`expected 'expanded', received 'collapsed'`). Restored, re-ran: green.
3. Removed the new `document.documentElement.dataset.sidebar = state;` line → 3 tests failed for
   the right reason (`expected 'collapsed', received 'expanded'` on the update/deferred/reject
   cases). Restored, re-ran: 11/11 green.

### Gate table

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` (backend untouched, run for completeness) | `backend/` | 0 | `evidence/repair-2-gates/mvn-test.log` — 289/289 |
| 2 | `./mvnw -q verify` | `backend/` | 0 | `evidence/repair-2-gates/mvn-verify.log` — 379 run, 0 failures, 0 errors, 1 skipped (repair-1's RED-proof IT, by design) |
| 3 | `npm run test -- --run` | `frontend/` | 0 | `evidence/repair-2-gates/npm-test.log` — 164/164 |
| 4 | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/repair-2-gates/npm-test-integration.log` — 131/131 |
| 5 | `npm run lint` | `frontend/` | 0 | `evidence/repair-2-gates/npm-lint.log` — 0 errors, 12 pre-existing warnings, none in files touched here |
| 6 | `npm run typecheck` | `frontend/` | 0 | `evidence/repair-2-gates/npm-typecheck.log` |
| 7 | `npm run format:check` | `frontend/` | 0 | `evidence/repair-2-gates/npm-format-check.log` |
| 8 | `npm run build` | `frontend/` | 0 | `evidence/repair-2-gates/npm-build.log` |
| 9 | `docker compose -p kivvi-w-shell -f compose.next.yaml up -d --build --wait` | repo root | 0 | `evidence/repair-2-gates/compose-up.log` — all three containers healthy |
| 10 | `compare.mjs --journey login --dimension contract` | repo root | 0 | `evidence/repair-2/compare-login-contract.txt` — **0 regressions** |
| 11 | `compare.mjs --journey shell-navigation --dimension contract` | repo root | 0 | `evidence/repair-2/compare-shell-navigation-contract.txt` — **0 regressions** |
| 12 | `compare.mjs --journey login --dimension visual` | repo root | 0 | `evidence/repair-2/compare-login-visual.txt` — **0 regressions** |
| 13 | `compare.mjs --journey shell-navigation --dimension visual` | repo root | first run: 1 (7 regressions, pre-existing, see above) → after the `<html>` sync fix: 0 | `evidence/repair-2/compare-shell-navigation-visual.txt` (final, 0 regressions) — first-run report preserved at `evidence/repair-2/compare/shell-navigation-visual-retry/` for the record |
| 14 | `performance.mjs --base https://localhost:19141` | repo root | 0 | `evidence/repair-2/performance.txt` — `collectP95Millis` 6.45 ms / 500 ms, all four budgets pass |
| 15 | `E2E_BASE_URL=https://localhost:19141 npx playwright test navigation.spec.ts --workers=1` × 20, under simulated load | `tests/e2e/` | 0 each | `evidence/repair-2/load-runs/run-{1..20}.txt` + `baseline.txt` — see the 20-run table below |
| 16 | `... playwright test dashboard.spec.ts --workers=1` × 3 | `tests/e2e/` | 0 each | `evidence/repair-2/other-specs/dashboard-{1,2,3}.txt` |
| 17 | `... playwright test events.spec.ts --workers=1` × 3 | `tests/e2e/` | 0 each | `evidence/repair-2/other-specs/events-{1,2,3}.txt` |
| 18 | `... playwright test public.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-2/other-specs/public.txt` — 5/5 |
| 19 | `... playwright test settings.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-2/other-specs/settings.txt` — 12/12 |
| 20 | `... playwright test customers.spec.ts` | `tests/e2e/` | 0 | `evidence/repair-2/other-specs/customers.txt` — 4/4 |
| 21 | `docker compose -p kivvi-w-shell -f compose.next.yaml down -v` + `docker rmi kivvi-w-shell-api` | repo root | 0 | `evidence/repair-2-gates/compose-down.log` — containers/volumes/network removed, image untagged and deleted |

Note on ordering: gates 10–20 above were run twice — once against the build that first surfaced
the pre-existing `<html>`/sidebar bug, and again (rebuilding the image in between) against the
final candidate once that bug was fixed. Only the final, passing run's logs are kept under
`evidence/repair-2-gates/` and `evidence/repair-2/`; the mid-investigation visual report is kept
at `evidence/repair-2/compare/shell-navigation-visual-retry/` as the record of the reproduction.

### The 20-run load proof

Simulated load: `npm run build` looped continuously in `frontend/` for the full duration of all 20
runs, started before run 1 and stopped only after run 20 completed. A baseline run (no load) was
taken first. Unlike wave-3 (where `popups`/`import` still mounted `EmptyPageView`), wave-4's
dashboard/popups/import all have real pages now, so a clean run is 14/14, not 12/14.

| Run | Exit | `theme toggle survives a reload` | `sidebar collapse survives a reload` | Total |
| --- | --- | --- | --- | --- |
| baseline | 0 | ✓ pass | ✓ pass | 14 passed |
| 1–20 | 0 | ✓ pass | ✓ pass | 14 passed (identical on every run) |

**20/20 runs plus the baseline are all 14/14 — zero flakes** on either target test, under
continuous build-loop load, on the heavier wave-4 dashboard — up from the round-2 finding of 2/5
failing under isolated (not even simulated-heavy) load before this fix.

### Out of scope / not touched (repair-2)

- `backend/` — no files changed; gates re-run once for completeness per the packet, unchanged
  from repair-1.
- CSS (`frontend/src/styles/*.css`, `assets/styles/*.css`) — confirmed byte-identical between old
  and new stack (`diff`); the pre-existing bug found above was fixed entirely inside
  `shell.ts` (the authorized file), never in CSS.
- No old-stack paths, no other journey's files, no push/merge.
- `evidence/` left untracked, matching prior convention.
