# Verifier report — wave FINAL (all journeys), dimension: unit

- **Wave SHA:** `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194` (checkout `/home/muszkin/work/kivvi-click-wt/verify-final-unit`, detached, clean, verified `git rev-parse HEAD` before running anything)
- **Oracle manifest:** `context/migration-oracle/symfony-to-spring-vue/manifest.json`, sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — matches the packet.
- **Dimension status: FAIL**, bound to SHA `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`.

## Why FAIL despite 0 red tests

Every test that ran is green (see Commands below: 289/289 backend, 165/165 frontend, 0
failures/errors). The FAIL is a **coverage** miss, not a **correctness** miss: three behaviours in
`behaviours.json` — **B02, B03, B21** — have no `Bnn`-tagged test reachable by the two mandated
unit commands (`./mvnw -q test` and `npm run test -- --run`). The verifier contract for this
dimension is explicit: *"every behaviour in `behaviours.json` for the wave's journeys has a named
test (`@DisplayName("Bnn …")` / `describe("Bnn …")`) … FAIL if one has no test or any test is
red."* Full mapping and evidence: [`unit/behaviour-mapping.md`](unit/behaviour-mapping.md).

B02 and B03 are proven instead at the `integration` dimension (`*ApiIT.java` Failsafe tests +
`frontend/test/integration/*`); B21 only at `e2e` (`tests/e2e/specs/navigation.spec.ts`, per the
oracle's own test reference). This is a deliberate, documented split in the source (see the
Javadoc block on `ShellPagesIT.java` and the header comment on
`frontend/test/integration/ShellNavigation.spec.ts`) — but no entry in
`tools/migration-verify/deviations.json` (DEV-1..DEV-12) or the plan's DEV-13 covers a `unit`
coverage gap; every recorded deviation is for `visual`, `contract`, or `performance`. The unit
dimension's own gate, taken literally as written, is not met for these three behaviours.

## Per-journey verdicts (all 13)

| # | Journey | Verdict | Notes |
| --- | --- | --- | --- |
| 1 | login | parity | B11–B14, B01/B22 (also) all unit-covered, green |
| 2 | landing | parity | B22, B01 (also) unit-covered, green |
| 3 | feeds | parity | B30, B01 (also) unit-covered, green |
| 4 | scheduler-heartbeat | parity | B20, B33 unit-covered, green (incl. new `HeartbeatJobTest`/`HeartbeatTriggerTest` added for B33, which had no prior test per the oracle) |
| 5 | event-stream | parity | B15–B19, B24, B01 (also) unit-covered, green |
| 6 | customers | **regression** | B03 ("detail routes keep their index section active") has no `Bnn`-tagged unit test — only integration (`CustomersApiIT`, `CustomerDetailSidebarSection.spec.ts`). B05, B25, B01 (also) are fine. |
| 7 | automations | parity | B26, B01 (also) unit-covered, green |
| 8 | settings | parity | B06, B32, B01 (also) unit-covered, green |
| 9 | campaigns-email-editor | parity | B27, B28, B01 (also) unit-covered, green |
| 10 | popups-widget-editor | parity | B29, B01 (also) unit-covered, green |
| 11 | import-wizard | parity | B31, B01 (also) unit-covered, green |
| 12 | dashboard | parity | B23, B01 (also) unit-covered, green |
| 13 | shell-navigation | **regression** | B02 ("panel pages carry sidebar/topbar/.main-scroll, aria-current") and B21 (navigation/scroll/reload/locale/breadcrumb) have no `Bnn`-tagged unit test — B02 only at integration, B21 only at e2e. B01, B04, B07–B10 are fine. |

No `accepted-deviation` verdicts: no deviation in `tools/migration-verify/deviations.json` targets
the `unit` dimension for B02/B03/B21.

## Commands run (cwd, exit code)

| Command | cwd | Exit code | Result |
| --- | --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-final-unit/backend` | 0 | 48 Surefire `*Test.java` classes, 289 tests, 0 failures, 0 errors |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-final-unit/frontend` | 0 | 259 packages installed, 0 vulnerabilities |
| `npm run test -- --run` (→ `vitest run test/unit`) | `/home/muszkin/work/kivvi-click-wt/verify-final-unit/frontend` | 0 | 26 test files, 165 tests, all green |

Java: OpenJDK Temurin 25.0.4.1 LTS (`/home/muszkin/.cache/kivvi-toolchains/jdk-25`). Node: v26.8.1 / npm 11.19.0.

## Evidence paths

- [`unit/behaviour-mapping.md`](unit/behaviour-mapping.md) — full B01–B33 → test mapping, the ✅/❌ table and gap rationale.
- [`unit/backend-surefire-reports/`](unit/backend-surefire-reports/) — raw Surefire `*.txt` reports for all 48 `*Test.java` classes (289 tests, 0 failures/errors).
- [`unit/backend-unit-test.log.summary`](unit/backend-unit-test.log.summary) — aggregated `Tests run:` lines + total (289 tests, 0 failures, 0 errors, 48 classes).
- [`unit/backend-unit-test.log.tail`](unit/backend-unit-test.log.tail) — tail of the raw `./mvnw -q test` console output (full log was Spring Boot startup noise per test class; trimmed for size).
- [`unit/backend-displayname-index.txt`](unit/backend-displayname-index.txt) — every `@DisplayName` string extracted from `backend/src/test/java/**/*Test.java`, file-qualified (source for the mapping table).
- [`unit/frontend-unit-test.log`](unit/frontend-unit-test.log) — full `npm run test -- --run` console output (26 files / 165 tests passed).

## Disk

Checkout disk usage before cleanup: `backend/target` 4.1M, `frontend/node_modules` 161M. Both
deleted from `/home/muszkin/work/kivvi-click-wt/verify-final-unit` immediately after evidence was
copied out, per the packet's disk-critical instruction (host `/` was at 2.7–2.9 GB free
throughout this run).
