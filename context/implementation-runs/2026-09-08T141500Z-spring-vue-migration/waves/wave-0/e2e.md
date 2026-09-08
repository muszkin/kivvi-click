# Verifier — wave wave-0, dimension e2e (round 3)

**Wave SHA:** `887af4543c5a51255735f1ddfc0d8860d11551c0`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-0-e2e` (HEAD == wave SHA, clean working tree — verified before any command)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`) — not started/stopped by this verifier
**Browser:** system Chrome via Playwright `channel: "chrome"`, headless (project config, no `headless: false` override; confirmed `/usr/bin/google-chrome-stable` on host)

## Dimension status: **PASS**

## Journey verdict

| Journey | Verdict |
| --- | --- |
| login | parity |

Both tests in `public.spec.ts`'s `login` describe block pass, both targeted `navigation.spec.ts` shell tests pass, and the frontend performance budgets (DEV-8) are met. The 3 failures found by the full `public.spec.ts` run are confined to the `landing` describe block (marketing page), which is out-of-wave — landing ships in wave-1 per the packet instruction — and are classified as out-of-wave, not a login regression.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e` | 0 |
| 2 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts -g "login" --reporter=list,json,html` | `tests/e2e` | 0 |
| 3 | `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse\|theme toggle" --reporter=list,json,html` | `tests/e2e` | 0 |
| 4 | `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts --reporter=list,json,html` (full file, out-of-wave classification pass) | `tests/e2e` | 1 (3 landing failures, 2 login pass — see below) |
| 5 | `npm ci` | `tools/migration-verify` | 0 |
| 6 | `npm ci` | `frontend` | 0 |
| 7 | `npm run build` (`vue-tsc --noEmit && vite build`) | `frontend` | 0 |
| 8 | `node tools/migration-verify/performance.mjs --base https://localhost:19101` | repo root (checkout) | 0 |

## Test list and results

**public.spec.ts -g "login" (invocation #2, in-scope):**
- `login › signs in and shows the identity in the sidebar` — PASS (878ms)
- `login › the browser blocks a malformed address before it is sent` — PASS (935ms)

**navigation.spec.ts -g "sidebar collapse|theme toggle" (invocation #3, in-scope):**
- `app shell › sidebar collapse survives a reload` — PASS (645ms)
- `app shell › theme toggle survives a reload` — PASS (678ms)

**public.spec.ts full run (invocation #4, out-of-wave classification):**
- `login › signs in and shows the identity in the sidebar` — PASS
- `login › the browser blocks a malformed address before it is sent` — PASS
- `landing › hero, preview frame, features, steps and pricing` — FAIL (`.hero h1` not found) — **out-of-wave**: landing page is wave-1 scope, not present at wave-0 SHA
- `landing › the demo button lands in the panel` — FAIL (30s timeout waiting for `.hero-cta a`) — **out-of-wave**, same reason
- `landing › English landing keeps the same structure` — FAIL (`.hero h1` not found) — **out-of-wave**, same reason

All 3 failures are inside the `landing` describe block; none touch `login`. No deviation id required for out-of-wave failures per packet instruction (landing ships wave-1).

## Performance vs budget (DEV-8, `tools/migration-verify/budget.json`)

| Metric | Value | Budget | Result |
| --- | --- | --- | --- |
| initialJsGzipBytes | 62,765 B | 307,200 B | PASS |
| lcpMillis (`/pl/login`) | 160 ms | 2,000 ms | PASS |
| ttiMillis | 14.2 ms | 2,500 ms | PASS |
| collectP95Millis | skipped — `/collect` not introduced until wave-2 (script's own note) | 500 ms | N/A, not in scope |

No budget exceeded; no deviation id needed.

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/e2e/`:
- `npm-ci-e2e.log`, `npm-ci-migration-verify.log`, `npm-ci-frontend.log`
- `login-run.log`, `playwright-report-login/`, `test-results-login/`
- `navigation-run.log`, `playwright-report-navigation/`, `test-results-navigation/`
- `public-full-run.log`, `playwright-report-public-full/`, `test-results-public-full/` (includes traces/screenshots for the 3 out-of-wave landing failures)
- `frontend-build.log`
- `performance.log`
