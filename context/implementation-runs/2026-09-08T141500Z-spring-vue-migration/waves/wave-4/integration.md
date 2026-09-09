# Wave-4 integration verdict (round 3)

**Dimension status: PASS**, bound to wave SHA `60445ebac139bcf1179b397c997600073a356e95`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration`, detached at the wave SHA,
`git status --porcelain` clean except the untracked `.m2-repo/` scratch dir this run created (removed
at the end, see Cleanup). No tracked file was edited. Read-only verification; no worker claims were
read; round-1/round-2 evidence under `waves/wave-4/round-1/` and `round-2/` was not read, per the
packet.

Journeys in scope: popups-widget-editor, import-wizard, dashboard.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | `WidgetApiIT` (10/10 backend ITs green) + `PopupsView.spec.ts` (9/9) + `PopupEditorView.spec.ts` (16/16) frontend integration tests green. B29 and B01 rows mapped by name; DEV-7 (no `POST …/blocks` on block drag/drop) asserted directly. |
| import-wizard | parity | `ImportApiIT` (8/8 backend ITs green, Testcontainers Postgres + real HTTP) + `ImportView.spec.ts` (frontend integration, part of the 131 all-green run) green. B31 and B01 rows mapped by name; DEV-12 (step 10 = 404 document) asserted; DEV-5/DEV-9 mechanisms (session cookie, `var/import files` counter) are contract-dimension, not re-asserted here beyond the session round-trip proven by `ImportApiIT`. |
| dashboard | parity | `DashboardApiIT` (3/3 backend ITs green) + `DashboardView.spec.ts` (12/12 frontend integration tests green, fake `EventSource`, faked canvas 2D context). B23 and B01 rows mapped by name; DEV-1/DEV-2/DEV-3 are visual/contract-dimension concerns (clock strings, cardiogram canvas mask, JSON Mercure payload) not re-asserted here — DEV-3 already names `CollectApiIT` and `EventStream.spec.ts` (event-stream journey, not this wave) as its own proof, not a dashboard-journey obligation. |

No regression across any of the other 14 backend `*IT.java` classes exercised by the same `verify`
run (all green — see evidence); no journey required a re-run (nothing failed).

## Behaviour-id mapping (packet-mandated: "map by name")

| Behaviour | Backend IT (real-HTTP, Testcontainers) | Frontend integration test |
| --- | --- | --- |
| B29 | `WidgetApiIT`: `popupsPayloadMatchesTheOracleThroughTheRealHttpLayer`, `previewQuerySelectsTheBannerWidgetThroughTheRealHttpLayer`, `knownPopupPayloadMatchesTheOracleThroughTheRealHttpLayer`, `typeAndDeviceQueryOverrideThroughTheRealHttpLayer`, `newPopupPayloadMatchesTheOracleThroughTheRealHttpLayer` (fallback: blank-draft widget), `unmatchedPopupIdIsNotFoundThroughTheRealHttpLayer` (fallback: 404 on a bogus id) | `PopupsView.spec.ts` (`describe("B29 the popup index matches the oracle…")`, 9 tests) + `PopupEditorView.spec.ts` (`describe("B29 the popup editor matches the oracle…")`, 16 tests, incl. the DEV-7 drag/drop no-network-call test) |
| B31 | `ImportApiIT`: `outOfRangeStepApiIsNotFound` + `outOfRangeStepDocumentIsNotFound` (unknown step → 404), `missingFilePartIsNotFound` (missing file → 404), `uploadRedirectsAndTheFileNameSurvivesASecondRequest` (upload → 302, session round-trip via `spring_session`, not same-JVM memory), `withoutAnUploadStepTwoFallsBackToTheFixtureFileName`, `emptyFileIsStoredAndRedirectsLikeTheOldStack` (empty file → 302, stored), `pathTraversalOriginalNameNeverEscapesTheUploadDirectoryOverHttp` (path traversal, end to end over real HTTP multipart, scoped to the class's own private `@TempDir static Path root`, never the shared `/tmp`) | `ImportView.spec.ts` (`describe("B31/B01 the import wizard matches the oracle: 4 steps, one URL each")`, part of the 131-test all-green run; 5 of its tests independently re-run verbose, all green) |
| B23 | `DashboardApiIT`: `dashboardPayloadMatchesTheOracleThroughTheRealHttpLayer` (4 KPIs, 10 events, 6 recent customers with `lastSeen` strings, 4 top automations, `mercureTopic`) | `DashboardView.spec.ts` (`describe("B23 the dashboard matches the oracle: KPIs, cardiogram, live feed, recent customers, top automations")`, 12 tests) using a `FakeEventSource` class (stubbed `EventSource` global) and a stubbed `HTMLCanvasElement.prototype.getContext` — real router + real view mount, only the network (fetch/EventSource) and canvas 2D context are faked |
| B01 (popups-widget-editor / import-wizard / dashboard rows) | `WidgetApiIT.popupsPageRenders200`, `.popupEditorDocumentRenders200`, `.newPopupDocumentRenders200`; `ImportApiIT.everyImportDocumentUrlRenders200` (loops `/pl/import`, `/pl/import/1..4`); `DashboardApiIT.dashboardPageRenders200` — each a real-HTTP 200 assertion, not the sliced `SpaDocumentControllerTest` | `PopupEditorView.spec.ts` tags two of its tests `B01/B29`; `ImportView.spec.ts`'s describe block is tagged `B31/B01` |

## Round-3-specific checks (packet addendum)

- **Shell store: preference POST awaited before DOM/state update.** `frontend/src/stores/shell.ts`
  `setTheme`/`setSidebar` now `await fetch(...)` before mutating `this.theme`/`this.sidebar` and the
  `document.documentElement.dataset.*` attributes (repair-2, w3-shell-preferences, wave-4). Confirmed
  by reading the source and by the two dedicated tests in
  `frontend/test/integration/shellStore.spec.ts`:
  - `"B08 setTheme applies the DOM attribute and store state only after the POST resolves"` — asserts
    `shell.theme`/`document.documentElement.dataset.theme` are still the *old* value immediately after
    calling `setTheme`, and only flip once the stubbed fetch promise is resolved and awaited.
  - `"B10 setSidebar applies the store state and <html> attribute only after the POST resolves"` — same
    pattern for `setSidebar`/`data-sidebar`.
  Both green (see verbose log). The suite also keeps the pre-existing keepalive and
  reject-still-applies tests green (9/9 in the file).
- **RecentImports.vue / ListCard.vue `event-row` scoping** (architecture-dimension rule, checked here
  only for cross-dimension awareness per the round-3 note): `RecentImports.vue`'s template now uses
  the literal `class="event-row"` (no script-side class constant), matching `ListCard.vue`'s existing
  pattern; `frontend/eslint.config.js` scopes the `vue/no-restricted-class` override and the
  `no-restricted-syntax` "event-row" string-literal hardening to exactly
  `EventRow.vue`/`ListCard.vue`/`RecentImports.vue`. This is the architecture verifier's rule to
  enforce; recorded here only because the round-3 note called it out and because `ImportView.spec.ts`
  / the `RecentImports.vue` mount exercises this component through the integration test path.
- **`ImportApiIT` traversal test uses a private `@TempDir`.** Confirmed: `@TempDir static Path root;`
  with `uploadDirectory = root.resolve("import")` wired via `@DynamicPropertySource`. Every filesystem
  assertion in the class (`storedFileNames`, `childNames`, `filesNamed`) is scoped under `root`, never
  the shared `java.io.tmpdir`. This is exactly the repair-3 fix the packet's round-3 note describes.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify -Pintegration -Dmaven.repo.local=/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/.m2-repo` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/backend` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/frontend` | 0 |
