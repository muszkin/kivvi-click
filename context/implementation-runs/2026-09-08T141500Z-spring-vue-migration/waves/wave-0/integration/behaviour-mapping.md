# Integration dimension — behaviour → test mapping (round 3, wave-0, journey `login`)

Wave SHA `887af4543c5a51255735f1ddfc0d8860d11551c0`. Checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int` (HEAD confirmed == wave SHA).

All eight required behaviours map to `*IT` classes that boot a real Spring context (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), talk over real HTTP via `TestRestTemplate`, and back sessions with a real Testcontainers Postgres 18 (`postgres:18-alpine`, confirmed by Flyway log `Database: jdbc:postgresql://localhost:.../test ... (PostgreSQL 18.6)` in both classes). No `@WebMvcTest` slice is counted as integration evidence.

| B-id | Behaviour (oracle) | Test class | Test method | DisplayName cites | Oracle-value assertion |
| --- | --- | --- | --- | --- | --- |
| B04 | GET .../shell returns localized navigation | `ShellApiIT` | `englishShellReturnsEnglishLabels` | B04 | exact English label list (`Dashboard`, `Event stream`, … `Settings`) |
| B04 | (pl) | `ShellApiIT` | `polishShellReturnsPolishLabels` | B04 | exact Polish label list (`Pulpit`, `Strumień zdarzeń`, … `Ustawienia`) |
| B07 | Unsupported locale prefix → 404 | `ShellApiIT` | `unsupportedLocaleIsNotFound` | B07 | `GET /de/dashboard` → status 404 |
| B07 | (unknown path) | `ShellApiIT` | `unknownPathIsNotFound` | B07 | `GET /pl/nonexistent` → status 404 |
| B08 | POST /preferences/theme persists, reflected in shell | `SessionRoundTripIT` | `themePreferenceSurvivesASecondRequestViaTheJdbcSession` | B08 | `shell.theme() == "dark"` on a second request via the SESSION cookie |
| B10 | POST /preferences/sidebar persists, reflected in shell + SPA document | `SessionRoundTripIT` | `sidebarPreferenceRoundTripsThroughShellAndSpaDocument` | B10 | `shell.sidebar() == "collapsed"`; SPA document body contains `data-sidebar="collapsed"` |
| B11 | Valid sign-in redirects to dashboard, sidebar shows the e-mail | `SessionRoundTripIT` | `signInPersistsThroughJdbcIndexedSessionRepository` | B11 | 302; a `spring_session` row and a `panel.identity` attribute row exist for the session id (SQL `count(*) = 1`); `JdbcIndexedSessionRepository.findById` returns `panel.identity == "anna@aureashop.pl"` |
| B11 | (redirect + identity through HTTP) | `SessionRoundTripIT` | `signInThenSignOutRoundTripsTheIdentity` | B11/B14 | 302 with `Location: /pl/dashboard`; `shell.user().email() == "anna@aureashop.pl"` after login |
| B12 | Empty e-mail rejected with `"Podaj adres e-mail."` | `ShellApiIT` | `emptyEmailIsRejectedThroughTheRealHttpLayer` | B12 | response body contains `data-login-error="Podaj adres e-mail."` (exact PL string) and `data-last-username="maciej@aureashop.pl"`; shell identity afterwards still the default (`maciej@aureashop.pl`) |
| B13 | Malformed e-mail rejected with `"To nie wygląda na poprawny adres e-mail."` | `ShellApiIT` | `malformedEmailIsRejectedThroughTheRealHttpLayer` | B13 | exact PL message string in `data-login-error`; identity unchanged afterwards |
| B14 | Logout restores the default identity | `SessionRoundTripIT` | `signInThenSignOutRoundTripsTheIdentity` | B11/B14 | after `POST /pl/logout`, `shell.user().email() == "maciej@aureashop.pl"` |

Result: 11/11 `*IT` tests green (`SessionRoundTripIT`: 4, `ShellApiIT`: 7 — see `failsafe-reports/failsafe-summary.xml`), each behaviour has at least one real-HTTP/Testcontainers test, and every assertion above is a concrete oracle value (exact status code, exact Polish/English strings, exact e-mail addresses, exact DB row counts) rather than a shape/type check — each would fail for the defect it targets (e.g. B11's `spring_session`/`JdbcIndexedSessionRepository` assertions fail if the app falls back to Tomcat's in-memory session, per the class Javadoc's own repair-1 note).

## B-id citation audit

Checked every `@DisplayName`/`describe` string in the new stack's `*IT` and frontend `test/integration/*.spec.ts` files for a B-id that the test body does not actually exercise.

- Backend `*IT` classes: no mismatch. `SessionRoundTripIT` carries an in-code comment (repair-3, R3-A) documenting that an earlier round's `signInPersistsThroughJdbcIndexedSessionRepository` DisplayName over-cited B14 without calling `/logout`; the current DisplayName on that method cites only B11, and B14 is exercised by `signInThenSignOutRoundTripsTheIdentity` instead, which does call `/pl/logout`. No live citation problem.
- Frontend `test/integration/LoginView.spec.ts`, `describe("B11 the login view shows the welcome copy and submit action", …)`: the two tests inside only assert the rendered `<h1>` heading text and submit button label for `/pl/login` and `/en/login` — they never submit the form, follow a redirect, or touch the sidebar footer, so they do not exercise the B11 behaviour (valid sign-in → dashboard redirect → sidebar shows the signed-in e-mail) as described in the oracle. This is a mislabeled **frontend** describe block, not a backend integration gap: the real B11 behaviour already has concrete real-HTTP coverage in `SessionRoundTripIT` (above), so this does not leave B11 without a qualifying test and is not FAIL-triggering under the packet's three conditions, but it is flagged here as an over-citation for the delivery record.
- Frontend `test/integration/shellStore.spec.ts` `describe("B08/B10 shell store preference toggles", …)`: tests do call `shell.setTheme`/`shell.setSidebar` and assert the `fetch` call and resulting store/DOM state, which is a fair (if fetch-mocked, not real-HTTP) exercise of the B08/B10 client-side behaviour; no over-citation.
