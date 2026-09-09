# Verifier report — dimension: visual — wave-3, round 3

**Dimension status: PASS**, bound to wave SHA `60296f16250c6ebd25f7b4435c795268772fb6be`.

Checkout verified: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual`, detached HEAD =
`60296f16250c6ebd25f7b4435c795268772fb6be` (confirmed via `git rev-parse HEAD`; working tree
clean). Oracle manifest re-hashed independently: `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json`
= `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — matches the packet.
Stack confirmed already running and healthy (`docker compose -p kivvi-int ps`: api/database/mercure
all `Up ... (healthy)`); not started or stopped by this run. `curl -sk https://localhost:19101/` → 200.

No cold-stack timeout occurred on any of the three primary journeys — each compare.mjs invocation
succeeded on the first attempt, so no retry was needed (packet's "retry once on a cold-stack
timeout" contingency was not triggered).

## Per-journey verdicts

| Journey | Role | Verdict | Deviation IDs | Regressions (compare.mjs) |
| --- | --- | --- | --- | --- |
| automations | primary (wave-3) | **parity** | none | 0 |
| settings | primary (wave-3) | **accepted-deviation** | DEV-12 (step 10, 404 document) | 0 |
| campaigns-email-editor | primary (wave-3) | **parity** | none | 0 |
| login | regression guard (earlier wave) | **accepted-deviation** | DEV-11 (steps 4–8, `.main-scroll` mask, wave-4 dashboard body not yet built) | 0 |
| customers | regression guard (earlier wave) | **accepted-deviation** | DEV-12 (step 6, 404 document) | 0 |

No journey in scope produced a `regression` verdict on any step or sub-dimension (url / texts / aria /
screenshotDesktop / screenshotMobile). DEV-4 and DEV-7 (contract-dimension deviations named in the
packet's "Deviations in scope") do not act on the visual dimension and were not exercised here, as
expected.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual/tools/migration-verify` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual/tests/e2e` | 0 |
| 3 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --out <evidence-dir> --dimension visual` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` | 0 |
| 4 | `node tools/migration-verify/compare.mjs --journey settings --base https://localhost:19101 --out <evidence-dir> --dimension visual` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` | 0 |
| 5 | `node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --out <evidence-dir> --dimension visual` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` | 0 |
| 6 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --out <evidence-dir> --dimension visual` (regression guard) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` | 0 |
| 7 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --out <evidence-dir> --dimension visual` (regression guard) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` | 0 |
| 8 | Independent spot-check: for every non-skipped step, `identify -format "%wx%h"` + `compare -metric AE <oracle>.png <candidate>.png /dev/null` (ImageMagick 6.9.12) on `desktop.png`/`mobile.png`, plus `cmp -s`/`diff` on `texts.json` and `a11y.json` | absolute paths, no cwd dependency | 0 (script) |
| 9 | Independent DEV-11 re-check: `convert <oracle-step-N-desktop\|mobile>.png -fill "#ff00ff" -draw "rectangle 248,56 <w-1>,<h-1>" <tmp>.png` then `compare -metric AE <tmp>.png <candidate>.png /dev/null`, for login steps 4–8 (desktop 1440×900, mobile 390×844) | absolute paths | 0 |

`<evidence-dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual`.

## Independent spot-check findings (ImageMagick + byte diffs)

Ran an independent pixel-diff (ImageMagick `compare -metric AE`, no anti-aliasing tolerance —
stricter than compare.mjs's `pixelmatch` threshold 0.1) and a byte-for-byte `diff`/`cmp` of every
`texts.json` and `a11y.json` for every step across all five journeys (39 steps total: 7 + 10 + 8 + 8 + 6),
skipping only the two DEV-12 404 steps (settings step 10, customers step 6 — independently confirmed
`documentStatus: 404` in both the oracle's `step.json` and the candidate's own captured `step.json`).

- **37 of 37 checked steps**: `texts.json` and `a11y.json` byte-identical, oracle vs candidate.
- **Screenshots, all steps except login 4–8**: raw AE ratio ≤ 0.02% (worst case: campaigns-email-editor
  steps 1–3 desktop, AE 298–354/1,296,000 px = 0.02%, vs compare.mjs's own tolerant pixelmatch figure of
  0.008–0.009% for the same steps) — both comfortably inside the ≤ 0.5% budget, and the gap between the
  two tools' numbers is explained by pixelmatch's built-in anti-aliasing tolerance vs ImageMagick's exact
  per-pixel `AE` metric on the same PNGs.
- **login steps 4–8 (DEV-11)**: raw, unmasked AE was large by design (desktop 66.89%, mobile 33.99%) —
  the candidate screenshot already has the `.main-scroll` region painted magenta at capture time
  (wave-4 dashboard body not yet built), while the oracle's stored PNG still shows the real dashboard
  content there. Independently re-painted the identical mask rectangle (origin x=248,y=56; full
  remaining viewport) onto a scratch copy of the oracle PNGs and re-ran `compare -metric AE` against the
  candidate: **AE 0 or ≤ 6px (≈0.0000–0.0005%) for all 5×2 = 10 image pairs**, confirming DEV-11's
  masked-region comparison is legitimate and not concealing a shell-chrome regression outside the masked
  rectangle.
- **login steps 4–8 texts/a11y**: independently recomputed the "candidate is a prefix of oracle" check
  compare.mjs applies under DEV-11 — candidate `texts.json` is exactly the oracle's first 26 lines (of
  182) and candidate `a11y.json` is exactly the oracle's first 35 lines (of 82), for all five steps.
  Confirmed true for every step; no divergence within the claimed prefix.
- No dimension mismatches, no missing files, no unexplained diffs anywhere else.

Raw spot-check transcript: `<evidence-dir>/independent-spotcheck.log`.

## Per-step table (from compare.mjs's own `report.json`, independently cross-checked above)

### automations (parity, 0 regressions)

| Step | url | texts | aria | screenshotDesktop | screenshotMobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity — 0.001% (9/1,296,000) | parity — 0.000% (0/329,160) |
| 2 | parity | parity | parity | parity — 0.000% (3/1,296,000) | parity — 0.000% (0/329,160) |
| 3 | parity | parity | parity | parity — 0.001% (9/1,296,000) | parity — 0.000% (0/329,160) |
| 4 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 5 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 6 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 7 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |

### settings (accepted-deviation DEV-12 on step 10, otherwise parity; 0 regressions)

| Step | url | texts | aria | screenshotDesktop | screenshotMobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 2 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 3 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 4 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 5 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 6 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 7 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 8 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 9 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 10 | skipped | accepted-deviation(DEV-12) — 404 document, visual/a11y/text skipped | (same) | (same) | (same) |

### campaigns-email-editor (parity, 0 regressions)

| Step | url | texts | aria | screenshotDesktop | screenshotMobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity — 0.008% (107/1,296,000) | parity — 0.000% (0/329,160) |
| 2 | parity | parity | parity | parity — 0.009% (114/1,296,000) | parity — 0.000% (0/329,160) |
| 3 | parity | parity | parity | parity — 0.008% (107/1,296,000) | parity — 0.000% (0/329,160) |
| 4 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 5 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 6 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 7 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 8 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |

### login (regression guard — accepted-deviation DEV-11 on steps 4–8, parity on 1–3; 0 regressions)

| Step | url | texts | aria | screenshotDesktop | screenshotMobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 2 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 3 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 4 | parity | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) — 0.000% (0/1,296,000) | accepted-deviation(DEV-11) — 0.000% (0/329,160) |
| 5 | parity | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) — 0.000% (0/1,296,000) | accepted-deviation(DEV-11) — 0.000% (0/329,160) |
| 6 | parity | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) — 0.000% (0/1,296,000) | accepted-deviation(DEV-11) — 0.000% (0/329,160) |
| 7 | parity | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) — 0.000% (0/1,296,000) | accepted-deviation(DEV-11) — 0.000% (0/329,160) |
| 8 | parity | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) | accepted-deviation(DEV-11) — 0.000% (0/1,296,000) | accepted-deviation(DEV-11) — 0.000% (0/329,160) |

