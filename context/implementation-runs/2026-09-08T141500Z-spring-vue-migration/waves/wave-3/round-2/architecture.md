# Wave-3 verifier — architecture dimension (round 2)

**Dimension status: PASS**, bound to wave SHA `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4`.

Independent, read-only verification from a detached checkout at
`/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` (confirmed `git rev-parse HEAD` = the wave SHA
throughout; `git status --porcelain` empty at the end). No worker reports, review files, or round-1 evidence
were read. No Docker stack was started or stopped. No tracked file was left modified — every throwaway
negative-case probe file was deleted immediately after its run.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | own organisms (`AutoCard`, `FlowCanvas`, `RulePipeline`) imported only by `AutomationsView`/`AutomationEditorView`; no cross-journey import; ESLint clean |
| settings | parity | `components/settings/*` and `SettingsNav` imported only by `SettingsView`; `defaultParams` route-meta addition is journey-local; DEV-12 extension in scope; ESLint clean |
| campaigns-email-editor | parity | `BlockLibrary`/`EditorShell`/`EmailDocument`/`EmailEnvelope`/`EmailInspector`/`EmailVariables` imported only by `EmailEditorView`; `CampaignsView` imports only shared atoms/molecules/organisms; DEV-7 (no `POST …/blocks`) is a contract-dimension deviation, not an architecture concern; ESLint clean |

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat) are regression
guards only for this dimension; the shell changes below are the only wave-3 additions outside the three named
journeys and were checked explicitly.

## Rules-translated.md coverage

Every row of `context/migration-oracle/symfony-to-spring-vue/architecture/rules-translated.md` maps to a
named enforcement mechanism and, where that mechanism is ArchUnit or ESLint (the architecture dimension's
contract scope), a negative case was proven to fire this round. Full detail, including two rounds of probe
files, exact error text, and the one scope caveat found (a bare `.class` literal reference does not trip
ArchUnit's `onlyBeAccessed()`, only the earlier `dependOnClassesThat()`-based rule and a real
call/constructor/field access do — expected ArchUnit semantics, not a rule gap) is in
`architecture/negative-cases.md`. Summary:

| Rule | Enforcement | Negative case proven |
| --- | --- | --- |
| Layering (`web`→`domain`, `application`→`infrastructure`, no cycles) | ArchUnit `ArchitectureTest`: `domainDoesNotDependOnWeb`, `infrastructureIsOnlyUsedFromApplication`, `topLevelPackagesFormNoCycle` | yes — 3/3 |
| Mercure topic built only server-side | ArchUnit `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` + `accountsTopicLiteralExistsOnlyInEventStreamTopic`; ESLint `no-restricted-syntax` (`/accounts/` literal, `src/**/*.{ts,vue}`) | yes — 3/3 |
| Row markup in one place (`.event-row`) | ESLint `vue/no-restricted-class` scoped to all `src/**/*.vue` except `EventRow.vue` | yes |
| Formatting decided server-side (`Intl`) | ESLint `no-restricted-imports`, `no-restricted-globals`, `no-restricted-syntax` scoped to all `src/**` except `format.ts` | yes |
| Static typing gate, Formatting (`@Symfony`/Prettier), Test policy fail-on-warning | Maven compiler / `vue-tsc`, `spotless-maven-plugin`/Prettier, JUnit/Vitest reporter — the plan's separate "Static analysis" gate row, not this dimension's contract command | out of this dimension's scope by the plan's own gate split; not a rule gap (each names its tool in `rules-translated.md` itself) |
| Storybook dev-only | Vite build config exclusion | explicitly "Out of scope for parity" in `rules-translated.md`; recorded as DEV-10 | 

## Shell-file check (wave-3 note)

`frontend/src/router/scrollRestoration.ts`, `frontend/src/router/localeHref.ts`, `frontend/src/router/index.ts`,
`frontend/src/components/organisms/Topbar.vue` — read in full; none touch `Intl`, `/accounts/` literals, or
`.event-row` markup. `npx eslint` on exactly these four files: 0 problems (`shell-files-lint.txt`). Also
covered by the clean full-repo `npm run lint` baseline run (0 errors, 6 pre-existing
`vue/multiline-html-element-content-newline` warnings in unrelated files, unchanged from before this wave —
`eslint-baseline.txt`).

## Journey isolation

