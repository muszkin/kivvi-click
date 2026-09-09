# Wave-4 verifier — dimension: unit (round 2)

**Wave SHA:** `71d884d2ed96a7a7caad18147e76570c9e113d4d`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit` (detached at the wave SHA; confirmed clean, `git log -1` matched, `git status` clean, before any command ran)
**Journeys:** popups-widget-editor, import-wizard, dashboard
**Dimension status: PASS**, bound to `71d884d2ed96a7a7caad18147e76570c9e113d4d`

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/backend` | 0 |
| 2 | `npm ci --no-audit --no-fund` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |
| 3 | `npm run test -- --run` (→ `vitest run test/unit -- --run`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |
| 4 | `npx vitest run test/unit/widgetComponents.spec.ts test/unit/importComponents.spec.ts test/unit/Cardiogram.spec.ts --reporter=verbose` (targeted re-run, verbose, for behaviour-name evidence only; same suite already counted in #3) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |

## Results

- Backend (`./mvnw -q test`, Surefire `*Test.java` only): 48 test classes, 0 failures, 0 errors, 0 skipped (full per-class breakdown in `unit/backend-surefire-summary.txt`; raw run log `unit/backend-mvn-test.log`). This is the whole repository's unit suite (all waves), run as a regression guard per the packet ("earlier waves are regression guards only") — all green, no earlier-wave test went red.
- Frontend (`npm run test -- --run`, `vitest run test/unit`): 26 test files, 164 tests, 0 failures (`unit/frontend-npm-test.log`). Targeted verbose re-run of the wave-4 behaviour files in `unit/frontend-npm-test-verbose-B29-B31-B23.log`.
- `ArchitectureTest` (5 tests) is part of the full backend unit run and passed, but architecture is a separate dimension owned by another verifier — not adjudicated here.

## Behaviour → named test mapping (in-scope behaviours only)

### B29 — popups-widget-editor
Oracle: `tests/e2e/specs/lists.spec.ts widgets (2 tests) + editors.spec.ts popup editor (4 tests)` (e2e dimension, not this one).

Unit-level named tests, all green:
- Backend, `click.kivvi.web.WidgetControllerTest` (8 tests): `@DisplayName("B29 GET /api/v1/pl/popups returns 5 cards, a selected preview and 5 types")`, `"B29 ?preview=p2 selects the banner widget"`, `"B29 GET /api/v1/pl/popups/new returns the blank-draft widget shape"`, plus 2 more `"B29 …"` (widget-edit-shape variants).
- Backend, `click.kivvi.application.WidgetViewServiceTest` (10 tests): 8 tests tagged `"B29 …"` covering the popup index cards, `?preview=`/`?type=` selection and fallback, and the popup-edit resolution.
- Frontend, `test/unit/widgetComponents.spec.ts` (8 `describe` blocks, 14 tests, all tagged `describe("B29 …")`): `PopupStage`, `ShopMock`, `PopupWidget` (type/device switches), `PositionGrid`, `SwatchGrid`, `TriggerRow`, `AddSlot`, `PwTypeList` — matches the behaviour's "editor type/device switches, inspector triggers/audience/position grid" wording.

Verdict: **parity**.

### B31 — import-wizard
Oracle: `tests/e2e/specs/import.spec.ts (6 tests)` (e2e dimension).

Unit-level named tests, all green:
- Backend, `click.kivvi.web.ImportControllerTest` (6 tests): `"B31 GET /api/v1/pl/import/5 (outside 1-4) is not found"`, `"B31 GET /api/v1/pl/import/0 (outside 1-4) is not found"`, `"B31 every step returns the same shared 14-option target list per column"`.
- Backend, `click.kivvi.web.ImportUploadControllerTest` (2 tests): `"B31 a POSTed file is stored and redirects (302) to /pl/import/2"`, `"B31 a request with no file part is not found"`.
- Backend, `click.kivvi.application.ImportViewServiceTest` (7 tests): 6 tests tagged `"B31 …"` (upload fallback name, detection counts, skipped column, preview-row validation), plus `"B31/DEV-12 a step outside 1-4 throws, mapped to a 404 by ImportController"` (covers DEV-12's import-wizard step-10 case at unit level).
- Backend, `click.kivvi.infrastructure.importing.ImportUploadStorageTest` (9 tests, **import upload security control**): `"B31 a path-traversal original name never escapes the upload directory: the stored name is a random 32-hex-char name plus the whitelisted extension of its basename"` (asserts `../../x.sh` never escapes the temp upload dir, only the basename is remembered, exactly one file is created), plus the extension-whitelist boundary/fallback/case-folding/collision tests, all tagged `"B31 …"`.
- Frontend, `test/unit/importComponents.spec.ts` (2 `describe` blocks, 7 tests): `describe("B31 Stepper marks the current step and the ones already passed")` (4 tests — stepper forward/back state, `data-action`/`data-payload`, done-icon) and `describe("B31 Dropzone posts the file and follows a redirected response with a full navigation, exactly like assets/controllers/upload.ts")` (3 tests — upload → redirect → full navigation, matches "upload advances to step 2 and shows the file name").

Verdict: **parity**. Note: `ImportApiIT`'s traversal test (mentioned in the round-2 note as using a private `@TempDir` root) is an integration test (`*IT.java`), out of Surefire's `*Test.java` scope and not run or adjudicated here; the unit-level traversal/security coverage above is `ImportUploadStorageTest`, independent of that IT.

### B23 — dashboard
Oracle: `tests/e2e/specs/dashboard.spec.ts (6 tests)` (e2e dimension).

Unit-level named tests, all green:
- Backend, `click.kivvi.web.DashboardControllerTest` (4 tests): includes `"B23 the first recently seen customer carries id, email, orders and lastSeen"`, `"B23 the first best-performing automation is the highest-revenue active one"` (links to customer/automation).
- Backend, `click.kivvi.application.DashboardViewServiceTest` (7 tests): `"B23 four KPIs, each with a 40-point sparkline, matching the oracle exactly"`, `"B23 the sparkline series is deterministic between two builds"`, `"B23 the cardiogram legend carries the three oracle entries"`, `"B23 ten live rows and the account's Mercure topic, matching the oracle's first row"`, `"B23 live rows translate their type label to English"`, `"B23 six recently seen customers, orders and lastSeen matching the oracle exactly"`.
- Frontend, `test/unit/Cardiogram.spec.ts` (4 of 7 tests tagged): `"B23 renders the canvas with role=img and the title as its accessible name"`, `"B23 draws once on mount"`, `"B23 redraws when <html data-theme> mutates, without reconnecting anything"`, `"B23 redraws once a second"` — covers "cardiogram canvas redraws on theme change".
- Pause-toggle / `data-paused` and live-row-topic-attribute behaviour is exercised at oracle level by `dashboard.spec.ts` (e2e) and backed by `DashboardViewServiceTest`'s live-rows/topic test above at unit level; no separate unit test claims pause-toggle DOM wiring — that is a Vue component behaviour covered by the wave's integration/e2e dimensions, not unit-level view-service/controller tests.

Verdict: **parity**.

### B01 — rows for popups(-widget-editor), import(-wizard), dashboard
Oracle: `tests/Controller/PanelPagesTest.php::testPageRenders (25 cases)` (old-stack test; new-stack equivalents are the `*ApiIT.java` files, which are integration-dimension, out of `*Test.java` unit scope).

Unit-level named tests, all green:
- Backend, `click.kivvi.domain.RouteTableTest`, `@DisplayName("B01 every panel/public URL in the route table is known")`: asserts `/pl/popups` → `popups`, `/pl/popups/p3` → `popup_edit`, `/pl/import` and `/pl/import/2` → `import`, `/pl/dashboard` → `dashboard` (plus every other panel route).
- Backend, `click.kivvi.web.SpaDocumentControllerTest`, `@DisplayName("B01 every panel/public URL renders 200 and carries the SPA document")` (parameterized, 21 cases): includes `/pl/popups`, `/pl/popups/new`, `/pl/popups/p1`, `/pl/import`, `/pl/import/2`, `/pl/dashboard` — each asserted `200` with `<html` in the body.

Verdict: **parity**.

## Deviations considered

Checked `tools/migration-verify/deviations.json` (oracle path) and the plan's "Accepted deviations" table for anything scoped to the `unit` dimension. DEV-1, DEV-2, DEV-3, DEV-5, DEV-9, DEV-11 are `dimension: visual` or `contract`; DEV-4, DEV-7 are `dimension: contract`; DEV-12 is `dimension: visual`. **None target the `unit` dimension** — no deviation was needed or applied to reach the PASS verdict above. DEV-12's import-wizard step-10 (404) and DEV-3's Mercure-JSON-payload case both have their own dedicated, independently-green unit tests regardless (`ImportViewServiceTest`'s `"B31/DEV-12 …"` test; `DashboardViewServiceTest`/`WidgetViewServiceTest`/`EventStream.spec.ts` are unaffected by DEV-3, which is an event-stream/contract-dimension concern outside this wave's journeys).

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | B29 + B01 rows fully mapped to named, green unit tests (backend + frontend); 0 failures |
| import-wizard | parity | B31 (incl. upload security/traversal control) + B01 row fully mapped to named, green unit tests; 0 failures |
| dashboard | parity | B23 + B01 row fully mapped to named, green unit tests; 0 failures |

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/unit/backend-mvn-test.log` — full `./mvnw -q test` output
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/unit/backend-surefire-summary.txt` — per-class `Tests run:` line for all 48 backend test classes
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/unit/frontend-npm-test.log` — full `npm run test -- --run` output (26 files / 164 tests)
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-4/unit/frontend-npm-test-verbose-B29-B31-B23.log` — verbose per-test names for the wave-4 behaviour files

## Housekeeping

Per the packet's disk instruction, `backend/target` and `frontend/node_modules` were deleted from this verifier's own checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit`) after producing the evidence above. No tracked file was edited; no other checkout, the main repo, or the oracle was touched. No Docker stack was started or stopped by this verifier.
