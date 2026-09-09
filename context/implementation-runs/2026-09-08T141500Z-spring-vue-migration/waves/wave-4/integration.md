# Wave-4 verifier — dimension: integration (round 1)

- Wave SHA: `fe37fad3b884864ca1d41a2ed7cf258126329e18` (confirmed via `git rev-parse HEAD` in the checkout below)
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration` (detached, not edited — confirmed clean at both start and end of this run)
- Journeys: popups-widget-editor, import-wizard, dashboard
- Behaviours in scope: B29 (popups-widget-editor), B31 (import-wizard), B23 (dashboard), all B01 rows of this wave
- Deviations in scope: DEV-4 (all — contract-dimension http-compare rule, not applicable to this dimension's own pass/fail), DEV-7 (popups: no POST …/blocks — confirmed by an explicit test, see below), DEV-5/DEV-9 (import: session + `var/import files` counter — contract-dimension, not applicable here), DEV-12 (import-wizard step 10 = 404 document — visual-dimension, not applicable here), DEV-1/DEV-2/DEV-3 (dashboard: clock strings, cardiogram canvas mask, JSON events — visual/contract-dimension, not applicable here). None of these cover the failure found below.

## Dimension status: **FAIL**

Bound to wave SHA `fe37fad3b884864ca1d41a2ed7cf258126329e18`. `cd backend && ./mvnw -q verify -Pintegration` exits 1: 1 of 90 backend `*IT.java` tests is red. `cd frontend && npm run test:integration` is fully green (127/127). The "0 failures" threshold in the plan's Verifier contract is not met, so this round cannot be certified PASS as-is — see the per-journey table and diagnosis note below for exactly how narrow the failure is and why it is very likely a host-environment artifact rather than a security regression.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | `WidgetApiIT` (10/10 green) + `PopupsView.spec.ts`/`PopupEditorView.spec.ts` (frontend integration, 24 tests, all green) map B29 and B01 by name; DEV-7 ("dragging a library block onto the canvas never issues a network request") proven by a named test in `PopupEditorView.spec.ts`; 0 failures |
| dashboard | parity | `DashboardApiIT` (3/3 green) + `DashboardView.spec.ts` (frontend integration, 10 tests, all green, incl. a `FakeEventSource` stub proving the real `EventSource`-driven store wiring, not a hand-fed prop) map B23 and B01 by name; 0 failures |
| import-wizard | **regression** (see diagnosis) | `ImportApiIT` 7/8 green; `pathTraversalOriginalNameNeverEscapesTheUploadDirectoryOverHttp` is red. `ImportView.spec.ts` (frontend integration, 5/5 green) maps B31/B01 by name. All five required scenarios (unknown step 404, missing file 404, empty file stored+302, path traversal end to end, session round-trip of the original file name) have a qualifying real-HTTP IT; four of the five pass, the fifth (path traversal) is red on this run. Reproduced twice identically; strong circumstantial evidence (below) points to a host-environment false positive in the test's own "nothing else changed under the shared /tmp" assertion, not a defect in the traversal-prevention logic itself — but this run cannot rule out the latter because the traversal-specific assertions in the same method never executed (AssertJ short-circuits on the first failure). See `integration/import-wizard-failure-diagnosis.txt`. |

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat, automations, settings, campaigns-email-editor) are regression guards only for this dimension: their own `*ApiIT` classes (`LandingApiIT`, `FeedsApiIT`, `EventsApiIT`, `CustomersApiIT`, `CollectApiIT`, `AutomationsApiIT`, `CampaignsApiIT`, `SettingsApiIT`, `HeartbeatSchedulerIT`, `EventDedupStoreIT`, `SessionRoundTripIT`, `SessionRequestSerializationIT`, `ShellApiIT`) all ran green in the same `./mvnw verify` invocation — no regression. `SessionRequestSerializationRedProofIT` is confirmed still `@Disabled` on purpose (1 skipped, 0 run) — unchanged from wave-3.

## Import-wizard failure — summary (full diagnosis in evidence)

`click.kivvi.ImportApiIT.pathTraversalOriginalNameNeverEscapesTheUploadDirectoryOverHttp` fails at:

```
assertThat(listEntries(uploadDirectory.getParent())).as("nothing written outside the upload directory").isEqualTo(siblingsBefore);
```

`uploadDirectory` is a JUnit `@TempDir` created directly under this host's `java.io.tmpdir` (`/tmp`), so the assertion is an exact `Set<Path>` equality of **all of /tmp's direct children** taken before vs. after one HTTP multipart upload — on a shared host currently holding ~3,160 entries in `/tmp` from clearly unrelated concurrent tooling (`magellan-ai-tooling-*`, `magellan-skill-validator-*`, `kotlin-daemon.*`, `blog_cache_*`, `phpunit_icon_*`, etc.). A direct 10-second watch of `/tmp`'s direct children during this session captured three add/remove events from an unrelated Kotlin compiler daemon inside single-second windows — well within this test's ~30–50 ms assertion window. AssertJ's default 1000-element print cap hides the actual differing entries in both the "expected" and "was" listings (the printed first-1000 elements are identical between them). The equivalent sanitisation logic is independently proven at unit level by `ImportUploadStorageTest` (9/9 green, same `verify` run, no real HTTP/servlet layer, no shared `/tmp` involved), and the other two ImportApiIT tests that also perform a real multipart upload and then inspect the upload directory both passed. Full reasoning, evidence and an explicit statement of what this diagnosis does *not* prove: `integration/import-wizard-failure-diagnosis.txt`.

## Commands run

| # | Command | cwd | Exit code | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration` | 0 (`fe37fad3b884864ca1d41a2ed7cf258126329e18`, matches packet) | — |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/frontend` | 0 | — |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify -Pintegration` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/backend` | **1** | `integration/backend-mvnw-verify.log`, `integration/backend-test-report-summary.txt` |
| 4 | `npm run test:integration` (`vitest run test/integration`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/frontend` | 0 | `integration/frontend-test-integration.log` |
| 5 | `./mvnw -q verify -Pintegration -Dit.test=ImportApiIT -DfailIfNoTests=false` (diagnostic reproduction) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-integration/backend` | 1 (same single failure, confirmed via correctly-captured `PIPESTATUS`) | `integration/import-wizard-failure-diagnosis.txt` |
| 6 | 10s / 1s-sampling watch of `/tmp`'s direct children (diagnostic) | host | — | `integration/import-wizard-failure-diagnosis.txt` |

Note on command #3's recorded exit code: the first full run was piped through `tee` for logging, so `$?` after the pipe reflected `tee`'s exit status, not `mvnw`'s. The exit code of 1 shown above is Maven's well-documented behaviour on a `MojoFailureException` (confirmed directly for the same failure via command #5, where `PIPESTATUS[0]` was captured correctly) and is corroborated by the explicit `[ERROR] Failed to execute goal ... failsafe-plugin:...:verify ... There are test failures` in `backend-mvnw-verify.log`.

## Result totals

- Backend unit (surefire, regression guard only for this dimension — unit dimension owns this gate): **289 tests, 0 failures, 0 errors, 0 skipped**, including `ImportUploadStorageTest` (9/9, the unit-level equivalent of the failing IT's traversal check).
- Backend integration (failsafe, `*IT.java`, Testcontainers Postgres 18): **90 tests run, 1 failure, 0 errors, 1 skipped** (the intentionally-`@Disabled` `SessionRequestSerializationRedProofIT`, unchanged from wave-3). Per-class breakdown in `integration/backend-test-report-summary.txt`: `DashboardApiIT` (3/3), `WidgetApiIT` (10/10), `ImportApiIT` (7/8, one red), plus all other wave and earlier-wave `*ApiIT`/session/scheduling/tracking classes, all green.
- Frontend integration (`vitest run test/integration`): **20 test files, 127 tests, all passed**, 0 failures — includes `PopupsView.spec.ts`, `PopupEditorView.spec.ts`, `DashboardView.spec.ts`, `ImportView.spec.ts` for this wave's three journeys, plus all earlier-wave integration specs (regression guards, all green).

## Behaviour-id mapping

Every in-scope behaviour id maps by name to a qualifying test (real-HTTP `*IT.java` and/or `frontend/test/integration/*.spec.ts` mounting the real view/router with only the network stubbed): B29, B31, B23, and all B01 rows of this wave. Full mapping with exact `@DisplayName`/`describe` strings: `integration/behaviour-mapping.txt`.

## Qualifying-test bar — confirmed by direct inspection

- Backend: only `backend/src/test/java/click/kivvi/*ApiIT.java` (and the session/scheduling/tracking `*IT.java` classes), run via `./mvnw -q verify -Pintegration` (failsafe, Testcontainers Postgres 18, `@SpringBootTest(webEnvironment = RANDOM_PORT)`), counted for this dimension — `@WebMvcTest`-sliced `*ControllerTest` classes (`WidgetControllerTest`, `DashboardControllerTest`, `ImportControllerTest`, `ImportUploadControllerTest`) exist but do **not** qualify and were excluded from the behaviour mapping and pass/fail count, consistent with `WidgetApiIT`'s own class Javadoc calling this out explicitly.
- Frontend: `PopupsView.spec.ts`, `PopupEditorView.spec.ts`, `DashboardView.spec.ts`, `ImportView.spec.ts` all use `createRouter({ history: createWebHistory(), routes })` + the real `routes` module + `mount(<RealView>, { global: { plugins: [router, i18n] } })` (ImportView additionally wires a real Pinia store via `setActivePinia`/`useShellStore`), stubbing only `fetch` (all four) plus `EventSource` and `HTMLCanvasElement.prototype.getContext` (DashboardView only, matching its cardiogram/live-stream surface). No component received data through hand-fed props in place of the real fetch/router/store path — confirmed by reading all four files in full.

## Evidence paths (this dimension's evidence dir only)

- `integration/backend-mvnw-verify.log` — full `./mvnw -q verify -Pintegration` console output (round-1 run)
- `integration/backend-test-report-summary.txt` — per-class and aggregate failsafe/surefire "Tests run" lines, incl. `ImportUploadStorageTest`
- `integration/frontend-test-integration.log` — full `npm run test:integration` output (verbose reporter)
- `integration/behaviour-mapping.txt` — B29/B31/B23/B01 → named test (backend `@DisplayName` + frontend `describe`/`it`) mapping for popups-widget-editor, import-wizard, dashboard
- `integration/import-wizard-failure-diagnosis.txt` — full reasoning and evidence for the `ImportApiIT` path-traversal test failure, incl. the `/tmp` churn observation and what this diagnosis does and does not prove
- `integration/backend-exit-code.txt`, `integration/frontend-exit-code.txt`

## Notes

- No compose stacks were started or stopped; no tracked files were edited; no worker reports or review files were read (only this dimension's own prior-wave evidence, `waves/wave-3/integration.md`, was read once for reporting-format consistency).
- Disk hygiene per packet: `backend/target` and `frontend/node_modules` are deleted from this checkout after this run (see below); this evidence dir is unaffected.
