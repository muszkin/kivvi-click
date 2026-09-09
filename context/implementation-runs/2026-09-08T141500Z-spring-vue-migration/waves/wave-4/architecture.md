# Wave-4 verifier verdict — dimension: architecture (round 1)

**Dimension status: FAIL**, bound to wave SHA `fe37fad3b884864ca1d41a2ed7cf258126329e18` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture`, detached, untouched — `git status --porcelain` empty at every checkpoint below and at end of run).

Journeys: popups-widget-editor, import-wizard, dashboard.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | `WidgetController`, `WidgetViewService`, `WidgetFixtures`, views `PopupsView.vue`/`PopupEditorView.vue`, components `PopupStage.vue`, `PopupWidget.vue`, `PwTypeList.vue`, `EditorShell.vue` (shared with campaigns), `ShopMock.vue` sit cleanly inside the `web`/`application`/`fixtures`/organisms layering. 0 ArchUnit violations, 0 ESLint errors, no cross-journey import, no restricted-`Intl` use, no stray `/accounts/` literal, no POST `…/blocks` route (DEV-7 confirmed: `grep -n "PostMapping" WidgetController.java` → no output). |
| import-wizard | **regression** | `ImportController`, `ImportUploadController`, `ImportUploadService`, `ImportViewService`, `ImportUploadStorage` (new `infrastructure.importing` package), `ImportView.vue` + `components/import/*` are correctly layered and free of `Intl`/`/accounts/` violations — **except** `frontend/src/components/import/RecentImports.vue`, which renders the guarded `.event-row` class through a `:class="recentImportRowClass"` binding to a JS string constant specifically to dodge `vue/no-restricted-class` (see finding below). This is a real violation of the "Row markup in one place" rule with no accepted deviation and no eslint.config.js exception covering it. |
| dashboard | parity | `DashboardController`, `DashboardViewService`, `DashboardFixtures`, `DashboardView.vue`, `Cardiogram.vue`, `ListCard.vue` sit cleanly inside the layering. The wave-4 `vue/no-restricted-class` override for `ListCard.vue` is file-scoped (`files: ["src/components/organisms/ListCard.vue"]`, not a directory glob) and matches old-stack precedent: `templates/pages/dashboard/recent-customers.html.twig:10` and `templates/pages/dashboard/top-automations.html.twig:10` both use `class="event-row"` for the same reason ListCard.vue's own comment states (grid-styling reuse, no event semantics). Confirmed the rule still fires for every other file (see negative probe below). DEV-1/DEV-2/DEV-3/DEV-11-closure are visual/contract-dimension deviations, not architecture-relevant. |

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat, automations, settings, campaigns-email-editor) are regression guards only for this dimension: full-repo `ArchitectureTest` (5 tests) and full-repo `npm run lint` both pass with 0 errors at HEAD (`archunit-final.log`, `eslint-baseline.log` — 12 pre-existing Prettier-class warnings only, unrelated to any rule row).

## Rules-translated.md coverage — one row at a time

| Row | Target tooling (as declared) | Enforced at this wave-4 SHA | Negative fired? |
| --- | --- | --- | --- |
| Static typing gate | `-Xlint:all`/`-Werror` (`backend/pom.xml:169-170`); `vue-tsc --noEmit` strict | present, unchanged since 11d3cc4 (`git diff` empty on `pom.xml`) | not re-proved this round — outside architecture's own command surface (compiler, not ArchUnit/ESLint); `./mvnw test` compiling cleanly implicitly exercises `-Werror` |
| Formatting `@Symfony` → Spotless/Prettier | `spotless-maven-plugin`; Prettier | present, unchanged since 11d3cc4 | not re-proved (same reasoning) |
| Test policy fail-on-warning | JUnit/Vitest reporter config | present, unchanged since 11d3cc4 | not re-proved (unit/integration dimension's surface) |
| **Layering convention** | `ArchitectureTest.domainDoesNotDependOnWeb`, `.infrastructureIsOnlyUsedFromApplication`, `.topLevelPackagesFormNoCycle` | **yes** — new packages `infrastructure.importing` (`ImportUploadStorage`), flat `application` classes (`DashboardViewService`, `ImportUploadService`, `ImportViewService`, `WidgetViewService`), new `web`/`web.dto` classes: controllers depend only on `application`+`domain`+`dto`; `ImportUploadService` (application) depends on `ImportUploadStorage` (infrastructure) — correct direction | **yes**, 2 probes: (1) `domain`→`web` field-type probe failed `domainDoesNotDependOnWeb` + `topLevelPackagesFormNoCycle` as expected; (2) `web`→`infrastructure.importing` **method-call** probe failed `infrastructureIsOnlyUsedFromApplication` as expected. **Caveat found**: an unused field-type-only reference from `web` to `infrastructure.importing.ImportUploadStorage` (no method call) did *not* trip `infrastructureIsOnlyUsedFromApplication` — `onlyBeAccessed()` only flags real call/field-access edges, not a bare declared-but-unused field type. Not currently exploited anywhere in the wave-4 diff (checked: no unused infra-typed field exists outside `application`); recorded as a minor tooling-rigor gap, not a live violation. |
| **Mercure topic built only server-side** | `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; ESLint `no-restricted-syntax` on `/accounts/` | yes, unchanged — no wave-4 file references `MercurePublisher`/`HttpMercurePublisher` outside its allowed packages; `grep -rn "accounts" frontend/src --include=*.ts --include=*.vue` → no hits | not re-probed this round (unchanged carried-over rule, no wave-4 file touches Mercure topic construction; wave-3 already proved both probes) |
| **Row markup in one place** | ESLint `vue/no-restricted-class` (`event-row`), base rule + wave-4 `ListCard.vue` file-scoped override | **violated** — see FAIL finding above/below | **yes** — literal-`class` probe in `DashboardView.vue` (arbitrary third file) correctly failed with `vue/no-restricted-class` (2 occurrences, exact expected message); `ListCard.vue`/`EventRow.vue` lint clean (positive control); **and** the dynamic-binding gap in `RecentImports.vue` was independently confirmed to pass (exit 0) despite rendering the same class — this is the FAIL. |
| Formatting decided server-side (`format.ts`) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl` | yes, unchanged — no wave-4 file (`PopupStage.vue`, `PopupWidget.vue`, `Cardiogram.vue`, `ListCard.vue`, `import/*.vue`, `DashboardView.vue`, `ImportView.vue`, `PopupsView.vue`, `PopupEditorView.vue`) references `Intl`/`toLocaleString`/`toFixed` | not re-probed this round (unchanged carried-over rule; wave-1/2/3 already proved it; grep-confirmed clean for every new wave-4 file) |
| Storybook dev-only | Vite `build` config | out of scope for parity, untouched | n/a |

No row lacks tooling. One row (Row markup in one place) has a rule violation with no accepted deviation — this is the FAIL condition per the verifier contract ("FAIL if a rule is violated").

## Finding: `.event-row` rendered outside the sanctioned set (import-wizard journey)

`frontend/src/components/import/RecentImports.vue` (new this wave, commit `d53f43a`, "feat: implement the four-step import wizard and file upload (#import-wizard)") declares:

```ts
const recentImportRowClass = "event-row";
```

and binds it as `:class="recentImportRowClass"` on each recent-import row. `npx eslint src/components/import/RecentImports.vue` exits **0** — `vue/no-restricted-class` pattern-matches literal/static class values and does not see a value carried through a bound identifier. The file's own comment states the intent plainly: "bound from this constant rather than written as a literal `class=\"event-row\"` in the template: eslint.config.js's `vue/no-restricted-class` forbids that literal … See worker-report.md."

This is a real instance of the guarded class outside the two sanctioned renderers (`EventRow.vue`, and wave-4's `ListCard.vue`), with:
- no `eslint.config.js` exception for this file (diffed `11d3cc4..HEAD` on `frontend/eslint.config.js`: only the `ListCard.vue` block was added),
- no entry in `tools/migration-verify/deviations.json` (DEV-1..DEV-12 reviewed; the only import-wizard entry, DEV-12, covers the unrelated step-10 404 document).

The underlying reuse is plausible and has old-stack precedent identical to ListCard.vue's: `templates/pages/import/recent-imports.html.twig:14` also uses `class="event-row"` on the parent repo. Substance is not the objection — process is: the dashboard journey extended the rule through the governed mechanism (a file-scoped `eslint.config.js` override, reviewable and auditable, justified against the old templates); the import-wizard journey instead routed around the ESLint check via a JS-level indirection, so the rule as written ("EventRow.vue [+ListCard.vue] is the only component rendering `.event-row`") is factually false at this SHA, and nothing in the checked-in tooling would catch a second, less-considered reuse of the same trick elsewhere. Full trail: `architecture/finding-recentimports-event-row-bypass.log`.

**Remediation implied (not performed by this read-only verifier):** either add a matching file-scoped `eslint.config.js` override for `RecentImports.vue` (mirroring the `ListCard.vue` block, with the same old-template justification) and record it, or replace the ad hoc row markup with the already-existing generic `ListCard.vue` organism.

## Cross-journey imports

Confirmed for all four wave-4 views (`ImportView.vue`, `PopupsView.vue`, `PopupEditorView.vue`, `DashboardView.vue`): every import is a Vue/vue-router/vue-i18n framework import, a shared `atoms`/`molecules`/`organisms` primitive, or a component the view's own journey owns. Checked the reverse direction for journey-owned files (`components/import/*`, `PopupStage.vue`, `PopupWidget.vue`, `PwTypeList.vue`, `ListCard.vue`, `Cardiogram.vue`, `ShopMock.vue`): every cross-file import inside a journey folder stays inside that folder or resolves to a pre-existing shared tier (`KpiGrid`). No file under `components/import/` imports from `components/settings|campaigns|automations`, and no popups-widget-editor or dashboard file imports from `components/import/`. `EditorShell.vue` is intentionally shared (already used by campaigns-email-editor since wave-3; consumed read-only by `PopupEditorView.vue` here, matching the plan's own dependency-audit note "`EditorShell` owned by J8 and consumed by J9 later"). Full grep output: `architecture/cross-journey-imports.log`.

## Upload directory config

`kivvi.import.upload-directory` defaults to `${KIVVI_IMPORT_UPLOAD_DIRECTORY:./var/import}` (`backend/src/main/resources/application.yml`), a path relative to the image's `WORKDIR /app` (`backend/Dockerfile`), resolving to `/app/var/import` purely inside the `api` container's own filesystem. `compose.next.yaml`'s `api` service declares no `volumes:` at all — the directory is never bind-mounted to the host or a named volume, and no override is set in compose, so the shipped default is what actually runs. `ImportUploadStorage.store()` strips client-supplied path components (`basename()`) and writes under a random 32-hex generated name, never the client's own name — traversal-safe regardless of the directory's location. Safe default confirmed. Evidence: `architecture/upload-directory-config.log`.

## Global constraints (protected paths, Flyway, dependency pins) since wave-3 SHA `11d3cc4`

- `git diff --stat 11d3cc4..HEAD -- src/ templates/ assets/ translations/ migrations/ tests/**/*.php tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` → empty, exit 0. No old-stack/protected path touched.
- Flyway: `git diff --stat 11d3cc4..HEAD -- backend/src/main/resources/db/migration/` → empty, exit 0. No schema drift.
- Dependency pins: `git diff 11d3cc4..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` → empty. No dependency added/bumped this wave.
- `tools/migration-verify/compare.mjs` changed only to add the DEV-5 `var/import files` filesystem counter (`importFileCount()`), matching the packet's note; `tools/migration-verify/deviations.json` changed only for DEV-5 (mapping key + note), DEV-11 (closed for `login`, `landing` unchanged), and DEV-12 (`import-wizard: [10]` added) — diffed line-by-line, nothing else in the file moved.
- Full changed-file list (`11d3cc4..HEAD`, 76 files) is entirely inside `backend/src/main/java/click/kivvi/{application,fixtures,infrastructure/importing,web}`, `backend/src/test/java`, `backend/src/main/resources/application.yml` (one new `import:` block), `frontend/src/{components/import,components/molecules,components/organisms,i18n,router,views}`, `frontend/eslint.config.js`, `frontend/README.md`, `frontend/test`, and `tools/migration-verify/{compare.mjs,deviations.json}` — matches the wave-4 scope (popups-widget-editor/import-wizard/dashboard) with no unrelated backend package touch. See `architecture/global-constraints-drift.log`.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q -Dtest=ArchitectureTest test` (baseline) | `backend/` | 0 |
| 2 | same, with throwaway `domain`→`web` field probe present | `backend/` | 1 (expected — 2 rules fail as designed) |
| 3 | same, with throwaway `web`→`infrastructure.importing` field-only probe (no call) | `backend/` | 0 (unexpected pass — tooling-rigor gap noted above, not exploited) |
| 4 | same, with the probe upgraded to a real method call | `backend/` | 1 (expected — `infrastructureIsOnlyUsedFromApplication` fails) |
| 5 | same, probes removed, final confirmation | `backend/` | 0 |
| 6 | `npm ci --no-audit --no-fund` | `frontend/` | 0 |
| 7 | `npm run lint` (baseline, full repo) | `frontend/` | 0 (12 pre-existing warnings, 0 errors) |
| 8 | `npx eslint src/views/DashboardView.vue` with throwaway `class="event-row"` div inserted | `frontend/` | 1 (expected — `vue/no-restricted-class` fires twice) |
| 9 | `npx eslint src/components/organisms/ListCard.vue src/components/molecules/EventRow.vue` | `frontend/` | 0 (expected — sanctioned renderers) |
| 10 | `npx eslint src/components/import/RecentImports.vue` | `frontend/` | 0 (**FAIL finding** — renders `.event-row` via dynamic binding, uncaught) |
| 11 | `git diff --stat 11d3cc4..HEAD -- <protected paths>` | repo root | 0, empty |
| 12 | `git diff 11d3cc4..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json` | repo root | 0, empty |
| 13 | `git diff --stat 11d3cc4..HEAD -- backend/src/main/resources/db/migration/` | repo root | 0, empty |
| 14 | `git status --porcelain` (after every probe revert and at end of run) | repo root | 0, empty every time |

