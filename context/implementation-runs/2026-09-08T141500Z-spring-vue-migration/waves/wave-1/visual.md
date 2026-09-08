# Wave-1 verifier — dimension `visual` (round 2)

Status: **PASS** — bound to wave SHA `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual` (HEAD confirmed `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`, matches packet's wave SHA).
Stack: `https://localhost:19101` (compose project `kivvi-int`; `curl -sk https://localhost:19101/pl` → 200, `/pl/feeds` → 200; containers `api`/`database`/`mercure` healthy).

## Journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| landing | **parity** (steps 1–4), **accepted-deviation(DEV-11)** (step 5) | screenshots exact, texts/a11y byte-identical steps 1–4; step 5 `.main-scroll` masked per DEV-11 (extended to landing) |
| feeds | **parity** | step 1 screenshots exact, texts/a11y byte-identical |
| scheduler-heartbeat | **NOT_APPLICABLE** | oracle `capture/scenarios.json["scheduler-heartbeat"]` has `"static": true, "steps": []`; `journeys/scheduler-heartbeat/` contains only `contract.md` and `scenario.md` — no `steps/` dir, no screenshots/a11y/texts captured. There is no browser surface and nothing for the visual dimension (screenshots/a11y/texts) to compare — verified via B20/B33 under the contract dimension instead, per the packet. |

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual/tests/e2e` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual/tools/migration-verify` | 0 |
| `node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual` | 0 |
| `node tools/migration-verify/compare.mjs --journey feeds --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual` | 0 |
| `compare -metric AE <oracle>/steps/N/{desktop,mobile}.png <candidate>/steps/N/{desktop,mobile}.png /dev/null` (ImageMagick, zero fuzz) — landing 1–4, feeds 1 | (ad hoc, both checkout + evidence dir) | AE=0 every image |
| `diff <oracle>/steps/N/{texts,a11y}.json <candidate>/steps/N/{texts,a11y}.json` — landing 1–5, feeds 1 | (ad hoc) | 0 for all except landing step 5 (expected DEV-11 prefix divergence) |
| node script using `pngjs`+`pixelmatch` (threshold 0.1, same setting as compare.mjs) on cropped chrome strips of landing step 5 | (ad hoc) | 0 diff pixels |
| `grep -n "sidebar-w\|topbar-h" frontend/src/styles/01-tokens.css` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-visual` | confirms `--sidebar-w: 248px`, `--topbar-h: 56px` |

## Per-step table

| Journey | Step | Desktop diff | Mobile diff | Texts | A11y | Deviation |
| --- | --- | --- | --- | --- | --- | --- |
| landing | 1 | 0.000% (compare.mjs); independent AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 2 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 3 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 4 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |
| landing | 5 | 0.000% (masked rect) | 0.000% (masked rect) | valid prefix | valid prefix | DEV-11 |
| feeds | 1 | 0.000%; AE=0 | 0.000%; AE=0 | identical | identical | — |

## Independent audit (beyond compare.mjs's own verdicts)

- **Unmasked exact-pixel audit** (ImageMagick `compare -metric AE`, zero fuzz, stricter than compare.mjs's `pixelmatch` threshold 0.1) on landing steps 1–4 and feeds step 1, desktop + mobile: **AE = 0** on every screenshot — full pixel identity.
- **texts.json / a11y.json**: `diff` oracle vs candidate for every step in scope — byte-identical for landing steps 1–4 and feeds step 1.
- **DEV-11 mask geometry, landing step 5**: verified `.main-scroll` is the only masked region and chrome is untouched.
  - Cropped the candidate/oracle desktop screenshots into the complement of the mask rectangle `(248,56)–(1440,900)`: left sidebar strip `0,0–248,900` and top strip `248,0–1440,56`. Top strip: AE=0. Sidebar strip: raw AE=30/223,200 px (0.013%), all with RGB deltas ≤18 concentrated in a ~20×30px region at y 855–884 (sidebar footer text). Re-checked that same crop with `pixelmatch` at the identical `threshold: 0.1` setting compare.mjs itself uses: **0 diff pixels** — confirms sub-pixel font antialiasing noise, not a content regression.
  - Mobile: complement strips (left `0,0–248,844`, top `248,0–390,56`) both AE=0 — chrome pixel-identical on mobile too.
  - `--sidebar-w: 248px` and `--topbar-h: 56px` confirmed in `frontend/src/styles/01-tokens.css`, matching `compare.mjs`'s `MAIN_SCROLL_ORIGIN` exactly — the mask rectangle is correctly derived from the design tokens, not an arbitrary guess.
  - texts/a11y prefix check: candidate's step-5 lines are an exact line-for-line prefix of the oracle's (candidate ends at `button "Powiadomienia"`, the last topbar element; oracle's next line is the dashboard body heading `"Co dzieje się teraz"`) — confirms the DOM mask removed exactly the `.main-scroll` subtree and nothing from shell chrome.

## Deviation ledger applied

- **DEV-11** — landing step 5 only, confirmed via `deviations.json`'s `steps: { "landing": [5] }`; correctly reported as `accepted-deviation(DEV-11)`, not silently folded into `parity`, and not applied to steps 1–4.
- DEV-1, DEV-2, DEV-12 — confirmed not invoked for landing/feeds (no `.event-row__time`, no cardiogram canvas, no 404 step in scope this wave); `journeyDevs` filter in `compare.mjs` correctly scopes deviations by journey id.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/visual/landing/report.{md,json}` + `steps/{1..5}/{desktop,mobile}.png,.diff.png,a11y.json,texts.json,url.txt,http.jsonl,step.json`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/visual/feeds/report.{md,json}` + `steps/1/{desktop,mobile}.png,.diff.png,a11y.json,texts.json,url.txt,http.jsonl,step.json`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/visual/.oracle-mask-{desktop,mobile}-5.png` (compare.mjs's internal masked-oracle copies for step 5)

## Scheduler-heartbeat — NOT_APPLICABLE reasoning

`context/migration-oracle/symfony-to-spring-vue/capture/scenarios.json["scheduler-heartbeat"]` declares `"static": true, "steps": []`. `journeys/scheduler-heartbeat/` on disk holds only `contract.md` and `scenario.md` — no `steps/` directory exists, so there are no screenshots, a11y snapshots, or texts captures in the oracle for this journey at all. The visual dimension has no browser surface to assert against here (per packet: "scheduler-heartbeat → no browser spec (static journey): contract.md, B20/B33 via the IT and the api log"), so the correct verdict is NOT_APPLICABLE, not PASS or FAIL.
