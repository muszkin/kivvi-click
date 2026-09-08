<!-- BEGIN project-context-initializer:artifact -->
# Architecture and flows

Single Symfony 8 application served by FrankenPHP (worker mode) behind Caddy, with a second
container running the Messenger worker and one Postgres instance. Diagrams:
`diagrams/module-dependencies.mmd`, `diagrams/primary-runtime-flow.mmd` (Mermaid source, rendering `not-run`).

## Runtime entrypoints (Observed)

| Entrypoint | Where | Notes |
| --- | --- | --- |
| HTTP front controller | `public/index.php` via `frankenphp/Caddyfile` (`@phpRoute` rewrite, worker `./public/index.php`) | Caddy serves `/assets/*` immutable, `/.well-known/mercure*` goes to the hub |
| Console | `bin/console` | migrations, `typescript:build`, `messenger:consume` |
| Worker | compose `worker`: `messenger:consume async scheduler_default --time-limit=3600 --memory-limit=128M` | dev + prod |
| One-shot migrations | compose.prod `migrations` job, `doctrine:migrations:migrate --all-or-nothing` | `php`/`worker` wait on `service_completed_successfully` |
| Entrypoint script | `frankenphp/docker-entrypoint.sh` | waits for DB, auto-migrates when `AUTO_MIGRATE=1`, builds TS in dev |

## Routing shape (Observed, `src/Controller/*.php`)

- Locale prefix `#[Route('/{_locale}')]` with `pl|en` requirement and default `pl` on every panel
  controller; `translation.yaml` sets `default_locale: pl`, `enabled_locales: [pl, en]`.
- Panel GET routes: `dashboard`, `events`, `customers`, `customer_show` (`c_\d+`), `automations`,
  `automation_new|edit` (`a\d+`, `?view=list|flow`), `campaigns`, `email_new|edit` (`k\d+`), `popups`
  (`?preview=`), `popup_new|edit` (`p\d+`, `?type=&device=`), `feeds`, `import` (`/{_locale}/import/{step}` 1–4),
  `settings` (`/settings/{tab}`), `login` (GET+POST), `logout` (POST), `home`, `demo`.
- Non-localised: `POST /collect` (ingestion), `POST /preferences/theme|sidebar`, `POST /import/upload`,
  `/_storybook/**` (condition `env('APP_ENV') in ['dev','test']`).
- Not found handling: unknown customer/settings tab/locale → 404 (`tests/Controller/PanelPagesTest.php`).

## Vertical flow 1 — panel page render

`GET /pl/dashboard` → `DashboardController::index` → `Panel\Content\{DashboardMetrics,EventFeed,CustomerDirectory,AutomationCatalog}` +
`Panel\EventStreamTopic` → `Panel\Format` (PL number/money/time) → `templates/pages/dashboard.html.twig`
extends `layout/app.html.twig` → shell reads Twig global `panel` (`Twig\PanelExtension` → `Panel\PanelContext`
→ `Navigation`, `Workspace`, `PanelPreferences`, `PanelIdentity` from the session) → sidebar/topbar organisms.
All page data is static sample content; only preferences and identity come from the Postgres session.

## Vertical flow 2 — event ingestion and live stream (the only real domain flow)

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
5. Caddy Mercure hub: `anonymous` subscribers and `subscriptions` API enabled (`frankenphp/Caddyfile`);
   publisher JWT from `MERCURE_JWT_SECRET` with `publish: '*'` (`config/packages/mercure.yaml`).

Trust boundary: `/collect` is unauthenticated, has no tenant/site key check and no rate limit; the
account id is hard-coded (`'1'`). Dedup is cache-based, so a cache flush re-admits old ids (Observed).

## Vertical flow 3 — login, preferences, import upload

- `POST /pl/login` validates the e-mail format only, stores it in session key `panel.identity`,
  redirects to dashboard; `POST /pl/logout` clears it. No password check, no firewall (`SecurityController`, `PanelIdentity`).
