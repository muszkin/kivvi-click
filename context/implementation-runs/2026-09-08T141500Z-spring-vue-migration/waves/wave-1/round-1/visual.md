# Wave-1 verifier — dimension `visual`

Status: **PASS** — bound to wave SHA `9427fdb1d92e4e606e67471638031d1bc0fb73d4`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual` (HEAD confirmed == wave SHA, clean, untouched).
Stack: `https://localhost:19101` (compose project `kivvi-int`).
Oracle manifest sha256 confirmed: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet).

## Journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| landing | **parity** (steps 1–4), **accepted-deviation(DEV-11)** (step 5) | screenshots exact (0 px diff, desktop+mobile), texts/a11y byte-identical steps 1–4; step 5 `.main-scroll` masked per plan (landing now in scope of DEV-11 per packet) |
| feeds | **parity** | step 1 screenshots exact (0 px diff, desktop+mobile), texts/a11y byte-identical |
| scheduler-heartbeat | **NOT_APPLICABLE** | backend-only journey: `capture/scenarios.json["scheduler-heartbeat"].steps == []`, oracle dir has no `steps/` folder, packet itself states "no browser spec (static journey)". No screenshots/a11y/texts exist to compare — nothing for the visual dimension to assert. |

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `.../verify-wave-1-visual/tests/e2e` | 0 |
| `npm ci` | `.../verify-wave-1-visual/tools/migration-verify` | 0 |
| `node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19101 --dimension visual --out <evidence>/waves/wave-1/visual` | `.../verify-wave-1-visual` | 0 |
| `node tools/migration-verify/compare.mjs --journey feeds --base https://localhost:19101 --dimension visual --out <evidence>/waves/wave-1/visual` | `.../verify-wave-1-visual` | 0 |

## Per-step table

| Journey | Step | Desktop diff | Mobile diff | Texts | A11y | Deviation |
| --- | --- | --- | --- | --- | --- | --- |
| landing | 1 | 0.000% (0/1,296,000); independent AE=0 | 0.000% (0/329,160); independent AE=0 | identical | identical | — |
| landing | 2 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 3 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 4 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 5 | 0.000% (masked) | 0.000% (masked) | valid prefix (26/182 lines) | valid prefix (35/82 lines) | DEV-11 |
| feeds | 1 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |

## Independent audit (beyond compare.mjs's own pixelmatch verdicts)

- **Unmasked exact-pixel audit** (ImageMagick `compare -metric AE`, zero fuzz — stricter than compare.mjs's `pixelmatch` threshold 0.1) on landing steps 1–4 and feeds step 1, desktop + mobile: **AE = 0** on every screenshot. Full pixel identity, not just "under threshold."
- **texts.json / a11y.json**: `diff -q` oracle vs candidate for every unmasked step — byte-identical in all cases (checked above).
- **DEV-11 mask geometry, landing step 5**:
  - Candidate's baked-in magenta fill (`#ff00ff`) covers exactly the rectangle `(248,56)–(1439,899)` on desktop (pixel count 1,006,048 == `(1440-248)×(900-56)`; bounding box matches exactly) and `(248,56)–(389,843)` on mobile — no bleed outside the declared `.main-scroll` rect on either viewport.
  - Sidebar+topbar region (outside the mask rect) audited by exact-pixel AE: topbar strip AE=0 (desktop+mobile); sidebar strip AE=30/223,200 px (desktop only) — inspected pixel-by-pixel: all 30 are sub-threshold anti-aliasing noise (RGB deltas ≤ 18) in a ~20×30px avatar-icon region near the sidebar foot (y 855–884), well inside compare.mjs's pixelmatch threshold (which reports 0 for this region) and two orders of magnitude below the 0.5% budget even taken alone (0.013%). Not a content regression — sidebar/topbar are functionally pixel-identical.
  - Mobile: the unmasked strip left of the fixed rect (`x:0–248, y:56–844`, 195,424 px) is real rendered sidebar content (confirmed via histogram — cream/brown/green tones consistent with the sidebar, not blank canvas) and is AE=0 against oracle, i.e. the sidebar renders identically at mobile viewport width too and the fixed-rect mask does not need to (and does not) hide any leaked dashboard-body content there.
  - texts/a11y prefix check for step 5: candidate's 26 text lines / 35 a11y lines are an exact line-for-line prefix of the oracle's 182/82 lines; the oracle's first divergent line is the dashboard body heading `"Co dzieje się teraz"` — confirms the DOM mask removed exactly the `.main-scroll` subtree and nothing from the shell chrome.

## Deviation ledger applied

- DEV-11 — landing step 5 only (per packet's explicit extension: "DEV-11 now also covers landing step 5, which lands on the still-empty dashboard"). Verified as accepted-deviation, not silently absorbed as parity, and not misapplied to steps 1–4.
- DEV-1, DEV-2, DEV-12 — confirmed **not** applicable to these two journeys (landing/feeds have no `.event-row__time`, no cardiogram canvas, no 404 steps in scope this wave) and were correctly not invoked by `deviations.json`'s journey filter.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/visual/landing/report.{md,json}` + `steps/{1..5}/{desktop,mobile}.png,.diff.png,a11y.json,texts.json,url.txt,http.jsonl,step.json`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/visual/feeds/report.{md,json}` + `steps/1/{desktop,mobile}.png,.diff.png,a11y.json,texts.json,url.txt,http.jsonl,step.json`
- `.oracle-mask-{desktop,mobile}-5.png` (tool-internal masked oracle copies used by compare.mjs for step 5)

## Scheduler-heartbeat — NOT_APPLICABLE reasoning

Per the packet, scheduler-heartbeat has no browser spec: it is verified via `contract.md` and the integration test/api log, not a Playwright journey. Independently confirmed: `context/migration-oracle/symfony-to-spring-vue/capture/scenarios.json["scheduler-heartbeat"].steps` is an empty array, and `journeys/scheduler-heartbeat/` contains only `contract.md` and `scenario.md` — no `steps/` directory, no screenshots, no a11y/texts capture exists in the oracle for this journey. There is nothing for the visual dimension to assert against, so `visual` for scheduler-heartbeat is NOT_APPLICABLE rather than PASS or FAIL.
