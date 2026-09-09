# Wave-4 verifier — dimension: visual (round 2)

**Dimension status: PASS**, bound to wave SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`.

Independent read-only verification. No worker reports, review files or round-1 evidence were
read. Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` (detached at the wave
SHA, confirmed clean before and after this run). Stack: `https://localhost:19101` (compose
project `kivvi-int`), already running — not started or stopped by this verifier. Oracle manifest
hash confirmed: `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` →
`4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet).

## Per-journey verdicts

| Journey | Role | Verdict | Deviation ids observed | Regressions |
| --- | --- | --- | --- | --- |
| popups-widget-editor | primary (in scope) | **parity** | none active (DEV-7 is contract-only, not exercised here) | 0/35 |
| import-wizard | primary (in scope) | **parity + accepted-deviation** | DEV-12 (step 10, 404 document, skipped) | 0/45 |
| dashboard | primary (in scope) | **parity** | DEV-1, DEV-2 (masks baked into both oracle and candidate screenshots via `normalize.json`; confirmed live, see below) | 0/20 |
| login | regression guard | **parity — DEV-11 closed** | none (steps 4/8 compare as full, unmasked parity; DEV-2's canvas mask still fires as `normalize.json`-level masking, not as a distinct deviation verdict) | 0/40 |
| customers | regression guard | **parity + accepted-deviation** | DEV-12 (step 6, 404 document, skipped) | 0/25 |

`compare.mjs`'s own regression counters (0 for all five journeys) and this verifier's independent
re-derivation (below) agree.

### DEV-11 closure check (login steps 4 and 8)

Packet states DEV-11 is CLOSED for login steps 4/8 by this wave. Confirmed two ways:
- `tools/migration-verify/deviations.json` on the wave SHA: DEV-11's `journeys`/`steps` list only
  `landing` step 5 — `login` was removed from the entry (with an explanatory note in the file).
- `compare.mjs`'s login report shows steps 4 and 8 as plain `parity` for `url`, `texts`, `aria`,
  `screenshotDesktop` (0.000%) and `screenshotMobile` (0.000%) — not
  `accepted-deviation(DEV-11)`. A `parity` verdict on a masked-prefix comparison is impossible in
  the tool's logic (`compareTexts`/`compareAria` only ever return `parity` or
  `accepted-deviation(DEV-11)` when `masked` is true), so this is conclusive: the unmasked
  full-page compare ran and matched exactly.

## Per-step table

Legend: `AE` = ImageMagick `compare -metric AE -fuzz 10%` absolute error pixel count (independent
re-check, separate algorithm from `compare.mjs`'s `pixelmatch`); `pixelmatch %` = the tool's own
reported ratio; `texts`/`a11y` = byte-for-byte `diff` of `texts.json`/`a11y.json` against the
oracle (independent check, not merely re-reading the tool's verdict).

### popups-widget-editor (7 steps, all in scope)

| Step | url | texts (byte diff) | a11y (byte diff) | screenshotDesktop pixelmatch % | screenshotMobile pixelmatch % | AE desktop | AE mobile | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 2 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 3 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 4 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 5 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 6 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 7 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |

### import-wizard (10 steps; step 10 = DEV-12 404 document)

| Step | url | texts (byte diff) | a11y (byte diff) | screenshotDesktop pixelmatch % | screenshotMobile pixelmatch % | AE desktop | AE mobile | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 2 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 3 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 4 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 5 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 5/329160 (0.0015%, ImageMagick-only; see note) | parity |
| 6 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 5/329160 (0.0015%, ImageMagick-only; see note) | parity |
| 7 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 8 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 9 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 10 | — | skipped (404) | skipped (404) | skipped (404) | skipped (404) | — | — | accepted-deviation(DEV-12) |

Note on step 5/6 mobile: ImageMagick's fuzz-based `AE` (0.0015%) disagrees in kind, not in
outcome, with `pixelmatch`'s 0.000%. Pixel-level inspection (Python/Pillow, exact RGB diff)
found 30 pixels in a small cluster (x≈17–37, y≈799–827) with 1–20-unit RGB deltas — sub-pixel
anti-aliasing jitter on a glyph/icon edge, not a structural or content difference. Both figures
are far below the 0.5% threshold; verdict unaffected.

### dashboard (4 steps, all in scope)

| Step | url | texts (byte diff) | a11y (byte diff) | screenshotDesktop pixelmatch % | screenshotMobile pixelmatch % | AE desktop | AE mobile | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity (DEV-1/DEV-2 masks active — see below) |
| 2 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 3 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 4 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |

DEV-1/DEV-2 confirmed live (not coincidental parity): sampled every 3rd pixel of
`dashboard/steps/1/desktop.png` for the mask paint colour `#ff00ff` and found 15,462 hits,
confirming `normalize.json`'s `#cg-main, .cardiogram-canvas` and `.event-row__time` masks are
painted into both the oracle and candidate screenshots before `pixelmatch` runs, per
`compare.mjs`'s `captureScreenshot`/Playwright `mask` option.

