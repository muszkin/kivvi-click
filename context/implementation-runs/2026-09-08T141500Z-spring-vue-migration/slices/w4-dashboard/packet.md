# Slice packet w4-dashboard — wave-4, journey J11 "dashboard"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w4-dashboard · branch `migration/wave-4/dashboard` · parent SHA 11d3cc49fdc15e6e696c9a6f175d8953caec8a2e. **Lease:** compose project `kivvi-w-dashboard`, HTTP_PORT=19130, HTTPS_PORT=19131. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can see the operational overview and jump from it into a customer profile or a rule.
**Oracle:** journeys/dashboard/steps/1..4. Behaviours: B23, B01 (dashboard row), and closing DEV-11 (login steps 4 and 8 become unmasked — re-run `compare.mjs --journey login` with the DEV-11 mask removed for those steps and prove parity, then edit `tools/migration-verify/deviations.json` accordingly).
**Old-stack sources:** `src/Controller/DashboardController.php` (`kpis`, `legend`, `events` = `EventFeed::rows(LIVE_ROWS, now)`, `mercure_topic`, `recent_customers` with `lastSeen = Format::timeAgo(now - lastSeenMinutes)`, `top_automations` mapped the same way — read the rest of the method), `src/Panel/Content/DashboardMetrics.php` (`kpis`, `cardiogramLegend`), `CustomerDirectory::recent`, `AutomationCatalog` top list, `templates/pages/dashboard.html.twig` + `templates/pages/dashboard/{recent-customers,top-automations}.html.twig`, organisms `cardiogram`, `list-card`, `kpi-grid` (from feeds), `event-stream` (from event-stream — reuse `EventStream.vue` unchanged, including the `pause-stream` intent), `assets/controllers/cardiogram.ts` (canvas redraw on `kivvi:event` and on a `MutationObserver` for `data-theme`/`style`, random events-per-second); `tests/e2e/specs/dashboard.spec.ts` (6 tests).

## In scope
- Backend: `GET /api/v1/{locale}/dashboard` → `{ kpis, legend, events (LIVE_ROWS rows, DEV-3 shape, same as event-stream), mercureTopic, recentCustomers[{…, lastSeen}], topAutomations[…] }` using the existing `Format.timeAgo` port and the existing `EventFeed`/`CustomerDirectory`/`AutomationCatalog` fixtures.
- Frontend: `DashboardView.vue`: `KpiGrid`, `Cardiogram` (port of `cardiogram.ts`: canvas, legend, redraw on `data-theme` mutation and on `kivvi:event`, random ticks), `EventStream` subscribed to the same Mercure topic (`EventStream` already owns the SSE; feed it the initial rows), `ListCard` ×2 (recent customers link to `/pl/customers/{id}`, top automations link to `/pl/automations/{id}`), `pause-stream` intent works here too.
- Tests: Vitest for `Cardiogram` redraw on a `data-theme` mutation (spy on the 2D context) and `ListCard` links; JUnit `DashboardApiIT` for the payload and the `lastSeen` strings; e2e `dashboard.spec.ts` (`--workers=1`, it subscribes to SSE).

## Out of scope
Real metrics; the event stream internals (owned by event-stream — reuse only).

## Deviations in scope
DEV-1, DEV-2 (mask `#cg-main, .cardiogram-canvas`), DEV-3, DEV-4; closes DEV-11.

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19131 npx playwright test dashboard.spec.ts --workers=1`

## Parallel-safety obligations
Shared wave with `popups-widget-editor` and `import-wizard`. Touch only: `backend/src/main/java/click/kivvi/{web/DashboardController.java,web/dto/Dashboard*.java,application/DashboardViewService.java,fixtures/DashboardFixtures.java}`, `backend/src/test/**/*dashboard*`, `frontend/src/views/DashboardView.vue` (replace the wave-0 placeholder body), `frontend/src/components/dashboard/**`, `frontend/src/components/**/{Cardiogram,ListCard}.vue`, `frontend/src/i18n/messages/dashboard.*.ts`, `frontend/src/router/routes.ts` (only if the dashboard route changes), `frontend/test/**/*dashboard*`, `frontend/test/**/*cardiogram*`, `tools/migration-verify/deviations.json` (DEV-11 expiry only), your report/evidence. Never edit `EventStream`, `EventRow`, `KpiGrid`, `Sidebar` — stop and report if a shared component needs a change.
