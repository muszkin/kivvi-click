# Verifier: visual — wave-2

Wave SHA: `22d7fcb6380723728a33fc21fda22a92594a2e88` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-2-visual`, HEAD confirmed equal, clean tree).
Stack: `https://localhost:19101` (compose project `kivvi-int`).

**Dimension status: PASS**

## Journeys

| Journey | Verdict |
| --- | --- |
| event-stream | parity (7/7 steps parity; no regressions) |
| customers | parity (steps 1-5 parity; step 6 accepted-deviation(DEV-12)) |

## Commands run

All from cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-2-visual` unless noted.

| Command | Exit |
| --- | --- |
| `cd tests/e2e && npm ci` | 0 |
| `cd tools/migration-verify && npm ci` | 0 |
| `node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension visual --out <evidence>` (1st attempt) | 1 — `TimeoutError` waiting for `#event-stream[data-stream-state=live]`; reproduced independently as a cold-stack flake (SPA mounts and Mercure subscription go `connecting`→`live` in ~1-2s once the container is warm — see probe below); not a code defect |
| same command, 2nd attempt | 0 |
| `node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19101 --dimension visual --out <evidence>` (1st attempt) | 1 — `TimeoutError` on `.page-head a.btn.ghost` click; independently reproduced the same selector against a fresh page load and it resolved immediately (1 match, href `/pl/customers`) — same cold-stack flake class, not a defect |
| same command, 2nd attempt | 0 |
| `VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` set for both compare.mjs invocations (db counts unused by `--dimension visual`) | n/a |
| ImageMagick `compare -metric AE` independent pixel audit, event-stream steps 1/6/7 + customers steps 1-5, desktop+mobile (12 pairs) | all 0 (exit codes not relevant — informational tool) |
| `diff` of texts.json/a11y.json, oracle vs candidate, event-stream all 7 steps + customers steps 1-5 | all identical (0 differences) |

Evidence dir: `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/visual/{event-stream,customers}/` (report.json, report.md, per-step screenshots/diff.png/a11y.json/texts.json/http.jsonl/step.json/url.txt).

## Independent audit — pixel diff (ImageMagick `compare -metric AE`, DEV-1 mask already baked into both PNGs at capture time via Playwright's `mask` option on `.event-row__time`)

| Journey | Step | Desktop AE% | Mobile AE% | Notes |
| --- | --- | --- | --- | --- |
| event-stream | 1 | 0.0000% | 0.0000% | |
| event-stream | 6 | 0.0348% (451/1,296,000 px) | 0.0000% | Diff isolated to a 66×11px region (offset 875,405) — the run's own injected order-id text `ORACLE-<RUN-ID>` inside the event-row detail, e.g. oracle "ORACLE-cap4d" vs candidate "ORACLE-mtt7uisbd". This substring is normalized to `<RUN-ID>` in texts.json/a11y.json but is **not** pixel-masked by normalize.json (only `.event-row__time` and `#cg-main` are) — an inherent artifact of per-invocation run-id randomization, not a regression. Well under the 0.5% (6,480px) threshold. |
| event-stream | 7 | 0.0000% | 0.0000% | |
| customers | 1-5 | 0.0000% each | 0.0000% each | |

compare.mjs's own pixelmatch pass (threshold 0.1) reported consistent low numbers for the same step (0.019%, 242/1,296,000 px) — different algorithm, same conclusion: parity, no deviation id needed (below the 0.5% gate).

## Independent audit — texts/a11y (normalize.json rules)

Confirmed normalize.json's `text_rules` are actually applied (not merely assumed): grepped all event-stream texts.json for raw `HH:MM:SS`-shaped timestamps and raw `ORACLE-`/`oracle-(evt|bad)-` ids — none found unmasked; `<HH:MM:SS>` and `<RUN-ID>` placeholders present throughout. `diff`'d oracle vs candidate texts.json and a11y.json byte-for-byte for event-stream steps 1-7 and customers steps 1-5: **all identical**, no unaccepted-deviation text or role/name differences.

**Live row check (event-stream step 2):** the row injected by the step-2 `/collect` POST (`Zakup` / `412,00 PLN · zamówienie <RUN-ID>` / `HK` / `Hania Kowalska` / `aureashop.pl` / `<HH:MM:SS>`) appears at the top of the log in both oracle and candidate texts.json, byte-identical apart from the normalized run-id/time placeholders — the DOM/text of the live-published row matches the oracle exactly.

## Deviations applied

- **DEV-1** (`.event-row__time` mask + HH:MM:SS text rule): active on every event-stream step; verified the pixel mask is baked identically into both sides and the text rule fires — confirmed above.
- **DEV-12** (customers step 6 = 404 document, skip visual): confirmed both oracle and candidate `step.json` report `documentStatus: 404`; compare.mjs correctly emitted `accepted-deviation(DEV-12)` and skipped screenshot/aria/texts for that step. Titles differ as expected ("404" vs Symfony's dev-mode exception page title) — that divergence is exactly what DEV-12 authorizes.
- **DEV-11**: confirmed not applicable — neither journey's deviation entry lists event-stream/customers.

## Mobile

All mobile screenshots (event-stream 7 steps, customers steps 1-5) at 0.000% diff, both via compare.mjs and the independent ImageMagick pass.

No missing roles/names, no unaccepted text differences, no screenshot diff above 0.5% found anywhere in either journey.
