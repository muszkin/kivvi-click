# Wave-0 verifier — dimension: integration (round 3)

Wave SHA: `887af4543c5a51255735f1ddfc0d8860d11551c0` — checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int`, HEAD confirmed equal to the wave SHA before any command ran.

## Dimension status: PASS

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| login | parity |

All 8 required integration-level behaviours for `login` (B04, B07, B08, B10, B11, B12, B13, B14) are exercised by `*IT` classes that boot a real Spring context with `@SpringBootTest(webEnvironment = RANDOM_PORT)`, issue real HTTP calls via `TestRestTemplate`, and back the session store with a real Testcontainers Postgres 18 container. No behaviour is covered only by a `@WebMvcTest` slice. Every test that ran is green; Testcontainers were not skipped. Full mapping and a B-id citation audit: `waves/wave-0/integration/behaviour-mapping.md`.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int` | 0 (`887af4543c5a51255735f1ddfc0d8860d11551c0`) |
| 2 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/backend` | 0 (re-run confirmed clean, exit 0) |
| 3 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/frontend` | 0 |
| 4 | `npm run test:integration -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/frontend` | 0 |

## Backend (`./mvnw -q verify`)

- Unit phase (surefire, 12 classes incl. `ArchitectureTest`, `LoginControllerTest`, `PreferencesControllerTest`, `SpaDocumentControllerTest` — all `@WebMvcTest`/plain slices, not counted for this dimension): all green, 0 failures/errors.
- Integration phase (failsafe, Testcontainers Postgres 18 `postgres:18-alpine`): 2 `*IT` classes ran.
  - `SessionRoundTripIT` — 4 tests, 0 failures/errors, 0 skipped (6.332s). Covers B08, B10, B11, B14.
  - `ShellApiIT` — 7 tests, 0 failures/errors, 0 skipped (2.555s). Covers B04 (×2), B07 (×2), B12, B13, plus one non-B-id oracle-parity test (dashboard shell payload).
  - `target/failsafe-reports/failsafe-summary.xml`: `completed=11 errors=0 failures=0 skipped=0 flakes=0`.
- Testcontainers confirmed started per `*IT` class from the log (`tc.postgres:18-alpine -- Container ... is starting` / `is started`, and Flyway logging `Database: jdbc:postgresql://localhost:<port>/test ... (PostgreSQL 18.6)`), i.e. two independent Postgres 18 containers, one per `@SpringBootTest` context.
- Build produced `target/kivvi-click-0.1.0-w0-login.jar`; overall Maven exit code 0.

## Frontend (`npm run test:integration -- --run`)

- `vitest run test/integration --run`: 3 test files, 13 tests, all passed (1.65s).
  - `LoginView.spec.ts` (component-level, router+i18n mounted): welcome-copy rendering, native form contract, server-injected error/last-username attribute rendering for B12/B13-shaped states.
  - `shellStore.spec.ts` (Pinia store, fetch mocked): `setTheme`/`setSidebar` client behaviour for B08/B10, plus `load()`.
  - `Sidebar.spec.ts` (component-level): active-nav-item and identity-footer rendering.
  - These are supplementary — they do not by themselves satisfy the FAIL conditions since the authoritative real-HTTP/Testcontainers coverage for B04/B07/B08/B10/B11/B12/B13/B14 already exists in the backend `*IT` classes above.

## Findings

- No behaviour lacks a qualifying test; no test is red; Testcontainers ran (not skipped) — none of the packet's three FAIL conditions apply.
- One citation issue flagged for the record (not FAIL-triggering, since B11 has genuine backend coverage): the frontend `LoginView.spec.ts` describe block `"B11 the login view shows the welcome copy and submit action"` only asserts static heading/button text, not the sign-in → redirect → sidebar-identity behaviour B11 actually describes. Detail in `waves/wave-0/integration/behaviour-mapping.md` § B-id citation audit.

## Evidence paths

- `waves/wave-0/integration/behaviour-mapping.md` — full behaviour → test table and citation audit.
- `waves/wave-0/integration/mvnw-verify.log` — full backend build/test log (clean re-run, exit 0).
- `waves/wave-0/integration/failsafe-reports/` — copied `failsafe-summary.xml` + per-class `*IT` reports (`.txt` and `TEST-*.xml`) from the checkout's `backend/target/failsafe-reports`.
- `waves/wave-0/integration/npm-test-integration.log` — full frontend `vitest run test/integration --run` output (13/13 passed).
