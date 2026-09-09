<!-- BEGIN project-context-initializer:artifact -->
# Architecture and flows

Two stacks coexist in this repository during the migration (see `project-overview.md`). The old
Symfony 8 stack (`src/`, `templates/`, `assets/`, `config/`, `frankenphp/`) is unchanged and still
serves production on port 23456. The next Spring Boot 4.1.1 + Vue 3 SPA stack (`backend/`,
`frontend/`, `mercure/`) exists on branch `migration/spring-vue`, runs only behind
`compose.next.yaml`/`compose.next.prod.yaml`, and has not been cut over. Diagrams: old stack —
`diagrams/module-dependencies.mmd`, `diagrams/primary-runtime-flow.mmd`; next stack —
`diagrams/next-stack-module-dependencies.mmd`, `diagrams/next-stack-event-flow.mmd` (Mermaid
source, rendering not run).

## Old stack — Symfony (unchanged since `91f8f85`)

Single Symfony 8 application served by FrankenPHP (worker mode) behind Caddy, with a second
container running the Messenger worker and one Postgres instance.

### Runtime entrypoints (Observed)

| Entrypoint | Where | Notes |
| --- | --- | --- |
| HTTP front controller | `public/index.php` via `frankenphp/Caddyfile` (`@phpRoute` rewrite, worker `./public/index.php`) | Caddy serves `/assets/*` immutable, `/.well-known/mercure*` goes to the hub |
| Console | `bin/console` | migrations, `typescript:build`, `messenger:consume` |
| Worker | compose `worker`: `messenger:consume async scheduler_default --time-limit=3600 --memory-limit=128M` | dev + prod |
| One-shot migrations | compose.prod `migrations` job, `doctrine:migrations:migrate --all-or-nothing` | `php`/`worker` wait on `service_completed_successfully` |
| Entrypoint script | `frankenphp/docker-entrypoint.sh` | waits for DB, auto-migrates when `AUTO_MIGRATE=1`, builds TS in dev |

### Routing shape (Observed, `src/Controller/*.php`)

- Locale prefix `#[Route('/{_locale}')]` with `pl|en` requirement and default `pl` on every panel
  controller.
- Panel GET routes: `dashboard`, `events`, `customers`, `customer_show` (`c_\d+`), `automations`,
  `automation_new|edit` (`a\d+`, `?view=list|flow`), `campaigns`, `email_new|edit` (`k\d+`), `popups`
  (`?preview=`), `popup_new|edit` (`p\d+`, `?type=&device=`), `feeds`, `import` (`/{_locale}/import/{step}` 1–4),
  `settings` (`/settings/{tab}`), `login` (GET+POST), `logout` (POST), `home`, `demo`.
- Non-localised: `POST /collect` (ingestion), `POST /preferences/theme|sidebar`, `POST /import/upload`,
  `/_storybook/**` (dev/test only).
- Not found handling: unknown customer/settings tab/locale → 404.

### Vertical flow 1 — panel page render

`GET /pl/dashboard` → `DashboardController::index` → `Panel\Content\{DashboardMetrics,EventFeed,CustomerDirectory,AutomationCatalog}` +
`Panel\EventStreamTopic` → `Panel\Format` (PL number/money/time) → `templates/pages/dashboard.html.twig`
extends `layout/app.html.twig` → shell reads Twig global `panel` (`Twig\PanelExtension` → `Panel\PanelContext`
→ `Navigation`, `Workspace`, `PanelPreferences`, `PanelIdentity` from the session) → sidebar/topbar organisms.
All page data is static sample content; only preferences and identity come from the Postgres session.

### Vertical flow 2 — event ingestion and live stream (the only real domain flow)

1. Browser / tracker: `POST /collect` JSON (`idempotency_id`, `type`, `detail`, `customer_id`,
   `customer_name`, `site`, optional `occurred_at`).
2. `EventIngestionController::collect` → `Tracking\TrackedEvent::fromPayload` (13 supported types;
   throws `InvalidEventPayload` → 400).
3. `Tracking\EventIngestion::ingest`: cache key `event.seen.<xxh128(idempotency_id)>` in `cache.app`
   (Doctrine DBAL adapter → `cache_items` UNLOGGED table). Hit → `false` → HTTP 200 `duplicate`.
   Miss → save 24 h, render `components/molecules/event-row.html.twig`, publish
   `{"html": …}` to Mercure topic `/accounts/1/events` → HTTP 202 `accepted`.
