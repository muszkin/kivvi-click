# Verifier report — wave wave-4 (round 1), dimension contract

**Dimension status: PASS**, bound to wave SHA `fe37fad3b884864ca1d41a2ed7cf258126329e18`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract` (detached at the wave SHA;
verified clean and unmodified, still detached at `fe37fad3b884864ca1d41a2ed7cf258126329e18`,
before and after this run — `git status --short` empty for tracked files). Stack:
`https://localhost:19101` (compose project `kivvi-int`), already running (containers `api`,
`database`, `mercure`, all healthy) — not started or stopped by this verifier; no other
verifier was using it during this run (unit/integration/architecture/visual had already
finished; e2e had not started). Oracle manifest
`context/migration-oracle/symfony-to-spring-vue/manifest.json` sha256
`4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — independently recomputed
with `sha256sum`, matches the packet's recorded hash (K3 not triggered).

## Per-journey verdicts

| Journey | Verdict | Deviation ids applied | Regressions (compare.mjs) |
| --- | --- | --- | --- |
| popups-widget-editor | parity | DEV-4 (all steps, SPA-shell document compare), DEV-7 (all steps, no `POST …/blocks` call in either oracle or candidate recording) | 0 |
| import-wizard | parity | DEV-4 (all steps); DEV-5 (db `var/import files` +1, `sessions`→`spring_session` +1); DEV-12 applies to visual only — step 10's 404 is verified exactly on contract (status 404, method/path match, no `Location`) | 0 |
| dashboard | parity | DEV-4 (all steps, incl. step 3's browser-back reproducing a full document reload to `/pl/dashboard`, matching the oracle exactly) | 0 |
| login (regression guard) | parity, all steps compared **unmasked** on contract | DEV-4, DEV-5, DEV-9 (session count tolerance ±1: oracle delta 0, candidate delta 1 — journey's own sign-in); full-parity deep body checks matched exactly on steps 4 (302→`/pl/dashboard`), 5/6 (`data-login-error`/`data-last-username` extracted values byte-identical after canonicalization), 7 (`POST /pl/logout` → 302 `/pl/login`), 8 (post-logout `GET /pl/dashboard` → 200, matching oracle) | 0 |
| event-stream (regression guard) | parity | DEV-3 (steps 2-3, not diffed — Mercure payload excluded from `http.jsonl` by `normalize.json`'s own ignore path, unaffected by dimension result); DEV-4, DEV-5 (db `cache_items`→`event_dedup` delta 1/1); full-parity `/collect` response bodies matched exactly for 202 accepted, 200 duplicate, and both 400 Polish error messages | 0 |
| shell-navigation (regression guard) | parity, all 21 steps | DEV-4, DEV-9; db `sessions`→`spring_session` delta 1/1; step 21 (`GET /de/dashboard` → 404) matches oracle on method/path/status | 0 |

All six journeys: **0 regressions**, confirmed both by `compare.mjs`'s own exit code/report and
by this verifier's independent line-by-line diff of every `http.jsonl` and `db.json` pair
(oracle vs. candidate) for every in-scope step — see "Independent verification" below.

## Commands run

All commands: `cwd=/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract`,
`VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml"` exported for the `dbCounts()`
Postgres/filesystem counters.

| Journey | Command | Exit code | Wall time | Retry needed |
| --- | --- | --- | --- | --- |
| popups-widget-editor | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 4.7s | no |
| import-wizard | `node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 5.7s | no |
| dashboard | `node tools/migration-verify/compare.mjs --journey dashboard --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 2.9s | no |
| login | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 4.5s | no |
| event-stream | `node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 4.1s | no |
| shell-navigation | `node tools/migration-verify/compare.mjs --journey shell-navigation --base https://localhost:19101 --dimension contract --out <evidence-dir>` | 0 | 11.0s | no |

Every run passed clean on the first attempt (`compare.mjs` neither threw nor timed out on this
stack), so no retry/second run was required for any journey. Setup commands (also
`cwd=/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract`, this checkout only):
`(cd tests/e2e && npm ci)` — exit 0, 3 packages (reuses system `google-chrome` via Playwright's
`channel: "chrome"`, already cached at `~/.cache/ms-playwright`, no browser download needed);
`(cd tools/migration-verify && npm ci)` — exit 0, 2 packages (`pixelmatch`, `pngjs`).

