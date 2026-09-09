# Wave-3 verifier report — dimension contract (round 1)

**Wave SHA:** `32a68311594e611aaa8eaa0803bc72bba7d735a9`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-3-contract` (detached at wave SHA, verified clean, untouched — `git status --short --branch` showed `## HEAD (no branch)` with no other output)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`, edge http `19100`), already running, healthy (`api`, `database`, `mercure` all `Up ... (healthy)`); not started or stopped by this verifier. Confirmed this run was the sole contract-dimension traffic on the shared stack — e2e and visual had already finished (their `e2e.md`/`visual.md` summaries pre-existed) and no other verifier ran concurrently.

## Dimension status: **PASS**

All three leading journeys (automations, settings, campaigns-email-editor) and both regression-guard
journeys (login, event-stream) replay with **0 regressions** against the oracle on the first
`compare.mjs --dimension contract` run each — no retry was needed on any journey. I independently
re-diffed every step's HTTP recording (method/path/status/location, and JSON/extracted body for
every DEV-4 full-parity path) and every journey's `db.json` delta against the oracle myself
(script + full output at `contract/independent-diff.txt`), not just trusting the tool's exit code,
and found no discrepancy the tool's verdict didn't already account for.

## compare.mjs diff audit (b87a701 → wave SHA)

One commit touched `tools/migration-verify/compare.mjs` in this wave:

```
2f7128d fix: capture response body for full-parity paths reached via a click step (#campaigns-email-editor)
 tools/migration-verify/compare.mjs | 16 ++++++++++++++++
 1 file changed, 16 insertions(+)
```

Pure addition, no deletions, no other file touched. It adds a `response.text()` capture — scoped
by `isFullParityPath(entry.path)` — to the passive `page.on("response")` listener, so a DEV-4
full-parity endpoint reached via a `click` step (the theme toggle, `POST /preferences/theme`) gets
its response body recorded, same as the pre-existing explicit `s.post` step branch already did.
Before this fix `entry.body` was `undefined` for such calls and `compareHttpEntry`'s
`/preferences/` deep-check (`compareJsonBodies(oracleEntry.body ?? "{}", candidateEntry.body ?? "{}")`)
would compare `"{}"` against the real oracle body and report a false regression — I confirmed this
is exactly campaigns-email-editor's steps 5/7 case (`step.json`: `"click": "[data-action=\"set-theme\"]"`),
where the candidate's `http.jsonl` now correctly shows `"body":"{\"theme\":\"dark\"}"` /
`"{\"theme\":\"light\"}"`, matching the oracle body exactly.

**No status/body exclusion was added or widened anywhere in the file** — `isFullParityPath`,
`FULL_PARITY_PATTERNS`, `compareHttpEntry`'s three deep-check branches (login-extraction,
preferences-JSON, /collect-JSON), `compareHttp`'s count check, and `compareDb`'s DEV-5/DEV-9
mapping are byte-identical to `b87a701`. The fix only makes the comparator see more data (a body
it previously silently missed), which can only make it *stricter*, never more lenient. Full diff
saved at `contract/compare-mjs-diff-b87a701..wave-sha.txt`.

## Per-journey verdicts

| Journey | Role | Verdict | Deviation ids applied | Evidence |
| --- | --- | --- | --- | --- |
| automations | leading (wave-3) | **parity** | DEV-4 (all 7 steps), DEV-5/DEV-9 (db, deltas 0=0) | `contract/automations/report.md`, `report.json`, `db.json` |
| settings | leading (wave-3) | **parity** | DEV-4 (all 10 steps, incl. step 10 = DEV-12 404 document: status 404 matched both sides, method+path+status only per DEV-4 document rule), DEV-5/DEV-9 (db, deltas 0=0) | `contract/settings/report.md`, `report.json`, `db.json` |
| campaigns-email-editor | leading (wave-3) | **parity** | DEV-4 (all 8 steps), DEV-7 (no `POST …/blocks` on either side — verified independently, see below), DEV-5 (db, 0=0), DEV-9 (db, sessions 1=1) | `contract/campaigns-email-editor/report.md`, `report.json`, `db.json` |
| login | regression guard (earlier wave) | **parity** | DEV-4 (all 6 recorded steps), DEV-9 (Set-Cookie name stripped entirely by `normalize.json`; sessions delta oracle=0/candidate=1, within the documented tolerance-1), DEV-5 (cache_items, 0=0) | `contract/login/report.md`, `report.json`, `db.json` |
| event-stream | regression guard (earlier wave) | **parity** | DEV-3 (steps 2–3, not mechanically diffed — see deviations.json note, verified out-of-band by other dimensions), DEV-4 (all 6 recorded steps, incl. /collect body full parity for 202/200 `{"status":"accepted"}`/`{"status":"duplicate"}`), DEV-5 (cache_items delta 1=1), DEV-9 (sessions 0=0) | `contract/event-stream/report.md`, `report.json`, `db.json` |

No `regression` verdicts anywhere. DEV-12 for `shell-navigation`/`customers`/`import-wizard` is out
of scope for wave-3 (those journeys are not in this wave and not run here).

### DEV-7 independent check (campaigns-email-editor)

