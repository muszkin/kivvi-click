# Wave-0 verifier — dimension `visual` — round 2

**Wave SHA:** `6a53642c9c5ec9c256625854e6670f035e0292be`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-0-visual` (HEAD confirmed == wave SHA, clean, untouched)
**Stack:** `https://localhost:19101` (compose project `kivvi-int`)
**Oracle:** `context/migration-oracle/symfony-to-spring-vue`, manifest sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — verified by `sha256sum` in the checkout's own oracle copy; `diff -rq` against the canonical oracle at the main repo shows zero differences.

## Dimension status: **PASS**

## Journey verdict: `login` — **accepted-deviation** (parity on steps 1–3; DEV-11 accepted deviation on steps 4–8; no regressions)

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `npm ci` | `.../verify-wave-0-visual/tests/e2e` | 0 |
| `npm ci` | `.../verify-wave-0-visual/tools/migration-verify` | 0 |
| `node -e "chromium.launch({channel:'chrome',headless:true})"` (smoke check) | `.../verify-wave-0-visual/tests/e2e` | 0 (launches) |
| `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence dir>` | `.../verify-wave-0-visual` (repo root) | 0 — "0 regression(s)" |
| `compare -metric AE oracle/step-N/{desktop,mobile}.png candidate/step-N/{desktop,mobile}.png diff.png` (ImageMagick, independent, unmasked) | scratchpad | steps 1–3: AE=0 both viewports |
| `convert oracle candidate -compose difference -composite -colorspace gray -threshold 2% ... | -trim info:` (independent bbox of the unmasked diff region, steps 4–8) | scratchpad | bbox = exactly `1440x900+248+56`→`1192x844` (desktop) and `390x844+248+56`→`142x788` (mobile), both viewports, all 5 steps |
| `diff oracle/step-N/{texts,a11y}.json candidate/...` + Python prefix check | scratchpad | steps 1–3 byte-identical; steps 4–8 candidate is a strict line-for-line prefix of oracle (texts: 26/182 lines desktop-agnostic; a11y: 35/82 lines) |

## Per-step table

| Step | Desktop % (tool, masked) | Desktop % (independent, unmasked) | Mobile % (tool, masked) | Mobile % (independent, unmasked) | texts.json | a11y.json | Deviation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 0.000% | 0.000% (AE=0) | 0.000% | 0.000% (AE=0) | identical | identical | — (parity) |
| 2 | 0.000% | 0.000% (AE=0) | 0.000% | 0.000% (AE=0) | identical | identical | — (parity) |
| 3 | 0.000% | 0.000% (AE=0) | 0.000% | 0.000% (AE=0) | identical | identical | — (parity) |
| 4 | 0.000% (post-mask) | 66.894% (AE=866948/1296000) | 0.000% (post-mask) | 33.994% (AE=111896/329160) | valid prefix (26/182) | valid prefix (35/82) | DEV-11 |
| 5 | 0.000% (post-mask) | 66.895% | 0.000% (post-mask) | 33.994% | valid prefix | valid prefix | DEV-11 |
| 6 | 0.000% (post-mask) | 66.895% | 0.000% (post-mask) | 33.994% | valid prefix | valid prefix | DEV-11 |
| 7 | 0.000% (post-mask) | 66.895% | 0.000% (post-mask) | 33.994% | valid prefix | valid prefix | DEV-11 |
| 8 | 0.000% (post-mask) | 66.894% | 0.000% (post-mask) | 33.994% | valid prefix | valid prefix | DEV-11 |

All step URLs (`url.txt`) match oracle exactly for all 8 steps.

## Independent audit findings

- **(a) Steps 1–3, unmasked pixel diff:** computed directly with ImageMagick `compare -metric AE` on the raw oracle vs. candidate PNGs (no tool-applied masks). AE = 0 for both desktop (1440×900) and mobile (390×844) on all three steps — exact pixel match, well under the ≤0.5% threshold with margin to spare.
- **(b) texts.json / a11y.json, every step:** steps 1–3 are byte-identical to the oracle. Steps 4–8: `compare.mjs` compares candidate as a "prefix" of the oracle per the DEV-11 mechanism (candidate DOM has `.main-scroll` removed before `ariaSnapshot()`/`innerText()`). I independently re-derived this prefix check from the raw JSON files (not trusting the tool's verdict string) and confirmed `candidate.length <= oracle.length` and every candidate line equals the oracle line at the same index, for both texts and aria, on all 5 masked steps. No unexplained divergent text found — every remaining shell-chrome line (nav labels, workspace card, avatar/identity) matches exactly.
- **(c) DEV-11 mask region, steps 4–8:** independently derived the unmasked pixel-diff bounding box via ImageMagick difference-compose + threshold + trim (a method that does not depend on the tool's own mask code). The diff region is **exactly** `1192×844 @ (248,56)` on desktop and `142×788 @ (248,56)` on mobile, for every one of steps 4–8 — i.e. the *only* place oracle and candidate ever disagree is the `.main-scroll` rectangle (sidebar 0–248px, topbar 0–56px, both untouched). This independently confirms the mask covers only `.main-scroll` and nothing more/less, and that the sidebar/topbar are pixel-identical to the oracle outside it.
- **Identity change on step 8:** visually inspected oracle vs. candidate `steps/8/{desktop,mobile}.png`. Both show the sidebar footer switching from "Anna Kowalska / anna@aureashop.pl" (post-login) to "Maciej Kowalczyk / maciej@aureashop.pl" (post-logout, default identity) — matches exactly, and is covered by the same zero-unmasked-diff-outside-`.main-scroll` finding above (the footer sits outside the masked rectangle).
- **(d) Mobile screenshots:** verified identically to desktop above — mobile sidebar is NOT collapsed/hidden in this design (visually confirmed: full nav + workspace card + identity footer render at the same 248px width on a 390px viewport), so the tool's fixed mask origin `{x:248,y:56}` is correct for both viewports, not just desktop. This was independently cross-checked, not assumed from the tool's comment.
- **DEV-1 / DEV-2:** correctly not invoked for this journey — report.json shows no DEV-1/DEV-2 references; `deviations.json` itself documents both as not exercised by/superseded on the login journey. No spurious deviation claims found.
- **DEV-12:** not applicable — no 404 steps in the `login` journey.

## Evidence paths

- Tool output: `waves/wave-0/visual/login/report.md`, `report.json`, `waves/wave-0/visual/login/steps/{1..8}/{desktop,mobile}.png,{desktop,mobile}.diff.png,texts.json,a11y.json,http.jsonl,step.json,url.txt`
- Oracle-side mask scratch files (tool-generated, harmless): `waves/wave-0/visual/.oracle-mask-{desktop,mobile}-{4..8}.png`
- Independent audit artifacts (scratchpad, not part of evidence dir): AE diffs and bbox trims in `/tmp/claude-1000/-home-muszkin-work-kivvi-click/10289fb7-a05e-4951-9d8a-97048ccf524b/scratchpad/ae-diffs/`

## Conclusion

No unmasked regression, no missing/renamed accessibility role or name, no unexplained text difference. The only differences found are exactly the ones DEV-11 accepts (dashboard body not yet implemented, wave-0 walking skeleton), confined to precisely the `.main-scroll` rectangle on steps 4–8, with sidebar/topbar — including the post-logout identity swap on step 8 — pixel- and text-identical to the oracle. **Dimension `visual`: PASS**, bound to wave SHA `6a53642c9c5ec9c256625854e6670f035e0292be`.