### customers (regression guard — accepted-deviation DEV-12 on step 6, parity on 1–5; 0 regressions)

| Step | url | texts | aria | screenshotDesktop | screenshotMobile |
| --- | --- | --- | --- | --- | --- |
| 1 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 2 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 3 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 4 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 5 | parity | parity | parity | parity — 0.000% (0/1,296,000) | parity — 0.000% (0/329,160) |
| 6 | skipped | accepted-deviation(DEV-12) — 404 document, visual/a11y/text skipped | (same) | (same) | (same) |

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/automations/{report.json,report.md,steps/1..7/*}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/settings/{report.json,report.md,steps/1..10/*}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/campaigns-email-editor/{report.json,report.md,steps/1..8/*}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/login/{report.json,report.md,steps/1..8/*}` (regression guard)
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/customers/{report.json,report.md,steps/1..6/*}` (regression guard)
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/independent-spotcheck.log` (this run's independent ImageMagick + byte-diff transcript)
- This summary: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual.md`

## Notes / scope confirmation

- Contract-dimension deviations named in the packet's scope (DEV-4, DEV-7) do not apply to the visual
  dimension and were correctly not triggered by compare.mjs for any of the three primary journeys.
- Round-1/round-2 evidence under `waves/wave-3/round-1/` and `waves/wave-3/round-2/` was not read, per
  the packet's instruction to verify afresh.
- Worker reports, review files, and other verifiers' outputs were not read.
- No tracked files were edited; only the evidence directory and this summary file were written, plus
  `npm ci` in the detached checkout's own `tools/migration-verify/` and `tests/e2e/` (git-ignored
  `node_modules`, no tracked-file changes).
