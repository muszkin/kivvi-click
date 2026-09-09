# CUT-1 — post-cutover production verification

**Verdict: PASS**

Cutover to the Spring Boot + Vue stack (compose project `kivvi-click`, `compose.next.yaml` +
`compose.next.prod.yaml`, checkout `/home/muszkin/work/kivvi-click-wt/integration` @ `9205294`,
published on `http://localhost:23456` behind the TLS-terminating reverse proxy at
`https://kivvi.click`) was verified 2026-09-09T14:19Z–14:23Z, ~1–5 minutes after the recorded
cutover time (2026-09-09T14:18:42Z). All checks passed: header/cookie/redirect contract, the full
Playwright E2E suite (66/66), all four DEV-8 performance budgets, and clean `api`/`mercure`
container logs. No fix or rollback was attempted — this run is report-only, read-only throughout;
no compose stack or container was started, stopped, or restarted, and no tracked file was edited.

This verifier is read-only with one intentional exception required to execute
`performance.mjs` as instructed: it needs a local `frontend/dist/assets` build to measure
`initialJsGzipBytes`, which did not exist in the checkout, so `npm ci && npm run build` was run in
`frontend/` (gitignored `dist/`, no tracked file touched, no container involved) — see §3 and
Commands. `git status --porcelain` in the checkout was empty at the end, confirming no tracked
file was modified during this run.

## 1. HTTP contract: headers, cookies, redirects

Evidence: `cut1/step1-curl.txt`, `cut1/step1-login-curl.txt`, `cut1/step1-login-malformed-body.html`.

| Request | Expected | Actual | Verdict |
| --- | --- | --- | --- |
| `GET https://kivvi.click/pl` | 200 | `HTTP/2 200`, `content-type: text/html;charset=UTF-8` | PASS |
| `GET https://kivvi.click/pl/dashboard` | 200 | `HTTP/2 200`, `content-type: text/html;charset=UTF-8` | PASS |
| `GET https://kivvi.click/api/v1/pl/shell` | 200 | `HTTP/2 200`, `content-type: application/json`, `content-length: 20` | PASS |
| `GET https://kivvi.click/de/dashboard` | 404 (unsupported locale) | `HTTP/2 404`, `content-type: application/json` | PASS |
| `POST https://kivvi.click/pl/login` `_username=nie-adres&_password=x` (malformed e-mail) | 200, Polish validation message, same as old stack | `HTTP/2 200`, `content-type: text/html;charset=UTF-8`, body contains `To nie wygląda na poprawny adres e-mail.` (matches `EmailValidation.MALFORMED_MESSAGE` in `backend/src/main/java/click/kivvi/domain/EmailValidation.java`) | PASS |
| `POST https://kivvi.click/pl/login` `_username=anna@aureashop.pl&_password=hasło-testowe` (valid-looking, for cookie/redirect contract) | 302, session cookie `Secure; HttpOnly; SameSite=Lax`, relative/HTTPS `Location` | `HTTP/2 302`, `location: /pl/dashboard` (relative — no scheme, never `http://`), `set-cookie: SESSION=…; Path=/; Secure; HttpOnly; SameSite=Lax` | PASS |

No `Location` header on any request carried an absolute `http://` URL; the one redirect observed
(`/pl/dashboard`) was relative. Cookie flags matched exactly: `Secure; HttpOnly; SameSite=Lax`.
Cloudflare is fronting the TLS-terminating proxy (`server: cloudflare`, `cf-ray` present, `via: 1.1
Caddy` behind it) — expected topology, not a finding.

## 2. Playwright E2E suite (`E2E_BASE_URL=https://kivvi.click`)

`npm ci` in `tests/e2e` (cwd `tests/e2e`, exit 0, 3 packages, Chrome for Testing 151.0.7922.34
already present at `~/.cache/ms-playwright`). One spec file per invocation, `--workers=1` for
`events`, `dashboard`, `navigation` per instructions; default parallel workers for the rest.

