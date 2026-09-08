# Unit dimension — behaviour -> executed test map (journey: login), round 3

Wave SHA 887af4543c5a51255735f1ddfc0d8860d11551c0. Checkout
/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit (HEAD confirmed == wave SHA, clean, no
`backend/src/main/resources/static` or `frontend/dist` present before or after the runs below).

Note: this repo pairs each oracle behaviour with a *unit*-slice test (WebMvcTest / plain
JUnit / MockHttpSession, no Spring context boot, no Testcontainers) and a separate
*integration*-slice test (`*IT.java`, full `@SpringBootTest` + Postgres Testcontainers, run by
`mvn verify`/failsafe, NOT by `mvn test`/surefire). This split is intentional and documented in
the source itself (see `ShellApiIT` class javadoc and the `LoginView.spec.ts` unit-spec
comment: "matching every other behaviour row's unit/integration pairing in this slice"). This
map records only the **unit**-slice test per behaviour, since that is this dimension's duty;
the full end-to-end assertion (e.g. "the next document renders data-theme accordingly") is
proven by the sibling `*IT` classes, which are out of scope for `unit` and belong to
`integration`.

| Behaviour | Executed unit test | Assertion is non-tautological (can fail) |
| --- | --- | --- |
| B01 (login/dashboard rows) | `RouteTableTest.everyKnownRouteMatches` (`/pl/login`->`login`, `/pl/dashboard`->`dashboard`, among 21 routes) + `SpaDocumentControllerTest.everyKnownRouteRenders200[...]` (`/pl/login`, `/pl/dashboard` cases of the 21-case `@ValueSource`) | Yes — route-name string equality and HTTP 200 + `<html` body checks; a broken route table or controller mapping fails these |
| B04 | `ShellViewServiceTest.englishLocaleTranslatesTheNavigation` | Yes — checks specific EN nav labels ("Dashboard", "Event stream", ...) and crumb "Event stream" |
| B07 | `RouteTableTest.unsupportedLocaleIsUnknown` + `SpaDocumentControllerTest.unsupportedLocaleIsNotFound` | Yes — `/de/dashboard` must be empty/404 |
| B08 | `PreferencesServiceTest.themeChoicePersists` + `PreferencesControllerTest.themeChoiceIsStoredFromJson` | Yes — session attribute + JSON echo checked against literal `dark` |
| B09 | `PreferencesServiceTest.unknownThemeFallsBackToLight` + `PreferencesControllerTest.unknownThemeFallsBackToLight` | Yes — `sepia` must resolve to `light` |
| B10 | `PreferencesServiceTest.sidebarChoicePersists` + `PreferencesControllerTest.sidebarStateIsStoredFromJson` | Yes — session attribute + JSON echo checked against literal `collapsed` |
| B11 | `LoginControllerTest.successfulSignInRedirectsToTheDashboard` (302 -> `/pl/dashboard`) + `LoginServiceTest.successfulSignInEstablishesTheIdentity` (`identityStore.current(...).email()=="anna@aureashop.pl"`, `.name()=="Anna"`) | Yes — specific redirect target and derived-name logic |
| B12 | `LoginControllerTest.emptyEmailIsRejected` + `LoginServiceTest.emptyEmailIsRejected` (message text + no session created) | Yes — literal Polish message string, and session-nullness check |
| B13 | `LoginControllerTest.malformedEmailIsRejected` + `LoginServiceTest.malformedEmailIsRejected` | Yes — literal Polish message string |
| B14 | `LoginControllerTest.signOutRedirectsToLogin` (302 -> `/pl/login`) + `LoginServiceTest.signOutRestoresTheDefaultIdentity` (identity reverts to `maciej@aureashop.pl` / "Maciej Kowalczyk") | Yes — specific redirect target and default-identity values |
| B22 (login part: browser-side email validation contract) | `frontend/test/unit/LoginView.spec.ts` — 4 `it()` cases: `type=email` on `#f-_username`, `type=password` on `#f-_password`, native POST to `/{locale}/login`, submit button exists | Yes — literal attribute-value assertions on mounted DOM |

## Gap explicitly checked and found NOT to be a gap

B08/B10's oracle text ("...and the next document renders data-theme/data-sidebar
accordingly") is only proven end-to-end by `SessionRoundTripIT` (Testcontainers,
`*IT` suffix). That class is excluded from `mvn test` by the surefire/failsafe naming split
declared in `backend/pom.xml` (confirmed: it is absent from every `target/surefire-reports/*`
produced by the `mvn test` run in this checkout — see `backend-surefire-summary.txt`). This is
the `integration` dimension's evidence, not `unit`'s; the `unit` dimension's own row above
(session-attribute persistence + JSON echo) is what this dimension is required to prove, and it
does, with a real, failable assertion.

## Commands run (this checkout only)

- `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH`
- cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/backend`: `./mvnw -q test` -> exit 0
  (66 tests, 0 failures, 0 errors, 0 skipped across 12 `*Test.java` classes — see
  `backend-surefire-summary.txt`, raw log `backend-mvnw-test.log`)
- cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend`: `npm ci` -> exit 0
- cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit/frontend`: `npm run test -- --run`
  (= `vitest run test/unit --run`) -> exit 0 (6 test files, 36 tests, all passed — raw log
  `frontend-vitest-unit.log`)
