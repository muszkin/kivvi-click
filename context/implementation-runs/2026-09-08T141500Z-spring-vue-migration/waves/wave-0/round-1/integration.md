# Verifier summary — dimension `integration`, wave-0

- **Wave SHA:** `3b17c07e23354e493edbab572e4090f69d2c0c71` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int`, detached HEAD confirmed via `git rev-parse HEAD`, working tree clean, not edited).
- **Oracle manifest:** sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — matches the packet's recorded value (`sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json`).
- **Dimension status: FAIL**
- **Journey `login`: regression** — behaviours B04 (EN locale nav) and B07 (unsupported-locale 404) have no integration-level test on either the backend Testcontainers suite or the frontend `test/integration/` suite. Every test that does exist is green, but the FAIL rule ("FAIL if any behaviour lacks an integration-level test, any test is red, or the Testcontainers tests were skipped") is triggered by the missing coverage, not by a red test.

## Commands run (this checkout only)

| Command | cwd | Exit code | Notes |
| --- | --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `backend/` | 1 (first attempt) | Failed: `IndexHtmlTemplate` — `frontend/dist/index.html was not copied into backend/src/main/resources/static before this jar was built`. This is a documented build prerequisite (`backend/README.md` § Build), not a code defect — `KivviApplication`'s context (loaded by every `@SpringBootTest`/`@WebMvcTest`) instantiates `IndexHtmlTemplate` eagerly. Full log: `waves/wave-0/integration/backend-mvnw-verify.log`. |
| `npm ci && npm run build` | `frontend/` | 0 | Produced `frontend/dist/` (vite 8.2.2, vue-tsc clean). |
| `rm -rf backend/src/main/resources/static && mkdir -p ... && cp -r frontend/dist/. backend/src/main/resources/static/` | repo root | 0 | Per `backend/README.md` § Build, exactly the step `backend/Dockerfile`'s multi-stage build performs automatically. Build output only — no tracked file touched. |
| `export JAVA_HOME=...; ./mvnw -q verify` (retry) | `backend/` | **0** | BUILD SUCCESS. Surefire: 11 test classes, all green (52 tests). Failsafe: `SessionRoundTripIT` — 3/3 green, against a real `postgres:18-alpine` Testcontainer (id `d3d5df90a75d5bcc2c82301dbafa181dff05c291ae2cb6b7557142f69a58f42b`, started in PT1.36S; Ryuk reaper active). Full log: `waves/wave-0/integration/backend-mvnw-verify-2.log`. |
| `npm run test:integration -- --run` | `frontend/` | **0** | Vitest: 3 files / 13 tests, all green (`LoginView.spec.ts`, `shellStore.spec.ts`, `Sidebar.spec.ts`). Log: `waves/wave-0/integration/frontend-test-integration.log`. |

## Testcontainers confirmation (from `backend-mvnw-verify-2.log`)

```
17:20:36.024 ... Testcontainers version: 2.0.5
17:20:36.469 tc.testcontainers/ryuk:0.14.0 -- Creating container for image: testcontainers/ryuk:0.14.0
17:20:36.943 tc.postgres:18-alpine -- Creating container for image: postgres:18-alpine
17:20:38.302 tc.postgres:18-alpine -- Container postgres:18-alpine started in PT1.357912996S
17:20:38.303 tc.postgres:18-alpine -- Container is started (JDBC URL: jdbc:postgresql://localhost:33001/test...)
```
`target/failsafe-reports/click.kivvi.SessionRoundTripIT.txt`: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`. Its three tests assert real rows in `spring_session`/`spring_session_attributes` (queried via `JdbcTemplate` against the container) and a successful read-back through `JdbcIndexedSessionRepository` — not skipped, not mocked.

## Behaviour mapping (journey `login`)

Full mapping and rationale: `waves/wave-0/integration/behaviour-mapping.md`. Summary:

| Behaviour | Integration-level test | Result |
| --- | --- | --- |
| B04 EN locale nav | none (only unit-level `ShellViewServiceTest` / `frontend/test/unit/i18n.spec.ts`; `ShellController` has zero test coverage at any level) | **missing — GAP** |
| B07 unsupported locale 404 | none (only unit-level `SpaDocumentControllerTest` / `RouteTableTest` / `frontend/test/unit/routes.spec.ts`) | **missing — GAP** |
| B08 theme round-trip | `SessionRoundTripIT::preferenceSurvivesASecondRequestViaTheJdbcSession` + `shellStore.spec.ts` | covered, meaningful |
| B10 sidebar round-trip | `shellStore.spec.ts::"B10 setSidebar..."` covers it; the backend `SessionRoundTripIT` test's `@DisplayName` claims B10 but its body never calls `/preferences/sidebar` (theme-only) | covered (frontend); backend label overclaims — noted, not independently a FAIL |
| B11 sign-in | `SessionRoundTripIT` (x2) + `LoginView.spec.ts` | covered, meaningful |
| B12 empty e-mail | `LoginView.spec.ts::"B12 ..."` (frontend only — consistent with the native-form-post architecture) | covered |
| B13 malformed e-mail | `LoginView.spec.ts` — functionally matches but the `it()` title is unlabeled (no "B13" prefix, unlike its siblings) | functionally covered; traceability nit |
| B14 logout | `SessionRoundTripIT::signInThenSignOutRoundTripsTheIdentity` | covered, meaningful |

## Evidence paths

- `waves/wave-0/integration/backend-mvnw-verify.log` (first attempt, build-order failure)
- `waves/wave-0/integration/backend-mvnw-verify-2.log` (retry, BUILD SUCCESS, Testcontainers log)
- `waves/wave-0/integration/frontend-test-integration.log` (Vitest run)
- `waves/wave-0/integration/behaviour-mapping.md` (full per-behaviour rationale)
