<!-- BEGIN project-context-initializer:artifact -->
# Architecture and flows

Single stack (see `project-overview.md` for the migration history that got here). Spring Boot
4.1.1 API (`backend/`) behind a standalone Mercure hub used purely as the public edge
(`mercure/`, Caddy + `dunglas/mercure:v0.24.2`), serving a built Vue 3.5.42 SPA (`frontend/`) and
a JSON API, over one Postgres 18 instance — no separate worker container (the scheduler runs
inside `api`). Orchestrated by `compose.yaml` (dev/verification, `-p kivvi-dev` on this host) and
`compose.prod.yaml` (the layer running production, `-p kivvi-click`). Diagrams:
`diagrams/module-dependencies.mmd`, `diagrams/primary-runtime-flow.mmd` (Mermaid source,
rendering not run).

## Runtime entrypoints (Observed)

| Entrypoint | Where | Notes |
| --- | --- | --- |
| Edge / TLS termination (dev) or edge-only (prod, external proxy terminates TLS) | `mercure` container (`mercure/Caddyfile`) | reverse-proxies everything except `/.well-known/mercure*` to `api:8080`; blocks `/actuator/*` |
| SPA document + 404 | `backend/.../web/SpaDocumentController.java` | matches `domain/RouteTable.java`; serves the built SPA's document or a minimal 404 document, explicit UTF-8 content type |
| JSON API | `backend/.../web/*Controller.java` | `GET /api/v1/{locale}/...` per page, plus `POST /collect`, `POST /{locale}/login\|logout`, `POST /preferences/theme\|sidebar`, `POST /import/upload` |
| Scheduler | `infrastructure/scheduling/{HeartbeatJob,SchedulingConfig}.java` | `@Scheduled` via `SchedulingConfigurer` + ShedLock, runs inside `api`, no separate worker |
| Health | Spring Boot Actuator `/actuator/health` | internal only, not reachable through the public edge |

## Vertical flow 1 — panel page render

`GET /pl/dashboard` (browser navigation, always a real document request) → `mercure` edge →
`SpaDocumentController` matches `RouteTable` → returns the built SPA document → Vue app boots →
`router/index.ts` resolves `DashboardView.vue` → view calls `GET /api/v1/pl/dashboard`
(`DashboardController` → `DashboardViewService` → `DashboardFixtures` + `domain/Format.java`) →
renders, numbers formatted server-side (never client-side — enforced by
`frontend/eslint.config.js`). Shell chrome (`stores/shell.ts`) is loaded separately via
`GET /api/v1/{locale}/shell`.

## Vertical flow 2 — event ingestion and live stream (the only real domain flow)

1. `POST /collect` (JSON: `idempotency_id`, `type`, `detail`, `customer_id`, `customer_name`,
   `site`, optional `occurred_at`) → `mercure` edge → `CollectController` →
   `EventIngestionService` → `domain/tracking/TrackedEvent` validation (13 supported types,
   `InvalidEventPayload` → 400 with PL messages).
2. Dedup: `infrastructure/tracking/EventDedupStore` inserts into Postgres `event_dedup`
   (`idempotency_hash` PK, `expires_at`, 24h TTL) — insert-or-conflict. Conflict → 200
   `duplicate`; insert → save, publish, 202 `accepted`.
3. Publish: `infrastructure/mercure/HttpMercurePublisher` posts to the Mercure hub over plain
   HTTP inside the compose network with a JSON event payload
   (`{"event": {time,type,typeIcon,tone,detail,customerId?,customerName?,siteName?,siteColor?}}`).
   Topic is `/accounts/1/events` (tenant boundary still a hard-coded constant — see Trust
   boundaries below). A publish failure propagates uncaught (500) by design (Javadoc,
   `HttpMercurePublisher`).
4. `frontend/src/composables/useEventStream.ts` opens `EventSource` on
   `/.well-known/mercure?topic=…`, renders `EventRow.vue` client-side from the JSON payload,
   prepends rows (max 80) unless paused.
