# Verifier report — dimension e2e — wave-0 (round 2)

**Wave SHA:** 6a53642c9c5ec9c256625854e6670f035e0292be
**Checkout:** /home/muszkin/work/kivvi-click-wt/verify-wave-0-e2e (HEAD confirmed == wave SHA)
**Stack:** https://localhost:19101 (compose project kivvi-int) — reachable (HTTP 200)

## Dimension status: PASS

## Journey verdicts

| Journey | Verdict | Notes |
|---|---|---|
| login | parity | Both targeted specs pass; full `public.spec.ts` login describe block passes (2/2). No deviation needed. |

## Test list and results

Targeted, per-invocation runs (Playwright `-g` keeps only the last grep, so each ran as a separate `npx playwright test` call):

- `public.spec.ts -g "login"` — 2/2 passed
  - login › signs in and shows the identity in the sidebar — PASS
  - login › the browser blocks a malformed address before it is sent — PASS
- `navigation.spec.ts -g "sidebar collapse|theme toggle"` — 2/2 passed
  - app shell › sidebar collapse survives a reload — PASS
  - app shell › theme toggle survives a reload — PASS

Full-file run for out-of-wave classification:

- `public.spec.ts` (no filter, 5 tests total) — 2 passed, 3 failed
  - login › signs in and shows the identity in the sidebar — PASS
  - login › the browser blocks a malformed address before it is sent — PASS
  - landing › hero, preview frame, features, steps and pricing — FAIL (`.hero h1` not found, PL landing markup absent)
  - landing › the demo button lands in the panel — FAIL (timeout waiting for `.hero-cta a` "demo")
  - landing › English landing keeps the same structure — FAIL (`.hero h1` not found, EN landing markup absent)

**Classification:** all 3 failures are in the `landing` describe block, not `login`. Landing ships in wave-1 per the packet, so these are **out-of-wave, not regressions**. The `login` describe block (the only in-scope journey for wave-0) is 2/2 green in both the targeted run and the full-file run.

## Performance vs budget (tools/migration-verify/performance.mjs, DEV-8)

Ran against https://localhost:19101 after `frontend/` build (dist/assets: JS 175.13 kB / gzip 63.48 kB; CSS 45.85 kB / gzip 8.67 kB):

| Metric | Value | Budget | Result |
|---|---|---|---|
| initialJsGzipBytes | 62,765 B | 307,200 B | PASS |
| lcpMillis | 188 ms | 2000 ms | PASS |
| ttiMillis | 25.4 ms | 2500 ms | PASS |
| collectP95Millis | skipped | 500 ms | skipped — `/collect` not introduced until wave-2 (script's own note); not applicable to wave-0 |

No budget exceeded; no deviation id needed beyond DEV-8 (the mechanism entry itself), which already covers absolute-threshold performance checking.

## Commands run (cwd, exit code)

1. `git rev-parse HEAD` — cwd `.../verify-wave-0-e2e` — exit 0 → matches wave SHA
2. `curl -k https://localhost:19101` — exit 0 → HTTP 200
3. `npm ci` — cwd `.../verify-wave-0-e2e/tests/e2e` — exit 0
4. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts -g "login"` — cwd `.../tests/e2e` — exit 0
5. `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse|theme toggle"` — cwd `.../tests/e2e` — exit 0
6. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts` — cwd `.../tests/e2e` — exit 1 (3 landing failures, out-of-wave; login block green)
7. `npm ci` — cwd `.../verify-wave-0-e2e/tools/migration-verify` — exit 0
8. `npm ci` — cwd `.../verify-wave-0-e2e/frontend` — exit 0
9. `npm run build` — cwd `.../verify-wave-0-e2e/frontend` — exit 0
10. `node tools/migration-verify/performance.mjs --base https://localhost:19101` — cwd `.../verify-wave-0-e2e` — exit 0

All runs used the `chrome` Playwright project (system Chrome channel, headless — no display on this host).

## Evidence paths

- `e2e/commands-run.log` — full command log with cwd/exit codes
- `e2e/npm-ci-e2e.log`
- `e2e/targeted-login/run.log`, `e2e/targeted-login/test-results/`
- `e2e/targeted-navigation/run.log`, `e2e/targeted-navigation/test-results/`
- `e2e/public-full/run.log`, `e2e/public-full/test-results/` (traces + screenshots for the 3 out-of-wave landing failures)
- `e2e/performance/npm-ci-migration-verify.log`
- `e2e/performance/npm-ci-frontend.log`
- `e2e/performance/frontend-build.log`
- `e2e/performance/performance-run.log` (raw JSON budget comparison)

Round-1 evidence under `round-1/` was not read or modified.
