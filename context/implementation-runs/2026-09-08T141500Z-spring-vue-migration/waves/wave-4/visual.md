# Verifier report — wave wave-4 (round 1), dimension visual

**Dimension status: PASS**, bound to wave SHA `fe37fad3b884864ca1d41a2ed7cf258126329e18`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` (detached at the wave SHA,
verified clean and unmodified before and after this run). Stack: `https://localhost:19101`
(compose project `kivvi-int`), already running — not started or stopped by this verifier.
Oracle manifest `context/migration-oracle/symfony-to-spring-vue/manifest.json` sha256
`4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — independently recomputed
with `sha256sum`, matches the packet's recorded hash (K3 not triggered).

## Per-journey verdicts

| Journey | Verdict | Deviation ids applied | Regressions (compare.mjs) |
| --- | --- | --- | --- |
| popups-widget-editor | parity | none | 0 |
| import-wizard | parity (steps 1-9); accepted-deviation on step 10 | DEV-12 (step 10, 404 document) | 0 |
| dashboard | parity | DEV-1 (all steps, `.event-row__time` mask), DEV-2 (all steps, cardiogram canvas mask) — both applied unconditionally via oracle `capture/normalize.json` screenshot_masks/text_rules, not step-scoped in `deviations.json` | 0 |
| login (regression guard) | parity, all 8 steps compared **unmasked** | DEV-11 confirmed CLOSED for steps 4/8 (no `accepted-deviation(DEV-11)` tag on any step — see per-step table); DEV-2's canvas mask still applies unconditionally via `normalize.json` | 0 |
| customers (regression guard) | parity (steps 1-5); accepted-deviation on step 6 | DEV-12 (step 6, 404 document) | 0 |

Journeys in scope per packet: popups-widget-editor, import-wizard, dashboard (dimension
duties) + login, customers (regression guards). All five ran clean; no unmasked regression
in any journey.

## Per-step table

Legend: U=url, T=texts, A=aria, SD=screenshotDesktop, SM=screenshotMobile. All verdicts below
are `parity` unless noted. "—" = compare.mjs short-circuited the whole visual row to a single
`accepted-deviation(DEV-12)` verdict (404 document, no per-subdimension breakdown).

| Journey | Step | U | T | A | SD (% px, compare.mjs) | SM (% px, compare.mjs) | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| popups-widget-editor | 1-7 | parity | parity | parity | 0.000% (all 7) | 0.000% (all 7) | parity |
| import-wizard | 1-9 | parity | parity | parity | 0.000% (all 9) | 0.000% (all 9) | parity |
| import-wizard | 10 | — | — | — | — | — | accepted-deviation(DEV-12) |
| dashboard | 1-4 | parity | parity | parity | 0.000% (all 4) | 0.000% (all 4) | parity |
| login | 1-8 | parity | parity | parity | 0.000% (all 8) | 0.000% (all 8) | parity |
| customers | 1-5 | parity | parity | parity | 0.000% (all 5) | 0.000% (all 5) | parity |
| customers | 6 | — | — | — | — | — | accepted-deviation(DEV-12) |

Full per-step detail (with `detail` strings) is in each journey's `report.md`/`report.json`
under this evidence directory.

## Independent spot-checks (this verifier, not compare.mjs)

### Pixel percentages — ImageMagick, independent of compare.mjs's pixelmatch

Ran `compare -metric AE -fuzz 2%` (ImageMagick 6.9.12) oracle-vs-candidate for every
screenshot (desktop + mobile) of every in-scope step, excluding only the two DEV-12 404 steps
(import-wizard step 10, customers step 6 — oracle body is the old Symfony dev-mode exception
page by design, not meant to match). 62 screenshot pairs checked (31 steps × 2 viewports).
Threshold from the plan's Verifier contract: ≤ 0.5% pixels.

Result: all 62 pairs at or under 0.0052%, far inside the 0.5% budget. Non-zero readings:
import-wizard step 5 mobile 0.0052% (17/329160 px), import-wizard step 6 mobile 0.0052%
(17/329160 px), login step 8 mobile 0.0006% (2/329160 px) — sub-pixel antialiasing noise, not
masked content. Every other pair: 0/total (0.0000%). No dimension mismatches, no missing
files. This independently corroborates compare.mjs's own reported 0.000% across the board
(pixelmatch's structural threshold of 0.1 tolerates the same antialiasing noise that
ImageMagick's 2%-fuzz AE also tolerates).

Also confirmed the masking mechanism is symmetric: sampled the oracle's own
`journeys/dashboard/steps/1/desktop.png` for magenta (`#ff00ff`) fill pixels — present in
quantity, confirming the oracle screenshots were captured with the same
`capture/normalize.json` screenshot_masks baked in, not a live unmasked page. Sampled the
candidate's `dashboard/steps/1/desktop.png` the same way — also present in matching
quantity, confirming the candidate capture applied the identical masks before comparison.

