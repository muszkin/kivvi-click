# Wave-0 verifier — dimension `contract` (round 2)

Wave SHA: `6a53642c9c5ec9c256625854e6670f035e0292be` · Journeys: `login`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-contract` (HEAD confirmed at wave SHA)
Stack: `https://localhost:19101`, compose project `kivvi-int`

## Dimension status: PASS

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| login | parity (all steps `parity` or `accepted-deviation(...)` with a valid deviation id; 0 `regression`) |

Per-step verdicts from `compare.mjs` (contract dimension only), all bound to accepted deviations already in the plan's deviation table:

- step 1 (`GET /pl/login`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — document request, method+path+status compared only, per DEV-4
- steps 4–6 (`POST /pl/login` ×3: success, empty, malformed): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — full parity (method/path/status/Location/normalized body) confirmed by hand
- step 7 (`POST /pl/logout`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — full parity confirmed by hand (302, `Location: /pl/login`)
- step 8 (`GET /pl/dashboard`): `accepted-deviation(DEV-4,DEV-5,DEV-9)` — document request parity; extra `/api/v1/pl/shell` call recorded as DEV-4 API baseline, not a regression
- `db.sessions`: oracle delta 0 vs candidate `spring_session` delta 1 → `accepted-deviation(DEV-9)` (tolerance 1, sign-in persists session eagerly)
- `db.cache_items`: oracle delta 0 vs candidate `event_dedup+shedlock` delta 0 → `accepted-deviation(DEV-5)` (table mapping)

`compare.mjs` reported **0 regressions**.

## Manual hand-audit (http.jsonl vs oracle, per step)

Compared candidate `contract/login/steps/<n>/http.jsonl` against oracle `context/migration-oracle/symfony-to-spring-vue/journeys/login/steps/<n>/http.jsonl` for all 8 steps:

- No missing or extra calls beyond the DEV-4-sanctioned `/api/v1/*` baseline additions (steps 4 and 8).
- `POST /pl/login` (steps 4, 5, 6) and `POST /pl/logout` (step 7): method, path, status, and `Location` (steps 4, 7) identical; normalized body comparison (DEV-4's login-error/last-username extraction) matches oracle exactly on steps 5 and 6.
- Document requests (`GET /pl/login`, `GET /pl/dashboard` steps 1, 4, 8): method+path+status match; bodies correctly not compared per DEV-4.
- `db.json` deltas map 1:1 per DEV-5 (`sessions→spring_session`, `cache_items→event_dedup+shedlock`, `messenger_messages` ignored) and DEV-9 (session count tolerance 1).

No missing/extra/changed call found without a deviation id.

## Direct probes (raw responses in `contract/probes/`)

1. `POST /pl/login` with malformed e-mail → `HTTP/2 200`, `data-login-error="To nie wygląda na poprawny adres e-mail."`, `data-last-username="not-an-email"` — matches expected (200 + error attribute). Evidence: `probes/probe1-login-malformed-email.txt`.
2. `POST /preferences/sidebar` with `{"state":"sideways"}` → `HTTP/2 200`, body `{"state":"expanded"}` — matches expected. Evidence: `probes/probe2-sidebar-invalid-state.txt`.
3. `GET /de/dashboard` → `HTTP/2 404` (`{"status":404,"error":"Not Found","path":"/de/dashboard"}`) — matches expected. Evidence: `probes/probe3-de-dashboard.txt`.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `npm ci` | `.../verify-wave-0-contract/tests/e2e` | 0 |
| `npm ci` | `.../verify-wave-0-contract/tools/migration-verify` | 0 |
| `node tools/migration-verify/compare.mjs --help` | `.../verify-wave-0-contract` | 2 (usage banner; no `--help` flag implemented, expected) |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <evidence dir>` | `.../verify-wave-0-contract` | 0 |
| `docker compose -p kivvi-int ps` | `.../verify-wave-0-contract` | 0 |
| `docker inspect --format='{{.Image}}' kivvi-int-api-1` | any | 0 |
| `curl -sk -i -X POST https://localhost:19101/pl/login -H "Content-Type: application/x-www-form-urlencoded" --data "_username=not-an-email&_password="` | any | 0 |
| `curl -sk -i -X POST https://localhost:19101/preferences/sidebar -H "Content-Type: application/json" --data '{"state":"sideways"}'` | any | 0 |
| `curl -sk -i https://localhost:19101/de/dashboard` | any | 0 |

## Evidence paths

- `waves/wave-0/contract/login/report.json`, `waves/wave-0/contract/login/report.md` — compare.mjs output
- `waves/wave-0/contract/login/steps/<1-8>/http.jsonl`, `step.json`, `url.txt` — candidate recordings
- `waves/wave-0/contract/login/db.json` — candidate db deltas
- `waves/wave-0/contract/probes/probe1-login-malformed-email.txt`
- `waves/wave-0/contract/probes/probe2-sidebar-invalid-state.txt`
- `waves/wave-0/contract/probes/probe3-de-dashboard.txt`
- `waves/wave-0/contract/compose-ps.txt`
- `waves/wave-0/contract/api-image-id.txt` (`sha256:2c82ba59b017af539c48a2aafbfbe8698b581f2c62fddda1ad8fde6f42b64d1f`)

api image id: `sha256:2c82ba59b017af539c48a2aafbfbe8698b581f2c62fddda1ad8fde6f42b64d1f`
