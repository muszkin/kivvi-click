# RR-1 rehearsal verification — kivvi-stage @ http://localhost:23458

**Verdict: PASS**

- Checkout: `/home/muszkin/work/kivvi-click-wt/integration`, branch `migration/spring-vue`, `git rev-parse HEAD` = `92052947a6238ecf4ee177f4e72493122315ce32` (matches requested SHA `9205294`), working tree clean.
- Stack: compose project `kivvi-stage` (`compose.next.yaml` + `compose.next.prod.yaml`), containers `kivvi-stage-mercure-1` (0.0.0.0:23458->80, healthy), `kivvi-stage-api-1` (healthy), `kivvi-stage-database-1` (healthy). Plain HTTP, no TLS-terminating proxy in front.
- Old prod stack `kivvi-click` (ports 23456/23457) was not started, stopped, or otherwise touched during this run — verified below.
- Evidence root: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/rr1/`

## 1. HTTP smoke checks and cookie/header observations

Raw output: `rr1/step1-curl-headers.txt`, `rr1/step1-cookie-probe.txt`, `rr1/step1-login-cookie-probe.txt`, `rr1/shell_nocookie.json`, `rr1/shell_cookie.json`.

| Request | Status | Notable headers |
| --- | --- | --- |
| `GET /pl` | 200 | `Content-Type: text/html;charset=UTF-8`, `Via: 1.1 Caddy`, `Permissions-Policy: browsing-topics=()`. No `Set-Cookie`. |
| `GET /pl/dashboard` | 200 | Same SPA-shell document as `/pl` (client-side routed; server does not gate this route on a session). No `Set-Cookie`. |
| `GET /api/v1/pl/shell` | 200 | `Content-Type: application/json`. No `Set-Cookie` on a bare GET. |
| `POST /pl/login` (`_username=rehearsal@kivvi.click`) | 302 → `/pl/dashboard` | `Set-Cookie: SESSION=<id>; Path=/; Secure; HttpOnly; SameSite=Lax` |

**Cookie/TLS finding.** `backend/src/main/resources/application.yml` hard-codes `server.servlet.session.cookie.secure: true` with a comment stating the only entry point is the TLS-terminating Mercure edge, "hard-coded rather than left to `request.isSecure()`". RR-1 runs behind no such edge (plain HTTP on 23458), so every session cookie the app issues here still carries `Secure`.

Confirmed empirically that this cookie is issued: `POST /pl/login` returns `Set-Cookie: SESSION=...; Secure; HttpOnly; SameSite=Lax` even over plain `http://localhost:23458`. `curl` happily stores and replays a `Secure` cookie over `http://` (it does not enforce the attribute), so a curl-only smoke check cannot tell you whether a *browser* would actually keep this cookie — confirmed instead via the real request path: with the cookie replayed to `GET /api/v1/pl/shell`, the JSON `user` block changes from the anonymous/default fixture identity (`Maciej Kowalczyk / maciej@aureashop.pl`) to the session identity (`Rehearsal / rehearsal@kivvi.click`), so the app *is* session-aware here.

**Do the browser tests still work over plain HTTP, and why.** Yes. Playwright's Chrome did store and replay the `Secure` cookie across the login → dashboard redirect: `public.spec.ts › login › signs in and shows the identity in the sidebar` (which asserts `.sb-foot` shows the just-typed identity after the redirect) passed. This works specifically because the origin is literally `http://localhost:23458` — Chromium (and current Firefox) treat `localhost`/`127.0.0.1`/`[::1]` as a "potentially trustworthy" secure context per the W3C Secure Contexts spec's loopback exception, so a `Secure`-flagged cookie is still accepted and sent even without TLS, as long as the host is the loopback name. This is a narrow, host-specific exemption: it would **not** hold if the rehearsal (or a future cutover step) were reached by IP-that-isn't-loopback, a real hostname, or a reverse-proxied domain over plain HTTP without TLS — on any of those, real browsers would silently drop the `Secure` cookie and every session-dependent flow (login persistence, personalization) would break, even though `curl`-based smoke checks would look identical to what's captured here. This is worth flagging for CUT-1/production cutover: the hard-coded `secure: true` is only safe because production is always reached through the TLS-terminating Mercure edge — it must stay that way, and this RR-1 pass over `localhost` plain HTTP does not validate the cookie policy for any non-loopback plain-HTTP path.

