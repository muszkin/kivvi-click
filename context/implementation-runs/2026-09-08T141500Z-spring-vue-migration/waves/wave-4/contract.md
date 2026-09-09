# Wave-4 verifier — dimension: contract (round 3)

- Wave SHA: `60445ebac139bcf1179b397c997600073a356e95` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract`, detached, verified via `git rev-parse HEAD`; working tree clean throughout, no tracked files edited).
- Stack: `https://localhost:19101` (compose project `kivvi-int`), not started/stopped by this verifier. `docker compose -p kivvi-int ps` showed `api`, `database`, `mercure` all healthy for the full run.
- Oracle manifest sha256 confirmed: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet).
- Round-1/round-2 evidence NOT read, per packet instruction. This round supersedes.

## Dimension status: **PASS** (bound to wave SHA `60445eb`)

0 regressions across all six journeys replayed (three leading + three regression guards). No unmasked contract difference found; every accepted-deviation label maps to a deviation id in scope for this wave. No 5xx response anywhere in any recorded HTTP entry.

## Pre-flight: compare.mjs / deviations.json diff vs wave-3 (11d3cc4)

`git diff 11d3cc4 60445ebac139bcf1179b397c997600073a356e95 -- tools/migration-verify/` — confirmed by inspection (not by trusting the summary):

