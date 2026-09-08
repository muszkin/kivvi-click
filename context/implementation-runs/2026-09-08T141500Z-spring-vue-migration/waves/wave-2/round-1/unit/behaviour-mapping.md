# Wave-2 unit dimension — behaviour -> executed unit-command test mapping

Wave SHA 22d7fcb6380723728a33fc21fda22a92594a2e88. Checkout /home/muszkin/work/kivvi-click-wt/verify-wave-2-unit.
Unit commands: backend `./mvnw -q test` (JAVA_HOME=jdk-25) and frontend `npm ci && npm run test -- --run`
(script = `vitest run test/unit`). Neither command executes `*IT.java` (Failsafe-only) or
`frontend/test/integration/*` files — confirmed empirically: surefire-reports/ contains only the
28 `*Test.java` classes, none of the 8 `*IT.java` classes.

## event-stream journey

| Behaviour | Required test (oracle) | Executed unit-command test found | Verdict |
| --- | --- | --- | --- |
| B15 | idempotency_id missing -> 400 | `CollectControllerTest::missingIdempotencyIdAnswers400` (@DisplayName "B15...") + `TrackedEventTest::eventWithoutIdempotencyIdIsRejected` (@DisplayName "B15...") — both green, assert exact Polish error text | covered |
| B16 | unknown event type -> 400 | `CollectControllerTest::unknownTypeAnswers400` (@DisplayName "B16...") + `TrackedEventTest::unknownEventTypeIsRejected` — both green, assert exact Polish error text | covered |
| B17 | accepted event published once as translated event-row (time/type/typeIcon/tone/detail/customer/site) | **none in unit-command output.** The only test exercising `EventIngestionService.ingest()`/`toEnvelope()` end-to-end is `CollectApiIT::acceptedEventIsPublishedAsJson` (@DisplayName "DEV-3...", `backend/src/test/java/click/kivvi/CollectApiIT.java`) — an `*IT.java` Testcontainers test bound to Failsafe, not run by `mvn test`. `CollectControllerTest` stubs `EventIngestionService.ingest()` entirely (never calls the real `toEnvelope()`); `HttpMercurePublisherTest` only proves generic form-encoded/JWT transport, not the translated envelope content; no `EventIngestionServiceTest.java` exists. **GAP** | **not covered** |
| B18 | same idempotency id never published twice | **none in unit-command output.** `EventDedupStore.claim()` semantics are proven only by `EventDedupStoreIT` (@DisplayName "B18...", `infrastructure/tracking/EventDedupStoreIT.java`), a Testcontainers `*IT.java` file. `EventDedupCleanupJobTest` covers the sweep/cleanup job, not claim/dedup itself. `CollectControllerTest`'s "duplicate" test stubs the ingestion result directly, never exercising the real store. **GAP** | **not covered** |
| B19 | POST /collect: 202 accepted, 200 duplicate, 400 invalid | `CollectControllerTest::acceptedEventAnswers202` + `::duplicateEventAnswers200` (both @DisplayName "B19...", green) together with B15/B16's 400 tests cover all three status branches | covered |
| B24 | 30 rows, 9 type + 4 site filters, translations, SSE prepend | `EventsViewServiceTest` (9 tests, incl. "B24 30 rows, 9 type filters..."), `EventsControllerTest::eventsPayloadShape` ("B24 GET /api/v1/pl/events..."), frontend `EventStream.spec.ts` (3 "B24" tests: initial 30 rows/subscribe, live-state flip, SSE prepend+flag new), `EventRow.spec.ts`, `Segmented.spec.ts`, `FilterChip.spec.ts` — all green | covered |
| B01 (events row) | `/pl/events` renders 200 | `SpaDocumentControllerTest::everyKnownRouteRenders200["/pl/events"]` (@DisplayName "B01...", parameterized, green); `RouteTableTest::everyKnownRouteMatches` resolves `/pl/events` -> `events` | covered |

Event-stream verdict: **regression** — B17 and B18 lack any executed unit-command test; no deviation in `tools/migration-verify/deviations.json` (DEV-1..DEV-12) covers the unit dimension for either (DEV-3 only covers the `contract` dimension and explicitly cites the `*IT` test, not a unit-command one).

## customers journey

| Behaviour | Required test (oracle) | Executed unit-command test found | Verdict |
| --- | --- | --- | --- |
| B03 | detail routes keep index section active (`customer_show` -> `customers`) | `ShellViewServiceTest::detailRouteKeepsItsIndexSectionCurrent` — builds the shell for route "customer_show" and asserts `view.currentSection()).isEqualTo("customers")`; green, falsifiable (fails if the section-mapping logic regresses) | covered |
| B05 | unknown customer id -> 404 | `CustomersControllerTest::unknownCustomerApiIsNotFound` + `::unknownCustomerDocumentIsNotFound` (@DisplayName "B05..."), `CustomersFixturesTest::byIdThrowsForAnUnknownId` (@DisplayName "B05...") — all green | covered |
| B25 | 24 rows/7 cols, page 1 of 192, profile facts/scores/timeline/tabs, back link | `CustomersViewServiceTest` (8 tests), `CustomersControllerTest` (7 tests), `CustomersFixturesTest` (4 tests) — all tagged "B25", green; frontend `customersComponents.spec.ts` (Table/Pagination, "B25") — green | covered |
| B01 (customers + profile rows) | `/pl/customers` and `/pl/customers/c_1` render 200 | `SpaDocumentControllerTest::everyKnownRouteRenders200["/pl/customers"]` and `["/pl/customers/c_1"]` (@DisplayName "B01...", green); `RouteTableTest` resolves both to `customers`/`customer_show` | covered |

Customers verdict: **parity** — every mapped behaviour has an executed, green, non-tautological unit-command test.

## Regression check — earlier waves (wave-0, wave-1 journeys)

The commands above run the entire backend/frontend unit suites unconditionally (not scoped to
wave-2 journeys): 28 backend `*Test.java` classes / 145 tests and 14 frontend `test/unit/*.spec.ts`
files / 74 tests, all green, 0 failures/errors. This includes the wave-0/wave-1 journeys'
behaviours (shell-navigation, login, dashboard, landing, scheduler-heartbeat, settings-adjacent
fixtures, etc. — e.g. `RouteTableTest`, `LoginServiceTest`, `LoginControllerTest`,
`PreferencesServiceTest`/`PreferencesControllerTest`, `LandingFixturesTest`,
`HeartbeatJobTest`/`HeartbeatTriggerTest`, `ArchitectureTest`, `i18n.spec.ts`, `landing.spec.ts`,
`routes.spec.ts`, `LoginView.spec.ts`), all still passing at this wave SHA.
