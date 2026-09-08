<!-- BEGIN project-context-initializer:context -->
# Context: `frankenphp/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `conf.d/` rolled-up. Deployment/runtime boundary together with root `Dockerfile` and `compose*.yaml`.

## Files
- `Caddyfile` — `{$SERVER_NAME:localhost}` site; `root /app/public`; `encode zstd br gzip`; Mercure module with publisher/subscriber JWT from env, **`anonymous` + `subscriptions` enabled**; `vulcain`; log filter redacts `authorization` query param; `/assets/*` immutable 1-year cache; `@phpRoute` rewrites everything except `/.well-known/mercure*` and real files to `index.php`; FrankenPHP worker on `./public/index.php`; `file_server hide *.php`.
- `docker-entrypoint.sh` — installs vendors if empty; waits up to 60 s for DB (`dbal:run-sql "SELECT 1"`); runs `doctrine:migrations:migrate --all-or-nothing` when `AUTO_MIGRATE` != 0; in dev builds TS if `var/typescript/assets/app.js` missing; then `docker-php-entrypoint`.
- `conf.d/10-app.ini` (opcache, realpath cache, strict sessions, UTC), `20-app.dev.ini` (Xdebug host), `20-app.prod.ini` (opcache preload `config/preload.php`, no timestamp validation).

## Environment names consumed
`SERVER_NAME`, `CADDY_GLOBAL_OPTIONS`, `CADDY_EXTRA_CONFIG`, `CADDY_SERVER_LOG_OPTIONS`, `CADDY_SERVER_EXTRA_DIRECTIVES`, `FRANKENPHP_CONFIG`, `FRANKENPHP_SITE_CONFIG`, `FRANKENPHP_WORKER_CONFIG`, `MERCURE_PUBLISHER_JWT_KEY`, `MERCURE_SUBSCRIBER_JWT_KEY`, `MERCURE_*_JWT_ALG`, `MERCURE_EXTRA_DIRECTIVES`, `AUTO_MIGRATE`, `APP_ENV`.

## Prod specifics (root files)
Prod: `SERVER_NAME=:80` (plain HTTP), TLS at external proxy, port 23456, `www-data` user, `debian:13-slim` runtime with copied binaries, one-shot `migrations` service, `*_prod` volumes. Dev: bind-mount, hot reload, Xdebug, dev image runs as root (leaves root-owned files: `vendor`, `var`, caches observed).

## Invariants
- Mercure is the Caddy module — never a separate hub container.
- Exactly one container runs migrations (`AUTO_MIGRATE`), worker never.
- Proxy must forward `X-Forwarded-Proto/Host/For` and not buffer SSE.

## Risks
Anonymous Mercure subscription (R4); shared compose project with running prod (R5); Postgres version fallback mismatch (technology notes); external proxy config not in repo.

## Evidence
`frankenphp/*`, `Dockerfile`, `compose.yaml`, `compose.override.yaml`, `compose.prod.yaml`, `README.md` Production section.
<!-- END project-context-initializer:context -->
