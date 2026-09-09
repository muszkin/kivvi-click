# Verifier report — wave FINAL (all journeys, round 2), dimension contract

**Wave SHA:** `dae169614a52532c130bd34435994d6a914165c0`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-final-contract` (detached HEAD, confirmed `git rev-parse HEAD` == wave SHA)
**Stack under test:** `https://localhost:19101` (compose project `kivvi-int`); source checkout that built the running images is a *different* worktree, `/home/muszkin/work/kivvi-click-wt/verify-final`, independently confirmed at the same SHA `dae169614a52532c130bd34435994d6a914165c0` — the running stack matches this dimension's wave SHA. Stack was already running; not started or stopped by this verifier.
**Independence:** no worker reports, review files, or round-1 evidence were read. All verdicts below come from this verifier's own `compare.mjs` runs, its own reading of `journeys/*/steps/*/http.jsonl` in the oracle and in this run's output, and its own read-only `psql`/`docker logs` queries.

## Dimension status: **PASS**

All 13 journeys are `parity` / `accepted-deviation`. Zero `regression` verdicts. Zero 5xx responses across 94 recorded HTTP entries (226×200, 1×202, 4×302, 2×400, 4×404 — see "5xx sweep" below). No journey required a retry (no cold-stack timeouts encountered — every `compare.mjs` invocation succeeded on the first attempt).

## Per-journey verdicts

| Journey | Verdict | Deviation ids used | Steps compared | Notes |
| --- | --- | --- | --- | --- |
| login | accepted-deviation | DEV-4, DEV-5, DEV-9 | 6/8 (steps 2–3 have no oracle HTTP entries — form-fill only, correctly skipped) | 0 regressions |
| landing | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 5/5 | 0 regressions |
| feeds | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 1/1 | 0 regressions |
| scheduler-heartbeat | accepted-deviation | DEV-5, DEV-6 (verified independently, not via compare.mjs — see below) | n/a (static journey, 0 browser steps) | see "scheduler-heartbeat" section |
| event-stream | accepted-deviation | DEV-3, DEV-4, DEV-5, DEV-9 | 6/7 (step 6 is a click with no oracle HTTP entry) | every `/collect` case byte-identical; DEV-13 **unused** — see below |
| customers | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 6/6 | includes DEV-12 404 step (6); contract still checked status 404 exactly, matched |
| automations | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 7/7 | 0 regressions |
| settings | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 10/10 | includes DEV-12 404 step (10), matched |
| campaigns-email-editor | accepted-deviation | DEV-4, DEV-7, DEV-9 | 8/8 | absent `POST …/blocks` confirmed (DEV-7) |
| popups-widget-editor | accepted-deviation | DEV-4, DEV-7 (DEV-9 db row, zero delta) | 7/7 | absent `POST …/blocks` confirmed (DEV-7) |
| import-wizard | accepted-deviation | DEV-4, DEV-5, DEV-9 | 10/10 | includes DEV-12 404 step (10), matched; `var/import files` delta 1=1 |
| dashboard | accepted-deviation | DEV-4 (DEV-5/DEV-9 db rows, all-zero deltas) | 4/4 | 0 regressions |
| shell-navigation | accepted-deviation | DEV-4, DEV-9 (DEV-5 db row, zero delta) | 21/21 | includes DEV-12 404 step (21), matched; session delta 1=1 |

"Steps compared" = rows compare.mjs emitted a `contract` verdict for, out of the oracle's total step count for that journey (`capture/scenarios.json`); the gap is always steps with zero oracle HTTP entries (pure UI interaction, nothing to diff), never a truncation — confirmed by cross-checking every report against `scenarios.json`'s step counts.

## DEV-13 determination: **unused, safe to retire**

Packet instruction: confirm whether every `/collect` case in the event-stream oracle (incl. any `occurred_at` variants) matches byte-for-byte **without** a DEV-13 mask; if a mask would be needed, that's a FAIL.

