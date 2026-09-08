# Wave-1 verifier — dimension: integration (round 2)

**Status: PASS** — bound to wave SHA `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration`
(`git rev-parse HEAD` == wave SHA; working tree clean before any command ran; no frontend build
output created).

Round-1 (SHA `9427fdb1d92e4e606e67471638031d1bc0fb73d4`) failed this dimension because
`LandingApiIT` did not exist. The repair adds it; this round confirms the fix and re-runs the full
gate against the fast-forwarded wave SHA.

## Per-journey verdicts

| Journey | Verdict | Reason |
| --- | --- | --- |
| landing | parity | B01 (landing row) and B22 (landing part) both proven: backend via `LandingApiIT` (real `@SpringBootTest` + Testcontainers Postgres 18 + `TestRestTemplate`, 4/4 green) covering the view-model payload, the `/en` copy behaviour and the `/pl/demo` → `/pl/dashboard` redirect; frontend via `frontend/test/integration/landing.spec.ts` (real Vue Router + mounted component, 3/3 green) covering route resolution and the plain (non-router) demo link. |
| feeds | parity | B01 (feeds row) and B30 both proven by `FeedsApiIT` (real `@SpringBootTest` + Testcontainers Postgres 18 + `TestRestTemplate`, 3/3 green): document route 200, and the payload's 4 sources/4 feeds/one `HTTP 503` failure/142 mismatched, all asserted with concrete values. |
| scheduler-heartbeat | parity | B20 (Postgres-backed shedlock round-trip) and B33 (tick logged exactly once; a repeat call inside `lockAtLeastFor` is absorbed) both proven by `HeartbeatSchedulerIT` (real `@SpringBootTest` + Testcontainers Postgres 18, 1/1 green): exactly one `shedlock` row and exactly one captured `INFO "Scheduler heartbeat tick."` log line, including after a direct second `tick()` call. |

No behaviour in scope (B01 landing/feeds rows, B22 landing part, B30, B20, B33) is covered only by
a `@WebMvcTest` slice — see `integration/behaviour-mapping.md` for the full test-by-test citation.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration` | 0 (`4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`) |
| 2 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/backend` | 0 |
| 3 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/frontend` | 0 |
| 4 | `npm run test:integration -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/frontend` | 0 |

## Backend `*IT` inventory and results (`./mvnw -q verify`, Failsafe)

`find backend/src/test -iname "*IT.java"` → 5 classes, all `@SpringBootTest` + `@Testcontainers` +
`@Container @ServiceConnection PostgreSQLContainer("postgres:18-alpine")`; 19 tests total, 0
failures/errors/skipped:

- `LandingApiIT` — 4 tests (B01 landing, B22 `/en` copy + `/pl/demo` redirect, plus one
  unsupported-locale 404 test not in this wave's behaviour scope). **New in this repair — this is
  the class round-1 found missing.**
- `FeedsApiIT` — 3 tests (B01 feeds, B30, plus one unsupported-locale 404 test).
- `HeartbeatSchedulerIT` — 1 test (B20, B33).
- `ShellApiIT` — 7 tests (wave-0, unaffected — still green).
- `SessionRoundTripIT` — 4 tests (wave-0, unaffected — still green).

Testcontainers confirmed started, not skipped, per class — log lines
`tc.postgres:18-alpine -- Container postgres:18-alpine started in ...s` /
`Container is started (JDBC URL: jdbc:postgresql://localhost:<port>/test...)`, one independent
Postgres 18 container per `@SpringBootTest` context (4 distinct containers/ports logged across the
5 classes, some contexts shared under Spring's test-context cache). Testcontainers version 2.0.5,
Ryuk reaper started. Flyway confirms `PostgreSQL 18.6` on every container. `./mvnw -q verify` (runs
`test` → Surefire unit slices, then Failsafe `integration-test`/`verify`) exited 0.

## Frontend integration suite (`npm run test:integration -- --run`, Vitest)

`vitest run test/integration --run`: **5 files, 20 tests, all passed** (1.33s) —
`FeedsView.spec.ts`, `landing.spec.ts` (wave-1, B22 frontend part), `LoginView.spec.ts`,
`shellStore.spec.ts`, `Sidebar.spec.ts` (wave-0, unaffected — still green).

## Findings

- No behaviour in scope lacks a qualifying test; no test is red; Testcontainers ran on every `*IT`
  class (none skipped) — none of the packet's three FAIL conditions apply.
- Naming honesty check passed for all wave-1 `*IT` classes and `landing.spec.ts`: every
  `@DisplayName`/`describe`/`it` string matches what its test body actually asserts (see
  `integration/behaviour-mapping.md` § Naming honesty check).
- Wave-0 `*IT` classes (`ShellApiIT`, `SessionRoundTripIT`) and wave-0 frontend integration specs
  ran in the same invocations as the wave-1 ones and remain green — no regression observed (wave-0
  behaviours are not separately re-scored here per the packet).

## Evidence paths

- `waves/wave-1/integration/behaviour-mapping.md` — full behaviour → test table, naming-honesty
  check, wave-0 regression check.
- `waves/wave-1/integration/backend-mvnw-verify.log` — full backend `./mvnw -q verify` output.
- `waves/wave-1/integration/failsafe-reports/` — per-class Failsafe `.txt` summaries for all 5
  `*IT` classes (copied from the checkout's `backend/target/failsafe-reports`).
- `waves/wave-1/integration/frontend-npm-ci.log` — `npm ci` output.
- `waves/wave-1/integration/frontend-npm-test-integration.log` — full
  `vitest run test/integration --run` output (20/20 passed).
- Source read directly from the checkout (read-only, not copied): `backend/src/test/java/click/kivvi/LandingApiIT.java`, `FeedsApiIT.java`,
  `infrastructure/scheduling/HeartbeatSchedulerIT.java`,
  `frontend/test/integration/landing.spec.ts`.

Round-1 evidence remains archived, untouched, at `waves/wave-1/round-1/integration.md` and
`waves/wave-1/round-1/integration/`.