- `compare.mjs`: **only** the additive `var/import files` counter — `dbCounts()` now also calls a new `importFileCount()` helper (`docker compose exec -T api sh -c "find var/import -type f | wc -l"`) and adds it to the returned counts map. No other line changed. 19 insertions / 1 deletion, all inside `dbCounts()`.
- `deviations.json`: DEV-5 mapping gained `"var/import files": ["var/import files"]`; DEV-11 dropped the `login` journey/steps (closed at wave-4 per the plan's Accepted-deviations table — dashboard body now exists); DEV-12 gained `import-wizard: [10]`. All three changes match the packet's "Deviations in scope" list exactly; nothing else touched.

## Per-journey verdicts

| Journey | Role | Verdict | Deviation ids observed | Steps compared (oracle http.jsonl > 0) | DB deltas |
| --- | --- | --- | --- | --- | --- |
| popups-widget-editor | leading | **parity** (accepted-deviation) | DEV-4, DEV-7 | 1–7 (all 7) | sessions 0=0, cache_items 0=0, var/import files 0=0 |
| import-wizard | leading | **parity** (accepted-deviation) | DEV-4, DEV-5 | 1–10 (all 10) | sessions 1=1 (DEV-9 tol.), cache_items 0=0, **var/import files 1=1** |
| dashboard | leading | **parity** (accepted-deviation) | DEV-4, DEV-1/DEV-3 (scope; no contract-level mechanism triggered — see note) | 1–4 (all 4) | sessions 0=0, cache_items 0=0, var/import files 0=0 |
| login (guard) | regression guard | **parity** (accepted-deviation) | DEV-4, DEV-5, DEV-9 | 1, 4–8 (steps 2–3 have no oracle HTTP — fill-only, correctly excluded) | sessions oracle 0 / candidate 1 (DEV-9 tolerance ±1 — sign-in) |
| event-stream (guard) | regression guard | **parity** (accepted-deviation) | DEV-4, DEV-5, DEV-3 (scope; not a mechanical diff — see note) | 1–5, 7 (step 6 has no oracle HTTP — pause click only) | cache_items oracle 1 / candidate 1 |
| shell-navigation (guard) | regression guard | **parity** (accepted-deviation) | DEV-4, DEV-9 | 1–21 (all 21) | sessions 1=1 (DEV-9 tol.) |

No journey produced a `regression` verdict on any step or any `db` row. `report.json`'s `regressions` field is `0` for all six (independently re-read, not just the console line).

## Independent verification beyond trusting compare.mjs's own summary

Per the packet's "independently diff HTTP recordings and db.json deltas against the oracle," the following were hand-diffed against oracle `http.jsonl`/`step.json`, not just accepted from `report.md`:

- **import-wizard step 9**: candidate recorded 5 entries (`POST /import/upload xhr 302`, `GET /pl/import/2 xhr 200`, `GET /pl/import/2 document 200`, plus 2 `/api/v1/*` baseline calls); oracle recorded exactly 3. The 3 core (non-baseline) candidate entries match the oracle's 3 byte-for-byte on method/path/status, matching the packet's "step 9: 3 entries incl. the full document GET." The 2 extra are DEV-4 API-baseline calls, correctly excluded from the count comparison by `compareHttp()`'s `isApiBaseline()` filter.
- **import-wizard db.json**: `var/import files` oracle delta 1, candidate delta 1 — confirmed via `report.md` and cross-checked the underlying counts are computed inside the `api` container (`find var/import -type f`), consistent with DEV-5's note that this is a filesystem count, not a Postgres table.
- **import-wizard step 10 (DEV-12 404)**: candidate `GET /pl/import/5 document 404`, oracle identical; both `step.json` `documentStatus: 404`. DEV-12 is a **visual**-dimension deviation only (`dimension: "visual"` in `deviations.json`) — at the contract dimension it needs no special handling because DEV-4 already limits document-kind comparison to method/path/status/final-URL, and 404=404 satisfies that with no body compared. Confirmed no contract-level DEV-12 branch exists in `compare.mjs` (grep) and none was needed.
- **popups-widget-editor DEV-7**: grepped every recorded `http.jsonl` (both candidate and oracle, all steps) for `blocks` — zero matches on both sides, confirming the SPA editor makes no `POST …/blocks` call and the oracle never expected one either (dead call, R6).
- **event-stream DEV-3 (steps 2–3)**: confirmed `context/migration-oracle/.../capture/normalize.json`'s `ignore_paths` includes `^/\.well-known/mercure`, so the Mercure SSE payload is excluded from `http.jsonl` capture on both oracle and candidate sides — `compare.mjs` has nothing to mechanically diff for the JSON-vs-HTML payload shape. Verified the `/collect` request that triggers the publish is itself full-parity under DEV-4: step 2 candidate/oracle both `202 {"status":"accepted"}` (bodies byte-identical, request bodies differ only in the `__RUN__`-substituted idempotency id, which is not compared); step 3 both `200 {"status":"duplicate"}`. The DEV-3 label on these two steps in `report.md` is therefore informational (deviation is "in scope" per journey/step) rather than evidence of a masked difference — actual JSON-vs-HTML payload parity is owned by other dimensions' tests (`CollectApiIT`, `EventStream.spec.ts`), out of scope for the contract dimension, per `deviations.json`'s own note.
- **Round-3 addendum — preference POSTs awaited before DOM update**: hand-diffed the full recorded entries (not just status) for shell-navigation steps 11 (`POST /preferences/sidebar`) and 14 (`POST /preferences/theme`) against the oracle. Request body, response body, status, and content-type are **byte-identical**: `{"state":"collapsed"}` / `{"theme":"dark"}` on both sides, status 200 both sides. The only differing fields are response headers not compared by contract (`cache-control`, `expires`, `x-robots-tag` present only in the oracle capture — outside `compareHttpEntry`'s compared field set: method/path/status/Location/body-for-full-parity-paths). Confirms the `stores/shell.ts` change (await POST before applying DOM state) did not alter the wire contract.
- **No 5xx anywhere**: scanned every `http.jsonl` file across all six journeys' evidence (all steps, all entries) for `status >= 500` — zero matches.
- **Session count discipline (DEV-9)**: login shows oracle delta 0 / candidate delta 1 (accepted within the documented ±1 tolerance — Spring Session persists eagerly on sign-in); import-wizard and shell-navigation both show 1=1 exactly; popups-widget-editor, dashboard, event-stream show 0=0 exactly. No unexplained session growth.

## Commands run

All commands from cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract` unless noted, `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` exported for the `node tools/migration-verify/compare.mjs` calls.

| # | Command | cwd | Exit | Notes |
| --- | --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | worktree root | 0 | confirmed `60445ebac139bcf1179b397c997600073a356e95` |
| 2 | `git status --porcelain` | worktree root | 0 | clean, both before and after the run |
| 3 | `curl -sk https://localhost:19101/` | n/a | 0 | HTTP 200, stack reachable |
| 4 | `docker compose -p kivvi-int ps` | worktree root | 0 | api/database/mercure healthy |
| 5 | `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` | worktree root | 0 | matches packet's hash |
| 6 | `npm ci --no-audit --no-fund` | `tests/e2e` | 0 | 3 packages added |
| 7 | `npm ci --no-audit --no-fund` | `tools/migration-verify` | 0 | 2 packages added |
| 8 | `git diff 11d3cc4 60445eb -- tools/migration-verify/` | worktree root | 0 | inspected by hand (see pre-flight section) |
| 9 | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| 10 | `node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| 11 | `node tools/migration-verify/compare.mjs --journey dashboard --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| 12 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| 13 | `node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| 14 | `node tools/migration-verify/compare.mjs --journey shell-navigation --base https://localhost:19101 --dimension contract --out <contract evidence dir>` | worktree root | 0 | "0 regression(s)", no retry needed |
| — | `rm -rf tests/e2e/node_modules tools/migration-verify/node_modules` | worktree root | 0 | cleanup, run after evidence was written (see below) |

No cold-stack timeout occurred on any invocation (all six completed on the first attempt inside their 180–240s timeout budgets), so no retry-and-record-both case applies this round.

`<contract evidence dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/contract`

## Evidence paths

- `waves/wave-4/contract/popups-widget-editor/{report.md,report.json,db.json,steps/1..7/}`
- `waves/wave-4/contract/import-wizard/{report.md,report.json,db.json,steps/1..10/}`
- `waves/wave-4/contract/dashboard/{report.md,report.json,db.json,steps/1..4/}`
- `waves/wave-4/contract/login/{report.md,report.json,db.json,steps/1..8/}`
- `waves/wave-4/contract/event-stream/{report.md,report.json,db.json,steps/1..7/}`
- `waves/wave-4/contract/shell-navigation/{report.md,report.json,db.json,steps/1..21/}`
- This file: `waves/wave-4/contract.md`

## Environment / disk

Disk stayed at ~3.0 GB free throughout (`df -h /`). Only `tests/e2e/node_modules` (19M) and `tools/migration-verify/node_modules` (768K) were installed, scoped to the packet's instruction; both removed at the end of the run. No other checkout, the main repo, or the oracle was touched.
