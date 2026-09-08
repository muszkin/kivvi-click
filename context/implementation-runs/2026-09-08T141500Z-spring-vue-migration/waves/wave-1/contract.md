# Verifier report — wave-1, dimension `contract`, round 2

**Status: PASS** — bound to wave SHA `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`.

Oracle manifest verified: `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` = `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet).

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-contract`, `git rev-parse HEAD` = `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`, `git status --porcelain=v1` clean.

Stack: `docker compose -p kivvi-int ps` — api (`kivvi-int-api-1`, image id `7b7ac951c1ac`, healthy), database (postgres:18-alpine, healthy), mercure (v0.24.2, healthy), edge published on `19100`/`19101`.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| landing | parity | 5 steps, all `accepted-deviation(DEV-4)` (document kind → SPA shell + `/api/v1/*` baseline, per DEV-4's own rule — not a departure); db `sessions`/`cache_items` delta 0=0 both sides |
| feeds | parity | 1 step, `accepted-deviation(DEV-4)`; db delta 0=0 both sides |
| scheduler-heartbeat | parity | static journey (no browser steps, `capture/scenarios.json` confirms `"static": true, "steps": []`); verified via live DB + api log per packet instructions |

## Commands run

- `cd tests/e2e && npm ci` (cwd `.../verify-wave-1-contract/tests/e2e`) — exit 0
- `cd tools/migration-verify && npm ci` (cwd `.../verify-wave-1-contract/tools/migration-verify`) — exit 0
- `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19101 --dimension contract --out <evidence>/contract` (cwd `.../verify-wave-1-contract`) — exit 0, 0 regressions
- same for `--journey feeds` — exit 0, 0 regressions
- same for `--journey scheduler-heartbeat` — exit 0, 0 regressions (0 steps to replay)
- `docker compose -p kivvi-int exec -T database psql -U app -d app -tAc "select name, locked_by is not null, lock_until > locked_at from shedlock"` → `heartbeat|t|t`
- `docker compose -p kivvi-int logs api | grep -c "Scheduler heartbeat tick\."` → `1`
- `curl -sk -i https://localhost:19101/api/v1/pl/landing | head -5` → `HTTP/2 200`, `content-type: application/json`
- `curl -sk -o /dev/null -w '%{http_code}' https://localhost:19101/api/v1/de/landing` → `404`

## Hand-audit of http.jsonl vs oracle (DEV-4)

- **landing step 1** (`GET /pl`): oracle `200`, candidate `200`, path/method match. Candidate adds `xhr GET /api/v1/pl/landing 200` (api baseline, DEV-4). Same pattern for steps 2 (`/en`) and 4 (`/pl`).
- **landing step 3** (`GET /`): oracle `200`, candidate `200`, path `/` matches exactly (root, not `/pl`); candidate adds `xhr GET /api/v1/pl/landing 200`.
- **landing step 5** (demo click): oracle `GET /pl/demo` → `302` → `Location: /pl/dashboard`, then `GET /pl/dashboard` → `200`. Candidate: `GET /pl/demo` → `302`, `location: /pl/dashboard` (exact match), then `GET /pl/dashboard` → `200`, plus `xhr GET /api/v1/pl/shell?route=dashboard` (api baseline).
- **feeds step 1** (`GET /pl/feeds`): oracle `200`, candidate `200`, path match; candidate adds `xhr /api/v1/pl/shell?route=feeds` and `xhr /api/v1/pl/feeds` (api baseline). Spot-checked the feeds API body content beyond the tool's mechanical method/path/status scope: 4 sources, 4 feeds present, one feed `status:"error"` with `"HTTP 503 — Service Unavailable"`, top-level `mismatched: 142` — matches B30 exactly.
- Content-type header formatting differs cosmetically (`text/html; charset=UTF-8` vs `text/html;charset=UTF-8`) and several response headers present in the oracle (`cache-control`, `expires`, `x-robots-tag`) are absent from the candidate; `compareHttpEntry` in `compare.mjs` does not compare headers other than `Location` on 3xx per DEV-4's rule, so this is not a contract-dimension finding.

## DB deltas (DEV-5/DEV-9)

Both journeys: oracle `sessions`/`cache_items` delta 0, candidate `spring_session`/`event_dedup`+`shedlock` delta 0 — exact parity, no tolerance needed. Note: `compare.mjs`'s `compareDb()` applies the DEV-5/DEV-9 table mapping unconditionally to every journey's db.json rather than filtering by each deviation's journey list (DEV-9 lists only `login`/`shell-navigation`; DEV-5 lists `event-stream`/`scheduler-heartbeat`/`login`/`import-wizard` — neither lists `landing` or `feeds`). This is a tool-generality quirk, not a masked regression: the compared deltas are 0=0 on both sides in all cases, so no actual difference is being hidden by the mislabeling.

## scheduler-heartbeat static checks

- `shedlock` row `heartbeat`: `locked_by is not null` = `t`, `lock_until > locked_at` = `t`.
- `api` log: exactly 1 line `Scheduler heartbeat tick.` since container start (`c.k.i.scheduling.HeartbeatJob`), satisfying B33 ("missed runs execute once after restart" — one tick recorded since this compose-up).
- `/api/v1/pl/landing` → `200` JSON (baseline probe); `/api/v1/de/landing` → `404` (unknown-locale rule, DEV-4/DEV-12 adjacent).

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract/landing/{report.md,report.json,db.json,steps/1..5/http.jsonl}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract/feeds/{report.md,report.json,db.json,steps/1/http.jsonl}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract/scheduler-heartbeat/{report.md,report.json,manual-checks.log}`
- This file: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/contract.md`

No missing/extra/changed call and no db delta without a deviation id was found. Dimension `contract`: **PASS**.

## Note on evidence recovery

This evidence was originally written under a wrongly-nested `waves/wave-1/round-2/contract/` path; that whole `round-2/` tree (which by the time of writing also held stray `architecture/`/`integration/` output from other verifiers mid-run) was gone by the time the coordinator flagged the missing path — the sibling dimensions had by then consolidated their own round-2 output directly under `waves/wave-1/<dimension>/`, matching the packet's literal path. Per the coordinator's instruction not to re-run anything, every file placed at the corrected path above was reconstructed byte-for-byte from this verifier's own tool output already captured in-session, with two exceptions reconstructed (not byte-captured) from the same session's data: `landing/report.json` (fields derived from the captured `report.md` verdicts and the captured per-step `http.jsonl` xhr counts) and `scheduler-heartbeat/report.json` (minimal `{steps:[],regressions:0}`, consistent with the captured `report.md` and the journey's `"static": true, "steps": []` scenario definition — compare.mjs's own code path only attaches a `db` block when `desktopResult.delta` is set, which a zero-step journey does not produce). No dockers/curl/compare.mjs commands were re-run to produce this file; the stack was not touched.
