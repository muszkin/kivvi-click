# Wave-4 verifier verdict — dimension: architecture (round 2)

**Dimension status: PASS**, bound to wave SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`
(checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture`, detached at that SHA;
`git status --porcelain` empty at every checkpoint below except the read-only scratch Maven repo
`.m2repo-verify/`, created by this run and deleted at the end per the packet's disk instruction).

Journeys: popups-widget-editor, import-wizard, dashboard.

A note on process: while reconstructing the protected-path/dependency-pin/Flyway drift check, a
`grep` over the run directory incidentally matched (and printed a few lines of) files under
`waves/wave-4/round-1/`, which the packet explicitly says not to read. I did not use anything from
those lines as evidence — every claim in this verdict was independently re-derived from my own
fresh commands against the checkout (see "Global constraints" below), not from what leaked through
that grep.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| dashboard | parity | Journey-owned backend (`DashboardController`, `DashboardViewService`, `DashboardFixtures`, `DashboardResponse`) sits cleanly inside the `web`/`application`/`fixtures` layering; frontend (`DashboardView.vue`, `Cardiogram.vue`, `ListCard.vue`) has 0 ESLint errors. `ListCard.vue`'s `event-row` override is file-scoped and justified by `templates/pages/dashboard/{recent-customers,top-automations}.html.twig` (both use `class="event-row"` on the old stack). `EventStream.vue` and `KpiGrid.vue` are consumed read-only, matching the plan's declared cross-journey reuse. |
| import-wizard | parity | Journey-owned backend (`ImportController`, `ImportUploadController`, `ImportViewService`, `ImportUploadService`, `ImportUploadStorage`, `ImportFixtures`) layers correctly. Frontend (`ImportView.vue` + `components/import/*`, `Stepper.vue`) has 0 ESLint errors. `RecentImports.vue`'s `event-row` override is file-scoped and justified by `templates/pages/import/recent-imports.html.twig:14`. The round-2 repair's hardened `no-restricted-syntax` selectors (the exact bypass an earlier `RecentImports.vue` draft used — a script-side `const … = "event-row"` bound through `:class`) were proven to still fire outside the three allowed files. Upload directory defaults to `./var/import`, inside the `api` container, no host bind mount. |
| popups-widget-editor | parity | Journey-owned backend (`WidgetController`, `WidgetViewService`, `WidgetFixtures`, `WidgetsResponse`, `WidgetEditorResponse`) layers correctly. Frontend (`PopupsView.vue`, `PopupEditorView.vue`, `PopupStage.vue`, `PopupWidget.vue`, `PwTypeList.vue`, `TriggerRow.vue`, `SwatchGrid.vue`, `PositionGrid.vue`, `AddSlot.vue`) has 0 ESLint errors, no journey-private import from elsewhere. `EditorShell.vue`/`BlockLibrary.vue` reuse from campaigns-email-editor (J8) is declared and documented in both components' own header comments, matching the plan's Group D dependency audit. |

DEV-4 (contract, `*`), DEV-7 (contract, popups: no `POST …/blocks`), DEV-5/DEV-9 (contract,
import: session/table-name mapping), DEV-12 (visual, import step 10 = 404 document), DEV-1/DEV-2/
DEV-3 (visual/contract, dashboard: clock strings, cardiogram canvas mask, JSON event payload) are
all contract- or visual-dimension deviations. None of them changes an architecture-dimension
verdict — no ArchUnit or ESLint boundary rule is affected by any of them.

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat,
automations, settings, campaigns-email-editor) are regression guards only for this dimension.
`ArchitectureTest` imports the whole `click.kivvi` package tree and `eslint .` scans the whole
`frontend/src` tree, so every run below is a full-repo regression check, not scoped only to the
three wave-4 journeys — both came back clean (0 ArchUnit violations, 0 ESLint errors).

## Rules-translated.md coverage — every row, with a proven negative

