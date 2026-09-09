# Wave-3 verifier — dimension: architecture (round 1)

**Status: PASS**, bound to wave SHA `32a68311594e611aaa8eaa0803bc72bba7d735a9`.
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` (`git rev-parse HEAD` confirmed == wave SHA before and after the run; every throwaway probe file was deleted; `git status --porcelain` is empty at the end of this run).

Verified independently: no worker reports or review files were read for this run. No Docker stack was started or stopped (ArchUnit/ESLint are static checks; the shared stack at `https://localhost:19101` was not touched).

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | New code (`application/AutomationsViewService`, `web/AutomationsController`, `web/dto/Automation*Response`, `fixtures/AutomationsFixtures`; frontend `AutomationsView`, `AutomationEditorView`, `AutoCard`, `FlowCanvas`, `FlowNode`, `RulePipeline`, `RbBlock`, `RbStep`, `SimulationCard`) sits inside the existing layering; all rule-owned components are imported only from within the automations journey (`cross-journey-imports.log`) |
| settings | parity | New code (`application/SettingsViewService`, `web/SettingsController`, `web/dto/SettingsResponse`, `fixtures/SettingsFixtures`; frontend `SettingsView`, `SettingsNav`, `AccountTab`/`ApiTab`/`BillingTab`/`GdprTab`/`NotificationsTab`/`ProvidersTab`/`SitesTab`/`TeamTab`, `HookRow`, `DnsRow`) sits inside the existing layering; the new `route.meta.defaultParams` mechanism (`router/meta.d.ts`, `routes.ts`) and the generic `router/localeHref.ts` consumer in `Topbar.vue` were reviewed directly — journey-agnostic, no Intl/`/accounts/`/`event-row` usage, no cross-journey import |
| campaigns-email-editor | parity | New code (`application/CampaignsViewService`, `web/CampaignsController`, `web/dto/Campaign*Response`, `fixtures/CampaignsFixtures`; frontend `CampaignsView`, `EmailEditorView`, `EditorShell`, `EmailDocument`, `EmailEnvelope`, `EmailInspector`, `EmailVariables`, `BlockLibrary`, `CouponCode`) sits inside the existing layering; all editor components are imported only from `EmailEditorView`/`EmailDocument`, never from automations or settings (`cross-journey-imports.log`) |

No journey needed a deviation for this dimension. Deviations in this wave's scope (DEV-4, DEV-7, DEV-12) are all tagged `dimension: contract` or `dimension: visual` in `tools/migration-verify/deviations.json` — none apply to the architecture dimension, confirmed by reading each entry directly (see "Deviation scope check" below).

## Rule → tooling table (`architecture/rules-translated.md`)

No row in the table is tagged `wave-3` — the table has no new architecture-dimension row for this wave (`grep -c wave-3 rules-translated.md` = 0). Every row is therefore a carried-over guard for wave-3; each was independently re-verified with a fresh negative-case proof (not merely confirmed green), because a boundary rule that no longer fires is indistinguishable from one that was quietly removed.

