# Wave-3 verifier report — dimension: architecture (round 3)

**Dimension status: PASS**, bound to wave SHA `60296f16250c6ebd25f7b4435c795268772fb6be`.

Verifier: independent, read-only. Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture`
(detached at the wave SHA). No worker reports, review files, or earlier-round evidence were read.
No Docker stack was started/stopped or touched (architecture dimension needs no running stack).
All throwaway violation-probe files were deleted after use; `git status --porcelain` is empty in
the checkout as of the final command below.

## Per-journey verdict

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | Owned files (`AutomationsController`, `AutomationsViewService`, `AutomationsFixtures`, `FlowCanvas.vue`, `RulePipeline.vue`, `AutoCard.vue`, `FlowNode.vue`, `RbBlock.vue`, `RbStep.vue`, `useIntents.ts`, i18n) import only their own package/fixtures plus shared atoms/molecules/domain — no cross-journey imports, no rule violation. |
| settings | parity | Owned files (`SettingsController`, `SettingsViewService`, `SettingsFixtures`, `SettingsView.vue`, `components/settings/*`, `SettingsNav.vue`, `DnsRow.vue`, `ToggleRow.vue`, `CodeBlock.vue`, `highlight.ts`) import only their own package/fixtures plus shared atoms/molecules/domain — no cross-journey imports, no rule violation. |
| campaigns-email-editor | parity | Owned files (`CampaignsController`, `CampaignsViewService`, `CampaignsFixtures`, `CampaignsView.vue`, `EmailEditorView.vue`, `EditorShell.vue`, `BlockLibrary.vue`, `EmailDocument.vue`, `EmailEnvelope.vue`, `EmailInspector.vue`, `EmailVariables.vue`, `CouponCode.vue`, `useEditorDrag.ts`) import only their own package/fixtures plus shared atoms/molecules/organisms (`KpiGrid`, `PageHead`) — no cross-journey imports, no rule violation. DEV-7 (no `POST …/blocks` call) is a contract-dimension deviation, not architecture; nothing to enforce here. |

No deviation was needed for the architecture dimension in this wave (consistent with
`rules-translated.md`'s own statement that every source rule has a target equivalent).

## Rule → enforcement mapping and negative-fire proof

Every row of `architecture/rules-translated.md` mapped to its enforcing ArchUnit test / ESLint
rule / build-gate, each proven to fire on a deliberate, throwaway violation, then reverted.

| # | Rule (rules-translated.md) | Enforcement | Negative probe | Result | Evidence |
| - | --- | --- | --- | --- | --- |
| 1 | Static typing gate | Maven `maven-compiler-plugin` `-Xlint:all -Werror` (backend `pom.xml`); `vue-tsc --noEmit` under `frontend/tsconfig.json` (`strict: true`, `noUncheckedIndexedAccess`, …) | (a) raw-type `List`/`ArrayList` field in a throwaway `domain` class → `mvn compile`; (b) `const probe: number = "not-a-number"` in a throwaway `.ts` → `npm run typecheck` | (a) FAILED: `warnings found and -Werror specified`; (b) FAILED: `TS2322: Type 'string' is not assignable to type 'number'` | `17-negative-javac-werror-rawtypes.log`, `18-negative-vuetsc-typeerror.log` |
| 2 | Formatting `@Symfony` | `spotless-maven-plugin` (google-java-format), bound to `verify` phase; Prettier 3.8.x (`npm run format:check`) | (a) Allman-braced throwaway `domain` class → `mvn spotless:check`; (b) unformatted throwaway `.ts` → `npm run format:check` | (a) FAILED: diff shown for `ArchProbeFormatting.java`; (b) FAILED: `Code style issues found` | `19-negative-spotless-formatting.log`, `20-negative-prettier-formatting.log` |
| 3 | Test policy fail-on-warning | Backend: `FailOnWarnLogExtension` (JUnit auto-detected extension, `click.kivvi.*` logger WARN+/unexpected `System.err`); frontend: `test/setup.ts` (`console.warn`/`console.error` from first-party `src/**` stack frames, and Vue's `warnHandler` unconditionally) | (a) repo's own `FailOnWarnLogExtensionTest` (white-box proof of the detection logic) run directly; (b) throwaway JUnit test with a live `LOG.warn(...)` on a `click.kivvi.*` logger; (c) throwaway Vitest test mounting `Icon.vue` without its required `name` prop (real Vue runtime warning) | (a) 7/7 green (proves detector reports WARN/ERROR, ignores INFO/DEBUG/blank); (b) FAILED: `AssertionError: click.kivvi.* logged at WARN or above …`; (c) FAILED: `Test policy: … Vue warning: Missing required prop: "name"` | `22-backend-failonwarn-selftest.log`, `21-negative-backend-live-failonwarn.log`, `23-negative-frontend-live-failonwarn.log` |
| 4 | Layering convention (controller → application → domain → infrastructure, no cycles) | `ArchitectureTest.domainDoesNotDependOnWeb`, `.infrastructureIsOnlyUsedFromApplication`, `.topLevelPackagesFormNoCycle` | (a) throwaway `domain` class with a field typed `click.kivvi.web.AutomationsController`; (b) throwaway `web` class calling a method on `click.kivvi.infrastructure.NavigationLabels` | (a) FAILED both `domainDoesNotDependOnWeb` **and** `topLevelPackagesFormNoCycle` (the domain→web edge closes a cycle through `web`'s own existing dependency on `application`/`domain`) — 2/5 sub-tests red; (b) FAILED `infrastructureIsOnlyUsedFromApplication` only — 1/5 sub-tests red | `10-negative-domain-to-web-and-cycle.log`, `11-negative-web-to-infrastructure.log` |
| 5 | Mercure topic built only server-side | `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` + `.accountsTopicLiteralExistsOnlyInEventStreamTopic` (backend); ESLint `no-restricted-syntax` (`Literal`/`TemplateElement` matching `/accounts\//`) scoped to `src/**/*.{ts,vue}` (frontend) | (a) throwaway `web` class calling `MercurePublisher.publish(...)`; (b) throwaway `domain` class holding a second `"/accounts/1/events"` string literal; (c) throwaway `.vue` file under `src/components/molecules/` with a `"/accounts/1/events"` literal in its `<script setup>` | (a) FAILED `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` (and, collaterally, `infrastructureIsOnlyUsedFromApplication`, correctly — the publisher is also `infrastructure`); (b) FAILED `accountsTopicLiteralExistsOnlyInEventStreamTopic` (two files now match, assertion expects exactly one); (c) FAILED ESLint `no-restricted-syntax` with the exact "Mercure topics are built server-side only" message | `12-negative-mercure-publisher-access.log`, `13-negative-accounts-literal-sourcescan.log`, `14-negative-eslint-accounts-literal.log` |
| 6 | Row markup in one place (`EventRow.vue` only renders `.event-row`) | ESLint `vue/no-restricted-class: ["error", "event-row"]` scoped to `src/**/*.vue`, `ignores: ["src/components/molecules/EventRow.vue"]` | throwaway `.vue` file with `<div class="event-row">` | FAILED: `'event-row' class is not allowed` (`vue/no-restricted-class`) | `15-negative-eslint-event-row-class.log` |
| 7 | Formatting decided server-side (`format.ts` is the only `Intl` importer) | ESLint `no-restricted-imports` (pattern `*Intl*`), `no-restricted-globals` (`Intl`), `no-restricted-syntax` (`Intl.*` member expr, `.toLocaleString()`, `.toFixed()`) — all with a carve-out for `src/format.ts` | throwaway `src/archProbeIntlUsage.ts` calling `new Intl.NumberFormat("pl-PL").format(value)` (outside `format.ts`) | FAILED: both `no-restricted-globals` (`Unexpected use of 'Intl'`) and `no-restricted-syntax` fired on the same line | `16-negative-eslint-intl-usage.log` |
| 8 | Storybook dev-only | Vite `build` config would exclude a `stories/` dir; no Vue storybook exists yet in `frontend/` (only the old Symfony `config/storybook.php`/`src/Storybook`/`templates/storybook`, untouched, old-stack-only) | not applicable — no stories directory exists in the new stack to probe | **N/A this wave** (rules-translated.md itself marks this row's Wave column "Out of scope for parity"; not tied to wave-3 or to any behaviour/deviation in scope) | — |

## Shell-file regression guard (wave-3 note)

Read and grepped the five shell files named in the packet for `Intl`, `toLocaleString(`, `toFixed(`,
`/accounts/`, and `event-row`; none found (one unrelated code-comment mention of
`.event-row__time` in `scrollRestoration.ts`, not an actual class binding, not parsed by
`vue/no-restricted-class` which only inspects `.vue` template `class` attributes):

- `frontend/src/router/scrollRestoration.ts` — imports only `vue-router` types.
- `frontend/src/router/localeHref.ts` — imports only `vue-router` types.
- `frontend/src/router/index.ts` — imports `vue-router`, local `./routes`, `./scrollRestoration`.
- `frontend/src/components/organisms/Topbar.vue` — imports `vue`, `vue-i18n`, `vue-router`,
  shared atoms (`Icon`, `Kbd`), and `@/router/localeHref`.
- `frontend/src/stores/shell.ts` — imports only `pinia`.

All clean; all pass `npm run lint` / `npm run typecheck` in the full-repo baseline run.

## Cross-journey import check

Grepped every `import` line in each journey's owned files (backend `web`/`application`/`fixtures`
classes and frontend `views`/journey-specific `components`/`composables`) for wave-3's three
journeys. Findings:

- **automations**: only shared atoms/molecules (`Button`, `Chip`, `Icon`, `Card`, `Tabs`,
  `Segmented`, `FilterChip`), shared organism `PageHead`, shared store `shell.ts`, own package
  members. No import from `settings` or `campaigns`/email-editor code.
- **settings**: only shared atoms/molecules and shared organism `PageHead`, own
  `components/settings/*` and own fixtures/service/controller. No import from `automations` or
  `campaigns` code.
- **campaigns-email-editor**: only shared atoms/molecules, shared organism `KpiGrid` (documented
  in the plan's dependency audit as a Group-C-wide read-only shared consumer) and `PageHead`, own
  package members. No import from `automations` or `settings` code.
- Backend: `AutomationsController`/`SettingsController`/`CampaignsController` and their
  `*ViewService`/`*Fixtures` each import only `click.kivvi.domain.*` (shared) and their own
  `fixtures` class — zero cross-journey backend imports confirmed by direct grep.

No cross-journey imports beyond shared components found.

## Protected-path / dependency-pin / Flyway drift since `b87a701`

`git diff --stat b87a701 HEAD` (93 files changed, all under `backend/src/**`, `frontend/src/**`,
`frontend/test/**`, and `tools/migration-verify/{compare.mjs,deviations.json}`):

- **Protected paths (old Symfony stack)**: `git diff --stat b87a701 HEAD -- src/ templates/ assets/ config/ translations/ migrations/ composer.json composer.lock symfony.lock importmap.php public/ bin/` → empty. No old-stack path touched.
- **Dependency pins**: `git diff --stat b87a701 HEAD -- '**/pom.xml' pom.xml '**/package.json' package.json '**/package-lock.json' package-lock.json composer.json composer.lock yarn.lock symfony.lock` → empty. No dependency-manifest drift.
- **Flyway**: `git diff --stat b87a701 HEAD -- '**/db/migration/**'` → empty; `find backend/src/main/resources/db/migration -type f` → only `V1__baseline.sql` (the frozen wave-0 baseline, unmodified). No new or edited migration.
- `tools/migration-verify/deviations.json` changed (DEV-12 `steps` object gained a `"settings": [10]` entry) — expected verifier-tooling metadata, matches this packet's own "Deviations in scope: … DEV-12 (settings step 10 = 404 document)"; not a protected path.

No drift found.

## Commands run (cwd, exit code)

Environment for every backend command: `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH`. `-Dmaven.repo.local=<scratchpad>/m2` used throughout (writes only to the scratchpad, never `~/.m2`, per the confirmed-present shared read cache being left untouched). `frontend/node_modules` installed via `npm ci` once at the start (not present in the checkout initially).

| Step | cwd | Command | Exit |
| --- | --- | --- | --- |
| install | `frontend/` | `npm ci` | 0 |
| baseline | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | 0 |
| baseline | `frontend/` | `npm run lint` | 0 (6 pre-existing unrelated Vue formatting *warnings*, 0 errors) |
| baseline | `frontend/` | `npm run typecheck` | 0 |
| baseline | `backend/` | `./mvnw -q spotless:check` | 0 |
| baseline | `frontend/` | `npm run format:check` | 0 |
| probe 1 (add) | `backend/` | add `domain/ArchProbeDomainToWeb.java` (field of type `web.AutomationsController`) | — |
| probe 1 (run) | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | **1** — `domainDoesNotDependOnWeb`, `topLevelPackagesFormNoCycle` FAIL (2/5) |
| probe 1 (revert) | `backend/` | `rm domain/ArchProbeDomainToWeb.java` | — |
| probe 2 (add) | `backend/` | add `web/ArchProbeWebToInfrastructure.java` (calls `NavigationLabels.label(...)`) | — |
| probe 2 (run) | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | **1** — `infrastructureIsOnlyUsedFromApplication` FAIL (1/5) |
| probe 2 (revert) | `backend/` | `rm web/ArchProbeWebToInfrastructure.java` | — |
| probe 3 (add) | `backend/` | add `web/ArchProbeMercurePublisherAccess.java` (calls `MercurePublisher.publish(...)`) | — |
| probe 3 (run) | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | **1** — `infrastructureIsOnlyUsedFromApplication`, `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` FAIL (2/5) |
| probe 3 (revert) | `backend/` | `rm web/ArchProbeMercurePublisherAccess.java` | — |
| probe 4 (add) | `backend/` | add `domain/ArchProbeAccountsLiteral.java` (second `"/accounts/1/events"` literal) | — |
| probe 4 (run) | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | **1** — `accountsTopicLiteralExistsOnlyInEventStreamTopic` FAIL (1/5) |
| probe 4 (revert) | `backend/` | `rm domain/ArchProbeAccountsLiteral.java` | — |
| probe 5 (add+run+revert) | `frontend/` | add `src/components/molecules/ArchProbeAccountsLiteral.vue` (`/accounts/1/events` literal); `npm run lint`; `rm` | **1** — `no-restricted-syntax` |
| probe 6 (add+run+revert) | `frontend/` | add `src/components/molecules/ArchProbeEventRowClass.vue` (`class="event-row"`); `npm run lint`; `rm` | **1** — `vue/no-restricted-class` |
| probe 7 (add+run+revert) | `frontend/` | add `src/archProbeIntlUsage.ts` (`new Intl.NumberFormat(...)`); `npm run lint`; `rm` | **1** — `no-restricted-globals`, `no-restricted-syntax` |
| probe 8 (add+run+revert) | `backend/` | add `domain/ArchProbeUncheckedWarning.java` (raw `List`); `./mvnw -q compile`; `rm` | **1** — `warnings found and -Werror specified` |
| probe 9 (add+run+revert) | `frontend/` | add `src/archProbeTypeError.ts` (`const x: number = "not-a-number"`); `npm run typecheck`; `rm` | **2** — `TS2322` |
| probe 10 (add+run+revert) | `backend/` | add `domain/ArchProbeFormatting.java` (Allman braces); `./mvnw -q spotless:check`; `rm` | **1** — spotless diff |
| probe 11 (add+run+revert) | `frontend/` | add `src/archProbeFormatting.ts` (unformatted); `npm run format:check`; `rm` | **1** — prettier warn |
| probe 12 (add+run+revert) | `backend/` | add `testsupport/ArchProbeLiveWarnTest.java` (`LOG.warn(...)`); `./mvnw -q test -Dtest=ArchProbeLiveWarnTest`; `rm` | **1** — `FailOnWarnLogExtension` AssertionError |
| self-test | `backend/` | `./mvnw -q test -Dtest=FailOnWarnLogExtensionTest` | 0 (7/7 green — white-box proof) |
| probe 13 (add+run+revert) | `frontend/` | add `test/unit/archProbeLiveWarn.spec.ts` (mount `Icon` without required `name` prop); `npm run test -- --run test/unit/archProbeLiveWarn.spec.ts`; `rm` | **1** — fail-on-warning policy (`Missing required prop: "name"`) |
| final | `backend/` | `./mvnw -q test -Dtest=ArchitectureTest` | 0 |
| final | `frontend/` | `npm run lint` | 0 |
| final | `frontend/` | `npm run typecheck` | 0 |
| final | `frontend/` | `npm run format:check` | 0 |
| final | `backend/` | `./mvnw -q spotless:check` | 0 |
| final | (repo root of checkout) | `git status --porcelain` | 0, empty output |

## Evidence paths

All under `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/architecture/`:

- `01-archunit-baseline.log`, `02-eslint-baseline.log`, `03-typecheck-baseline.log`,
  `04-spotless-baseline.log`, `05-prettier-baseline.log` — pre-probe baselines, all green.
- `10-negative-domain-to-web-and-cycle.log` … `16-negative-eslint-intl-usage.log` — layering/Mercure
  negative-fire proofs (backend ArchUnit + frontend ESLint).
- `17-negative-javac-werror-rawtypes.log`, `18-negative-vuetsc-typeerror.log` — static-typing-gate
  negative-fire proofs.
- `19-negative-spotless-formatting.log`, `20-negative-prettier-formatting.log` — formatting-gate
  negative-fire proofs.
- `21-negative-backend-live-failonwarn.log`, `22-backend-failonwarn-selftest.log`,
  `23-negative-frontend-live-failonwarn.log` — fail-on-warning test-policy proofs.
- `30-archunit-final.log` … `34-spotless-final.log` — post-cleanup re-confirmation, all green.

## Conclusion

Every row of `architecture/rules-translated.md` applicable to this wave has a named, currently
green ArchUnit test / ESLint rule / build gate, and every one of them was proven to fire on a
real, throwaway violation before being reverted. The wave-3 shell-file regression guard (router
scroll restoration, locale toggle, Topbar, shell store) is clean. No cross-journey imports beyond
shared components exist between automations, settings, and campaigns-email-editor. No
protected-path, dependency-pin, or Flyway drift exists since `b87a701`. Checkout is clean
(`git status --porcelain` empty).

**Dimension status: PASS.** Per-journey: automations = parity, settings = parity,
campaigns-email-editor = parity.
