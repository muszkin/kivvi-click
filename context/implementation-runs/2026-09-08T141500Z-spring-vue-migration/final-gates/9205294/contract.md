# Contract verifier — FINAL gate, SHA 9205294

**Dimension:** contract
**Gate:** Phase-5 final gate, feature SHA `92052947a6238ecf4ee177f4e72493122315ce32` (short `9205294`)
**Wave SHA referenced by the packet:** `dae169614a52532c130bd34435994d6a914165c0` (round-2 wave-final baseline; this run re-verifies the same 13 journeys on the frozen feature SHA)
**Verifier:** independent, read-only, ran alone
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-final-contract`, detached HEAD confirmed at `92052947a6238ecf4ee177f4e72493122315ce32`, tree clean before and after this run (`git status --porcelain=v1` → 0 lines both times; `tests/e2e/node_modules` and `tools/migration-verify/node_modules` are gitignored, installed for this run only, deleted afterward)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`, edge http `19100`), pre-started by the orchestrator, healthy throughout (`kivvi-int-api-1`, `kivvi-int-database-1`, `kivvi-int-mercure-1` all `Up ... (healthy)`), **not** started or stopped by this verifier
**Evidence root:** `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/final-gates/9205294/contract/`

## Gate status: **PASS**, bound to SHA `92052947a6238ecf4ee177f4e72493122315ce32`

0 regressions across all 13 journeys. Every observed difference from the oracle carries a recorded deviation id from the plan's "Accepted deviations" table (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md` § Accepted deviations) and `tools/migration-verify/deviations.json`. No 5xx, no 503, no session-lock-filter failure, in any of the 231 recorded HTTP responses. The forwarded-headers change did not alter any recorded status or `Location`. DEV-13 is confirmed unused and retirable.

## Per-journey verdicts

