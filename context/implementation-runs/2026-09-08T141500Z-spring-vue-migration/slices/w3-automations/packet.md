# Slice packet w3-automations — wave-3, journey J6 "automations"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w3-automations · branch `migration/wave-3/automations` · parent SHA <PARENT-SHA> (feature HEAD after wave-2). **Lease:** compose project `kivvi-w-automations`, HTTP_PORT=19060, HTTPS_PORT=19061. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can review automation rules and edit one as a list or a diagram with its simulation.
**Oracle:** journeys/automations/steps/1..7 (`/pl/automations`, `?status=active`, back, click first `.auto-card` → `/pl/automations/a1`, `Diagram` → `?view=flow`, `Lista` → `?view=list`, `/pl/automations/new`). Behaviours: B26, B01 (automations + editor rows).
**Old-stack sources:** `src/Controller/AutomationController.php`, `src/Panel/Content/AutomationCatalog.php` (note `activeForCustomer` already exists in `fixtures/AutomationsFixtures.java` from wave-2 customers — extend that class, keep the method), `templates/pages/automations.html.twig`, `templates/pages/automation-editor.html.twig`, `templates/pages/automations/simulation.html.twig`, organisms `rule-pipeline`, `flow-canvas`, `editor-shell`? (check — the automation editor may not use `editor-shell`; `editor-shell` belongs to campaigns in this wave), molecules `rb-block`, `cond-rule`, `flow-node`, `filter-chip` (from event-stream), `segmented` (from event-stream), `tabs` (from customers), `card`, `chip`; `assets/controllers` (flow-canvas has `data-controller="flow-canvas"` but no TS controller exists — reproduce the attribute, no behaviour); `tests/e2e/specs/automations.spec.ts`.

## In scope
- Backend: `GET /api/v1/{locale}/automations?status=` → `{ filters, automations }`; `GET /api/v1/{locale}/automations/{id}?view=` (`new` or `a\d+`) → `{ automation (header), view, tabs, steps, nodes, edges, simulation }` exactly as the controller passes them; unknown id → 404.
- Frontend: `AutomationsView.vue`, `AutomationEditorView.vue`; owned components: `AutoCard`, `RulePipeline`, `RbStep`/`RbBlock`, `CondRule`, `FlowCanvas` (with the SVG edges: `.flow-svg path` ×5), `FlowNode`, simulation card; `?view=` switch via real navigation (`navigate` intent, `.seg [data-action=navigate]`); `go-automation` intent.
- Tests: Vitest for `FlowCanvas` (6 nodes, 5 paths), `RulePipeline` (3 steps, 3 blocks in step 2, kicker "KIEDY"); JUnit fixture `header('new')` and 404; e2e `automations.spec.ts` (4 tests).

## Out of scope
Dashboard "top automations" list (dashboard journey), customers' active automations (already served).

## Deviations in scope
DEV-4.

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19061 npx playwright test automations.spec.ts`

## Parallel-safety obligations
Shared wave with `settings` and `campaigns-email-editor`. Touch only: `backend/src/main/java/click/kivvi/{web/AutomationsController.java,web/dto/Automation*.java,application/AutomationsViewService.java,fixtures/AutomationsFixtures.java (extend),domain/* new types}`, `backend/src/test/**/automation*`, `frontend/src/views/{AutomationsView,AutomationEditorView}.vue`, `frontend/src/components/**/{AutoCard,RulePipeline,RbStep,RbBlock,CondRule,FlowCanvas,FlowNode,SimulationCard}.vue`, `frontend/src/composables/useIntents.ts` (add `go-automation` only if missing), `frontend/src/i18n/messages/automations.*.ts`, `frontend/src/router/routes.ts` (your three component lines + imports), `frontend/test/**/automation*`, your report/evidence.
