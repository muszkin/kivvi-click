<!-- BEGIN project-context-initializer:context -->
# `backend/` — project context

| Field | Value |
| --- | --- |
| Path | `backend/` |
| Scope | Spring Boot API for kivvi-click, post-migration (single stack) |
| Source revision | `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main` |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (rolled up: `.mvn/`, `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources` — all structural leaves under this boundary) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.
Precedence: current user instructions, `CLAUDE.md`/`AGENTS.md`, code and runtime behaviour
outrank this file for anything stale.

## Purpose

Spring Boot 4.1.1 / Java 25 backend that replaced the Symfony app after a completed migration
(`../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`). Serves a JSON API under
`GET /api/v1/{locale}/...` for the Vue 3 SPA in `../frontend/`, plus the document routes the SPA
itself is served from. This is now the **only** implementation — the migration's cutover (CUT-1,
live 2026-09-09T14:18:42Z) and cleanup (CON-1) removed the old Symfony application entirely; this
backend runs behind `../compose.yaml` (dev) / `../compose.prod.yaml` (prod, currently serving
`https://kivvi.click` on this host) and there is no old-stack read-only prohibition to observe
any more.

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
`domain.tracking.EventStreamTopic` (a source-text scan, not a bytecode rule — ArchUnit cannot see
a compiler-folded string constant).

Per-page pattern: `web/<Page>Controller` → `application/<Page>ViewService` →
`fixtures/<Page>Fixtures` (+ domain types), returning a JSON view-model pre-formatted with
`domain/Format.java` (PL number/money/percent/`timeAgo`) — **the SPA never formats numbers
itself**, enforced by `../frontend/eslint.config.js`'s `no-restricted-imports`/
`no-restricted-globals`/`no-restricted-syntax` rules against `Intl`/`toLocaleString`/`toFixed`.

## Entrypoints

| Entrypoint | Where | Notes |
| --- | --- | --- |
| App bootstrap | `src/main/java/click/kivvi/KivviApplication.java` | plain `SpringApplication.run` |
| SPA document + 404 | `web/SpaDocumentController.java` | `GET /`, `/{locale:pl\|en}`, `/{locale:pl\|en}/**`; matches against `domain/RouteTable.java`; unmatched path → 404 document, `Content-Type: text/html; charset=UTF-8` explicit (servlet default is ISO-8859-1 — Polish diacritics would mangle otherwise) |
| Shell view-model | `web/ShellController.java` → `application/ShellViewService.java` | `GET /api/v1/{locale}/shell` — nav groups, workspace, user, theme, sidebar; drives `../frontend/src/stores/shell.ts` |
| Page view-models | `web/{Dashboard,Events,Customers,Automations,Campaigns,Feeds,Import,Settings,Widget,Landing}Controller.java` | one `GET /api/v1/{locale}/<page>` per journey; DTOs in `web/dto/*Response.java` |
| Event ingestion | `web/CollectController.java` → `application/tracking/EventIngestionService.java` | `POST /collect`; validates via `domain/tracking/TrackedEvent.java`, dedups via `infrastructure/tracking/EventDedupStore.java` (`event_dedup` table), publishes via `infrastructure/mercure/HttpMercurePublisher.java` |
| Login / logout | `web/LoginController.java` → `application/LoginService.java` | `POST /{locale}/login` (form → redirect or re-render with error), `POST /{locale}/logout` |
| Preferences | `web/PreferencesController.java` → `application/PreferencesService.java` | `POST /preferences/theme`, `POST /preferences/sidebar` |
| Import upload | `web/ImportUploadController.java` → `application/ImportUploadService.java` → `infrastructure/importing/ImportUploadStorage.java` | multipart → random-name/whitelisted-ext file inside the container, 302 to step 2 |
| Scheduler | `infrastructure/scheduling/{HeartbeatJob,HeartbeatTrigger,SchedulingConfig}.java` | hourly heartbeat, runs inside `api` (no separate worker container) |
| Health | Spring Boot Actuator, `management.endpoints.web.exposure.include: health` (`application.yml`) | `/actuator/health`; kept off the public Mercure edge (`../mercure/Caddyfile` `respond /actuator/* 404`) |

