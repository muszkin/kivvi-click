# Verifier evidence — wave-1, dimension `unit`

Wave SHA: `9427fdb1d92e4e606e67471638031d1bc0fb73d4`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-unit` (detached, HEAD confirmed == wave SHA; `backend/src/main/resources/static` and `frontend/dist` confirmed absent before and after the run; no files edited, no commits made).

## Status: PASS

## Commands run

| # | Command | cwd | Exit code | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw test` | `backend/` | 0 | `unit/backend-mvnw-test.log` — 86 tests, 0 failures, 0 errors, BUILD SUCCESS |
| 2 | `npm ci` | `frontend/` | 0 | (not persisted — routine dependency install, 259 packages, 0 vulnerabilities) |
| 3 | `npm run test -- --run` | `frontend/` | 0 | `unit/frontend-npm-test.log` — 9 test files, 51 tests, all passed |
| 4 | `npx vitest run test/unit --reporter=verbose` (same suite, verbose reporter to capture per-test names for the mapping below) | `frontend/` | 0 | `unit/frontend-vitest-verbose.log` |

## Behaviour → named test mapping

| Behaviour | Journey | Backend unit test(s) (`./mvnw test`) | Frontend unit test(s) (`npm run test -- --run`) |
| --- | --- | --- | --- |
| B22 (landing part) | landing | `LandingControllerTest` (4 `@DisplayName("B22 …")` cases: `/api/v1/pl/landing`, `/en/landing`, unsupported locale 404, demo redirect); `LandingFixturesTest` (5 `@DisplayName("B22 …")` cases: features, steps, plans, preview tiles, sparkline) | `test/unit/landing.spec.ts` → `describe("B22 the landing page structure (unit)")`, 9 `it(...)` cases: hero heading/CTAs, preview KPIs+sparkline, features/steps counts, price cards, EN variant, KPI/price/trust-point spacing |
| B01 (landing, feeds rows) | landing, feeds | `RouteTableTest::everyKnownRouteMatches` (`@DisplayName("B01 …")`, asserts `/pl`→home and `/pl/feeds`→feeds among 20 routes); `SpaDocumentControllerTest::everyKnownRouteRenders200` (`@DisplayName("B01 …")`, 21-value `@ParameterizedTest` incl. `/pl` and `/pl/feeds`, asserts 200 + SPA document) | `test/unit/routes.spec.ts` → `describe("B01 every panel/public route in the table resolves")`, `it.each` over 21 paths incl. `/pl` and `/pl/feeds`, asserts a named route resolves |
| B30 | feeds | `FeedsControllerTest::feedsPayloadShape` (`@DisplayName("B30 …")`, asserts 4 sources/4 feeds/1 error feed HTTP 503/4 coverage bars/142 mismatched); `FeedsViewServiceTest` (4 `@DisplayName("B30 …")` cases: KPIs, sources, feeds incl. the 503 row, coverage/fallback/mismatched) | `test/unit/FeedCard.spec.ts`, 3 `it("B30 …")` cases: HTTP 503 error strip, synced mapped-%, syncing live chip |
| B20 | scheduler-heartbeat | `HeartbeatTriggerTest::firstEverTickFiresImmediately` (`@DisplayName("B20/B33 …")`) plus the trigger's Postgres-lock-history-driven missed-run logic exercised by the other 3 cases in the same class | — (backend-only journey) |
| B33 | scheduler-heartbeat | `HeartbeatJobTest::tickLogsTheHeartbeatMessageAtInfo` (`@DisplayName("B33 …")`, asserts the exact log line via a Logback `ListAppender`); `HeartbeatTriggerTest` (3 further `@DisplayName("B33 …")` cases: restart-shortly-after, restart-after-downtime catch-up, anchor-on-own-completion) | — (backend-only journey) |

All listed tests were confirmed present and green in the command output (grep of `Running click.kivvi…` / `Tests run:` lines in the backend log; verbose `✓` lines in the frontend log).

Note: `FeedsApiIT` and `HeartbeatSchedulerIT` (Testcontainers, `*IT` naming) correctly did **not** run under `./mvnw test` (Surefire/Failsafe split confirmed via `pom.xml`) — they are `integration`-dimension evidence, not double-counted here. B20's full Postgres round-trip proof is likewise integration-dimension; the unit dimension's B20 test covers the trigger's lock-history-driven scheduling logic in isolation, which is what plan section J3 commits to at the unit level ("unit with a mutable Clock; integration with Testcontainers").

Tautology check: inspected assertions for all mapped tests — each compares a real computed/rendered value (JSON payload field, DOM text/count, log line, `Instant` computed by `HeartbeatTrigger`) against a literal expected value; none assert a value against itself or a constant with no dependency on production code. No tautologies found.

## Journey verdicts

| Journey | Verdict |
| --- | --- |
| landing | parity |
| feeds | parity |
| scheduler-heartbeat | parity |

## Regression (wave-0 unit suites)

Wave-0 (login/shell walking skeleton) unit tests ran in the same suite and are green: backend `LoginControllerTest` (4), `PreferencesControllerTest` (4), `LoginServiceTest` (6), `PreferencesServiceTest` (4), `ShellViewServiceTest` (4), `ArchitectureTest` (3), `EmailValidationTest` (3), `SpaDocumentTest` (3), `IdentityTest` (2), `FailOnWarnLogExtensionTest` (7); frontend `test/unit/LoginView.spec.ts` (B22 login part, 4 cases) and `test/unit/i18n.spec.ts` (B04, 3 cases), plus `Avatar`, `Icon`, `KpiGrid`, `WorkspaceCard` component specs — all passed. No regression.

## Totals

Backend: 86/86 tests passed. Frontend: 51/51 tests passed (9/9 files). 0 failures, 0 errors, 0 skipped across both commands.
