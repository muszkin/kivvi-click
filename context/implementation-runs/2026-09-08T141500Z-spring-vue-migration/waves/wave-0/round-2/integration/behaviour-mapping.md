# Integration dimension — behaviour → test mapping (journey login, round 2)

Wave SHA `6a53642c9c5ec9c256625854e6670f035e0292be`. Checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int`.
"Integration-level" here = a real Spring context booted via `@SpringBootTest`, with a real
Testcontainers Postgres 18 datasource, exercised through the real HTTP layer (`TestRestTemplate`) —
matching the source oracle's `WebTestCase`-based functional tests. A `@WebMvcTest`-sliced test
(partial context, mocked collaborators) is unit-dimension, not integration-dimension; this
distinction is stated explicitly in-repo in `ShellApiIT`'s class javadoc, written to close the
exact same B04/B07 gap round-1 failed on.

| B-id | Behaviour (oracle) | Integration-level test found | Concrete/oracle assertions | Verdict |
| --- | --- | --- | --- | --- |
| B04 | EN locale nav renders English navigation/title, through HTTP | `ShellApiIT.englishShellReturnsEnglishLabels` / `.polishShellReturnsPolishLabels` | full nav-label lists asserted per locale (`Dashboard`/`Pulpit` etc.), `locale` field | covered |
| B07 | Unsupported locale prefix -> 404, through HTTP | `ShellApiIT.unsupportedLocaleIsNotFound` (`/de/dashboard`), `.unknownPathIsNotFound` (`/pl/nonexistent`) | HTTP 404 status | covered |
| B08 | POST /preferences/theme persists; next document/API render honours it | `SessionRoundTripIT.themePreferenceSurvivesASecondRequestViaTheJdbcSession` | `SESSION=` cookie issued, second-request `shell.theme() == "dark"` | covered |
| B10 | POST /preferences/sidebar persists; shell + SPA document honour it | `SessionRoundTripIT.sidebarPreferenceRoundTripsThroughShellAndSpaDocument` | `sidebar.state()=="collapsed"`, `shell.sidebar()=="collapsed"`, document contains `data-sidebar="collapsed"` | covered |
| B11 | Valid e-mail sign-in redirects to dashboard, shows e-mail in sidebar footer | `SessionRoundTripIT.signInThenSignOutRoundTripsTheIdentity` | 302, `Location: /pl/dashboard`, post-login shell `user().email()=="anna@aureashop.pl"` | covered |
| B12 | Empty e-mail rejected with "Podaj adres e-mail." | none at integration level | `LoginControllerTest` (`@WebMvcTest`, unit-dimension slice) has it; frontend `LoginView.spec.ts` "B12 …" test only replays a *pre-set* `data-login-error` attribute, it never calls `/pl/login` | **gap** |
| B13 | Malformed e-mail rejected with "…poprawny adres e-mail…" | none at integration level, and no test anywhere is named/tagged B13 for the HTTP path | `LoginControllerTest` (unit-dimension slice) and `LoginServiceTest`/`EmailValidationTest` (pure unit) cover it; no `*IT.java` exercises `POST /pl/login` with a malformed address | **gap** |
| B14 | Logout redirects to login, restores default identity | `SessionRoundTripIT.signInThenSignOutRoundTripsTheIdentity` | POST `/pl/logout`, subsequent shell `user().email()=="maciej@aureashop.pl"` (default identity) | covered |

## Overclaim check

`SessionRoundTripIT.signInPersistsThroughJdbcIndexedSessionRepository` is `@DisplayName`-tagged
"B11/B14" but only performs sign-in (no logout) — it exercises the session/attribute-persistence
half of B11, not B14's logout/restore-default-identity behaviour. This is a minor naming overclaim;
it is not fatal on its own because `signInThenSignOutRoundTripsTheIdentity` (same class) separately
and correctly exercises the full B11 and B14 behaviours with concrete oracle values. No other
integration-level test name was found to overclaim a B-id it doesn't exercise.

## Verdict driver

B12 and B13 are validated at the oracle (Symfony `SecurityControllerTest`, itself an HTTP-level
functional test) but in this SHA have no equivalent full-context, real-HTTP Spring test —
only a `@WebMvcTest` slice (unit dimension) and, for B12 only, a frontend component test that
fakes the server-emitted attributes rather than obtaining them from a real `POST /pl/login`.
Per the packet's explicit rule ("FAIL if any behaviour lacks an integration-level test"), this
is a coverage gap, independent of the fact that every test that does exist is green.