## Independent verification (this verifier, not just trusting compare.mjs's exit code)

- **No 503s / no unmasked 5xx anywhere**: `grep -rhoE '"status":[0-9]+' contract/*/steps/*/http.jsonl`
  across all six journeys' candidate recordings → status distribution `200×129, 202×1, 302×3,
  400×2, 404×2`. Zero occurrences of any `5xx`.
- **`tools/migration-verify/compare.mjs` diff vs. wave-3 SHA `11d3cc4`**: `git diff 11d3cc4
  fe37fad3 -- tools/migration-verify/compare.mjs` shows exactly one change — `dbCounts()` gained
  a `counts["var/import files"] = importFileCount()` line and the new `importFileCount()`
  helper (a `find var/import -type f | wc -l` inside the `api` container). No existing
  comparison, threshold, filter, or dimension classification was touched or loosened; the diff
  is purely additive (a new counted key). `tools/migration-verify/deviations.json` also diffed
  against wave-3: DEV-5's `mapping` gained the `"var/import files": ["var/import files"]` entry
  (matching the new counter — tolerance stays 0, not widened), DEV-11 dropped `login` from
  `journeys`/`steps` (the packet's stated wave-4 closure), and DEV-12 gained `import-wizard`
  step 10. All three changes match the packet's own "Wave-4 note" description exactly
  (`deviations.json changed only for DEV-5, DEV-12 and DEV-11 closure`) — nothing unexpected.
- **Line-by-line oracle-vs-candidate diff, read manually (not just compare.mjs's verdict)** for
  every step with `http.jsonl` content in every in-scope journey, plus every `db.json`:
  - popups-widget-editor: all 7 steps are single `document` GETs, method/path/status identical
    both sides; candidate additionally makes 2-3 `/api/v1/**` XHRs per step (correctly excluded
    from the DEV-4 `core` count as `apiBaseline`); no `POST …/blocks` call in either recording
    (DEV-7 confirmed as a non-event on this journey, not a masked failure).
  - import-wizard step 9: candidate `http.jsonl` has the same 3 non-API entries as the oracle,
    in the same order — `POST /import/upload` → 302 `Location: /pl/import/2`, the SPA's own
    fetch-followed `GET /pl/import/2` → 200 (`kind: xhr`), and the full-navigation
    `GET /pl/import/2` → 200 (`kind: document`) — plus 2 `/api/v1/**` baseline calls correctly
    excluded. `db.json`: `before.var/import files: 3` → `after: 4`, delta `+1`, matching the
    oracle's own delta `+1` for the same key exactly (not just within tolerance).
    Step 10: `GET /pl/import/5` → 404 both sides, single `document` entry, no `Location` header
    on either side (not a 3xx, so DEV-4's location check is correctly skipped).
  - dashboard: step 1 `GET /pl/dashboard`, step 2 click lands on `GET /pl/customers/c_1000`
    (matches oracle's `c_1000` exactly — same seeded id, not just "some" customer), step 3
    (browser back) reproduces a genuine full-document reload back to `GET /pl/dashboard`
    (confirms the candidate does not silently use the History API/bfcache to skip the request —
    it round-trips the same as the oracle), step 4 click lands on `GET /pl/automations/a3`
    (again the exact same seeded id as the oracle). `db.json` delta all-zero both sides.
  - login: steps 2-3 (form fills) correctly produce no HTTP entries in either recording (skipped
    by `compare.mjs` itself via `oracleStep.http.length > 0`, and independently confirmed by
    reading both `http.jsonl` files — both absent/empty). Step 4: `POST /pl/login` → 302
    `Location: /pl/dashboard`, then `GET /pl/dashboard` → 200, plus (only on the candidate) two
    `/api/v1/**` baseline calls — DEV-11 no longer masks this step and the plain method/path/
    status/location compare passed with zero problems, confirming the packet's headline claim
    that DEV-11 is closed for login on this wave. Steps 5/6: `POST /pl/login` 200, response
    bodies carry `data-login-error`/`data-last-username` attributes on the candidate vs. inline
    `<span>` markup on the oracle — `extractLoginErrorAndUsername()`'s two regex alternatives
    handle exactly this, and the extracted values are byte-identical
    (`"Podaj adres e-mail."` / `"maciej@aureashop.pl"` for step 5,
    `"To nie wygląda na poprawny adres e-mail."` / `"not-an-email"` for step 6). Step 7:
    `POST /pl/logout` → 302 `Location: /pl/login` both sides. Step 8: `GET /pl/dashboard` → 200
    both sides (post-logout dashboard access is allowed on both stacks — a genuine product
    behaviour match, not a hidden auth bypass on only one side). `db.json`: `sessions` delta
    oracle 0 / candidate 1 — inside DEV-9's declared ±1 tolerance, correctly tagged
    `accepted-deviation(DEV-9)` rather than silently passed as `parity`.
  - event-stream: steps 2-3's `/collect` response bodies (`{"status":"accepted"}`,
    `{"status":"duplicate"}`) and steps 4-5's two Polish 400 error bodies
    (`Pole „idempotency_id” jest wymagane.`, `Nieznany typ zdarzenia „teleport”.`) are exact
    matches after canonicalization (oracle stores `„`-escaped, candidate stores the raw
    UTF-8 `„` glyph — `JSON.parse`+`JSON.stringify` round-trip both to the same value, confirmed
    by direct inspection, not just trusting `compareJsonBodies`'s boolean result). Per-run
    `idempotency_id`/`detail` values differ between the two captures (`cap4d` vs. `mttrxg4zd`,
    different `RUN_ID`s) but that is expected and irrelevant — only response bodies are
    deep-compared, never the request. `db.json`: oracle `cache_items` delta 1 maps exactly to
    candidate `event_dedup` delta 1 (`shedlock` delta 0), an exact match, not a tolerance case.
  - shell-navigation: spot-checked step 21 (`GET /de/dashboard` → 404, identical both sides) and
    confirmed a spread of 17 distinct `document`-kind requests across the 21 steps land on the
    expected sidebar routes (`/pl/dashboard`, `/pl/customers`, `/pl/events`, `/en/customers`,
    `/pl/campaigns`, `/de/dashboard`, `/pl/feeds`, `/pl/settings`, `/pl/import`,
    `/en/dashboard`, `/pl/popups`, `/pl/automations`), all 200 except the deliberate 404. `db.json`
    `sessions` delta 1/1 exact.
- No journey's contract dimension required a second (retry) run — recorded here per the packet's
  instruction to record both runs on a throw/timeout; none occurred.

## Evidence paths

Evidence root: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/contract/`
(absolute: `/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/contract/`).

- `popups-widget-editor/report.md`, `popups-widget-editor/report.json`, `popups-widget-editor/steps/1..7/{http.jsonl,url.txt,step.json}`, `popups-widget-editor/db.json`
- `import-wizard/report.md`, `import-wizard/report.json`, `import-wizard/steps/1..10/{http.jsonl,url.txt,step.json}`, `import-wizard/db.json`
- `dashboard/report.md`, `dashboard/report.json`, `dashboard/steps/1..4/{http.jsonl,url.txt,step.json}`, `dashboard/db.json`
- `login/report.md`, `login/report.json`, `login/steps/1..8/{http.jsonl,url.txt,step.json}`, `login/db.json`
- `event-stream/report.md`, `event-stream/report.json`, `event-stream/steps/1..7/{http.jsonl,url.txt,step.json}`, `event-stream/db.json`
- `shell-navigation/report.md`, `shell-navigation/report.json`, `shell-navigation/steps/1..21/{http.jsonl,url.txt,step.json}`, `shell-navigation/db.json`
- `logs/{popups-widget-editor,import-wizard,dashboard,login,event-stream,shell-navigation}.run1.log` — raw stdout of each `compare.mjs` invocation

## Post-run cleanup

Per the packet's disk-tight instruction, deleted `tests/e2e/node_modules` and
`tools/migration-verify/node_modules` from this verifier's own checkout
(`/home/muszkin/work/kivvi-click-wt/verify-wave-4-contract`) after all six runs completed and
evidence was written. No other checkout, the main repo, or the oracle was touched.