| # | Journey | Verdict | Deviation ids used | compare.mjs exit | Report |
| --- | --- | --- | --- | --- | --- |
| 1 | login | accepted-deviation | DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/login/report.md` |
| 2 | landing | accepted-deviation | DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/landing/report.md` |
| 3 | feeds | accepted-deviation | DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/feeds/report.md` |
| 4 | scheduler-heartbeat | accepted-deviation (verified via read-only psql/logs, not compare.mjs) | DEV-5, DEV-6 | 0 (compare.mjs run produced an empty/trivial report — no oracle `db.json`, no steps — see note below) | `contract/scheduler-heartbeat/psql-logs-evidence.md`, `contract/raw/scheduler-heartbeat/report.md` |
| 5 | event-stream | accepted-deviation | DEV-3, DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/event-stream/report.md` |
| 6 | customers | accepted-deviation | DEV-4, DEV-5, DEV-9, DEV-12 (status 404 parity at step 6; contract dimension has no `steps` entry for DEV-12 since it's a visual-only mask — see "DEV-12 scope" below) | 0 | `contract/raw/customers/report.md` |
| 7 | automations | accepted-deviation | DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/automations/report.md` |
| 8 | settings | accepted-deviation | DEV-4, DEV-5, DEV-9, DEV-12 (status 404 parity at step 10) | 0 | `contract/raw/settings/report.md` |
| 9 | campaigns-email-editor | accepted-deviation | DEV-4, DEV-5, DEV-7, DEV-9 | 0 | `contract/raw/campaigns-email-editor/report.md` |
| 10 | popups-widget-editor | accepted-deviation | DEV-4, DEV-5, DEV-7, DEV-9 | 0 | `contract/raw/popups-widget-editor/report.md` |
| 11 | import-wizard | accepted-deviation | DEV-4, DEV-5, DEV-9, DEV-12 (status 404 parity at step 10) | 0 | `contract/raw/import-wizard/report.md` |
| 12 | dashboard | accepted-deviation | DEV-4, DEV-5, DEV-9 | 0 | `contract/raw/dashboard/report.md` |
| 13 | shell-navigation | accepted-deviation | DEV-4, DEV-5, DEV-9, DEV-12 (status 404 parity at step 21) | 0 | `contract/raw/shell-navigation/report.md` |

No journey produced a `regression` verdict on any step or `db` row (`grep -l "| regression |" contract/raw/*/report.md` → no matches).

### DEV-12 scope note

`compare.mjs`'s `--dimension contract` path does not itself tag the four 404 steps with `DEV-12` — DEV-12 is declared `"dimension": "visual"` in `deviations.json` (mechanism `skip-on-404`, skips screenshot/aria/texts only). For `contract`, these steps go through the ordinary `compareHttp` path with no mask, and the plan's own DEV-12 row states "contract still requires status 404" — which this run confirms by direct comparison, not by a verifier-side mask:

- customers step 6 (`GET /pl/customers/c_9999`): oracle `status: 404`, candidate `status: 404` — match.
- settings step 10 (`GET /pl/settings/nonexistent`): oracle/candidate `status: 404` — match (report shows no per-step regression).
- import-wizard step 10 (`GET /pl/import/5`): oracle/candidate `status: 404` — match.
- shell-navigation step 21: oracle/candidate `status: 404` — match (21/21 steps report `accepted-deviation`, 0 regressions).

Table entries above list DEV-12 as "used" in the loose sense that these are the plan's DEV-12 steps and their 404 parity was independently confirmed; the contract dimension itself needed no DEV-12 mask to reach parity, exactly as the plan requires.

## DEV-13 disposition (event-stream `/collect` `occurred_at`)

Per the packet's "Note for the contract verifier (FINAL round 2)": confirmed explicitly.

- `tools/migration-verify/deviations.json` on `9205294` carries `DEV-1..DEV-12` only (`grep -o '"id": *"DEV-[0-9]*"' … | sort -u` → DEV-1, DEV-10, DEV-11, DEV-12, DEV-2..DEV-9; no DEV-13 entry, no `DEV-13` string anywhere in `compare.mjs`).
- Every `/collect` case in the event-stream oracle was inspected directly (`journeys/event-stream/steps/{2,3,4,5}/step.json`): idempotent purchase (202), duplicate replay (200), missing `idempotency_id` (400), unknown type (400). **None sends `occurred_at`.**
- The live replay against `9205294` (`contract/raw/event-stream/steps/{2,3,4,5}/http.jsonl`) confirms the same: none of the four candidate requests carries `occurred_at` either, and all four bodies matched the oracle byte-for-byte via `compareJsonBodies` (the `/collect` full-parity deep check) with **no regression and no DEV-13 mask applied** — because none exists to apply.
- `occurred_at` appears only in `context/migration-oracle/symfony-to-spring-vue/inventory/endpoints.json` and `capture/inventory.mjs` as an optional contract field (`{idempotency_id,type,detail?,customer_id?,customer_name?,site?,occurred_at?}`), never exercised by an oracle step or by `tests/e2e/specs/events.spec.ts` (`grep -rn occurred_at tests/e2e/` → no matches).
- The backend (`backend/src/main/java/click/kivvi/domain/tracking/TrackedEvent.java:58-86`) does implement the narrower grammar DEV-13 describes (`Instant.parse` → `OffsetDateTime.parse` → `LocalDateTime.parse`, defaulting to `Instant.now()` when the field is absent) — the code-level gap DEV-13 documents is real, it is simply never reached by any case this oracle or this journey's e2e spec sends.

**Conclusion: DEV-13 is confirmed unused on this SHA — no `/collect` case needed it, no mask was needed, and none was applied. This is not a FAIL (there is no unmasked, unrecorded difference — there is no difference at all in the cases exercised). DEV-13 can be retired** the next time the plan's deviation table is revised, pending the k.js client work the plan itself ties it to (PIO-80); retiring it is a plan-authoring action, out of scope for this read-only verifier.

## No-5xx / no-503 / session-lock-filter check

- Status codes across all 231 recorded HTTP responses in `contract/raw/*/steps/*/http.jsonl`: `200` × 226, `202` × 1, `302` × 4, `400` × 2, `404` × 4. **Zero 5xx, zero 503.**
- `docker compose -p kivvi-int -f compose.next.yaml logs api | grep -iE "503|SessionLock|session.lock|lock.*timeout"` → no output (no session-lock filter rejection at any point during the run).

## Forwarded-headers change (`server.forward-headers-strategy: native`)

Confirmed present in `backend/src/main/resources/application.yml:45` on this checkout. All recordings go through the Mercure/Caddy edge, which is the only hop that adds `X-Forwarded-*` before Spring sees the request — so a misconfiguration here would surface as a wrong scheme/host in a `Location` header or a redirect failure, not as a missing request header (Playwright captures the browser's own outgoing headers, which never carry `X-Forwarded-*`). Every full-parity redirect was diffed directly against the oracle and matched exactly:

| Case | Journey/step | Candidate | Oracle | Match |
| --- | --- | --- | --- | --- |
| login POST → dashboard | login step 4 | `302`, `Location: /pl/dashboard` | `302`, `Location: /pl/dashboard` | yes |
| import upload | import-wizard step 9 | `302`, `Location: /pl/import/2` | `302`, `Location: /pl/import/2` | yes |
| logout | login step 7 | `302`, `Location: /pl/login` | `302`, `Location: /pl/login` | yes |

No absolute URL, wrong scheme, or internal-hostname leak in any `Location` header observed; all relative paths, all matching the oracle. `compareHttp`'s `locationPath()` check ran on every 3xx entry in every journey (login steps 4/8, shell-navigation's session-establishing steps, import-wizard step 9) with 0 regressions.

## `tools/migration-verify` drift vs `dae1696`

```
$ git diff --stat dae169614a52532c130bd34435994d6a914165c0 92052947a6238ecf4ee177f4e72493122315ce32 -- tools/migration-verify/
 tools/migration-verify/.agents/project-context.md | 105 ++++++++++++++++++++++
 tools/migration-verify/deviations.json            |   2 +-
 2 files changed, 106 insertions(+), 1 deletion(-)
```

- `compare.mjs`: **byte-identical** to `dae1696` (no diff at all).
- `deviations.json`: **one line changed** — the DEV-12 `note` field gained a reconciliation paragraph (w5-ops-cutover, `final-review-tooling-ops.md` finding #5) confirming the plan's canonical "Accepted deviations" table and this file agree on DEV-12's scope, plus an explicit statement that DEV-13 is intentionally absent from this file. This is the documented DEV-12 note the packet asked to confirm — no `id`, `journeys`, `dimension`, `mechanism`, or `rule` field changed for any deviation (checked by extracting all `"id": "DEV-N"` entries — still exactly DEV-1..DEV-12, same set as `dae1696`).
- `normalize.json` **does not live under `tools/migration-verify/`** at either SHA (`git ls-tree -r --name-only <sha> -- tools/migration-verify/ | grep -i normalize` → no match at both `dae1696` and `9205294`). `compare.mjs` reads normalization rules from `context/migration-oracle/symfony-to-spring-vue/capture/normalize.json` instead (`compare.mjs:75`, `oracleDir` join) — that file is part of the immutable oracle (single commit `e44fdbe`, no history since), untouched by either SHA. The packet's reference to a `tools/migration-verify/normalize.json` path does not match the actual file layout; the file that plays that role (the oracle's `normalize.json`) is unchanged and out of scope for edits by design.
- The one other changed file, `tools/migration-verify/.agents/project-context.md`, is a scoped-context documentation file (not verifier logic), added between the two SHAs; it does not affect `compare.mjs` behavior (confirmed by the byte-identical diff on `compare.mjs` itself).

**Conclusion: `compare.mjs` and `deviations.json` differ from `dae1696` only by the documented DEV-12 note, exactly as the packet requires. `normalize.json` is not part of `tools/migration-verify/` and was not touched (the oracle copy it actually reads from is immutable and unchanged).**

## Commands run

All commands below ran with cwd `/home/muszkin/work/kivvi-click-wt/verify-final-contract` unless noted.

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` / `git status --porcelain=v1 --branch` | checkout root | 0 (HEAD `92052947a6238ecf4ee177f4e72493122315ce32`, clean) |
| 2 | `git diff --stat dae169614a52532c130bd34435994d6a914165c0 92052947a6238ecf4ee177f4e72493122315ce32 -- tools/migration-verify/` | checkout root | 0 |
| 3 | `npm ci` | `tests/e2e/` | 0 (3 packages; Playwright browsers already cached at `~/.cache/ms-playwright`) |
| 4 | `npm ci` | `tools/migration-verify/` | 0 (2 packages: `pixelmatch`, `pngjs`) |
| 5 | `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out .../contract/raw` | checkout root | 0 |
| 6 | same, `--journey landing` | checkout root | 0 |
| 7 | same, `--journey feeds` | checkout root | 0 |
| 8 | same, `--journey event-stream` | checkout root | 0 |
| 9 | same, `--journey customers` | checkout root | 0 |
| 10 | same, `--journey automations` | checkout root | 0 |
| 11 | same, `--journey settings` | checkout root | 0 |
| 12 | same, `--journey campaigns-email-editor` | checkout root | 0 |
| 13 | same, `--journey popups-widget-editor` | checkout root | 0 |
| 14 | same, `--journey import-wizard` | checkout root | 0 |
| 15 | same, `--journey dashboard` | checkout root | 0 |
| 16 | same, `--journey shell-navigation` | checkout root | 0 |
| 17 | same, `--journey scheduler-heartbeat` | checkout root | 0 (empty/trivial report — no oracle `db.json`, no browser steps for this journey; see scheduler-heartbeat note) |
| 18 | `docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "select name, lock_until, locked_at, locked_by from shedlock;"` | checkout root | 0 |
| 19 | `docker compose -p kivvi-int -f compose.next.yaml logs api \| grep -c "Scheduler heartbeat tick\."` | checkout root | 0 (count 1) |
| 20 | `docker compose -p kivvi-int -f compose.next.yaml logs api \| grep -iE "503\|SessionLock\|session.lock\|lock.*timeout"` | checkout root | 1 (no match — confirms absence) |
| 21 | `grep -oh '"status":[0-9]*' contract/raw/*/steps/*/http.jsonl \| sort \| uniq -c` | evidence dir | 0 |
| 22 | `rm -rf tests/e2e/node_modules tools/migration-verify/node_modules` | checkout root | 0 (cleanup) |

No timeout occurred on any of the 13 `compare.mjs` invocations (all completed on the first attempt against the pre-warmed, already-healthy stack); the "retry once on a cold-stack timeout" policy was not needed.

## Scheduler-heartbeat: why psql/logs, not `compare.mjs`

`context/migration-oracle/symfony-to-spring-vue/journeys/scheduler-heartbeat/` holds only `scenario.md` and `contract.md` — no `steps/` directory and no `db.json` (this is a backgroundjob journey with no browser surface, `scenarios.json`'s entry has `steps: []`). Running `compare.mjs --journey scheduler-heartbeat --dimension contract` therefore executes zero comparison steps and produces an empty `report.md` (0 regressions, but 0 signal) — confirmed by the actual run (`contract/raw/scheduler-heartbeat/report.md`). Per the packet, the real contract check is read-only `psql`/log inspection against the frozen SHA's already-running, already-healthy stack (up ~26 minutes at the time of the check, uptime long enough for the fresh-DB "first execution immediately" path in `HeartbeatTrigger.firstExecutionAfterProcessStart` to have already fired once):

- `shedlock` table carries one `heartbeat` row: `locked_at 2026-09-09 13:19:15.196`, `lock_until 2026-09-09 13:20:15.185` (4 seconds after the `api` container's own start time `13:19:11.082`, and the `lock_until − locked_at` gap is exactly the configured `lockAtLeastFor = PT1M`) — matches `HeartbeatJob`'s `@SchedulerLock(name = "heartbeat", lockAtLeastFor = "PT1M")`.
- `api` container logs carry exactly one `INFO … c.k.i.scheduling.HeartbeatJob : Scheduler heartbeat tick.` line, matching the oracle's `contract.md`-recorded message (`Scheduler heartbeat tick.` at INFO) verbatim, logged exactly once — the missed-run-once semantics documented in `contract.md` ("after a worker outage exactly one missed run executes") reproduced correctly for the fresh-start case, and no duplicate tick inside the `lockAtLeastFor` window.
- No other database writes are attributable to this job (the only two `shedlock` rows present are `heartbeat` and the unrelated `event-dedup-cleanup` job; neither `spring_session` nor `event_dedup` rows are touched by this journey).

Full capture: `contract/scheduler-heartbeat/psql-logs-evidence.md`.

## Evidence paths

- Per-journey `compare.mjs` raw output (steps, `http.jsonl`, `db.json`, `report.json`, `report.md`): `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/final-gates/9205294/contract/raw/<journey>/`
- Run logs and exit codes: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/final-gates/9205294/contract/logs/`
- Scheduler-heartbeat read-only psql/log capture: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/final-gates/9205294/contract/scheduler-heartbeat/psql-logs-evidence.md`
- This summary: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/final-gates/9205294/contract.md`
