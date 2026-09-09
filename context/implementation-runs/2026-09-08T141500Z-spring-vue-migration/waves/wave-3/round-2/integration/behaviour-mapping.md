# Wave-3 integration dimension — behaviour -> qualifying-test mapping (round 2)

Scope: B26 (automations), B06 + B32 (settings, 8 tabs), B27 + B28 (campaigns-email-editor), and
every B01 row of this wave (automations, settings x8 tabs, campaigns, email-editor). A
"qualifying test" is either a real-HTTP `*IT.java` (`@SpringBootTest(webEnvironment=RANDOM_PORT)`
+ Testcontainers Postgres, run under `./mvnw -q verify`) or a `frontend/test/integration/*.spec.ts`
test that mounts the real view/component with the real vue-router (and pinia where the component
uses a store), stubbing only `fetch`.

## B01 rows (this wave)

| Row | Backend IT (qualifying) | Frontend integration (qualifying) |
| --- | --- | --- |
| `/pl/automations` | `AutomationsApiIT.automationsIndexPageRenders200` ("B01/B26 GET /pl/automations renders 200") | `AutomationsView.spec.ts` (mounted, real router) |
| `/pl/automations/new` | `AutomationsApiIT.automationNewPageRenders200` ("B01/B26 GET /pl/automations/new renders 200") | `AutomationEditorView.spec.ts > "B01/B26 GET /automations/new renders and shows its own headline"` |
| `/pl/automations/a99` (shape-valid, unseeded) | `AutomationsApiIT.unseededShapeValidAutomationDocumentStillRenders200` | -- (backend-only row) |
| `/pl/settings/account` | `SettingsApiIT.accountRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "B01 the account tab shows its marker text"` |
| `/pl/settings/sites` | `SettingsApiIT.sitesRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "the sites tab highlights the tracker snippet..."` (row rendering, not B01-tagged client-side; backend IT carries the B01 marker for this row) |
| `/pl/settings/team` | `SettingsApiIT.teamRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "B01 the team tab shows its marker text"` and `"B01 GET /pl/settings/team renders and shows its own page title and subtitle marker"` |
| `/pl/settings/providers` | `SettingsApiIT.providersRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "the providers tab shows 4 DNS rows..."` (backend IT carries the B01 marker) |
| `/pl/settings/api` | `SettingsApiIT.apiRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "the api tab shows 3 webhooks..."` (backend IT carries the B01 marker) |
| `/pl/settings/notifications` | `SettingsApiIT.notificationsRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "the notifications tab renders an 8x3 checkbox matrix..."` (backend IT carries the B01 marker) |
| `/pl/settings/billing` | `SettingsApiIT.billingRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "B01 the billing tab shows its marker text"` |
| `/pl/settings/gdpr` | `SettingsApiIT.gdprRowRendersWithItsOwnMarker` | `SettingsView.spec.ts > "B01 the gdpr tab shows its marker text"` |
| `/pl/campaigns` | `CampaignsApiIT.campaignsPageRenders200` ("B01 GET /pl/campaigns renders 200") | `CampaignsView.spec.ts > "B01/B27 renders 4 KPI tiles and 4 filter chips from the payload"` |
| `/pl/emails/new` | `CampaignsApiIT.newEmailDocumentRenders200` ("B01/B28 GET /pl/emails/new renders 200") | `EmailEditorView.spec.ts > "B01/B28 the \"new\" route shows the blank draft template"` |
| `/pl/emails/k1` (known id) | `CampaignsApiIT.knownEmailDocumentRenders200` ("B01/B28 GET /pl/emails/k1 renders 200") | `EmailEditorView.spec.ts > "B01/B28 renders the back link, title and 10 block-library buttons"` |
| `/pl/emails/k999` (unseeded, shape-valid) | `CampaignsApiIT.unseededButShapeValidEmailDocumentStillRenders200` (labelled B28, documents the id-agnostic editor route) | -- (backend-only row) |

Every B01 row of this wave has at least one qualifying test; all 8 settings tabs are covered
individually by name in `SettingsApiIT` (12 tests total: 8 row markers + B32 payload + B06 API
404 + B06/DEV-12 document 404 + bare-`/pl/settings` 200).

## B26, B06, B32, B27, B28

