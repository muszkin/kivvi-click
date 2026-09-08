# Unit dimension — behaviour → test mapping, journey `login` (round 2, wave SHA 6a53642c9c5ec9c256625854e6670f035e0292be)

Unit-dimension test surface = exactly what the mandated commands execute:
`cd backend && ./mvnw -q test` (Surefire default include `**/*Test.java`, excludes `**/*IT.java`)
and `cd frontend && npm run test -- --run` (`vitest run test/unit`, excludes `test/integration`).

| Behaviour | Oracle description | New-stack test(s) actually run by the unit command | Result | Tautology check |
| --- | --- | --- | --- | --- |
| B01 (login/dashboard rows) | every panel/public URL renders 200 + marker | `SpaDocumentControllerTest.everyKnownRouteRenders200` (25 cases, incl. `/pl/login`, `/en/dashboard`) + `routes.spec.ts` `describe("B01 …")` (router resolves every path incl. `/pl/login`) | green | real: asserts `status 200` + `<html` body / `resolved.matched.length > 0`; fails if route table drops an entry |
| B04 | `/en/...` renders English nav + title | `ShellViewServiceTest.englishLocaleTranslatesTheNavigation` + `i18n.spec.ts` `describe("B04 …")` | green | real: asserts translated label strings, not identity checks |
| B07 | unsupported locale → 404 | `SpaDocumentControllerTest.unsupportedLocaleIsNotFound` + `routes.spec.ts` `describe("B07 …")` | green | real: asserts 404 / empty match |
| B08 | POST /preferences/theme persists, next document reflects it | `PreferencesServiceTest.themeChoicePersists` + `PreferencesControllerTest.themeChoiceIsStoredFromJson` | green | real: `PreferencesServiceTest` re-reads via a **second, independently constructed** `SessionPreferencesStore` off the same `MockHttpSession`, so it fails unless state is genuinely written to the session, not a field |
| B09 | unknown theme falls back to light | `PreferencesServiceTest.unknownThemeFallsBackToLight` + `PreferencesControllerTest.unknownThemeFallsBackToLight` | green | real |
| B10 | POST /preferences/sidebar persists, shell reflects it | `PreferencesServiceTest.sidebarChoicePersists` + `PreferencesControllerTest.sidebarStateIsStoredFromJson` | green | real (same independent-store pattern as B08) |
| B11 | valid sign-in → dashboard, identity in sidebar | `LoginControllerTest.successfulSignInRedirectsToTheDashboard` + `LoginServiceTest.successfulSignInEstablishesTheIdentity` | green | real: identity re-read through a **fresh** `SessionIdentityStore` instance against the mutated `HttpSession`/`MockHttpServletRequest` |
| B12 | empty e-mail rejected, "Podaj adres e-mail." | `LoginControllerTest.emptyEmailIsRejected`, `LoginServiceTest.emptyEmailIsRejected`, `EmailValidationTest.emptyEmailIsRejected` | green | real |
| B13 | malformed e-mail rejected, "poprawny adres e-mail" | `LoginControllerTest.malformedEmailIsRejected`, `LoginServiceTest.malformedEmailIsRejected`, `EmailValidationTest.malformedEmailIsRejected` | green | real |
| B14 | logout → login, restores default identity | `LoginControllerTest.signOutRedirectsToLogin` + `LoginServiceTest.signOutRestoresTheDefaultIdentity` | green | real: re-reads identity through a fresh store instance after `signOut` |
| B22 (login part: "browser blocks malformed e-mail") | native `type=email` blocks bad input before submit | **none in the unit surface.** The only named test, `describe("B22 the login form matches the browser-validated, server-rendered contract", …)`, lives in `frontend/test/integration/LoginView.spec.ts` — excluded by `npm run test -- --run` (confirmed: the run executed exactly 5 files / 32 tests — `Avatar`, `i18n`, `Icon`, `routes`, `WorkspaceCard` — `LoginView.spec.ts` is not among them). It is only reachable through `npm run test:integration`, the **integration** dimension's command. | **GAP** | n/a — no unit-dimension test exists to inspect |

## Command evidence

- `cd backend && ./mvnw -q test` → exit 0. 12 `*Test.java` classes, 66 tests, 0 failures/errors/skipped (`backend-mvnw-test.log`, Surefire reports under the checkout's own `backend/target/surefire-reports/`, not copied here as they are build output of the checkout). `SessionRoundTripIT.java` and `ShellApiIT.java` (both carry `B08`/`B10`/`B11`/`B14`/`B04`/`B07` labels) correctly did NOT run — Surefire's default `*Test.java` pattern excludes `*IT.java`; those are the integration dimension's evidence.
- `cd frontend && npm ci` → exit 0 (`frontend-npm-ci.log`).
- `cd frontend && npm run test -- --run` → exit 0. `Test Files 5 passed (5)`, `Tests 32 passed (32)` (`frontend-npm-test.log`). `test/integration/*` (LoginView.spec.ts, shellStore.spec.ts, Sidebar.spec.ts) not executed — confirms the unit/integration split is enforced by the npm script's positional path, not just convention.

## Fail-on-warning test policy (unit-dimension fact, not an architecture verdict)

- Backend: `backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` registers `click.kivvi.testsupport.FailOnWarnLogExtension`; `junit-platform.properties` sets `junit.jupiter.extensions.autodetection.enabled=true`. Auto-detected extensions attach to every test in the module, including all 12 classes run above — active, not opt-in per class.
- Frontend: `frontend/vite.config.ts` `test.setupFiles = ["./test/setup.ts"]` is a global Vitest option, applied to every test file the run touches, including the 5 files executed by `npm run test -- --run`. `test/setup.ts` throws in `afterEach` on any first-party `console.warn`/`console.error`/Vue runtime warning, and `dangerouslyIgnoreUnhandledErrors` stays `false`. Active for this run.

## Conclusion

11 of the 12 checked behaviour rows have a real, non-tautological, green unit-dimension test. B22's login-relevant assertion ("browser blocks malformed e-mail") has no test reachable by the unit dimension's mandated command — its only test is integration-scoped. Per the FAIL rule ("FAIL if any behaviour lacks a test"), this is a gap for `unit`.
