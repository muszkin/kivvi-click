# Wave-1 integration dimension — behaviour → test mapping (round 2)

Scope per packet: B22 (landing part), B01 (landing/feeds rows), B30, B20, B33. Wave-0
behaviours are not re-scored here.

| Behaviour | Test | Real Spring context? | Testcontainers Postgres 18? | Real HTTP? | Concrete assertions | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| B01 (landing row) | `LandingApiIT.landingPayloadMatchesTheOracleThroughTheRealHttpLayer` | Yes — `@SpringBootTest(webEnvironment = RANDOM_PORT)` | Yes — `@Container @ServiceConnection PostgreSQLContainer("postgres:18-alpine")` | Yes — `TestRestTemplate.getForEntity("/api/v1/pl/landing", ...)` | `features` size 6, `steps` size 3, `plans` size 2, `plans[1].tier == "Pro"`, `previewTiles` size 4 with values `"8 410"`/`"94 200"` | covered |
| B01 (feeds row) | `FeedsApiIT.feedsPageRenders200` + `feedsPayloadMatchesTheOracleThroughTheRealHttpLayer` | Yes | Yes | Yes | `/pl/feeds` → 200; `/api/v1/pl/feeds` → `kpis`/`sources`/`feeds` size 4, `feeds[2].status == "error"`, error text `"HTTP 503 — Service Unavailable"`, `mismatched == 142` | covered |
| B22 (landing part, backend: `/en` copy + `/pl/demo` redirect) | `LandingApiIT.englishRouteReturnsTheSamePolishCopyThroughTheRealHttpLayer` + `demoRedirectsToDashboardThroughTheRealHttpLayer` | Yes | Yes | Yes | `/api/v1/en/landing` returns the same Polish copy (`features[0].title`, `plans[0].cta` asserted verbatim); `/pl/demo` → 302, `Location: /pl/dashboard` | covered |
| B22 (landing part, frontend router/component) | `frontend/test/integration/landing.spec.ts` (3 tests) — real Vue Router + mounted `LandingView`, `fetch` stubbed at the network boundary only | N/A (frontend) | N/A | N/A (frontend integration, not e2e) | bare `/` resolves to route name `home` and fetches `/api/v1/pl/landing`; `/pl` renders `.hero h1` and a plain (non-router) `href="/pl/demo"` anchor; `/en` fetches `/api/v1/en/landing` | covered |
| B30 | `FeedsApiIT.feedsPayloadMatchesTheOracleThroughTheRealHttpLayer` (same test as B01 feeds row) | Yes | Yes | Yes | 4 sources, 4 feeds, one `status == "error"` with `HTTP 503`, `mismatched == 142` | covered |
| B20 | `HeartbeatSchedulerIT.schedulerTicksOnceAndLockBlocksASecondAttempt` | Yes | Yes | N/A (direct bean call + `JdbcTemplate`, not HTTP — B20/B33 are non-HTTP infra behaviours per the plan) | `select count(*) from shedlock where name = ?` → exactly 1 row | covered |
| B33 | `HeartbeatSchedulerIT.schedulerTicksOnceAndLockBlocksASecondAttempt` | Yes | Yes | N/A | Logback `ListAppender` captures exactly one `INFO "Scheduler heartbeat tick."` event after the first tick; a direct second `heartbeatJob.tick()` call still yields exactly one event (lockAtLeastFor absorbs the repeat) | covered |

No behaviour in scope is covered only by a `@WebMvcTest` slice — `LandingControllerTest` and
`FeedsControllerTest` exist as `@WebMvcTest` slices too, but each in-scope behaviour above has an
independent `*IT`-class assertion with a full Spring context, Testcontainers Postgres 18, and (where
applicable) a real HTTP call through `TestRestTemplate`.

## Naming honesty check

`@DisplayName` / `describe` strings were read against their assertion bodies for all five `*IT`
classes and all 5 frontend integration spec files. No overclaiming found: every display name's
claimed behaviour (status code, payload shape and value, redirect target, lock/log count) is the
thing actually asserted in the test body.

## Wave-0 regression check

`SessionRoundTripIT` (4 tests) and `ShellApiIT` (7 tests) — both wave-0 `*IT` classes — ran in the
same `./mvnw -q verify` invocation as the wave-1 classes and are still green (0 failures/errors/
skipped each). Frontend wave-0 integration specs (`LoginView.spec.ts`, `Sidebar.spec.ts`,
`shellStore.spec.ts`) ran in the same `vitest run test/integration --run` invocation and are still
green. Wave-0 behaviours are not separately re-scored per the packet, but no regression was
observed.