| Behaviour | Qualifying test(s) |
| --- | --- |
| B26 | `AutomationsApiIT.automationsListPayloadMatchesTheOracleThroughTheRealHttpLayer` (6 cards, 4 filters), `automationEditorPayloadMatchesTheOracleThroughTheRealHttpLayer` (3 steps/6 nodes/5 edges/3 sim rows), `unknownIdShapeApiIsNotFoundThroughTheRealHttpLayer`; `AutomationsView.spec.ts` (6 cards/4 chips, card metrics, go-automation intent); `AutomationEditorView.spec.ts` (list view 3 steps, flow view 6 nodes/5 edges, Lista/Diagram navigation, simulation card + publish button) |
| B06 | `SettingsApiIT.unknownTabApiIsNotFoundThroughTheRealHttpLayer` (API 404), `unknownTabDocumentIsNotFoundThroughTheRealHttpLayer` (document 404, DEV-12); `SettingsView.spec.ts > "B06 an unknown tab never renders a page body"` |
| B32 | `SettingsApiIT.settingsPayloadMatchesTheOracleThroughTheRealHttpLayer` (8 tabs, 3 sites, 5 team rows), `bareSettingsDocumentStillRendersTheSpaShell`; `SettingsView.spec.ts` (8 tabs aria-current, tracker snippet, DNS rows 3-good/1-warn, webhooks incl. failing Slack 410, notification 8x3 matrix) |
| B27 | `CampaignsApiIT.campaignsPayloadMatchesTheOracleThroughTheRealHttpLayer` (4 KPIs, 4 filters, 5 rows, row 0 typeLabel, row 4 sent="—"); `CampaignsView.spec.ts` (4 KPI tiles, 4 filter chips, one row per campaign, go-email intent, "Nowa kampania" real link) |
| B28 | `CampaignsApiIT.knownEmailPayloadMatchesTheOracleThroughTheRealHttpLayer` (10 blocks, 5 variables, 5 sections, selectedBlock hero_1), `newEmailPayloadMatchesTheOracleThroughTheRealHttpLayer`; `EmailEditorView.spec.ts` (library/envelope/document/inspector rendering, DEV-7 no-network-on-drag) |

## Deviations in scope

- DEV-7 (campaigns-email-editor: SPA makes no POST .../blocks) -- `EmailEditorView.spec.ts >
  "dragging a library block onto the canvas never issues a network request (DEV-7)"`: asserts
  the `fetch` mock's call count is unchanged after a simulated drag-and-drop.
- DEV-12 (settings step 10 = 404 document) -- `SettingsApiIT.unknownTabDocumentIsNotFoundThroughTheRealHttpLayer`
  ("B06/DEV-12 GET /pl/settings/nonexistent is a 404 document even though the tab shape matches
  the route table's settings pattern"): proves `SettingsController`'s exact mapping wins over
  `SpaDocumentController`'s wildcard, through the full context (a `@WebMvcTest` slice of
  `SettingsController` alone could not prove this).
- DEV-4 is a contract-dimension deviation (HTTP compare rule for `compare.mjs`); not applicable
  to the integration dimension's own pass/fail criteria.

## Wave-3 shell changes

| Change | Qualifying integration test |
| --- | --- |
| Locale toggle via `route.meta.defaultParams` (`frontend/src/router/localeHref.ts`, `Topbar.vue`) | `LocaleToggle.spec.ts`: mounts the real `AppLayout` (which renders `AppShell`/`Topbar`) with the real router and real Pinia, stubbing only `fetch` for the shell payload; asserts the toggle's `href` at `/pl/settings/account` (default tab -> `/en/settings`), `/pl/settings/billing` (non-default tab -> `/en/settings/billing`), and `/pl/customers/c_1001` (no `meta.defaultParams` -> unmodified prefix swap) |
| Reload scroll restoration (`frontend/src/router/scrollRestoration.ts`, `router/index.ts`) | `ScrollRestoration.spec.ts`: imports the real production router singleton (`@/router`, not a hand-built router) and asserts `router.options.scrollBehavior` is the module's own function, then invokes it with a real resolved route from an actual `router.push()`, proving both the wiring (`createRouter({ scrollBehavior })`) and the layout-stabilisation wait. This is a router-integration test (imports and exercises the real, singleton production router instance) rather than a mounted-component test; it does not mount a Vue SFC. Noted as a partial fit against the strict "mounts real views/components" wording of this dimension's qualifying-test definition -- it is not a `@WebMvcTest` slice or a prop-fed component test (the two explicitly excluded categories), and it is the *only* test at the integration level (as opposed to `frontend/test/unit/scrollRestoration.spec.ts`, which calls the exported helper directly and never touches `router/index.ts`) that proves vue-router itself was configured with the real `scrollBehavior` function. Recommend the orchestrator treat this as accepted evidence for the wiring claim specifically ("was `router/index.ts`'s createRouter really given this function"), not as evidence of the visible reload-scroll UX, which is instead covered by the contract/visual dimensions' own reload-related deviations (DEV-1/DEV-11 masks) and the e2e `navigation.spec.ts` run. |
