# Verifier report — wave FINAL (all journeys, round 2), dimension visual

**Dimension status: PASS**, bound to wave SHA `dae169614a52532c130bd34435994d6a914165c0` (checkout `/home/muszkin/work/kivvi-click-wt/verify-final-visual`, detached HEAD `dae1696 test: label B02/B03/B21 unit coverage and pin the main-scroll invariant (#shell-navigation)`, clean working tree, verified before and after the run).

Candidate stack: `https://localhost:19101` (compose project `kivvi-int`), left running throughout, not started or stopped by this verifier. Oracle: `context/migration-oracle/symfony-to-spring-vue` (manifest sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`), read-only, never modified.

No FAILs, no regressions in any of the 12 browser journeys. `scheduler-heartbeat` has no browser steps in `capture/scenarios.json` (`"steps": []`) → **NOT_APPLICABLE** to the visual dimension (its contract-dimension `db.json` shedlock delta is out of scope for this packet).

## Per-journey verdicts (13)

| Journey | Verdict | Regressions | Deviations applied | Notes |
| --- | --- | --- | --- | --- |
| login | **parity** | 0 | none | 8/8 steps parity, incl. steps 4/8 (DEV-11 no longer applies to login — closed at wave-4 `WAVE_INTEGRATED`; DEV-2 canvas mask still applies via normalize.json, unconditionally) |
| landing | **accepted-deviation** | 0 | DEV-11 (step 5) | steps 1–4 parity; step 5 `.main-scroll` masked (rect screenshot mask + aria/text prefix match) per the plan's still-active landing carve-out |
| feeds | **parity** | 0 | none | 1/1 step parity |
| event-stream | **accepted-deviation (mask-only, no tag)** | 0 | DEV-1, DEV-2 (unconditional via normalize.json) | 7/7 steps parity; steps 2–6 show small non-zero residual pixel diffs (0.014% pixelmatch / ~0.025% independent AE) from the `.event-row__time` + cardiogram canvas mask edges, both far under the 0.5% threshold |
| customers | **accepted-deviation** | 0 | DEV-12 (step 6) | steps 1–5 parity; step 6 is the oracle's 404 document (`GET /pl/customers/c_9999`), visual/a11y/text skipped per DEV-12 |
| automations | **parity** | 0 | none | 7/7 steps parity |
| settings | **accepted-deviation** | 0 | DEV-12 (step 10) | steps 1–9 parity; step 10 is the oracle's 404 document (`GET /pl/settings/nonexistent`), skipped per DEV-12 |
| campaigns-email-editor | **parity** | 0 | none | 8/8 steps parity |
| popups-widget-editor | **parity** | 0 | none | 7/7 steps parity |
| import-wizard | **accepted-deviation** | 0 | DEV-12 (step 10) | steps 1–9 parity; step 10 is the oracle's 404 document (`GET /pl/import/5`), skipped per DEV-12 |
| dashboard | **parity** | 0 | none (DEV-1/DEV-2 unconditional, no residual masking artifacts observed) | 4/4 steps parity |
| shell-navigation | **accepted-deviation** | 0 | DEV-12 (step 21) | steps 1–20 parity; step 21 is the oracle's 404 document, skipped per DEV-12 |
| scheduler-heartbeat | **NOT_APPLICABLE** | — | — | `scenarios.json` records 0 browser steps for this journey; nothing for `compare.mjs --dimension visual` to replay |

No journey needed a retry: every `compare.mjs` invocation succeeded on the first attempt (no cold-stack timeouts observed).

## Per-step summary table (94 browser steps across 12 journeys, condensed over identical-verdict runs)

| Journey | Step(s) | Verdict | Max screenshot diff, desktop / mobile (pixelmatch, compare.mjs) | Deviation(s) |
| --- | --- | --- | --- | --- |
| login | 1–8 | parity | 0.000% / 0.000% | - |
| landing | 1–4 | parity | 0.000% / 0.000% | - |
| landing | 5 | accepted-deviation | 0.000% / 0.000% (post-mask) | DEV-11 |
| feeds | 1 | parity | 0.000% / 0.000% | - |
| event-stream | 1 | parity | 0.000% / 0.000% | - |
| event-stream | 2–6 | parity | 0.014% / 0.000% | - |
| event-stream | 7 | parity | 0.000% / 0.000% | - |
| customers | 1–5 | parity | 0.000% / 0.000% | - |
| customers | 6 | accepted-deviation | n/a (skipped, 404) | DEV-12 |
| automations | 1 | parity | 0.001% / 0.000% | - |
| automations | 2 | parity | 0.000% / 0.000% | - |
| automations | 3 | parity | 0.001% / 0.000% | - |
| automations | 4–7 | parity | 0.000% / 0.000% | - |
| settings | 1–9 | parity | 0.000% / 0.000% | - |
| settings | 10 | accepted-deviation | n/a (skipped, 404) | DEV-12 |
| campaigns-email-editor | 1 | parity | 0.008% / 0.000% | - |
| campaigns-email-editor | 2 | parity | 0.009% / 0.000% | - |
| campaigns-email-editor | 3 | parity | 0.008% / 0.000% | - |
| campaigns-email-editor | 4–8 | parity | 0.000% / 0.000% | - |
| popups-widget-editor | 1–7 | parity | 0.000% / 0.000% | - |
| import-wizard | 1–9 | parity | 0.000% / 0.000% | - |
| import-wizard | 10 | accepted-deviation | n/a (skipped, 404) | DEV-12 |
| dashboard | 1–4 | parity | 0.000% / 0.000% | - |
| shell-navigation | 1–3 | parity | 0.000% / 0.000% | - |
| shell-navigation | 4 | parity | 0.001% / 0.000% | - |
| shell-navigation | 5 | parity | 0.008% / 0.000% | - |
| shell-navigation | 6–20 | parity | 0.000% / 0.000% | - |
| shell-navigation | 21 | accepted-deviation | n/a (skipped, 404) | DEV-12 |

All non-zero cells are well under the 0.5% threshold; every accepted-deviation cell carries a deviation id per the plan's Accepted deviations table and `tools/migration-verify/deviations.json`. `texts.json`/`a11y.json` were exact-match (or DEV-11 prefix-match on landing step 5) for all 94 steps — see independent spot-check below.

## Independent spot-check (ImageMagick + byte diff, all 94 steps)

`compare.mjs` uses `pixelmatch` (threshold 0.1) for screenshots and `JSON.stringify` equality (or DEV-11 prefix-match) for `texts.json`/`a11y.json`. As an independent cross-check, a standalone script re-derived pixel-diff percentages with ImageMagick's `compare -metric AE -fuzz 10%` (a different algorithm/tolerance) directly on the same stored PNGs, and byte-diffed `texts.json`/`a11y.json` (or applied the DEV-11 prefix rule) directly against the oracle's stored files, independently of `compare.mjs`'s own comparison code path.

- Coverage: all 94 non-skipped-dimension checks × (desktop + mobile screenshot + texts.json + a11y.json) = 364 individual checks, minus DEV-12 steps which were skipped exactly as `compare.mjs` skips them (`customers` step 6, `settings` step 10, `import-wizard` step 10, `shell-navigation` step 21 — all four confirmed `documentStatus: 404` in the oracle's own `step.json`).
- Result: **0 mismatches**. Every screenshot's independent AE% was ≤ 0.5% (worst case: event-stream steps 2–6 at ~0.025% AE vs. compare.mjs's own 0.014% pixelmatch — different algorithms, same order of magnitude, both far under threshold). Every `texts.json`/`a11y.json` was byte-identical to the oracle (or a valid DEV-11 prefix on landing step 5).
- DEV-11 masking verified directly: painting the same `.main-scroll` rectangle (248,56 → viewport edge) onto a copy of the oracle's landing-step-5 screenshot and diffing against the candidate's already-masked screenshot gave AE=5/1,296,000 px (~0.0004%), confirming the mask was applied identically on both sides rather than coincidentally matching.
- DEV-1/DEV-2 masking verified as non-trivial: event-stream steps 2–6 show a genuine small residual diff (edge antialiasing around the masked `.event-row__time`/cardiogram regions), ruling out a false "0% because nothing rendered" result.

Spot-check script and its full 364-line log are scratch artifacts (not written to the evidence dir per the packet's write-only-there rule); available on request but not required for the verdict since every number is independently reproducible from the oracle + evidence dir.

## Commands run

All commands run with `cwd=/home/muszkin/work/kivvi-click-wt/verify-final-visual` unless noted.

| Command | Exit code |
| --- | --- |
| `cd tests/e2e && npm ci --no-audit --no-fund` | 0 |
| `cd tools/migration-verify && npm ci --no-audit --no-fund` | 0 |
| `npx playwright --version` (tests/e2e) → `1.62.1` | 0 |
| `curl -sk https://localhost:19101/` → 200 | 0 |
| `curl -sk https://localhost:19101/pl/login` → 200 | 0 |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey feeds --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey settings --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey dashboard --base https://localhost:19101 --dimension visual --out <evidence-dir>` | 0 |
| `VERIFY_RUN_ID=finalv2 node tools/migration-verify/compare.mjs --journey shell-navigation --base https://localhost:19101 --dimension visual --out <evidence-dir>` (timeout 300s wrapper, completed well within it) | 0 |
| Independent spot-check: `python3 spotcheck.py` (scratch dir; ImageMagick `compare -metric AE -fuzz 10%` + `convert -draw rectangle` for DEV-11 + byte/prefix diff of `texts.json`/`a11y.json`), 364 checks | 0 (no mismatches) |
| Cleanup: `rm -rf tests/e2e/node_modules tools/migration-verify/node_modules` (checkout) | 0 |
| Cleanup: `rm -rf <evidence-dir>/<journey>/steps` for all 12 journeys + `rm -f <evidence-dir>/.oracle-mask-*.png` | 0 |

`<evidence-dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual`.

`VERIFY_COMPOSE` was set only for the first (login) invocation for parity with the packet's environment note; the visual dimension never calls `dbCounts()` (that only fires when `--dimension` includes `contract`), so it had no effect and was dropped from the remaining commands.

## Evidence paths (final contents — screenshots/JSON step artifacts removed after verification per the disk mandate)

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/login/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/landing/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/feeds/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/event-stream/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/customers/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/automations/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/settings/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/campaigns-email-editor/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/popups-widget-editor/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/import-wizard/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/dashboard/{report.json,report.md}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual/shell-navigation/{report.json,report.md}`
- Summary: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/final/visual.md` (this file)

**Kept:** `report.json` + `report.md` for all 12 journeys. **Deleted:** every `steps/<n>/{desktop.png,mobile.png,desktop.diff.png,mobile.diff.png,a11y.json,texts.json,http.jsonl,step.json,url.txt}` directory (0 regressions in any journey, so there were no regression diff images to retain) and the two stray `.oracle-mask-desktop-5.png` / `.oracle-mask-mobile-5.png` temp files `compare.mjs` leaves at the evidence-dir root when comparing landing step 5's DEV-11-masked screenshot. Final evidence footprint: 192 KB (was 42 MB before cleanup). `node_modules` in `tests/e2e/` and `tools/migration-verify/` (checkout only) removed after the run; checkout confirmed clean (`git status --short` empty) with no other untracked files.
