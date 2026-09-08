# Verifier summary — wave wave-0, dimension `contract`

**Wave SHA:** `3b17c07e23354e493edbab572e4090f69d2c0c71`
**Dimension status:** **PASS**
**Journey verdicts:**

| Journey | Verdict |
| --- | --- |
| login | accepted-deviation(DEV-4, DEV-5, DEV-9) |

No `regression` verdicts. Every `accepted-deviation` cites a deviation id present in the plan's
"Accepted deviations" table (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`) and in
`tools/migration-verify/deviations.json`, correctly scoped to the `login` journey.

## Checkout and stack

- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract`, detached at
  `3b17c07e23354e493edbab572e4090f69d2c0c71` (confirmed via `git rev-parse HEAD`), clean working tree.
- Oracle manifest hash recomputed: `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json`
  = `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`, matches the packet's recorded value.
- Stack: compose project `kivvi-int` — `docker compose -p kivvi-int ps` shows `api`, `database`, `mercure`
  all `healthy`. API image id `sha256:b845c28f25e29ed596ee78ea79f215a9ed6a20128e85c3ab3c87aa819ddbf77f`
  (image `kivvi-int-api`, created 2026-09-08T17:17:05Z). `GET https://localhost:19101/` → `200`, SPA
  document (`<html data-theme="light" ...><title>Kivvi-click</title>`) — confirmed the stack serves the SPA.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `.../verify-wave-0-contract/tests/e2e` | 0 |
| `npm ci` | `.../verify-wave-0-contract/tools/migration-verify` | 0 |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <evidence>/contract/login` | `.../verify-wave-0-contract` | 0 |

`VERIFY_COMPOSE` was overridden from the tool's default (`kivvi-w-login`) to the actual running
compose project `kivvi-int` per the packet's stack description; `db.json` counts came back non-zero,
confirming the override reached the right database container.

## compare.mjs result (`--dimension contract`)

`compare.mjs: 0 regression(s)`. Full report: `contract/login/login/report.md` and `report.json`.
Per-step verdicts: steps 1, 4–8 `accepted-deviation(DEV-4,DEV-5,DEV-9)` (steps 2–3 are `fill` actions,
no HTTP). `db.json` deltas: `sessions` oracle 0 → candidate 1 (`accepted-deviation(DEV-9)`, mapped to
`spring_session` — the packet's stated 0→1 tolerance); `cache_items` oracle 0 → candidate 0
(`accepted-deviation(DEV-5)`, mapped to `event_dedup`+`shedlock`).

## Independent manual audit

Compared every step's `http.jsonl` in `contract/login/login/steps/<n>/` against
`context/migration-oracle/symfony-to-spring-vue/journeys/login/steps/<n>/http.jsonl` by hand
(method, path, status, `Location`, `kind`):

- Step 1 `GET /pl/login` → 200: identical.
- Step 4 `POST /pl/login` → 302 `Location: /pl/dashboard`, then `GET /pl/dashboard` → 200: identical to
  oracle; candidate additionally makes `GET /api/v1/pl/shell?route=dashboard` (kind `xhr`, status 200).
  This extra call matches `/^\/api\/v1\//` and is excluded from the core-count comparison by DEV-4's
  rule ("API calls recorded per wave as the new baseline") — confirmed in `compare.mjs`'s
  `compareHttp()`, which partitions candidate entries into `core` (compared 1:1 against the oracle) and
  `apiBaseline` (`/api/v1/*`, recorded only). No unaccounted extra call.
- Step 5 `POST /pl/login` (empty credentials) → 200: identical.
- Step 6 `POST /pl/login` (malformed e-mail) → 200: identical.
- Step 7 `POST /pl/logout` → 302: identical (no `Location` recorded in either).
- Step 8 `GET /pl/dashboard` → 200, plus the same `/api/v1/pl/shell?route=dashboard` xhr as step 4 —
  same DEV-4 basis, no unaccounted extra call.

Preserved-endpoint requests (`POST /pl/login` ×3 [steps 4, 5, 6], `POST /pl/logout` ×1 [step 7]) all
have identical method, path and status to the oracle; the one 302 (step 4) has an identical `Location`.
No `/preferences/*` call occurs in the login journey. Document requests (steps 1, 4, 8) match
method+path+status per DEV-4. `db.json` deltas map per DEV-5/DEV-9 as above, sessions 0→1 tolerated.

## Direct probes (curl, against `https://localhost:19101`)

1. **`POST /pl/login` with malformed e-mail** (`_username=not-an-email`) → `HTTP/2 200`, body contains
   `data-login-error="To nie wygląda na poprawny adres e-mail."`. **Matches expectation** (200 + error
   attribute). Raw: `contract/probes/probe1-login-malformed-email.txt`, `probe1-body.html`.
2. **`POST /preferences/theme` with `{"theme":"neon"}`**, authenticated (logged in first via
   `POST /pl/login` with `anna@aureashop.pl` / `haslo-testowe` → `302 Location: /pl/dashboard`,
   `Set-Cookie: SESSION=...`, per DEV-9's cookie-name change) → `HTTP/2 200`, body `{"theme":"light"}`.
   **Matches expectation** — the invalid value is rejected server-side and the default is returned
   rather than echoed. Raw: `contract/probes/probe2-login.txt`, `probe2-theme.txt`, `probe2-body.json`.
3. **`GET /de/dashboard`** → `HTTP/2 404`. **Matches expectation**. Raw:
   `contract/probes/probe3-de-dashboard.txt`, `probe3-body.html`.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/contract/login/login/report.md`
- `.../contract/login/login/report.json`
- `.../contract/login/login/db.json`
- `.../contract/login/login/steps/{1,4,5,6,7,8}/http.jsonl`
- `.../contract/compare-contract-login.log`
- `.../contract/probes/probe1-login-malformed-email.txt` (+ `probe1-body.html`)
- `.../contract/probes/probe2-login.txt`, `probe2-theme.txt` (+ `probe2-body.json`, `cookies.txt`)
- `.../contract/probes/probe3-de-dashboard.txt` (+ `probe3-body.html`)

## Verdict

Dimension `contract`: **PASS**, bound to wave SHA `3b17c07e23354e493edbab572e4090f69d2c0c71`.
Journey `login`: **accepted-deviation(DEV-4, DEV-5, DEV-9)** — no missing, extra or changed call
without a deviation record; all three direct probes behaved as specified.