| Rule (wave introduced) | Tooling (as declared in `rules-translated.md`) | Enforcing test/rule found in checkout | Negative-case proof this round |
| --- | --- | --- | --- |
| Layering: `domain` never depends on `web` (wave-0) | ArchUnit `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAPackage("..web..")` | `ArchitectureTest.domainDoesNotDependOnWeb` | Added `backend/src/main/java/click/kivvi/domain/TmpArchViolation.java` referencing `click.kivvi.web.LoginController` → `./mvnw test -Dtest=ArchitectureTest` failed (exit 1): `domainDoesNotDependOnWeb` **and** `topLevelPackagesFormNoCycle` both fired in the same run (web already depends on domain in production code, so a reverse edge closes a two-node cycle) — one probe proved two rules at once. Deleted; `git status --porcelain` clean; re-run exit 0 |
| Layering: `infrastructure` only used from `application`/`infrastructure` (wave-0) | ArchUnit `classes().that().resideInAPackage("..infrastructure..").should().onlyBeAccessed().byAnyPackage("..infrastructure..", "..application..")` | `ArchitectureTest.infrastructureIsOnlyUsedFromApplication` | Added `backend/src/main/java/click/kivvi/web/TmpArchViolation.java` constructing and calling `SessionIdentityStore` (infrastructure) directly from `web` → failed (exit 1), naming both the constructor and method call as violations. **Note:** a first attempt that only referenced `SessionIdentityStore.class` as a class-object literal did *not* trigger `onlyBeAccessed` (exit 0) — ArchUnit's `onlyBeAccessed` condition tracks real member accesses (constructor/method/field), not a bare class-object reference; a real call was needed and used for every boundary probe below. Deleted; clean; re-run exit 0 |
| Layering: `web`/`application`/`domain`/`infrastructure` form no cycle (wave-0) | ArchUnit `SlicesRuleDefinition.slices().matching("click.kivvi.(*)..").should().beFreeOfCycles()` | `ArchitectureTest.topLevelPackagesFormNoCycle` | Proven together with the domain→web probe above (same failing run, same two-line violation report: `Cycle detected: Slice domain -> Slice web -> Slice fixtures -> Slice domain`) |
| Mercure topic built only server-side (wave-2) | ArchUnit (publisher-access boundary) + ArchUnit/plain source scan (topic-literal ownership) + ESLint (`no-restricted-syntax` on `/accounts/` literals) | `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; `ArchitectureTest.accountsTopicLiteralExistsOnlyInEventStreamTopic`; `frontend/eslint.config.js` `files: ["src/**/*.{ts,vue}"]` block's `no-restricted-syntax` (`Literal`/`TemplateElement` selectors matching `/accounts/`) | **Backend, one probe, three rules at once:** `web/TmpArchViolation.java` injected a `MercurePublisher` and called `publisher.publish("/accounts/1/events", "{}")` from `web` → `./mvnw test -Dtest=ArchitectureTest` failed (exit 1, 3 failures): `infrastructureIsOnlyUsedFromApplication`, `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`, **and** `accountsTopicLiteralExistsOnlyInEventStreamTopic` (the literal now exists in two files). Deleted; clean; re-run exit 0. **Frontend:** `frontend/src/tmp-arch-violation.ts` exporting `` `/accounts/${1}/events` `` → `npx eslint` failed (exit 1, `no-restricted-syntax`, the Mercure-topic message). Deleted; clean |
| Row markup in one place (wave-2) | ESLint `vue/no-restricted-class` for `event-row` outside `EventRow.vue` | `frontend/eslint.config.js` `files: ["src/**/*.vue"], ignores: ["src/components/molecules/EventRow.vue"]` block's `vue/no-restricted-class: ["error", "event-row"]`. Backend side stays structurally out of scope (no HTML emitted server-side) | Added `frontend/src/components/molecules/TmpArchViolation.vue` with `<div class="event-row">` → `npx eslint` failed (exit 1, `'event-row' class is not allowed`). Deleted; clean |
| Formatting decided server-side, SPA `format.ts` sole `Intl` importer (wave-1) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl`, `toLocaleString`, `toFixed` | `frontend/eslint.config.js` base rules block (backed by the `src/**/*.{ts,vue}` cascading block) | Added `frontend/src/tmp-arch-violation.ts` calling `value.toLocaleString("pl-PL")` → `npx eslint` failed (exit 1, `no-restricted-syntax`, the server-side-formatting message). Deleted; clean |
| Static typing gate / Formatting `@Symfony` / Test policy fail-on-warning (wave-0) | Maven compiler / Spotless / JUnit / Vitest config | out of this dimension's scope — compiler and test-runner concerns, covered by the `unit` and `static analysis` gates, not `architecture` (ArchUnit/ESLint boundary rules) | not re-probed here, consistent with the wave-2 precedent |
| Storybook dev-only | Vite build config | tagged "Out of scope for parity" in the table itself | not checked |

Every boundary-rule row in `rules-translated.md` has real, working enforcement; none lacks tooling; no deviation was needed for this dimension.

