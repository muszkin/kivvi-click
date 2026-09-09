# Verifier report — wave FINAL (all journeys, round 2), dimension: integration

**Dimension status: PASS**, bound to wave SHA `dae169614a52532c130bd34435994d6a914165c0`
(checkout `/home/muszkin/work/kivvi-click-wt/verify-final-integration`, detached HEAD
confirmed equal to the wave SHA before any command ran).

Read-only verification: no worker claims, no round-1 evidence, and no other dimension's
evidence were read. Nothing was started or stopped on the shared `kivvi-int` compose
stack — this dimension's contract (component/store/router tests + Spring integration
tests) runs entirely against Testcontainers-managed Postgres and Vitest's own jsdom, not
the running stack at https://localhost:19101.

## Commands run

| # | Command | cwd | Exit code | Notes |
| --- | --- | --- | --- | --- |
| 1 | `npm ci` | `frontend` | 0 | cache-warm, 259 packages, 0 vulnerabilities |
| 2 | `npm run test:integration -- --reporter=verbose` | `frontend` | 0 | 21 files, **152/152 tests passed** |
| 3 | `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify` (1st attempt) | `backend` | not captured | launched via `nohup … & disown` to run alongside the frontend suite; a verifier-side process-management mistake (not a test failure) lost the shell's `$?`. All artefacts it produced (`target/failsafe-reports/*.txt`, `target/surefire-reports/*.txt`, `target/failsafe-reports/failsafe-summary.xml`) independently show **0 failures, 0 errors** across every unit and IT class, so the run was not discarded as inconclusive — it was corroborated by attempt 2 below and superseded as the evidence-of-record. |
| 4 | `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify` (2nd attempt, foreground, exit code captured) | `backend` | **0** | Re-run cleanly to get an unambiguous exit code. **298 unit (surefire) tests, 0 failures/errors; 142 integration (failsafe, Testcontainers Postgres 18) tests completed, 0 failures/errors, 1 intentionally-skipped (`SessionRequestSerializationRedProofIT`, `@Disabled("RED proof only")`)**. `ArchitectureTest` (5/5) and `spotless:check` also passed as part of the same `verify` lifecycle (informational only — not this dimension's gate). |

No IT class failed on either attempt, so the packet's "re-run a failing class once" clause
did not apply.

**Benign noise observed on both attempts, not a failure:** near the very end of the forked
test JVM's life, cached Spring contexts race their own shutdown against Spring Session's
background `cleanUpExpiredSessions` scheduled task, producing `CannotCreateTransactionException`
/ `HikariPool - Interrupted during connection acquisition` stack traces and a final
`[ERROR] Surefire is going to kill self fork JVM. The exit has elapsed 30 seconds after
System.exit(0).` line. This happens strictly after all test results are already recorded in
the XML reports (confirmed identical on both runs, and the `verify`-phase build still exited
0 on the clean attempt) — cosmetic shutdown-hook noise, not a test or build failure.

## Behaviour -> qualifying-test mapping (behaviours.json, B01-B33)

Full table: `waves/final/integration/behaviour-mapping.md`. Qualifying test = real-HTTP
`*IT.java` (Testcontainers) or `frontend/test/integration/*.spec.ts` mounting real
views/components with the real router/store, stubbing only the network; `@WebMvcTest`
slices and unit-layer tests do not qualify, per the packet.

- **30 / 33 behaviours mapped BY NAME** to at least one qualifying test.
- **B09** ("Unknown theme value falls back to light") and **B21** ("only `.main-scroll`
  scrolls...") have no qualifying integration-dimension test — both are unit-owned by the
  plan's own Coverage matrix (row "B01–B14, B21, B22 (login)" → "JUnit shell/session tests,
  Vitest shell components"), confirmed present at `backend/src/test/java/click/kivvi/application/PreferencesServiceTest.java`
  / `backend/src/test/java/click/kivvi/web/PreferencesControllerTest.java` (B09, unit-layer,
  out of scope for this dimension) and `frontend/test/unit/AppShell.spec.ts` (B21, tagged
  `describe("B21 only .main-scroll scrolls...")`). This is exactly what this round's own
  note describes ("shell-navigation label repair (test-only: B02/B03/B21 **unit** tests)")
  — not a new finding, not a regression.
- **B17** ("Accepted event is published once ... as a rendered event-row with translated
  type and detail") has no test tagged `B17` by name, but is substantively verified by the
  qualifying IT `CollectApiIT::acceptedEventIsPublishedAsJson` (`@DisplayName("DEV-3 the
  accepted event is published to the hub as JSON, not server-rendered HTML")`), which is the
  exact test `tools/migration-verify/deviations.json`'s own `DEV-3` entry names as the
  backend verification vehicle for this behaviour under the accepted contract-shape change.
  Treated as accepted-deviation (DEV-3), not a regression.
- `scheduler-heartbeat` maps to `HeartbeatSchedulerIT` as instructed
  (`@DisplayName("B20/B33 the scheduler ticks once on a fresh shedlock table, persists
  exactly one shedlock row, and lockAtLeastFor blocks a second attempt within the same
  window")`) — single test, both behaviours, green.

**Discrepancy noted, does not change any verdict:** the packet refers to "the whole table
DEV-1..DEV-13", but `tools/migration-verify/deviations.json` on this SHA only defines
`DEV-1`..`DEV-12`. No qualifying test in this dimension references a `DEV-13`.

## Per-journey verdicts (13/13)

| Journey | Behaviours owned | Verdict | Basis |
| --- | --- | --- | --- |
| login | B11, B12, B13, B14 (+B01, B22 also) | parity | `SessionRoundTripIT`, `ShellApiIT`, `LandingApiIT`; frontend `LoginView.spec.ts`, `landing.spec.ts` — all green |
| landing | B22 (+B01 also) | parity | `LandingApiIT`; frontend `landing.spec.ts` — green |
| feeds | B30 (+B01 also) | parity | `FeedsApiIT`; frontend `FeedsView.spec.ts` — green |
| scheduler-heartbeat | B20, B33 | parity | `HeartbeatSchedulerIT` (1 test, both behaviours) — green |
| event-stream | B15, B16, B17, B18, B19 (+B24 also) | accepted-deviation (DEV-3) | `CollectApiIT`, `EventDedupStoreIT`, `EventsApiIT`; frontend `EventsView.spec.ts` — all green; B17 covered under DEV-3's own named test, not tagged B17 (see above) |
| customers | B03, B05, B25 | parity | `AutomationsApiIT`/`CustomersApiIT` (B03), `CustomersApiIT` (B05, B25); frontend `CustomersView.spec.ts`, `CustomerView.spec.ts`, `CustomerDetailSidebarSection.spec.ts` — all green |
| automations | B26 | parity | `AutomationsApiIT`; frontend `AutomationsView.spec.ts`, `AutomationEditorView.spec.ts` — green |
| settings | B06, B32 | parity | `SettingsApiIT`; frontend `SettingsView.spec.ts` — green |
| campaigns-email-editor | B27, B28 | parity | `CampaignsApiIT`; frontend `CampaignsView.spec.ts`, `EmailEditorView.spec.ts` — green |
| popups-widget-editor | B29 | parity | `WidgetApiIT`; frontend `PopupsView.spec.ts`, `PopupEditorView.spec.ts` — green |
| import-wizard | B31 (+B01 also) | parity | `ImportApiIT`; frontend `ImportView.spec.ts` — green |
| dashboard | B23 | parity | `DashboardApiIT`; frontend `DashboardView.spec.ts` — green |
| shell-navigation | B02, B04, B07, B08, B09, B10, B21 (+B01, B03 also) | parity | `ShellPagesIT`, `ShellApiIT`, `SessionRoundTripIT`; frontend `ShellNavigation.spec.ts`, `Sidebar.spec.ts`, `LocaleToggle.spec.ts`, `ScrollRestoration.spec.ts` — all green; B09/B21 correctly unit-owned per plan Coverage matrix (see mapping notes), not a gap in this dimension's actual test run |

13/13 journeys: parity, except event-stream (accepted-deviation, DEV-3 — a naming detail on
an already-passing test, not a functional gap).

## Evidence paths

- `waves/final/integration/backend-mvnw-verify.log` — full console output of the clean
  (exit-code-captured) `./mvnw -q verify` run.
- `waves/final/integration/backend-failsafe-summary.xml` — failsafe aggregate: 142
  completed, 0 errors, 0 failures, 1 skipped, 0 flakes.
- `waves/final/integration/backend-test-class-summary.txt` — one-line-per-class pass/fail
  counts for every surefire (unit) and failsafe (integration/IT) test class.
- `waves/final/integration/frontend-test-integration.log` — full Vitest verbose output,
  `npm run test:integration`: 21 files / 152 tests, all green.
- `waves/final/integration/behaviour-mapping.md` — full B01-B33 -> qualifying-test mapping
  with the three gap explanations above.

## Disk

Checkout cleaned per packet instruction immediately after both suites finished:
`backend/target` (31M) and `frontend/node_modules` (161M) deleted from
`/home/muszkin/work/kivvi-click-wt/verify-final-integration`. Host free space recovered
from ~2.4 GB to ~5.2 GB after cleanup (some of the recovery is from concurrent verifiers'
own cleanup, not solely this run).