4. `assets/controllers/event-stream.ts` opens `EventSource` on `/.well-known/mercure?topic=…`
   (`withCredentials`), sets `data-stream-state` connecting→live, prepends rows (max 80) unless
   `data-paused="true"`.
5. Caddy Mercure hub: `anonymous` subscribers and `subscriptions` API enabled; publisher JWT from
   `MERCURE_JWT_SECRET` with `publish: '*'`.

Trust boundary: `/collect` is unauthenticated, has no tenant/site key check and no rate limit; the
account id is hard-coded (`'1'`). Dedup is cache-based, so a cache flush re-admits old ids (Observed).

### Vertical flow 3 — login, preferences, import upload

- `POST /pl/login` validates the e-mail format only, stores it in session key `panel.identity`,
  redirects to dashboard; `POST /pl/logout` clears it. No password check, no firewall.
- `POST /preferences/theme|sidebar` store `panel.theme` / `panel.sidebar`; theme is rendered
  server-side so there is no flash.
- `POST /import/upload` moves the file to `var/import/<random>.<whitelisted-ext>`; wizard steps are
  URLs `/pl/import/1..4`. No parsing of the file happens yet.

### Data and state — old stack (Observed)

| Store | Table / location | Owner | Notes |
| --- | --- | --- | --- |
| Sessions | `sessions` (LOGGED) | `PdoSessionHandler` over the DBAL native PDO | test env uses mock file storage |
| Cache | `cache_items` (UNLOGGED) | `cache.adapter.doctrine_dbal` | dedup keys, scheduler state |
| Messenger | `messenger_messages` (auto_setup) | Doctrine transport `async`, `failed` queue | in-memory in test |
| Uploads | `var/import/` | `ImportUploadStorage` | container-local volume in dev |
| Domain entities | none | `src/Entity/` empty | product-level entities not started |

### Deployment shape — old stack

- Dev: `compose.yaml` + `compose.override.yaml`, bind-mount `./:/app`, `var/` anonymous volume,
  Xdebug, hot reload, ports `HTTP_PORT/HTTPS_PORT/HTTP3_PORT`.
- Prod on this host: `compose.yaml` + `compose.prod.yaml` with `--env-file .env.prod.docker`,
  plain HTTP on port 23456 behind an external TLS proxy for https://kivvi.click, dedicated
  `*_prod` volumes. **Still the same shared compose-project-name hazard as before** (R5) —
  unaffected by the migration until CUT-1.

## Next stack — Spring Boot API + Vue 3 SPA (branch `migration/spring-vue`)

Spring Boot 4.1.1 API (`backend/`) behind a standalone Mercure hub used purely as the edge
(`mercure/`, Caddy + `dunglas/mercure:v0.24.2`), serving a built Vue 3.5.42 SPA (`frontend/`) and a
JSON API, over one Postgres 18 instance — no separate worker container (the scheduler runs inside
`api`, DEV-6). Orchestrated by `compose.next.yaml` (dev/verification) and `compose.next.prod.yaml`
(delivered, not deployed).

### Runtime entrypoints (Observed)

| Entrypoint | Where | Notes |
| --- | --- | --- |
| Edge / TLS termination | `mercure` container (`mercure/Caddyfile`) | reverse-proxies everything except `/.well-known/mercure*` to `api:8080`; blocks `/actuator/*` |
| SPA document + 404 | `backend/.../web/SpaDocumentController.java` | matches `domain/RouteTable.java`; serves the built SPA's `index.html` equivalent or a minimal 404 document |
| JSON API | `backend/.../web/*Controller.java` | `GET /api/v1/{locale}/...` per page, plus `POST /collect`, `POST /{locale}/login\|logout`, `POST /preferences/theme\|sidebar`, `POST /import/upload` |
| Scheduler | `infrastructure/scheduling/{HeartbeatJob,SchedulingConfig}.java` | `@Scheduled` via `SchedulingConfigurer` + ShedLock, runs inside `api` |
| Health | Spring Boot Actuator `/actuator/health` | internal only, not reachable through the edge |

### Vertical flow 1 — panel page render (next stack)

`GET /pl/dashboard` (browser navigation, real document request) → `mercure` edge →
`SpaDocumentController` matches `RouteTable` → returns the built SPA document → Vue app boots →
`router/index.ts` resolves `DashboardView.vue` → view calls `GET /api/v1/pl/dashboard`
(`DashboardController` → `DashboardViewService` → `DashboardFixtures` + `Format`) → renders with the
same markup/classes as the old Twig template, formatted numbers computed server-side (never
client-side — enforced by `frontend/eslint.config.js`). Shell chrome (`shellstore`) is loaded
separately via `GET /api/v1/{locale}/shell`.

