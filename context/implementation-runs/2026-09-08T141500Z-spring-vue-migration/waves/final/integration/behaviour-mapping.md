# Behaviour -> qualifying integration-dimension test mapping (round 2, FINAL cohort, dimension: integration)

Qualifying test = real-HTTP `*IT.java` (Testcontainers, run via `./mvnw -q verify`) or
`frontend/test/integration/*.spec.ts` mounting real views/components with the real
router/store, stubbing only the network. `@WebMvcTest` slices, application-layer unit
tests, and frontend `test/unit` specs do NOT qualify for this dimension, even when they
carry the same `Bnn` tag. Built by grepping `backend/src/test/java/click/kivvi/**/*IT.java`
and `frontend/test/integration/**/*.ts` for literal `Bnn` tokens in `@DisplayName`/`describe`/
`it` titles ("map BY NAME").

| Behaviour | Qualifying test(s) found BY NAME | Verdict |
| --- | --- | --- |
| B01 | `ShellPagesIT` (52 tests, "every one of PanelPagesTest.php's 25 URLs"); individual `B01 GET ...` tests in `AutomationsApiIT`, `CampaignsApiIT`, `CustomersApiIT`, `DashboardApiIT`, `EventsApiIT`, `FeedsApiIT`, `ImportApiIT`, `LandingApiIT` (indirectly via B22 test), `SettingsApiIT` (x8 tabs), `WidgetApiIT`; frontend `ShellNavigation.spec.ts`, `SettingsView.spec.ts`, `ImportView.spec.ts`, `EmailEditorView.spec.ts`, `PopupEditorView.spec.ts`, `AutomationEditorView.spec.ts`, `CampaignsView.spec.ts` | mapped |
| B02 | `ShellPagesIT::...` ("every panel page's shell payload resolves the currentSection/crumb"); frontend `ShellNavigation.spec.ts` ("B02 every panel route carries the shell...") | mapped |
| B03 | `AutomationsApiIT` ("B03 GET /api/v1/pl/shell?route=automation_edit..."), `CustomersApiIT` ("B03 GET /api/v1/pl/shell?route=customer_show..."); frontend `CustomerDetailSidebarSection.spec.ts` ("B03 detail routes keep their index section active (integration)") | mapped |
| B04 | `ShellApiIT` ("B04 GET /api/v1/en/shell...", "B04 GET /api/v1/pl/shell..."), `ShellPagesIT` ("B04 GET /en/dashboard renders 200...") | mapped |
| B05 | `CustomersApiIT` ("B05 GET /api/v1/pl/customers/c_9999 is not found...", "B05/DEV-12 GET /pl/customers/c_9999 is a 404 document...") | mapped |
| B06 | `SettingsApiIT` ("B06 GET /api/v1/pl/settings/nonexistent...", "B06/DEV-12 ..."); frontend `SettingsView.spec.ts` ("B06 an unknown tab never renders a page body") | mapped |
| B07 | `CampaignsApiIT`, `DashboardApiIT`, `EventsApiIT`, `FeedsApiIT`, `LandingApiIT`, `WidgetApiIT`, `ShellApiIT` (x2), `ShellPagesIT` — each "B07 GET .../de/... is not found" | mapped |
| B08 | `SessionRoundTripIT` ("B08 a stored theme preference survives a second request via the JDBC session"); frontend `ShellNavigation.spec.ts` ("B08/B10 the shell store re-hydrates...") | mapped |
| B09 | none found | **GAP** (see note 1) |
| B10 | `SessionRoundTripIT` ("B10 a stored sidebar preference survives..."); frontend `ShellNavigation.spec.ts` ("B08/B10 ...") | mapped |
| B11 | `SessionRoundTripIT` ("B11 sign-in writes a real spring_session row...", "B11/B14 sign-in then sign-out..."); frontend `LoginView.spec.ts` ("B11 the login view shows the welcome copy...") | mapped |
| B12 | `ShellApiIT` ("B12 POST /pl/login with an empty address is rejected..."); frontend `LoginView.spec.ts` ("B12 renders the empty-e-mail error...") | mapped |
| B13 | `ShellApiIT` ("B13 POST /pl/login with a malformed address is rejected...") | mapped |
| B14 | `SessionRoundTripIT` ("B11/B14 sign-in then sign-out round-trips the identity...") | mapped |
| B15 | `CollectApiIT` ("B15 a missing idempotency_id answers 400...") | mapped |
| B16 | `CollectApiIT` ("B16 an unknown event type answers 400...") | mapped |
| B17 | none tagged `B17`; substantively covered (untagged) by `CollectApiIT::acceptedEventIsPublishedAsJson` ("DEV-3 the accepted event is published to the hub as JSON, not server-rendered HTML" — asserts exactly-once publish, topic `/accounts/1/events`, translated type/detail/customer fields) | **GAP (by name)**, deviation-covered (see note 2) |
| B18 | `CollectApiIT` ("B18 a live duplicate is answered 200..."), `EventDedupStoreIT` ("B18 a fresh idempotency id is claimed and recorded", "B18 the same idempotency id is never claimed twice...") | mapped |
| B19 | `CollectApiIT` ("B19 POST /collect: 202 accepted, then 200 duplicate, then 400...") | mapped |
| B20 | `HeartbeatSchedulerIT` ("B20/B33 the scheduler ticks once on a fresh shedlock table, persists exactly one shedlock row, and lockAtLeastFor blocks a second attempt within the same window") | mapped |
| B21 | none found | **GAP** (see note 1) |
| B22 | `LandingApiIT` ("B22 GET /api/v1/en/landing...", "B22 GET /pl/demo redirects to /pl/dashboard..."); frontend `LoginView.spec.ts`, `landing.spec.ts` ("B22 the home route resolves to the landing view...") | mapped |
| B23 | `DashboardApiIT` ("B23 GET /api/v1/pl/dashboard: 4 KPIs..."); frontend `DashboardView.spec.ts` ("B23 the dashboard matches the oracle...") | mapped |
| B24 | `EventsApiIT` ("B24 GET /api/v1/pl/events: 30 rows..."); frontend `EventsView.spec.ts` ("B24 the event stream page matches the oracle...", "B24 the live event stream: connecting -> live...") | mapped |
| B25 | `CustomersApiIT` (4 tests); frontend `CustomersView.spec.ts`, `CustomerView.spec.ts` ("B25 the 360 profile matches the oracle...") | mapped |
| B26 | `AutomationsApiIT` (4 tests); frontend `AutomationsView.spec.ts`, `AutomationEditorView.spec.ts` | mapped |
| B27 | `CampaignsApiIT` ("B27 GET /api/v1/pl/campaigns: 4 KPIs, 4 filters, 5 rows..."); frontend `CampaignsView.spec.ts` ("B01/B27 renders 4 KPI tiles and 4 filter chips...") | mapped |
| B28 | `CampaignsApiIT` (4 tests); frontend `EmailEditorView.spec.ts` | mapped |
| B29 | `WidgetApiIT` (7 tests); frontend `PopupsView.spec.ts`, `PopupEditorView.spec.ts` | mapped |
| B30 | `FeedsApiIT` ("B30 GET /api/v1/pl/feeds: 4 sources..."); frontend `FeedsView.spec.ts` | mapped |
| B31 | `ImportApiIT` (5 tests); frontend `ImportView.spec.ts` ("B31/B01 the import wizard matches the oracle...") | mapped |
| B32 | `SettingsApiIT` ("B32 GET /api/v1/pl/settings/account: 8 tabs...", "B32 GET /pl/settings (no tab) still renders..."); frontend `SettingsView.spec.ts` | mapped |
| B33 | `HeartbeatSchedulerIT` (same test as B20, see above) | mapped |

