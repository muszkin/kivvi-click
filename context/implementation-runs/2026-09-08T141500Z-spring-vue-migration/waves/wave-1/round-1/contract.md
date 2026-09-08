# Wave-1 verifier — dimension: contract

Wave SHA: `9427fdb1d92e4e606e67471638031d1bc0fb73d4`. Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-contract` (HEAD confirmed == wave SHA, clean, detached). Stack: compose project `kivvi-int`, edge `https://localhost:19101`.

## Status: PASS

| Journey | Verdict | Notes |
| --- | --- | --- |
| landing | parity | steps 1-5 method+path+status(+location) exact; extra `/api/v1/*` calls counted separately as baseline (DEV-4); db deltas 0/0 accepted per DEV-9/DEV-5 |
| feeds | parity | step 1 exact; 2 `/api/v1/*` baseline calls; db deltas 0/0 accepted per DEV-9/DEV-5 |
| scheduler-heartbeat | parity | static journey — no browser steps; candidate db.json shows steady-state deltas (0/0/0); oracle has no db.json for this journey (contract.md-only, static capture) so compare.mjs emits 0 `db` rows — read directly, confirmed by manual DB/log checks below |

Dimension-level: 0 `regression` verdicts across all three journeys; every non-`parity` verdict carries a deviation id present in `tools/migration-verify/deviations.json` (DEV-4, DEV-5, DEV-9). No missing/extra/changed call without a deviation id.

## Commands

All run from cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-1-contract` unless noted.

1. `git rev-parse HEAD` → `9427fdb1d92e4e606e67471638031d1bc0fb73d4`; `git status --short` → clean. Exit 0.
2. `cd tests/e2e && npm ci` — exit 0 (3 packages).
3. `cd tools/migration-verify && npm ci` — exit 0 (2 packages).
4. `docker compose -p kivvi-int ps` — 3 services `Up ... (healthy)`: `api` (image `kivvi-int-api`, id `sha256:a8dda84bb2fe2a5dcfceeaa5b00bd012110cab7bfa1b70c934ba7dbd95b4a54e`), `database` (postgres:18-alpine, port 33195), `mercure` (v0.24.2, ports 19100/19101). Exit 0.
5. `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19101 --dimension contract --out <evidence dir>` — `0 regression(s)`. Exit 0.
6. Same for `--journey feeds` — `0 regression(s)`. Exit 0.
7. Same for `--journey scheduler-heartbeat` — `0 regression(s)`. Exit 0.
8. `docker compose -p kivvi-int exec -T database psql -U app -d app -tAc "select name, locked_by is not null, lock_until > locked_at from shedlock"` → `heartbeat|t|t` (exactly one row, held lock, `lock_until > locked_at`). Exit 0.
9. `docker compose -p kivvi-int logs api | grep -c "Scheduler heartbeat tick\."` → `1` (single line, `HeartbeatJob : Scheduler heartbeat tick.` at `19:50:55.261Z`, INFO). Exit 0.
10. `curl -sk -i https://localhost:19101/api/v1/pl/feeds` → `HTTP/2 200`, `content-type: application/json`, JSON body with the feeds view-model keys (`kpis`, `sources`, `feeds`, `coverage`, `fallbackRules`, `mismatched`). Exit 0.

## Hand-audit vs oracle `http.jsonl`

- **landing** step 3 (`/`): oracle `GET / 200`; candidate `GET / 200` + one `/api/v1/pl/landing` baseline call. Step 5 (demo click): oracle `GET /pl/demo 302 → Location: /pl/dashboard` then `GET /pl/dashboard 200`; candidate identical (`302`, same `Location`, then `200`) plus one `/api/v1/pl/shell?route=dashboard` baseline call. Steps 1/2/4 (`/pl`, `/en`, `/pl` again) match method/path/status exactly, each with one extra `/api/v1/{locale}/landing` baseline call.
- **feeds** step 1: oracle `GET /pl/feeds 200`; candidate identical plus two baseline calls (`/api/v1/pl/shell?route=feeds`, `/api/v1/pl/feeds`).
- All `/api/v1/*` calls match the DEV-4 `apiBaseline` rule (`^/api/v1/`) — recorded as baseline, not compared 1:1 against the SSR-only oracle, and not counted toward the `core.length !== oracleEntries.length` regression check in `compare.mjs`.
- `db.json` deltas: `spring_session`/`event_dedup`+`shedlock` both 0 vs oracle 0 for landing and feeds (DEV-9/DEV-5 mapping). scheduler-heartbeat: candidate `before`==`after` (`spring_session:3, event_dedup:0, shedlock:1`) — no writes during the short capture window, consistent with `contract.md`'s "no database write besides the scheduler state key" and the direct `shedlock` query showing the pre-existing `heartbeat` row still locked.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract/{landing,feeds,scheduler-heartbeat}/{report.md,report.json,db.json,steps/...}`
- This file: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract.md`