### Vertical flow 2 — event ingestion and live stream (next stack)

Same external contract as the old stack, with one accepted deviation (DEV-3):

1. `POST /collect` → `mercure` edge → `CollectController` → `EventIngestionService` →
   `domain/tracking/TrackedEvent` validation (same 13 types, same PL error messages).
2. Dedup: `infrastructure/tracking/EventDedupStore` inserts into Postgres `event_dedup`
   (`idempotency_hash` PK, `expires_at`, 24 h TTL) — insert-or-conflict replaces the old stack's
   cache-key hit/miss. Conflict → 200 `duplicate`; insert → save, publish, 202 `accepted`.
3. Publish: `infrastructure/mercure/HttpMercurePublisher` posts to the Mercure hub over plain HTTP
   inside the compose network, with a **JSON event payload** (`{"event": {time,type,typeIcon,tone,
   detail,customerId?,customerName?,siteName?,siteColor?}}`) instead of the old stack's
   `{"html": …}` — DEV-3. Topic (`/accounts/1/events`) is unchanged. A publish failure propagates
   uncaught (500), matching the old stack exactly.
4. `frontend/src/composables/useEventStream.ts` opens the same `EventSource` URL as the old
   stack's `event-stream.ts`, renders `EventRow.vue` client-side from the JSON payload, same
   prepend/cap-80/pause semantics.
5. Cleanup: hourly `EventDedupCleanupJob` piggybacks on the same scheduler/ShedLock infrastructure
   as the heartbeat job.

See `diagrams/next-stack-event-flow.mmd` for the full sequence.

### Vertical flow 3 — login, preferences, import upload (next stack)

- `POST /{locale}/login` → `LoginController`/`LoginService`, Spring Session JDBC (`SESSION` cookie,
  `spring_session`/`spring_session_attributes` tables) replaces PHP's native session store — DEV-9
  (cookie name changes, sessions are not migrated at cutover; nobody is a real logged-in user
  today so this has no user-visible cost).
- `POST /preferences/theme|sidebar` → `PreferencesController`/`PreferencesService`, same session
  keys semantics; the SPA POST uses `keepalive: true` and is awaited before any reload
  (`frontend/src/stores/shell.ts`).
- `POST /import/upload` → `ImportUploadController`/`ImportUploadService`/`ImportUploadStorage`,
  same random-name/whitelisted-extension storage, same 302-to-step-2 contract; the SPA's `Dropzone`
  posts with `fetch` and follows the redirect exactly like the old stack's `upload.ts`.

### Data and state — next stack (Observed, `V1__baseline.sql`)

| Store | Table / location | Owner | Notes |
| --- | --- | --- | --- |
| Sessions | `spring_session`, `spring_session_attributes` | Spring Session JDBC | DEV-5/DEV-9 mapping of the old `sessions` table |
| Scheduler lock | `shedlock` | ShedLock JDBC provider | DEV-5 mapping of the old cache-based scheduler state |
| Event dedup | `event_dedup` (`idempotency_hash` PK, `expires_at`) | `EventDedupStore` | DEV-5 mapping of the old `cache_items` dedup keys |
| Async queue | none | — | `messenger_messages` has no counterpart; DEV-5 records its delta as always 0 in the oracle |
| Uploads | `var/import/` inside the `api` container | `ImportUploadStorage` | not a named volume — no need to survive a container restart during the journey |
| Domain entities | none beyond the above | Schema frozen at `V1__baseline.sql` until CON-1 | fixtures carry sample data as Java classes, not DB rows — see R20 |

### Session concurrency invariant (next stack, no old-stack equivalent needed)

PHP's native session handler serializes concurrent requests to the same session file for free;
Spring Session JDBC does not. `infrastructure/session/SessionRequestSerializationFilter` (+
`SessionLockRegistry`, `SessionRequestSerializationConfig`) reproduces that lock explicitly, keyed
on the `SESSION` cookie value, registered outside `SessionRepositoryFilter`'s own filter order, with
a bounded 30 s timeout (503 on timeout). See `backend/.agents/project-context.md` for the full
mechanism.

### Frontend architecture (next stack, Observed)