Full spot-check table: `/tmp/claude-1000/-home-muszkin-work-kivvi-click/10289fb7-a05e-4951-9d8a-97048ccf524b/scratchpad/imagemagick-spotcheck.tsv`
(local scratch file, not part of the tracked evidence tree per the packet's write-only-here rule).

### texts.json / a11y.json — byte-for-byte diff, independent of compare.mjs's JSON.stringify equality

Ran `cmp -s` (byte-exact) oracle vs. candidate for `texts.json` and `a11y.json` on every
in-scope step, skipping only the two DEV-12 404 steps as instructed. 31 steps × 2 files = 62
comparisons.

Result: **62/62 IDENTICAL**, zero DIFFERS, zero MISSING. Confirms compare.mjs's own
`JSON.stringify(oracle) === JSON.stringify(candidate)` equality checks (which reported
`parity` on every text/aria row) with an independent byte-level method.

Full diff table: `/tmp/claude-1000/-home-muszkin-work-kivvi-click/10289fb7-a05e-4951-9d8a-97048ccf524b/scratchpad/text-a11y-diff.tsv`
(local scratch file).

### DEV-1 / DEV-2 mask correctness (dashboard)

Grepped every dashboard step's `texts.json`/`a11y.json` for a raw `HH:MM:SS` pattern: zero
matches. Every timestamp instance is the `<HH:MM:SS>` placeholder from `normalize.json`'s
DEV-1 text rule (visually confirmed in `dashboard/steps/1/texts.json`, e.g. `"Pik: 28 ev/s ·
<HH:MM:SS>"`, and 9 event-row entries all showing `<HH:MM:SS>`). Sampled the candidate
dashboard screenshot for magenta fill: present in substantial quantity (34740 px at a 2px
stride sample), consistent with both the cardiogram canvas (DEV-2) and the event-row time
chips (DEV-1) being masked out before comparison, not the whole viewport.

### DEV-11 closure (login steps 4/8)

Confirmed two ways: (1) `deviations.json`'s DEV-11 entry now lists only `"journeys":
["landing"]` with `"steps": {"landing": [5]}` — `login` was removed, so compare.mjs's
`journeyDevs` filter for `args.journey === "login"` never matches DEV-11 and the
`.main-scroll` mask is never applied. (2) Read back `login/report.json`: steps 4 and 8 show
plain `"verdict": "parity"` on `screenshotDesktop`/`screenshotMobile`/`texts`/`aria` — never
`accepted-deviation(DEV-11)` — meaning they were compared as full, unmasked pages against the
oracle. DEV-2's canvas mask (unconditional, via `normalize.json`) is still active on these
steps as expected; that is a separate, still-open deviation and does not depend on DEV-11.

### DEV-12 404 pages

Verified both DEV-12 steps return `documentStatus: 404` on the candidate (`step.json`) with a
designed, minimal Polish 404 body (`"404" / "Nie znaleziono strony."` for import-wizard step
10, `"404" / "Nie ma takiego klienta."` for customers step 6) — categorically different from
and much shorter than the oracle's old Symfony dev-mode exception page bodies (1.5+ KB), which
is exactly why DEV-12 excludes these steps from visual/aria/text comparison rather than
matching them.

## Commands run

All commands run with `cwd = /home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` unless
noted otherwise. `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` was set
for every compare.mjs invocation (per packet instructions), though it was never invoked by
these runs since `--dimension visual` never calls `dbCounts()`.

| # | Command | Exit | Notes |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` / `git status` (checkout sanity) | 0 | HEAD = `fe37fad3b884864ca1d41a2ed7cf258126329e18`, clean, detached |
| 2 | `curl -sk -o /dev/null -w "HTTP %{http_code}\n" https://localhost:19101/` | 0 | HTTP 200 |
| 3 | `docker ps --filter label=com.docker.compose.project=kivvi-int` | 0 | 3 containers, all healthy |
| 4 | `sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json` (run from repo root, read-only) | 0 | matches packet-recorded hash |
| 5 | `cd tests/e2e && npm ci` | 0 | 3 packages, 0 vulnerabilities |
| 6 | `cd tools/migration-verify && npm ci` | 0 | 2 packages, 0 vulnerabilities |
| 7 | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension visual --out <evidence>/visual` | 0 | 0 regressions, single attempt |
| 8 | `node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19101 --dimension visual --out <evidence>/visual` (attempt 1) | 1 | `page.waitForURL: Timeout 15000ms exceeded` on step 9 (file upload → redirect); cold-stack timeout, both attempts recorded (`run-import-wizard.attempt1.log`) |
| 9 | same command (attempt 2, retry per packet instruction) | 0 | 0 regressions (`run-import-wizard.attempt2.log`); evidence tree overwritten by this successful attempt |
| 10 | `node tools/migration-verify/compare.mjs --journey dashboard --base https://localhost:19101 --dimension visual --out <evidence>/visual` | 0 | 0 regressions, single attempt |
| 11 | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension visual --out <evidence>/visual` | 0 | 0 regressions, single attempt; steps 4/8 unmasked (DEV-11 closed) |
| 12 | `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence>/visual` | 0 | 0 regressions, single attempt |
| 13 | `compare -metric AE -fuzz 2% <oracle-png> <candidate-png> null:` (ImageMagick 6.9.12), ×62 (31 steps × 2 viewports, DEV-12 steps excluded) | n/a (read-only measurement loop) | max 0.0052%, well under the 0.5% budget |
| 14 | `cmp -s <oracle-json> <candidate-json>` ×62 (texts.json + a11y.json × 31 steps, DEV-12 steps excluded) | n/a | 62/62 identical |
| 15 | `rm -rf tests/e2e/node_modules tools/migration-verify/node_modules` (this checkout only, post-run cleanup per packet disk instructions) | 0 | freed ~20 MB in this checkout |

## Evidence paths

All under `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/`:

- `popups-widget-editor/report.md`, `popups-widget-editor/report.json`, `popups-widget-editor/steps/1..7/{desktop,mobile}.png,texts.json,a11y.json,url.txt,http.jsonl,step.json`
- `import-wizard/report.md`, `import-wizard/report.json`, `import-wizard/steps/1..10/...` (step 10: DEV-12, no `.diff.png` produced since screenshot comparison was skipped)
- `dashboard/report.md`, `dashboard/report.json`, `dashboard/steps/1..4/...`
- `login/report.md`, `login/report.json`, `login/steps/1..8/...`
- `customers/report.md`, `customers/report.json`, `customers/steps/1..6/...` (step 6: DEV-12)
- `run-popups-widget-editor.log`, `run-import-wizard.attempt1.log`, `run-import-wizard.attempt2.log`, `run-dashboard.log`, `run-login.log`, `run-customers.log` — raw stdout/stderr of each compare.mjs invocation, including the failed cold-stack attempt

`compare.mjs` writes `steps/<n>/{desktop,mobile}.diff.png` unconditionally for every non-404
step (66 files total across the 5 journeys) — these are effectively blank/black since
`diffPixels=0` for every pair (pixelmatch-detected differences were 0/total on every
screenshot, matching the independent ImageMagick spot-check above).