| Spec file | Tests | Passed | Failed | Workers | Duration |
| --- | --- | --- | --- | --- | --- |
| `public.spec.ts` | 5 | 5 | 0 | 5 | 2.5s |
| `navigation.spec.ts` | 14 | 14 | 0 | 1 | 11.4s |
| `events.spec.ts` | 4 | 4 | 0 | 1 | 4.4s |
| `dashboard.spec.ts` | 6 | 6 | 0 | 1 | 4.5s |
| `customers.spec.ts` | 4 | 4 | 0 | 4 | 1.8s |
| `lists.spec.ts` | 5 | 5 | 0 | 5 | 2.0s |
| `editors.spec.ts` | 6 | 6 | 0 | 6 | 2.7s |
| `automations.spec.ts` | 4 | 4 | 0 | 4 | 2.0s |
| `settings.spec.ts` | 12 | 12 | 0 | 8 | 4.0s |
| `import.spec.ts` | 6 | 6 | 0 | 6 | 2.9s |
| **Total** | **66** | **66** | **0** | | |

All 10 runs exited 0. No failing test names, no flaky retries. `import.spec.ts` uploaded its small
demo CSV into the live import wizard as pre-approved; `performance.mjs` (below) posted 50 real
`purchase` events to `/collect` as pre-approved, since this is a demo app. Full stdout per file:
`cut1/e2e/<file>.log`.

## 3. Performance budgets (DEV-8)

Command: `node tools/migration-verify/performance.mjs --base https://kivvi.click`
(cwd `/home/muszkin/work/kivvi-click-wt/integration`, exit 0). It supports an HTTPS base directly
(`args.base` is a plain string, `chromium.launch({ channel: "chrome" })` with
`ignoreHTTPSErrors: true` for the LCP/TTI page load, and `NODE_TLS_REJECT_UNAUTHORIZED=0` scoped
to the `/collect` fetch loop) — not skipped.

It first failed once with `frontend/dist/assets not found` — `initialJsGzipBytes` is computed from
a **local** production build, not fetched from the deployed server, and no `frontend/dist` existed
in this checkout. Ran `npm ci && npm run build` in `frontend/` (`frontend/dist` is listed in
`.gitignore`; exit 0 both) to unblock the metric, then re-ran the script.

| Metric | Value | Budget | Verdict |
| --- | --- | --- | --- |
| `initialJsGzipBytes` | 101,990 B | ≤ 307,200 B | PASS |
| `lcpMillis` (`/pl/login`) | 288 ms | ≤ 2000 ms | PASS |
| `ttiMillis` (`domInteractive`, `/pl/login`) | 114 ms | ≤ 2500 ms | PASS |
| `collectP95Millis` (50× `POST /collect`) | 59.23 ms | ≤ 500 ms | PASS |

All four budgets passed with wide margin. Raw JSON: `cut1/step3-performance.txt`.

## 4. Container logs and stack status

`docker compose -p kivvi-click -f compose.next.yaml -f compose.next.prod.yaml ps`
(cwd `/home/muszkin/work/kivvi-click-wt/integration`, exit 0) — all three services `Up …
(healthy)`, started ~14:18–14:19Z, matching the 14:18:42Z cutover:

| Service | Container | Status | Ports |
| --- | --- | --- | --- |
| api | `kivvi-click-api-1` | Up (healthy) | 8080/tcp (internal) |
| database | `kivvi-click-database-1` | Up (healthy) | 5432/tcp (internal) |
| mercure | `kivvi-click-mercure-1` | Up (healthy) | `0.0.0.0:23456->80/tcp`, `[::]:23456->80/tcp` |

(`docker compose ... ps` printed a `POSTGRES_PASSWORD not set, defaulting to blank` warning —
an artifact of running the CLI without `--env-file .env.prod.docker` from this session; all three
containers are independently healthy, so this is not a runtime finding.)

`docker logs --since 20m kivvi-click-api-1` and `kivvi-click-mercure-1`, grepped case-insensitively
for `error|exception|warn|503`:

