# Wave-3 verifier report — dimension: visual (round 2, independent verification)

**Wave SHA:** `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4` (verifier checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual`, detached, confirmed clean — `git status --short` empty)

**Dimension status: PASS**, bound to wave SHA `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`.

Oracle manifest integrity confirmed: `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` =
`4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — matches the packet's stated hash exactly.

Stack used (not started/stopped by this verifier): `https://localhost:19101`, compose project `kivvi-int`
(`api`, `database`, `mercure` all `running healthy` at verification time).

This is a fresh, independent round-2 verification. Round-1 evidence under `waves/wave-3/round-1/` was
**not read**, per instructions.

## Per-journey verdicts

| Journey | Verdict | Deviation IDs exercised | Steps | Regressions |
| --- | --- | --- | --- | --- |
| automations (in scope) | **parity** | none | 7/7 | 0 |
| settings (in scope) | **accepted-deviation** | DEV-12 (step 10, 404 document) | 10/10 | 0 |
| campaigns-email-editor (in scope) | **parity** | none (DEV-7 is a contract-only deviation, not exercised on this dimension) | 8/8 | 0 |
| login (regression guard) | **accepted-deviation** | DEV-11 (steps 4–8, `.main-scroll` masked — dashboard body not yet built, wave-4) | 8/8 | 0 |
| customers (regression guard) | **accepted-deviation** | DEV-12 (step 6, 404 document) | 6/6 | 0 |

No regressions in any of the five journeys. No deviation was invoked outside the set the plan and packet
authorize for this wave/step combination.

## Per-step table

Legend: `parity` = compare.mjs found 0 unmasked differences; `dev(ID)` = accepted-deviation with that ID;
pixel % is compare.mjs's own `pixelmatch` ratio (threshold 0.1), independently corroborated below.

### automations (in scope journey)

| Step | url | texts | aria | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity (0.001%) | parity (0.000%) |
| 2 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 3 | parity | parity | parity | parity (0.001%) | parity (0.000%) |
| 4 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 5 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 6 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 7 | parity | parity | parity | parity (0.000%) | parity (0.000%) |

### settings (in scope journey — step 5 is the "Webhooks i API" tab, step 10 is DEV-12)

| Step | url | texts | aria | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 2 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 3 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 4 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| **5 (api tab)** | **parity** | **parity** | **parity** | **parity (0.000%)** | **parity (0.000%)** |
| 6 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 7 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 8 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 9 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 10 (404) | — | **dev(DEV-12)** skip | **dev(DEV-12)** skip | **dev(DEV-12)** skip | **dev(DEV-12)** skip |

### campaigns-email-editor (in scope journey — step 6 is `reload: true`)

| Step | url | texts | aria | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity (0.008%) | parity (0.000%) |
| 2 | parity | parity | parity | parity (0.009%) | parity (0.000%) |
| 3 | parity | parity | parity | parity (0.008%) | parity (0.000%) |
| 4 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 5 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| **6 (reload)** | **parity** | **parity** | **parity** | **parity (0.000%)** | **parity (0.000%)** |
| 7 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 8 | parity | parity | parity | parity (0.000%) | parity (0.000%) |

### login (regression guard — steps 4–8 are DEV-11)

| Step | url | texts | aria | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 2 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 3 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 4 | parity | dev(DEV-11) | dev(DEV-11) | dev(DEV-11) (0.000%) | dev(DEV-11) (0.000%) |
| 5 | parity | dev(DEV-11) | dev(DEV-11) | dev(DEV-11) (0.000%) | dev(DEV-11) (0.000%) |
| 6 | parity | dev(DEV-11) | dev(DEV-11) | dev(DEV-11) (0.000%) | dev(DEV-11) (0.000%) |
| 7 | parity | dev(DEV-11) | dev(DEV-11) | dev(DEV-11) (0.000%) | dev(DEV-11) (0.000%) |
| 8 | parity | dev(DEV-11) | dev(DEV-11) | dev(DEV-11) (0.000%) | dev(DEV-11) (0.000%) |

### customers (regression guard — step 6 is DEV-12)

| Step | url | texts | aria | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 2 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 3 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 4 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 5 | parity | parity | parity | parity (0.000%) | parity (0.000%) |
| 6 (404) | — | dev(DEV-12) skip | dev(DEV-12) skip | dev(DEV-12) skip | dev(DEV-12) skip |

All pixel ratios are ≤ 0.5% (max observed: 0.009%, campaigns-email-editor step 2 desktop) — well inside the
plan's threshold.

## Independent spot-checks (this verifier, not compare.mjs)

### 1. ImageMagick `compare -metric AE -fuzz 10%` against the raw oracle/candidate PNGs

Run for every captured step/viewport in all five journeys (39 steps × 2 viewports = 78 comparisons). For
login steps 4–8 the pre-masked oracle temp file compare.mjs itself writes
(`.oracle-mask-<viewport>-<n>.png`, painting the same `.main-scroll` rectangle DEV-11 specifies) was used
as the comparison baseline, matching what compare.mjs itself compares against.

Result: every non-404, non-DEV-11-masked step showed AE ratios of **0.0000%–0.0170%** (max:
campaigns-email-editor step 2 desktop, 220/1,296,000 px), fully consistent with — same order of magnitude
as — compare.mjs's own `pixelmatch` numbers (different algorithms, so exact figures differ, but both
agree the images are near-identical and far under the 0.5% threshold). Login steps 4–8 (DEV-11-masked)
showed **0.0000%** AE against the masked oracle baseline. The two DEV-12 steps (settings step 10, customers
step 6) showed **30.58%/38.20%** and **30.46%/38.04%** AE respectively (desktop/mobile) — confirming these
really are drastically different pages (old Symfony debug/exception page vs. new minimal 404) and that
skipping visual comparison there via DEV-12 is a real, warranted deviation rather than a way to hide a
regression.

