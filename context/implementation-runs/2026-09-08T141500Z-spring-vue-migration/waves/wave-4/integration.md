# Wave-4 verifier — dimension: integration (round 2)

**Dimension status: PASS**, bound to wave SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration` (verified detached at the wave
SHA before and after; tree clean throughout — no tracked files edited). Round-1 evidence under
`waves/wave-4/round-1/` was not read, per packet instructions.

## Commands run

| # | Command | cwd | Exit code | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify -Pintegration` (attempt 1) | `backend` | process force-killed a fork JVM after `System.exit(0)` (Surefire watchdog); `failsafe-summary.xml` nonetheless recorded `completed=90 errors=0 failures=0 skipped=1`; re-run for a clean confirmation (see #2) | `integration/backend-mvn-verify.log` |
| 2 | `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify -Pintegration` (attempt 2, clean) | `backend` | **0** | `integration/backend-mvn-verify-run2.log` |
| 3 | `npm ci` | `frontend` | 0 | `integration/frontend-npm-ci.log` |
| 4 | `npm run test:integration` (→ `vitest run test/integration`) | `frontend` | **0** | `integration/frontend-test-integration.log` |

No IT class actually failed on any run — see "About the attempt-1 re-run" below for why a full
re-run was still done for a clean confirmation.

### Backend result (attempt 2, authoritative)

`failsafe-summary.xml`: `completed=90 errors=0 failures=0 skipped=1 flakes=0`. `mvn` exited 0, which
also confirms `failsafe:verify` (bound to the `verify` phase) and `spotless:check` (also bound to
`verify`) both passed — the `-q` flag suppresses the `BUILD SUCCESS` banner itself but not the
process exit code, which was captured explicitly (`echo EXITCODE:$?`).

The 1 skip is `SessionRequestSerializationRedProofIT` — a `@Disabled("RED proof only …")` class from
an earlier wave (login journey, repair-1), deliberately disabled by design to document a bug the
repair fixes; not a regression, not in wave-4's scope, unaffected by this run.

### About the attempt-1 re-run

Attempt 1's forked test JVM logged `[ERROR] Surefire is going to kill self fork JVM. The exit has
elapsed 30 seconds after System.exit(0).` after tests finished, and `failsafe-summary.xml`'s
`result` attribute came back `"null"` (ambiguous) instead of a normal status. Individual test counts
in that same summary were already `0 errors / 0 failures`, and `target/surefire-reports` /
`target/failsafe-reports/*.txt` contained no `FAILURE`/`ERROR` text — but to remove any doubt about
whether the forced kill caused `failsafe:verify` to fail the build regardless, the whole `verify`
command was re-run cleanly end-to-end (attempt 2), this time exiting 0 with no forced-kill warning
and an identical `completed=90 errors=0 failures=0 skipped=1` count — deterministic across both
runs. Both logs are kept as evidence.

### Frontend result

`Test Files 20 passed (20)` / `Tests 127 passed (127)`, exit 0. (Console noise `Not implemented:
Window's scrollTo()` is a routine jsdom warning, not a failure.)

## Behaviour → test mapping (all named, all green)

**B29 (popups-widget-editor)** — `tests/e2e/specs/lists.spec.ts widgets` + `editors.spec.ts popup
editor` per `behaviours.json`; integration-dimension equivalents:
- Backend `WidgetApiIT` (real HTTP, real Postgres via Testcontainers): `popupsPageRenders200`,
  `popupEditorDocumentRenders200`, `newPopupDocumentRenders200` (B01 document routes),
  `popupsPayloadMatchesTheOracleThroughTheRealHttpLayer`,
  `previewQuerySelectsTheBannerWidgetThroughTheRealHttpLayer` (`?preview=`),
  `knownPopupPayloadMatchesTheOracleThroughTheRealHttpLayer` (default device/viewport),
  `typeAndDeviceQueryOverrideThroughTheRealHttpLayer` (`?type=&device=`),
  `newPopupPayloadMatchesTheOracleThroughTheRealHttpLayer`,
  `unmatchedPopupIdIsNotFoundThroughTheRealHttpLayer` (B29/DEV-7 — id shape outside `new`/`p\d+`
  404s, matching DEV-7's "no POST …/blocks" contract note that popup ids are otherwise unchanged).
- Frontend `PopupsView.spec.ts` (mounts the real view + real router + real pinia, stubs only
  `fetch`): index cards, `?preview=` forwarded to the API call and to card navigation, banner
  reshape. `PopupEditorView.spec.ts`: `?type=`/`?device=` forwarded to the API call, widget-type and
  device-switch links, preview reshape and viewport readout per device.

**B31 (import-wizard)** — `tests/e2e/specs/import.spec.ts`; integration-dimension equivalents:
- Backend `ImportApiIT` (real HTTP, real Postgres, private `@TempDir root` — never the shared
  `/tmp`, see below): `outOfRangeStepApiIsNotFound` + `outOfRangeStepDocumentIsNotFound` (unknown
  step 5 → 404 on both the API and the document, the latter tagged B31/DEV-12),
  `everyImportDocumentUrlRenders200` (B01, steps 1-4 + bare `/pl/import`),
  `missingFilePartIsNotFound` (missing file → 404),
  `uploadRedirectsAndTheFileNameSurvivesASecondRequest` (upload → 302 to `/pl/import/2`, then a
  **second** real HTTP request replaying the `SESSION` cookie proves the file name round-trips
  through the real JDBC-backed `spring_session` table, not an in-JVM session),
  `withoutAnUploadStepTwoFallsBackToTheFixtureFileName`,
  `emptyFileIsStoredAndRedirectsLikeTheOldStack` (empty file stored + 302),
  `pathTraversalOriginalNameNeverEscapesTheUploadDirectoryOverHttp` (path traversal end to end over
  real HTTP multipart parsing).
- Frontend `ImportView.spec.ts`: all 4 steps' own content (dropzone/recent-imports card, column
  mapping + confidence read-out + file-name pill, dedup/GDPR, KPI summary + bad-row count), stepper
  renders 4 `.step` nodes, uploaded file name (`klienci-oracle.csv`) reflected on step 2.

**B23 (dashboard)** — `tests/e2e/specs/dashboard.spec.ts`; integration-dimension equivalents:
- Backend `DashboardApiIT`: `dashboardPageRenders200` (B01), `dashboardPayloadMatchesTheOracleThrough
  TheRealHttpLayer` (4 KPIs, 3-entry legend, 10 events, `mercureTopic=/accounts/1/events`, 6 recent
  customers with `lastSeen` strings `teraz`/`N min temu` — DEV-1's clock-string mask target — 4 top
  automations).
- Frontend `DashboardView.spec.ts`: mounts the real view + router with a `FakeEventSource` stub
  (constructor/`onmessage`/`close` only — stubs the network, not the component) and a fake 2D canvas
  context (DEV-2's cardiogram-mask target), asserts the stream mounts subscribed to the topic
  attribute and KPI/canvas rendering — real component/store/router, network-only stubbing, per the
  packet's qualifying-test definition.

**B01** (all rows for this wave's journeys) — covered inline above by each `*ApiIT`'s document-route
tests (`popupsPageRenders200`, `popupEditorDocumentRenders200`, `newPopupDocumentRenders200`,
`everyImportDocumentUrlRenders200`, `dashboardPageRenders200`), each a real HTTP round trip through
the full Spring context, not a sliced `@WebMvcTest`.

Every behaviour id in scope (B29, B31, B23, all B01 rows of this wave) maps to at least one named
backend `*IT` test and one named frontend `test/integration` spec; none is untested, none is red.

## Deviations in scope — how this dimension observed them

- **DEV-4** (`*`, contract dimension) — not this dimension's concern directly, but every IT above
  exercises the same document/method/path/status shapes DEV-4 governs; nothing here contradicts it.
- **DEV-7** (popups: no POST …/blocks) — `WidgetApiIT.unmatchedPopupIdIsNotFoundThroughTheRealHttpLayer`
  is explicitly tagged `B29/DEV-7` in its `@DisplayName`; confirms the id-shape 404 boundary the
  deviation note describes.
- **DEV-5/DEV-9** (import: session + `var/import files` counter) — contract-dimension db-count
  concerns; this dimension's contribution is `ImportApiIT`'s real `spring_session`-backed round trip
  (`uploadRedirectsAndTheFileNameSurvivesASecondRequest`), proving the session mechanism DEV-9's
  `sessionsDeltaComparedByCountOnly` rule assumes actually persists file-name state across requests.
- **DEV-12** (import-wizard step 10 = 404 document) — `ImportApiIT.outOfRangeStepDocumentIsNotFound`
  (`GET /pl/import/5` → 404) is the integration-dimension proof behind the oracle's recorded
  `documentStatus 404` for that step.
- **DEV-1/DEV-2/DEV-3** (dashboard: clock strings, cardiogram mask, JSON events) — visual/contract
  concerns; this dimension's `DashboardApiIT` payload assertions on `lastSeen` strings and
  `DashboardView.spec.ts`'s fake canvas context are the underlying real data/rendering paths those
  masks/rules apply over. DEV-3's own backend proof (`CollectApiIT`) belongs to the event-stream
  journey, outside this wave's scope, and was not re-verified here.
- **DEV-11** (closed for login steps 4/8 by this wave) — a visual-dimension deviation; not exercised
  by this dimension's commands. No action needed here.

## Round-2 repair spot-check (context, not this dimension's gate)

Confirmed by direct code read (not re-verified as a gate — that's the architecture verifier's job):
`ImportApiIT` uses `@TempDir static Path root` (private to the test class) with the upload directory
configured as `root/import` via `@DynamicPropertySource`; every filesystem assertion in the
path-traversal and empty-file tests is scoped to `root` and never touches the shared `/tmp`. This
matches the round-2 note and was exercised (not merely present) by both `verify -Pintegration` runs
above.

## Cleanup performed (per packet instruction)

`backend/target` (31M) and `frontend/node_modules` (161M) deleted from this checkout after the runs
completed; `git status` confirmed clean (both are gitignored) and `HEAD` unchanged at the wave SHA.

## Verdict

| Journey | Verdict |
| --- | --- |
| popups-widget-editor | parity |
| import-wizard | parity |
| dashboard | parity |

**Dimension status: PASS.**