| Row | Tooling | Negative fired this round? |
| --- | --- | --- |
| Static typing gate (PHPStan 5 equivalent) | `-Xlint:all -Werror` (`backend/pom.xml` maven-compiler-plugin); `vue-tsc --noEmit` strict | **yes** — raw-typed unchecked-warning probe failed `compile` with `-Werror`; deliberate `string`/`number` mismatch probe failed `vue-tsc --noEmit` with 2 `TS2322` errors |
| Formatting `@Symfony` | Spotless/google-java-format bound to Maven `verify` phase; Prettier | **yes** — mis-formatted Java probe failed `spotless:check`; mis-formatted TS probe failed `prettier --check` |
| Test policy fail-on-warning | `FailOnWarnLogExtension` (JUnit, auto-registered via `META-INF/services`); Vitest `dangerouslyIgnoreUnhandledErrors: false` | proof already self-contained in the suite (`FailOnWarnLogExtensionTest` asserts a deliberate WARN/ERROR event is reported) — re-ran it, green |
| **Layering convention** (web/application/domain/infrastructure, no cycles) | `ArchitectureTest.domainDoesNotDependOnWeb` / `.infrastructureIsOnlyUsedFromApplication` / `.topLevelPackagesFormNoCycle` | **yes** — 3 throwaway probes (domain field of type a `web` controller; a `web` method calling an `infrastructure.tracking` method; the same web/infra call feeding the cycle check) turned all 3 tests red with the expected violation messages |
| **Mercure topic built only server-side** | `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; `.accountsTopicLiteralExistsOnlyInEventStreamTopic`; ESLint `no-restricted-syntax` on `/accounts/` literals | **yes** — a `web` method calling `MercurePublisher.publish(...)` failed the access-boundary test; a stray `"/accounts/1/events"` literal in a second file failed the source-scan test; a frontend template-literal probe (`` `/accounts/${id}/events` ``) failed ESLint's `no-restricted-syntax` |
| **Row markup in one place** (`event-row`) | ESLint `vue/no-restricted-class` (file-scoped overrides for `EventRow.vue`, `ListCard.vue`, `RecentImports.vue`) + `no-restricted-syntax` script-side literal hardening (repair-3) | **yes** — see `event-row-rule.md`: (a) a bare `class="event-row"` outside the three files fails `vue/no-restricted-class`; (b) a script-side `const x = "event-row"` bound through `:class` outside the three files fails the hardened `no-restricted-syntax` selector; (c) the three allowed files lint clean; (d) each override is file-scoped (three explicit file paths, not a glob) and justified by a real old-stack Twig partial using the same literal class for the same styling-reuse reason |
| Formatting decided server-side (`format.ts`) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl` | **yes** — a probe calling `new Intl.NumberFormat(...)` outside `format.ts` failed both `no-restricted-globals` and `no-restricted-syntax` |
| Storybook dev-only | Vite `build` excludes `stories/` | out of scope for parity (rules-translated.md marks this row explicitly); not exercised, not required for this dimension's threshold |

No row lacks tooling; no deviation was required for the architecture dimension this wave.

## New backend packages — layering

`click.kivvi.web.WidgetController` / `application.WidgetViewService` / `fixtures.WidgetFixtures` /
`web.dto.{WidgetsResponse,WidgetEditorResponse}` (popups-widget-editor); the equivalent
`Dashboard*`/`Import*` classes (dashboard, import-wizard) — all sit in the pre-existing
`web`/`application`/`domain`/`infrastructure`/`fixtures`/`web.dto` top-level packages, no new
top-level package introduced this wave. `ArchitectureTest` (whole-repo `ClassFileImporter`) covers
all of them; the negative probes above prove the layering/publisher/topic rules would catch a
violation in any of these new classes exactly as they would anywhere else.

## No cross-journey imports beyond shared components

See `cross-journey-and-upload-dir.md` for the full audit. Every cross-journey import found
(`EventStream.vue` into `DashboardView.vue`, `KpiGrid.vue` broadly, `BlockLibrary.vue`/
`EditorShell.vue` from campaigns-email-editor into the popup editor) is declared and
plan-consistent; each reused component's own header comment documents the reuse. No wave-4 journey
privately imports another journey's non-shared component.

## Upload directory default

`upload-directory: ${KIVVI_IMPORT_UPLOAD_DIRECTORY:./var/import}` — relative path resolved inside
the `api` container's working directory; `compose.next.yaml` mounts no volume over it. Uploads
live in the container's writable layer, never on a host bind mount.

## Global constraints (protected paths, dependency pins, Flyway) since wave-3 SHA `11d3cc4`