## New wave-3 shell files — layering/ESLint conformance

Checked directly (not merely by running the suite) because these three files are shared across all future journeys, not owned by one:

- `frontend/src/router/scrollRestoration.ts` (vue-router `scrollBehavior` + reload scroll restoration): no `Intl`/`toLocaleString`/`toFixed`, no `/accounts/` literal, no `event-row` class, no import of any journey-owned component. Pure browser-API module (`history`, `sessionStorage`, `performance.getEntriesByType`).
- `frontend/src/router/localeHref.ts` (generic locale-toggle href builder driven by `route.meta.defaultParams`): same checks clean; imports only `vue-router` types.
- `frontend/src/components/organisms/Topbar.vue` (consumes `buildLocaleHref`): imports only `@/components/atoms/Icon.vue`, `@/components/atoms/Kbd.vue`, and `@/router/localeHref` — all shared/atom-level, no journey-owned component, no rule-tripping literal.
- `frontend/src/router/routes.ts` diff confirmed narrow: only the six wave-3 routes (`automations`, `automation_new`, `automation_edit`, `campaigns`, `email_new`, `email_edit`, `settings`) had their `component: EmptyPageView` replaced, plus the new `meta.defaultParams: { tab: "account" }` on the `settings` route and the corresponding `RouteMeta.defaultParams` type addition in `router/meta.d.ts` — no other route line touched, matching the common-journey-rules.md constraint verbatim.

Evidence: `architecture/cross-journey-imports.log` (grep output for these files), `architecture/layering.log` (`git diff --stat` of new/changed backend+frontend source since wave-2).

## Cross-journey component-import check

Every component exclusively owned by one of this wave's three journeys was traced to its only import site:

- automations: `AutoCard` ← `AutomationsView.vue` only; `FlowCanvas`, `RulePipeline`, `SimulationCard` ← `AutomationEditorView.vue` only; `FlowNode` ← `FlowCanvas.vue` only; `RbBlock` ← `RbStep.vue` only; `RbStep` ← `RulePipeline.vue` only.
- settings: `SettingsNav` ← `SettingsView.vue` only; all eight `components/settings/*Tab.vue` files ← `SettingsView.vue` only; `HookRow` ← `ApiTab.vue` only; `DnsRow` ← `ProvidersTab.vue` only.
- campaigns-email-editor: `EditorShell`, `EmailEnvelope`, `EmailInspector`, `EmailVariables`, `BlockLibrary` ← `EmailEditorView.vue` only; `EmailDocument` ← `EmailEditorView.vue` only; `CouponCode` ← `EmailDocument.vue` only.

