# Unit dimension — behaviour-to-test mapping (B01–B33)

Wave SHA `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`. Built by grepping/parsing every
`@DisplayName("Bnn …")` in `backend/src/test/java/**/*Test.java` (Surefire, run by
`./mvnw -q test`) and every `describe("Bnn …")`/`it("Bnn …")` in `frontend/test/unit/*.spec.ts`
(run by `npm run test -- --run`, which resolves to `vitest run test/unit`). `*IT.java` (Failsafe)
and `frontend/test/integration/*` were NOT run by these commands and are excluded from this
mapping even where they happen to carry the same `Bnn` tag — that coverage belongs to the
`integration` dimension, not `unit`.

Legend: ✅ = at least one green, Bnn-tagged unit test found. ❌ = no Bnn-tagged unit test found
anywhere in the unit-run scope (backend Surefire `*Test.java` + frontend `test/unit`).

| Behaviour | Journey | Unit coverage | Representative test(s) |
| --- | --- | --- | --- |
| B01 | shell-navigation (+ all pages) | ✅ | `RouteTableTest::…` "B01 every panel/public URL in the route table is known"; `SpaDocumentControllerTest::…` "B01 every panel/public URL renders 200 and carries the SPA document" |
| B02 | shell-navigation | ❌ | none in `*Test.java`/`test/unit`. Only in `ShellPagesIT.java` ("B02 every panel page's shell payload resolves the currentSection/crumb…", Failsafe/integration) and `frontend/test/integration/ShellNavigation.spec.ts` ("B02 every panel route carries the shell…", integration). The codebase's own Javadoc on `ShellPagesIT` explicitly documents this split: server half → `ShellPagesIT` (integration), client-rendered half (`.sidebar`/`.topbar`/`.main-scroll`/`aria-current` DOM) → `ShellNavigation.spec.ts` (integration). Neither is unit-run. |
| B03 | customers | ❌ | none in `*Test.java`/`test/unit`. Only in `CustomersApiIT.java` ("B03 GET /api/v1/pl/shell?route=customer_show reports currentSection \"customers\"…", integration) and `frontend/test/integration/CustomerDetailSidebarSection.spec.ts` ("B03 detail routes keep their index section active (integration)"). `ShellViewServiceTest.java` (unit) has an untagged test asserting the same underlying behaviour (`detailRouteKeepsItsIndexSectionCurrent`) but it carries no `B03` tag and is not named as the contract requires. |
| B04 | shell-navigation | ✅ | `ShellViewServiceTest::…` "B04 /en/... renders English navigation and page title" |
| B05 | customers | ✅ | `CustomersFixturesTest`, `CustomersControllerTest` — "B05 GET /api/v1/pl/customers/c_9999 is not found" (+ DEV-12 variant) |
| B06 | settings | ✅ | `SettingsViewServiceTest`, `SettingsFixturesTest`, `SettingsControllerTest` — "B06 …unknown tab…404" |
| B07 | shell-navigation | ✅ | `RouteTableTest` + one `B07` 404 test per journey controller (Automations/Campaigns/Customers/Dashboard/Events/Feeds/Import/Settings/SpaDocument/Widget ControllerTest) |
| B08 | shell-navigation | ✅ | `PreferencesServiceTest`, `PreferencesControllerTest` — "B08 …theme…persists" |
| B09 | shell-navigation | ✅ | `PreferencesServiceTest`, `PreferencesControllerTest` — "B09 unknown theme value falls back to light" |
| B10 | shell-navigation | ✅ | `PreferencesServiceTest`, `PreferencesControllerTest` — "B10 …sidebar…persists" |
| B11 | login | ✅ | `LoginServiceTest`, `LoginControllerTest` — "B11 valid e-mail sign-in…" |
| B12 | login | ✅ | `LoginServiceTest`, `EmailValidationTest`, `LoginControllerTest` — "B12 empty e-mail is rejected…" |
| B13 | login | ✅ | `LoginServiceTest`, `EmailValidationTest`, `LoginControllerTest` — "B13 malformed e-mail is rejected…" |
| B14 | login | ✅ | `LoginServiceTest`, `LoginControllerTest` — "B14 sign-out…" |
| B15 | event-stream | ✅ | `TrackedEventTest`, `CollectControllerTest` — "B15 event without idempotency_id is rejected" |
| B16 | event-stream | ✅ | `TrackedEventTest`, `CollectControllerTest` — "B16 unknown event type is rejected" |
| B17 | event-stream | ✅ | `EventIngestionServiceTest` — "B17 an accepted event is published once on /accounts/1/events…" |
| B18 | event-stream | ✅ | `EventIngestionServiceTest` — "B18 the same idempotency id is never published twice" |
| B19 | event-stream | ✅ | `CollectControllerTest` — "B19 a new event is accepted with 202…" / "…duplicate…" |
| B20 | scheduler-heartbeat | ✅ | `HeartbeatTriggerTest` — "B20/B33 with no prior lock row, the first-ever tick fires immediately" |
| B21 | shell-navigation | ❌ | no `B21` tag anywhere in the repository's test trees at all (unit, integration, or e2e file headers) — the oracle's own reference for B21 is `tests/e2e/specs/navigation.spec.ts` (e2e dimension); `frontend/test/unit/scrollRestoration.spec.ts` exercises the underlying scroll-restoration helper functions untagged, but does not assert the full B21 behaviour (sidebar navigation + only-`.main-scroll`-scrolls + reload persistence + locale switch + breadcrumb) end to end. |
| B22 | landing (+ login) | ✅ | `LandingFixturesTest`, `LandingControllerTest` (backend); `landing.spec.ts`, `LoginView.spec.ts` (frontend unit) |
| B23 | dashboard | ✅ | `DashboardViewServiceTest`, `DashboardControllerTest` (backend); `Cardiogram.spec.ts` (frontend unit) |
| B24 | event-stream | ✅ | `EventsViewServiceTest`, `EventsControllerTest` (backend); `EventRow.spec.ts`, `EventStream.spec.ts`, `Segmented.spec.ts`, `FilterChip.spec.ts` (frontend unit) |
| B25 | customers | ✅ | `CustomersViewServiceTest`, `CustomersFixturesTest`, `CustomersControllerTest` (backend); `customersComponents.spec.ts` (frontend unit) |
| B26 | automations | ✅ | `AutomationsViewServiceTest`, `AutomationsFixturesTest`, `AutomationsControllerTest` (backend); `automationsComponents.spec.ts` (frontend unit) |
| B27 | campaigns-email-editor | ✅ | `CampaignsViewServiceTest`, `CampaignsControllerTest` (backend) |
| B28 | campaigns-email-editor | ✅ | `CampaignsViewServiceTest`, `CampaignsControllerTest` (backend); `emailDocument.spec.ts`, `campaignsComponents.spec.ts` (frontend unit) |
| B29 | popups-widget-editor | ✅ | `WidgetViewServiceTest`, `WidgetControllerTest` (backend); `widgetComponents.spec.ts` (frontend unit) |
| B30 | feeds | ✅ | `FeedsViewServiceTest`, `FeedsControllerTest` (backend); `FeedCard.spec.ts` (frontend unit) |
| B31 | import-wizard | ✅ | `ImportViewServiceTest`, `ImportUploadStorageTest`, `ImportControllerTest`, `ImportUploadControllerTest` (backend); `importComponents.spec.ts` (frontend unit) |
| B32 | settings | ✅ | `SettingsViewServiceTest`, `SettingsFixturesTest`, `SettingsControllerTest` (backend); `settingsComponents.spec.ts`, `highlight.spec.ts`, `localeHref.spec.ts` (frontend unit) |
| B33 | scheduler-heartbeat | ✅ | `HeartbeatJobTest`, `HeartbeatTriggerTest` — "B33 tick() logs exactly…", "…routine restart…", "…downtime longer than the interval…" |

## Gap summary

Three behaviours have **no** Bnn-tagged test inside the unit-run scope (`./mvnw -q test` +
`npm run test -- --run`): **B02, B03, B21**. All three are deliberately covered one level up, at
`integration` (B02, B03: `*ApiIT.java` + `frontend/test/integration/*`) or `e2e` (B21:
`tests/e2e/specs/navigation.spec.ts`) — this is a documented, intentional split in the source
(see the Javadoc on `ShellPagesIT.java` and the header comment on
`frontend/test/integration/ShellNavigation.spec.ts`), not an accident. No entry in
`tools/migration-verify/deviations.json` (DEV-1..DEV-12, nor DEV-13 in the plan text) covers a
`unit`-dimension coverage gap — every deviation on file is for `visual`, `contract` or
`performance`. Per the plan's verifier contract for `unit`
("every behaviour in `behaviours.json` for the wave's journeys has a named test
(`@DisplayName("Bnn …")` / `describe("Bnn …")`)"), this is a genuine threshold miss for this
dimension, not an accepted deviation.

Journeys touched by the gap: **shell-navigation** (B02, B21) and **customers** (B03). All other
11 journeys have full Bnn-tagged unit coverage with 0 failing tests.
