# Verifier verdict — wave wave-0, dimension `unit` (round 2)

- Wave SHA: `6a53642c9c5ec9c256625854e6670f035e0292be`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit`, `git rev-parse HEAD` = `6a53642c9c5ec9c256625854e6670f035e0292be` (matches).
- Confirmed absent, not created: `frontend/dist`, `backend/src/main/resources/static`.

## Dimension status: FAIL

## Journey verdict

| Journey | Verdict | Deviation ID |
| --- | --- | --- |
| login | regression | none |

Reason: behaviour B22's login-relevant assertion ("browser blocks malformed e-mail" via native `type=email`) has no test reachable by the unit dimension's mandated command. Its only named test, `describe("B22 the login form matches the browser-validated, server-rendered contract", …)` in `frontend/test/integration/LoginView.spec.ts`, is excluded from `npm run test -- --run` (`vitest run test/unit`) and only runs under `npm run test:integration`. No deviation in the plan's "Accepted deviations" table (DEV-1..DEV-12) covers this gap.

All other checked behaviours (B01 login/dashboard rows, B04, B07, B08, B09, B10, B11, B12, B13, B14) map to real, green, non-tautological unit-dimension tests in both backend and frontend. Both test suites themselves are fully green.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend` | 0 |
| `npm run test -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend` | 0 |

Backend: 12 `*Test.java` classes, 66 tests, 0 failures/errors/skipped (`*IT.java` files — `SessionRoundTripIT`, `ShellApiIT` — correctly excluded by Surefire's default pattern; they belong to the integration dimension).
Frontend: `Test Files 5 passed (5)`, `Tests 32 passed (32)` (`test/unit` only; `test/integration` correctly excluded).

Fail-on-warning test policy (unit-dimension fact): active on both sides — backend via auto-detected `FailOnWarnLogExtension` (`META-INF/services` + `junit.jupiter.extensions.autodetection.enabled=true`), frontend via global `vite.config.ts` `test.setupFiles: ["./test/setup.ts"]` (throws on first-party `console.warn`/`console.error`/Vue runtime warnings; `dangerouslyIgnoreUnhandledErrors=false`).

## Evidence

- `waves/wave-0/unit/behaviour-test-mapping.md` — full behaviour → test table with tautology notes
- `waves/wave-0/unit/backend-mvnw-test.log`
- `waves/wave-0/unit/frontend-npm-ci.log`
- `waves/wave-0/unit/frontend-npm-test.log`