## 2. Playwright E2E suite (`tests/e2e`, `E2E_BASE_URL=http://localhost:23458`)

`npm ci` in `tests/e2e` (cwd `tests/e2e`, exit 0, added 3 packages). System Chrome used (`channel: chrome`, `/usr/bin/google-chrome`). One file per invocation as requested; `--workers=1` for `events`, `dashboard`, `navigation`; `navigation` run twice.

| Spec file | Workers | Tests | Result | Duration | Exit |
| --- | --- | --- | --- | --- | --- |
| automations.spec.ts | default (4) | 4 | 4 passed | 1.9s | 0 |
| customers.spec.ts | default | 4 | 4 passed | 1.9s | 0 |
| editors.spec.ts | default | 6 | 6 passed | 2.4s | 0 |
| import.spec.ts | default | 6 | 6 passed | 2.6s | 0 |
| lists.spec.ts | default | 5 | 5 passed | 2.0s | 0 |
| public.spec.ts | default | 5 | 5 passed | 2.0s | 0 |
| settings.spec.ts | default | 12 | 12 passed | 4.1s | 0 |
| events.spec.ts | 1 | 4 | 4 passed | 4.2s | 0 |
| dashboard.spec.ts | 1 | 6 | 6 passed | 4.1s | 0 |
| navigation.spec.ts (run 1) | 1 | 14 | 14 passed | 9.6s | 0 |
| navigation.spec.ts (run 2) | 1 | 14 | 14 passed | 10.2s | 0 |

Totals: 66/66 unique tests passed (matches `playwright test --list`: "Total: 66 tests in 10 files"), 0 failed, 0 skipped, both `navigation` runs identical/stable (no flake). Full logs: `rr1/e2e/<file>.log` (each ends with an explicit `EXIT:0` line appended after the run).

No failures to report for this section.

## 3. migration-verify compare.mjs (contract + visual, 5 journeys)

`npm ci` in `tools/migration-verify` (cwd `tools/migration-verify`, exit 0, added 2 packages; reuses Playwright from `tests/e2e/node_modules` via `createRequire`, confirmed by reading `compare.mjs`). `compare.mjs` takes `--base` as a plain argument (`BASE = args.base.replace(...)`, `chromium.launch({ channel: "chrome", ignoreHTTPSErrors: true })`) — no hardcoded `https://` scheme or port `19101` found anywhere in the script outside the `--help`/usage example text on line 12; it works unmodified against `http://localhost:23458`. Nothing to report/flag on that front.

