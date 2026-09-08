# Verifier report — wave wave-2 (round 2), dimension visual

Independent read-only verification. No worker reports or review files were read.

**Dimension status: PASS** — bound to wave SHA `b87a701244f5316e1a53bace7a1e41facdffc277`

## Journey verdicts

| Journey | Verdict | Deviation ids observed | Regressions |
| --- | --- | --- | --- |
| event-stream | parity | none (all steps `parity`) | 0/7 steps |
| customers | accepted-deviation | DEV-12 (step 6, 404 document) | 0/6 steps |

## Setup and identity

- Verifier checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-visual`, detached HEAD at `b87a701244f5316e1a53bace7a1e41facdffc277` (matches packet), working tree clean (`git status --short` empty).
- Running stack: `https://localhost:19101`, compose project `kivvi-int`. Cross-checked: `kivvi-int-api-1`'s compose config file is `/home/muszkin/work/kivvi-click-wt/verify-wave-2/compose.next.yaml` (the orchestrator's build checkout); that checkout's `git rev-parse HEAD` is also `b87a701244f5316e1a53bace7a1e41facdffc277` with a clean tree — the serving image is bound to the wave SHA. Image created `2026-09-08T22:45:06Z`, minutes before this run.
- Did not start/stop any stack. Did not edit tracked files. Did not read `round-1/` evidence or any worker/review files.
- `npm ci` run only inside `tools/migration-verify/` and `tests/e2e/` of my own checkout (visual dimension needs Playwright's chromium from `tests/e2e/node_modules` via `compare.mjs`'s `createRequire`, plus `pixelmatch`/`pngjs` in `tools/migration-verify/`). `frontend/` was not touched — not required for `compare.mjs --dimension visual`.

## Commands run

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `npm ci` | `.../verify-wave-2-visual/tools/migration-verify` | 0 |
| 2 | `npm ci` | `.../verify-wave-2-visual/tests/e2e` | 0 |
| 3 | `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension visual --out .../wave-2/visual/compare-out` | `.../verify-wave-2-visual` | 0 |
| 4 | `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out .../wave-2/visual/compare-out` | `.../verify-wave-2-visual` | 0 |

Both journeys' first run completed cleanly (no cold-stack timeout) — **no retry was needed**; only one run each was recorded (`run1-event-stream.log`, `run1-customers.log`).

## Independent spot-checks (not derived from compare.mjs's own numbers)

### 1. ImageMagick `compare` pixel counts (cross-check against pixelmatch's report)

Ran `compare -metric AE -fuzz 2% <oracle>/<step>/<viewport>.png <candidate>/.../<viewport>.png` for every non-404 step/viewport of both journeys (`spotcheck/summary.csv`). ImageMagick's fuzz-based AE metric is a different algorithm from pixelmatch (no antialiasing-aware matching), so absolute counts differ from compare.mjs's own numbers, but both stayed far under the 0.5% threshold:

- event-stream steps 1, 7 (page loads/scroll-to view, no POST in between): 0.0000% both viewports.
- event-stream steps 2–6 (each follows a `/collect` POST): 0.0353% desktop (458/1,296,000 px), 0.0000% mobile — vs. pixelmatch's own 0.019% (246/1,296,000). Traced the diff to a small bounding box (65×11 px at offset 875,405) via `convert ... -compose difference -composite -threshold 5% -trim`. Cropped both PNGs at that region: the difference is the `ORACLE-<run-id>` suffix inside the event-row detail text (`ORACLE-cap4d` in the oracle vs. a freshly-generated `ORACLE-mtt9epxfd` in this replay). This is expected: `compare.mjs` deliberately re-randomizes `__RUN__` per invocation (documented in the source, lines ~90–97) so a real, non-idempotent `/collect` POST doesn't collide with the oracle's own dedup key; `normalize.json`'s text rule `ORACLE-[a-z0-9]+ → <RUN-ID>` absorbs this for texts/a11y but there is no screenshot mask for it, so a small, harmless pixel delta is inherent to every replay and is already inside the 0.5% budget — not a regression, not a gap needing a new deviation id.
- customers steps 1–5: 0.0000% both viewports, both metrics.
- customers step 6: skipped (oracle `documentStatus: 404`, DEV-12).

Diff artifacts: `spotcheck/*.diff.png`, `spotcheck/crops/*.png`, raw counts in `spotcheck/summary.csv`.

### 2. `texts.json` / `a11y.json` byte-for-byte diff (oracle vs. candidate, independent of compare.mjs's own JSON-equality check)

`diff -q` on every non-404 step's `texts.json` and `a11y.json` for both journeys: **all IDENTICAL** (26 file pairs: 7×2 event-stream + 5×2 customers; customers step 6 skipped for DEV-12). Full list in `spotcheck/text-a11y-diff.csv`.

## Per-step table

### event-stream (7 steps, all in scope — no 404s)

| Step | url | texts | a11y | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 2 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.019% (246/1,296,000; run-id text, see spot-check) | parity, 0.000% |
| 3 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.019% (246/1,296,000) | parity, 0.000% |
| 4 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.019% (246/1,296,000) | parity, 0.000% |
| 5 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.019% (246/1,296,000) | parity, 0.000% |
| 6 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.019% (246/1,296,000) | parity, 0.000% |
| 7 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |

### customers (6 steps; step 6 is DEV-12)

| Step | url | texts | a11y | screenshot desktop | screenshot mobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 2 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 3 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 4 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 5 | parity | parity (byte-identical) | parity (byte-identical) | parity, 0.000% | parity, 0.000% |
| 6 | — | — | — | accepted-deviation(DEV-12) — 404 document, visual/a11y/text skipped by design | accepted-deviation(DEV-12) |

## Evidence paths

- `compare-out/event-stream/report.md`, `compare-out/event-stream/report.json`, `compare-out/event-stream/steps/1..7/{desktop,mobile}.png,.diff.png,texts.json,a11y.json,url.txt,step.json,http.jsonl}`
- `compare-out/customers/report.md`, `compare-out/customers/report.json`, `compare-out/customers/steps/1..6/{...}` (step 6 has no `.diff.png` — skipped by DEV-12)
- `run1-event-stream.log`, `run1-customers.log` — raw compare.mjs console output for the (single, successful) run of each journey
- `spotcheck/summary.csv` — independent ImageMagick AE pixel counts per step/viewport
- `spotcheck/*.diff.png` — ImageMagick difference renders
- `spotcheck/crops/*.png` — cropped region used to trace the event-stream run-id pixel delta
- `spotcheck/text-a11y-diff.csv` — byte-for-byte `diff -q` results for every `texts.json`/`a11y.json` pair

## Verdict summary

**visual: PASS.** Both journeys within the ≤0.5% screenshot-pixel threshold on every applicable step/viewport (max observed 0.035% by independent fuzzy AE metric, 0.019% by the tool's own pixelmatch), with `texts.json` and `a11y.json` byte-for-byte identical to the oracle everywhere it was possible to compare. Customers step 6 correctly invokes DEV-12 (oracle `documentStatus: 404`) and is excluded from visual/a11y/text comparison by design, matching the plan's accepted-deviation table. Zero regressions reported by `compare.mjs` and zero found by independent spot-check. No cold-stack timeout occurred, so no retries were needed.
