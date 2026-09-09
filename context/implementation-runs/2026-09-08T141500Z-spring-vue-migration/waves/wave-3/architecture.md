# Wave-3 verifier verdict — dimension: architecture (round 4)

**Dimension status: PASS**, bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture`, detached, untouched — `git status --porcelain` empty at every checkpoint below).

Journeys: automations, settings, campaigns-email-editor.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| automations | parity | Journey-owned code (`AutomationsController`, `AutomationsViewService`, `AutomationsFixtures`, views `AutomationsView.vue`/`AutomationEditorView.vue`, components `AutoCard`, `FlowCanvas`, `RulePipeline`) sits cleanly inside the `web`/`application`/`fixtures` layering, 0 ArchUnit violations, 0 ESLint errors, no cross-journey import. |
| settings | parity | `SettingsController`, `SettingsViewService`, `SettingsFixtures`, `SettingsView.vue` + `components/settings/*`, `SettingsNav.vue`: same result. DEV-12 (settings step 10 = 404 document) is a contract-dimension deviation, not an architecture concern — nothing in this dimension's scope is affected by it. |
| campaigns-email-editor | parity | `CampaignsController`, `CampaignsViewService`, `CampaignsFixtures`, `CampaignsView.vue`/`EmailEditorView.vue`, components `EditorShell`, `EmailDocument`, `EmailEnvelope`, `EmailInspector`, `EmailVariables`, `BlockLibrary`: same result. DEV-7 (campaigns: SPA makes no POST …/blocks) and DEV-4 (contract http-compare rule) are contract-dimension deviations; not architecture-relevant. |

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat) are regression guards only for this dimension: their ArchUnit rules (layering, Mercure-publisher access, `/accounts/` literal) and ESLint rules (Intl ban, `event-row` class ban) all still hold at HEAD (see `archunit-baseline.log`, `eslint-baseline.log` — full-repo runs, not journey-scoped).

## Rules-translated.md coverage — one row at a time

| Row | Target tooling (as declared) | Enforced in this wave-3 checkout | Negative fired? |
| --- | --- | --- | --- |
| Static typing gate | `-Xlint:all` (Maven compiler, confirmed in `backend/pom.xml`); `vue-tsc --noEmit` with `"strict": true` (`frontend/tsconfig.json`) | present, unchanged since b87a701 | not re-proved this round (wave-0 mechanism, not modified this wave; outside the architecture dimension's own command surface — compiler/`vue-tsc`, not ArchUnit/ESLint) |
| Formatting `@Symfony` → Spotless/Prettier | `spotless-maven-plugin` (`backend/pom.xml`); Prettier (existing `frontend` config) | present, unchanged since b87a701 | not re-proved (same reasoning as above) |
| Test policy fail-on-warning | `FailOnWarnLogExtension` (JUnit, `backend/src/test/java/click/kivvi/testsupport/FailOnWarnLogExtension.java`); Vitest reporter | present, unchanged since b87a701 | not re-proved (unit/integration dimension's command surface, not architecture's) |
| **Layering convention** (web/application/domain/infrastructure, no cycles) | `ArchitectureTest.domainDoesNotDependOnWeb`, `.infrastructureIsOnlyUsedFromApplication`, `.topLevelPackagesFormNoCycle` | **yes** — verified the new `infrastructure.session` package (filter + lock registry) sits correctly: no import of it from `domain`/`application`/`web` anywhere in `src/main/java` (see `shell-and-scope-checks.log`); `SessionRequestSerializationConfig` is `@Configuration`-scanned, never imported directly, so it never becomes a second dependency edge | **yes** — 3 throwaway probes (`domain`→`web` field, `web`→`infrastructure.session` constructor call, `web`→`MercurePublisher` method call) turned all 3 ArchUnit tests + the cycle test red with the exact expected messages; reverted, `git status --porcelain` empty, tests green again (`archunit-negative-probes.log`, `probe-cleanup.log`, `archunit-baseline.log`) |
| **Mercure topic built only server-side** | `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; ESLint `no-restricted-syntax` on `/accounts/` literals (`src/**/*.{ts,vue}` block) | yes, unchanged — no wave-3 file references `MercurePublisher`/`HttpMercurePublisher` outside `application.tracking`/`infrastructure.mercure`, no `/accounts/` literal outside `EventStreamTopic.java` | **yes** — backend probe (`web` class calling `MercurePublisher.publish(...)`) failed `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; backend probe (stray `"/accounts/1/events"` literal in `application/probe`) failed `accountsTopicLiteralExistsOnlyInEventStreamTopic`; frontend probe (`PROBE_TOPIC = "/accounts/1/events"` in `src/probe/probeMercureTopic.ts`) failed ESLint's `no-restricted-syntax` (`Literal`/`TemplateElement` selectors) — all reverted, clean |
| **Row markup in one place** (`.event-row` only in `EventRow.vue`) | ESLint `vue/no-restricted-class` | yes, unchanged — no wave-3 component renders `.event-row` | **yes** — frontend probe `ProbeEventRow.vue` with a bare `class="event-row"` failed `vue/no-restricted-class`; reverted, clean |
| Formatting decided server-side (`format.ts`) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl` | yes, unchanged — no wave-3 file imports or calls `Intl` outside `format.ts` | **yes** — frontend probe `probeIntl.ts` calling `new Intl.NumberFormat(...)` failed both `no-restricted-globals` and `no-restricted-syntax`; reverted, clean |
| Storybook dev-only | Vite `build` config excludes `stories/` | out of scope for parity (rules-translated.md says so explicitly); not touched this wave | n/a |

No row of `rules-translated.md` lacks tooling; no deviation was needed for architecture (DEV-4/DEV-7/DEV-12 in scope for this wave are all contract/visual-dimension deviations, none of them architecture).

## Shell files and new backend package — specific checks required by the packet

- `frontend/src/router/scrollRestoration.ts`: imports only `vue-router`'s `RouterScrollBehavior` type. No journey import.
- `frontend/src/router/localeHref.ts`: imports only `vue-router` types; `buildLocaleHref` is explicitly documented and implemented as journey-agnostic (reads `route.meta.defaultParams`, generic across journeys).
- `frontend/src/router/index.ts`: imports `./routes`, `./scrollRestoration` only.
- `frontend/src/stores/shell.ts`: imports only `pinia`.
- `frontend/src/components/organisms/Topbar.vue`: imports `vue`, `vue-i18n`, `vue-router`, shared atoms `Icon.vue`/`Kbd.vue`, and `@/router/localeHref` — all shared/framework, no journey-specific import.
- `click.kivvi.infrastructure.session` (`SessionRequestSerializationFilter`, `SessionLockRegistry`, `SessionRequestSerializationConfig`): correctly placed under `infrastructure`; `SessionLockRegistry` is package-private (compiler-enforced, only reachable from its own package); `SessionRequestSerializationConfig` is `@Configuration` (component-scanned, no direct importer anywhere); no `domain`/`application`/`web` class imports anything from this package (grep-confirmed, `shell-and-scope-checks.log`). The negative probes above prove the layering rule would actually catch a `web`→`infrastructure.session` access if one were ever added.

## Cross-journey imports

Confirmed for all five wave-3 views (`AutomationsView.vue`, `AutomationEditorView.vue`, `CampaignsView.vue`, `EmailEditorView.vue`, `SettingsView.vue`): every import is either a Vue/vue-router/vue-i18n framework import, a shared `atoms`/`molecules`/`organisms` primitive (`Button`, `Icon`, `Chip`, `Card`, `FilterChip`, `Table`, `KpiGrid`, `PageHead`, `Segmented`, `SimulationCard`, `Tabs`), or a component the view's own journey owns. Cross-checked the reverse direction too: journey-owned organisms (`AutoCard`, `FlowCanvas`, `RulePipeline`, `EditorShell`, `EmailDocument`, `EmailEnvelope`, `EmailInspector`, `EmailVariables`, `BlockLibrary`, `SettingsNav`) are each imported only from their own journey's view/sibling component (one incidental doc-comment mention of `EmailDocument.vue` in `CouponCode.vue`, not an import). No cross-journey leakage. Full grep output in `shell-and-scope-checks.log`.

## Global constraints (protected paths, Flyway, dependency pins) since wave-2's assembled tip `b87a701`

- `git diff --stat b87a701..HEAD -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` → empty, exit 0. No old-stack/protected path touched.
- Flyway: only `backend/src/main/resources/db/migration/V1__baseline.sql` exists; `git diff --stat b87a701..HEAD -- backend/src/main/resources/db/migration` → empty, exit 0. No schema drift.
- Dependency pins: `git diff b87a701..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` → empty. No dependency added/bumped this wave.
- Full changed-file list (`b87a701..HEAD`, 100 files) is entirely inside `backend/src/main/java/click/kivvi/{application,fixtures,infrastructure/session,web}`, `backend/src/test/java`, `frontend/src/{components,composables,i18n,router,stores,views}`, `frontend/test`, and `tools/migration-verify/{compare.mjs,deviations.json}` — matches the wave-3 scope (automations/settings/campaigns-email-editor + the shell repair) with no unrelated-package touch (no `tracking`, `customers`, `scheduling`, `landing`, `feeds` backend package modified). See `global-constraints.log`.

## Commands run

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test -Dtest=ArchitectureTest` (baseline, clean checkout) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/backend` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/frontend` | 0 |
| 3 | `npm run lint` (baseline, clean checkout) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/frontend` | 0 (6 pre-existing formatting *warnings*, 0 errors — `vue/multiline-html-element-content-newline` in `HookRow.vue`, `FeedCard.vue`, `ApiTab.vue`; not a boundary-rule warning, not this dimension's concern) |
| 4 | (write 4 throwaway backend negative probes: `domain.probe.ProbeDomainToWeb`, `web.probe.ProbeWebToInfrastructure`, `web.probe.ProbeWebToMercurePublisher`, `application.probe.ProbeAccountsLiteral`) | `backend/src/main/java/click/kivvi/{domain,web,application}/probe/*.java` | — |
| 5 | `./mvnw -q test -Dtest=ArchitectureTest` (with probes) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/backend` | 1 — **5/5 ArchUnit tests failed**, each with the expected violation message |
| 6 | `rm -rf backend/src/main/java/click/kivvi/{domain,web,application}/probe` then `git status --porcelain` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` | 0 (empty status) |
| 7 | `./mvnw -q test -Dtest=ArchitectureTest` (re-confirm clean/green) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/backend` | 0 |
| 8 | (write 3 throwaway frontend negative probes: `src/probe/probeIntl.ts`, `src/probe/ProbeEventRow.vue`, `src/probe/probeMercureTopic.ts`) | `frontend/src/probe/*` | — |
| 9 | `npm run lint` (with probes) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/frontend` | 1 — **4 errors**: `vue/no-restricted-class` on `ProbeEventRow.vue`, `no-restricted-globals` + `no-restricted-syntax` on `probeIntl.ts`, `no-restricted-syntax` on `probeMercureTopic.ts` |
| 10 | `rm -rf frontend/src/probe` then `git status --porcelain` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` | 0 (empty status) |
| 11 | `npm run lint` (re-confirm clean/green) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture/frontend` | 0 (same 6 warnings, 0 errors) |
| 12 | `git diff --stat b87a701..HEAD -- <protected paths>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` | 0, empty |
| 13 | `git diff --stat b87a701..HEAD -- backend/src/main/resources/db/migration` | same | 0, empty |
| 14 | `git diff b87a701..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` | same | 0, empty |
| 15 | `git diff --name-only b87a701..HEAD` (scope sanity check) | same | 0 |
| 16 | grep-based shell-file import checks, cross-journey import checks, `infrastructure.session` import checks | same | — (all confirmed clean, see log) |
| 17 | final `git status --porcelain` and `git rev-parse HEAD` | same | 0 (empty); `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e` |

## Evidence paths

- `waves/wave-3/architecture/archunit-baseline.log` — clean-checkout `ArchitectureTest` run, 0 failures.
- `waves/wave-3/architecture/archunit-negative-probes.log` — same command with 4 throwaway probes injected, 5/5 tests fail with expected messages; full surefire report included.
- `waves/wave-3/architecture/eslint-baseline.log` — clean-checkout `npm run lint`, 0 errors / 6 pre-existing formatting warnings.
- `waves/wave-3/architecture/eslint-negative-probes.log` — same command with 3 throwaway probes injected, 4 errors with expected rule names/messages.
- `waves/wave-3/architecture/probe-cleanup.log` — `git status --porcelain` empty after each cleanup (backend and frontend).
- `waves/wave-3/architecture/global-constraints.log` — protected-path diff, Flyway migration list/diff, dependency-pin diff, full changed-file list, all since `b87a701`.
- `waves/wave-3/architecture/shell-and-scope-checks.log` — shell-file import dumps, `infrastructure.session` placement/import grep, top-level package list, cross-journey import checks (forward and reverse) for all five wave-3 views/components.

## Conclusion

All 5 ArchUnit rules and all 3 ESLint boundary-rule families named in `rules-translated.md` are live, correctly scoped to include the wave-3 additions (`infrastructure.session`, automations/settings/campaigns-email-editor code), and were each independently proven to fire on a throwaway violation this round, then cleanly reverted (checkout `git status --porcelain` empty, HEAD unchanged at `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`). No cross-journey import beyond shared `atoms`/`molecules`/`organisms` components. No protected old-stack path, Flyway migration, or dependency pin drifted since wave-2's assembled tip `b87a701`. No rule in `rules-translated.md` lacks tooling; no deviation was needed for the architecture dimension.

**Dimension status: PASS.**