### login (8 steps — regression guard; steps 4/8 are the DEV-11-closure check)

| Step | url | texts (byte diff) | a11y (byte diff) | screenshotDesktop pixelmatch % | screenshotMobile pixelmatch % | AE desktop | AE mobile | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 2 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 3 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 4 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | **parity, unmasked (DEV-11 closed)** |
| 5 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 6 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 7 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 8 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | **parity, unmasked (DEV-11 closed)** |

### customers (6 steps — regression guard; step 6 = DEV-12 404 document)

| Step | url | texts (byte diff) | a11y (byte diff) | screenshotDesktop pixelmatch % | screenshotMobile pixelmatch % | AE desktop | AE mobile | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 2 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 3 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 4 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 5 | parity | identical | identical | 0.000% | 0.000% | 0/1296000 | 0/329160 | parity |
| 6 | — | skipped (404) | skipped (404) | skipped (404) | skipped (404) | — | — | accepted-deviation(DEV-12) |

Total steps compared: 35 desktop+mobile screenshot pairs, 33 texts.json pairs, 33 a11y.json
pairs (35 steps minus 2 DEV-12 404 steps); 0 regressions found by `compare.mjs`, 0 regressions
found by this verifier's independent re-check.

## Commands run

All `compare.mjs` invocations ran on the first attempt with no cold-stack timeout — no retries
were needed.

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` | `/home/muszkin/work/kivvi-click` | 0 (hash matched packet) |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual/tests/e2e` | 0 |
| 3 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual/tools/migration-verify` | 0 |
| 4 | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension visual --out <evidence-dir>` (env: `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |
| 5 | `node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19101 --dimension visual --out <evidence-dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |
| 6 | `node tools/migration-verify/compare.mjs --journey dashboard --base https://localhost:19101 --dimension visual --out <evidence-dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |
| 7 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence-dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |
| 8 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence-dir>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |
| 9 | Independent spot-check script: `identify`/`compare -metric AE -fuzz 10%` (ImageMagick) over every step's desktop/mobile PNG pair + `diff -q` over every step's `texts.json`/`a11y.json` pair, skipping only the two DEV-12 steps | ad hoc (evidence-dir + oracle-dir inputs, no repo writes) | 0 (script completed; no regressions found) |
| 10 | `rm -rf tests/e2e/node_modules tools/migration-verify/node_modules` (post-verification cleanup, disk hygiene) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` | 0 |

`<evidence-dir>` = `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual`

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/popups-widget-editor/{report.md,report.json,steps/1..7/}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/import-wizard/{report.md,report.json,steps/1..10/}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/dashboard/{report.md,report.json,steps/1..4/}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/login/{report.md,report.json,steps/1..8/}`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/customers/{report.md,report.json,steps/1..6/}`
- This summary: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual.md`

Each `steps/<n>/` directory holds `desktop.png`, `mobile.png`, `desktop.diff.png`,
`mobile.diff.png` (pixelmatch diff images, magenta = 0 everywhere observed), `a11y.json`,
`texts.json`, `step.json`, `url.txt`, `http.jsonl` — written only under the evidence dir passed
via `--out`; nothing was written to the checkout beyond the `npm ci`-installed `node_modules`,
which were deleted at the end per the packet's disk-hygiene instruction. `git status` in the
checkout was clean before this run and remains clean (detached HEAD, no tracked-file edits) after
it.

## Conclusion

Dimension **visual: PASS** for wave SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`. All three
in-scope journeys (popups-widget-editor, import-wizard, dashboard) and both regression guards
(login, customers) show 0 regressions under `compare.mjs --dimension visual`, corroborated by an
independent ImageMagick pixel re-check and byte-for-byte `texts.json`/`a11y.json` diffs on every
non-DEV-12 step. DEV-11 is confirmed closed for login steps 4/8 (unmasked parity, not a masked
verdict). DEV-12 correctly skips visual/a11y/text comparison on the two 404 steps in scope
(import-wizard step 10, customers step 6). DEV-1/DEV-2 masking confirmed live on the dashboard
journey via direct pixel inspection.