Independently re-derived (not taken from any prior round's evidence):

- `git diff --stat 11d3cc4..HEAD -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` → empty, exit 0. No old-stack/protected path touched.
- `git diff --stat 11d3cc4..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` → empty, exit 0. No dependency-pin drift.
- Flyway: only `backend/src/main/resources/db/migration/V1__baseline.sql` exists; `git diff --stat 11d3cc4..HEAD -- backend/src/main/resources/db/migration` → empty, exit 0. No new/changed migration.
- Full changed-file list (`git diff --name-only 11d3cc4..HEAD`) is scoped to the three wave-4 journeys' backend/frontend/test/i18n files, `frontend/router/routes.ts` (only the popups/import route entries + the now-unused `EmptyPageView` import removal — dashboard's route already pointed at `DashboardView.vue` since wave-0), `frontend/eslint.config.js` (this round's repair-3 hardening), `frontend/README.md`, and `tools/migration-verify/{compare.mjs,deviations.json}`.

## Commands run (this round, all in the detached checkout)

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `npm ci` | `.../verify-wave-4-architecture/frontend` | 0 |
| 2 | `./mvnw -q -Dmaven.repo.local=<scratch> test -Dtest=ArchitectureTest` (baseline) | `.../verify-wave-4-architecture/backend` | 0 |
| 3 | `npm run lint` (baseline, final) | `.../verify-wave-4-architecture/frontend` | 0 (0 errors, 12 pre-existing formatting warnings unrelated to `event-row`) |
| 4 | `./mvnw -q -Dmaven.repo.local=<scratch> test -Dtest=ArchitectureTest` (4 probes present) | `.../verify-wave-4-architecture/backend` | 1 (5/5 tests failed as expected) |
| 5 | `./mvnw -q -Dmaven.repo.local=<scratch> test -Dtest=ArchitectureTest` (probes removed, re-run) | `.../verify-wave-4-architecture/backend` | 0 |
| 6 | `npx eslint src/components/molecules/ZZZProbeEventRowLiteral.vue` | `.../verify-wave-4-architecture/frontend` | 1 (expected) |
| 7 | `npx eslint src/components/molecules/ZZZProbeEventRowScript.vue` | `.../verify-wave-4-architecture/frontend` | 1 (expected) |
| 8 | `npx eslint src/components/molecules/EventRow.vue src/components/organisms/ListCard.vue src/components/import/RecentImports.vue` | `.../verify-wave-4-architecture/frontend` | 0 |
| 9 | `npx eslint src/ZZZProbeIntl.ts` | `.../verify-wave-4-architecture/frontend` | 1 (expected) |
| 10 | `npx eslint src/ZZZProbeTopic.ts` | `.../verify-wave-4-architecture/frontend` | 1 (expected) |
| 11 | `npx vue-tsc --noEmit -p tsconfig.json` (with type-error probe) | `.../verify-wave-4-architecture/frontend` | 2 (expected) |
| 12 | `npx prettier --check src/ZZZProbeFormat.ts` | `.../verify-wave-4-architecture/frontend` | 1 (expected) |
| 13 | `npm run format:check` (baseline) | `.../verify-wave-4-architecture/frontend` | 0 |
| 14 | `./mvnw -q -Dmaven.repo.local=<scratch> spotless:check` (with mis-formatted probe) | `.../verify-wave-4-architecture/backend` | 1 (expected) |
| 15 | `./mvnw -q -Dmaven.repo.local=<scratch> compile` (with unchecked-warning probe, no `@SuppressWarnings`) | `.../verify-wave-4-architecture/backend` | 1 (expected: `-Werror` failure) |
| 16 | `./mvnw -q -Dmaven.repo.local=<scratch> spotless:check compile` (probes removed, re-run) | `.../verify-wave-4-architecture/backend` | 0 |
| 17 | `./mvnw -q -Dmaven.repo.local=<scratch> test -Dtest=FailOnWarnLogExtensionTest` | `.../verify-wave-4-architecture/backend` | 0 |
| 18 | `git diff --stat 11d3cc4..HEAD -- <protected paths>` | `.../verify-wave-4-architecture` | 0, empty |
| 19 | `git diff --stat 11d3cc4..HEAD -- <dependency pins>` | `.../verify-wave-4-architecture` | 0, empty |
| 20 | `git diff --stat 11d3cc4..HEAD -- backend/.../db/migration` | `.../verify-wave-4-architecture` | 0, empty |
| 21 | `git status --porcelain` (repeated after every probe) | `.../verify-wave-4-architecture` | always empty except the scratch `.m2repo-verify/` (deleted at the end) |

## Evidence paths

- `waves/wave-4/architecture/negative-probes.md` — full rules-translated.md coverage table and every non-`event-row` negative-fire probe (ArchUnit x5, Intl, Mercure-topic literal, vue-tsc, Prettier, Spotless, `-Werror`).
- `waves/wave-4/architecture/event-row-rule.md` — the (a)/(b)/(c)/(d) breakdown for the `event-row` rule, including the old-stack Twig partial evidence.
- `waves/wave-4/architecture/cross-journey-and-upload-dir.md` — cross-journey import audit and upload-directory default evidence.
- `waves/wave-4/architecture/global-constraints-drift.txt` — protected-path/dependency-pin/Flyway diff commands and output, plus the full changed-file list since `11d3cc4`.
- `waves/wave-4/architecture/archunit-clean-run.txt` — final clean `ArchitectureTest` run (post-cleanup).
- `waves/wave-4/architecture/archunit-negative-run-raw.txt` — raw Surefire output with all 4 probes present (5/5 tests failed).
- `waves/wave-4/architecture/eslint-clean-run.txt` — final clean `npm run lint` run.

## Disk cleanup

Per the packet's instruction, `backend/target`, `frontend/node_modules`, and this run's scratch
Maven repo `.m2repo-verify/` are deleted from the checkout at the end of this verification (after
the evidence above was captured).