Read the oracle's raw `http.jsonl` for all 8 steps directly (not through compare.mjs): every step
records exactly one non-`/api/v1/*` entry (document or the two `/preferences/theme` POSTs); none
is a `POST …/blocks`. Read the candidate's raw `http.jsonl` for the same 8 steps: identical shape
(one document GET + two `/api/v1/**` reads per editor page load, filtered out as baseline; the two
theme POSTs). Both sides make zero `/blocks` calls, so the DEV-7 mask never actually had to mask a
count mismatch in this replay — recorded as `accepted-deviation(DEV-7)` by the tool because DEV-7's
scope (`journeys: ["campaigns-email-editor", ...]`, `steps: "all"`) unconditionally applies to
every step of this journey regardless of whether a difference actually manifested.

### DEV-12 independent check (settings step 10)

`context/migration-oracle/.../journeys/settings/steps/10/step.json`: `documentStatus: 404`
(`GET /pl/settings/nonexistent`). Candidate's `steps/10/step.json`: `documentStatus: 404` too.
Both `http.jsonl` entries: `document`, `GET`, `/pl/settings/nonexistent`, `status: 404` — exact
method/path/status match, consistent with DEV-4's document rule (method+path+status only, no body
diff for `kind=document`) and DEV-12's note that "contract still requires status 404" (only the
*visual* dimension skips the 404 page body for this step, which is out of scope here).

### Login extraction re-implementation (regression guard)

`compareHttpEntry`'s `/login` deep-check does not raw-compare HTML bodies (the old and new stacks'
login pages share no markup); it regex-extracts only the login error message and last-submitted
username and compares those two fields. I re-implemented that exact regex pair in Python against
the raw recorded bodies, independently of compare.mjs:

| Step | Oracle (error, username) | Candidate (error, username) | Match |
| --- | --- | --- | --- |
| 1 (`GET /pl/login`) | `(None, None)` | `(None, None)` | yes |
| 5 (`POST` empty email) | `("Podaj adres e-mail.", "maciej@aureashop.pl")` | same | yes |
| 6 (`POST` malformed email) | `("To nie wygląda na poprawny adres e-mail.", "not-an-email")` | same | yes |

Step 7 (`POST /pl/logout`) is a 302 on both sides (`Location: /pl/login` both sides); no branch in
`compareHttpEntry` deep-checks a logout body, so the two stacks' differing redirect-page HTML
(Symfony's default vs. the new stack's empty body) is correctly out of scope — not a regression.

## Retries

None needed — all five `compare.mjs --dimension contract` invocations exited 0 on the first
attempt against the already-warm `kivvi-int` stack. No cold-start throw/timeout was observed, so
there is only one run recorded per journey (`*.run1.log`).

## Commands run

| # | Command | cwd | Exit | Result |
| --- | --- | --- | --- | --- |
| 1 | `npm ci` | `tests/e2e` (this checkout) | 0 | 3 packages (playwright/playwright-core/@playwright/test) |
| 2 | `npm ci` | `tools/migration-verify` (this checkout) | 0 | 2 packages (pngjs, pixelmatch) |
| 3 | `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --dimension contract --out .../contract` | repo root of checkout | 0 | 0 regressions |
| 4 | same, `--journey settings` | repo root of checkout | 0 | 0 regressions |
| 5 | same, `--journey campaigns-email-editor` | repo root of checkout | 0 | 0 regressions |
| 6 | same, `--journey login` | repo root of checkout | 0 | 0 regressions |
| 7 | same, `--journey event-stream` | repo root of checkout | 0 | 0 regressions |

`repo root of checkout` = `/home/muszkin/work/kivvi-click-wt/verify-wave-3-contract`.

## Evidence paths

All under `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/contract/`:

- `automations/report.md`, `automations/report.json`, `automations/db.json`, `automations/steps/**`, `automations.run1.log`
- `settings/report.md`, `settings/report.json`, `settings/db.json`, `settings/steps/**`, `settings.run1.log`
- `campaigns-email-editor/report.md`, `campaigns-email-editor/report.json`, `campaigns-email-editor/db.json`, `campaigns-email-editor/steps/**`, `campaigns-email-editor.run1.log`
- `login/report.md`, `login/report.json`, `login/db.json`, `login/steps/**`, `login.run1.log`
- `event-stream/report.md`, `event-stream/report.json`, `event-stream/db.json`, `event-stream/steps/**`, `event-stream.run1.log`
- `independent-diff.txt` — this verifier's own HTTP/db diff against the oracle (script output, not the tool's)
- `compare-mjs-diff-b87a701..wave-sha.txt` — the audited compare.mjs diff and its commit log

## Notes for the orchestrator

- Shell changes named in the wave-3 note (`scrollBehavior`/reload scroll restoration,
  `route.meta.defaultParams` locale toggle) are not exercised by either contract-dimension
  regression-guard scenario (`login`, `event-stream`) — neither scenario's scripted steps click a
  locale toggle or trigger a reload/back navigation that would produce an HTTP call for the
  contract dimension to compare. They make no server calls of their own (client-side router
  behaviour) and are covered by the e2e dimension's `navigation.spec.ts` run (already green per
  `e2e.md`, two pre-authorized failures for wave-4 routes) and the visual dimension, not by
  contract.
- Stack was left running (not started/stopped by this verifier), per instructions.
- `tests/e2e/node_modules` and `tools/migration-verify/node_modules` were created only inside this
  verifier's own checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-3-contract`) to satisfy
  `compare.mjs`'s `npm ci` prerequisites (it `require()`s `playwright` via `tests/e2e/package.json`
  and `pngjs`/`pixelmatch` via its own `package.json`). No tracked file was edited; only
  evidence-dir files (outside the checkout) and untracked `node_modules` (inside the checkout)
  were written.
