# Integration dimension — behaviour-to-test mapping (journey `login`, wave-0)

Wave SHA `3b17c07e23354e493edbab572e4090f69d2c0c71`. "Integration-level" here means the two
suites the plan's verifier contract and the J0 packet actually bind to the integration
dimension: backend `backend/src/test/java/click/kivvi/SessionRoundTripIT.java` (Testcontainers
Postgres 18, run only by `mvnw verify` via failsafe's `*IT` binding) and the three files under
`frontend/test/integration/` (`LoginView.spec.ts`, `shellStore.spec.ts`, `Sidebar.spec.ts`, run by
`npm run test:integration`). `*Test.java` classes (WebMvcTest/MockMvc, e.g. `LoginControllerTest`,
`PreferencesControllerTest`, `SpaDocumentControllerTest`) and `frontend/test/unit/*` are bound to
surefire/`mvnw test` — the **unit** dimension's command, per this repo's own split (J0 packet
"Test cycle" row and the plan's Verifier contract table) — and are noted below only for contrast,
not counted as integration evidence.

| Behaviour | Required scope | Integration-level test found | Verdict |
| --- | --- | --- | --- |
| B04 EN locale nav | `/en/...` renders English navigation and page title | None. `ShellViewServiceTest::englishLocaleTranslatesTheNavigation` (backend) constructs `ShellViewService` directly with a `MockHttpSession` — no HTTP layer, no Spring context, no `ShellController`; it is a plain unit test (surefire, not failsafe). `i18n.spec.ts` (backend-equivalent on the frontend) lives in `frontend/test/unit/`, not `test/integration/`, and only diffs the PL/EN key sets — no router, no store, no component. **`ShellController` (`GET /api/v1/{locale}/shell`) has zero test coverage of any kind** — grep of `backend/src/test` for `ShellController`/`MockMvc` touching `/api/v1/en/shell` returns nothing, and `SessionRoundTripIT` only ever calls `/api/v1/pl/shell`. | **No integration-level test — GAP** |
| B07 unsupported locale 404 | `/de/...` → 404 | None in the integration-bound suites. `SpaDocumentControllerTest::unsupportedLocaleIsNotFound` is MockMvc (`*Test`, surefire/unit). `RouteTableTest` is a domain unit test. `routes.spec.ts` (`B07 an unsupported locale prefix is unknown to the router`) lives in `frontend/test/unit/`, not `test/integration/`. | **No integration-level test — GAP** |
| B08 theme persists | POST `/preferences/theme` then a later request reflects it | Backend: `SessionRoundTripIT::preferenceSurvivesASecondRequestViaTheJdbcSession` (`@DisplayName("B08/B10 ...")`) — real HTTP (`TestRestTemplate`) + real Postgres container, posts theme then re-reads `/api/v1/pl/shell`, asserts `theme() == "dark"`. Frontend: `shellStore.spec.ts::"B08 setTheme updates the document attribute and posts the new value"` — asserts store state, DOM attribute and the `fetch` call shape. | **Covered, meaningful** |
| B10 sidebar persists | POST `/preferences/sidebar` then a later request reflects it | Backend: the same `SessionRoundTripIT` test is labelled `B08/B10` but its body only calls `/preferences/theme` — it never exercises `/preferences/sidebar` or re-reads the sidebar field, so the B10 half of that `@DisplayName` is not actually proven at the Testcontainers level (assertion-quality gap, not a missing-test gap). Frontend: `shellStore.spec.ts::"B10 setSidebar updates the store and posts the new value"` genuinely asserts the store/fetch round trip for sidebar. | **Covered on the frontend; backend `@DisplayName` overclaims — note, not a FAIL by itself** |
| B11 valid sign-in | Redirect to dashboard, identity shown | Backend: `SessionRoundTripIT::signInPersistsThroughJdbcIndexedSessionRepository` and `::signInThenSignOutRoundTripsTheIdentity` (both `@DisplayName("B11/B14 ...")`) — real HTTP sign-in, real `spring_session`/`spring_session_attributes` rows, real session-repository read-back, real shell re-fetch showing `anna@aureashop.pl`. Frontend: `LoginView.spec.ts` describe block `"B11 the login view shows the welcome copy and submit action"` (component + router + i18n). | **Covered, meaningful** |
| B12 empty e-mail rejected | 200 + `Podaj adres e-mail.` | Frontend only: `LoginView.spec.ts::"B12 renders the empty-e-mail error with the default address redisplayed, not a blank field"` — mounts the routed component with the server-injected `data-login-error`/`data-last-username` attributes and asserts the rendered message and repopulated field. No backend Testcontainers-level test posts an empty e-mail (only the MockMvc `LoginControllerTest::emptyEmailIsRejected`, unit-dimension). | **Covered on the frontend; backend integration side relies on the unit-dimension MockMvc test — acceptable given this app's native-form-post architecture (see LoginView.spec.ts's own "posts natively to /{locale}/login" test), not a FAIL** |
| B13 malformed e-mail rejected | 200 + `poprawny adres e-mail` | Frontend only, and **unlabeled**: `LoginView.spec.ts::"renders the injected error and carries back the typed address"` sets `data-login-error="To nie wygląda na poprawny adres e-mail."` / `data-last-username="not-an-email"` and asserts both — functionally identical to B13, but the test title carries no `B13` tag, unlike its B12/B11 siblings. Same backend caveat as B12 (only `LoginControllerTest`/`EmailValidationTest`, unit-dimension). | **Functionally covered on the frontend (content matches, assertions meaningful); traceability nit — title should say "B13"** |
| B14 logout restores default identity | POST `/{locale}/logout` → login, default identity | Backend: `SessionRoundTripIT::signInThenSignOutRoundTripsTheIdentity` (`B11/B14`) — signs in, asserts `anna@aureashop.pl`, logs out over real HTTP, re-reads the shell and asserts it falls back to `maciej@aureashop.pl`. | **Covered, meaningful** |

## Verdict driver

Per the verifier packet's FAIL rule ("FAIL if any behaviour lacks an integration-level test..."),
**B04** and **B07** have no integration-level test on either side of the stack — dimension
`integration` is **FAIL** for journey `login` on this basis alone, independent of the fact that
every test that *does* exist is green and Testcontainers genuinely started (see
`backend-mvnw-verify-2.log`, `SessionRoundTripIT` section, real `postgres:18-alpine` container
`d3d5df90a75d5bcc2c82301dbafa181dff05c291ae2cb6b7557142f69a58f42b`).