## Invariants (do not break)

- **Session-lock parity** (`infrastructure/session/SessionRequestSerializationFilter.java` +
  `SessionLockRegistry.java` + `SessionRequestSerializationConfig.java`, formalized in
  `../docs/adr/0001-serialize-requests-per-session-like-php.md`): PHP's native session handler
  used to serialize concurrent requests to the same session file; Spring Session JDBC has no
  such lock by default. This filter reproduces it — registered at `Ordered.HIGHEST_PRECEDENCE +
  10` (outside `SessionRepositoryFilter`, whose own order is `HIGHEST_PRECEDENCE + 50`), reads
  the session id straight off the `SESSION` cookie (not `getRequestedSessionId()`, which only
  knows Tomcat's own `JSESSIONID`), serializes per-session with a bounded 30s timeout (503 on
  timeout), and is reference-counted so a quiet session leaves nothing in the registry. Do not
  remove or reorder this filter, and do not rename the session cookie away from `SESSION`
  without updating it (`server.servlet.session.cookie.name`, `application.yml`).
- **ShedLock-backed heartbeat**: `HeartbeatJob.tick()` is
  `@SchedulerLock(name="heartbeat", lockAtLeastFor="PT1M")`; `HeartbeatTrigger` fires exactly one
  interval after the *last recorded lock*. `SchedulingConfig` wires this through
  `SchedulingConfigurer` (not `@Scheduled`, since the trigger is custom), with
  `defaultLockAtMostFor = PT55M` bounding a crashed run. The `shedlock` table shape is frozen in
  `src/main/resources/db/migration/V1__baseline.sql` — do not rename it.
- **Schema is frozen** at `V1__baseline.sql` (`SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`,
  `shedlock`, `event_dedup`) — still the only migration file (Observed, `ls
  src/main/resources/db/migration/`, 2026-09-09). A new table needs a new `V2__*.sql`, never an
  edit to `V1__baseline.sql`.
- **Mercure publish over plain HTTP inside the compose network** (`HttpMercurePublisher.java`):
  posts `topic`/`data` as `application/x-www-form-urlencoded` with a publisher JWT to
  `kivvi.mercure.url` (`http://mercure/.well-known/mercure` by default — see `../mercure/
  Caddyfile`'s second `mercure:80` site address, which is what lets Caddy skip automatic HTTPS
  for this one internal address). A publish failure is **not** caught here — it propagates out
  of `CollectController` as an uncaught exception (500), by design (class Javadoc).
- **Mercure event payload is JSON, not HTML**: the topic (`/accounts/1/events`) carries
  `{"event":{time,type,typeIcon,tone,detail,customerId?,customerName?,siteName?,siteColor?}}`;
  the SPA renders the row client-side.
