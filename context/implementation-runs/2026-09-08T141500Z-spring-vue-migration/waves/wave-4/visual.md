# Wave-4 verifier — dimension: visual (round 3)

**Dimension status: PASS**, bound to wave SHA `60445ebac139bcf1179b397c997600073a356e95` (checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual`, clean, `git status --short` empty).

Scope per packet (round 3 + round-3 addendum): primary journeys popups-widget-editor,
import-wizard, dashboard; regression guards login (unmasked steps 4/8, DEV-11 closed this
wave), customers, and — new in round 3 — shell-navigation (all 21 steps, DEV-12 step 21),
added because a pre-existing `<html data-sidebar>` desync (fixed by shell-preferences
repair-2) was invisible to contract-only guards. Round-1/round-2 evidence under
`waves/wave-4/round-1/` and `round-2/` was not read, per the round-3 note.

## Per-journey verdicts

| Journey | Steps | Verdict | Deviation ids exercised | Regressions (compare.mjs) |
| --- | --- | --- | --- | --- |
| popups-widget-editor | 7 | parity | none active for this journey's visual steps (DEV-7 is contract-only) | 0 |
| import-wizard | 10 | parity (9) + accepted-deviation (1) | DEV-12 step 10 (404 document, skip-on-404) | 0 |
| dashboard | 4 | parity | DEV-1 (`.event-row__time` mask), DEV-2 (`#cg-main`/`.cardiogram-canvas` mask) — both applied identically to oracle and candidate, verified pixel-identical | 0 |
| login (regression guard) | 8 | parity (all 8, incl. 4 and 8) | DEV-11 closed at wave-4 — steps 4/8 compared with mask *removed*, per packet instruction; confirmed exact pixel/text/aria parity, no residual masking needed. DEV-2's canvas mask still applies (dashboard body now real) | 0 |
| customers (regression guard) | 6 | parity (5) + accepted-deviation (1) | DEV-12 step 6 (404 document) | 0 |
| shell-navigation (regression guard, round-3 addendum) | 21 | parity (20) + accepted-deviation (1) | DEV-12 step 21 (404 document). Steps 4–5 (sidebar collapse / theme toggle) at 0.001%/0.008% pixel diff, well under the 0.5% threshold — no desync regression found | 0 |

All journeys: `regressions: 0` in `report.json`; no step reported `verdict: "regression"`.

## Independent spot-checks (beyond compare.mjs's own report)

- **Pixel percentages (ImageMagick `compare -metric AE -fuzz 10%`)**: recomputed independently
  for every desktop screenshot pair (oracle vs candidate) across all 53 non-404 steps (7+10+4+8+6+21
  minus the three DEV-12 steps) plus the three DEV-12 steps themselves for contrast.
  - All 50 non-DEV-12 steps: **0.0000% AE** (bit-identical images) except shell-navigation
    steps 4 (0.0032%) and 5 (0.0162%) — sub-threshold antialiasing noise, consistent in
    magnitude with compare.mjs's own pixelmatch numbers for the same steps (0.001% / 0.008%,
    9/1,296,000 and 107/1,296,000 pixels), far under the 0.5% (6,480 px) gate.
  - The 3 DEV-12 steps (import-wizard step 10, customers step 6, shell-navigation step 21):
    35.71%, 30.46%, 35.70% AE respectively — large, as expected, since the old stack's Symfony
    dev-mode 404 exception page and the new SPA's minimal 404 body are intentionally not
    compared (DEV-12 skips screenshot/aria/texts, contract still enforces status 404). This
    confirms the skip was the right call, not evidence of a hidden pass.
- **texts.json / a11y.json byte-for-byte diff (`cmp`)**: 106 file pairs (53 non-404 steps ×
  2 files) across all 6 journeys — **0 diffs**. Oracle files already carry the DEV-1 clock-string
  normalization (`<HH:MM:SS>`) baked in at capture time, so raw `cmp` is a valid independent
  check equivalent to compare.mjs's `JSON.stringify` equality.
- **DEV-12 step.json confirmation**: `documentStatus: 404` verified directly in the candidate's
  own `step.json` for all three DEV-12 steps (import-wizard/10, customers/6, shell-navigation/21).
- **Step-count sanity**: shell-navigation produced exactly 21 step directories, import-wizard 10,
  login 8 — matching the packet's explicit counts.

## Commands run

