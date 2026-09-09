# Wave-4 architecture verifier — round 3

**Dimension status: PASS**, bound to wave SHA `60445ebac139bcf1179b397c997600073a356e95` (checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture`, detached HEAD confirmed at this SHA).

Journeys: `popups-widget-editor`, `import-wizard`, `dashboard`. The architecture dimension's rules (ArchUnit boundary tests, ESLint boundary rules) are repo-wide, not per-journey, so all three journeys share one verdict.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | `WidgetController`/`WidgetViewService` follow the existing web→application→domain/fixtures layering; `PopupStage.vue`/`PopupWidget.vue`/`PwTypeList.vue`/`ShopMock.vue` live in shared `organisms/` but are consumed only by `PopupsView.vue`/`PopupEditorView.vue` (both this journey) — no cross-journey import. No `event-row`/`/accounts/`/`Intl` construct present. |
| import-wizard | parity | `ImportUploadStorage`/`ImportUploadController`/`ImportViewService` respect layering; upload directory default resolves inside the container (see below); path-traversal control re-proved by `ImportApiIT` with a private `@TempDir` root (round-3 repair confirmed, not re-tested by me — the file is read-only evidence here). `RecentImports.vue`'s `event-row` use is the third, file-scoped ESLint override, proven not to leak to other files (negative probe). `components/import/*` imports only shared atoms/molecules/organisms plus its own dir — no import from `components/settings/*`. |
| dashboard | parity | `DashboardController`/`DashboardViewService` respect layering; `Cardiogram.vue`/`ListCard.vue` consumed only by `DashboardView.vue`. `ListCard.vue`'s `event-row` use is the second file-scoped override, proven not to leak. `stores/shell.ts` (shared, but the round-3 addendum's own change) awaits the preference POST before applying `document.documentElement` state and keeps `<html data-sidebar>` in sync — read directly, no layering or boundary-rule violation (plain `fetch` calls, no cross-journey import). |

No DEV-1..DEV-12 deviation was needed for the architecture dimension itself (those are contract/visual-dimension deviations); this dimension found 0 rule violations and 0 rows in `rules-translated.md` lacking tooling.

## Rule coverage — every ArchUnit test / ESLint rule proven to fire on a throwaway negative

All four ArchUnit tests in `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` and all four independently-triggerable ESLint boundary-rule families in `frontend/eslint.config.js` were re-proved this round with a live negative: a throwaway violation was introduced, the exact command was re-run and observed failing with the expected rule/message, the probe file was deleted, and `git status --porcelain` confirmed clean before moving to the next probe. Full transcripts in `architecture/archunit-negatives.log` and `architecture/eslint-negatives.log`.

| Rule (rules-translated.md row) | Test / config | Negative probe | Result |
| --- | --- | --- | --- |
| Layering: domain must not depend on web | `ArchitectureTest#domainDoesNotDependOnWeb` | field of type `web.dto.DashboardResponse` added to a class in `domain` | FAILS as expected (also trips the cycle rule below, since domain→web→fixtures→domain closes a cycle) |
| Layering: infrastructure only accessed from application/infrastructure | `ArchitectureTest#infrastructureIsOnlyUsedFromApplication` | a `web` class calls a method on `infrastructure.importing.ImportUploadStorage` | FAILS as expected |
| Layering: no cycle between top-level packages | `ArchitectureTest#topLevelPackagesFormNoCycle` | (see domain→web probe above) | FAILS as expected |
| Mercure topic built only server-side (backend half) | `ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` | a `web` class calls `MercurePublisher.publish(...)` | FAILS as expected |
| Mercure topic built only server-side (literal-location half) | `ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic` | a second `.java` file adds a `"/accounts/999/events"` string literal | FAILS as expected (assertion lists 2 files instead of exactly `EventStreamTopic.java`) |
| Mercure topic built only server-side (frontend half) | `no-restricted-syntax` `/accounts/` selectors, `frontend/eslint.config.js` | a `.ts` file under `src/` exports a `"/accounts/999/events"` string | `eslint` exits 1 with the expected message |
| Row markup in one place — `event-row` class | `vue/no-restricted-class` on `event-row`, scoped everywhere except `EventRow.vue`/`ListCard.vue`/`RecentImports.vue` | a new `.vue` file under `components/atoms/` renders `class="event-row"` in its template | `eslint` exits 1 (`'event-row' class is not allowed`) |
| Row markup in one place — hardened script-literal bypass (repair-3) | `no-restricted-syntax` `Literal`/`TemplateElement` selectors matching `/event-row/`, scoped to `src/**/*.{ts,vue}` minus the three allowed files | a new `.vue` file under `components/atoms/` binds `:class="probeRowClass"` where `probeRowClass = "event-row"` — the exact bypass `RecentImports.vue` originally attempted | `eslint` exits 1 with the repair-3 message naming the three allowed files |
| Formatting decided server-side — `Intl` ban | `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl`, exempted only for `src/format.ts` (which does not yet exist in this checkout — no client-side formatting has been introduced by any journey through wave-4) | a new `.ts` file at `src/` root calls `new Intl.NumberFormat(...)` | `eslint` exits 1 with 2 errors (`no-restricted-globals` + `no-restricted-syntax`) |

Baseline (`ArchitectureTest`, all 5 tests) and full `npm run lint` were both green before and after every probe sequence (0 ArchUnit failures; 0 ESLint errors, only 12 pre-existing `vue/multiline-html-element-content-newline` formatting warnings unrelated to any boundary rule, unchanged from baseline).

## The three file-scoped `event-row` overrides (justified by the old Twig partials)

`frontend/eslint.config.js` exempts exactly three files from the `vue/no-restricted-class`/`no-restricted-syntax` `event-row` restriction, each with a comment tracing back to the old stack's Twig partial that reused `.event-row` for grid styling only (no live-stream semantics):

1. `src/components/molecules/EventRow.vue` — the canonical row renderer (base `ignores` entry on the `vue/no-restricted-class` block).
2. `src/components/organisms/ListCard.vue` — dashboard recent-customers/top-automations lists; comment cites `pages/dashboard/{recent-customers,top-automations}.html.twig`.
3. `src/components/import/RecentImports.vue` — import-wizard's recent imports list; comment cites `pages/import/recent-imports.html.twig:14`.

Each override is a `files: [...]` block scoped to exactly one path (verified by reading `frontend/eslint.config.js` directly — no glob broader than the single filename). The negative probes above (a fourth, unrelated file under `components/atoms/`) confirm the exemption does **not** leak outside these three: the same `class="event-row"` / `:class="\"event-row\""` construct that is legal in `ListCard.vue`/`RecentImports.vue` fails lint everywhere else, including a file in the same `components/` tree.

## `stores/shell.ts` and new backend packages — layering check

- `frontend/src/stores/shell.ts` (round-3 addendum change): read in full. `setTheme`/`setSidebar` now `await` their `POST /preferences/{theme,sidebar}` before touching `document.documentElement.dataset`, and `setSidebar` also syncs `<html data-sidebar>` (previously only `.app`'s reactive binding). No import outside `pinia`/DOM globals; no journey-specific or cross-journey import. Consistent with every ESLint/ArchUnit rule in scope — no `Intl`, no `/accounts/` literal, no `event-row`.
- New backend packages/classes since wave-3 (`11d3cc4`): `application.{DashboardViewService,ImportUploadService,ImportViewService,WidgetViewService}` (root `application` package, same convention as every existing `*ViewService`), `fixtures.{DashboardFixtures,ImportFixtures,WidgetFixtures}`, `infrastructure.importing.ImportUploadStorage` (new leaf package under `infrastructure`, only ever accessed from `application.ImportUploadService` per `git grep`), `web.{DashboardController,ImportController,ImportUploadController,WidgetController}` + `web.dto.*`. All follow the established `web → application → domain/fixtures` and `infrastructure ← application` shape; the full `ArchitectureTest` suite (which imports all of `click.kivvi` and checks every class, not just changed ones) passed 5/5 both before and after the negative-probe sequence, so these new packages are covered by, and compliant with, the same layering rules as the rest of the backend.

## Upload directory default — inside the container

- `backend/src/main/resources/application.yml:81`: `upload-directory: ${KIVVI_IMPORT_UPLOAD_DIRECTORY:./var/import}`.
- `ImportUploadStorage`'s `@Value("${kivvi.import.upload-directory:./var/import}")` constructor param — same relative default, resolved via `Path.of(...)` (never made absolute against anything but the JVM's own CWD).
- `backend/Dockerfile` runtime stage: `WORKDIR /app` set before `RUN chown -R kivvi:kivvi /app`, `USER kivvi`, `ENTRYPOINT ["java","-jar","/app/app.jar"]` — so the JVM's CWD is `/app`, and the relative default resolves to `/app/var/import`, created lazily by `Files.createDirectories(...)` under the already-`kivvi`-owned `/app` tree.
- `compose.next.yaml`'s `api` service declares no `volumes:` entry at all (only the `mercure` and `database` services do) — the upload directory is never bind-mounted to the host; it lives purely in the container's own writable layer, exactly as the packet requires ("upload directory default inside the container").
- Path-traversal containment (`basename()` stripping + `<32-hex>.<ext>` generated names, `EXTENSION_PATTERN` allow-list) read directly in `ImportUploadStorage.java`; the round-3 repair's `ImportApiIT` traversal test (private `@TempDir` root, not the shared `/tmp`) was read but not re-executed by me — Testcontainers/Postgres is out of scope for a read-only architecture-only check, and re-running it would duplicate the `integration`/`unit` verifiers' own job.

Evidence: `architecture/upload-directory.log`.

## No cross-journey imports beyond shared components

Grepped every `import` statement in `components/import/*` and `components/settings/*` (the two journey-owned directories) and in the four wave-4 views (`DashboardView.vue`, `PopupsView.vue`, `PopupEditorView.vue`, `ImportView.vue`): every import resolves to `@/components/{atoms,molecules,organisms}/*`, `@/stores/*`, a same-directory sibling, or a framework package (`vue`, `vue-router`, `vue-i18n`, `pinia`). Zero imports of `components/settings/*` from `components/import/*` or vice versa (the one textual hit was a documentation comment in `SelectField.vue` naming its settings-journey counterpart, not an import). The journey-specific organisms placed in the shared `organisms/` bucket (`Cardiogram.vue`, `ListCard.vue` for dashboard; `PopupStage.vue`, `PopupWidget.vue`, `PwTypeList.vue`, `ShopMock.vue` for popups-widget-editor) are each consumed from exactly one journey's view(s) — confirmed by a repo-wide grep for each component name.

Evidence: `architecture/cross-journey-imports.log`.

## No protected-path / dependency-pin / Flyway drift since wave-3 (`11d3cc4`)

- `git diff --stat 11d3cc4 HEAD -- <old-stack/protected paths from the plan's CON-1 packet: src/ templates/ assets/ translations/ migrations/ tests/e2e/specs/ tests/**/*.php composer.json composer.lock symfony.lock importmap.php yarn.lock package.json Dockerfile frankenphp/ compose.yaml compose.override.yaml compose.prod.yaml phpunit.dist.xml phpstan.neon.dist .php-cs-fixer.dist.php config/ public/ bin/ .github/workflows/docker-build.yml>` → empty, exit 0.
- `git diff --stat 11d3cc4 HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` → empty, exit 0 — no dependency-pin drift.
- `git diff --stat 11d3cc4 HEAD -- backend/src/main/resources/db/migration` → empty, exit 0; only `V1__baseline.sql` exists, unchanged — no Flyway drift.
- Full `git diff --name-only 11d3cc4 HEAD` (82 files) confined to `backend/src/{main,test}/java/click/kivvi/{application,fixtures,infrastructure/importing,web}/*`, `backend/src/main/resources/application.yml`, `frontend/{eslint.config.js,README.md,src/**,test/**}`, and `tools/migration-verify/{compare.mjs,deviations.json}` — every changed path is either new-stack application code or verifier tooling, nothing old-stack.

Evidence: `architecture/global-constraints.log`.

## Commands run (this round)

| # | Command | cwd | Exit |
| --- | --- | --- | --- |
| 1 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture/frontend` | 0 |
| 2 | `npm run lint` (baseline) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture/frontend` | 0 (0 errors, 12 pre-existing warnings) |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q -o test -Dtest=ArchitectureTest` (baseline) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture/backend` | 0 (5/5 pass) |
| 4 | same, with `ProbeDomainDependsOnWeb.java` present | same | 1 (2 failures: `domainDoesNotDependOnWeb`, `topLevelPackagesFormNoCycle`) |
| 5 | `./mvnw -q -o test -Dtest=ArchitectureTest#infrastructureIsOnlyUsedFromApplication`, with `ProbeWebAccessesInfrastructure.java` present | same | 1 |
| 6 | `./mvnw -q -o test -Dtest=ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`, with `ProbeWebAccessesMercurePublisher.java` present | same | 1 |
| 7 | `./mvnw -q -o test -Dtest=ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic`, with `ProbeAccountsLiteral.java` present | same | 1 |
| 8 | `./mvnw -q -o test -Dtest=ArchitectureTest` (final re-run, all probes removed) | same | 0 (5/5 pass) |
| 9 | `npx eslint src/probeAccountsTopic.ts` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture/frontend` | 1 |
| 10 | `npx eslint src/components/atoms/ProbeEventRow.vue` (template `class="event-row"`) | same | 1 |
| 11 | `npx eslint src/components/atoms/ProbeEventRow.vue` (`:class="probeRowClass"` script literal) | same | 1 |
| 12 | `npx eslint src/probeIntl.ts` | same | 1 |
| 13 | `npm run lint` (final re-run, all probes removed) | same | 0 (0 errors, 12 pre-existing warnings, identical to baseline) |
| 14 | `git diff --stat 11d3cc4 HEAD -- <protected paths>` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture` | 0 (empty) |
| 15 | `git diff --stat 11d3cc4 HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` | same | 0 (empty) |
| 16 | `git diff --stat 11d3cc4 HEAD -- backend/src/main/resources/db/migration` | same | 0 (empty) |
| 17 | `git status --porcelain` (after every probe cycle and at the end) | same | 0 (empty every time) |

All probe source files were throwaway, deleted immediately after their run; no tracked file was left modified. Final `git status --porcelain` for the whole checkout: empty.

## Evidence paths

- `architecture/archunit-negatives.log` — full transcript of the baseline, all 4 ArchUnit negative probes, and the final green re-run.
- `architecture/eslint-negatives.log` — full transcript of the baseline, all 4 ESLint negative probes, and the final green re-run.
- `architecture/global-constraints.log` — protected-path diff, dependency-pin diff, Flyway migration diff/listing, full changed-file list since `11d3cc4`.
- `architecture/cross-journey-imports.log` — import grep for `components/import/*`, `components/settings/*`, the four wave-4 views, and per-component usage-site grep for the six journey-specific `organisms/*` components.
- `architecture/upload-directory.log` — `application.yml` property, `ImportUploadStorage` constructor default, Dockerfile `WORKDIR`/`USER`/`ENTRYPOINT`, `compose.next.yaml` `api` service volume block (absent).

## Cleanup

Per the packet's disk instruction, `backend/target` and `frontend/node_modules` in this checkout (`/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture`) are deleted at the end of this run (see below) — this does not affect the evidence above, which was captured before cleanup.
