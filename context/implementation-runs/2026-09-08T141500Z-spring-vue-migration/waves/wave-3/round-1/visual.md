# Wave-3 verifier report — dimension: visual (round 1)

**Wave SHA:** `32a68311594e611aaa8eaa0803bc72bba7d735a9` (checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual`, detached, clean, not modified by this verification)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`), already running, not started/stopped by this verifier.

## Dimension status: **FAIL** (bound to `32a68311594e611aaa8eaa0803bc72bba7d735a9`)

One unaccepted visual regression found in the primary journey `campaigns-email-editor` (step 6, mobile screenshot). No accepted deviation in the plan's "Accepted deviations" table or `tools/migration-verify/deviations.json` covers it. Everything else (both remaining wave-3 journeys, and the `login`/`customers` regression guards for this wave's shell changes) is parity or a correctly-applied accepted deviation.

## Per-journey verdicts

| Journey | Role | compare.mjs regressions | Verdict | Deviation id(s) |
| --- | --- | --- | --- | --- |
| `automations` | primary (wave-3) | 0 | **parity** | none |
| `settings` | primary (wave-3) | 0 | **parity** (step 10 skipped) | DEV-12 (step 10, 404 document) |
| `campaigns-email-editor` | primary (wave-3) | 1 | **regression** | DEV-7 applies to contract dim only (not this finding); no visual deviation covers step 6 |
| `login` | regression guard (shell: scroll restoration, locale toggle) | 0 | **parity** (steps 4–8 masked) | DEV-11 (steps 4–8, dashboard body not yet implemented — verified prefix match, not superficial) |
| `customers` | regression guard (shell: scroll restoration, locale toggle) | 0 | **parity** (step 6 skipped) | DEV-12 (step 6, 404 document) |

### The regression: `campaigns-email-editor` step 6, mobile screenshot

Step 6 is `{"reload": true, "waitAttr": ["html","data-theme","dark"]}` — the journey's only reload step (confirmed: it is also the *only* `reload: true` step anywhere in wave-0..wave-3 journeys; `shell-navigation`'s two reload steps belong to wave-5, out of scope here). This directly exercises shell change (1) named in the packet ("vue-router `scrollBehavior` + reload scroll restoration").

- `compare.mjs` (pixelmatch, threshold 0.1): **5.286% pixels differ (17401/329160)**, verdict `regression`. Desktop screenshot for the same step: 0.000% (parity). All other 7 steps × 2 viewports: parity.
- Independent spot-check (ImageMagick `compare -metric AE -fuzz 3%`, this verifier, not part of the pipeline): **69.715% (229473/329160)** differing pixels — same direction, far over threshold by either metric. Diff image: `_independent-checks/campaigns-email-editor_step6_mobile.imagemagick-diff.png`.
- `texts.json` and `a11y.json` for step 6 are **byte-identical** between oracle and candidate (confirmed both by `compare.mjs`'s own `parity` verdict and by this verifier's independent `cmp`). This isolates the regression to rendering/scroll state, not missing or wrong DOM content: the oracle's mobile screenshot at steps 4/5/6 shows the editor scrolled to a non-default offset (visible as a narrow cut-off strip of text on the left edge, consistent across steps 4→5→6 in the oracle), while the candidate at step 6 renders the un-scrolled top of the editor (full sidebar, "Powrót do koszyka — wariant A" heading, block library) instead of reproducing that same scroll offset after the reload. Desktop is unaffected because the desktop viewport's pre-reload scroll state happens to already be at the top.
- Not a cold-stack timeout (the run completed normally, exit 1, with a fully-populated report) and reproduced identically across the pixelmatch and ImageMagick methods, so no retry was performed per the packet's retry condition ("retry once on a cold-stack timeout" — not applicable here).
- No deviation in `tools/migration-verify/deviations.json` names `campaigns-email-editor` step 6 for the visual dimension: DEV-7 (also scoped to `campaigns-email-editor`) is contract-only (`POST …/blocks` absence) and explicitly "not applicable" outside contract. DEV-1/DEV-2/DEV-11/DEV-12 all name other journeys/steps.

Evidence: `campaigns-email-editor/steps/6/{desktop,mobile}.png`, `mobile.diff.png` (pixelmatch), `report.json`, `report.md`.

## Regression guards (login, customers) — shell changes in scope

Wave-3 note names two shell changes to guard: (1) vue-router `scrollBehavior` + reload scroll restoration, (2) generic locale toggle via `route.meta.defaultParams`.

- `login` has no `reload` step, so it cannot exercise (1) directly; its own DEV-11-masked steps 4–8 (out-of-band POSTs, dashboard body still empty per wave-4 dependency) passed with a genuine prefix match, independently re-derived: oracle texts/aria for steps 4–8 extend the candidate's shell-only content with dashboard-body lines (`heading "Co dzieje się teraz"`, `Pulsacja zdarzeń`, …) that are absent from the candidate, and the candidate's lines are an exact prefix of the oracle's, matching DEV-11's documented mechanism (not a superficial pass).
- `login` and `customers` both exercise the locale toggle (`PL` link → `/en/…`) as static content (`link "PL": /url: /en/dashboard` etc. in the a11y tree) — matched exactly, no regression.
- `customers` step 6 (`/pl/customers/c_9999`) is the oracle's 404 document, correctly skipped under DEV-12.
- Neither regression guard shows any pixel/text/aria regression. **The scroll-restoration defect found in `campaigns-email-editor` step 6 is not reproduced/detectable by these two guards**, because neither guard journey contains a `reload` step — this is a real gap in wave-2's regression-guard coverage for wave-3's reload-scroll-restoration change, worth flagging to the orchestrator even though it doesn't change this dimension's verdict (the primary journey itself already fails).

