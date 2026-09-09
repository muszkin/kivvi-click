# Wave-3 verifier — dimension: integration (round 2)

**Dimension status: PASS**, bound to wave SHA `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`.

Independent, read-only verification. No worker reports, review files, or round-1 evidence were
read (round-1 evidence lives under `waves/wave-3/round-1/` and was not opened). Verified from a
detached checkout at the wave SHA: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration`
(confirmed `git rev-parse HEAD` = `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`, `git status
--porcelain` clean before and after the run — build artifacts land under `backend/target/` and
`frontend/node_modules/`, both gitignored, no tracked file was edited).

Oracle: `/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue`
(`manifest.json` sha256 recomputed as `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`
— matches the packet). Deviations read from `tools/migration-verify/deviations.json` in the
checkout (matches the plan's "Accepted deviations" table by id).

## Journeys in scope and verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| automations | parity | B26 and every B01 row for this journey (`/pl/automations`, `/pl/automations/new`, `/pl/automations/a99`) map to a named, green backend `*IT.java` and frontend `test/integration` test |
| settings | parity | B06, B32, and every one of the 8 B01 settings-tab rows map to a named, green backend `SettingsApiIT` test (12/12 green) and frontend `SettingsView.spec.ts` test; DEV-12 (settings step 10 = 404 document) is exercised and green (`unknownTabDocumentIsNotFoundThroughTheRealHttpLayer`, labelled "B06/DEV-12") |
| campaigns-email-editor | parity | B27, B28, and every B01 row (`/pl/campaigns`, `/pl/emails/new`, `/pl/emails/k1`, `/pl/emails/k999`) map to a named, green `CampaignsApiIT` test and frontend `CampaignsView.spec.ts`/`EmailEditorView.spec.ts` test; DEV-7 (no `POST …/blocks` on drag) is exercised and green (`EmailEditorView.spec.ts`'s dedicated DEV-7 test) |

Regression guard (earlier-wave journeys, this dimension only): login, landing, feeds,
event-stream, customers, and scheduler-heartbeat all still have green backend ITs
(`SessionRoundTripIT`, `LandingApiIT`, `FeedsApiIT`, `EventsApiIT`, `CollectApiIT`,
`CustomersApiIT`, `HeartbeatSchedulerIT`, `EventDedupStoreIT`, `ShellApiIT`) and green frontend
integration specs (`LoginView.spec.ts`, `landing.spec.ts`, `FeedsView.spec.ts`,
`EventsView.spec.ts`, `CustomersView.spec.ts`, `CustomerView.spec.ts`,
`CustomerDetailSidebarSection.spec.ts`, `Sidebar.spec.ts`, `shellStore.spec.ts`) — no regression
found.

No accepted-deviation *verdict* was needed at the journey level (all three in-scope journeys are
full parity); DEV-7 and DEV-12 are exercised as passing tests, not as parity gaps.

## Behaviour -> test mapping

Full table: `integration/behaviour-mapping.md`. Summary: every one of B26, B06, B32, B27, B28 and
every B01 row named in the packet ("automations, settings x8 tabs, campaigns, email-editor") maps
to at least one qualifying test, and every one of those tests passed:

- Backend: `AutomationsApiIT` (7 tests), `SettingsApiIT` (12 tests), `CampaignsApiIT` (8 tests) —
  each a real `*IT.java` (`@Testcontainers` + `@SpringBootTest(webEnvironment=RANDOM_PORT)` +
  `PostgreSQLContainer`), run under `./mvnw -q verify` via the failsafe plugin (never under plain
  `test`/surefire — confirmed by `backend/pom.xml`'s failsafe-plugin comment and by the fact these
  three classes appear only in `target/failsafe-reports/`, not `target/surefire-reports/`).
- Frontend: `AutomationsView.spec.ts`, `AutomationEditorView.spec.ts`, `SettingsView.spec.ts`,
  `CampaignsView.spec.ts`, `EmailEditorView.spec.ts` under `frontend/test/integration/` — each
  mounts the real view component with a real `vue-router` built from the app's own `routes`
  table, stubbing only `global.fetch` (the network boundary); none of these views use a Pinia
  store, so no store double was needed or used.

## Wave-3 shell-change regression guard

| Change | Test | Verdict |
| --- | --- | --- |
| Locale toggle via `route.meta.defaultParams` | `frontend/test/integration/LocaleToggle.spec.ts` (3 tests): mounts the real `AppLayout` with the real router + real Pinia, stubbing only `fetch`; covers the default-tab-omits-segment case, the non-default-tab-keeps-segment case, and the no-`meta.defaultParams` unmodified-prefix-swap case | qualifying, green |
| Reload scroll restoration | `frontend/test/integration/ScrollRestoration.spec.ts` (3 tests): imports the real production router singleton (`@/router`) and asserts `router.options.scrollBehavior` is the module's own function, then drives it with a real resolved route from `router.push()` | qualifying with a caveat (see below), green |

**Caveat on `ScrollRestoration.spec.ts`:** this test does not `mount()` a Vue SFC — it exercises
the real, singleton production router module directly (`import { router } from "@/router"`),
proving `createRouter({ scrollBehavior })` was wired with the module's own function and that the
function correctly awaits layout stabilisation before resolving a saved position. It is not a
`@WebMvcTest` slice and not a prop-fed component test (the two categories this dimension's
contract explicitly excludes), and it is the only integration-level test of this wiring — the
unit-level `test/unit/scrollRestoration.spec.ts` calls the exported helper directly and never
touches `router/index.ts` at all. I am treating it as qualifying (it is a genuine router
integration test, matching this dimension's own duty description "component/store/router …
tests"), but flagging the shape mismatch against the literal "mounts real views/components"
wording for the orchestrator's own judgement. It does not by itself prove the visible reload-scroll
UX in a browser; that end-to-end behaviour is instead the concern of the visual/contract
dimensions' DEV-1/DEV-11 masks and the e2e `navigation.spec.ts`/`events.spec.ts` runs (out of
this dimension's scope).

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration` | 0 (`c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`) |
| 2 | `git status --porcelain=v1` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration` | 0 (clean) |
| 3 | `sha256sum manifest.json` | `context/migration-oracle/symfony-to-spring-vue` (main checkout, read-only) | 0 (matches packet) |
| 4 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |
| 5 | `npm run test:integration` (= `vitest run test/integration`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 — 16 files, 89 tests, 0 failures |
| 6 | `npx vitest run test/integration --reporter=verbose` (evidence-capture re-run of #5, same result) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |
| 7 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify -Pintegration` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/backend` | 0 — failsafe-summary: 66 completed, 0 errors, 0 failures, 0 skipped (Maven does not define a profile named `integration` in this `pom.xml`; `-Pintegration` is a no-op flag and `verify` already binds `maven-failsafe-plugin`'s `*IT.java` execution by Maven's standard lifecycle convention — confirmed both by the pom's own comment at line ~155 and by `target/failsafe-reports/` containing exactly the 12 `*IT.java` classes, none of the 38 `*Test.java` classes in `target/surefire-reports/`) |

## Results detail

- Backend `*IT.java` per-class results (all green), from `backend/target/failsafe-reports/`:
  `AutomationsApiIT` 7/7, `CampaignsApiIT` 8/8, `CollectApiIT` 6/6, `CustomersApiIT` 7/7,
  `EventsApiIT` 3/3, `FeedsApiIT` 3/3, `HeartbeatSchedulerIT` 1/1, `EventDedupStoreIT` 4/4,
  `LandingApiIT` 4/4, `SessionRoundTripIT` 4/4, `SettingsApiIT` 12/12, `ShellApiIT` 7/7 — sum 66,
  matching `failsafe-summary.xml`'s `<completed>66</completed><errors>0</errors><failures>0</failures>`.
- Frontend `test/integration` per-file results (all green): 16 files / 89 tests — full verbose
  transcript in `integration/frontend-integration-verbose.log`.

## Evidence paths

- `integration/backend-verify-mvnw.log` — full `./mvnw -q verify -Pintegration` console output.
- `integration/backend-failsafe-summary.xml` — Maven Failsafe aggregate summary (66/0/0/0).
- `integration/backend-failsafe-reports/*.txt` — per-`*IT.java` Failsafe text reports.
- `integration/frontend-integration-verbose.log` — full `vitest run test/integration
  --reporter=verbose` transcript (16 files, 89 tests, all named).
- `integration/behaviour-mapping.md` — full B01/B26/B06/B32/B27/B28 -> test mapping table and
  DEV-7/DEV-12 mapping.
