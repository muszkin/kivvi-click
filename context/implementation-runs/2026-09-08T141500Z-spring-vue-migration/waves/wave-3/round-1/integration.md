# Wave-3 verifier — dimension: integration

**Wave SHA:** `32a68311594e611aaa8eaa0803bc72bba7d735a9` (checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration`, detached, unmodified)
**Journeys:** automations, settings, campaigns-email-editor (login, landing, feeds, event-stream, customers, scheduler-heartbeat run as regression guards only)

## Dimension status: **FAIL** (bound to `32a68311594e611aaa8eaa0803bc72bba7d735a9`)

All executed qualifying integration tests are green (0 failures across 58 backend `*IT.java` tests
and 83 frontend `test/integration` tests). The FAIL is a coverage-mapping gap, not a functional
regression: two of the six in-scope behaviour rows (**B01/settings**, **B01/email-editor**) have no
qualifying integration-dimension test whose name maps to `B01`, per the qualifying-test definition
given for this run (`*IT.java` under `./mvnw verify`, or `test/integration/*.spec.ts` mounting a
real component with real router, stubbing only the network boundary — `@WebMvcTest` slices and
prop-fed component tests do not qualify). See "B01 mapping gap" below.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | **parity** | B26 and B01/automations row both fully covered by name, all green. |
| settings | **parity**, with one **mapping gap** (B01) | B06 and B32 fully covered by name, all green. B01/settings row has no qualifying named test (see below); functionally exercised (200 responses) under B32/B06-tagged tests instead. |
| campaigns-email-editor | **parity**, with one **mapping gap** (B01) | B27 and B28 fully covered by name, all green, including a DEV-7 regression test. B01/email-editor row has no qualifying named test (see below); functionally exercised (200 responses) under B28-tagged tests instead. |
| login, landing, feeds, event-stream, customers, scheduler-heartbeat (regression guards) | **parity** | All earlier-wave `*IT.java` and `test/integration/*.spec.ts` still green — no regression from the wave-3 shell changes. |

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify -Pintegration -Dmaven.repo.local=/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/.m2-repo` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |
| `npm run test:integration -- --reporter=verbose` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |

(`-Pintegration` is not a defined Maven profile in `backend/pom.xml` — Maven ignores the unknown
`-P` activation request with a warning and proceeds; the failsafe plugin's default binding already
runs every `*IT.java` on the `integration-test`/`verify` phases, which is what actually matters for
this dimension. Testcontainers pulled/started a fresh `postgres:18-alpine` container per IT class,
consistent with the plan's Testcontainers Postgres 18 requirement.)

## Backend: `*IT.java` results (Testcontainers Postgres 18, real HTTP via `TestRestTemplate` against a
booted `@SpringBootTest(webEnvironment = RANDOM_PORT)` context)

All 12 IT classes green, 58 tests, 0 failures, 0 errors (see `integration/failsafe-reports/*.txt`):

| Class | Tests | Result | In-scope behaviours it names |
| --- | --- | --- | --- |
| `AutomationsApiIT` | 7 | green | B01/B26 (×3), B26 (×3), B03 |
| `SettingsApiIT` | 5 | green | B32 (×3), B06 (×2, incl. DEV-12) |
| `CampaignsApiIT` | 7 | green | B01 (campaigns row), B27, B28 (×4), B07 |
| `LandingApiIT` | 4 | green | regression guard (landing) |
| `FeedsApiIT` | 3 | green | regression guard (feeds) |
| `EventsApiIT` | 3 | green | regression guard (event-stream) |
| `CustomersApiIT` | 7 | green | regression guard (customers) |
| `CollectApiIT` | 6 | green | regression guard (event ingestion) |
| `SessionRoundTripIT` | 4 | green | regression guard (login/session) |
| `ShellApiIT` | 7 | green | regression guard (shell/navigation) |
| `HeartbeatSchedulerIT` | 1 | green | regression guard (scheduler-heartbeat) |
| `EventDedupStoreIT` | 4 | green | regression guard (event dedup) |

Full class-by-class summaries: `integration/failsafe-reports/click.kivvi.*.txt`. Full console log:
`integration/backend-mvnw-verify.log`.

## Frontend: `test/integration/*.spec.ts` results (Vitest + `@vue/test-utils`, real
`createRouter`/real route table/real i18n, `fetch` stubbed at the network boundary only)

14 test files, 83 tests, 0 failures (see `integration/frontend-test-integration.log`):

| File | In-scope behaviours it names |
| --- | --- |
| `AutomationsView.spec.ts` | B26 (×6) |
| `AutomationEditorView.spec.ts` | B26 (×7), B01/B26 (×1) |
| `SettingsView.spec.ts` | B32 (×11), B06 (×1) |
| `CampaignsView.spec.ts` | B27 (×5) |
| `EmailEditorView.spec.ts` | B28 (×7, incl. the DEV-7 drag-drop-issues-no-network-request test) |
| `shellStore.spec.ts`, `Sidebar.spec.ts`, `CustomerDetailSidebarSection.spec.ts`, `LoginView.spec.ts`, `CustomersView.spec.ts`, `CustomerView.spec.ts`, `landing.spec.ts`, `FeedsView.spec.ts`, `EventsView.spec.ts` | regression guards (earlier-wave journeys) |

## B01 mapping gap (drives the FAIL)

Behaviour B01 ("every panel/public URL renders 200 and shows its own headline/marker text") is
scoped for this wave to 4 rows: automations, settings, campaigns, email-editor.

- **automations row** — named and qualifying: `AutomationsApiIT` (`B01/B26 GET /pl/automations …`,
  `B01/B26 GET /pl/automations/new …`, `B01/B26 GET /pl/automations/a99 …`) and
  `AutomationEditorView.spec.ts` (`B01/B26 GET /automations/new renders and shows its own
  headline …`). All green.
- **campaigns row** — named and qualifying on the backend: `CampaignsApiIT` ("B01 GET
  /pl/campaigns renders 200 through the real HTTP layer"), green. No frontend test carries an
  explicit `B01` tag for the campaigns index; it is functionally exercised (route renders, payload
  fetched) under the `B27`-tagged tests in `CampaignsView.spec.ts`.
- **settings row** — **no qualifying test carries a `B01` tag.** `SettingsApiIT` renders
  `/pl/settings/account` (200) and `/pl/settings` (200) under `B32`-tagged tests, and
  `/pl/settings/nonexistent` (404) under a `B06/DEV-12`-tagged test — all real-HTTP-layer,
  Testcontainers-backed, and green — but none is named `B01`. The only test in the repository
  explicitly tagged `"B01 every panel/public URL renders 200 and carries the SPA document"` that
  covers `/pl/settings` is `SpaDocumentControllerTest` (`backend/src/test/java/click/kivvi/web/
  SpaDocumentControllerTest.java`), a `@WebMvcTest` slice — explicitly disqualified as an
  integration-dimension test by this run's qualifying-test definition.
- **email-editor row** — **no qualifying test carries a `B01` tag.** `CampaignsApiIT` renders
  `/pl/emails/k1` (200) and `/pl/emails/k999` (200, unseeded-but-shape-valid id) under `B28`-tagged
  tests — real-HTTP-layer, Testcontainers-backed, green — but none is named `B01`. Again, the only
  `B01`-named coverage of `/pl/emails/*` is in the disqualified `SpaDocumentControllerTest`
  `@WebMvcTest` slice.

Net effect: the underlying behaviour (settings and email-editor pages return 200 through a real
Spring context with a real database) **is** proven by qualifying integration tests today — just
filed under sibling behaviour tags (`B32`/`B06`, `B28`) rather than `B01`. This is a traceability
gap against the "map every behaviour id in scope to a qualifying test by name" bar, not a product
defect. Recommend either adding an explicit `B01`-tagged assertion (mirroring the pattern already
used in `AutomationsApiIT` and `CampaignsApiIT`'s own `/pl/campaigns` test) to `SettingsApiIT` and
`CampaignsApiIT`'s email tests, or explicitly accepting the sibling-tag mapping as sufficient (a
plan/packet decision, not this verifier's to make).

## Wave-3 shell changes — regression-guard coverage at the integration dimension

- **vue-router `scrollBehavior` + reload scroll restoration**
  (`frontend/src/router/scrollRestoration.ts`, `router/index.ts`): only pure-function unit tests
  exist (`frontend/test/unit/scrollRestoration.spec.ts`); no `test/integration` test mounts the real
  `router/index.ts` (with `scrollBehavior` wired) or exercises real browser scroll position — jsdom
  does not lay out content, so this is not meaningfully testable at the integration dimension. This
  is properly an e2e-dimension concern, not a gap in this dimension's coverage.
- **generic locale toggle via `route.meta.defaultParams`**
  (`frontend/src/router/localeHref.ts`, `Topbar.vue`): `frontend/test/unit/localeHref.spec.ts`
  builds a real `createRouter` against the production `routes` table and calls `buildLocaleHref`
  directly (7 tests, all green) — functionally rigorous, but it lives under `test/unit` and never
  mounts a component, so it does not meet this run's qualifying-test shape for the integration
  dimension either. No `test/integration` spec mounts `Topbar.vue` with a real router to prove the
  toggle end-to-end. Not one of the wave's scoped behaviour ids, so it does not affect the dimension
  verdict, but is noted per the packet's "MUST be covered by your regression guard where your
  dimension applies" instruction — recommend a `Topbar` integration mount test if this dimension is
  meant to carry it.

## Deviations in scope — applicability to this dimension

- **DEV-4** (contract dimension — HTTP method+path+status comparison rules): not applicable to
  integration; no action taken.
- **DEV-7** (campaigns-email-editor: the SPA makes no POST `…/blocks`; dimension `contract`):
  not formally this dimension's, but directly corroborated here —
  `EmailEditorView.spec.ts`'s `"dragging a library block onto the canvas never issues a network
  request (DEV-7)"` test (green) proves the same invariant at the integration level.
- **DEV-12** (settings step 10 = 404 document; dimension `visual`): not applicable to integration
  for the screenshot/aria/texts skip itself, but the underlying 404 status is independently proven
  by `SettingsApiIT`'s `"B06/DEV-12 GET /pl/settings/nonexistent is a 404 document …"` test (green).

## Evidence paths

- `integration/backend-mvnw-verify.log` — full `./mvnw -q verify -Pintegration` console output.
- `integration/failsafe-reports/click.kivvi.*.txt` — per-class failsafe summaries (12 files).
- `integration/frontend-test-integration.log` — full `npm run test:integration` console output.
