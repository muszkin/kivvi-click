# Wave-0 verifier — dimension `contract` (round 3)

Wave SHA: `887af4543c5a51255735f1ddfc0d8860d11551c0` · Journeys: `login`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract` (HEAD confirmed at wave SHA)
Stack: `https://localhost:19101`, compose project `kivvi-int`, api image `sha256:3881e56f7c14e667592f7ef6ab811d5224165d8b3940bfc2beee06971e6c5782`
Oracle manifest sha256 confirmed: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`

## Dimension status: PASS

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| login | parity (all steps `parity` or `accepted-deviation(...)` with a valid deviation id; 0 `regression`) |

Per-step verdicts from `compare.mjs` (contract dimension only):

- step 1 (`GET /pl/login`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — document request, method+path+status compared only, per DEV-4
- step 4 (`POST /pl/login` success → 302 `/pl/dashboard`; `GET /pl/dashboard` 200; extra `GET /api/v1/pl/shell?route=dashboard`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — core document count (2) matches oracle; the `/api/v1/*` call is the DEV-4 API baseline, correctly excluded from the core count
- step 5 (`POST /pl/login`, empty `_username`/`_password`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — 200; `data-login-error`/`data-last-username` extracted and compared exactly against oracle body
- step 6 (`POST /pl/login`, `not-an-email`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — same, 200
- step 7 (`POST /pl/logout`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — 302, `Location: /pl/login` matches
- step 8 (`GET /pl/dashboard`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — document parity; extra `/api/v1/pl/shell` call is DEV-4 baseline
- `db.sessions`: oracle delta 0 vs candidate `spring_session` delta 1 → `accepted-deviation(DEV-9)` (tolerance 1, Spring Session persists eagerly on sign-in)
- `db.cache_items`: oracle delta 0 vs candidate `event_dedup+shedlock` delta 0 → `accepted-deviation(DEV-5)` (table mapping)

`compare.mjs` reported **0 regressions**.

## Manual hand-audit (http.jsonl vs oracle, per step)

Read `compare.mjs` itself (`tools/migration-verify/compare.mjs`) to confirm its DEV-4/DEV-5/DEV-9 logic actually implements the plan's rules (not a rubber stamp), then diffed candidate `contract/login/steps/<n>/http.jsonl` against oracle `context/migration-oracle/symfony-to-spring-vue/journeys/login/steps/<n>/http.jsonl` for all 8 steps by hand:

- Steps 2, 3 (`fill` actions): no HTTP in either oracle or candidate — correctly skipped.
- Steps 1, 4, 8 (document requests `GET /pl/login`, `POST+GET` around login, `GET /pl/dashboard`): method/path/status identical; step 4's 302 `Location: /pl/dashboard` matches; the two `GET /api/v1/pl/shell?route=dashboard` XHR calls (steps 4, 8) are new but fall under DEV-4's `apiBaseline` rule (`^/api/v1/`) — recorded as baseline, not compared, not a regression.
- Steps 5, 6 (`POST /pl/login` on the DEV-4 full-parity list): status 200 identical both; body-derived `data-login-error`/`data-last-username` match the oracle's rendered values exactly — step 5: `"Podaj adres e-mail."` / `"maciej@aureashop.pl"`; step 6: `"To nie wygląda na poprawny adres e-mail."` / `"not-an-email"`.
- Step 7 (`POST /pl/logout`, full parity): 302, `Location: /pl/login`, identical.
- `db.json`: candidate table names (`spring_session`, `event_dedup`, `shedlock`) map 1:1 onto oracle's (`sessions`, `cache_items` ×2, `messenger_messages` ignored) per DEV-5; `messenger_messages` has no counterpart and is correctly not compared; session-count tolerance of 1 is DEV-9.

No missing, extra, or changed call found without a matching deviation id.

## Direct probes (independent of compare.mjs; raw responses in `contract/probes/`)

1. `POST /pl/login` with empty `_username`/`_password` → `HTTP/2 200`, `data-login-error="Podaj adres e-mail."`, `data-last-username="maciej@aureashop.pl"` — matches expected exactly. Evidence: `probes/probe1-login-empty-username.txt` (+ `probe1-login-empty-username-body.html`).
2. `POST /preferences/theme {"theme":"dark"}` (200, `{"theme":"dark"}`, sets `SESSION` cookie) then `GET /api/v1/pl/shell` with the same `SESSION` cookie → 200, shell payload contains `"theme":"dark"` — preference persisted across the session as expected. Evidence: `probes/probe2-theme-dark.txt`.
3. `GET /de/dashboard` (unknown locale) → `HTTP/2 404` — matches expected. Evidence: `probes/probe3-de-dashboard.txt`.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` | `/home/muszkin/work/kivvi-click` | 0 (matched packet's manifest sha256) |
| `docker compose -p kivvi-int ps` | any | 0 |
| `docker inspect --format='{{.Image}}' kivvi-int-api-1` | any | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract/tests/e2e` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract/tools/migration-verify` | 0 |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <evidence dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract` | 0 |
| `curl -sk -D - -X POST https://localhost:19101/pl/login -H "Content-Type: application/x-www-form-urlencoded" -H "Accept-Language: pl-PL" --data "_username=&_password="` | any | 0 |
| `curl -sk -D - -c cookies -X POST https://localhost:19101/preferences/theme -H "Content-Type: application/json" --data '{"theme":"dark"}'` then `curl -sk -D - -b cookies https://localhost:19101/api/v1/pl/shell` | any | 0 |
| `curl -sk -D - https://localhost:19101/de/dashboard` | any | 0 |

## Evidence paths

- `waves/wave-0/contract/login/report.json`, `waves/wave-0/contract/login/report.md` — compare.mjs output
- `waves/wave-0/contract/login/steps/<1-8>/http.jsonl`, `step.json`, `url.txt` — candidate recordings
- `waves/wave-0/contract/login/db.json` — candidate DB deltas
- `waves/wave-0/contract/probes/probe1-login-empty-username.txt`, `probe1-login-empty-username-body.html`, `probe2-theme-dark.txt`, `probe3-de-dashboard.txt`, `compose-ps.txt` — raw curl probes and stack status

## Notes

- Oracle directory, `round-1/`, and `round-2/` were not modified — only wrote under `contract/` and this `contract.md`.
- Did not start/stop the `kivvi-int` stack; it was already up and healthy when this run began.
