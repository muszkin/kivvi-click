# Slice packet w3-campaigns-email-editor — wave-3, journey J8 "campaigns-email-editor"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w3-campaigns-email-editor · branch `migration/wave-3/campaigns-email-editor` · parent SHA b87a701244f5316e1a53bace7a1e41facdffc277. **Lease:** compose project `kivvi-w-campaigns`, HTTP_PORT=19080, HTTPS_PORT=19081. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can review e-mail campaigns and open the template editor, whose document keeps literal colours in dark mode.
**Oracle:** journeys/campaigns-email-editor/steps/1..8 (`/pl/campaigns`, `?filter=triggered`, back, click first `.table tbody tr` → `/pl/emails/k1`, theme dark (computed `.ee-doc` background rgb(255,255,255)), reload, theme light, `/pl/emails/new`). Behaviours: B27, B28, B01 (campaigns + email editor rows).
**Old-stack sources:** `src/Controller/CampaignController.php`, `src/Panel/Content/CampaignCatalog.php`, `templates/pages/campaigns.html.twig`, `templates/pages/email-editor.html.twig`, `templates/pages/emails/{envelope,inspector,variables}.html.twig`, organisms `editor-shell`, `block-library`, `email-document`, molecules `coupon-code`, `tabs` (customers), `table` (customers), `filter-chip` (event-stream), `kpi-grid` (feeds), atoms `field`, `toggle-row`? (settings owns ToggleRow — if the inspector needs it, coordinate: the packet authorizes you to create `ToggleRow` ONLY if it does not exist at your parent SHA; settings integrates before you alphabetically? No — integration order is alphabetical: automations, campaigns-email-editor, settings. So campaigns integrates BEFORE settings: if you need ToggleRow, you own it and settings will consume it); `assets/controllers/editor.ts` (drag/drop only — no `/blocks` request, DEV-7); `tests/e2e/specs/lists.spec.ts` (campaigns), `editors.spec.ts` (email editor).

## In scope
- Backend: `GET /api/v1/{locale}/campaigns?filter=` → `{ kpis, filters, columns, rows }`; `GET /api/v1/{locale}/emails/{id}` (`new` or `k\d+`) → `{ template, blocks, variables, sections, selectedBlock }`.
- Frontend: `CampaignsView.vue`, `EmailEditorView.vue`; owned: `EditorShell` (organism; `.email-right` inspector column), `BlockLibrary` (`.ee-block-lib button` ×10, draggable), `EmailDocument` (`.ee-doc` with literal colours — inline styles/classes as in Twig so dark theme never affects it), `CouponCode`, envelope/inspector/variables partials, `hero_title` input value; drag-and-drop: `dragstart` sets text/plain block type, drop on `.ee-canvas-wrap` does NOT call any endpoint (DEV-7) — reproduce the current visible behaviour (nothing happens server-side); `go-email` intent; `copy-variable` intent.
- Tests: Vitest computed-style test for `.ee-doc` background under `data-theme=dark` (B28), library count; JUnit payload + 404; e2e `lists.spec.ts -g "campaigns"` and `editors.spec.ts -g "email editor"` (separate invocations).

## Out of scope
Popup editor (wave-4 consumes `EditorShell`), sending e-mails.

## Deviations in scope
DEV-4, DEV-7.

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19081 npx playwright test lists.spec.ts -g "campaigns"` and `… editors.spec.ts -g "email editor"`.

## Parallel-safety obligations
Shared wave with `automations` and `settings`. Touch only: `backend/src/main/java/click/kivvi/{web/CampaignsController.java,web/dto/Campaign*.java,application/CampaignsViewService.java,fixtures/CampaignsFixtures.java}`, `backend/src/test/**/campaign*`, `frontend/src/views/{CampaignsView,EmailEditorView}.vue`, `frontend/src/components/**/{EditorShell,BlockLibrary,EmailDocument,CouponCode,EmailEnvelope,EmailInspector,EmailVariables,ToggleRow(only if absent)}.vue`, `frontend/src/composables/useEditorDrag.ts`, `frontend/src/composables/useIntents.ts` (add `go-email`, `copy-variable` only if missing), `frontend/src/i18n/messages/campaigns.*.ts`, `frontend/src/router/routes.ts` (your three lines + imports), `frontend/test/**/{campaign,email}*`, your report/evidence.
