# Wave-3 verifier — dimension: unit (round 2)

**Dimension status: PASS**, bound to wave SHA `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`.

Independent verification. No worker reports, review files, or round-1 evidence were read.
Verified from a detached checkout at the wave SHA: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit`
(confirmed `git rev-parse HEAD` = `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`, `git status --short` clean).
Oracle: `/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue`
(`manifest.json` sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — matches the packet).

## Journeys in scope and verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| automations | parity | B26, and B01's automations rows, each map to a named, green unit test (backend + frontend) |
| settings | parity | B06, B32, and B01's settings row, each map to a named, green unit test (backend + frontend); DEV-12 (settings step 10 = 404 document) is a visual-dimension deviation, not unit — no unit-test impact |
| campaigns-email-editor | parity | B27, B28, and B01's campaigns/email-editor rows, each map to a named, green unit test (backend + frontend); DEV-7 (no POST …/blocks) is a contract-dimension deviation — no unit-test impact |

No regressions found; no accepted-deviation verdicts were needed for this dimension (DEV-4,
DEV-7, DEV-12 in the packet's "deviations in scope" all apply to the contract/visual
dimensions, not unit — confirmed against `tools/migration-verify/deviations.json`).

## Behaviour → test mapping

Full table: `unit/behaviour-mapping.md`. Summary: every behaviour in `behaviours.json` whose
`journey` or `also` list includes `automations`, `settings`, or `campaigns-email-editor`
(B01, B06, B26, B27, B28, B32 — confirmed exhaustive by filtering the oracle file directly,
matching the packet's "Behaviours in scope" line) has at least one named unit test
(`@DisplayName("Bnn …")` in a Surefire `*Test.java`, or `describe("Bnn …")` in
`frontend/test/unit/*.spec.ts`), and every one of those tests passed.

`*IT.java` files (`AutomationsApiIT`, `CampaignsApiIT`, `SettingsApiIT`, etc. — the round-2
repair's "B01-labelled real-HTTP ITs") were identified but excluded from this dimension per
the packet's definition (unit = Surefire `*Test.java` only; `*IT.java` does NOT count). They
are the integration dimension's evidence.

## Wave-3 shell regression guard

Both shell changes named in the packet are covered by green frontend unit tests:
`frontend/test/unit/scrollRestoration.spec.ts` (14 tests) and
`frontend/test/unit/localeHref.spec.ts` (7 tests, `B32/repair-1 buildLocaleHref`). Detail in
`unit/behaviour-mapping.md`.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit` | 0 (confirmed `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`) |
| 2 | `git status --short --branch` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit` | 0 (clean) |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/backend` | 0 |
| 4 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |
| 5 | `npm run test -- --run` (= `vitest run test/unit --run`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |
| 6 | `npx vitest run test/unit --reporter=verbose` (evidence-capture re-run of #5, same result) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |

## Results

- Backend (`./mvnw -q test`): 37 Surefire test classes, 225 tests, 0 failures, 0 errors, 0
  skipped. Per-class breakdown: `unit/backend-surefire-summary.csv`. No `*IT.java` file was
  executed by this command (confirmed: the 37 classes in `backend/target/surefire-reports/`
  match exactly the 37 `*Test.java` files under `backend/src/test`, none of the 11 `*IT.java`
  files ran).
- Frontend (`npm run test -- --run`): 21 test files under `frontend/test/unit`, 126 tests, 0
  failures. `frontend/test/integration/*` was not invoked (that script is `test:integration`,
  a separate npm script, not run here).

## Evidence paths

- `unit/backend-mvnw-test.log` — full stdout/stderr of the backend unit run.
- `unit/backend-surefire-summary.csv` — per-class tests/failures/errors/skipped, parsed from
  `backend/target/surefire-reports/*.txt` in the verifier checkout.
- `unit/frontend-vitest-unit.log` — `npm run test -- --run` output (21 files / 126 tests, all
  passed).
- `unit/frontend-vitest-unit-verbose.log` — verbose per-test listing of the same run, used to
  confirm each behaviour-labelled `describe`/`it` block is present and green.
- `unit/behaviour-mapping.md` — full behaviour → test table and regression-guard detail.