Environment: `export VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"`, cwd
`/home/muszkin/work/kivvi-click-wt/verify-wave-4-visual` for all `node` invocations below.

| # | Command | cwd | Exit | Notes |
| --- | --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` / `git status --short` | checkout root | 0 | confirms SHA `60445eba...` clean |
| 2 | `curl -sk https://localhost:19101/` | — | 0 (200) | stack reachable before first run |
| 3 | `docker compose -p kivvi-int ps` | checkout root | 0 | api/database/mercure healthy |
| 4 | `npm ci` | `tests/e2e/` | 0 | 3 packages (playwright reused from `~/.cache/ms-playwright`) |
| 5 | `npm ci` | `tools/migration-verify/` | 0 | pixelmatch + pngjs |
| 6 | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension visual --out <evidence-dir>` | checkout root | 0 | 0 regressions, 7 steps |
| 7 | same, `--journey import-wizard` | checkout root | 0 | 0 regressions, 10 steps (step 10 = DEV-12) |
| 8 | same, `--journey dashboard` | checkout root | 0 | 0 regressions, 4 steps |
| 9 | same, `--journey login` (attempt 1) | checkout root | **1** | `page.waitForURL: Timeout 15000ms exceeded` → `chrome-error://chromewebdata/` — cold-stack timeout, recorded at `run-login.log` |
| 10 | same, `--journey login` (attempt 2, retry) | checkout root | 0 | passed cleanly, 0 regressions, 8 steps — `run-login-retry1.log` |
| 11 | same, `--journey customers` | checkout root | 0 | 0 regressions, 6 steps (step 6 = DEV-12) |
| 12 | same, `--journey shell-navigation` | checkout root | 0 | 0 regressions, 21 steps (step 21 = DEV-12) |
| 13 | `compare -metric AE -fuzz 10% <oracle>/desktop.png <candidate>/desktop.png /dev/null` × 53 (non-404 steps) + 3 (DEV-12 steps) | ImageMagick, ad hoc | n/a (measurement only) | independent pixel spot-check, see above |
| 14 | `cmp -s <oracle>/{texts,a11y}.json <candidate>/{texts,a11y}.json` × 106 | ad hoc | n/a (measurement only) | 0 diffs |
| 15 | `docker compose -p kivvi-int ps` (re-check after login timeout) | checkout root | 0 | stack still healthy, retry not a stack-down condition |

Full stdout/stderr of runs 6–12 saved at `<evidence-dir>/run-<journey>.log`
(and `run-login-retry1.log` for the retry).

## Evidence paths

- Evidence root: `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/visual/`
- Per journey: `visual/<journey>/report.json`, `visual/<journey>/report.md`,
  `visual/<journey>/steps/<n>/{desktop.png,desktop.diff.png,mobile.png,mobile.diff.png,texts.json,a11y.json,url.txt,http.jsonl,step.json}`
  (DEV-12 steps still capture screenshots/text/aria for the record but are not diffed).
- Run logs: `visual/run-popups-widget-editor.log`, `visual/run-import-wizard.log`,
  `visual/run-dashboard.log`, `visual/run-login.log` (attempt 1, timeout),
  `visual/run-login-retry1.log` (attempt 2, pass), `visual/run-customers.log`,
  `visual/run-shell-navigation.log`.
- This summary: `visual.md` (this file).

## Disk cleanup

Per packet instruction (disk tight, 5.0 GB free on `/` before this run), removed after
finishing all runs and spot-checks:

```
rm -rf /home/muszkin/work/kivvi-click-wt/verify-wave-4-visual/tests/e2e/node_modules
rm -rf /home/muszkin/work/kivvi-click-wt/verify-wave-4-visual/tools/migration-verify/node_modules
```

(Playwright browser binaries themselves live in the shared `~/.cache/ms-playwright`, not in
`node_modules`, and were not touched — they're not this checkout's to delete.)

## Conclusion

Every in-scope journey and every regression guard, including the round-3-addendum
shell-navigation guard (all 21 steps), reaches visual parity at the wave SHA: 0 unmasked
pixel/a11y/text differences anywhere except the three DEV-12 404 steps, which the plan's
Accepted deviations table explicitly excludes from visual comparison and which this run's own
independent AE measurements confirm are the right steps to skip (large, expected diffs from
comparing incomparable error pages). No new masks, no threshold breaches, no regressions.

**Dimension status: PASS.**