- `POST /preferences/theme|sidebar` store `panel.theme` / `panel.sidebar`; `layout/base.html.twig`
  renders `data-theme` server-side so there is no flash.
- `POST /import/upload` moves the file to `var/import/<random>.<whitelisted-ext>` and keeps name/path
  in session; wizard steps are URLs `/pl/import/1..4`. No parsing of the file happens yet.

## Data and state (Observed)

| Store | Table / location | Owner | Notes |
| --- | --- | --- | --- |
| Sessions | `sessions` (LOGGED) | `PdoSessionHandler` over the DBAL native PDO (`config/services.yaml`) | test env uses mock file storage |
| Cache | `cache_items` (UNLOGGED) | `cache.adapter.doctrine_dbal` | dedup keys, scheduler state |
| Messenger | `messenger_messages` (auto_setup) | Doctrine transport `async`, `failed` queue | in-memory in test |
| Uploads | `var/import/` | `ImportUploadStorage` | container-local volume in dev |
| Domain entities | none | `src/Entity/` empty | roadmap S-01..S-04 not started |

## Async and scheduled behaviour

`src/Schedule.php` (`#[AsSchedule]`, stateful in cache, `processOnlyLastMissedRun`) emits
`Message\Heartbeat` hourly; `MessageHandler\HeartbeatHandler` logs it. No routing to `async` is
configured, so handlers run synchronously unless routed (`config/packages/messenger.yaml`).

## Frontend architecture (Observed)

- One delegated click listener maps `data-action`/`data-payload` to intents; `[data-controller]`
  mounts registrars from `assets/controllers/` (`cardiogram`, `event-stream`, `modal`, `shell`,
  `upload`, `editor`). No framework, no bundler; AssetMapper importmap + `typescript:build`.
- Editors post block drops to `<editor-endpoint>/blocks` expecting server-rendered HTML
  (`assets/controllers/editor.ts`); **no such route exists** in `src/Controller` (Observed gap).
- CSS layers: `01-tokens`, `02-base`, `03-components`, `04-patterns`, `app.css`, `storybook.css`.
- Storybook: 257 stories in `config/storybook.php`, rendered from production templates.

## Security and trust boundaries

- No `security.yaml`, no CSRF on POST forms/JSON endpoints, no authorization; `PanelIdentity`
  docblock states it is deliberately not a security identity.
- `TRUSTED_PROXIES`/`TRUSTED_HOSTS` configured for the kivvi.click reverse proxy (`framework.yaml`).
- Mercure hub allows anonymous subscribers to any topic — cross-tenant read once topics carry real data.
- Caddy log filter redacts the `authorization` query parameter.

## Observability

Monolog default (no `monolog.yaml` in `config/packages/`) — logs to stderr through FrankenPHP;
Caddy access log with redaction; Docker healthcheck on Caddy admin metrics `:2019/metrics`.
No metrics, tracing, or error tracker (Unknown → none observed).

## Deployment shape

- Dev: `compose.yaml` + `compose.override.yaml`, bind-mount `./:/app`, `var/` anonymous volume,
  Xdebug, hot reload, ports `HTTP_PORT/HTTPS_PORT/HTTP3_PORT` (README uses 8080/8443).
- Prod on this host: `compose.yaml` + `compose.prod.yaml` with `--env-file .env.prod.docker`,
  plain HTTP on port 23456 behind an external TLS proxy for https://kivvi.click, dedicated
  `*_prod` volumes. **Both stacks share the compose project name `kivvi-click`** — observed running
  containers `kivvi-click-php-1`/`kivvi-click-worker-1` are the prod image; starting the dev
  override without `-p` would recreate them (Inferred from `docker compose ps`).

## Contradictions

See `risks-and-unknowns.md` (stale `AGENTS.md` "Current Shape", stale foundation docs, framework
decision under review, e2e port default).
<!-- END project-context-initializer:artifact -->
