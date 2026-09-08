# Verifier report — wave wave-2, dimension contract (independent, round 2)

**Dimension status: PASS**, bound to wave SHA `b87a701244f5316e1a53bace7a1e41facdffc277`.

Verifier: read-only, independent — no worker reports or review files were read. All commands
were run from my own detached checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-2-contract`
(confirmed `git rev-parse HEAD` == the wave SHA above, working tree clean, detached) against the
orchestrator-managed stack at `https://localhost:19101` (compose project `kivvi-int`). I did not
start or stop the stack.

## Journeys and verdicts

| Journey | Verdict | Deviation ids exercised | Notes |
| --- | --- | --- | --- |
| event-stream | **parity** | DEV-3, DEV-4, DEV-5 | 0 regressions on first run; every `/collect` status (202/200/400/400) and body matched the oracle exactly; `db.json` delta matched the oracle exactly (`cache_items` +1 vs +1). |
| customers | **parity** | DEV-4, DEV-5, DEV-9 | 0 regressions on round-2 (quiet-stack) run. Round 1 reported a false `cache_items` regression (`db` delta +1 vs oracle 0); root-caused to concurrent `/collect` traffic from another dimension's verifier hitting the *same shared* `kivvi-int` stack during my journey's narrow before/after measurement window — not a candidate defect. See "Round-1 false positive" below for the evidence trail. |

No unmasked contract differences without an accepted-deviation id were found on either journey.
No regression traces to product code in the wave SHA.

## Commands run

All commands below were run with `cwd = /home/muszkin/work/kivvi-click-wt/verify-wave-2-contract`
(the detached wave-SHA checkout), `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"`.

1. `git rev-parse HEAD` → `b87a701244f5316e1a53bace7a1e41facdffc277` (exit 0) — confirms checkout is bound to the wave SHA.
2. `git status` → "Not currently on any branch. nothing to commit, working tree clean" (exit 0).
3. `cd tests/e2e && npm ci` (exit 0) — installs `@playwright/test`, required by `compare.mjs`'s `createRequire(tests/e2e/package.json)`.
4. `cd tools/migration-verify && npm ci` (exit 0) — installs `pixelmatch`, `pngjs`.
5. `node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension contract --out <evidence>/contract/out` — **exit 0**, "0 regression(s)".
6. `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension contract --out <evidence>/contract/out` — **exit 1**, "1 regression(s)" (round 1 — see false-positive analysis below).
7. Diagnostic: `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "select name, lock_until, locked_at, locked_by from shedlock order by locked_at;"` and the equivalent `select idempotency_hash, expires_at from event_dedup order by expires_at;` — both exit 0 — used to timestamp-forensic the false positive (see below).
8. Stability poll: three `select count(*) from event_dedup; select count(*) from shedlock;` 4s apart — all exit 0, counts stable at `59`/`2`, confirming the earlier burst of concurrent traffic had finished.
9. `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension contract --out <evidence>/contract/out-retry` — **exit 0**, "0 regression(s)" (round 2, quiet stack).

`<evidence>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/contract`.

Neither `compare.mjs` invocation threw or timed out — both completed normally and returned a real
report; the customers retry was not the packet's "retry on throw/timeout" case, but an
evidence-gathering rerun to distinguish a genuine regression from stack contention, which the
timestamp forensics below independently confirm.

## Independent HTTP recording diff (not just compare.mjs's summary)

I manually diffed every step's `http.jsonl` (candidate vs oracle) for both journeys, not just
`compare.mjs`'s per-step verdict:

- **event-stream**: step 1 (`GET /pl/events`) and step 7 (`GET /pl/events?type=purchase&site=aurea&range=24h`)
  are `kind:document` — method/path/status match exactly (DEV-4 scopes document-kind parity to
  method+path+status+URL only; HTML bodies are not compared, matching the SPA-shell contract).
  Steps 2–5 are the four `/collect` calls — every status code (202, 200, 400, 400) and every JSON
  body matched the oracle byte-for-byte, including the two exact Polish error messages
  (`"Pole „idempotency_id” jest wymagane."`, `"Nieznany typ zdarzenia „teleport”."`) and the
  success bodies (`{"status":"accepted"}`, `{"status":"duplicate"}`). Step 6 (button click, no HTTP)
  has no `http.jsonl` on either side, as expected. Mercure's SSE payload (DEV-3) is excluded from
  `http.jsonl` on both oracle and candidate by `normalize.json`'s `ignore_paths` for
  `/.well-known/mercure`, so it isn't diffable this way by design — DEV-3 documents the separate
  backend-test evidence for that payload shape change, which is out of this dimension's scope
  (compare.mjs itself has "nothing to diff…mechanically" per `deviations.json`).
- **customers**: all 6 steps (`GET /pl/customers`, `?page=2`, back to `/pl/customers`, click into
  `/pl/customers/c_1000`, back to `/pl/customers`, `GET /pl/customers/c_9999` → 404) match the
  oracle on method/path/status exactly, including the 404 on the missing-customer route (DEV-4
  document-kind scope again; DEV-12 for the missing-customer step is a *visual*-dimension
  deviation, out of scope here — contract still required the 404 status, which matched).