(Note: an initial pass of this spot-check reused stale `.oracle-mask-*.png` temp files by step-number
across journeys and produced spurious ~77–99% "differences" for later steps in automations/settings/
campaigns-email-editor/customers — that was a bug in this verifier's own script, not a product issue.
Rerunning with mask reuse correctly scoped to the login journey only reproduced the low percentages
above; see the corrected commands list below.)

### 2. Byte-for-byte diff of every `texts.json` and `a11y.json` (`diff`/`cmp`, not compare.mjs's JSON-string-equality)

Ran for all 39 steps × 2 files = 78 comparisons. Byte-identical for every step **except**:

- `settings` step 10 and `customers` step 6 — expected: oracle recorded the old stack's Symfony exception
  page text/aria tree (`"Symfony Exception"`, `"NotFoundHttpException"`, stack trace lines, etc.); DEV-12
  authorizes skipping this comparison and compare.mjs did skip it (see per-step table).
- `login` steps 4–8 — expected: candidate's shell-only text/aria array is a **strict prefix** of the
  oracle's full array (verified line-by-line — every candidate line matches the corresponding oracle line
  in order, and the candidate array is shorter), which is exactly the DEV-11 prefix mechanism compare.mjs's
  `compareTexts`/`compareAria` implement.

### 3. Manual check of the two flagged steps

- **settings step 5 (Webhooks i API tab)**: `url.txt` = `/pl/settings/api` on both sides; `texts.json` and
  `a11y.json` byte-identical to the oracle.
- **campaigns-email-editor step 6 (`reload: true`)**: oracle `step.json` records
  `{"reload":true,"waitAttr":["html","data-theme","dark"]}`; candidate `step.json` shows
  `documentStatus: 200` and the same recorded `url.txt` (`/pl/emails/k1`). Since `runStep` blocks on
  `page.waitForFunction(...)` for the `data-theme=dark` attribute with a 15s timeout and throws (crashing
  the whole compare.mjs invocation) if it's never satisfied, and the run completed and exited 0, the
  candidate's reload correctly restored `data-theme=dark` (the theme preference persisted across the
  reload on the new stack, matching the oracle's own captured behaviour). `texts.json`/`a11y.json` are
  byte-identical to the oracle for this step.

## Commands run

