# Slice packet w4-import-wizard — wave-4, journey J10 "import-wizard"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w4-import · branch `migration/wave-4/import-wizard` · parent SHA <PARENT-SHA>. **Lease:** compose project `kivvi-w-import`, HTTP_PORT=19120, HTTPS_PORT=19121. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can walk the four-step customer import and upload a file that advances the wizard.
**Oracle:** journeys/import-wizard/steps/1..10 (steps 1–4 at `/pl/import/{1..4}`, stepper forward/back, the upload step recorded with `http.jsonl` showing `POST /import/upload` → 302 `/pl/import/2`, `db.json` delta, `/pl/import/5`/`/pl/import/nonexistent` → 404). Behaviours: B31, B01 (import rows).
**Old-stack sources:** `src/Controller/ImportController.php` (`wizard` step `[1-4]` default 1 — every `ImportWizard` method is payload: `steps, step, file(currentFileName), columns, targets, detection, validations, dedup_strategies, summary, preview, row_count, error_count, recent`; `upload`: missing file → 404, store → 302 to `import` step 2 — note the redirect target locale: reproduce exactly what the oracle `http.jsonl` records), `src/Panel/ImportUploadStorage.php` (stored name `bin2hex(random_bytes(16)).'.'.ext`, ext = lower-cased client extension if it matches `^[a-z0-9]{1,8}$` else `csv`; session keys `import.file_name` (original client name) and `import.file_path`; upload dir `var/import`), `src/Panel/Content/ImportWizard.php`, `templates/pages/import.html.twig` + the 10 partials under `templates/pages/import/`, molecules `stepper`, `dropzone`, `map-row`, `cond-rule` (from automations), `table` (from customers), organisms `kpi-grid` (from feeds), `assets/controllers/upload.ts` (`fetch("/import/upload", {method:"POST", body})` then `if (r.redirected) window.location.href = r.url`); `tests/e2e/specs/import.spec.ts` (6 tests incl. the real file upload via `.dropzone input[type="file"]` → `waitForURL(/\/pl\/import\/2/)` → `.file-pill` shows `klienci-e2e.csv`).

## In scope
- Backend: `GET /api/v1/{locale}/import/{step}` with the full wizard payload; step outside 1–4 → 404 (API and document); `POST /import/upload` multipart (`file`) → store under the api container's `var/import` (configurable directory property, default `./var/import`), put original name + stored path in the Spring Session, respond 302 to the same Location the oracle recorded; missing file → 404; storage failure → 500 (as today's uncaught RuntimeException). Session-derived `file.name` must appear in the next `GET …/import/2` payload.
- Frontend: `ImportView.vue` with `Stepper` (real navigations), `Dropzone` (posts with `fetch`, follows `r.redirected` → router navigation to the same path, so `waitForURL` sees `/pl/import/2`), `MapRow`, `FilePill`, validations list, dedup radios, consent, summary, recent imports; wizard partial components under `components/import/`.
- Tests: JUnit `ImportUploadStorageTest` reproducing the path-traversal control (`../../x.sh` → `<32 hex>.sh`; `evil.CSV` → `.csv`; `x.tar.gz9999` → `.csv`; no extension → `.csv`), `ImportApiIT` for the 404s, the 302 and the session round-trip of the file name; Vitest for `Stepper` current/back links and `Dropzone` redirect handling with a fake `fetch`; e2e `import.spec.ts` (6 tests).

## Out of scope
Parsing the CSV, mapping persistence, running the import.

## Deviations in scope
DEV-4, DEV-5 (`db.json`: no `messenger_messages`; `var/import` +1 file delta stays), DEV-9, DEV-12 (404 documents).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19121 npx playwright test import.spec.ts`

## Parallel-safety obligations
Shared wave with `popups-widget-editor` and `dashboard`. Touch only: `backend/src/main/java/click/kivvi/{web/ImportController.java,web/ImportUploadController.java,web/dto/Import*.java,application/ImportViewService.java,application/importing/**,infrastructure/importing/**,fixtures/ImportFixtures.java}`, `backend/src/main/resources/application*.yml` (only an `kivvi.import.upload-directory` key — report the exact diff), `backend/src/test/**/*import*`, `compose.next*.yaml` ONLY if a volume for `var/import` is unavoidable (report it; prefer a path inside the container), `frontend/src/views/ImportView.vue`, `frontend/src/components/import/**`, `frontend/src/components/**/{Stepper,Dropzone,MapRow,FilePill}.vue`, `frontend/src/i18n/messages/import.*.ts`, `frontend/src/router/routes.ts` (your lines + imports only), `frontend/test/**/*import*`, `frontend/test/**/*dropzone*`, your report/evidence. Never edit `CondRule`, `KpiGrid`, `Table`, `EventStream` — stop and report if a shared component needs a change.
