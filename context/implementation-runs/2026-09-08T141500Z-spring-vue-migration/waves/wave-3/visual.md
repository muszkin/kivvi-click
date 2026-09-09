# Wave-3 verifier — dimension: visual (round 4)

**Dimension status: PASS**, bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

Independent read-only verification. No worker reports, review files, or earlier-round evidence
(`waves/wave-3/round-1..3/`) were read. Verified afresh against the running stack at
`https://localhost:19101` (compose project `kivvi-int`), built from a checkout confirmed at the
same SHA (`git -C /home/muszkin/work/kivvi-click-wt/verify-wave-3 rev-parse HEAD` →
`11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`, clean). Oracle manifest hash confirmed by recomputing
`sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` →
`4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet). Stack was
already running; not started or stopped by this verifier.

## Per-journey verdicts

| Journey | Role | Verdict | Deviation ids | Regressions (compare.mjs) |
| --- | --- | --- | --- | --- |
| automations | wave-3 primary | **parity** | — | 0 |
| settings | wave-3 primary | **parity** (step 10 accepted-deviation) | DEV-12 (step 10, 404 document) | 0 |
| campaigns-email-editor | wave-3 primary | **parity** | — | 0 |
| login | regression guard | **parity** (steps 4–8 accepted-deviation) | DEV-11 (steps 4–8, dashboard body not yet implemented) | 0 |
| customers | regression guard | **parity** (step 6 accepted-deviation) | DEV-12 (step 6, 404 document) | 0 |

No unmasked screenshot/a11y/text differences and no regressions in any of the five journeys.
DEV-4 and DEV-7 (also "in scope" per the packet) are `contract`-dimension deviations per
`tools/migration-verify/deviations.json` — they do not manifest in the visual dimension and none
of the visual reports needed to invoke them, confirmed by inspection.

## Per-step table

Legend: P = parity, AD = accepted-deviation, R = regression. `%px` = compare.mjs's own
pixelmatch ratio (desktop/mobile); IM-AE = this verifier's independent ImageMagick `compare
-metric AE -fuzz 10%` pixel count out of total (desktop/mobile); texts/a11y = byte-for-byte
`cmp` of `texts.json`/`a11y.json` against the oracle (IDENTICAL or DIFFERS, independent of
compare.mjs's own prefix/exact logic).

### automations (7 steps, all desktop 1440×900=1,296,000px / mobile 390×844=329,160px)

| Step | Verdict | %px (compare.mjs) | IM-AE desktop/mobile | texts.json | a11y.json |
| --- | --- | --- | --- | --- | --- |
| 1 | P | 0.001% / 0.000% | 41 / 0 | IDENTICAL | IDENTICAL |
| 2 | P | 0.000% / 0.000% | 39 / 0 | IDENTICAL | IDENTICAL |
| 3 | P | 0.001% / 0.000% | 41 / 0 | IDENTICAL | IDENTICAL |
| 4 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 5 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 6 | P | 0.000% / 0.000% | 5 / 0 | IDENTICAL | IDENTICAL |
| 7 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |

### settings (10 steps)

| Step | Verdict | %px (compare.mjs) | IM-AE desktop/mobile | texts.json | a11y.json |
| --- | --- | --- | --- | --- | --- |
| 1 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 2 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 3 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 4 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 5 | P | 0.000% / 0.000% | 5 / 0 | IDENTICAL | IDENTICAL |
| 6 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 7 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 8 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 9 | P | 0.000% / 0.000% | 0 / 5 | IDENTICAL | IDENTICAL |
| 10 | AD (DEV-12) | skipped (404 document) | skipped — oracle `step.json.documentStatus`=404 confirmed | skipped | skipped |

### campaigns-email-editor (8 steps)

| Step | Verdict | %px (compare.mjs) | IM-AE desktop/mobile | texts.json | a11y.json |
| --- | --- | --- | --- | --- | --- |
| 1 | P | 0.008% / 0.000% | 210 / 0 | IDENTICAL | IDENTICAL |
| 2 | P | 0.009% / 0.000% | 220 / 0 | IDENTICAL | IDENTICAL |
| 3 | P | 0.008% / 0.000% | 210 / 5 | IDENTICAL | IDENTICAL |
| 4 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 5 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 6 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 7 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 8 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |

Ran once, PASS on first attempt — no cold-stack timeout, no retry needed.

### login (8 steps — regression guard only)

| Step | Verdict | %px (compare.mjs) | IM-AE desktop/mobile (unmasked) | texts.json | a11y.json |
| --- | --- | --- | --- | --- | --- |
| 1 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 2 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 3 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 4 | AD (DEV-11) | 0.000% / 0.000% (post-mask) | 866948/1296000 / 111896/329160 (pre-mask, expected — `.main-scroll` unmasked) | DIFFERS (prefix-verified, see note) | DIFFERS (prefix-verified) |
| 5 | AD (DEV-11) | 0.000% / 0.000% (post-mask) | 866948/1296000 / 111896/329160 | DIFFERS (prefix-verified) | DIFFERS (prefix-verified) |
| 6 | AD (DEV-11) | 0.000% / 0.000% (post-mask) | 866948/1296000 / 111896/329160 | DIFFERS (prefix-verified) | DIFFERS (prefix-verified) |
| 7 | AD (DEV-11) | 0.000% / 0.000% (post-mask) | 866948/1296000 / 111896/329160 | DIFFERS (prefix-verified) | DIFFERS (prefix-verified) |
| 8 | AD (DEV-11) | 0.000% / 0.000% (post-mask) | 866948/1296000 / 111896/329160 | DIFFERS (prefix-verified) | DIFFERS (prefix-verified) |

Note on steps 4–8: independently re-implemented compare.mjs's DEV-11 prefix check in a standalone
script against `texts.json` — candidate is 26 lines, oracle is 182 lines, and the candidate's 26
lines are byte-identical to the oracle's first 26 lines for all five steps (`prefixOk: true` in
every case). This is the expected walking-skeleton shape (dashboard body not implemented until
wave-4) and matches the accepted-deviation description exactly, not an unexplained divergence.

### customers (6 steps — regression guard only)

| Step | Verdict | %px (compare.mjs) | IM-AE desktop/mobile | texts.json | a11y.json |
| --- | --- | --- | --- | --- | --- |
| 1 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 2 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 3 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 4 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 5 | P | 0.000% / 0.000% | 0 / 0 | IDENTICAL | IDENTICAL |
| 6 | AD (DEV-12) | skipped (404 document) | skipped — oracle `step.json.documentStatus`=404 confirmed | skipped | skipped |

All non-deviation IM-AE counts (0–220 px) are ≪ the 0.5% threshold (6,480 px on desktop /
1,646 px on mobile) — consistent in order of magnitude with compare.mjs's own pixelmatch ratios
(different algorithm/fuzz, same near-zero conclusion).

## Commands run

All commands run inside the detached checkout
`/home/muszkin/work/kivvi-click-wt/verify-wave-3-visual` (detached at `11d3cc49f...`), except
where noted.

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `.../verify-wave-3-visual/tests/e2e` | 0 |
| 2 | `npm ci` | `.../verify-wave-3-visual/tools/migration-verify` | 0 |
| 3 | `curl -sk -o /dev/null -w "HTTP %{http_code}\n" https://localhost:19101/` | (shell default) | 0 (HTTP 200) |
| 4 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `.../verify-wave-3-visual` | 0 |
| 5 | `node tools/migration-verify/compare.mjs --journey settings --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `.../verify-wave-3-visual` | 0 |
| 6 | `node tools/migration-verify/compare.mjs --journey campaigns-email-editor --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `.../verify-wave-3-visual` | 0 (first attempt, no retry needed) |
| 7 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `.../verify-wave-3-visual` | 0 |
| 8 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence>/visual` | `.../verify-wave-3-visual` | 0 |
| 9 | `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` | `.../verify-wave-3-visual` | 0 (`4945a8de...` matches packet) |
| 10 | `git rev-parse HEAD` | `.../verify-wave-3-visual` | 0 (`11d3cc49f...`) |
| 11 | `git -C /home/muszkin/work/kivvi-click-wt/verify-wave-3 rev-parse HEAD; git status --short` | (n/a — read-only inspection of the orchestrator's stack-build checkout) | 0 (`11d3cc49f...`, clean) |
| 12 | `docker inspect kivvi-int-api-1 --format '{{json .Config.Labels}}'` | (n/a) | 0 (confirms build working_dir = `verify-wave-3`) |
| 13 | Independent ImageMagick spot-check: `compare -metric AE -fuzz 10% <oracle png> <candidate png> null:` for every step/viewport of all 5 journeys (skipping DEV-12 steps) | scratchpad script | 0 (all invocations; see per-step table) |
| 14 | Independent byte-for-byte diff: `cmp -s <candidate texts.json/a11y.json> <oracle texts.json/a11y.json>` for every step of all 5 journeys (skipping DEV-12 steps) | scratchpad script | 0 (all invocations; see per-step table) |
| 15 | Independent DEV-11 prefix re-check (Node) on login steps 4–8 `texts.json` | scratchpad script | 0 (`prefixOk: true` for steps 4–8) |

`<evidence>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3`

## Evidence paths

- `visual/automations/` — `report.json`, `report.md`, `steps/1..7/{desktop.png,mobile.png,desktop.diff.png,mobile.diff.png,texts.json,a11y.json,url.txt,http.jsonl,step.json}`
- `visual/settings/` — same shape, `steps/1..10` (step 10 has no diff/screenshot artifacts — DEV-12 skip)
- `visual/campaigns-email-editor/` — same shape, `steps/1..8`
- `visual/login/` — same shape, `steps/1..8` (steps 4–8 diff images show the masked-then-zero comparison compare.mjs performed; this verifier's independent AE numbers above were computed on the **unmasked** originals to confirm the pre-mask divergence is real and expected)
- `visual/customers/` — same shape, `steps/1..6` (step 6 has no diff/screenshot artifacts — DEV-12 skip)

All under `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/`.

## Conclusion

**Dimension status: PASS.** Zero regressions across all five journeys (three wave-3 primaries —
automations, settings, campaigns-email-editor — plus two regression guards — login, customers).
Every accepted-deviation attribution (DEV-11 on login steps 4–8, DEV-12 on settings step 10 and
customers step 6) was independently re-derived from the raw artifacts and holds. No round-4
repair (session-lock serialization filter, preference-POST keepalive, reload marker) introduced
any visual regression in this wave's journeys or in the regression-guard journeys checked here.
