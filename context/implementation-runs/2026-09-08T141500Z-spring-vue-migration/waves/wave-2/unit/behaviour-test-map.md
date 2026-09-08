# Behaviour → named unit test map (wave-2 round 2, dimension unit)

SHA: b87a701244f5316e1a53bace7a1e41facdffc277
Scope (packet): B15–B19, B24 (event-stream); B03, B05, B25 (customers); B01 events/customers/profile rows.

| Behaviour | Oracle description | Named test(s) found | Suite / file | Result |
| --- | --- | --- | --- | --- |
| B01 | Every panel/public URL renders 200 and shows its own headline/marker text (scope: events/customers/profile rows only) | `@DisplayName("B01 every panel/public URL renders 200 and carries the SPA document")` (parameterized, includes `/pl/events`, `/pl/customers`, `/pl/customers/c_1`) | backend `click.kivvi.web.SpaDocumentControllerTest` | green |
| | | `describe("B01 every panel/public route in the table resolves")` (parameterized, includes `/pl/events`, `/pl/customers`, `/pl/customers/c_1`) | frontend `test/unit/routes.spec.ts` | green |
| B03 | Detail routes keep their index section active in the sidebar | `detailRouteKeepsItsIndexSectionCurrent()` — `@DisplayName("a detail route keeps its index section current, mirroring Navigation::currentSection")` | backend `click.kivvi.application.ShellViewServiceTest` | green — **see GAP-1**: DisplayName does not carry the "B03" tag used by every other behaviour-mapped test in this run |
| B05 | Unknown customer id -> 404 | `@DisplayName("B05 GET /api/v1/pl/customers/c_9999 is not found")`; `@DisplayName("B05/DEV-12 GET /pl/customers/c_9999 is a 404 document, not the 200 SPA shell")` | backend `click.kivvi.web.CustomersControllerTest` | green |
| B15 | Event without idempotency_id is rejected | `@DisplayName("B15 a missing idempotency_id answers 400 with the exact Polish message")` | backend `click.kivvi.web.CollectControllerTest` | green |
| B16 | Unknown event type is rejected | `@DisplayName("B16 an unknown type answers 400 with the exact Polish message")` | backend `click.kivvi.web.CollectControllerTest` | green |
| B17 | Accepted event is published once on /accounts/1/events as a rendered event-row with translated type and detail | `@DisplayName("B17 an accepted event is published once on /accounts/1/events with the translated type, detail, customer, site and the add_to_cart icon/tone from EventFeed::TYPES")` | backend `click.kivvi.application.tracking.EventIngestionServiceTest` | green |
| B18 | Same idempotency id is never published twice | `@DisplayName("B18 the same idempotency id is never published twice")` | backend `click.kivvi.application.tracking.EventIngestionServiceTest` | green |
| B19 | POST /collect answers 202, then 200 on replay, 400 on invalid payload | `@DisplayName("B19 a new event is accepted with 202 {\"status\":\"accepted\"}")`; `@DisplayName("B19 a duplicate event is answered 200 {\"status\":\"duplicate\"}")`; (400 cases covered by B15/B16 tests above, same class) | backend `click.kivvi.web.CollectControllerTest` | green |
| B24 | 30 rows, 9 type + 4 site + 4 range filters, collected event arrives as .event-row.new over SSE, replay not shown twice (unit-level decomposition: row rendering, subscription/prepend, filter chip, tab segment) | `it("B24 renders time, icon tone, type, detail, customer and site")` | frontend `test/unit/EventRow.spec.ts` | green |
| | | `it("B24 renders the initial 30 server-provided rows and subscribes to the given topic")`; `it("B24 flips data-stream-state to 'live' once the subscription opens")`; `it("B24 prepends a row from an SSE message and flags it 'new'")` | frontend `test/unit/EventStream.spec.ts` | green |
| | | `it("B24 renders the label, data-active, data-action and data-payload")` | frontend `test/unit/FilterChip.spec.ts` | green |
| | | `it("B24 renders one tab per option with role=tablist/tab and aria-selected")` | frontend `test/unit/Segmented.spec.ts` | green |
| B25 | 24 rows, 7 columns, pagination 'Strona 1 z 192', row opens profile, profile facts/scores/timeline/tabs, back link | `@DisplayName("B25 GET /api/v1/pl/customers returns 24 rows and the pager fields")`; `@DisplayName("B25 ?page= only moves the pager; the same 24 rows render on every page")`; `@DisplayName("B25 GET /api/v1/pl/customers/{id} returns the 360 profile shape")`; `@DisplayName("B25 GET /pl/customers/{id} renders the SPA document for a known customer")` | backend `click.kivvi.web.CustomersControllerTest` | green |
| | | `describe("B25 Table", ...)`; `it("B25 carries the row intent as data-action/data-payload...")`; `describe("B25 Pagination", ...)` | frontend `test/unit/customersComponents.spec.ts` | green |

## GAP-1 (non-blocking observation)

B03's only unit-level test, `ShellViewServiceTest.detailRouteKeepsItsIndexSectionCurrent()`, does not carry the
`@DisplayName("B03 …")` naming convention used by every other behaviour-mapped test in this run (B01, B04, B05,
B07, B15–B19, B24, B25 all do). The test's assertions and behaviour description are an exact semantic match for
B03 ("a detail route keeps its index section current, mirroring Navigation::currentSection" ==
"Detail routes keep their index section active in the sidebar"), it is green, and it is not `@Disabled`. Found
by content search (`currentSection`), not by the `Bnn` naming convention the contract describes. Not a
functional regression — behaviour is proven and passing — but a traceability/labeling gap against the letter of
the verifier contract's `@DisplayName("Bnn …")` pattern. Recommend the wave-2 repair loop add the "B03" prefix
to this DisplayName for consistency; does not block PASS since the test exists, is correctly asserting the
oracle behaviour, and is green.

## Coverage note

B03's *server-wiring* proof through the real router (`GET /api/v1/pl/shell?route=customer_show`) lives in
`click.kivvi.CustomersApiIT` (`*IT.java`, Testcontainers-backed) and its client-wiring proof lives in
`frontend/test/integration/CustomerDetailSidebarSection.spec.ts` — both out of scope for the **unit** dimension
by Maven Surefire/Failsafe convention (`*IT.java` runs only on `verify`) and by the frontend `test:integration`
script split; the **integration** dimension verifier covers those. The unit dimension's own proof is the pure
`ShellViewService` logic test above (GAP-1 noted).
