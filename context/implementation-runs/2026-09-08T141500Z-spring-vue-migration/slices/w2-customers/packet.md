# Slice packet w2-customers — wave-2, journey J5 "customers"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w2-customers · branch `migration/wave-2/customers` · parent SHA 4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a. **Lease:** compose project `kivvi-w-customers`, HTTP_PORT=19050, HTTPS_PORT=19051. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can browse customers, open a 360 profile and return to the list.
**Oracle:** journeys/customers/steps/1..6 (`/pl/customers`, `?page=2`, back to list, click first row → `/pl/customers/c_…`, back link → list, `/pl/customers/c_9999` → 404). Behaviours: B03, B05, B25, B01 (customers + profile rows).
**Old-stack sources:** `src/Controller/CustomerController.php` (segments, timeline, facts, tabs, scores are built there — port them verbatim), `src/Panel/Content/CustomerDirectory.php`, `src/Panel/Content/AutomationCatalog.php::activeForCustomer` (consumed read-only: create a minimal `AutomationsFixtures.activeForCustomer` if the automations journey has not landed — it has not; keep the method name so wave-3 can extend the class), `templates/pages/customers.html.twig`, `templates/pages/customer.html.twig`, `templates/pages/customers/{timeline,active-automations}.html.twig`, molecules `table`, `pagination`, `profile-fact`, `timeline-item`, `tabs`, `filter-chip` (exists after wave-2 event-stream? NO — it is being built in parallel: if you need `FilterChip`, check `templates/pages/customers.html.twig` — customers segments use their own markup; do not create `FilterChip`), atoms `avatar` (exists), `chip` (exists), `kpi-grid` (from feeds), `page-head` (exists); `tests/e2e/specs/customers.spec.ts`.

## In scope
- Backend: `GET /api/v1/{locale}/customers?page=` → `{ subtitle, segments, customersRows (24 rows, pre-formatted revenue/lastSeen), page, pages (192) }`; `GET /api/v1/{locale}/customers/{id}` → `{ customer (+tags), profileSub, facts (7), automations (activeForCustomer), tabs (5), scores (3), timeline (9) }`; unknown id → 404 document for the page route AND 404 JSON for the API. `lastSeen` computed from the request clock exactly like `Format::timeAgo` with the fixture minute offsets (deterministic strings like "12 min temu").
- Frontend: `CustomersView.vue`, `CustomerView.vue` reproducing the two pages; components `Table` (molecule — first owner; match the Twig `table.html.twig` params: columns, rows with cells/links/intents, `.table thead th`, `tbody tr` with `data-action`/`data-payload`), `Pagination` (`.row .mono.muted` "Strona 1 z 192", `button[data-action=go-page]` with the first disabled on page 1), `ProfileFact`, `TimelineItem`, `Tabs` (`.tab-strip .tab[aria-selected]`), customers segments strip; `go-customer`, `go-page`, `set-segment` intents; the profile's back link `.page-head a.btn.ghost` to the index; detail route keeps `customers` active in the sidebar (B03).
- Tests: JUnit fixture `byId` unknown → 404; Vitest `Table` row intent, `Pagination` disabled state; e2e `customers.spec.ts` (4 tests).

## Out of scope
Real pagination data (all pages render the same 24 rows as today? — check `CustomerController::index`: `page` only changes the pager; reproduce exactly), automations editor, dashboard.

## Deviations in scope
DEV-4, DEV-12 (step 6 is a 404 document).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19051 npx playwright test customers.spec.ts`

## Parallel-safety obligations
Shared wave with `event-stream`. Touch only: `backend/src/main/java/click/kivvi/{web/CustomersController.java,web/dto/Customer*.java,application/CustomersViewService.java,fixtures/CustomersFixtures.java,fixtures/AutomationsFixtures.java (create with activeForCustomer only)}`, `backend/src/test/**/customer*`, `frontend/src/views/{CustomersView,CustomerView}.vue`, `frontend/src/components/**/{Table,Pagination,ProfileFact,TimelineItem,Tabs,SegmentStrip}.vue`, `frontend/src/composables/useIntents.ts` (add `go-customer`, `go-page`, `set-segment` only if missing), `frontend/src/i18n/messages/customers.*.ts`, `frontend/src/router/routes.ts` (your two lines + imports), `frontend/test/**/customer*`, your report/evidence.
