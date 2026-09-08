# Wave-1 verifier — dimension `unit` — round 2

Wave SHA: `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a` · Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-1-unit` (HEAD confirmed at wave SHA, clean, detached; `backend/src/main/resources/static` and `frontend/dist` absent, as expected). Oracle manifest sha256 confirmed `4945a8de...0a90f74f`. Plan sha256 confirmed `044bb247...850c757e4`.

## Dimension status: PASS

## Commands run

| Command | Cwd | Exit code | Result |
| --- | --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25 && ./mvnw test` | `backend/` | 0 | 86 tests, 0 failures, 0 errors |
| `npm ci` | `frontend/` | 0 | clean install, 0 vulnerabilities |
| `npm run test -- --run` (`vitest run test/unit --run`) | `frontend/` | 0 | 9 files, 51 tests, 0 failures |

Evidence: `unit/backend-mvnw-test.log`, `unit/frontend-npm-test.log` (verbose reporter, one line per test).

Failsafe-suffixed `*IT.java` classes (FeedsApiIT, LandingApiIT, SessionRoundTripIT, ShellApiIT, HeartbeatSchedulerIT) correctly did NOT run under `mvnw test` (confirmed via `grep "Running click.kivvi"` on the log) — Surefire/Failsafe split is real, not just documented in the pom comment.

## Behaviour → test mapping

| Behaviour | Journey | Mapped test(s) | Tag | Verdict |
| --- | --- | --- | --- | --- |
| B22 (landing part) | landing | `LandingFixturesTest` (5), `LandingControllerTest` (4) [backend]; `test/unit/landing.spec.ts` (9) [frontend] | `@DisplayName("B22 ...")` / `describe("B22 ...")` | parity |
| B01 (landing row) | landing | `RouteTableTest.everyKnownRouteMatches`, `SpaDocumentControllerTest.everyKnownRouteRenders200` (incl. `/`, `/pl`) [backend]; `routes.spec.ts` (incl. `/`, `/pl`) [frontend]; headline text asserted by `landing.spec.ts` (`.hero h1` contains "Widzisz") | `B01` | parity |
| B01 (feeds row) | feeds | same `RouteTableTest`/`SpaDocumentControllerTest`/`routes.spec.ts` parametrized cases for `/pl/feeds` | `B01` | parity, with a note below |
| B30 | feeds | `FeedsViewServiceTest` (4), `FeedsControllerTest` (2) [backend]; `test/unit/FeedCard.spec.ts` (4) [frontend] | `B30` | parity |
| B20 | scheduler-heartbeat | `HeartbeatTriggerTest.firstEverTickFiresImmediately` (`@DisplayName("B20/B33 ...")`) | `B20` | parity |
| B33 | scheduler-heartbeat | `HeartbeatJobTest.tickLogsTheHeartbeatMessageAtInfo`, `HeartbeatTriggerTest` (3 more cases) | `B33` | parity |

All mapped tests are non-tautological: they exercise real production code (`RouteTable.match`, `FeedsViewService.build`, `HeartbeatTrigger.nextExecution`, `HeartbeatJob.tick`, rendered Vue templates) against independently-authored expected values, not the same literal the test also feeds in. Each could fail under a plausible regression (e.g. a dropped route, a wrong tuple, a changed catch-up formula, a missing space in markup).

**Note on B01 (feeds row):** the "renders 200" / "route is known" half of the behaviour is unit-tested for `/pl/feeds` in both backend and frontend. The "shows its own headline/marker text" half is proven for **feeds** only by `test/integration/FeedsView.spec.ts` (`renders the page title` → `.page-title` == `"Feedy produktów"`), which runs under `npm run test:integration`, not the unit command in scope here — architecturally the SPA shell carries no per-route markup server-side, so the marker-text proof for feeds lives one layer up from the landing case (which got a full-view unit mount in `landing.spec.ts`). B01 still carries an executed, non-tautological, taggable unit test for both rows per the literal Verifier-contract threshold ("every behaviour ... has a named test"), so this is recorded as a coverage asymmetry, not a FAIL trigger.

## Wave-0 unit suites still pass

Confirmed green in the same run: `LoginServiceTest`, `PreferencesServiceTest`, `ShellViewServiceTest`, `IdentityTest`, `EmailValidationTest`, `SpaDocumentTest`, `ArchitectureTest`, `FailOnWarnLogExtensionTest` (backend); `Avatar.spec.ts`, `Icon.spec.ts`, `WorkspaceCard.spec.ts`, `i18n.spec.ts`, `LoginView.spec.ts` (frontend). No regressions.

## Per-journey verdict

| Journey | Verdict | Deviation ID |
| --- | --- | --- |
| landing | parity | — |
| feeds | parity | — |
| scheduler-heartbeat | parity | — |

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/unit/backend-mvnw-test.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/unit/frontend-npm-test.log`
- This file: `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-1/unit.md`

round-1/ evidence untouched.
