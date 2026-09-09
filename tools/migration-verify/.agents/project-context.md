<!-- BEGIN project-context-initializer:context -->
# `tools/migration-verify/` — project context

| Field | Value |
| --- | --- |
| Path | `tools/migration-verify/` |
| Scope | Post-cutover parity/regression-check tooling (six-dimension verifier, kept running after the migration) |
| Source revision | `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main` |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (single flat directory, no children) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

Replays the immutable oracle's `capture/scenarios.json` against a candidate stack and reports
contract/visual/performance parity, applying the migration plan's accepted deviations (DEV-1..12;
DEV-13 retired 2026-09-09, unused) on top. Built during the migration specifically so the oracle
directory itself (`../../context/migration-oracle/symfony-to-spring-vue/`) never needed to be
modified while the migration ran. The migration is now complete and the old stack it compared
against is deleted, but `CLAUDE.md`/`AGENTS.md` both explicitly keep this tool as a **post-cutover
regression check** — it still verifies the current stack against the frozen pre-migration oracle
capture, which remains a valid, byte-exact behavioural reference for the panel's UI/contract
surface. It was used this way, read-only, in the CON-1 exit verification
(`performance.mjs --base https://kivvi.click`, see
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md`).

## Files (Observed)

| File | Purpose |
| --- | --- |
| `compare.mjs` (702 lines) | Contract + visual dimensions: replays a journey's oracle scenario against `--base <url>`, screenshots (desktop 1440×900, mobile 390×844) diffed with `pixelmatch`/`pngjs`, a11y tree and text comparison, HTTP recording parity, `db.json`-style delta comparison |
| `performance.mjs` (135 lines) | Performance dimension: bundle-size / LCP / TTI against `budget.json`, `/collect` p95 over 50 sequential requests — this is the one actually exercised post-cutover so far |
| `deviations.json` | Mechanical encoding of the plan's "Accepted deviations" table (DEV-1..13, DEV-13 marked retired): which journeys/steps, which dimension, and the exact masking/normalization mechanism or "not-diffed" rule |
| `budget.json` | DEV-8 absolute performance budgets: `initialJsGzipBytes: 307200` (300 kB gzip), `lcpMillis: 2000`, `ttiMillis: 2500`, `collectP95Millis: 500` — all four confirmed PASS against production 2026-09-09 |
| `package.json` | Declares `pixelmatch`/`pngjs`; reuses Playwright from `../../tests/e2e/node_modules` via `createRequire` rather than its own copy |

## Invariants (do not break)

- **Reads the oracle, never writes to it.** `compare.mjs`/`performance.mjs` `readFileSync` from
  `../../context/migration-oracle/symfony-to-spring-vue/{capture,journeys}/**` — that directory
  is immutable evidence, and this tooling's whole reason to exist outside the oracle directory is
  to preserve that.
- **Extend `deviations.json` only with rows for a journey's own `DEV-n` ids** — it mechanically
  encodes the plan's Accepted deviations table; a new entry here without a corresponding plan row
  would be a planning gap, not a tooling fix.
- **The page clock is frozen** (`page.clock.setFixedTime(new Date("2026-09-08T12:00:00Z"))` in
  `compare.mjs`, matching the oracle capture's own fixed clock). Under that freeze,
  `performance.getEntriesByType("navigation")` returns permanently empty and `Date.now()` is
  constant — this is deliberate verifier behaviour, not a quirk to route around. **Never make
  product code (frontend or backend) depend on Navigation Timing or wall-clock deltas for
  correctness** — anything that does will misbehave only under this verifier (see
  `../../frontend/.agents/project-context.md`'s scroll-restoration note for the concrete
  consequence that shaped `docs/adr/0002`).
- **Screenshot diff threshold**: `SCREENSHOT_DIFF_THRESHOLD = 0.005` (0.5%) after masks — do not
  loosen it locally to make a run pass.
- **`MAIN_SCROLL_ORIGIN = { x: 248, y: 56 }`** anchors a fixed-rectangle mask tied to the design
  system's sidebar-width/topbar-height tokens (now `../../frontend/src/styles/`) — keep this in
  sync if those token values ever change.
- **`VERIFY_COMPOSE` env var** controls which compose invocation supplies `db.json` counts for
  `compare.mjs`. Its default still references a per-journey migration-worktree project name
  (`kivvi-w-login`) from when the migration was actively running — pass the current stack's own
  compose project name (e.g. `kivvi-dev` for local dev, or the prod project if ever pointed at
  prod) explicitly when running `compare.mjs` post-cutover; the wrong project name silently reads
  someone else's database deltas.

## Commands (Observed, `package.json`, `budget.json`)

- `node tools/migration-verify/compare.mjs --journey <id> --base <url> --out <dir> [--dimension contract|visual|performance|all]`
- `node tools/migration-verify/performance.mjs --base <url>` — this is the one CLAUDE.md's own
  Commands section documents post-cutover; last run `--base https://kivvi.click`, all 4 budgets
  PASS (`cutover/con1-e2e.md`).
- Requires a local `frontend/dist` build for the JS-bundle-size metric when not fetching a
  pre-built server response — `cd frontend && npm ci && npm run build` first if `dist/` is
  absent (Observed, `cutover/con1-e2e.md` step1).

## Tests

None of its own (it *is* the test harness). No unit-test suite for `compare.mjs`/
`performance.mjs` themselves — treat a change to either as high-risk and confirm it against a
known journey (e.g. `login`) before relying on it.

## Deployment / operational notes (current, single stack)

- Talks to whatever `--base` URL is passed — no port of its own. During the migration this was a
  leased journey-worktree edge port; post-cutover it is typically the dev stack
  (`https://localhost:8443`) or, as in the CON-1 exit check, production (`https://kivvi.click`).
- Depends on Docker only for `compare.mjs`'s `db.json` delta comparison (via `VERIFY_COMPOSE`)
  and on Playwright's Chromium being installed under `../../tests/e2e/node_modules` — do not
  delete or relocate that `node_modules` without updating the `createRequire` path in
  `compare.mjs`.
- Historical: R21 in `../../context/map/risks-and-unknowns.md` records host disk/CPU contention
  that affected verification runs during the migration; disk pressure has since eased (37 GB
  free as of this refresh) but the underlying host-sharing condition (other tenants' containers,
  autoscaled CI runners) is unchanged — re-run an ambiguous flaky result in isolation before
  ruling on it.

## Evidence paths

`tools/migration-verify/{compare.mjs,performance.mjs,deviations.json,budget.json,package.json}`,
`../../context/migration-oracle/symfony-to-spring-vue/{capture,journeys,manifest.json}`
(read-only input), `../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`
§"Accepted deviations" and §"Verifier contract",
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/{common-journey-rules.md,
cutover/con1-e2e.md}`.
<!-- END project-context-initializer:context -->