No journey imports another journey's exclusive component. The plan's dependency audit (Group C) names two intentional *forward* dependencies neither yet realized: `CondRule` "owned by J6 [automations], consumed by J10 [import-wizard] later" and `EditorShell` "owned by J8 [campaigns-email-editor], consumed by J9 [popups-widget-editor] later" — J9/J10 do not exist yet (only their `packet.md` exists under `slices/`, wave-4 has not started: `waves/wave-4/` does not exist in this run directory), so neither forward dependency is realized yet and there is nothing to violate. (No literal `CondRule` component exists in the checkout; the automations rule/condition UI is `RulePipeline`/`RbBlock`/`RbStep` — a naming difference from the plan's illustrative name, not a missing component.)

The only components imported from *outside* their apparent journey are genuinely shared ones declared as such in `common-journey-rules.md`/the plan's dependency audit: `Button`, `Chip`, `Icon`, `Card`, `FilterChip`, `Table`, `Segmented`, `Tabs`, `KpiGrid`, `PageHead` (used by `CampaignsView`/`AutomationsView`/`AutomationEditorView`/`SettingsView` alongside their own journey components) — all pre-existing shared atoms/molecules/organisms, none owned by another wave-3 journey. `WorkspaceCard` (Sidebar), `PriceCard` (LandingView, wave-1), `TimelineItem` (CustomerView, wave-2) are unrelated to this wave and unchanged.

Evidence: `architecture/cross-journey-imports.log`.

## Global constraints checked (no drift since wave-2's assembled SHA `b87a701244f5316e1a53bace7a1e41facdffc277`)

- `git diff --quiet b87a701..32a6831 -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` — exit 0, empty: no old-stack or protected path touched by wave-3.
- Full diff wave-2→wave-3: 88 files changed, 9470 insertions(+), 21 deletions(-).
- `find backend/src/main/resources/db/migration -type f` → only `V1__baseline.sql`, unchanged: no new Flyway migration, no schema-layering drift.
- `git diff b87a701..32a6831 -- backend/pom.xml frontend/package.json` — exit 0, empty: no dependency/version pin changed since wave-2.
- New backend main-source files for this wave sit only under the three existing top-level packages actually touched (`application`, `web`, `fixtures`) — no new top-level package, `domain`/`infrastructure` untouched by this wave's new code (consistent with automations/settings/campaigns-email-editor adding view-layer + fixture logic only, no new domain concept or infra integration).
- `event-row` class and `/accounts/` literal, repo-wide, confirmed to exist only where the rules allow (empty greps outside `EventRow.vue` / `EventStreamTopic.java`).

Evidence: `architecture/global-constraints.log`.

## Deviation scope check

`tools/migration-verify/deviations.json` entries in this wave's stated scope were read directly:

- **DEV-4** (`journeys: ["*"]`, all steps) — `dimension: "contract"` (HTTP full-parity/API-baseline compare rule). Not applicable to architecture.
- **DEV-7** (`campaigns-email-editor`, `popups-widget-editor`) — `dimension: "contract"`, `mechanism: "not applicable to login; recorded for completeness only"`. Not applicable to architecture.
- **DEV-12** (`shell-navigation`, `customers`, `settings`, `import-wizard`) — `dimension: "visual"` (`skip-on-404` for a 404 document step's screenshot/aria/texts). Not applicable to architecture.

None of the three in-scope deviations govern the architecture dimension; none of this wave's ArchUnit/ESLint rules needed a deviation.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` | 0 (`32a68311594e611aaa8eaa0803bc72bba7d735a9`, matches packet) |
| `git status --porcelain` (pre-run) | checkout root | 0, empty |
| `./mvnw -q test -Dtest=ArchitectureTest` (baseline) | `backend/` | 0 |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` (baseline) | `frontend/` | 0 (6 pre-existing unrelated warnings, 0 errors) |
| `npm run typecheck` (baseline) | `frontend/` | 0 |
| add `domain/TmpArchViolation.java` (domain→web probe); `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 1 (expected FAIL — `domainDoesNotDependOnWeb` + `topLevelPackagesFormNoCycle`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `web/TmpArchViolation.java` (class-object-literal probe against `SessionIdentityStore`); `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 0 (no failure — see note in the rule table: a bare class-object reference doesn't trip `onlyBeAccessed`) |
| rewrite probe to a real method call on `SessionIdentityStore`; `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 1 (expected FAIL — `infrastructureIsOnlyUsedFromApplication`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `web/TmpArchViolation.java` (Mercure-publisher-boundary + topic-literal probe); `./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 1 (expected FAIL — 3 rules at once) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| `./mvnw -q test -Dtest=ArchitectureTest` (final clean re-check) | `backend/` | 0 |
| add `frontend/src/tmp-arch-violation.ts` (`toLocaleString` probe); `npx eslint src/tmp-arch-violation.ts` | `frontend/` | 1 (expected FAIL — `no-restricted-syntax`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `frontend/src/tmp-arch-violation.ts` (`/accounts/` template-literal probe); `npx eslint src/tmp-arch-violation.ts` | `frontend/` | 1 (expected FAIL — `no-restricted-syntax`, Mercure-topic message) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| add `frontend/src/components/molecules/TmpArchViolation.vue` (`event-row` probe); `npx eslint src/components/molecules/TmpArchViolation.vue` | `frontend/` | 1 (expected FAIL — `vue/no-restricted-class`) |
| delete probe; `git status --porcelain` | checkout root | 0, empty |
| `npm run lint` (final clean re-check) | `frontend/` | 0 (same 6 pre-existing warnings, 0 errors) |
| `git diff --quiet b87a701244f5316e1a53bace7a1e41facdffc277..32a68311594e611aaa8eaa0803bc72bba7d735a9 -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | checkout root | 0 (empty diff) |
| `git diff b87a701244f5316e1a53bace7a1e41facdffc277..32a68311594e611aaa8eaa0803bc72bba7d735a9 -- backend/pom.xml frontend/package.json` | checkout root | 0 (empty diff) |
| grep sweeps for cross-journey imports, `event-row` class, `/accounts/` literal, `Intl`/`toLocaleString`/`toFixed` in the three new shell files | `frontend/` | see `cross-journey-imports.log`, `global-constraints.log` |
| `git status --porcelain` (post-run, final) | checkout root | 0, empty |

Environment: `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25` (`java -version` → `openjdk version "25.0.4.1" 2026-08-18 LTS`); host Node `v26.8.1`/npm `11.19.0`; `npm ci` run only inside `frontend/` of this checkout, per packet instructions. No Docker stack was started or stopped by this run.

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/architecture/`:

- `archunit-baseline.log` — baseline `ArchitectureTest` run, 0 violations
- `archunit-negative-domain-web-cycle.log` — negative probe, domain→web + no-cycle rules, exit 1
- `archunit-negative-infra-boundary.log` — negative probe, generic infrastructure-boundary rule (final version, real method call), exit 1
- `archunit-negative-mercure-boundary.log` — negative probe, Mercure-publisher boundary + topic-literal-scan rules (3 rules at once), exit 1
- `archunit-final-clean.log` — re-run after all backend probes were deleted, 0 violations
- `frontend-lint-baseline.log` — baseline `npm run lint`, 0 errors
- `frontend-typecheck-baseline.log` — baseline `npm run typecheck`, exit 0
- `eslint-negative-tolocalestring.log` — negative probe, `Intl`/`toLocaleString` ban, exit 1
- `eslint-negative-accounts-literal.log` — negative probe, `/accounts/` template-literal rule, exit 1
- `eslint-negative-event-row.log` — negative probe, `vue/no-restricted-class` `event-row`, exit 1
- `frontend-lint-final-clean.log` — re-run after all frontend probes were deleted, 0 errors
- `global-constraints.log` — protected-path diff, Flyway migration list, dependency-pin diff, top-level-package touch list, repo-wide `event-row`/`/accounts/` sweeps
- `layering.log` — full `git diff --stat` of new/changed backend+frontend source files this wave
- `npm-ci.log` — `npm ci` output
- `cross-journey-imports.log` — import-site grep for every journey-exclusive component plus the three new shell files' rule-literal sweep

## Verdict rationale

Every boundary-rule row of `rules-translated.md` — the wave-0 layering triad, the wave-1 `Intl`/format-server-side rule, and the two wave-2 rows (Mercure-topic ownership, row-markup ownership) — has real, working ArchUnit/ESLint enforcement, and each was independently proven to fire this round by introducing a throwaway violation, observing the expected exit-1 failure naming the expected rule, then deleting the probe and confirming a clean `git status --porcelain` and a green re-run; no row lacks tooling and no wave-3 row exists in the table needing new coverage. The three new shell files (`scrollRestoration.ts`, `localeHref.ts`, `Topbar.vue`) were read directly and contain no rule-tripping construct and no cross-journey import; `routes.ts`'s diff touches only the six wave-3 routes plus one new, journey-agnostic `meta.defaultParams` mechanism. Every component exclusively owned by automations, settings, or campaigns-email-editor is imported only from within its own journey; the only cross-file imports are pre-existing shared atoms/molecules/organisms. No old-stack or protected path was touched since wave-2's assembled SHA, no new Flyway migration was added, no dependency pin drifted, and no new top-level backend package was introduced. The checkout's `git status --porcelain` is empty at the end of this run — no tracked-file change was left behind. **Dimension status: PASS.**