`VERIFY_COMPOSE="docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml"` was set for every run (overriding the script's own default of `-p kivvi-w-login`), used for the `db.json` row counts against the stage database — confirmed working (all 5 journeys report real `sessions`/`cache_items`/`var/import files` deltas mapped against `kivvi-stage-database-1`, not zeros-by-default-failure).

| Journey | Dimension | Verdict | Regressions | Notes |
| --- | --- | --- | --- | --- |
| login | contract | accepted-deviation only | 0 | DEV-4, DEV-5, DEV-9 (session table renamed to `spring_session`, cache/dedup remapped) |
| login | visual | parity | 0 | 0.000% pixel diff on every screenshot step |
| event-stream | contract | accepted-deviation only | 0 | DEV-3 (Mercure payload JSON not HTML — not diffable by design, verified elsewhere per deviations.json), DEV-4, DEV-5 |
| event-stream | visual | parity | 0 | max 0.017% pixel diff (216/1,296,000 px), well under the 0.5% threshold |
| import-wizard | contract | accepted-deviation only | 0 | DEV-4, DEV-5, DEV-9 |
| import-wizard | visual | parity | 0 | step 10 is an intentional 404 document, skipped per DEV-12; all other steps 0.000% diff |
| dashboard | contract | accepted-deviation only | 0 | DEV-4, DEV-5, DEV-9 |
| dashboard | visual | parity | 0 | 0.000% pixel diff on all 4 steps |
| shell-navigation | contract | accepted-deviation only | 0 | DEV-4, DEV-9 (21 steps) |
| shell-navigation | visual | parity | 0 | max 0.008% pixel diff (107/1,296,000 px); step 21 intentional 404 per DEV-12 |

All 10 runs exited 0 with `compare.mjs: 0 regression(s)`. No unaccepted contract or visual regressions found. Reports: `rr1/compare/<journey>-<dimension>-report.md` (per-dimension copies — `compare.mjs` writes a single `report.md` per journey that the next `--dimension` run overwrites, so each was copied out immediately after its run); full stdout/stderr in `rr1/compare/<journey>-<dimension>.log`.

No failures to report for this section.

## 4. Container logs (`api`, `mercure`)

Command: `docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml logs --no-color api mercure`, cwd `/home/muszkin/work/kivvi-click-wt/integration`, exit 0. Full capture: `rr1/step4-full-logs.txt` (1310 lines). Grep for `error|warn|exception|503` (case-insensitive): `rr1/step4-grep-findings.txt`.

- `error`: 0 matches. `exception`: 0 matches.
- `warn`: 6 raw matches, all 4 distinct messages are Mercure/Caddy **startup** warnings, all directly explained by running without the TLS-terminating proxy: `"server is listening only on the HTTP port, so no automatic HTTPS will be applied"`, `"HTTP/2 skipped because it requires TLS"`, `"HTTP/3 skipped because it requires TLS"`, `"Caddyfile input is not formatted"`. None from request handling; `api-1`'s 35 log lines are 25×`INFO` and 0×`WARN`/`ERROR`.
- `503`: 57 raw matches — all false positives, every one is an access-log field `"size":503,"status":200` (a 503-byte response body), never `"status":503`. Confirmed via `grep -oE '"status":[0-9]+' | sort | uniq -c`: 1029×200, 5×202, 20×302, 6×400, 12×404, **0×5xx**.
- The 6×400s are all `POST /collect` (deliberate malformed-payload assertions in the event-stream journey/tests). The 12×404s are 3×`/de/dashboard` (unsupported-locale check), 6×`/favicon.ico` (benign browser auto-request), 3×`/pl/import/5` (the intentional DEV-12 404 step exercised by both `import-wizard` and `shell-navigation` compare runs). None are unexplained.

No error-level findings; the only anomalies are the expected TLS-related startup warnings for this rehearsal's plain-HTTP topology.

## 5. Docker volumes

Command: `docker volume ls | grep kivvi` / `docker volume inspect <name>` / `docker system df -v`, exit 0 each. Full capture: `rr1/step5-volumes.txt`.

| Volume | Compose project label | Created | Size |
| --- | --- | --- | --- |
| `kivvi-next_database_data` | `kivvi-stage` | 2026-09-09T13:57:43Z (this rehearsal's fresh stack) | 65.88MB |
| `kivvi-next_mercure_config` | `kivvi-stage` | 2026-09-09T13:57:43Z | 1.035kB |
| `kivvi-next_mercure_data` | `kivvi-stage` | 2026-09-09T13:57:43Z | 262.3kB |
| `kivvi-click_database_data_prod` | `kivvi-click` | 2026-09-09T13:03:03Z (~55 min before this rehearsal started) | 65.76MB |
| `kivvi-click_caddy_data_prod` | `kivvi-click` | 2026-09-09T13:03:03Z | 264.1kB |
| `kivvi-click_caddy_config_prod` | `kivvi-click` | 2026-09-09T13:03:03Z | 1.584kB |
| `kivvi-click_database_data` | `kivvi-click` | 2026-09-09T13:03:03Z | 73.72MB |
| `kivvi-click_caddy_data` | `kivvi-click` | 2026-09-09T13:03:03Z | 51.81MB |
| `kivvi-click_caddy_config` | `kivvi-click` | 2026-09-09T13:03:03Z | 4.782kB |

Confirmed: the rehearsal stack (`kivvi-stage`) is entirely on its own `kivvi-next_*`-prefixed volumes, created fresh at stack start (13:57:43Z, "fresh empty database" premise holds — the 65.88MB is Flyway schema + fixture writes accumulated during this verification run, not a copy of prod data). The old prod stack's `kivvi-click_*_prod` volumes carry a `CreatedAt` of 13:03:03Z, ~55 minutes before `kivvi-stage` existed, and were not created, recreated, or resized by anything in this run (no `docker compose -p kivvi-click ...` command was issued at any point in this verification; only read-only `docker ps` / `volume ls` / `volume inspect` / `system df` touched that namespace). Container `kivvi-click-php-1` (ports 23456/23457) was never started, stopped, or restarted by this session. Also present but unrelated to either stack: a third compose project simply named `kivvi` (`kivvi_postgres_data`, `kivvi_redis_data`, `kivvi_redpanda_data`, all 0B, same 13:03:04Z timestamp) — untouched, out of scope.

**Non-interference check on the old prod stack.** `kivvi-click-php-1` (23456/23457) and `kivvi-click-database-1` were up and healthy throughout, never restarted. `kivvi-click-worker-1` was observed at `restartCount=1`, last started `2026-09-09T14:05:13Z` — exactly one hour after its prior start (`13:05:12Z`). Its own logs explain this: the `messenger:consume` worker is configured to "automatically exit once it has exceeded 128M of memory, been running for 3600s, or received a stop signal", and the container's `unless-stopped` restart policy brings it straight back up — a self-recycle built into the old stack, unrelated to this session (no `docker compose -p kivvi-click ...` or any command targeting ports 23456/23457 was ever issued here). Evidence: `rr1/step5-worker-non-interference-check.txt`.

## Commands run (cwd / exit code)

| Command | cwd | Exit |
| --- | --- | --- |
| `curl -sI http://localhost:23458/pl` (+ `/pl/dashboard`, `/api/v1/pl/shell`) | n/a (absolute URL) | 0 |
| `curl -sD - -X POST http://localhost:23458/pl/login --data-urlencode "_username=..."` (x2, plus cookie-jar replay) | n/a | 0 |
| `npm ci` | `tests/e2e` | 0 |
| `npm ci` | `tools/migration-verify` | 0 |
| `npx playwright test --list` | `tests/e2e` | 0 |
| `E2E_BASE_URL=http://localhost:23458 npx playwright test specs/<file>.spec.ts [--workers=1] --reporter=list` (×11: automations, customers, editors, import, lists, public, settings, events¹, dashboard¹, navigation¹×2) | `tests/e2e` | 0 (all) |
| `VERIFY_COMPOSE="docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml" node tools/migration-verify/compare.mjs --journey <j> --base http://localhost:23458 --dimension <contract\|visual> --out <evidence>/compare` (×10: login, event-stream, import-wizard, dashboard, shell-navigation × contract/visual) | `/home/muszkin/work/kivvi-click-wt/integration` | 0 (all) |
| `docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml logs --no-color api mercure` | `/home/muszkin/work/kivvi-click-wt/integration` | 0 |
| `docker volume ls`, `docker volume inspect <name>` (×9), `docker system df -v` | n/a | 0 |

¹ `--workers=1` per instructions.

## Failures

None. Every E2E test (66/66) and every compare.mjs dimension run (10/10) passed with no unaccepted regressions; no error/exception/5xx log lines; volumes correctly isolated.

## Cleanup

`node_modules` removed from both `tests/e2e` and `tools/migration-verify` at the end of this run per instructions (see below — done after this report was written). No tracked files were modified; no compose stack was started or stopped; ports 23456/23457 and project `kivvi-click` were never touched.
