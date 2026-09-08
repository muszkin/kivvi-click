# Wave-1 integration dimension — behaviour → test mapping

Wave SHA `9427fdb1d92e4e606e67471638031d1bc0fb73d4`. Checked in
`/home/muszkin/work/kivvi-click-wt/verify-wave-1-integration` (HEAD == wave SHA, clean tree).

## Backend *IT inventory (real Spring context + Testcontainers Postgres 18, real HTTP)

`find backend/src/test -iname "*IT.java"` → 4 classes, all using `@SpringBootTest` +
`@Testcontainers` + `PostgreSQLContainer("postgres:18-alpine")`:

| Class | Tests | Result |
| --- | --- | --- |
| `click.kivvi.ShellApiIT` | 7 | 0 failures, 0 errors |
| `click.kivvi.FeedsApiIT` | 3 | 0 failures, 0 errors |
| `click.kivvi.infrastructure.scheduling.HeartbeatSchedulerIT` | 1 | 0 failures, 0 errors |
| `click.kivvi.SessionRoundTripIT` (wave-0) | 4 | 0 failures, 0 errors |

Total: 15 IT tests, 0 failures, 0 errors, 0 skipped. Testcontainers started `postgres:18-alpine`
(log: `Container postgres:18-alpine started`, `Database: jdbc:postgresql://localhost:.../test`,
`PostgreSQL 18.6`) once per IT class (4 containers total, one per Spring context).

## Behaviour mapping

| Behaviour | Requires | Test found | Verdict |
| --- | --- | --- | --- |
| B01 — feeds row (`GET /pl/feeds` document + `GET /api/v1/pl/feeds` view-model) | real HTTP *IT | `FeedsApiIT.feedsPageRenders200`, `FeedsApiIT.feedsPayloadMatchesTheOracleThroughTheRealHttpLayer` | met |
| B01 — landing row (`GET /api/v1/{locale}/landing` view-model) | real HTTP *IT | **none** — only `LandingControllerTest` (`@WebMvcTest(LandingController.class)`), a slice test that never boots the full Spring context, `DataSource`, or Postgres | **NOT MET** |
| B22 — landing part (router/component integration on the frontend) | frontend integration suite | `frontend/test/integration/landing.spec.ts` — mounts a real `vue-router` + `LandingView` at `/`, `/pl`, `/en`, asserts route name, hero markup, plain (non-router) `/pl/demo` href, and per-locale fetch URL | met |
| B30 — feeds (4 sources, 4 feeds, one HTTP 503, 142 mismatched) | real HTTP *IT | `FeedsApiIT.feedsPayloadMatchesTheOracleThroughTheRealHttpLayer` — asserts `kpis`=4, `sources`=4, `feeds`=4, `feeds[2].status`="error", `feeds[2].error`="HTTP 503 — Service Unavailable", `mismatched`=142 | met |
| B20 — Postgres-backed lock/state round-trip | real HTTP-adjacent *IT (Testcontainers) | `HeartbeatSchedulerIT.schedulerTicksOnceAndLockBlocksASecondAttempt` — queries `select count(*) from shedlock where name = ?` and asserts exactly 1 row | met |
| B33 — tick logged, missed-run/lock semantics | real HTTP-adjacent *IT (Testcontainers) | same test — Logback `ListAppender` attached via `ApplicationContextInitializer`; asserts exactly 1 `INFO` event with message `HeartbeatJob.TICK_MESSAGE` ("Scheduler heartbeat tick.") after the fresh-table first tick, and still exactly 1 after a direct re-invocation of the `@SchedulerLock`-guarded `tick()` (proves `lockAtLeastFor` blocks the repeat) | met |

Grep confirmation that no backend *IT calls the landing endpoints:

```
$ grep -rn "getForEntity(\"/\|restTemplate.getForEntity" backend/src/test/java/click/kivvi/*.java
ShellApiIT.java:  "/api/v1/en/shell", "/api/v1/pl/shell", "/de/dashboard", "/pl/nonexistent",
                  "/api/v1/pl/shell?route=dashboard"
FeedsApiIT.java:  "/pl/feeds", "/api/v1/pl/feeds", "/api/v1/de/feeds"
```

No `*IT` class references `/landing` or `/api/v1/*/landing` anywhere in `backend/src/test`.

## Frontend integration suite

`npm run test:integration -- --run` → 5 files, 20 tests, all passed (1.60s):
`FeedsView.spec.ts`, `landing.spec.ts`, `LoginView.spec.ts`, `shellStore.spec.ts`,
`Sidebar.spec.ts`.