- `useIntents.ts` composable replaces the old stack's single delegated `data-action` click listener
  — `toggle-sidebar`, `set-theme`, `open-command-bar`, `navigate`, `go-customer`, `go-automation`,
  `go-email`, `go-page` intents. Every navigation intent is a real `window.location.href` change,
  never `router.push` — the app is a genuine multi-document-feeling SPA by design, matching the
  oracle's own recorded `GET`s.
- `router/scrollRestoration.ts` implements a hydration-aware `scrollBehavior` with a narrow
  `sessionStorage` reload fallback (native `history.state.scroll` restore does not survive an
  actual reload in the verified Chromium build) — see `frontend/.agents/project-context.md`.
- CSS is byte-identical to the old stack's four files, copied verbatim.
- i18n catalogue is assembled at build time from per-journey message files
  (`import.meta.glob("./messages/*.{pl,en}.ts")`), merged into base `pl.ts`/`en.ts` — the loader
  itself (`i18n/index.ts`) is frozen/canonical after wave-1.

### Security and trust boundaries (next stack — unchanged from old stack by design)

- No authentication beyond a display-only login form; `/collect` unauthenticated, no rate limit,
  account id still effectively hard-coded via `EventStreamTopic`. This is intentional parity, not a
  regression — R4 in `risks-and-unknowns.md` is unchanged, carried forward.
- Mercure hub still allows `anonymous` subscribers to any topic (`mercure/Caddyfile`, same as
  `frankenphp/Caddyfile`).
- Session cookie `SESSION` is `HttpOnly`, `SameSite=Lax`, hard-coded `Secure=true` (the only entry
  point is the TLS-terminating edge in both dev and prod).

### Observability (next stack)

Spring Boot Actuator `/actuator/health` (internal only, kept off the public edge); default Spring
Boot logging (JSON/console per Boot defaults — not further configured, Observed). No metrics,
tracing, or error tracker — same gap as the old stack (Unknown → none observed).

### Deployment shape — next stack

- Dev/verification: `compose.next.yaml` — `api` (Spring Boot, built via multi-stage `backend/Dockerfile`
  that also builds the SPA and copies `frontend/dist` into `backend/src/main/resources/static/`),
  `mercure` (edge, ports from `HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT`), `database` (`postgres:18-alpine`,
  no fixed host port — random published port per the resource-lease convention).
- Prod overlay (delivered, **not deployed**): `compose.next.prod.yaml` — `mercure` publishes plain
  HTTP on `${HTTP_PORT:-23456}` only (TLS terminates externally, same as today), `database` gets its
  own `database_data_prod` volume (never shared with the old stack's `database_data_prod`, so a
  future CUT-1 does not clobber the old stack's data before an explicit decision).
- Per-migration-journey-worker isolation: compose project `kivvi-w-<journey>`, edge port pool
  `19000 + 10·n`, concurrency capped at 2 (disk headroom) — see `backend/.agents/project-context.md`
  §"Ports / leases / hazards" and R21 in `risks-and-unknowns.md`.
- **Not yet cut over.** Production (port 23456, https://kivvi.click) is still served entirely by the
  old stack. `CUT-1` (swap the compose stacks on this host) is a separately authorized packet in the
  plan, reached only after `RR-1` (rollback rehearsal) and the final all-journey cohort passing —
  which passed 6/6 on `dae1696` (`run.json` final_cohort PASS; cutover not yet
  `CUTOVER_READY`).

## Breaking changes and accepted deviations between the two stacks

Full table in `context/plans/2026-09-08-symfony-to-spring-vue-migration.md` §"Accepted deviations"
(DEV-1..13); the ones with the widest blast radius:

- DEV-3: Mercure payload `{html}` → JSON event.
- DEV-4: document `GET`s return the SPA shell; page HTML bodies are not diffed, only method/path/
  status/final URL; API calls are the new contract baseline.
- DEV-5: table-name mapping (`sessions`→`spring_session`, `cache_items` dedup→`event_dedup`,
  scheduler state→`shedlock`, `messenger_messages`→none).
- DEV-6: no separate worker container — the heartbeat runs inside `api`.
- DEV-9: session cookie renamed `SESSION`; sessions are not migrated at cutover.

## Contradictions

See `risks-and-unknowns.md` — in particular R19 (`AGENTS.md`/`CLAUDE.md` still describe the old
stack as canonical), R20 (fixtures-as-Java-classes drift from the plan), R21 (host resource
contention affecting verification), plus the carried-over R2/R3/R5/R8/R17/R18 which are unaffected
by the migration until cutover.
<!-- END project-context-initializer:artifact -->