5. Cleanup: hourly `EventDedupCleanupJob` piggybacks on the same scheduler/ShedLock
   infrastructure as the heartbeat job.

See `diagrams/primary-runtime-flow.mmd` for the full sequence.

## Vertical flow 3 — login, preferences, import upload

- `POST /{locale}/login` → `LoginController`/`LoginService`, Spring Session JDBC (`SESSION`
  cookie, `spring_session`/`spring_session_attributes` tables). No password check, no firewall —
  the old stack never had one either (parity, not a regression).
- `POST /preferences/theme|sidebar` → `PreferencesController`/`PreferencesService`; the SPA POST
  uses `keepalive: true` and is `await`ed before any DOM/navigation change
  (`frontend/src/stores/shell.ts`) — a reload issued right after the click still observes the
  committed preference, which relies on the backend's session-lock filter (below). See
  `docs/adr/0001` for why this filter exists.
- `POST /import/upload` → `ImportUploadController`/`ImportUploadService`/`ImportUploadStorage`,
  random-name/whitelisted-extension storage inside the `api` container, 302-to-step-2 contract.

## Data and state (Observed, `backend/src/main/resources/db/migration/V1__baseline.sql`)

| Store | Table / location | Owner | Notes |
| --- | --- | --- | --- |
| Sessions | `spring_session`, `spring_session_attributes` | Spring Session JDBC | |
| Scheduler lock | `shedlock` | ShedLock JDBC provider | |
| Event dedup | `event_dedup` (`idempotency_hash` PK, `expires_at`) | `EventDedupStore` | 24h TTL, insert-or-conflict |
| Async queue | none | — | no message-queue/outbox layer exists |
| Uploads | inside the `api` container, no named volume | `ImportUploadStorage` | does not need to survive a container restart mid-wizard |
| Domain entities | none beyond the above | Schema is still exactly `V1__baseline.sql` — no `V2__*.sql` added since the cutover (Observed, `ls backend/src/main/resources/db/migration/`) | fixtures carry sample data as Java classes, not DB rows — R20 |

## Session concurrency invariant

PHP's native session handler used to serialize concurrent requests to the same session file for
free; Spring Session JDBC does not. `infrastructure/session/SessionRequestSerializationFilter`
(+ `SessionLockRegistry`, `SessionRequestSerializationConfig`) reproduces that lock explicitly,
keyed on the `SESSION` cookie value, registered outside `SessionRepositoryFilter`'s own filter
order, with a bounded 30s timeout (503 on timeout). Formalized in `docs/adr/0001`. See
`backend/.agents/project-context.md` for the full mechanism — do not remove or reorder this
filter.

## Frontend architecture (Observed)

- `useIntents.ts` composable is the single dispatcher for `data-action`-style interactions:
  `toggle-sidebar`, `set-theme`, `open-command-bar`, `navigate`, `go-customer`, `go-automation`,
  `go-email`, `go-page`. Every navigation intent is a real `window.location.href` change, never
  `router.push` — matching every transition the old server-rendered app made; the unchanged
  Playwright suite still asserts this.
- `router/scrollRestoration.ts` implements a hydration-aware `scrollBehavior` with a narrow
  `sessionStorage` reload fallback (native `history.state.scroll` restore does not survive an
  actual reload in the verified Chromium build). Formalized in `docs/adr/0002`.
- CSS is byte-identical to the original design system, copied verbatim into
  `frontend/src/styles/`.
- i18n catalogue is assembled at build time from per-journey message files
  (`import.meta.glob("./messages/*.{pl,en}.ts")`), merged into base `pl.ts`/`en.ts` — the loader
  itself (`i18n/index.ts`) is canonical; do not edit it directly for a single journey's strings.

## Security and trust boundaries

- No authentication beyond a display-only login form; `/collect` is unauthenticated, has no rate
  limit, and the tenant/account id is still effectively hard-coded via `EventStreamTopic`. This
  is a carried-over product-level gap, not introduced by the migration — see R4 in
  `risks-and-unknowns.md`. Production at `https://kivvi.click` runs with this gap live.