## Per-step table

Full per-step detail is in each journey's `report.md` (screenshot %, url/texts/aria verdicts) — not reproduced line-by-line here beyond the summary above and the regression callout. Condensed:

| Journey | Steps | All-parity steps | Deviation-covered steps | Regression steps |
| --- | --- | --- | --- | --- |
| automations | 7 | 7 | 0 | 0 |
| settings | 10 | 9 | 1 (step 10, DEV-12) | 0 |
| campaigns-email-editor | 8 | 7 | 0 | 1 (step 6, mobile screenshot only; url/texts/aria/desktop at step 6 all parity) |
| login | 8 | 3 | 5 (steps 4–8, DEV-11) | 0 |
| customers | 6 | 5 | 1 (step 6, DEV-12) | 0 |

Independent per-step ImageMagick + byte-diff spot-check (every non-404 step, both viewports, `texts.json`/`a11y.json`) is in `_independent-checks/imagemagick-and-text-spotcheck.log`. It corroborates every `compare.mjs` verdict above: all "parity" steps show ≤0.023% AE (antialiasing-level noise, well under the 0.5% threshold and consistent across desktop/mobile), all DEV-11/DEV-12 steps show exactly the masked/skipped pattern the deviation describes (not silently passing something unrelated), and the one regression (`campaigns-email-editor` step 6 mobile) is reproduced with a much larger AE than pixelmatch reports (different metric/fuzz, same conclusion).

## Commands run

All commands from `cwd = /home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` unless noted.

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `curl -sk -o /dev/null -w "HTTP %{http_code}\n" https://localhost:19101/pl/login --max-time 10` | (any) | 0 (HTTP 200 — stack reachable) |
| 2 | `npm ci` | `tools/migration-verify` | 0 |
| 3 | `npm ci` | `tests/e2e` | 0 |
| 4 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --dimension visual --out <run-dir>/waves/wave-3/visual` | `verify-wave-3-visual` | 0 (0 regressions) |
| 5 | `node tools/migration-verify/compare.mjs --journey settings --base https://localhost:19101 --dimension visual --out <run-dir>/waves/wave-3/visual` | `verify-wave-3-visual` | 0 (0 regressions) |
| 6 | `node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --dimension visual --out <run-dir>/waves/wave-3/visual` | `verify-wave-3-visual` | **1** (1 regression — step 6 mobile screenshot; not a timeout, no retry per contract) |
| 7 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <run-dir>/waves/wave-3/visual` | `verify-wave-3-visual` | 0 (0 regressions — regression guard) |
| 8 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <run-dir>/waves/wave-3/visual` | `verify-wave-3-visual` | 0 (0 regressions — regression guard) |
| 9 | `bash spotcheck.sh` (this verifier's own script: `identify` + `compare -metric AE -fuzz 3%` per step/viewport, `cmp`/`diff` on `texts.json`/`a11y.json`, skipping DEV-12 404 steps) — independent of `compare.mjs`, uses ImageMagick `compare`/`identify` (`/usr/bin/compare`, `/usr/bin/identify`) | scratchpad | 0 (spot-check completed; log saved as evidence) |

`<run-dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration`.

No retries were performed: none of the five `compare.mjs` invocations timed out or crashed — each produced a complete `report.json`/`report.md` on the first attempt, so the packet's "retry once on a cold-stack timeout" condition never triggered.

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/visual/`:

- `automations/{report.json,report.md,steps/1..7/**}`
- `settings/{report.json,report.md,steps/1..9/**}` (step 10 has no screenshot/a11y/texts output — DEV-12 skip)
- `campaigns-email-editor/{report.json,report.md,steps/1..8/**}` — see especially `steps/6/{desktop.png,mobile.png,mobile.diff.png,texts.json,a11y.json}`
- `login/{report.json,report.md,steps/1..8/**}`
- `customers/{report.json,report.md,steps/1..5/**}` (step 6 has no screenshot/a11y/texts output — DEV-12 skip)
- `_independent-checks/imagemagick-and-text-spotcheck.log` — this verifier's independent per-step spot-check (all 5 journeys, ImageMagick AE + byte diffs)
- `_independent-checks/campaigns-email-editor_step6_mobile.imagemagick-diff.png` — this verifier's independent diff render for the regression step

## Recommendation

Dimension **FAIL** for wave-3 round 1. The one blocking finding is narrowly scoped: `campaigns-email-editor` step 6, mobile viewport only, screenshot only (texts/aria/url and the desktop viewport all pass) — consistent with a reload-time scroll-restoration gap specific to this journey's editor page on the mobile breakpoint. Recommend the repair round target `frontend/src/router/scrollRestoration.ts` / `router/index.ts` behavior for this page and re-run `node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --dimension visual` plus the `login`/`customers` guards again on the fixed SHA.
