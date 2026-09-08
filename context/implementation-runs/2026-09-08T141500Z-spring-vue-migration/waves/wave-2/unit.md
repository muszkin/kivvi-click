# Wave-2 (round 2) — dimension: unit — verdict

**Dimension status: PASS**, bound to wave SHA `b87a701244f5316e1a53bace7a1e41facdffc277`.

Verified 2026-09-08T22:50Z by an independent read-only verifier, from the detached checkout
`/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit` (verified clean and at the exact SHA before
running anything; no files edited; no Docker stacks started/stopped; worker reports and review
files were not read). Journeys in scope: **event-stream**, **customers**. Behaviours in scope
(per packet): B15–B19, B24 (event-stream); B03, B05, B25 (customers); B01 events/customers/profile
rows.

## Per-journey verdicts

| Journey | Verdict |
| --- | --- |
| event-stream | **parity** — B15, B16, B17, B18, B19, B24 each map to a named, green unit test (backend and/or frontend); 0 failures. |
| customers | **parity** — B01 (events/customers/profile rows), B03, B05, B25 each map to a named, green unit test; 0 failures. B03's test lacks the "B03" DisplayName prefix (GAP-1, non-blocking — see `behaviour-test-map.md`). |

No behaviour in scope was found untested, and no test covering an in-scope behaviour was red,
disabled, or skipped.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git status && git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit` | 0 (clean, HEAD = `b87a701244f5316e1a53bace7a1e41facdffc277`) |
| 2 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; java -version` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/backend` | 0 (OpenJDK 25.0.4.1 LTS Temurin) |
| 3 | `./mvnw -q test` (first run, tee'd to `unit/backend-mvnw-test.log`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/backend` | 0 |
| 4 | `./mvnw -q test` (second, un-piped confirmation run) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/backend` | 0 (`MVNW_EXIT_CODE=0`, confirms run #3's pipe-obscured exit status) |
| 5 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/frontend` | 0 (259 packages, 0 vulnerabilities) |
| 6 | `npm run test -- --run` (i.e. `vitest run test/unit --run`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-unit/frontend` | 0 |

## Results

- **Backend** (`./mvnw -q test`, Surefire — Testcontainers-backed `*IT.java` classes correctly
  excluded from this phase per `pom.xml`'s Surefire/Failsafe split): **30 test classes, 170 tests,
  0 failures, 0 errors, 0 skipped.** Aggregated from `target/surefire-reports/*.txt`; see
  `unit/backend-surefire-summary.txt`.
- **Frontend** (`vitest run test/unit`): **14 test files, 74 tests, all passed, 0 skipped.** See
  `unit/frontend-vitest-unit.log`.
- Full behaviour → named-test mapping, with file/class/@DisplayName or describe/it text for every
  in-scope behaviour, is in `unit/behaviour-test-map.md`.

## Findings

- **GAP-1 (non-blocking):** `ShellViewServiceTest.detailRouteKeepsItsIndexSectionCurrent()` is the
  only unit-level proof of B03 and is green, but its `@DisplayName` does not carry the "B03" prefix
  used by every other behaviour-tagged test seen in this run (B01, B04, B05, B07, B15–B19, B24, B25
  all do). Found by content search, not by the `Bnn` naming convention the verifier contract
  describes. Does not change the PASS verdict — the behaviour is genuinely proven and the test is
  green — but is a traceability nit worth a one-line fix in a future repair pass.
- B03's full-stack (router + real HTTP) proof lives in `click.kivvi.CustomersApiIT` and
  `frontend/test/integration/CustomerDetailSidebarSection.spec.ts`; both are `*IT`/`test:integration`
  files, correctly out of scope for the **unit** command set (Surefire/Failsafe split; frontend
  `test` vs `test:integration` npm scripts) and belong to the **integration** dimension's own
  verification, not this one.
- Bonus signal (not this dimension's duty, not separately verified beyond what `mvnw test` itself
  ran): `ArchitectureTest` (ArchUnit, 5 tests) also ran clean as part of the same `mvnw test`
  invocation — 0 failures. The **architecture** dimension verifier owns that dimension's own
  verdict; noted here only because it rode along in the same Surefire run.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/unit/backend-mvnw-test.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/unit/backend-surefire-summary.txt`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/unit/frontend-vitest-unit.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-2/unit/behaviour-test-map.md`
- Raw Surefire XML/txt per class: `backend/target/surefire-reports/` in the verifier's own checkout
  (not copied wholesale into the evidence dir; the aggregate and per-class `Tests run:` lines are
  captured in `backend-surefire-summary.txt`).