| 3 | `npm run test:integration` (→ `vitest run test/integration`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/frontend` | 0 |
| 4 | `npx vitest run test/integration/shellStore.spec.ts test/integration/PopupsView.spec.ts test/integration/PopupEditorView.spec.ts test/integration/DashboardView.spec.ts test/integration/ImportView.spec.ts --reporter=verbose` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/frontend` | 0 |

No IT or integration test failed; the packet's "re-run the failing class once" clause was not
triggered.

### Result totals

- Backend (`./mvnw verify -Pintegration`, command 1): 17 `*IT.java` classes, **90 tests, 0 failures,
  0 errors, 1 skipped** (`SessionRequestSerializationRedProofIT`, `@Disabled` by design — a wave-3
  RED-proof documentation artifact, out of this wave's behaviour scope). Surefire (unit phase, run as
  part of the same `verify`): 289 tests, 0 failures, 0 errors — that scope belongs to the `unit`
  dimension, recorded here only because it is part of the same exit-0 build.
- Frontend (`npm run test:integration`, command 3): 20 files, **131 tests, 0 failures**. Command 4
  independently re-ran the 5 files covering this wave's three journeys + the shell store with
  per-test verbose output: **45/45 green**, all behaviour tags (`B08`, `B10`, `B29`, `B31`, `B01`,
  `B23`, `DEV-7`) visible by name in the test titles.

## Evidence paths

- `waves/wave-4/integration/backend-mvnw-verify.log` — full stdout/stderr of command 1 (Spring Boot
  app + Flyway + Testcontainers + Hikari logs for every `*IT.java` class; ends with exit code 0 after
  Surefire's routine post-`System.exit(0)` fork-shutdown diagnostic, not a failure).
- `waves/wave-4/integration/backend-failsafe-surefire-summary.txt` — the per-class
  `target/failsafe-reports/*.txt` and aggregated `target/surefire-reports/*.txt` contents, captured
  before `backend/target` was deleted for disk space (see Cleanup).
- `waves/wave-4/integration/frontend-test-integration.log` — full output of command 3 (20 files, 131
  tests, all green).
- `waves/wave-4/integration/frontend-test-integration-verbose-wave4-journeys.log` — full output of
  command 4 (45 named tests, all green, behaviour ids visible per test title).

## Cleanup (packet instruction: disk is tight)

- `backend/target` deleted immediately after collecting the summary above (before running the
  frontend suite).
- `frontend/node_modules` and the run-local `.m2-repo` (`-Dmaven.repo.local` target, not the shared
  `~/.m2`) deleted at the end of this run, after all evidence above was written.
