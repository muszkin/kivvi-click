# Wave-4 verification — dimension: unit (round 1)

**Wave SHA:** `fe37fad3b884864ca1d41a2ed7cf258126329e18`
**Journeys:** popups-widget-editor, import-wizard, dashboard
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit` (detached HEAD at the wave SHA; confirmed via `git rev-parse HEAD`; not edited).
**Oracle:** `/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue`, manifest sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — verified matching (`sha256sum manifest.json`).

## Dimension status: **PASS**

Bound to wave SHA `fe37fad3b884864ca1d41a2ed7cf258126329e18`.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/backend` | 0 |
| `npm ci --no-audit --no-fund` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |
| `npm run test -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |

Java: Temurin 25.0.4.1 LTS (`java -version` confirmed before running mvnw). Node: v26.8.1 (host), npm 11.19.0.

## Results

- Backend (Surefire, `*Test.java` only — confirmed no `*IT.java`/`*ApiIT.java` file appears among the 48 executed classes; those (`AutomationsApiIT`, `WidgetApiIT`, `ImportApiIT`, `CampaignsApiIT`, `CollectApiIT`, …) are Failsafe-bound to the `integration-test`/`verify` phases, never reached by `./mvnw test`, so they are out of unit scope): **48 test classes, 289 tests, 0 failures, 0 errors, 0 skipped.**
- Frontend (`vitest run test/unit`, 26 spec files under `frontend/test/unit` only — `frontend/test/integration/*` is a separate npm script (`test:integration`), out of unit scope): **26 test files, 164 tests, 0 failures.**

## Behaviour → named-test mapping (behaviours.json, wave-4 scope)

Scope per packet: B29 (popups-widget-editor), B31 (import-wizard), B23 (dashboard), and the B01 rows for popups/popup-editor/import/dashboard (`/pl/popups`, `/pl/popups/new`, `/pl/popups/p1`, `/pl/import`, `/pl/import/2`, `/pl/dashboard`, `/en/dashboard`). Every behaviour in scope has at least one named unit-scope test (`@DisplayName("Bnn …")` in a `backend/src/test/java/**/*Test.java`, and/or `describe("Bnn …")`/`it("Bnn …")` in `frontend/test/unit/**/*.spec.ts`), and every located test is green.

| Behaviour | Journey | Unit-scope tests found (file — count) | Verdict |
| --- | --- | --- | --- |
| B01 (popups/popup-editor/import/dashboard rows) | popups-widget-editor, import-wizard, dashboard | `backend/.../web/SpaDocumentControllerTest.java` (parameterized `everyKnownRouteRenders200`, includes `/pl/dashboard`, `/pl/popups`, `/pl/popups/new`, `/pl/popups/p1`, `/pl/import`, `/pl/import/2`, `/en/dashboard`); `backend/.../domain/RouteTableTest.java` (same URL set, route-table match); `frontend/test/unit/routes.spec.ts` (same URL set, router resolve) | named, green |
| B29 | popups-widget-editor | `backend/.../application/WidgetViewServiceTest.java` (6 `@DisplayName("B29 …")` tests: index cards, preview fallback, `?preview=` selection, single-widget resolve, `?type=` override, type fallback); `backend/.../web/WidgetControllerTest.java` (4 `@DisplayName("B29 …")` tests: index endpoint, `?preview=`, editor blank-draft shape, known-widget editor payload with 5 types/10 blocks/4 variables/3 triggers/4 …); `frontend/test/unit/widgetComponents.spec.ts` (7 `describe("B29 …")` blocks: `PopupStage`, `ShopMock`, `PopupWidget`, `PositionGrid`, `SwatchGrid`, `TriggerRow`, `AddSlot`, `PwTypeList`) | named, green |
| B31 | import-wizard | `backend/.../application/ImportViewServiceTest.java` (7 `@DisplayName("B31 …")` tests, incl. `B31/DEV-12` step-outside-1-4 mapping); `backend/.../web/ImportControllerTest.java` (3 `@DisplayName("B31 …")` tests: out-of-range steps, shared 14-option target list); `backend/.../web/ImportUploadControllerTest.java` (2 `@DisplayName("B31 …")` tests: POST redirects to `/pl/import/2`, missing file part is not found); `backend/.../infrastructure/importing/ImportUploadStorageTest.java` (7 `@DisplayName("B31 …")` tests — see security-control table below); `frontend/test/unit/importComponents.spec.ts` (2 `describe("B31 …")` blocks: `Stepper`, `Dropzone`) | named, green |
| B23 | dashboard | `backend/.../application/DashboardViewServiceTest.java` (6 `@DisplayName("B23 …")` tests: 4 KPIs w/ sparklines, deterministic series, cardiogram legend, 10 live rows + Mercure topic, EN translation, 6 recently-seen customers); `backend/.../web/DashboardControllerTest.java` (2 `@DisplayName("B23 …")` tests: first recently-seen customer, first best-performing automation); `frontend/test/unit/Cardiogram.spec.ts` (4 `it("B23 …")` tests: canvas role=img + accessible name, draws on mount, redraws on `data-theme` mutation, redraws once a second) | named, green |

Full grep-based mapping evidence: `unit/behaviour-test-mapping.txt`.

No behaviour in scope was found without a named unit test, and no located test was red.

## Security control unit tests — import upload (explicit packet requirement)

`backend/src/test/java/click/kivvi/infrastructure/importing/ImportUploadStorageTest.java` — 9 tests, 0 failures, 0 errors (Surefire report in `unit/backend-import-upload-security-tests.txt`):

| Control | Test (`@DisplayName`) | Verdict |
| --- | --- | --- |
| Extension whitelist (`^[a-z0-9]{1,8}$`, case-insensitive, else falls back to `csv`) | "B31 an 8-character alphanumeric extension is at the whitelist's own boundary and is accepted"; "B31 a 9-character extension exceeds the whitelist and falls back to csv"; "B31 a mixed-case extension is lower-cased before the whitelist check"; "B31 no extension at all falls back to csv"; "B31 an extension containing a non-alphanumeric character is rejected" | named, green (5/5) |
| Random stored name, immune to path traversal / collision | "B31 a path-traversal original name never escapes the upload directory: the stored name is a random 32-hex-char name plus the whitelisted extension of its basename" (asserts `^[0-9a-f]{32}\.sh$`, single file in upload dir, only the basename `x.sh` is remembered); "B31 two uploads in the same session never collide on the generated name" | named, green (2/2) |

Both controls are exercised directly against `ImportUploadStorage` (plain unit test, no Spring context), matching the packet's requirement to include the security control unit tests for the import upload.

## Deviations in scope (unit-dimension relevance)

- **DEV-4** (contract dimension, full-parity HTTP rule, `journeys: "*"`) — not a unit-dimension concern; no unit-command output references it directly.
- **DEV-7** (popups/campaigns: no `POST …/blocks`, contract dimension) — not applicable to unit; `blocks` in the widget/campaign tests refers to the editor payload's `blocks` array field (`payload.blocks()`, `$.blocks.length()`), not the removed endpoint. No unit-dimension action needed.
- **DEV-5/DEV-9** (import: session-cookie-name-ignored + `var/import files` counter mapping, contract dimension) — not applicable to unit; the underlying session-derived file-name/path behaviour is unit-tested green in `ImportUploadStorageTest` and `ImportViewServiceTest`, independent of the db-count comparison mechanism DEV-5/DEV-9 govern.
- **DEV-12** (import-wizard step 10 = 404 document, visual dimension) — the underlying backend behaviour is unit-tested: `backend/.../application/ImportViewServiceTest.java`'s `"B31/DEV-12 a step outside 1-4 throws, mapped to a 404 by ImportController"` (green); the HTTP-layer counterpart (`ImportApiIT.java`'s `"B31/DEV-12 GET /pl/import/5 …"`) is Failsafe-bound, integration-dimension, not run here. No unit-dimension deviation needed; parity holds outright.
- **DEV-1/DEV-2/DEV-3** (dashboard: clock strings, cardiogram canvas mask, JSON events — all visual/contract dimension) — not applicable to unit; `Cardiogram.spec.ts`'s B23 tests exercise the canvas redraw logic directly (not screenshot comparison), and `DashboardViewServiceTest`'s live-row test asserts the JSON shape at the service layer, both independent of the masks/normalize rules DEV-1/DEV-2/DEV-3 govern for the visual/contract dimensions.
- **DEV-11** (login steps 4/8 closure) — not applicable to unit; visual/e2e-dimension concern (regression guard screenshot mask), no unit-command output affected.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| popups-widget-editor | parity | B29 and its B01 rows (`/pl/popups`, `/pl/popups/new`, `/pl/popups/p1`) fully mapped to green unit tests, backend + frontend |
| import-wizard | parity | B31 and its B01 rows (`/pl/import`, `/pl/import/2`) fully mapped to green unit tests; import-upload security controls (extension whitelist, random stored name) unit-tested and green; DEV-12's backend behaviour is unit-tested and green (no deviation needed at this dimension) |
| dashboard | parity | B23 and its B01 rows (`/pl/dashboard`, `/en/dashboard`) fully mapped to green unit tests, backend + frontend |

## Evidence paths

- `unit/backend-mvnw-test.log` — full `./mvnw -q test` output
- `unit/backend-surefire-summary.txt` — per-class `Tests run` lines for all 48 executed classes
- `unit/backend-import-upload-security-tests.txt` — Surefire report + `@DisplayName` list for `ImportUploadStorageTest` (import-upload security controls)
- `unit/frontend-npm-test.log` — full `npm run test -- --run` output
- `unit/behaviour-test-mapping.txt` — grep evidence for the behaviour → named-test tables above

## Cleanup

Per packet instructions (disk is tight), after the runs above `backend/target` and `frontend/node_modules` were deleted in this verifier's own checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit`) only; no other checkout, the main repo, or the oracle was touched.
