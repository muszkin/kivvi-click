# Wave-2 verifier — dimension: integration

**Status: FAIL** — bound to wave SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration`
(`git rev-parse HEAD` == wave SHA; working tree clean before any command ran; no frontend build
output created).

All commands run and green (0 failures) — the FAIL is not a red test, it is missing coverage:
three behaviours in this wave's scope have no qualifying integration-level test (real Spring
context + Testcontainers + real HTTP, or a genuine frontend router/store integration test), per
the packet's explicit rule that a `@WebMvcTest` slice or a component test fed a hardcoded prop does
not count.

## Per-journey verdicts

| Journey | Verdict | Reason |
| --- | --- | --- |
| event-stream | **regression** (coverage gap, not a red test) | B15, B17, B19 proven (see below); B16 (unknown event type → 400) has **no `*IT` test** — only a disqualified `@WebMvcTest` slice and a pure domain unit test; B24's SSE-subscriber wiring has **no frontend integration test** — `EventsView.spec.ts` stubs `EventSource` but never asserts an instance was opened or exercises `onmessage`; B18 ("never published twice") is only weakly covered (real-Postgres store test, not through `POST /collect`, with no re-check of the stub hub's call count on replay). |
| customers | **regression** (coverage gap, not a red test) | B05, B25, and the customers/profile B01 rows are solidly proven through real HTTP. B03 (detail route keeps its index section active) has **no qualifying test**: no `*IT` requests `route=customer_show` against the real shell endpoint, and the one frontend test with a matching name (`Sidebar.spec.ts`) hands the component the already-computed `current: "customers"` prop instead of deriving it from a router navigation — it proves the presentation component, not the integration the behaviour describes. |

Full test-by-test citations, including which tests are real and which are honest-but-unindexed
vs. actually missing, are in `integration/behaviour-mapping.md`.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration` | 0 (`22d7fcb6380723728a33fc21fda22a92594a2e88`) |
| 2 | `git status --short` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration` | 0 (empty — clean tree) |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `.../verify-wave-2-integration/backend` | 0 |
| 4 | `npm ci` | `.../verify-wave-2-integration/frontend` | 0 |
| 5 | `npm run test:integration -- --run` | `.../verify-wave-2-integration/frontend` | 0 |

## Backend `*IT` inventory and results (`./mvnw -q verify`, Failsafe)

9 classes, all `@SpringBootTest` + `@Testcontainers` + `@Container @ServiceConnection
PostgreSQLContainer("postgres:18-alpine")`; 34 tests total, 0 failures/errors/skipped:

- `CollectApiIT` — 2 tests (B19 full 202/200/400 sequence; DEV-3/B17 JSON-publish content). **New this wave.**
- `CustomersApiIT` — 6 tests (B01/B25 document 200s, B25 list/detail payloads, B05 API and document 404s). **New this wave.**
- `EventsApiIT` — 3 tests (B01 document 200, B24 payload counts, B07 locale 404). **New this wave.**
- `EventDedupStoreIT` (`infrastructure.tracking`) — 4 tests (B18 store-level claim/refuse/expiry/sweep against real `event_dedup`). **New this wave.**
- `FeedsApiIT` — 3 tests (wave-1, unaffected — still green).
- `LandingApiIT` — 4 tests (wave-1, unaffected — still green).
- `ShellApiIT` — 7 tests (wave-0/1, unaffected — still green).
- `SessionRoundTripIT` — 4 tests (wave-0, unaffected — still green).
- `HeartbeatSchedulerIT` (`infrastructure.scheduling`) — 1 test (wave-1, unaffected — still green).

Testcontainers confirmed started (not skipped): log lines `tc.postgres:18-alpine -- Container
postgres:18-alpine started in ...s`, `Container is started (JDBC URL: jdbc:postgresql://...)`, one
container per fresh Spring test context; Flyway reports `PostgreSQL 18.6` on every container.
`./mvnw -q verify` (Surefire unit slices, then Failsafe integration-test/verify) exited 0.

`CollectApiIT`'s Mercure stub: **not a mock of the publisher class** — it swaps the network
endpoint. A local `com.sun.net.httpserver.HttpServer` is started once at class load and wired in as
`kivvi.mercure.url` via `@DynamicPropertySource`; the app's real HTTP client still makes a real POST
to it with the real signed JWT/form body, and the stub only replaces the far end (the actual
`dunglas/mercure` daemon) rather than intercepting the call inside the JVM. This is the honest
reading of "stubbed at the HTTP boundary" — see `CollectApiIT`'s class Javadoc for the documented
reason (no mocking-library dependency).

## Frontend integration suite (`npm run test:integration -- --run`, Vitest)

`vitest run test/integration --run`: **8 files, 36 tests, all passed** (2.77s) —
`CustomerView.spec.ts`, `CustomersView.spec.ts`, `EventsView.spec.ts` (all new this wave),
`FeedsView.spec.ts`, `landing.spec.ts`, `LoginView.spec.ts`, `shellStore.spec.ts`,
`Sidebar.spec.ts` (wave-0/1, unaffected — still green).

## Findings (why this is FAIL)

1. **B16** (unknown event type rejected) has zero coverage at the real-Spring-context + real-HTTP
   level. `grep -rn "Nieznany typ\|teleport" backend/src/test --include="*IT.java"` returns nothing.
2. **B24**'s SSE-subscriber component/router integration is not tested on the frontend:
   `EventsView.spec.ts` stubs `EventSource` and records instances but never asserts on them or
   drives a message through `onmessage`; all its assertions duplicate the static-payload ground
   `EventsApiIT` already covers on the backend.
3. **B03** (detail route keeps its index section active) has no test that derives the active
   section from an actual route — the one test with a matching name hands the answer to the
   component as a prop instead of computing it through router/store/API wiring.

Per the packet's FAIL rule ("FAIL if any behaviour lacks such a test"), these three gaps are
sufficient on their own, independent of the fact that every test that does exist is green.

## Evidence paths

- `waves/wave-2/integration/behaviour-mapping.md` — full behaviour → test table (including B18's
  weak coverage and the naming-honesty spot check).
- `waves/wave-2/integration/backend-mvnw-verify.log` — full backend `./mvnw -q verify` output.
- `waves/wave-2/integration/failsafe-reports/` — per-class Failsafe `.txt` summaries for all 9 `*IT`
  classes (copied from the checkout's `backend/target/failsafe-reports`).
- `waves/wave-2/integration/frontend-npm-ci.log` — `npm ci` output.
- `waves/wave-2/integration/frontend-npm-test-integration.log` — full
  `vitest run test/integration --run` output (36/36 passed).
- Source read directly from the checkout (read-only, not copied):
  `backend/src/test/java/click/kivvi/CollectApiIT.java`, `CustomersApiIT.java`, `EventsApiIT.java`,
  `ShellApiIT.java`, `infrastructure/tracking/EventDedupStoreIT.java`,
  `web/CollectControllerTest.java`, `domain/NavigationCatalog.java`;
  `frontend/test/integration/EventsView.spec.ts`, `CustomerView.spec.ts`, `Sidebar.spec.ts`,
  `shellStore.spec.ts`; `frontend/test/unit/EventStream.spec.ts` (confirmed unit-scope only).
