# Dimension `contract` — wave-2 — verdict PASS

Bound to wave SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`.
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-contract` (HEAD confirmed == wave SHA).
Stack: `kivvi-int` at `https://localhost:19101`, api image `sha256:f01556f909c07c6b2e78dbbcc02b8ba760fb1f9f4c7c7a9176954458acf406bc` — see `contract/stack-ps.txt`.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| event-stream | accepted-deviation | DEV-3 (steps 2-3, Mercure JSON payload), DEV-4 (all steps, document/`/collect` full-parity rule), DEV-5 (db table mapping). 0 regressions. |
| customers | accepted-deviation | DEV-4 (all steps), DEV-12 (step 6, 404 status parity only — visual/body comparison skipped by design). 0 regressions. |

No journey verdict is `regression`; every `accepted-deviation` cites a deviation id present in the plan's Accepted deviations table and `tools/migration-verify/deviations.json`.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `.../verify-wave-2-contract/tests/e2e` | 0 |
| `npm ci` | `.../verify-wave-2-contract/tools/migration-verify` | 0 |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension contract --out <evidence>/event-stream` | `.../verify-wave-2-contract` | 0, "0 regression(s)" |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension contract --out <evidence>/customers` | `.../verify-wave-2-contract` | 0, "0 regression(s)" |
| `docker compose -p kivvi-int ps` | host | 0 |
| raw curl/psql probes (a)-(f) per packet | host | see `contract/probes.md` |

## Hand audit highlights

- event-stream steps 2-5 `/collect`: request/response bodies decode byte-identically to the oracle (202 `{"status":"accepted"}`, 200 `{"status":"duplicate"}`, both 400s with exact Polish messages). `compare.mjs` itself does not diff `/collect` bodies (its body-check branch only covers `/preferences/*` and `/{locale}/login` at status 200) — this was verified independently by hand, see `contract/probes.md`.
- `db.json` deltas: event-stream `event_dedup` +1 (one new idempotency id across steps 2-3, step 3 is the replay → 0 extra rows), mapped from oracle `cache_items` +1 per DEV-5; customers `event_dedup` +0. Both confirmed a second time via `select count(*) from event_dedup` before/after a fresh raw probe (3→4 for one new id).
- Mercure SSE (`/.well-known/mercure?topic=/accounts/1/events`): payload is `{"event":{...type,detail,customerName...}}` JSON, not HTML — confirms DEV-3.
- customers step 6 (`GET /pl/customers/c_9999`): 404 status parity holds; DEV-12 correctly skips visual/a11y/text comparison (oracle body is a Symfony dev-mode exception page, candidate is a minimal 404 document).
- Raw probe (f): `GET /api/v1/pl/customers/c_9999` returns 404 with an **empty body** (no JSON error shape), unlike `/collect`'s JSON error bodies. This is new `/api/v1/*` surface with no oracle counterpart (DEV-4 records it as the new baseline, not oracle-diffed), so it is **not** a contract-dimension FAIL — recorded as an observation in `contract/probes.md` for product follow-up.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/contract/event-stream/event-stream/{report.md,report.json,db.json,steps/1..7}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/contract/customers/customers/{report.md,report.json,db.json,steps/1..6}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/contract/probes.md`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/contract/stack-ps.txt`

## Status

**contract: PASS** — bound to wave SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`.