- **api** (35 lines total): 0 matches. Clean Spring Boot 4.1.1 / Java 25.0.4 startup — Tomcat on
  8080, Hikari pool up, Flyway applied migration `v1 - baseline` to an empty schema, scheduler
  heartbeat and `EventDedupCleanupJob` ticked once, dispatcher servlet initialized. All lines
  `INFO`. Evidence: `cut1/step4-api-logs-full.txt`, `cut1/step4-api-grep.txt` (empty).
- **mercure** (541 lines total): 11 raw grep matches, all explained:
  - 4× `warn` — Caddy/Mercure startup notices only (`Caddyfile input is not formatted`, `server is
    listening only on the HTTP port, so no automatic HTTPS will be applied`, `HTTP/2 skipped
    because it requires TLS`, `HTTP/3 skipped because it requires TLS`). Expected: this container
    terminates plain HTTP on port 80 (mapped to host 23456) and relies on the external
    Cloudflare/reverse-proxy layer for TLS — not a defect.
  - 7× `503` substring — all false positives inside timestamps (`…12.015503`) or a subscriber UUID
    (`…7eda5503e8ea`), confirmed by a full status-code tally: `200`×404, `202`×52 (the login demo
    JSON responses plus the 50 performance-script `/collect` POSTs), `302`×4 (my login POSTs),
    `404`×2 — both `GET`/`HEAD /de/dashboard`, i.e. the expected-404 check from §1. **Zero**
    `5xx` responses in the window.
  - 0 `error`, 0 `exception`.
  Evidence: `cut1/step4-mercure-logs-full.txt`, `cut1/step4-mercure-grep.txt`.

No error-, exception-, or 5xx-level findings in either container.

## Commands run (cwd / exit code)

| Command | cwd | Exit |
| --- | --- | --- |
| `curl -sI https://kivvi.click/pl` (+ `/pl/dashboard`, `/api/v1/pl/shell`, `/de/dashboard`) | n/a | 0 |
| `curl -sD - -X POST https://kivvi.click/pl/login --data-urlencode "_username=nie-adres" --data-urlencode "_password=x"` | n/a | 0 |
| `curl -sD - -X POST https://kivvi.click/pl/login --data-urlencode "_username=anna@aureashop.pl" --data-urlencode "_password=hasło-testowe"` | n/a | 0 |
| `npm ci` | `tests/e2e` | 0 |
| `E2E_BASE_URL=https://kivvi.click npx playwright test specs/<file>.spec.ts [--workers=1] --reporter=list` (×10: public, navigation¹, events¹, dashboard¹, customers, lists, editors, automations, settings, import) | `tests/e2e` | 0 (all) |
| `npm ci` | `frontend` | 0 |
| `npm run build` (`vue-tsc --noEmit && vite build`) | `frontend` | 0 |
| `node tools/migration-verify/performance.mjs --base https://kivvi.click` | `/home/muszkin/work/kivvi-click-wt/integration` | 1 first run (missing `frontend/dist`), then 0 |
| `docker compose -p kivvi-click -f compose.next.yaml -f compose.next.prod.yaml ps` | `/home/muszkin/work/kivvi-click-wt/integration` | 0 |
| `docker logs --since 20m kivvi-click-api-1` / `kivvi-click-mercure-1` | n/a | 0 |
| `rm -rf tests/e2e/node_modules frontend/node_modules frontend/dist` (cleanup) | `/home/muszkin/work/kivvi-click-wt/integration` | 0 |

¹ `--workers=1` per instructions.

## Failures

None. Every HTTP contract check, every E2E test (66/66), every performance budget (4/4), and
every container-log check passed. `git status --porcelain` in the checkout was clean at the end
(no tracked file modified).

## Cleanup

`node_modules` removed from `tests/e2e` and `frontend`; `frontend/dist` (the build created solely
to run `performance.mjs`, gitignored) removed too. No compose stack or container was started,
stopped, or restarted at any point in this verification; no tracked file was edited; no commit was
made.
