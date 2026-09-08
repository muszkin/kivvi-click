# Verifier verdict — wave-0, dimension e2e

- Wave SHA: `3b17c07e23354e493edbab572e4090f69d2c0c71` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-e2e`, `git rev-parse HEAD` confirmed equal; checkout left unmodified — `git status --short` / `git diff --stat` both empty after all runs).
- Journeys: login.
- Stack: `https://localhost:19101` (compose project `kivvi-int`) — not started/stopped by this verifier.
- **Dimension status: PASS** (bound to `3b17c07e23354e493edbab572e4090f69d2c0c71`).
- **Journey verdict — login: parity.** All in-scope Playwright specs for `login` are green and all measurable DEV-8 performance budgets are met; no unmasked/unbudgeted failure exists.

## Playwright — test list and results

Run headless (no `--headed`; host has no display) via the system Chrome channel (`channel: "chrome"`, `/usr/bin/google-chrome`) — no browser download performed.

| Spec | Test | Result |
| --- | --- | --- |
| public.spec.ts | login › signs in and shows the identity in the sidebar | ✓ pass |
| public.spec.ts | login › the browser blocks a malformed address before it is sent | ✓ pass |
| navigation.spec.ts | app shell › sidebar collapse survives a reload | ✓ pass |
| navigation.spec.ts | app shell › theme toggle survives a reload | ✓ pass |
| public.spec.ts (full file) | landing › hero, preview frame, features, steps and pricing | ✘ fail — out-of-wave |
| public.spec.ts (full file) | landing › the demo button lands in the panel | ✘ fail — out-of-wave |
| public.spec.ts (full file) | landing › English landing keeps the same structure | ✘ fail — out-of-wave |

**CLI finding:** the packet's single literal command (`playwright test public.spec.ts -g "login" navigation.spec.ts -g "sidebar collapse|theme toggle"`) exits 0 but Playwright's CLI keeps only the *last* `-g/--grep` and applies it across every file argument — so it silently ran 0 tests from `public.spec.ts` (only the 2 navigation tests matched "sidebar collapse|theme toggle"). No spec file was touched; the two filters were re-run as separate invocations to actually execute both, and the full, unmodified `public.spec.ts` was also run standalone. Evidence for all three runs is kept (see below).

**Full `public.spec.ts` (unchanged) — failures outside login:** all 3 failures are in the `landing` describe block (`.hero h1` / `.hero-cta a` locators time out / not found — landing page markup doesn't exist on this SPA yet). Landing is wave-1 scope per the packet instruction — classified **out-of-wave, not a regression**. `login` tests inside the same full-file run also passed (2/2), consistent with the isolated run.

## Performance budgets (DEV-8, `tools/migration-verify/budget.json`)

`node tools/migration-verify/performance.mjs --base https://localhost:19101` (needed a local `frontend` build first — build output stays in this checkout, gitignored, not committed):

| Metric | Value | Budget | Result |
| --- | --- | --- | --- |
| initialJsGzipBytes | 62,765 B | 307,200 B | PASS |
| lcpMillis | 180 ms | 2000 ms | PASS |
| ttiMillis | 17.2 ms | 2500 ms | PASS |
| collectP95Millis | — | 500 ms | **skipped**, not a failure — `/collect` returns 404 (introduced wave-2); DEV-8 mechanism note in `deviations.json` covers this |

No budget exceeded; no deviation needed beyond the pre-recorded DEV-8 (whose mechanism is exactly `performance.mjs` + `budget.json`, already applied by using absolute thresholds).

## Commands run (cwd, exit code)

1. `git rev-parse HEAD` — cwd checkout root — exit 0, matches wave SHA
2. `npm ci` — cwd `tests/e2e/` — exit 0
3. `npm ci` — cwd `frontend/` — exit 0
4. `npm run build` — cwd `frontend/` — exit 0
5. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts -g "login" navigation.spec.ts -g "sidebar collapse|theme toggle"` (literal packet command) — cwd `tests/e2e/` — exit 0 (only 2/2 navigation tests actually selected, see CLI finding above)
6. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts -g "login" --reporter=list,html` — cwd `tests/e2e/` — exit 0, 2/2 passed
7. `E2E_BASE_URL=https://localhost:19101 npx playwright test navigation.spec.ts -g "sidebar collapse|theme toggle" --reporter=list,html` — cwd `tests/e2e/` — exit 0, 2/2 passed
8. `E2E_BASE_URL=https://localhost:19101 npx playwright test public.spec.ts --reporter=list,html` (full file, unchanged) — cwd `tests/e2e/` — exit 1, 2 passed / 3 failed (all landing, out-of-wave)
9. `npm ci` — cwd `tools/migration-verify/` — exit 0
10. `node tools/migration-verify/performance.mjs --base https://localhost:19101` — cwd checkout root — exit 0, all measured budgets pass

## Evidence paths

- `waves/wave-0/e2e/commands-run.log` — full command log with cwd/exit codes and the CLI-grep finding
- `waves/wave-0/e2e/targeted/run-literal-command.log` — output of the literal packet command as-given
- `waves/wave-0/e2e/targeted-login/run.log`, `targeted-login/playwright-report/`, `targeted-login/test-results/` — `public.spec.ts -g "login"` (2/2 pass)
- `waves/wave-0/e2e/targeted-navigation/run.log`, `targeted-navigation/playwright-report/` — `navigation.spec.ts -g "sidebar collapse|theme toggle"` (2/2 pass)
- `waves/wave-0/e2e/public-full/run.log`, `public-full/playwright-report/`, `public-full/test-results/` — full unmodified `public.spec.ts` (2 pass / 3 fail, out-of-wave, incl. screenshots + traces for each failure)
- `waves/wave-0/e2e/performance/performance.log` — `performance.mjs` JSON output vs `budget.json`