- Mercure hub allows `anonymous` subscribers to any topic (`mercure/Caddyfile` — `anonymous`,
  `subscriptions` both enabled).
- Session cookie `SESSION` is `HttpOnly`, `SameSite=Lax`, hard-coded `Secure=true` (the only
  entry point is the TLS-terminating edge in both dev and prod — `api` publishes no host port of
  its own).
- No `TRUSTED_PROXIES`-equivalent explicit CIDR list: `server.forward-headers-strategy: native`
  relies on Tomcat's own built-in private-range regex, sufficient because the compose bridge
  network only hands out RFC1918 addresses and `api` has no other ingress path.

## Observability

Spring Boot Actuator `/actuator/health` (internal only, kept off the public edge via
`mercure/Caddyfile`'s `respond /actuator/* 404`); default Spring Boot logging, not further
configured (Observed). No metrics, tracing, or error tracker (Unknown → none observed) —
monitoring gap tracked as R17/PIO-112 in `risks-and-unknowns.md`.

## Deployment shape

- Dev: `docker compose -p kivvi-dev up -d --build --wait` — `api` (multi-stage
  `backend/Dockerfile`: Node stage builds the SPA and copies `frontend/dist` into
  `backend/src/main/resources/static/`, then a Maven stage, then a JRE runtime image), `mercure`
  (edge, ports from `HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT`), `database` (`postgres:18-alpine`, no
  fixed host port — `docker compose -p kivvi-dev port database 5432`).
- Prod on this host, live since 2026-09-09T14:18:42Z (`docker compose -p kivvi-click
  --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait` from this
  directory only): `mercure` publishes plain HTTP on `${HTTP_PORT:-23456}` only; TLS terminates
  at an external reverse proxy for `https://kivvi.click`; `database` uses its own named volumes
  (`kivvi-next_database_data`, `kivvi-next_mercure_data`, `kivvi-next_mercure_config` —
  `compose.prod.yaml` explicit `name:` overrides; these live volume names must stay stable). Observed running and healthy 2026-09-09T16:20Z (`docker ps`): `kivvi-click-api-1`,
  `kivvi-click-mercure-1` (both started ~13:57-16:02Z, i.e. redeployed after the CON-1 code
  commits), `kivvi-click-database-1` (continuously up since the CUT-1 cutover, 14:18:25Z).
- No per-journey worktree leasing is needed for routine work now that the migration run has
  ended; `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/` retains the
  historical per-journey isolation scheme (`kivvi-w-<journey>` compose projects, port pool
  `19000 + 10·n`) as evidence, not as an active convention.

## Accepted parity deviations still in effect

Full table in `context/plans/2026-09-08-symfony-to-spring-vue-migration.md` §"Accepted
deviations" (DEV-1..12; DEV-13 retired 2026-09-09, unused). The ones with the widest blast
radius:

- DEV-3: Mercure payload `{html}` → JSON event (see Vertical flow 2).
- DEV-4: document `GET`s return the SPA shell; page HTML bodies are not diffed, only
  method/path/status/final URL.
- DEV-5: table-name mapping (`sessions`→`spring_session`, dedup cache key→`event_dedup`,
  scheduler state→`shedlock`).
- DEV-6: no separate worker container — the heartbeat runs inside `api`.
- DEV-9: session cookie renamed `SESSION`; sessions were not migrated at cutover (no real users
  existed to migrate).
- Two deviations were promoted to formal ADRs during the migration: session-lock parity
  (`docs/adr/0001`) and scroll-restoration parity (`docs/adr/0002`).

## Contradictions

See `risks-and-unknowns.md` — in particular R20 (fixtures-as-Java-classes drift from the plan,
accepted), R21 (host resource contention observed during verification, historical), plus
the carried-over R3/R5/R8/R17. R22/R23 were resolved in `c846294`. R2 and R18 were specific to the now-deleted Symfony/FrankenPHP
stack and are resolved/moot; R2's underlying instruction files were rewritten by CON-1.
<!-- END project-context-initializer:artifact -->