`journey-import-boundaries.txt`: grepped every journey-specific organism/component group
(automations: `AutoCard`/`FlowCanvas`/`RulePipeline`; settings: `SettingsNav`/`components/settings/*`;
campaigns-email-editor: `BlockLibrary`/`EditorShell`/`EmailDocument`/`EmailEnvelope`/`EmailInspector`/
`EmailVariables`) and confirmed each is imported only by its own journey's view(s), never by another
journey's view. Cross-journey sharing observed is limited to atoms/molecules/organisms explicitly designed
as shared (`Button`, `Card`, `PageHead`, `KpiGrid`, `FilterChip`, `Table`, etc.) — consistent with
`common-journey-rules.md`'s atomic-design layering; no violation found.

## Protected-path / dependency-pin / Flyway drift since `b87a701`

`b87a701` = `test: repair-2 for w2-event-stream` (last wave-2 commit before wave-3 started). `drift-check.txt`:

- `git diff --stat b87a701..HEAD -- compose.next.yaml compose.next.prod.yaml backend/Dockerfile backend/pom.xml frontend/package.json frontend/package-lock.json backend/src/main/resources/db` → empty (no changes) — no dependency-pin drift (Spring Boot/Java/Vue/Vite/vue-router/Pinia/vue-i18n/TS versions untouched), no compose/Dockerfile drift, no Flyway migration drift (schema still frozen at `V1__baseline.sql`).
- `frontend/src/i18n/index.ts`, `pl.ts`, `en.ts` untouched (protected files per `common-journey-rules.md`).
- `frontend/src/router/routes.ts` diff inspected line-by-line: only the automations/campaigns/emails/settings route entries were switched from `EmptyPageView` to their real views (plus a `defaultParams` meta addition on the settings route itself); every other route (import-wizard, popups, etc.) is untouched.
- `tools/migration-verify/deviations.json` diff inspected: only DEV-12's `steps` map gained a `"settings": [10]` entry, matching the packet's own "Deviations in scope" line; no new/unscoped deviation ids were added.
- Full 90-file changed list (`b87a701..HEAD`) reviewed; no touches to `assets/styles/*` equivalents, CSS, or any file outside `backend/src/{main,test}`, `frontend/src`, `frontend/test`, `tools/migration-verify/{compare.mjs,deviations.json}`.

## Commands run

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` (SHA confirmation) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` | 0 |
| 2 | `npm ci` | `.../frontend` | 0 |
| 3 | `npm run lint` (baseline) | `.../frontend` | 0 |
| 4 | `export JAVA_HOME=...jdk-25; ./mvnw -q test -Dtest=ArchitectureTest` (baseline, clean rebuild) | `.../backend` | 0 |
| 5 | 8× add-probe → rerun step 3 or 4 → observe failure → delete probe → confirm `git status --porcelain` empty (see `negative-cases.md` for each probe's file, command, and captured failure text) | `.../backend` and `.../frontend` | 1 each time a probe was present, 0 once removed |
| 6 | `npx eslint src/router/scrollRestoration.ts src/router/localeHref.ts src/router/index.ts src/components/organisms/Topbar.vue` | `.../frontend` | 0 |
| 7 | `grep -rln ... frontend/src` (journey-boundary greps, see `journey-import-boundaries.txt`) | `.../` (repo root) | n/a (grep) |
| 8 | `git diff --stat/--name-only b87a701..HEAD -- <protected paths>` and full diffs of `routes.ts`, `deviations.json` | `.../` (repo root) | n/a (git diff) |
| 9 | Final re-run of steps 3 and 4 to confirm PASS baseline after all probes removed | `.../backend`, `.../frontend` | 0, 0 |
| 10 | `git status --porcelain` (final cleanliness check) | `.../` (repo root) | 0, empty output |

## Evidence paths

- `architecture/archunit-baseline.txt` — full `mvnw -q test -Dtest=ArchitectureTest` output + surefire report (5/5 green)
- `architecture/eslint-baseline.txt` — full `npm run lint` output (0 errors, 6 pre-existing warnings)
- `architecture/negative-cases.md` — all 8 rule/negative-case proofs with exact failure text
- `architecture/shell-files-lint.txt` — targeted ESLint run on the four wave-3 shell files
- `architecture/journey-import-boundaries.txt` — cross-journey import grep evidence
- `architecture/drift-check.txt` — protected-path / dependency-pin / Flyway drift check since `b87a701`
