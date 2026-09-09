# Repair-1 sweep — B01–B32 → unit-test map

Scope per `repair-1.md`: every behaviour id `behaviours.json` lists as B01–B32 must be reachable
by a **unit-level** test — `./mvnw -q test` (a `*Test.java` file; `*IT.java` is integration,
run by `mvnw verify`/failsafe, not counted here) or `npm run test -- --run`
(`frontend/test/unit/**`; `frontend/test/integration/**` is not counted here) — carrying a
`"Bnn ..."`-prefixed `@DisplayName`/`describe`/`it` string. B33 is out of this sweep's scope
(scheduler-heartbeat journey; repair-1.md bounds the sweep at B32).

Method: `grep -rn` every `*Test.java` and `frontend/test/unit/*.ts` for each `Bnn` id, backend and
frontend independently, before and after this repair. Only B02, B03 and B21 were missing a
unit-level match before this repair (matching the coordinator's finding exactly) — every other
id already had at least one unit-level test on both sides where the behaviour has both a backend
and a frontend half, or on whichever side actually owns it. No other gap was found.

| ID | Backend unit test (`*Test.java`) | Frontend unit test (`test/unit`) | Status |
| --- | --- | --- | --- |
| B01 | `web/SpaDocumentControllerTest.java:27` "B01 every panel/public URL renders 200 and carries the SPA document"; `domain/RouteTableTest.java:11` "B01 every panel/public URL in the route table is known" | `routes.spec.ts:7` describe "B01 every panel/public route in the table resolves" | pre-existing |
| B02 | `application/ShellViewServiceTest.java:55` `@ParameterizedTest` "B02 the crumb names every section, matching its own nav label" (9 cases, one per `NavigationCatalog` section) | `Topbar.spec.ts:31` describe "B02 the breadcrumb names the current section" (2 cases) | **added this repair** |
| B03 | `application/ShellViewServiceTest.java:74-75` "B03 a detail route keeps its index section current, mirroring Navigation::currentSection" | — (owned/backend-only; no frontend unit-level equivalent needed — the frontend-side proof is `Sidebar.spec.ts`'s "keeps a detail route's index section highlighted", integration-level, and `CustomerDetailSidebarSection.spec.ts`, also integration-level) | **labelled this repair** (test pre-existed since wave-2, unlabelled — GAP-1) |
| B04 | `application/ShellViewServiceTest.java:24` "B04 /en/... renders English navigation and page title" | `i18n.spec.ts:13` describe "B04 the English catalogue translates every Polish navigation and page-head key" | pre-existing |
| B05 | `fixtures/CustomersFixturesTest.java:38,44`; `web/CustomersControllerTest.java:75` (3 tests) | — (owned by customers; page-body behaviour, no frontend unit test needed — proven by `CustomersView.spec.ts`, integration) | pre-existing |
| B06 | `application/SettingsViewServiceTest.java:60`; `fixtures/SettingsFixturesTest.java:28`; `web/SettingsControllerTest.java:74` (3 tests) | — (owned by settings; same shape as B05) | pre-existing |
| B07 | `web/{Feeds,Widget,Automations,Campaigns,Customers,Dashboard,Events,Import,Settings}ControllerTest.java` (9 tests, one per journey's API) + `domain/RouteTableTest.java` | `routes.spec.ts:37` describe "B07 an unsupported locale prefix is unknown to the router" | pre-existing |
| B08 | `application/PreferencesServiceTest.java:18`; `web/PreferencesControllerTest.java:24` | — (proven end-to-end by `shellStore.spec.ts`, integration; `useIntents.spec.ts`, unit-adjacent but titled around the keepalive/reload behaviour, not tagged B08) | pre-existing |
| B09 | `web/PreferencesControllerTest.java:35`; `application/PreferencesServiceTest.java:29` | — | pre-existing |
| B10 | `application/PreferencesServiceTest.java:39`; `web/PreferencesControllerTest.java:46` | — (same as B08) | pre-existing |
| B11 | `web/LoginControllerTest.java:34`; `application/LoginServiceTest.java:16` | — (proven by e2e `public.spec.ts`) | pre-existing |
| B12 | `domain/EmailValidationTest.java:11`; `application/LoginServiceTest.java:38`; `web/LoginControllerTest.java:45` | — | pre-existing |
| B13 | `domain/EmailValidationTest.java:19`; `application/LoginServiceTest.java:47`; `web/LoginControllerTest.java:53` | — | pre-existing |
| B14 | `application/LoginServiceTest.java:56`; `web/LoginControllerTest.java:62` | — | pre-existing |
| B15 | `web/CollectControllerTest.java:84`; `domain/tracking/TrackedEventTest.java:17,25` | — (event-stream journey; proven by e2e `events.spec.ts`) | pre-existing |
| B16 | `web/CollectControllerTest.java:95`; `domain/tracking/TrackedEventTest.java:34` | — | pre-existing |
| B17 | `application/tracking/EventIngestionServiceTest.java:64` "B17 an accepted event is published once on /accounts/1/events with the translated type,…" | — | pre-existing |
| B18 | `application/tracking/EventIngestionServiceTest.java:126` "B18 the same idempotency id is never published twice" | — | pre-existing |
| B19 | `web/CollectControllerTest.java:58,71` | — | pre-existing |
| B20 | `infrastructure/scheduling/HeartbeatTriggerTest.java:26` "B20/B33 with no prior lock row, the first-ever tick fires immediately" (scheduler-heartbeat journey; shared label with B33, out of this sweep's scope) | — | pre-existing |
| B21 | — (structural DOM invariant; no backend counterpart) | `AppShell.spec.ts:38` describe "B21 only .main-scroll scrolls — the app shell's structural invariant" (2 cases: exactly-one-`.main-scroll`-and-content-is-inside-it, and no-inline-overflow-style) | **added this repair** |
| B22 | `fixtures/LandingFixturesTest.java` (5 tests) | `landing.spec.ts:88` describe "B22 the landing page structure (unit)"; `LoginView.spec.ts:24` describe "B22 the login form matches the browser-validated, server-rendered contract (unit)" | pre-existing |
| B23 | `application/DashboardViewServiceTest.java` (7 tests) | `Cardiogram.spec.ts` (4 tests) | pre-existing |
| B24 | `application/EventsViewServiceTest.java:15`; `web/EventsControllerTest.java:28` | `EventRow.spec.ts`, `EventStream.spec.ts` (5 tests), `FilterChip.spec.ts`, `Segmented.spec.ts` | pre-existing |
| B25 | `fixtures/CustomersFixturesTest.java:14`; `application/CustomersViewServiceTest.java:47`; `web/CustomersControllerTest.java` (3 tests) | `customersComponents.spec.ts` (Table, Pagination) | pre-existing |
| B26 | `fixtures/AutomationsFixturesTest.java` (3 tests); `application/AutomationsViewServiceTest.java`; `web/AutomationsControllerTest.java` | `automationsComponents.spec.ts` (FlowCanvas, RulePipeline, AutoCard) | pre-existing |
| B27 | `application/CampaignsViewServiceTest.java:58`; `web/CampaignsControllerTest.java:28,42` | — (page-body only; proven by e2e `lists.spec.ts`) | pre-existing |
| B28 | `application/CampaignsViewServiceTest.java:107`; `web/CampaignsControllerTest.java:72` | `campaignsComponents.spec.ts` (BlockLibrary, CouponCode); `emailDocument.spec.ts` | pre-existing |
| B29 | `application/WidgetViewServiceTest.java` (7 tests) | `widgetComponents.spec.ts` (7 describes: PopupStage, ShopMock, PopupWidget, PositionGrid, SwatchGrid, TriggerRow, AddSlot, PwTypeList) | pre-existing |
| B30 | `application/FeedsViewServiceTest.java` (3 tests) | `FeedCard.spec.ts` (3 tests) | pre-existing |
| B31 | `application/ImportViewServiceTest.java` (many); `web/Import{,Upload}ControllerTest.java`; `infrastructure/importing/ImportUploadStorageTest.java` | `importComponents.spec.ts` (Stepper, Dropzone) | pre-existing |
| B32 | `application/SettingsViewServiceTest.java` (many); `web/SettingsControllerTest.java` | `highlight.spec.ts`; `settingsComponents.spec.ts` (SettingsNav, ToggleRow, DnsRow, HookRow, CodeBlock); `localeHref.spec.ts` | pre-existing |

## Result

29 of 32 ids already had unit-level coverage before this repair. **B02, B03, B21 fixed** — 0 gaps
remain in the B01–B32 range.
