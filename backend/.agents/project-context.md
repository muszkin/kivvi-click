<!-- BEGIN project-context-initializer:context -->
# `backend/` — project context

| Field | Value |
| --- | --- |
| Path | `backend/` |
| Scope | Spring Boot API for the Symfony→Spring Boot + Vue migration (`migration/spring-vue`) |
| Source revision | `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue` (worktree `/home/muszkin/work/kivvi-click-wt/integration`) |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (rolled up: `.mvn/`, `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources` — all structural leaves under this boundary) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.
Precedence: current user instructions, `CLAUDE.md`/`AGENTS.md` (old-stack rules, stale for this
directory — see R19 in `../../context/map/risks-and-unknowns.md`), the migration plan, code and
runtime behaviour outrank this file for anything stale.

## Purpose

Spring Boot 4.1.1 / Java 25 backend that replaces the Symfony app for the migration's "parallel
rewrite" strategy (Observed, `../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`).
Serves a JSON API under `GET /api/v1/{locale}/...` for the Vue 3 SPA in `../frontend/`, plus the
document routes the SPA itself is served from, and preserves the old stack's external contracts
byte-for-byte where the plan requires it (`POST /collect`, `POST /preferences/theme|sidebar`,
`POST /import/upload`, `POST /{locale}/login|logout`). The old Symfony app (`../../src/`) stays
read-only and keeps serving production on port 23456 until cutover (CUT-1); this backend runs only
behind `../../compose.next.yaml` / `compose.next.prod.yaml` today (Observed).

## Package layering (Observed, `pom.xml`, `ArchitectureTest.java`)

```
click.kivvi
├── web/            HTTP layer: @RestController classes + web/dto/* response records
├── application/     use-case services (one per page/journey), orchestrate domain + infrastructure
│   └── tracking/    EventIngestionService
├── domain/          pure logic: Format, Identity, RouteTable, SpaDocument, SupportedLocale, Theme…
│   └── tracking/     TrackedEvent, EventType, EventStreamTopic, InvalidEventPayload, TrackedSite
├── infrastructure/  Spring/Postgres/Mercure adapters: session, scheduling, mercure, importing, config
└── fixtures/        static sample data (records/constants) — the seed content each ViewService reads
```

Enforced by `src/test/java/click/kivvi/architecture/ArchitectureTest.java` (ArchUnit): `domain`
never depends on `web`; `infrastructure` is only reached from `application`; the four top-level
packages form no cycle; only `application.tracking` and `infrastructure.mercure` may touch the
Mercure publisher types; the `/accounts/` topic literal exists only in
`domain.tracking.EventStreamTopic` (a source-text scan, not a bytecode rule — ArchUnit cannot see a
compiler-folded string constant). This mirrors `architecture/rules-translated.md` in the migration
oracle (Observed).

Per-page pattern (Observed, `common-journey-rules.md`): `web/<Page>Controller` →
`application/<Page>ViewService` → `fixtures/<Page>Fixtures` (+ domain types), returning the exact
arrays the old Twig page's `render()` call received, pre-formatted with `domain/Format.java` (PL
number/money/percent/`timeAgo`, ported from `../../src/Panel/Format.php` — **the SPA never formats
numbers itself**, enforced by `../frontend/eslint.config.js`'s `no-restricted-imports`/
`no-restricted-globals`/`no-restricted-syntax` rules against `Intl`/`toLocaleString`/`toFixed`).

## Entrypoints

| Entrypoint | Where | Notes |
| --- | --- | --- |
| App bootstrap | `src/main/java/click/kivvi/KivviApplication.java` | plain `SpringApplication.run` |
| SPA document + 404 | `web/SpaDocumentController.java` | `GET /`, `/{locale:pl\|en}`, `/{locale:pl\|en}/**`; matches against `domain/RouteTable.java` (ported from `inventory/routes.json`, minus `/collect`, `/preferences/*`, `/import/upload`, `/_storybook`); unmatched path → 404 document, `Content-Type: text/html; charset=UTF-8` explicit (servlet default is ISO-8859-1 — Polish diacritics would mangle otherwise) |
| Shell view-model | `web/ShellController.java` → `application/ShellViewService.java` | `GET /api/v1/{locale}/shell` — nav groups, workspace, user, theme, sidebar; drives `../frontend/src/stores/shell.ts` |
| Page view-models | `web/{Dashboard,Events,Customers,Automations,Campaigns,Feeds,Import,Settings,Widget,Landing}Controller.java` | one `GET /api/v1/{locale}/<page>` per journey; DTOs in `web/dto/*Response.java` |
| Event ingestion | `web/CollectController.java` → `application/tracking/EventIngestionService.java` | `POST /collect`; validates via `domain/tracking/TrackedEvent.java`, dedups via `infrastructure/tracking/EventDedupStore.java` (`event_dedup` table), publishes via `infrastructure/mercure/HttpMercurePublisher.java` |
| Login / logout | `web/LoginController.java` → `application/LoginService.java` | `POST /{locale}/login` (form → redirect or re-render with error), `POST /{locale}/logout` |
| Preferences | `web/PreferencesController.java` → `application/PreferencesService.java` | `POST /preferences/theme`, `POST /preferences/sidebar` |
| Import upload | `web/ImportUploadController.java` → `application/ImportUploadService.java` → `infrastructure/importing/ImportUploadStorage.java` | multipart → `var/import/<32 hex>.<whitelisted ext>` inside the container, 302 to step 2 |
| Scheduler | `infrastructure/scheduling/{HeartbeatJob,HeartbeatTrigger,SchedulingConfig}.java` | hourly heartbeat, runs inside `api` (no separate worker — DEV-6) |
| Health | Spring Boot Actuator, `management.endpoints.web.exposure.include: health` (`application.yml`) | `/actuator/health`; kept off the public Mercure edge (`mercure/Caddyfile` `respond /actuator/* 404`) |

