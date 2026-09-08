# Wave-2 verifier — dimension: unit

Wave SHA: `22d7fcb6380723728a33fc21fda22a92594a2e88`. Checkout:
`/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit` (HEAD confirmed == wave SHA; `git status`
clean; `backend/src/main/resources/static` and `frontend/dist` confirmed absent, not created).

## Dimension status: FAIL

Bound to wave SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`.

## Per-journey verdicts

| Journey | Verdict | Reason |
| --- | --- | --- |
| event-stream | regression | B17 and B18 have no executed unit-command test (see behaviour-mapping.md). No `deviation_id` in `tools/migration-verify/deviations.json` covers the unit dimension for either. |
| customers | parity | B03, B05, B25 and the B01 customers/profile rows all map to executed, green, non-tautological unit-command tests. |

## Commands run

1. `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test`
   cwd: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/backend`
   exit code: 0
   Result: 28 test classes, 145 tests, 0 failures, 0 errors, 0 skipped (no `*IT.java` executed — Failsafe-only, confirmed absent from surefire-reports).

2. `npm ci`
   cwd: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/frontend`
   exit code: 0
   Result: 259 packages installed, 0 vulnerabilities.

3. `npm run test -- --run` (runs `vitest run test/unit`)
   cwd: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/frontend`
   exit code: 0
   Result: 14 test files, 74 tests, all passed (`test/integration/*` not executed by this script).

## Behaviour coverage (scope: B15, B16, B17, B18, B19, B24 event-stream; B03, B05, B25 customers; B01 events/customers/profile rows)

Full mapping table, per-test evidence and tautology check: see `unit/behaviour-mapping.md`.

Summary:
- Covered by an executed, green, falsifiable unit-command test: B15, B16, B19, B24, B03, B05, B25, B01 (events/customers/profile rows).
- **Not covered** — behaviour exists only in a Failsafe `*IT.java` integration test, never run by the unit commands:
  - **B17** ("accepted event is published once ... as a rendered event-row with translated type and detail") — only proven by `CollectApiIT::acceptedEventIsPublishedAsJson` (tagged "DEV-3", not run by `mvn test`). `CollectControllerTest` stubs the ingestion service and never exercises the real `EventIngestionService.toEnvelope()`/publish path; no unit test targets `EventIngestionService` directly.
  - **B18** ("same idempotency id is never published twice") — only proven by `EventDedupStoreIT::*` (Testcontainers, not run by `mvn test`); `EventDedupCleanupJobTest` covers the sweep job, not `claim()`.

No mapped test that did run showed tautological assertions (e.g. `assertTrue(true)`); all inspected tests assert concrete, falsifiable values (status codes, exact Polish error strings, row/column counts, section names) that would fail under a real regression.

## Cause of FAIL

Per the wave-2 rules, any journey verdict of `regression` gives dimension status `FAIL`. The
event-stream journey is `regression` because two in-scope behaviours (B17, B18) lack an executed
unit-command test, satisfying the packet's explicit FAIL condition ("FAIL if any wave-2 behaviour
lacks an executed unit-command test"). This is a coverage gap only — every unit-command test that
did run is green (145/145 backend, 74/74 frontend); nothing is red.

## Evidence paths (all under this wave's unit/ evidence dir)

- `unit/backend-mvnw-test.log` — full `./mvnw -q test` output
- `unit/backend-surefire-summary.txt` — per-class `Tests run` lines for all 28 executed classes
- `unit/frontend-npm-ci.log` — `npm ci` output
- `unit/frontend-npm-test.log` — `npm run test -- --run` output (14 files / 74 tests, vitest summary)
- `unit/behaviour-mapping.md` — full behaviour -> test mapping table with file paths, @DisplayName tags and gap analysis