30/33 mapped BY NAME. Gaps: B09, B17 (partial — see note 2), B21.

## Notes on the three gaps

1. **B09 and B21 have no qualifying integration-dimension test at all** (real-HTTP IT or
   frontend/test/integration). Both are covered only at the unit layer:
   - B09 ("Unknown theme value falls back to light"): `PreferencesServiceTest`
     (application-layer unit test) and `PreferencesControllerTest` (`@WebMvcTest` slice,
     explicitly excluded from this dimension's qualifying-test definition).
   - B21 ("only `.main-scroll` scrolls..."): `frontend/test/unit/AppShell.spec.ts`
     (`describe("B21 only .main-scroll scrolls — the app shell's structural invariant", ...)`).
   This is **by design**, not a regression: the plan's Coverage matrix
   (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md` § Coverage matrix, row
   "B01–B14, B21, B22 (login)") assigns this whole behaviour range to "JUnit shell/session
   tests, Vitest shell components" — unit-dimension test types, not Testcontainers IT /
   frontend-integration tests. The round-2 note on this packet itself says round 1's
   shell-navigation label repair added/fixed **unit** tests for B02/B03/B21, confirming B21
   is intentionally unit-owned. The `unit` dimension's own verifier (parallel run, not read
   here) is the correct place to confirm B09/B21 have a green named unit test; this
   dimension confirms they are absent at the integration layer as expected.

2. **B17** ("Accepted event is published once on `/accounts/1/events` as a rendered
   event-row with translated type and detail") has no test whose `@DisplayName` literally
   contains `B17`. It is substantively verified by `CollectApiIT::acceptedEventIsPublishedAsJson`
   (`@DisplayName("DEV-3 the accepted event is published to the hub as JSON, not
   server-rendered HTML")`), a real-HTTP Testcontainers test that asserts: the hub receives
   exactly one more publish after the request, on topic `/accounts/1/events`, with the JSON
   payload containing the translated type (`"type":"Zakup"`), detail, site colour and
   customer name — i.e. the DEV-3-adapted shape of the same behaviour. This is exactly the
   test `tools/migration-verify/deviations.json`'s own `DEV-3` entry names as the backend
   verification vehicle for this exact behavioural change ("`backend/src/test/java/click/kivvi/CollectApiIT.java`'s
   'DEV-3 the accepted event is published to the hub as JSON, not server-rendered HTML' test").
   Classified as accepted-deviation (DEV-3), not a regression: the coverage exists and is
   correct, only the literal `Bnn` tag is missing from the test name.

## Discrepancy noted (not scored against this dimension)

The packet text says "the whole table DEV-1..DEV-13 as recorded in
`tools/migration-verify/deviations.json` on the final SHA". The file at that path on this
SHA (`dae169614a52532c130bd34435994d6a914165c0`) contains only `DEV-1` through `DEV-12` —
there is no `DEV-13` entry. Recorded for the record; none of this dimension's qualifying
tests reference a `DEV-13`, so it does not change any verdict here.