All throwaway probe files were created and deleted within this run; the checkout's tracked tree is byte-identical to `fe37fad3b884864ca1d41a2ed7cf258126329e18` at the end (`git status --porcelain` empty). `backend/target` and `frontend/node_modules` deleted from this checkout after the run per the packet's disk instruction.

## Evidence paths

- `architecture/archunit-final.log` — clean baseline run (5/5 `ArchitectureTest` tests, exit 0)
- `architecture/archunit-negative-probe-domain-to-web.log` — probe 2 above (full Maven output)
- `architecture/archunit-negative-probe-web-to-infra-field-only-PASSED-gap.log` — probe 3 above (the tooling-rigor gap)
- `architecture/archunit-negative-probe-web-to-infra-method-call.log` — probe 4 above (expected failure)
- `architecture/eslint-baseline.log` — full-repo `npm run lint`, exit 0
- `architecture/eslint-negative-probe-DashboardView-event-row.log` — probe 8 above
- `architecture/finding-recentimports-event-row-bypass.log` — full trail for the FAIL finding (source, eslint run, config diff, deviations.json check, git log, old-stack twig precedent)
- `architecture/cross-journey-imports.log` — import audit for all four wave-4 views + journey-owned component folders
- `architecture/upload-directory-config.log` — application.yml, ImportUploadStorage, Dockerfile WORKDIR, compose.next.yaml volumes
- `architecture/global-constraints-drift.log` — protected-path/dependency-pin/Flyway diffs since `11d3cc4`, full changed-file list

## Verdict summary

**FAIL** — import-wizard journey has a genuine, unsanctioned violation of the "Row markup in one place" architecture rule (`RecentImports.vue` renders `.event-row` outside the two components the rule and its wave-4 extension allow, via a mechanism that evades the ESLint check meant to enforce it). popups-widget-editor and dashboard journeys are clean (parity). Recommend the orchestrator route this back to the import-wizard journey for either a matching file-scoped `eslint.config.js` exception (with the same old-template justification `ListCard.vue` used) or reuse of the existing `ListCard.vue` organism, then re-verify.
