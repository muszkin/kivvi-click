# Wave-2 integration — behaviour → test mapping

Wave SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`. Only `*IT` classes (`@SpringBootTest` +
`@Testcontainers` + real `TestRestTemplate` HTTP) and `frontend/test/integration/*.spec.ts`
(real Vue Router / Pinia / mounted components) count. A `@WebMvcTest` slice or a component test
that receives the already-computed prop instead of exercising router/store wiring does not count.

## event-stream (B15–B19, B24)

| Behaviour | Qualifying test found? | Evidence |
| --- | --- | --- |
| B15 missing/blank `idempotency_id` → 400 exact PL message | **Partial, unlabelled** | `CollectApiIT.acceptedThenDuplicateThenBadRequest` (`@DisplayName` says B19) asserts, as its third step, `POST /collect {"type":"purchase"}` → 400, `error == "Pole „idempotency_id” jest wymagane."`. Real Spring context + Testcontainers Postgres + real HTTP. The assertion is present and concrete, but the test's own name cites B19 only — B15 is not named anywhere against an `*IT` test. |
| B16 unknown event type → 400 exact PL message | **Missing** | No `*IT` class exercises an unknown-`type` payload. Only `CollectControllerTest.unknownTypeAnswers400` (`@WebMvcTest(CollectController.class)`, disqualified) and `TrackedEventTest` (pure domain unit, no Spring context at all) cover it. `grep -rn "Nieznany typ\|teleport" backend/src/test --include="*IT.java"` → no matches. |
| B17 accepted event published once as translated `type`/`detail` | **Yes** | `CollectApiIT.acceptedEventIsPublishedAsJson` (`@DisplayName` cites DEV-3, not B17, but the assertions are exactly B17's content) — real HTTP POST, real Postgres, and a local stub HTTP hub swapped in via `kivvi.mercure.url` (`@DynamicPropertySource`), so the publish call itself crosses a real HTTP boundary; only the far end (the real Mercure daemon) is replaced. Asserts topic `/accounts/1/events`, `"type":"Zakup"`, `"siteColor":"#7a8763"`, `"customerName":"Hania Kowalska"`, exact `detail`, and that the payload is JSON (`doesNotContain("event-row")`/`"<div"`) per DEV-3. |
| B18 same idempotency id never published twice | **Weak** | `EventDedupStoreIT` (`@SpringBootTest` + Testcontainers Postgres, real `event_dedup` table) proves `claim()` refuses a live duplicate and reclaims an expired row — real DB, but calls the store directly, not through `POST /collect`. `CollectApiIT`'s B19 test proves the HTTP-level 202→200 status split on replay but never re-reads `PUBLISHED_BODIES` after the replay call to prove the hub was *not* called a second time — the "never published twice" half of B18 has no HTTP-level assertion anywhere. |
| B19 `POST /collect` 202→200→400 | **Yes** | `CollectApiIT.acceptedThenDuplicateThenBadRequest` — real HTTP, real Testcontainers Postgres, correctly named and asserted (202/"accepted", 200/"duplicate", 400/exact PL message). |
| B24 30 rows, 9+4 filters, 4 ranges (backend payload) | **Yes** | `EventsApiIT.eventsPayloadMatchesTheOracleThroughTheRealHttpLayer` — real HTTP GET `/api/v1/pl/events`, real Postgres, asserts 30 events, 9 type filters, 4 site filters, 4 ranges, `total`, `shown`, `mercureTopic`. |
| B24 SSE subscriber component/router integration (frontend) | **Missing** | `frontend/test/integration/EventsView.spec.ts` defines a `FakeEventSource` class, `vi.stubGlobal("EventSource", FakeEventSource)`, and tracks `FakeEventSource.instances`, but **no test in the file ever asserts on `instances`** (`grep -n "instances\[" EventsView.spec.ts` → 0 matches) or calls `.onmessage(...)` to simulate an arriving event. All 5 `it()` blocks only exercise the initial `fetch` payload (30 rows/filters/ranges) — the identical ground already covered by `EventsApiIT`. Nothing in `test/integration/` proves the component actually opens an `EventSource` against the right topic, handles a live message, or suppresses a replayed `idempotency_id`. The genuine SSE-with-fake-`EventSource` message-handling test lives in `test/unit/EventStream.spec.ts`, which is unit-dimension scope, not integration. |

## customers (B03, B05, B25) and B01 rows

| Behaviour | Qualifying test found? | Evidence |
| --- | --- | --- |
| B01 events/customers/profile document rows render 200 | **Yes** | `EventsApiIT.eventsPageRenders200` (`GET /pl/events`), `CustomersApiIT.customersIndexPageRenders200` (`GET /pl/customers`), `CustomersApiIT.knownCustomerDocumentStillRendersTheSpaShell` (`GET /pl/customers/c_1000`) — all real HTTP + real Spring context + Testcontainers. `@DisplayName`s cite B01/B25 respectively; content is accurate even where the label says B25. |
| B03 detail route keeps its index section active in the sidebar | **Missing** | Backend: `NavigationCatalog.currentSection()` (the `customer_show → customers` mapping) is a pure function with no `*IT` exercising it through `GET /api/v1/{locale}/shell?route=customer_show` — `ShellApiIT` only ever requests `route=dashboard`. Frontend: `test/integration/Sidebar.spec.ts` `"keeps a detail route's index section highlighted (customer_show -> customers)"` mounts `Sidebar` with `current: "customers"` passed **directly as a hardcoded prop** — it never runs a router navigation or the shell store's `load()` against a customer-detail route, so it never proves the app actually *computes* "customers" when on `/pl/customers/c_1000"; it only proves the presentation component highlights whatever prop it is handed. `test/integration/CustomerView.spec.ts` mounts only `CustomerView` (no `Sidebar`/`AppShell`) and stubs the customer-detail fetch directly, so it never touches `currentSection` either. No test connects router → shell load(route) → currentSection → Sidebar highlighting for a detail route. |
| B05 unknown customer id → 404 (API and document) | **Yes** | `CustomersApiIT.unknownCustomerApiIsNotFoundThroughTheRealHttpLayer` (`GET /api/v1/pl/customers/c_9999` → 404) and `.unknownCustomerDocumentIsNotFoundThroughTheRealHttpLayer` (`GET /pl/customers/c_9999` → 404, proving `CustomersController`'s exact route wins over the SPA wildcard) — both real HTTP, real Spring context, real Postgres. |
| B25 24 rows/7 cols/`Strona 1 z 192`, profile facts/scores/timeline/tabs, back link | **Yes** | Backend: `CustomersApiIT.customersListPayloadMatchesTheOracleThroughTheRealHttpLayer` (24 rows, 6 segments, page 1/192) and `.customerDetailPayloadMatchesTheOracleThroughTheRealHttpLayer` (name "Anna K.", 7 facts, 3 automations, 5 tabs, 3 scores, 9 timeline) — real HTTP. Frontend: `test/integration/CustomerView.spec.ts` mounts `CustomerView` behind a real `vue-router` instance and asserts 7 facts, `VIP` chip, 3 KPIs, 9 timeline entries, 5 tabs (first selected), and a real `<a href="/pl/customers">` back link — 5/5 tests green. `test/integration/CustomersView.spec.ts` covers the list side similarly (not re-verified line-by-line here; file present and green in the 8/8 suite run). |

## Naming-honesty spot check

- `CollectApiIT.acceptedEventIsPublishedAsJson` is labelled "DEV-3 …" — content matches B17 but the
  name never says so; not dishonest, just unindexed.
- `CustomersApiIT`/`EventsApiIT` use B25/B01/B05/B07 labels that match their bodies exactly — no
  overclaiming found.
- `Sidebar.spec.ts`'s second test is titled "keeps a detail route's index section highlighted" —
  this name overstates what the test proves: it hands the component the answer (`current:
  "customers"`) rather than deriving it from a route, so the title implies an integration the test
  does not perform.
- `EventsView.spec.ts` sets up `FakeEventSource` machinery that is never asserted against — dead
  scaffolding that reads as SSE coverage but isn't exercised.

## Earlier-wave regression check

All 9 `*IT` classes ran under the single `./mvnw -q verify` invocation and are green (0
failures/errors/skipped across 34 tests) — this includes wave-0/1 classes `LandingApiIT` (4),
`FeedsApiIT` (3), `ShellApiIT` (7), `SessionRoundTripIT` (4), `HeartbeatSchedulerIT` (1), none of
which regressed. All 8 frontend integration spec files (36 tests) are green, including wave-0/1
files `landing.spec.ts`, `LoginView.spec.ts`, `shellStore.spec.ts`, `FeedsView.spec.ts`,
`Sidebar.spec.ts`.