## Invariants (do not break — sourced from the plan and `common-journey-rules.md`)

- **Session-lock parity** (`infrastructure/session/SessionRequestSerializationFilter.java` +
  `SessionLockRegistry.java` + `SessionRequestSerializationConfig.java`): PHP's native session
  handler serializes concurrent requests to the same session file; Spring Session JDBC has no such
  lock by default. This filter reproduces it — registered at `Ordered.HIGHEST_PRECEDENCE + 10`
  (outside `SessionRepositoryFilter`, whose own order is `HIGHEST_PRECEDENCE + 50`), reads the
  session id straight off the `SESSION` cookie (not `getRequestedSessionId()`, which only knows
  Tomcat's own `JSESSIONID`), serializes per-session with a bounded 30 s timeout (503 on timeout),
  and is reference-counted so a quiet session leaves nothing in the registry. Do not remove or
  reorder this filter, and do not rename the session cookie away from `SESSION` without updating
  it (`server.servlet.session.cookie.name`, `application.yml`).
- **ShedLock-backed heartbeat**, not Symfony Scheduler's cache-stateful semantics:
  `HeartbeatJob.tick()` is `@SchedulerLock(name="heartbeat", lockAtLeastFor="PT1M")`;
  `HeartbeatTrigger` reproduces `processOnlyLastMissedRun(true)` — next tick is exactly one
  interval after the *last recorded lock*, not "one tick per hour missed". `SchedulingConfig`
  wires this through `SchedulingConfigurer` (not `@Scheduled`, since the trigger is custom), with
  `defaultLockAtMostFor = PT55M` bounding a crashed run. The `shedlock` table shape is frozen in
  `src/main/resources/db/migration/V1__baseline.sql` — do not rename it.
- **Schema is frozen** at `V1__baseline.sql` (`SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`,
  `shedlock`, `event_dedup`) — no migration has been added since wave-0 (Observed, plan §"Global
  implementation constraints": "Schema frozen until CON-1"). A new table needs a new
  `V2__*.sql`, never an edit to `V1__baseline.sql`.
- **Mercure publish over plain HTTP inside the compose network** (`HttpMercurePublisher.java`):
  posts `topic`/`data` as `application/x-www-form-urlencoded` with a publisher JWT to
  `kivvi.mercure.url` (`http://mercure/.well-known/mercure` by default — see `mercure/Caddyfile`'s
  second `mercure:80` site address, which is what lets Caddy skip automatic HTTPS for this one
  internal address). A publish failure is **not** caught here — it propagates out of
  `CollectController` as an uncaught exception (500), matching the old stack's own
  `HubInterface::publish` behaviour exactly (Observed, class Javadoc).
- **Mercure event payload is JSON, not HTML** (DEV-3, plan's accepted deviations): the topic
  (`/accounts/1/events`) is unchanged, but the published `data` is
  `{"event":{time,type,typeIcon,tone,detail,customerId?,customerName?,siteName?,siteColor?}}`
  instead of the old stack's `{"html": "<rendered row>"}`. The SPA renders the row client-side.
- **Fixtures are Java classes, not JSON resources** (see R20,
  `../../context/map/risks-and-unknowns.md`): the plan's decision ledger described fixtures as
  "JSON resources under `backend/src/main/resources/fixtures/`"; what was actually built is
  `fixtures/*.java` — records/static lists compiled into the jar (e.g.
  `fixtures/DashboardFixtures.java`'s `KpiSeed`/`Legend` records). This is a recorded, accepted
  drift (`open_obligations` `OBL-fixtures-drift` in the run ledger) — do not "fix" it by adding a
  parallel JSON loader without a planning decision; keep adding new sample data the same way
  (`fixtures/<Page>Fixtures.java`) for consistency with the rest of the codebase.
- **Compile-time strictness**: `maven-compiler-plugin` runs with `-Xlint:all -Werror` — every
  warning is a build failure (the plan's stated PHPStan-level-5 equivalent).
- **Spotless (google-java-format) is bound to the `verify` phase**, not just invokable by name —
  `./mvnw verify` fails on a formatting violation; `./mvnw spotless:apply` fixes it.
- Session cookie is always marked `Secure` (`server.servlet.session.cookie.secure: true`,
  hard-coded rather than derived from `forward-headers-strategy`) because the only entry point is
  the TLS-terminating Mercure edge in both dev and prod.

## Commands (Observed, `pom.xml`, `common-journey-rules.md`, `.github/workflows/next-build.yml`)

- `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` (host has no system Java 25 — pinned toolchain path, per the run's common rules)
- `cd backend && ./mvnw -q test` — unit gate (JUnit; no Testcontainers)
- `cd backend && ./mvnw -q verify` — integration gate: unit + Testcontainers-backed `*IT` tests (Postgres 18 via Testcontainers) + ArchUnit (`ArchitectureTest`) + Spotless check, bound to `verify`
- `cd backend && ./mvnw -q spotless:check` / `./mvnw spotless:apply`
- `./mvnw spring-boot:run` (fast local iteration against the compose database) — every CI/gate command must still run against the full compose-built image (`common-journey-rules.md` "Stack build hint")
- CI: `.github/workflows/next-build.yml` job `backend` — builds the SPA first (`npm run build` in `../frontend`), copies `dist/` into `src/main/resources/static/`, then `./mvnw -B -q verify`

## Tests (Observed, `src/test/java/click/kivvi/**`)

- Unit: `application/*ViewServiceTest.java` (one per page), `domain/**` (`EmailValidationTest`,
  `IdentityTest`, `RouteTableTest`, `SpaDocumentTest`, `tracking/{EventTypeTest,TrackedEventTest,TrackedSiteTest}`),
  `infrastructure/**` (session lock, heartbeat, mercure publisher request-shape, dedup cleanup),
  `web/*ControllerTest.java` (MockMvc-style, one per controller), `fixtures/*FixturesTest.java`.
- Integration (`*IT.java`, Testcontainers Postgres): `{Automations,Campaigns,Collect,Customers,
  Dashboard,Events,Feeds,Import,Landing,Settings,Shell,Widget}ApiIT`, `SessionRoundTripIT`,
  `SessionRequestSerializationIT` + `SessionRequestSerializationRedProofIT` (proves the race exists
  without the lock, then that the lock closes it), `ShellPagesIT`.
- Architecture: `architecture/ArchitectureTest.java` (ArchUnit, see Package layering above).
- Test-support: `testsupport/FailOnWarnLogExtension.java` — a JUnit extension that fails a test on
  any WARN-level log line, matching the old stack's `phpunit.dist.xml` fail-on-deprecation policy
  (Inferred from naming and `common-journey-rules.md`'s cross-stack parity intent).
- Behaviour naming: tests are named after `behaviours.json` ids from the oracle
  (`@DisplayName("Bnn …")`) — every behaviour the wave's journeys claim must have at least one test
  that fails when the behaviour is removed (`common-journey-rules.md`).

## Ports / leases / hazards

- Runs only inside `compose.next.yaml` (dev/verification) or `compose.next.prod.yaml` (not
  deployed by any slice yet — CUT-1 is a separately authorized packet). No fixed host port of its
  own: reached through the `mercure` edge container (`reverse_proxy api:8080`,
  `../mercure/Caddyfile`).
- Per-journey-worker isolation during the migration run: compose project `kivvi-w-<journey>`, edge
  port pool `19000 + 10·n`, `-Dmaven.repo.local=$WORKTREE/.m2` for writes against a shared
  read-mostly `~/.m2` (plan §"Worktree resource lease"). Concurrency capped at 2 live worktrees —
  host disk headroom is the limiting resource (K7 kill criterion, ≤ 3 GB free triggers
  `BLOCKED_EXTERNAL`); see R21 in `../../context/map/risks-and-unknowns.md` for observed host
  contention from unrelated projects' containers (including self-hosted GitHub Actions runners).
- Never touches ports 23456/23457 (old-stack prod), 18080/18443 (oracle capture), or 8080/8443/5432
  (other host projects) — `resources.md` in the run ledger.
- The old stack (`../../src/`, `../../templates/`, `../../assets/`, `../../compose.yaml`,
  `../../compose.prod.yaml`, `../../Dockerfile`, `../../frankenphp/`) is read-only from this
  directory's perspective — the plan's "Old-stack prohibition" — and still serves production.

## Evidence paths

`backend/pom.xml`, `backend/src/main/java/click/kivvi/**`, `backend/src/main/resources/{application.yml,db/migration/V1__baseline.sql}`,
`backend/src/test/java/click/kivvi/**`, `backend/Dockerfile`, `backend/README.md`,
`../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/common-journey-rules.md`,
`../../context/migration-oracle/symfony-to-spring-vue/`.
<!-- END project-context-initializer:context -->
