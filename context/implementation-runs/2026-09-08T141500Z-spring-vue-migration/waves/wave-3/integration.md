# Wave-3 verifier — dimension: integration (round 4)

- Wave SHA: `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e` (confirmed via `git rev-parse HEAD` in the checkout below)
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration` (detached, not edited)
- Journeys: automations, settings, campaigns-email-editor
- Behaviours in scope: B26 (automations), B06, B32 (settings), B27, B28 (campaigns-email-editor), all B01 rows of this wave
- Deviations in scope: DEV-4 (all — contract-dimension http-compare rule, not applicable to this dimension's own pass/fail), DEV-7 (campaigns: SPA makes no POST …/blocks — confirmed by an explicit test, see below), DEV-12 (settings step 10 = 404 document — visual-dimension deviation, not applicable to this dimension)

## Dimension status: **PASS**

Bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| automations | parity | `AutomationsApiIT` (7/7 green) + `AutomationsView.spec.ts`/`AutomationEditorView.spec.ts` (frontend integration, green) map B26 and B01 by name; 0 failures |
| settings | parity | `SettingsApiIT` (12/12 green) + `SettingsView.spec.ts` (frontend integration, green) map B06, B32 and B01 (8 tabs) by name; 0 failures |
| campaigns-email-editor | parity | `CampaignsApiIT` (8/8 green) + `CampaignsView.spec.ts`/`EmailEditorView.spec.ts` (frontend integration, green) map B27, B28 and B01 by name; DEV-7 ("SPA makes no POST …/blocks") is proven by `EmailEditorView.spec.ts`'s "dragging a library block onto the canvas never issues a network request (DEV-7)" test |

Earlier-wave journeys (login, landing, feeds, event-stream, customers, scheduler-heartbeat) are regression guards only for this dimension: their own `*ApiIT` classes (`LandingApiIT`, `FeedsApiIT`, `EventsApiIT`, `CustomersApiIT`, `CollectApiIT`, `HeartbeatSchedulerIT`, `EventDedupStoreIT`, `SessionRoundTripIT`, `ShellApiIT`) all ran green in the same `./mvnw verify` invocation (see aggregate below) — no regression.

## Shell-change regression guards (wave-3, required for this dimension)

| Change | Test | Result |
| --- | --- | --- |
| vue-router `scrollBehavior` + reload scroll restoration | `frontend/test/integration/ScrollRestoration.spec.ts` (mounts the real `@/router`, drives `router.options.scrollBehavior` through a real resolved route) | green |
| generic locale toggle via `route.meta.defaultParams` (`localeHref.ts`, `Topbar.vue`) | `frontend/test/integration/LocaleToggle.spec.ts` (mounts real `AppLayout` + real router + `Topbar`'s `localeHref` computed off the live route; network stubbed) | green |
| keepalive preference POSTs (theme, sidebar) | `frontend/test/integration/shellStore.spec.ts` (real Pinia store `useShellStore`, network stubbed) — asserts `keepalive: true` on both `/preferences/theme` and `/preferences/sidebar` | green |
| per-session request-serialization filter (PHP session-lock parity) | `backend/.../SessionRequestSerializationIT.java` (real HTTP, Testcontainers Postgres 18, `@SpringBootTest(webEnvironment=RANDOM_PORT)`) | green — proves both (a) the filter is ordered outside `SessionRepositoryFilter`, and (b) a concurrent reload `GET /api/v1/pl/shell` blocks until an in-flight `POST /preferences/theme` on the same session commits, then observes the committed (new) theme, never the stale pre-POST one |
| RED-proof of the above fix | `backend/.../SessionRequestSerializationRedProofIT.java` | confirmed **`@Disabled`** at class level (`"RED proof only — see class Javadoc; enable manually to reproduce the pre-fix race"`), same scenario with `kivvi.session-lock.enabled=false` — intentionally excluded from the green run; failsafe report shows it as 1 skipped test, 0 run |

Full behaviour-id → test mapping excerpt: `integration/behaviour-mapping.txt`. Shell-change excerpt: `integration/shell-changes-coverage.txt`.

## Commands run

| # | Command | cwd | Exit code | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration` | 0 (`11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`, matches packet) | — |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 | — |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify -Pintegration` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/backend` | 0 | `integration/backend-mvnw-verify.log`, `integration/backend-exit-code.txt`, `integration/backend-test-report-summary.txt` |
| 4 | `npm run test:integration` (`vitest run test/integration`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-integration/frontend` | 0 | `integration/frontend-test-integration.log`, `integration/frontend-exit-code.txt` |

## Result totals

- Backend unit (surefire, regression guard only for this dimension — unit dimension owns this gate): 236 tests, 0 failures, 0 errors, 0 skipped.
- Backend integration (failsafe, `*IT.java`, Testcontainers Postgres 18): **69 tests run, 0 failures, 0 errors, 1 skipped** (the intentionally-`@Disabled` `SessionRequestSerializationRedProofIT`). Per-class breakdown in `integration/backend-test-report-summary.txt`; includes `AutomationsApiIT` (7), `CampaignsApiIT` (8), `SettingsApiIT` (12), plus all other wave and earlier-wave `*ApiIT`/session/scheduling/tracking classes, all green.
- Frontend integration (`vitest run test/integration`): **16 test files, 91 tests, all passed**, 0 failures.

## Evidence paths (this dimension's evidence dir only)

- `integration/backend-mvnw-verify.log` — full `./mvnw -q verify -Pintegration` console output (round-4 clean rerun)
- `integration/backend-exit-code.txt`
- `integration/backend-test-report-summary.txt` — per-class and aggregate failsafe/surefire "Tests run" lines
- `integration/frontend-test-integration.log` — full `npm run test:integration` output
- `integration/frontend-exit-code.txt`
- `integration/behaviour-mapping.txt` — B26/B06/B32/B27/B28/B01 → named test (backend `@DisplayName` + frontend `describe`/`it`) mapping for automations, settings, campaigns-email-editor
- `integration/shell-changes-coverage.txt` — scroll restoration, locale toggle, keepalive POSTs, session-serialization filter (incl. confirmed `@Disabled` RED-proof) → named test mapping

## Notes

- Qualifying-test bar applied per packet: backend real-HTTP `*IT.java` under Testcontainers via `./mvnw -q verify` (not unit `*Test.java`, which surefire runs separately and is out of this dimension's gate); frontend `frontend/test/integration/*.spec.ts` mounting real views/components/router/Pinia store with only `fetch` (network) stubbed (confirmed by direct inspection of `AutomationsView.spec.ts`, `LocaleToggle.spec.ts`, `ScrollRestoration.spec.ts`, `shellStore.spec.ts`, `EmailEditorView.spec.ts` — real `createRouter`/`routes`/`AppLayout`/`useShellStore`, `vi.stubGlobal("fetch", …)` is the only stub).
- DEV-4 and DEV-12 are contract/visual-dimension deviations respectively and carry no separate obligation for the integration dimension beyond the in-scope behaviour/shell-change coverage checked above; DEV-7 is directly proven by a named frontend integration test as noted.
- No compose stacks were started or stopped; no tracked files were edited; no worker reports, review files, or earlier-round evidence (`round-1`..`round-3`) were read.
