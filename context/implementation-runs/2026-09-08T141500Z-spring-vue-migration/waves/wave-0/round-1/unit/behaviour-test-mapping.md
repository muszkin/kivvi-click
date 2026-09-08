# unit dimension — journey `login` — behaviour→test mapping

Wave SHA `3b17c07e23354e493edbab572e4090f69d2c0c71`. Checkout
`/home/muszkin/work/kivvi-click-wt/verify-wave-0-unit` (HEAD verified equal to wave SHA).

Commands actually run (verbatim from the plan's Verifier contract):
- `cd backend && ./mvnw -q test` — exit 1 (BUILD FAILURE). Full run: 59 tests, 27 errors, 0 failures, 0 skipped.
- `cd frontend && npm ci && npm run test -- --run` — exit 0. `test` = `vitest run test/unit`
  (package.json), so this executes only `frontend/test/unit/*`, not `frontend/test/integration/*`.
  5 files, 32 tests, all passed.

| Behaviour | Text | Test(s) | Result | Tautology check |
| --- | --- | --- | --- | --- |
| B01 (login row) | Every panel/public URL renders 200 and shows its own headline/marker text | `SpaDocumentControllerTest.everyKnownRouteRenders200("/pl/login")` (backend); `routes.spec.ts` `%s matches a named route` for `/pl/login` (frontend) | backend **RED** (context load failure, see below); frontend PASS | Backend asserts exact `status().isOk()` + body contains `<html`; not tautological, but only proves a generic SPA shell renders, not a login-specific headline — that assertion lives in `test/integration/LoginView.spec.ts` (out of the unit command's scope, see B22 note). Frontend asserts a named route resolves — real, can fail. |
| B01 (dashboard row) | same, for `/pl/dashboard` | `SpaDocumentControllerTest.everyKnownRouteRenders200("/pl/dashboard")` (backend); `routes.spec.ts` for `/pl/dashboard` (frontend) | backend **RED**; frontend PASS | Same caveat as above; additionally no `test/unit/*` file mounts `DashboardView.vue`/`PageHead.vue` to assert the rendered headline — `i18n.spec.ts` only proves the translation string exists, not that `DashboardView` binds it. Coverage gap, not a tautology. |
| B04 | `/en/...` renders English navigation and page title | `ShellViewServiceTest.englishLocaleTranslatesTheNavigation` (backend); `i18n.spec.ts` "translates every nav.* label" (frontend) | both PASS | Both assert concrete translated strings (`"Dashboard"`, `"Event stream"`, …); can fail on a wrong key. Not tautological. |
| B07 | Unsupported locale prefix → 404 | `SpaDocumentControllerTest.unsupportedLocaleIsNotFound` (backend); `routes.spec.ts` "does not match /de/dashboard" (frontend) | backend **RED**; frontend PASS | Frontend asserts `matched.length === 0`; real. |
| B08 | POST /preferences/theme persists and next render carries data-theme | `PreferencesControllerTest.themeChoiceIsStoredFromJson` | PASS | Real value assertion, not part of the broken bean graph. |
| B09 | Unknown theme falls back to light | `PreferencesControllerTest.unknownThemeFallsBackToLight` | PASS | Real. |
| B10 | POST /preferences/sidebar persists and shell renders data-sidebar | `PreferencesControllerTest.sidebarStateIsStoredFromJson` | PASS | Real. |
| B11 | Valid e-mail sign-in redirects to dashboard and shows the e-mail in the sidebar footer | `LoginControllerTest.successfulSignInRedirectsToTheDashboard` (redirect half, backend); `LoginServiceTest.successfulSignInEstablishesTheIdentity` (identity/sidebar-footer half, backend) | Controller test **RED**; service test PASS | Service test asserts exact email/derived name; real. Controller test asserts exact 302 + `Location` header; real, but currently cannot run. |
| B12 | Empty e-mail rejected with 'Podaj adres e-mail.' | `LoginControllerTest.emptyEmailIsRejected` (backend, HTTP-level); `LoginServiceTest.emptyEmailIsRejected` (backend, service-level) | Controller test **RED**; service test PASS | Both assert the literal Polish message; real. |
| B13 | Malformed e-mail rejected with 'poprawny adres e-mail' | `LoginControllerTest.malformedEmailIsRejected`; `LoginServiceTest.malformedEmailIsRejected` | Controller test **RED**; service test PASS | Real, literal message assertions. |
| B14 | Logout redirects to login and restores default identity | `LoginControllerTest.signOutRedirectsToLogin` (redirect half); `LoginServiceTest.signOutRestoresTheDefaultIdentity` (identity half) | Controller test **RED**; service test PASS | Real; service test asserts the exact default identity (`maciej@aureashop.pl`, `Maciej Kowalczyk`) is restored. |
| B22 (login part) | sign-in shows identity, browser blocks malformed e-mail | `frontend/test/integration/LoginView.spec.ts` describe blocks `"B22 the login form matches…"` and `"B11 the login view shows…"` | **Not exercised by the unit command** | Named, non-tautological tests exist (assert `type="email"`, exact h1 text, injected error text) but live under `test/integration/`, which the plan's own verifier contract routes to `npm run test:integration` (the integration verifier), not `npm run test -- --run`. Flagged, not counted as a unit-dimension pass. |

## Root cause of the RED backend tests

`LoginControllerTest` (4/4 errors) and `SpaDocumentControllerTest` (23/23 errors) both `@Import`
`click.kivvi.infrastructure.IndexHtmlTemplate`, whose constructor eagerly reads
`classpath:static/index.html`:

```
Caused by: java.io.UncheckedIOException: frontend/dist/index.html was not copied into
backend/src/main/resources/static before this jar was built — see backend/README.md
Caused by: java.io.FileNotFoundException: class path resource [static/index.html] cannot be
opened because it does not exist
```

`backend/src/main/resources/static` does not exist in this checkout, and nothing in `pom.xml`
(no `frontend-maven-plugin`/`exec-maven-plugin` binding) or in `src/test/resources` (no stub
`static/index.html`) supplies it before `test` runs. `backend/README.md`'s "Build" section
documents the manual `npm run build && cp -r frontend/dist/. backend/src/main/resources/static/`
step, but the "Test" section's "Focused (no containers): `./mvnw test`" bullet does not mention
it, and the plan's Verifier contract command for the `unit` dimension (`cd backend && ./mvnw -q
test`) does not include it either — so the command this dimension is contracted to run is not
self-contained and reliably fails from a clean checkout. `PreferencesControllerTest` does not
`@Import` `IndexHtmlTemplate`/`SpaDocumentService`, which is why it alone among the `@WebMvcTest`
classes passed.