- Every candidate document/API response was also missing several headers the oracle recorded
  (`cache-control`, `expires`, `x-robots-tag` on document responses; `cache-control`,
  `x-robots-tag` on `/collect`; the old stack's `x-debug-exception*` Symfony dev-mode headers on
  the 404). None of these are part of the contract dimension's threshold: `compare.mjs`'s own
  `compareHttpEntry()` only ever compares method/path/status(+Location on 3xx), and DEV-4
  explicitly scopes document-kind parity to "method, path, status and final URL only" — headers
  are out of scope by the plan's own accepted deviation, and none of the missing headers are
  behaviourally significant for a POST (`/collect`) or an SPA-shell document response. Flagged here
  as an observation only, not a contract failure.

## Independent `db.json` delta diff — round-1 false positive on customers, root-caused

`compare.mjs`'s `dbCounts()` snapshots the *whole shared database* immediately before and after
each journey's own steps, then diffs the delta against the oracle. Because the wave-2 stack
(`kivvi-int`) is shared across all six dimension verifiers running concurrently against the same
running containers, any `/collect` traffic or scheduled-job tick from another verifier that lands
inside a journey's few-second measurement window shows up as that journey's own delta.

**event-stream** (first and only run, `contract/out/event-stream/db.json`):
`{"before":{"spring_session":0,"event_dedup":6,"shedlock":2},"after":{"spring_session":0,"event_dedup":7,"shedlock":2},"delta":{"spring_session":0,"event_dedup":1,"shedlock":0}}`.
Oracle `db.json` delta: `{"sessions":0,"cache_items":1,"messenger_messages":0}`. Mapped
`cache_items → event_dedup+shedlock` (DEV-5): candidate `1+0=1` vs oracle `1` — **exact match**,
and traces entirely to the journey's own two `/collect` calls (idempotent, so one net insert).

**customers round 1** (`contract/customers/round1-contaminated-run/db.json`):
`{"before":{"spring_session":0,"event_dedup":7,"shedlock":2},"after":{"spring_session":0,"event_dedup":8,"shedlock":2},"delta":{"spring_session":0,"event_dedup":1,"shedlock":0}}`.
Oracle delta: `{"sessions":0,"cache_items":0,"messenger_messages":0}` — expected 0, candidate
mapped-sum 1 → flagged `regression`. I independently confirmed the candidate's own `http.jsonl`
for all 6 steps of this run (`contract/customers/round1-contaminated-run` step files, and the
`contract/out/customers/steps/*/http.jsonl` behind it) contain **zero** `/collect` calls or any
other request that would legitimately write to `event_dedup` or `shedlock` — the journey is pure
`GET` browsing. The extra row therefore did not originate from the candidate code path under test.

Timestamp forensics on the live tables (`shedlock` `locked_at`, `event_dedup` `expires_at`) taken
minutes later showed `event_dedup` holding 59 rows, with 47 of them inserted in a single
sub-second burst at `22:47:47.72`–`22:47:48.54` — consistent with the plan's own DEV-8 performance
budget check ("`/collect` p95 over 50 requests"), which another verifier's `performance.mjs` or
`events.spec.ts` e2e run would generate against this same shared stack. `spring_session` also grew
from 0 to 3 between my two customers runs with no login step in this journey. Once the burst had
settled (three 4-second-apart polls showed `event_dedup`/`shedlock` counts stable), a repeat
`compare.mjs --journey customers --dimension contract` run reproduced **exact** oracle parity:
`{"before":{"spring_session":3,"event_dedup":59,"shedlock":2},"after":{"spring_session":3,"event_dedup":59,"shedlock":2},"delta":{"spring_session":0,"event_dedup":0,"shedlock":0}}`
(`contract/customers/db.diff.json`, and `contract/out-retry/customers/db.json`).

**Conclusion**: the round-1 customers regression is a test-environment race from concurrent
`/collect` traffic hitting the shared `kivvi-int` stack during another verifier's own run, not a
defect in the wave-2 candidate. The dimension verdict for customers is **parity**, evidenced by
the round-2 (quiet-stack) run, with the round-1 run kept as diagnostic evidence rather than
discarded.

## Evidence paths

- `contract/out/event-stream/` — event-stream compare.mjs run (report.json, report.md, db.json, per-step captures). Final/only run for this journey.
- `contract/out/customers/` — customers round-1 (contaminated) compare.mjs run.
- `contract/out-retry/customers/` — customers round-2 (quiet-stack) compare.mjs run. **Authoritative for the customers verdict.**
- `contract/event-stream/step-1.diff.json` … `step-7.diff.json`, `contract/event-stream/db.diff.json` — my independent oracle-vs-candidate HTTP and db diffs.
- `contract/customers/step-1.diff.json` … `step-6.diff.json`, `contract/customers/db.diff.json` — independent diffs, built from the round-2 (authoritative) run.
- `contract/customers/round1-contaminated-run/` — the round-1 report/db.json, preserved as the false-positive evidence trail.
