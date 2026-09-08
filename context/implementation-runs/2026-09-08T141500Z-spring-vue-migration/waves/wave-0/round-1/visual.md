# Verifier report — wave-0, dimension `visual`

**Wave SHA:** `3b17c07e23354e493edbab572e4090f69d2c0c71` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual`, `git rev-parse HEAD` matches)
**Oracle:** `context/migration-oracle/symfony-to-spring-vue`, manifest sha256 `4945a8de…0a90f74f` — verified via `sha256sum manifest.json` (match)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`) — `curl -sk https://localhost:19101/pl/login` → 200

## Status: PASS (bound to wave SHA `3b17c07e...`)

## Journey verdict

| Journey | Verdict | Notes |
| --- | --- | --- |
| login | **parity / accepted-deviation** | Steps 1–3 exact parity (0 px diff). Steps 4–8 accepted-deviation(DEV-11) — dashboard body correctly masked/absent; shell chrome outside the mask is exact parity. DEV-12 not applicable (no step has `documentStatus: 404`). |

## Per-step table

| Step | URL | Desktop % | Mobile % | Texts | A11y | Deviation |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | /pl/login | 0.000% | 0.000% | parity | parity | — |
| 2 | /pl/login | 0.000% | 0.000% | parity | parity | — |
| 3 | /pl/login | 0.000% | 0.000% | parity | parity | — |
| 4 | /pl/dashboard | 0.000%* | 0.000%* | prefix-parity | prefix-parity | DEV-11 |
| 5 | /pl/dashboard | 0.000%* | 0.000%* | prefix-parity | prefix-parity | DEV-11 |
| 6 | /pl/dashboard | 0.000%* | 0.000%* | prefix-parity | prefix-parity | DEV-11 |
| 7 | /pl/dashboard | 0.000%* | 0.000%* | prefix-parity | prefix-parity | DEV-11 |
| 8 | /pl/dashboard | 0.000%* | 0.000%* | prefix-parity | prefix-parity | DEV-11 |

\* Reported after both sides paint the identical `.main-scroll` rectangle magenta (mask-vs-mask is trivially equal); the number that matters is that the **unmasked** shell (sidebar + topbar) is itself 0.000% — confirmed independently, see Audit (a)/(c).

## Independent audit (tool not trusted on its own report)

**(a) Steps 1–3, no-mask independent diff (ImageMagick `compare -metric AE`, no masks, not the tool's pixelmatch):**
All six comparisons (desktop+mobile × steps 1–3) → `AE=0` (0 of 1,296,000 desktop px / 0 of 329,160 mobile px). Well under the ≤0.5% threshold — exact.

**(b) `diff` of oracle vs candidate `texts.json`/`a11y.json`:**
Steps 1–3: byte-identical (`diff` exit 0). Steps 4,5,6,7,8: diverge at exactly the same point in every step — immediately after the `"PL"` locale-toggle entry, which is the last shell-chrome line before the oracle's dashboard-body content (`"Co dzieje się teraz"` heading …). Verified programmatically that the candidate's `a11y.json` string is an exact prefix of the oracle's (`oracle.startsWith(candidate) === true`, cut point right before `heading "Co dzieje się teraz"`). No unmasked text/role differs anywhere; no difference lacks a deviation id.

**(c) DEV-11 mask geometry, from the diff/candidate PNGs directly (pngjs, no library trust):**
Located the exact bounding box of the painted `0xff00ff` rectangle in candidate desktop step 4: `(248,56)–(1439,899)` on a 1440×900 canvas — i.e. everything right of the 248px sidebar and below the 56px topbar strip, matching `--sidebar-w`/`--topbar-h` design tokens 1:1. Sidebar and topbar pixels are real, uncovered, compared content (confirmed visually — chrome text/icons render identically to the oracle, including the identity swap "Anna" → "Maciej Kowalczyk" after logout at step 8). No overreach into chrome.

**(d) Mobile checked:** mobile screenshots included in every check above. Oracle's own mobile step-4 capture shows the same persistent 248px sidebar (not a collapsed hamburger) — the fixed-rectangle mask is layout-correct at 390px width too; candidate mobile mask bbox `(248,56)–(389,843)` confirmed identical placement logic.

**Finding (non-blocking):** `tools/migration-verify/deviations.json` extends DEV-11's step range from the plan's literal "steps 4 and 8" (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md` § Accepted deviations) to steps `[4,5,6,7,8]`, with an embedded self-justifying rationale. Verified this extension is **inert**: the oracle's own steps 4–7 are byte-identical across `desktop.png`, `mobile.png`, and `a11y.json` (confirmed via `cmp`/`diff` — steps 5–7 are out-of-band `POST`s that never navigate the browser away from `/pl/dashboard`). Packet wording ("DEV-11 on login steps where the dashboard is shown") matches the wider range. Flagging for the record since it is a scope change to an accepted deviation not made by the plan owner, even though it hides nothing.

## Commands run

| Command | Cwd | Exit |
| --- | --- | --- |
| `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual` | 0 |
| `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` | `/home/muszkin/work/kivvi-click` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual/tests/e2e` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual/tools/migration-verify` | 0 |
| `curl -sk https://localhost:19101/pl/login` | — | 0 (HTTP 200) |
| `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual` | 0 — "0 regression(s)" |
| `compare -metric AE <oracle>/{1,2,3}/{desktop,mobile}.png <candidate>/{1,2,3}/{desktop,mobile}.png` ×6 | — | AE=0 all six |
| `diff <oracle>/{1..8}/texts.json <candidate>/{1..8}/texts.json` | — | 0 (steps 1–3), diverge at dashboard-body boundary only (steps 4–8, DEV-11) |
| `node` pngjs bbox scripts (mask geometry, steps 4/mobile-4) | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual` | 0 |

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/visual/login/report.md` and `report.json` (tool output)
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/visual/login/steps/1..8/{desktop,mobile}.png`, `*.diff.png`, `a11y.json`, `texts.json`, `url.txt`, `http.jsonl`, `step.json`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/visual/.oracle-mask-{desktop,mobile}-{4..8}.png` (tool's masked-oracle temp comparands, kept as evidence)
