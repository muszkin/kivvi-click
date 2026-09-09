# Wave-3 (round 1) — dimension: unit — verdict

**Dimension status: PASS**, bound to wave SHA `32a68311594e611aaa8eaa0803bc72bba7d735a9`.

Independent read-only verification. No worker claims or review files were read. Checkout
`/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit`, confirmed detached at the wave SHA before
running anything (`git rev-parse HEAD` = `32a68311594e611aaa8eaa0803bc72bba7d735a9`, clean working
tree). No Docker stacks started or stopped; no tracked files edited.

## Journeys in scope (round 1)

automations, settings, campaigns-email-editor. Earlier-wave journeys (login, landing, feeds,
event-stream, customers, scheduler-heartbeat) are regression guards only — see below.

## Per-journey verdicts

| Journey | Verdict |
| --- | --- |
| automations | parity |
| settings | parity |
| campaigns-email-editor | parity |

No deviation applied for this dimension: DEV-4 (contract), DEV-7 (contract), DEV-12 (visual) — all
scoped to other dimensions and have no unit-test-surface manifestation.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/backend` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |
| 3 | `npm run test -- --run` (→ `vitest run test/unit --run`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |

`npm ci` was required in `tools/migration-verify` and `tests/e2e` are untouched — this dimension
only needed `frontend/`. Toolchains used exactly as prescribed by the packet's Environment section
(Java 25 at `/home/muszkin/.cache/kivvi-toolchains/jdk-25`, host Node 26 — `node --version` →
v26.8.1).

## Results

### Backend (`./mvnw -q test`, Surefire)

Exit 0. 38 `*Test.java` classes on disk == 38 Surefire reports produced (Maven's default Surefire
include pattern naturally excludes `*IT.java`; verified by listing both sets — 38 `*Test.java` vs.
12 `*IT.java`, only the 38 ran). Every class: `Failures: 0, Errors: 0, Skipped: 0`. Full per-class
counts: `unit/backend-surefire-summary.txt`. Raw log: `unit/backend-mvnw-test.log`.

`click.kivvi.architecture.ArchitectureTest` also ran here (it is a `*Test.java` file picked up by
`mvn test`) — 5/5 green; this is incidental to the unit command, the architecture dimension is a
separate verifier.

### Frontend (`npm run test -- --run`)

Resolves to `vitest run test/unit --run` (`frontend/package.json` `"test"` script) — scoped to
`frontend/test/unit` only, confirmed against `package.json` (`test:integration` is the separate
`test/integration` script used by the integration dimension, not this one). Exit 0: **21 test
files, 125 tests, all passed**. Raw log: `unit/frontend-npm-test.log`.

## Behaviour → named-test mapping (behaviours.json, wave scope: B01 automations/settings/campaigns/email-editor rows, B26, B06, B32, B27, B28)

Evidence: `unit/behaviour-to-test-mapping.txt` (raw grep of `\bB01\b|\bB06\b|\bB26\b|\bB27\b|\bB28\b|\bB32\b`
over `backend/src/test` and `frontend/test/unit`). Only `*Test.java` (not `*IT.java`) and
`frontend/test/unit` (not `test/integration`) count toward this dimension per the packet's
Environment section; the table below marks what counts.

| Behaviour | Named test(s) counted for `unit` | Counts? |
| --- | --- | --- |
| B01 (automations/settings/campaigns/email-editor rows) | `RouteTableTest::"B01 every panel/public URL in the route table is known"` (backend `Test.java`); `SpaDocumentControllerTest::"B01 every panel/public URL renders 200 and carries the SPA document"` (23 parameterized cases incl. `/pl/automations`, `/pl/automations/new`, `/pl/automations/a1`, `/pl/campaigns`, `/pl/emails/new`, `/pl/emails/k1`, `/pl/settings` — backend `Test.java`); `routes.spec.ts::"B01 every panel/public route in the table resolves"` (frontend `test/unit`) | yes — backend `*ApiIT.java` (`AutomationsApiIT`, `CampaignsApiIT`, `SettingsApiIT`, `LandingApiIT`, `FeedsApiIT`, `EventsApiIT`) also carry `B01` `@DisplayName`s but are `*IT.java` and do **not** count; the `Test.java`/`test/unit` set above is sufficient on its own |
| B26 (automations) | `AutomationsViewServiceTest` (9 tests), `AutomationsFixturesTest` (7), `AutomationsControllerTest` (7) — all backend `Test.java`; `automationsComponents.spec.ts` describe blocks `"B26 FlowCanvas"`, `"B26 RulePipeline"`, `"B26 AutoCard"` — frontend `test/unit` | yes |
| B06 (settings) | `SettingsControllerTest::"B06 GET /api/v1/pl/settings/nonexistent is not found"` and `"B06/DEV-12 GET /pl/settings/nonexistent is a 404 document, not the 200 SPA shell"`; `SettingsFixturesTest::"B06 isKnownTab is true for every seeded tab and false for anything else"`; `SettingsViewServiceTest::"B06 an unknown tab throws, mirroring SettingsController's 404"` — all backend `Test.java` | yes — `SettingsApiIT`'s `B06` tests are `*IT.java`, excluded, but not needed |
| B32 (settings) | `SettingsControllerTest` (4 `B32` cases), `SettingsFixturesTest` (6), `SettingsViewServiceTest` (2) — backend `Test.java`; `settingsComponents.spec.ts` (`SettingsNav`, `ToggleRow`, `DnsRow`, `HookRow`, `CodeBlock`), `highlight.spec.ts::"B32 highlight() reproduces CodeHighlightExtension::highlight() byte-for-byte"`, `localeHref.spec.ts::"B32/repair-1 buildLocaleHref …"` — frontend `test/unit` | yes |
| B27 (campaigns-email-editor) | `CampaignsViewServiceTest` (4 `B27` cases), `CampaignsControllerTest` (2 `B27` cases) — backend `Test.java` | yes — `CampaignsApiIT`'s `B27` test is `*IT.java`, excluded; `CampaignsView.spec.ts::"B27 …"` lives in `frontend/test/integration`, excluded; backend `Test.java` coverage is sufficient on its own |
| B28 (campaigns-email-editor) | `CampaignsViewServiceTest` (3 `B28` cases), `CampaignsControllerTest` (2 `B28` cases) — backend `Test.java`; `emailDocument.spec.ts::"B28 EmailDocument keeps a literal white background regardless of theme"`, `campaignsComponents.spec.ts` (`"B28 BlockLibrary"`, `"B28 CouponCode"`) — frontend `test/unit` | yes |

Every behaviour in scope has at least one named test on the counted surface, and every counted
test class/file is green (see Results above). No behaviour has zero coverage.

## Regression guard

`./mvnw -q test` and `npm run test -- --run` run the **whole** unit surface, not filtered to
wave-3's three journeys, so earlier-wave journeys (login, landing, feeds, event-stream/tracking,
customers, scheduler-heartbeat) are exercised as regression guards in the same green run —
confirmed present in the file lists (`LoginServiceTest`, `LoginControllerTest`,
`LandingControllerTest`, `LandingFixturesTest`, `FeedsViewServiceTest`, `FeedsControllerTest`,
`EventsViewServiceTest`, `EventsControllerTest`, `EventIngestionServiceTest`,
`EventDedupCleanupJobTest`, `CustomersViewServiceTest`, `CustomersControllerTest`,
`CustomersFixturesTest`, `HeartbeatJobTest`, `HeartbeatTriggerTest`; frontend `EventRow.spec.ts`,
`EventStream.spec.ts`, `FeedCard.spec.ts`, `KpiGrid.spec.ts`, `landing.spec.ts`,
`LoginView.spec.ts`, `customersComponents.spec.ts`, `FilterChip.spec.ts`, `Segmented.spec.ts`), all
green in the same run.

Wave-3 shell changes named in the packet are covered on the unit surface and green:

- `frontend/test/unit/scrollRestoration.spec.ts` — vue-router `scrollBehavior` / reload scroll
  restoration — present, part of the 125 passing tests.
- `frontend/test/unit/localeHref.spec.ts` — generic locale toggle via `route.meta.defaultParams`
  (`describe("B32/repair-1 buildLocaleHref — generic Symfony-style default-param omission")`) —
  present, part of the 125 passing tests.

## Evidence paths

- `unit/backend-mvnw-test.log` — full `./mvnw -q test` output
- `unit/backend-surefire-summary.txt` — per-class `Tests run` lines for all 38 `*Test.java` classes
- `unit/frontend-npm-test.log` — full `npm run test -- --run` output
- `unit/behaviour-to-test-mapping.txt` — raw grep backing the behaviour → test table above
