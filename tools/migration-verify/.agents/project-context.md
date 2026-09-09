<!-- BEGIN project-context-initializer:context -->
# `tools/migration-verify/` — project context

| Field | Value |
| --- | --- |
| Path | `tools/migration-verify/` |
| Scope | six-dimension verifier tooling for the Symfony→Spring Boot + Vue migration (`migration/spring-vue`) |
| Source revision | `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue` (worktree `/home/muszkin/work/kivvi-click-wt/integration`) |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (single flat directory, no children) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

Replays the immutable oracle's `capture/scenarios.json` against a candidate stack (a journey
worktree, a wave cohort, or the final all-journey cohort) and reports contract/visual/performance
parity, applying the plan's accepted deviations (DEV-1..13) on top. Introduced in wave-0
specifically so the oracle directory itself (`../../context/migration-oracle/symfony-to-spring-vue/`)
never needs to be modified during the run (Observed, plan revision note at the top of
`../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`: "the compare tool moved to
`tools/migration-verify/` so the oracle directory stays immutable; no scope change").

## Files (Observed)

| File | Purpose |
| --- | --- |
| `compare.mjs` (702 lines) | Contract + visual dimensions: replays a journey's oracle scenario against `--base <url>`, screenshots (desktop 1440×900, mobile 390×844) diffed with `pixelmatch`/`pngjs`, a11y tree and text comparison, HTTP recording parity, `db.json`-style delta comparison |
| `performance.mjs` (135 lines) | Performance dimension: bundle-size / LCP / TTI against `budget.json`, `/collect` p95 over 50 sequential requests |
| `deviations.json` | Mechanical encoding of the plan's "Accepted deviations" table (DEV-1..13): which journeys/steps, which dimension, and the exact masking/normalization mechanism or "not-diffed" rule |
| `budget.json` | DEV-8 absolute performance budgets: `initialJsGzipBytes: 307200` (300 kB gzip), `lcpMillis: 2000`, `ttiMillis: 2500`, `collectP95Millis: 500` |
| `package.json` | Declares `pixelmatch`/`pngjs`; reuses Playwright from `../../tests/e2e/node_modules` via `createRequire` rather than its own copy — see `compare.mjs`'s own `require = createRequire(join(root, "tests/e2e/package.json"))` |

## Invariants (do not break)

- **Reads the oracle, never writes to it.** `compare.mjs`/`performance.mjs` `readFileSync` from
  `context/migration-oracle/symfony-to-spring-vue/{capture,journeys}/**` — that directory is
  immutable evidence owned by `migration-planning`, and this tooling's whole reason to exist
  outside the oracle directory is to preserve that.
- **Extend `deviations.json` only with rows for a journey's own `DEV-n` ids**, and only "if the
  mechanism is missing" (`common-journey-rules.md`) — it mechanically encodes the plan's Accepted
  deviations table; a new entry here without a corresponding plan row is a planning gap, not a
  tooling fix.
- **The page clock is frozen** (`page.clock.setFixedTime(new Date("2026-09-08T12:00:00Z"))` in
  `compare.mjs`, matching the oracle capture's own fixed clock). Under that freeze,
  `performance.getEntriesByType("navigation")` returns permanently empty and `Date.now()` is
  constant — this is the actual browser behaviour the verifier exercises, not a quirk to route
  around. **Never make product code (frontend or backend) depend on Navigation Timing or
  wall-clock deltas for correctness** — anything that does will misbehave only under this verifier,
  which is exactly backwards from the goal (`common-journey-rules.md`, wave-3 campaigns repair-3
  lesson, and see `../../frontend/.agents/project-context.md`'s scroll-restoration note for the
  concrete consequence).
- **Contract verifier isolation**: the contract dimension runs *alone* on the shared verification
  stack (or its own stack) — other verifiers' `/collect` traffic (performance budgets, e2e)
  contaminates the `db.json`-style delta comparison. The orchestrator sequences contract after
  e2e/visual finish; do not run contract concurrently with e2e/performance against the same stack.
- **Screenshot diff threshold**: `SCREENSHOT_DIFF_THRESHOLD = 0.005` (0.5%) after masks — matches
  the plan's stated visual parity threshold exactly; do not loosen it locally to make a run pass.
- **`MAIN_SCROLL_ORIGIN = { x: 248, y: 56 }`** (sidebar width × topbar height from
  `../../assets/styles/01-tokens.css`) anchors the DEV-11 fixed-rectangle mask — keep this in sync
  if those token values ever change (they are currently frozen/copied verbatim per the plan).
- **`VERIFY_COMPOSE` env var** controls which compose invocation supplies `db.json` counts (default
  `docker compose -p kivvi-w-login -f compose.yaml`) — pass the caller's own lease project name
  when verifying a different worktree; the wrong project name silently reads someone else's
  database deltas.

## Commands (Observed, `common-journey-rules.md` §Gates)

- `node tools/migration-verify/compare.mjs --journey <id> --base https://localhost:<https-port> --out <dir> [--dimension contract|visual|performance|all]`
- `node tools/migration-verify/performance.mjs --base https://localhost:<https-port>`
- Both are step 5/7 of the seven-step gate chain in `common-journey-rules.md` §Gates (after unit,
  integration, architecture/lint/typecheck, spotless/build, before/around the e2e gate).
- Expected initial state: run `compare.mjs` for a not-yet-implemented journey and it must FAIL
  (empty page / missing API) — that RED-before-GREEN run is required evidence
  (`common-journey-rules.md` §"Expected initial RED"), recorded under
  `<run-dir>/slices/<id>/evidence/red-*.txt`.

## Tests

None of its own (it *is* the test harness for the migration; verified indirectly by every journey's
gate run). No unit-test suite for `compare.mjs`/`performance.mjs` themselves — treat a change to
either as high-risk and confirm it against a known-good journey (e.g. `login`) before relying on it
for a new one.

## Ports / leases / hazards

- Talks to whatever `--base` URL is passed — no port of its own; the caller supplies the journey
  worktree's leased edge port (pool `19000 + 10·n`, plan §"Worktree resource lease").
- Depends on Docker (`execSync` calls the caller-supplied `VERIFY_COMPOSE` invocation) and on
  Playwright's Chromium being installed under `../../tests/e2e/node_modules` — do not delete or
  relocate that `node_modules` without updating the `createRequire` path in `compare.mjs`.
- See R21 in `../../context/map/risks-and-unknowns.md`: this host runs many unrelated containers
  (including self-hosted GitHub Actions runners) that compete for CPU/disk with a verification run;
  a flaky visual/e2e result under load should be re-run in isolation before it is ruled a real
  regression (`common-journey-rules.md`'s cohort-sequencing rule already encodes this for the
  CPU-heavy vs. e2e-alone ordering).

## Evidence paths

`tools/migration-verify/{compare.mjs,performance.mjs,deviations.json,budget.json,package.json}`,
`../../context/migration-oracle/symfony-to-spring-vue/{capture,journeys,manifest.json}` (read-only
input), `../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md` §"Accepted deviations"
and §"Verifier contract",
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/common-journey-rules.md`.
<!-- END project-context-initializer:context -->
