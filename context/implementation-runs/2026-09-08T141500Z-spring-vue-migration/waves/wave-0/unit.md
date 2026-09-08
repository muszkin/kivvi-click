# Wave-0 verifier — dimension `unit`

- Wave SHA: `3b17c07e23354e493edbab572e4090f69d2c0c71`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit` — `git rev-parse HEAD` confirmed
  equal to the wave SHA before any command ran.
- Journey: `login`
- **Dimension status: FAIL** (bound to `3b17c07e23354e493edbab572e4090f69d2c0c71`)
- **Journey verdict — login: regression** (no `deviation_id` in `tools/migration-verify/deviations.json`
  or the plan's Accepted-deviations table covers a `unit`-dimension gap for B01/B07/B11-B14)

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `backend/` | **1** |
| `npm ci` | `frontend/` | 0 |
| `npm run test -- --run` (= `vitest run test/unit`) | `frontend/` | 0 |

Backend: 59 tests, 0 failures, **27 errors**, 0 skipped. All 27 errors are in
`click.kivvi.web.LoginControllerTest` (4/4) and `click.kivvi.web.SpaDocumentControllerTest`
(23/23) — both fail `ApplicationContext` bootstrap because `IndexHtmlTemplate` cannot find
`classpath:static/index.html` (frontend build output was never copied into
`backend/src/main/resources/static`; see mapping doc for the exact stack trace and root-cause
analysis). `click.kivvi.web.PreferencesControllerTest` and every application/domain/architecture
test class passed. Testcontainers-based `*IT` classes (`SessionRoundTripIT`) are Failsafe-bound
(`./mvnw verify`), not run by `test`, per the integration verifier's remit.

Frontend: 5 files, 32 tests, all passed — but `npm run test` only runs `test/unit/*`
(`vitest run test/unit`); `test/integration/*` (including `LoginView.spec.ts`) is a separate
script (`npm run test:integration`) owned by the integration verifier.

## Behaviour → test mapping

Full table with per-test tautology inspection: `waves/wave-0/unit/behaviour-test-mapping.md`.

Summary: B01 (login/dashboard rows), B07, B11, B12, B13, B14 each have a named, non-tautological
backend test, but the controller-level half of each (`LoginControllerTest`,
`SpaDocumentControllerTest`) is currently red — see root cause above. B08, B09, B10, B04 pass in
full (backend + frontend). B22's login-relevant assertions exist and are real
(`test/integration/LoginView.spec.ts`) but are outside the `unit` command's scope by the plan's
own verifier-contract split; not counted as a unit-dimension pass, not treated as satisfying the
FAIL criterion either way since a named test exists — flagged for visibility.

## Verdict rationale

Dimension duty: FAIL if any listed behaviour has no test, if any test is red, or if a mapped test
is tautological. `LoginControllerTest` (B11, B12, B13, B14) and `SpaDocumentControllerTest` (B01
login/dashboard rows, B07) are red under the exact command the plan's Verifier contract assigns
to this dimension (`cd backend && ./mvnw -q test`), from a clean checkout at the wave SHA, with no
extra setup beyond `JAVA_HOME`. This is a reproducible defect (missing frontend-build-before-test
wiring), not a transient/environmental fluke — confirmed by inspecting `pom.xml` (no plugin binds
the frontend build into the `test` phase) and `src/test/resources` (no stub `static/index.html`).
No entry in `tools/migration-verify/deviations.json` (DEV-1..DEV-12) covers a `unit`-dimension gap
for this journey. Per the wave state-machine rule, any `regression` verdict gives the dimension
`FAIL`.

## Evidence

- `waves/wave-0/unit/backend-mvnw-test.log` — full `./mvnw -q test` output (stack traces, per-class
  Surefire results, final `Tests run: 59, Failures: 0, Errors: 27, Skipped: 0`).
- `waves/wave-0/unit/frontend-npm-ci.log`, `waves/wave-0/unit/frontend-npm-test.log`.
- `waves/wave-0/unit/behaviour-test-mapping.md` — per-behaviour test mapping and tautology notes.
