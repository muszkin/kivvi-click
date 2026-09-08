# Wave-1 verifier — dimension: integration

**Status: FAIL** — bound to wave SHA `9427fdb1d92e4e606e67471638031d1bc0fb73d4`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration`
(`git rev-parse HEAD` == wave SHA; working tree clean; no frontend build output created).

## Per-journey verdicts

| Journey | Verdict | Reason |
| --- | --- | --- |
| landing | **regression** | B01's landing view-model endpoint (`GET /api/v1/{locale}/landing`) has no real-HTTP integration test. The only backend test is `LandingControllerTest`, a `@WebMvcTest(LandingController.class)` slice (no Spring context boot, no `DataSource`, no Postgres) — explicitly excluded by the verifier contract. No `*IT` class references `/landing` anywhere. B22's frontend router/component part is fully covered (`frontend/test/integration/landing.spec.ts`, 3 tests, green), but that does not substitute for the missing backend behaviour test. |
| feeds | parity | B01 (`GET /pl/feeds` 200) and B30 (4 sources/4 feeds/one HTTP 503/142 mismatched) both proven through `FeedsApiIT`, a real `@SpringBootTest` + Testcontainers Postgres 18 + `TestRestTemplate` class. 3/3 green. |
| scheduler-heartbeat | parity | B20 (Postgres-backed shedlock round-trip: exactly 1 `shedlock` row) and B33 (exactly one `INFO "Scheduler heartbeat tick."` line captured, second attempt inside `lockAtLeastFor` blocked) both proven by `HeartbeatSchedulerIT`, a real `@SpringBootTest` + Testcontainers Postgres 18 class. 1/1 green. |

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/frontend` | 0 |
| `npm run test:integration -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration/frontend` | 0 |

## Backend `*IT` inventory and results

`find backend/src/test -iname "*IT.java"` → 4 classes (all `@SpringBootTest` +
`@Testcontainers` + `PostgreSQLContainer("postgres:18-alpine")`), 15 tests total, 0
failures/errors/skipped:

- `ShellApiIT` — 7 tests (B04, B07, B12, B13 + oracle-parity shell payload)
- `FeedsApiIT` — 3 tests (B01 feeds, B30, B07)
- `infrastructure.scheduling.HeartbeatSchedulerIT` — 1 test (B20, B33)
- `SessionRoundTripIT` (wave-0) — 4 tests, still green

Testcontainers Postgres 18 confirmed started in every class's log
(`Container postgres:18-alpine started`, `PostgreSQL 18.6`); the heartbeat IT log shows
exactly one `Scheduler heartbeat tick.` INFO line captured by its `ListAppender` assertion
and one row in `shedlock` for `HeartbeatJob.LOCK_NAME` (both asserted in-test, see
`HeartbeatSchedulerIT.schedulerTicksOnceAndLockBlocksASecondAttempt`).

## Frontend integration suite

`npm run test:integration -- --run` → 5 test files, 20 tests, all passed in 1.60s:
`FeedsView.spec.ts`, `landing.spec.ts`, `LoginView.spec.ts`, `shellStore.spec.ts`,
`Sidebar.spec.ts`. These are real `vue-router` + Pinia + component mounts (not smoke tests).

## Behaviour → test mapping (full detail)

See `integration/behaviour-mapping.md`. Summary:

| Behaviour | Verdict |
| --- | --- |
| B01 (feeds row) | met — `FeedsApiIT` |
| B01 (landing row) | **NOT MET** — no `*IT`/real-Spring-context test exists for `GET /api/v1/{locale}/landing`; only a `@WebMvcTest` slice |
| B22 (landing part, frontend) | met — `frontend/test/integration/landing.spec.ts` |
| B30 (feeds) | met — `FeedsApiIT` |
| B20 (lock/state round-trip) | met — `HeartbeatSchedulerIT` |
| B33 (tick logged) | met — `HeartbeatSchedulerIT` |

## Verdict rationale

Per the verifier contract: "FAIL if any behaviour lacks such a test, any test is red, or
Testcontainers tests were skipped." All executed tests are green and Testcontainers ran (not
skipped), but **B01's landing row lacks a qualifying integration-level test** — the packet is
explicit that a `@WebMvcTest` slice does not count, and the sole existing test for the landing
view-model endpoint is exactly that slice. This is a genuine coverage gap, not a flaky or
environmental issue: the wave never added a `LandingApiIT` (or equivalent) analogous to
`FeedsApiIT`. Dimension status is therefore **FAIL**, scoped to the `landing` journey; `feeds`
and `scheduler-heartbeat` are `parity`.

## Evidence paths

- `integration/backend-mvnw-verify.log` — full `./mvnw -q verify` output
- `integration/failsafe-summary.txt` — concatenated failsafe per-class summaries
- `integration/frontend-npm-test-integration.log` — `npm run test:integration -- --run` output
- `integration/behaviour-mapping.md` — full behaviour-to-test mapping and grep evidence
