# Slice packet w4-popups-widget-editor — wave-4, journey J9 "popups-widget-editor"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w4-popups · branch `migration/wave-4/popups-widget-editor` · parent SHA 11d3cc49fdc15e6e696c9a6f175d8953caec8a2e. **Lease:** compose project `kivvi-w-popups`, HTTP_PORT=19110, HTTPS_PORT=19111. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can preview on-site widgets and compose one for desktop or mobile.
**Oracle:** journeys/popups-widget-editor/steps/1..7 (`/pl/popups`, `?preview=p2`, `/pl/popups/p1`, `?type=banner`, `?device=mobile`, inspector, `/pl/popups/new`). Behaviours: B29, B01 (popups rows).
**Old-stack sources:** `src/Controller/WidgetController.php` (`index` with `?preview=` defaulting to `firstId()`, `create`, `edit` with `id` regex `p\d+`, `editor()` → `widget{id,name,meta,type,content}`, `device` desktop|mobile, `types`, `blocks`, `variables`, `triggers`, `audience`, `accent_colors`), `src/Panel/Content/WidgetCatalog.php` (every public method is payload), `templates/pages/popups.html.twig`, `templates/pages/popup-editor.html.twig`, partials `templates/pages/widgets/{blocks,inspector,stage-meta,type-list}.html.twig`, `templates/pages/popups/trigger-summary.html.twig`, organisms `popup-stage`, `popup-widget`, `editor-shell`, `block-library` (from campaigns), molecules `position-grid`, `swatch-grid`, `trigger-row`, `add-slot`, `segmented` (from customers), `card`; `tests/e2e/specs/lists.spec.ts` (widgets) and `editors.spec.ts` (popup editor).

## In scope
- Backend: `GET /api/v1/{locale}/popups?preview=` → `{ cards, selected, types }` (unknown preview id → same behaviour as today: check `WidgetCatalog::selected` and reproduce, including any 404); `GET /api/v1/{locale}/popups/{id}?type=&device=` and `GET /api/v1/{locale}/popups/new?type=&device=` → the editor payload with the exact keys the Twig page receives; `id` must match `p\d+` else 404 (API and document).
- Frontend: `PopupsView.vue` (list + live `PopupStage` preview, `?preview=` is a real navigation), `PopupEditorView.vue` reusing `EditorShell`, `BlockLibrary`, `Segmented`; new components `PopupStage`, `PopupWidget`, `ShopMock`, `PositionGrid`, `SwatchGrid`, `TriggerRow`, `AddSlot`, `CheckboxRow`, `PwTypeList`; viewport chip `1440 × 900` / `390 × 844`; type and device switches are real navigations with query params.
- Tests: JUnit for the id regex/404, the `?type=` fallback to the widget's own type and `?device=` fallback to desktop; Vitest for `PositionGrid` (9 cells, 1 checked) and `PopupStage` `data-type`; e2e `lists.spec.ts -g widgets` and `editors.spec.ts -g "popup editor"` (one `-g` per invocation).

## Out of scope
Saving widgets, `POST …/blocks` (DEV-7: dead call today, do not add it).

## Deviations in scope
DEV-4, DEV-7.

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19111 npx playwright test lists.spec.ts -g widgets` then `… editors.spec.ts -g "popup editor"`.

## Parallel-safety obligations
Shared wave with `import-wizard` and `dashboard`. Touch only: `backend/src/main/java/click/kivvi/{web/WidgetController.java,web/dto/Widget*.java,application/WidgetViewService.java,fixtures/WidgetFixtures.java}`, `backend/src/test/**/*widget*`, `frontend/src/views/{PopupsView,PopupEditorView}.vue`, `frontend/src/components/widgets/**`, `frontend/src/components/**/{PopupStage,PopupWidget,ShopMock,PositionGrid,SwatchGrid,TriggerRow,AddSlot,CheckboxRow,PwTypeList}.vue`, `frontend/src/i18n/messages/popups.*.ts`, `frontend/src/router/routes.ts` (your lines + imports only), `frontend/test/**/*popup*`, `frontend/test/**/*widget*`, your report/evidence. Never edit `EditorShell`, `BlockLibrary`, `Segmented`, `EventStream`, `KpiGrid`, `Table` — if a shared component needs a change, stop and report it to the orchestrator.