- **Fixtures are Java classes, not JSON resources** (see R20,
  `../context/map/risks-and-unknowns.md`): the migration plan's decision ledger described
  fixtures as "JSON resources under `backend/src/main/resources/fixtures/`"; what was actually
  built is `fixtures/*.java` — records/static lists compiled into the jar (e.g.
  `fixtures/DashboardFixtures.java`'s `KpiSeed`/`Legend` records). This is a recorded, accepted
  drift — do not "fix" it by adding a parallel JSON loader without a planning decision; keep
  adding new sample data the same way (`fixtures/<Page>Fixtures.java`).
- **Compile-time strictness**: `maven-compiler-plugin` runs with `-Xlint:all -Werror` — every
  warning is a build failure.
- **Spotless (google-java-format) is bound to the `verify` phase**, not just invokable by name —
  `./mvnw verify` fails on a formatting violation; `./mvnw spotless:apply` fixes it.
- Session cookie is always marked `Secure` (`server.servlet.session.cookie.secure: true`,
  hard-coded rather than derived from `forward-headers-strategy`) because the only entry point is
  the TLS-terminating Mercure edge in both dev and prod.

## Commands (Observed, `pom.xml`, `AGENTS.md`)

- `export JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` (host has
  no system Java 25 — pinned toolchain path)
- `cd backend && ./mvnw -q test` — unit gate (JUnit; no Testcontainers)
- `cd backend && ./mvnw -q verify` — integration gate: unit + Testcontainers-backed `*IT` tests
  (Postgres 18 via Testcontainers) + ArchUnit (`ArchitectureTest`) + Spotless check, bound to
  `verify`
- `cd backend && ./mvnw -q spotless:check` / `./mvnw spotless:apply`
- `./mvnw spring-boot:run` (fast local iteration against the compose database) — every CI/gate
  command must still run against the full compose-built image
- CI: `.github/workflows/build.yml` job `backend` — builds the SPA first (`npm run build` in
  `../frontend`), copies `dist/` into `src/main/resources/static/`, then `./mvnw -B -q verify`

## Tests (Observed, `src/test/java/click/kivvi/**`)

- Unit: `application/*ViewServiceTest.java` (one per page), `domain/**` (`EmailValidationTest`,
  `IdentityTest`, `RouteTableTest`, `SpaDocumentTest`,
  `tracking/{EventTypeTest,TrackedEventTest,TrackedSiteTest}`), `infrastructure/**` (session
  lock, heartbeat, mercure publisher request-shape, dedup cleanup),
  `web/*ControllerTest.java` (MockMvc-style, one per controller), `fixtures/*FixturesTest.java`.
- Integration (`*IT.java`, Testcontainers Postgres): `{Automations,Campaigns,Collect,Customers,
  Dashboard,Events,Feeds,Import,Landing,Settings,Shell,Widget}ApiIT`, `SessionRoundTripIT`,
  `SessionRequestSerializationIT` + `SessionRequestSerializationRedProofIT` (proves the race
  exists without the lock, then that the lock closes it), `ShellPagesIT`.
- Architecture: `architecture/ArchitectureTest.java` (ArchUnit, see Package layering above).
- Test-support: `testsupport/FailOnWarnLogExtension.java` — a JUnit extension that fails a test
  on any WARN-level log line.
- Behaviour naming: tests are named after `behaviours.json` ids from the migration oracle
  (`@DisplayName("Bnn …")`); this convention persists as documentation of intent even though the
  migration's own verification loop has ended.
- Last known-good result (final gates, pre-cutover): 299 unit + 142 integration tests, spotless
  and ArchUnit clean (`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/
  closeout.md`). Not re-run by this context refresh — see R12 in
  `../context/map/risks-and-unknowns.md`.

## Deployment (current, single stack)

- Dev: `docker compose -p kivvi-dev up -d --build --wait` (`../compose.yaml`).
- Prod: live on this host since 2026-09-09T14:18:42Z, `docker compose -p kivvi-click --env-file
  ../.env.prod.docker -f ../compose.yaml -f ../compose.prod.yaml up -d --wait`, reached only
  through the `mercure` edge container (`reverse_proxy api:8080`, `../mercure/Caddyfile`) — no
  fixed host port of its own. Observed healthy (`docker ps`, 2026-09-09T16:20Z).
- `pom.xml`'s own `<version>` is still `0.1.0-w0-login`, a leftover from the migration's first
  slice name — cosmetic, does not affect the build.

## Evidence paths

`backend/pom.xml`, `backend/src/main/java/click/kivvi/**`,
`backend/src/main/resources/{application.yml,db/migration/V1__baseline.sql}`,
`backend/src/test/java/click/kivvi/**`, `backend/Dockerfile`, `backend/README.md`,
`../docs/adr/0001-serialize-requests-per-session-like-php.md`,
`../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/{closeout.md,
common-journey-rules.md}`, `../context/migration-oracle/symfony-to-spring-vue/`.
<!-- END project-context-initializer:context -->
