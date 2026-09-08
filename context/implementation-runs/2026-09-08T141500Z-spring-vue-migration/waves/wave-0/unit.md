# Verifier verdict — wave wave-0, dimension unit, round 3

- Wave SHA: `887af4543c5a51255735f1ddfc0d8860d11551c0`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit` — `git rev-parse HEAD` ==
  wave SHA (confirmed before and after test runs), working tree clean, no tracked-file edits made.
- `backend/src/main/resources/static` and `frontend/dist`: absent before the run and absent
  after (confirmed) — not created by `mvn test` or `npm run test`.
- Oracle manifest sha256 recomputed: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`
  — matches the packet's recorded value.

## Dimension status: PASS (bound to SHA 887af4543c5a51255735f1ddfc0d8860d11551c0)

## Journey verdict

| Journey | Verdict |
| --- | --- |
| login | parity |

All eleven required behaviour rows (B01 login/dashboard rows, B04, B07, B08, B09, B10, B11,
B12, B13, B14, B22 login part) map to a named test that the unit commands actually execute,
every mapped test is green, and every mapped assertion is a real, failable check (literal
strings/status codes/redirect targets/session state, not a tautology). Full mapping and the
one gap explicitly investigated (B08/B10's "next document renders" clause, which belongs to
the `integration` dimension, not `unit` — see rationale) are in `unit/behaviour-map-login.md`.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend` | 0 |
| `npm run test -- --run` (= `vitest run test/unit --run`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend` | 0 |

Backend: 66 tests across 12 `*Test.java` classes, 0 failures, 0 errors, 0 skipped (JDK
25.0.4.1, confirmed from Spring Boot startup banners in the raw log). `*IT.java` classes
(`SessionRoundTripIT`, `ShellApiIT`) correctly did not run under `mvn test` — verified absent
from `target/surefire-reports/`; they are the `integration` dimension's evidence.

Frontend: 6 test files (`i18n.spec.ts`, `Icon.spec.ts`, `Avatar.spec.ts`, `LoginView.spec.ts`,
`WorkspaceCard.spec.ts`, `routes.spec.ts`), 36 tests, all passed.

## Evidence paths

- `waves/wave-0/unit/behaviour-map-login.md` — full behaviour -> test mapping and tautology check
- `waves/wave-0/unit/backend-mvnw-test.log` — raw `mvn test` output
- `waves/wave-0/unit/backend-surefire-summary.txt` — per-class surefire summaries (all green)
- `waves/wave-0/unit/frontend-vitest-unit.log` — raw `vitest run test/unit` output
