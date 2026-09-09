# Wave-3 verifier — dimension: integration (round 3)

**Wave SHA:** `60296f16250c6ebd25f7b4435c795268772fb6be`
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration` (verified detached at the wave SHA, clean tree, untouched)
**Dimension status: PASS**

## Scope

Journeys: automations, settings, campaigns-email-editor (earlier-wave journeys — login, landing,
feeds, event-stream, customers, scheduler-heartbeat — are regression guards only).
Behaviours in scope: B26, B06, B32, B27, B28, and all B01 rows for automations / settings /
campaigns-email-editor. Deviations in scope: DEV-4 (all — contract dimension, not integration;
recorded, no integration-level action needed), DEV-7 (campaigns/email-editor: no POST …/blocks),
DEV-12 (settings step 10 = 404 document). Shell changes requiring regression coverage: (1)
vue-router `scrollBehavior` + reload scroll restoration, (2) locale toggle via
`route.meta.defaultParams`, (3) preference POSTs (theme/sidebar) with `fetch keepalive`.

## Qualifying-test definition applied

Real-HTTP `*IT.java` under `backend/src/test/java` (`@Testcontainers` + `@SpringBootTest(webEnvironment = RANDOM_PORT)`,
`PostgreSQLContainer`, `TestRestTemplate`, run only by Failsafe on `verify`) — confirmed by reading
`AutomationsApiIT.java`, `CampaignsApiIT.java`, `SettingsApiIT.java` (all three declare
`@Container @ServiceConnection static PostgreSQLContainer postgres` and issue real HTTP calls via
`TestRestTemplate`). `@WebMvcTest`/prop-fed component tests were excluded from the mapping (none
were used for the in-scope behaviours anyway).

`frontend/test/integration/*.spec.ts` qualifies where it mounts a real view/component with a real
`vue-router` (`createRouter({ history: createWebHistory(), routes })`, the app's own `routes`
table) and/or a real Pinia store, stubbing only `fetch` via `vi.stubGlobal("fetch", …)`. Verified
by reading `SettingsView.spec.ts`, `AutomationsView.spec.ts`, `AutomationEditorView.spec.ts`,
`CampaignsView.spec.ts`, `EmailEditorView.spec.ts`, `LocaleToggle.spec.ts` (mounts the real
`AppLayout` + real router + real Pinia + real `i18n`), `shellStore.spec.ts` (real `useShellStore`
Pinia store, network stubbed), and `ScrollRestoration.spec.ts` (imports the real
`router` singleton from `@/router/index.ts` and drives its actual `scrollBehavior` option through
a real `router.push()`/`router.isReady()` navigation — distinct from the unit-level
`test/unit/scrollRestoration.spec.ts`, which calls the exported helper directly).

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/backend` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |
| 3 | `npm run test:integration -- --reporter=verbose` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 |

(`./mvnw -q verify -Pintegration` from the plan's verifier-contract table does not correspond to an
actual Maven profile in `backend/pom.xml` — there is no `<profiles>` section; the Failsafe plugin
comment there records that Testcontainers `*IT` tests are bound to the plain `verify` lifecycle
phase, which is what command #1 exercises. Failsafe reports confirm all `*IT` classes actually
ran, not skipped.)

Docker/Testcontainers was available and used (ephemeral `postgres:18-alpine` containers, one per
IT class); the shared `kivvi-int` compose stack used by other dimensions on this wave was not
started, stopped, or touched.

## Backend real-HTTP IT results (`./mvnw -q verify`, Failsafe)

`target/failsafe-reports/failsafe-summary.xml`: **completed 66, errors 0, failures 0, skipped 0.**
Per-class (`all-failsafe-reports.txt`):

| Test class | Tests | Failures | Errors | Journey |
| --- | --- | --- | --- | --- |
| `AutomationsApiIT` | 7 | 0 | 0 | automations (wave-3) |
| `CampaignsApiIT` | 8 | 0 | 0 | campaigns-email-editor (wave-3) |
| `SettingsApiIT` | 12 | 0 | 0 | settings (wave-3) |
| `CollectApiIT` | 6 | 0 | 0 | event-stream (regression) |
| `CustomersApiIT` | 7 | 0 | 0 | customers (regression) |
| `EventsApiIT` | 3 | 0 | 0 | event-stream (regression) |
| `FeedsApiIT` | 3 | 0 | 0 | feeds (regression) |
| `HeartbeatSchedulerIT` | 1 | 0 | 0 | scheduler-heartbeat (regression) |
| `EventDedupStoreIT` | 4 | 0 | 0 | event-stream (regression) |
| `LandingApiIT` | 4 | 0 | 0 | landing (regression) |
| `SessionRoundTripIT` | 4 | 0 | 0 | login (regression) |
| `ShellApiIT` | 7 | 0 | 0 | shell-navigation (regression) |

Surefire (unit-level, same `verify` run, incl. `RouteTableTest`, `SpaDocumentControllerTest`,
`ArchitectureTest`) also all green: `all-surefire-summaries.txt` — 0 failures/errors across every
reported test class. Spotless `check` and the `-Xlint:all -Werror` compile both passed (build did
not fail).

## Frontend integration results (`npm run test:integration`)

**16 test files passed (16), 91 tests passed (91), 0 failed, 0 skipped.**
(`Not implemented: Window's scrollTo() method` lines are benign jsdom console warnings from the
scroll-restoration tests, not failures — no test assertion depends on real scrolling; jsdom lacks
the method, the app code doesn't call it, and the affected tests pass.)

In-scope-behaviour mapping (all passed):

| Behaviour | Named in | Result |
| --- | --- | --- |
| B26 | `AutomationsView.spec.ts` ("B26 the automations index matches the oracle…", 6 tests), `AutomationEditorView.spec.ts` ("B26 the rule editor matches the oracle…", 8 tests incl. "B01/B26 GET /automations/new renders…"); backend `AutomationsApiIT` (3× `@DisplayName("B01/B26 …")`) | pass |
| B06 | `SettingsView.spec.ts` ("B06 an unknown tab never renders a page body"); backend `SettingsApiIT` (`@DisplayName("B06 …")`, `@DisplayName("B06/DEV-12 …")`) | pass |
| B32 | `SettingsView.spec.ts` ("B32 8 tabs at own URLs with markers…", 13 tests); backend `SettingsApiIT` (`@DisplayName("B32 GET /pl/settings (no tab) still renders…")`) | pass |
| B27 | `CampaignsView.spec.ts` ("B27 the campaigns index matches the oracle…", 5 tests incl. "B01/B27 renders 4 KPI tiles…"); backend `CampaignsApiIT` (`@DisplayName("B01 GET /pl/campaigns …")`, 2× `B01/B28`) | pass |
| B28 | `EmailEditorView.spec.ts` ("B28 the e-mail editor matches the oracle…", 6 tests incl. "B01/B28 renders the back link…", "B01/B28 the \"new\" route shows the blank draft template", "dragging a library block onto the canvas never issues a network request (DEV-7)"); backend `CampaignsApiIT` (`@DisplayName("B01/B28 …")`, 2×) | pass |
| B01 (automations row) | `AutomationEditorView.spec.ts` "B01/B26 GET /automations/new renders…"; backend `AutomationsApiIT` "B01/B26 GET /pl/automations…" (×3) | pass |
| B01 (settings row) | `SettingsView.spec.ts` 5× explicit `B01 …` tests (account/team/billing/gdpr marker text, `GET /pl/settings/team`); backend `SettingsApiIT` 7× `@DisplayName("B01 GET /pl/settings/<tab> renders…")` | pass |
| B01 (campaigns row) | `CampaignsView.spec.ts` "B01/B27 renders 4 KPI tiles…"; backend `CampaignsApiIT` `@DisplayName("B01 GET /pl/campaigns renders 200…")` | pass |
| B01 (email-editor row) | `EmailEditorView.spec.ts` "B01/B28 renders the back link…", "B01/B28 the \"new\" route…"; backend `CampaignsApiIT` 2× `@DisplayName("B01/B28 …")` | pass |

Shell-change regression coverage (all passed):

| Shell change | Named in | Result |
| --- | --- | --- |
| Locale toggle via `route.meta.defaultParams` | `LocaleToggle.spec.ts` — mounts real `AppLayout` + real router (`createRouter`/`createWebHistory`/app `routes`) + real Pinia + real `i18n`; 3 tests (settings default tab, settings non-default tab, a route with no `meta.defaultParams`) | pass |
| Reload scroll restoration | `ScrollRestoration.spec.ts` — imports the real `router` singleton, drives `router.options.scrollBehavior` via real navigation; 3 tests | pass |
| Preference POSTs use `fetch keepalive` | `shellStore.spec.ts` — real `useShellStore` Pinia store, network stubbed; explicit "B08 setTheme's POST survives an immediate reload (keepalive: true)" and "B10 setSidebar's POST survives an immediate reload (keepalive: true)" | pass |

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | B26 and its B01 row fully mapped by name in a real-HTTP IT (`AutomationsApiIT`, 7/7 green) and a real router+component integration test (`AutomationsView.spec.ts` + `AutomationEditorView.spec.ts`, 14/14 green) |
| settings | parity | B06, B32, and the B01 row fully mapped (`SettingsApiIT`, 12/12 green; `SettingsView.spec.ts`, 15/15 green including the DEV-12 404-document case) |
| campaigns-email-editor | parity | B27, B28 (incl. DEV-7 no-POST assertion), and their B01 rows fully mapped (`CampaignsApiIT`, 8/8 green; `CampaignsView.spec.ts` + `EmailEditorView.spec.ts`, 12/12 green) |

No `accepted-deviation` or `regression` verdicts were needed at the integration dimension for this
round: DEV-7 and DEV-12 are affirmatively *tested for* (not just tolerated) by the round-3 repair
suite, and DEV-4 is a contract-dimension deviation with no integration-level surface.

## Evidence paths

- `waves/wave-3/integration/backend/mvnw-verify.log` — full `./mvnw -q verify` output
- `waves/wave-3/integration/backend/failsafe-summary.xml` — aggregate Failsafe result (66/0/0/0)
- `waves/wave-3/integration/backend/all-failsafe-reports.txt` — per-`*IT`-class breakdown
- `waves/wave-3/integration/backend/all-surefire-summaries.txt` — per-unit-class breakdown (incl. ArchUnit, RouteTableTest, SpaDocumentControllerTest)
- `waves/wave-3/integration/backend/click.kivvi.AutomationsApiIT.txt`, `click.kivvi.CampaignsApiIT.txt`, `click.kivvi.SettingsApiIT.txt` — this wave's IT reports in full
- `waves/wave-3/integration/frontend/test-integration-verbose.log` — full `npm run test:integration -- --reporter=verbose` output (16 files / 91 tests, all passed, with per-test names)

## Conclusion

**Dimension status: PASS**, bound to wave SHA `60296f16250c6ebd25f7b4435c795268772fb6be`. Every
in-scope behaviour id maps by name to at least one qualifying integration test (real-HTTP
Testcontainers IT and/or real-router/real-store frontend integration test) and every such test is
green; the three wave-3 shell changes have qualifying regression coverage and are green; no
regressions found in the earlier-wave journeys exercised as regression guards by the same test
runs.