Findings:
- `grep -rn "occurred_at" context/migration-oracle/` shows `occurred_at` appears only in the *endpoint contract documentation* (`inventory/endpoints.json`, `capture/inventory.mjs`) as an optional field — **no oracle journey step, and no `tests/e2e/specs/*.ts` spec, ever sends `occurred_at`** in a `/collect` payload.
- `/collect` is exercised only by the `event-stream` journey (steps 2–5; confirmed no other journey directory references `/collect`).
- `tools/migration-verify/compare.mjs` and `deviations.json` contain **no** `occurred_at`-specific code or mask anywhere (`grep -n occurred_at` on both files: zero matches).
- Directly diffed oracle vs. candidate `http.jsonl` for event-stream steps 2–5: request JSON and response bodies are byte-identical after JSON canonicalization (oracle escapes `„` as `„`, candidate emits the raw UTF-8 character — `compareJsonBodies`'s `JSON.parse`/`JSON.stringify` round-trip already treats these as equal, which is a canonicalization, not a DEV-13-specific mask).
- `compare.mjs`'s own report for `event-stream` shows steps 2–3 as `accepted-deviation(DEV-3,DEV-4,DEV-5)` and steps 4–5 as `accepted-deviation(DEV-4,DEV-5)` — no DEV-13 anywhere, and 0 regressions.

Conclusion: DEV-13 (narrower Java `occurred_at` parsing grammar) is never exercised by the oracle and needs no mask to reach parity today. It should be retired from the plan the next time the deviation table is revised (already absent from `tools/migration-verify/deviations.json`, consistent with the packet note that it "carries DEV-1..DEV-12 only").

## scheduler-heartbeat: compare.mjs does not support this journey; independent psql/log verification used

`compare.mjs --journey scheduler-heartbeat --dimension contract` **ran successfully but produced a vacuous result** (`{"steps":[],"regressions":0,"db":[]}`): the oracle journey directory `context/migration-oracle/symfony-to-spring-vue/journeys/scheduler-heartbeat/` contains only `scenario.md` and `contract.md` — no `steps/` directory and no `db.json` baseline — so `compareDb()`'s guard `if (!oracleDb || !candidateDelta) return [];` short-circuits before comparing anything. This is not a tool bug for this journey; the oracle was deliberately captured as prose (`contract.md`, "static capture") because the journey has no browser surface. Per the packet's fallback instruction, verified independently instead, read-only:

- `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "select name, lock_until, locked_at, locked_by from shedlock;"` → two rows: `heartbeat` (locked_at `2026-09-09 11:17:11.959`, lock_until `2026-09-09 11:18:11.958`) and `event-dedup-cleanup`. The `shedlock` table and row name confirm DEV-5's mapping (old `cache_items` scheduler-state key → new `shedlock` table) is real and populated, not just declared.
- `docker logs kivvi-int-api-1 | grep "Scheduler heartbeat tick"` → exactly two log lines, `2026-09-09T10:17:11.956Z` and `2026-09-09T11:17:11.960Z` — **3600.004s apart**, i.e. the hourly cadence the oracle's `contract.md` requires ("emits a scheduler heartbeat every hour... logs it"). The first tick fired ~9s after container creation (`2026-09-09T10:17:02Z`), consistent with `HeartbeatTrigger`'s documented "no prior lock row → fire now" first-run semantics (read from `backend/src/main/java/click/kivvi/infrastructure/scheduling/HeartbeatTrigger.java`), not a shortened test interval.
- `backend/src/main/resources/application.yml` confirms `kivvi.scheduling.heartbeat.interval: PT1H` (no profile override; running container's env has no interval override) — the default profile, hourly, exactly as the oracle's `contract.md` specifies, explicitly commented "never override it in the default profile."
- Topology (DEV-6): `docker ps --filter name=kivvi-int` shows exactly three containers (`mercure`, `api`, `database`) — no separate `worker` service, confirming DEV-6 ("no separate worker container; the job runs inside `api`") holds on the running stack.
- `HeartbeatSchedulerIT` (integration-dimension evidence) was **not read or reused** — this verification relies only on this dimension's own `psql`/`docker logs` reads plus reading the scheduling source (`HeartbeatJob.java`, `SchedulingConfig.java`, `HeartbeatTrigger.java`, `JdbcHeartbeatLockHistory.java`).

Verdict: accepted-deviation(DEV-5, DEV-6) — the shedlock-backed, hourly, single-process heartbeat contract holds.

## Tooling diff review: compare.mjs, normalize.json, deviations.json vs. pre-migration baseline

- **`context/migration-oracle/symfony-to-spring-vue/capture/normalize.json`**: `git diff e44fdbe HEAD -- .../capture/normalize.json` is **empty** — byte-identical to the commit that captured the oracle (`e44fdbe docs: add Spring Boot + Vue 3 migration plan and pre-migration oracle`). Untouched throughout the whole migration.
- **`tools/migration-verify/compare.mjs`**: diffed `3b17c07` (wave-0 introduction) against `HEAD`. All 7 hunks are documented, additive, and each *strengthens* rather than weakens the contract check:
  1. Per-invocation + per-viewport `RUN_ID` suffixing — fixes a self-inflicted idempotency-id collision between the desktop and mobile passes of the same invocation (a testing artifact, not a product concern).
  2. `dbCounts()` gains a `"var/import files"` key (filesystem count inside the `api` container) — additive metric for DEV-5's import-wizard mapping; harmless no-op (0=0) for every other journey.
  3. Unconditional `mkdirSync(dir, {recursive:true})` before the per-step loop — fixes an `ENOENT` crash for zero-step journeys (scheduler-heartbeat), letting the tool at least attempt (and correctly no-op) a db comparison for it.
  4. The passive HTTP listener now also captures `entry.body` for full-parity paths reached via a `click` step (e.g. the theme toggle's `POST /preferences/theme`) — previously such requests were never body-checked at all; this closes a gap, it doesn't loosen one.
  5. `comparePrefsBody` renamed to `compareJsonBodies` and reused for `/collect` — extends the deep JSON-body check from "only `/preferences/*` at status 200" to also cover `/collect` at every status (202/200/400), which the DEV-4 full-parity list already named but the code hadn't actually implemented for non-200 `/collect` responses.
  All of these are bug fixes that make the tool check *more* of the contract, never less; none removes or bypasses an existing comparison.
- **`tools/migration-verify/deviations.json`**: diffed `3b17c07` against `HEAD`. Every change is a documentation/mapping addition that matches the plan's "Accepted deviations" table read for this run: DEV-3's mechanism note explaining why the Mercure SSE payload shape isn't mechanically diffable; DEV-5's `"var/import files"` mapping (added alongside compare.mjs's new metric); DEV-11 reassigned from `login` (steps 4–8) to `landing` (step 5) with an explicit expiry note — this matches the plan's own DEV-11 row (`wave-4 WAVE_INTEGRATED (dashboard)` expiry for login, landing still masked); DEV-12's per-journey `steps` map extended from `{"shell-navigation":[21]}` to also include `customers:[6]`, `settings:[10]`, `import-wizard:[10]` — matches the plan's DEV-12 row listing exactly those four journey/step pairs. No entry was deleted, no threshold was loosened, no journey/step was silently dropped from scope.

Conclusion: all three files differ from their pre-migration/wave-0 baseline only by documented, additive changes. No undocumented drift found.

## 5xx sweep

`grep -rn '"status":5' <evidence>/*/steps/*/http.jsonl` across all 94 recorded HTTP entries (12 browser journeys) → **zero matches**. Status distribution: 226×200, 1×202, 4×302, 2×400, 4×404, 0×5xx.

## Commands run

All from cwd `/home/muszkin/work/kivvi-click-wt/verify-final-contract` unless noted; `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` exported for all `compare.mjs` runs.

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `npm ci` | `tools/migration-verify/` | 0 |
| 2 | `npm ci` | `tests/e2e/` | 0 |
| 3 | `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -tAc "select 1;"` (sanity) | repo root | 0 |
| 4 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <evidence>` | repo root | 0 |
| 5 | same, `--journey landing` | repo root | 0 |
| 6 | same, `--journey feeds` | repo root | 0 |
| 7 | same, `--journey event-stream` | repo root | 0 |
| 8 | same, `--journey customers` | repo root | 0 |
| 9 | same, `--journey automations` | repo root | 0 |
| 10 | same, `--journey settings` | repo root | 0 |
| 11 | same, `--journey campaigns-email-editor` | repo root | 0 |
| 12 | same, `--journey popups-widget-editor` | repo root | 0 |
| 13 | same, `--journey import-wizard` | repo root | 0 |
| 14 | same, `--journey dashboard` | repo root | 0 |
| 15 | same, `--journey shell-navigation` | repo root | 0 |
| 16 | same, `--journey scheduler-heartbeat` (vacuous result, see above) | repo root | 0 |
| 17 | `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "\d shedlock"` | repo root | 0 |
| 18 | `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "select name, lock_until, locked_at, locked_by from shedlock;"` | repo root | 0 |
| 19 | `docker logs kivvi-int-api-1 \| grep "Scheduler heartbeat tick"` | n/a (docker CLI) | 0 |
| 20 | `docker ps --filter name=kivvi-int` (topology check) | n/a | 0 |
| 21 | `git diff e44fdbe HEAD -- context/migration-oracle/symfony-to-spring-vue/capture/normalize.json` | repo root | 0 (empty diff) |
| 22 | `git diff 3b17c07 HEAD -- tools/migration-verify/compare.mjs` | repo root | 0 |
| 23 | `git diff 3b17c07 HEAD -- tools/migration-verify/deviations.json` | repo root | 0 |
| 24 | `grep -rn '"status":5' <evidence>/*/steps/*/http.jsonl` | repo root | 1 (no match, expected) |

No journey required a retry — every `compare.mjs` invocation exited 0 on the first attempt against the already-warm stack.

## Evidence paths

- Per-journey `compare.mjs` output (report.md, report.json, per-step `http.jsonl`/`step.json`/`db.json`, etc.): `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/contract/<journey>/`
- This summary: `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/contract.md`
- No files were written or modified outside the evidence directory and this summary file; the checkout `/home/muszkin/work/kivvi-click-wt/verify-final-contract` was not edited (only `node_modules/` installed under `tools/migration-verify/` and `tests/e2e/`, removed after this run per the DISK instruction).

## DISK

`tools/migration-verify/node_modules` and `tests/e2e/node_modules` were installed for this run only and removed immediately after use, per the packet's disk-tight instruction.