All commands run from the verifier checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual`
unless noted. `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` exported for every
compare.mjs invocation (not exercised by the visual dimension itself — no `--dimension contract` — but set
per the packet's instruction).

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | verifier checkout | 0 (`c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`) |
| 2 | `git status --short` | verifier checkout | 0 (empty — clean) |
| 3 | `sha256sum manifest.json` | `context/migration-oracle/symfony-to-spring-vue` | 0 (matches packet hash) |
| 4 | `curl -sk -o /dev/null -w "%{http_code}" https://localhost:19101/` | n/a | 0 (`200`) |
| 5 | `docker compose -p kivvi-int ps` | n/a | 0 (api/database/mercure all `running healthy`) |
| 6 | `npm ci` | verifier checkout `tests/e2e/` | 0 |
| 7 | `npm ci` | verifier checkout `tools/migration-verify/` | 0 |
| 8 | `npx playwright install --dry-run chrome` | verifier checkout `tests/e2e/` | 0 (system Chrome channel already present) |
| 9 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --dimension visual --out <evidence-dir>` | verifier checkout | 0 (0 regressions) |
| 10 | `node tools/migration-verify/compare.mjs --journey settings --base https://localhost:19101 --dimension visual --out <evidence-dir>` | verifier checkout | 0 (0 regressions) |
| 11 | `node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --dimension visual --out <evidence-dir>` | verifier checkout | 0 (0 regressions; first attempt succeeded — stack was already warm, **no cold-stack timeout occurred, no retry needed**) |
| 12 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence-dir>` | verifier checkout | 0 (0 regressions — regression guard) |
| 13 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence-dir>` | verifier checkout | 0 (0 regressions — regression guard) |
| 14 | ImageMagick `compare -metric AE -fuzz 10% <oracle-or-masked-oracle>.png <candidate>.png null:` looped over all 39 steps × 2 viewports for all 5 journeys | n/a (image paths absolute) | all 0 (comparisons ran; see spot-check §1 for the ratios — script exit code is comparison-result-dependent in ImageMagick, not itself a pass/fail signal here, so results were read from the printed AE counts, not the exit status) |
| 15 | `diff`/`cmp` looped over all 39 steps × {`texts.json`,`a11y.json`} for all 5 journeys | n/a (file paths absolute) | see spot-check §2 (only the two DEV-12 steps and login steps 4–8 differed, all expected) |

`<evidence-dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual`

## Evidence paths

- Per-journey compare.mjs output (screenshots, diffs, texts.json, a11y.json, url.txt, http.jsonl, step.json,
  report.json, report.md) under:
  - `<evidence-dir>/automations/steps/1..7/`, `<evidence-dir>/automations/report.{json,md}`
  - `<evidence-dir>/settings/steps/1..10/`, `<evidence-dir>/settings/report.{json,md}`
  - `<evidence-dir>/campaigns-email-editor/steps/1..8/`, `<evidence-dir>/campaigns-email-editor/report.{json,md}`
  - `<evidence-dir>/login/steps/1..8/`, `<evidence-dir>/login/report.{json,md}` (regression guard)
  - `<evidence-dir>/customers/steps/1..6/`, `<evidence-dir>/customers/report.{json,md}` (regression guard)
- Leftover DEV-11 temp masked-oracle PNGs written by compare.mjs itself during the login run (harmless,
  left in place per "write only here" — not deleted): `<evidence-dir>/.oracle-mask-{desktop,mobile}-{4..8}.png`
- This summary: `<evidence-dir>/../visual.md` (this file)

## Notes / anomalies

- No stack start/stop was performed by this verifier; the `kivvi-int` compose project was already running
  and healthy at the wave SHA when work began and remained so throughout.
- No tracked files were edited; no worker reports, review files, or round-1 evidence were read.
- The only steps skipped from visual/a11y/text comparison were the two the plan's DEV-12 explicitly
  authorizes (`settings` step 10, `customers` step 6, both oracle `documentStatus: 404`) — confirmed by
  reading each journey's oracle `step.json` files directly, not just trusting compare.mjs's own gate.
- `campaigns-email-editor` needed no retry — the packet's "retry once on a cold-stack timeout" contingency
  did not trigger; both the single attempt's command and its clean exit code are recorded above as
  instructed.
