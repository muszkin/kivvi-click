<!-- BEGIN project-context-initializer:artifact -->
# Technology

Columns: declared (manifest range), locked (lockfile), detected (usage), verified (executed in
this run). Decision state `Decided` = documented in `CLAUDE.md`/`AGENTS.md`; nothing was run.

| Technology | Role | Declared | Locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PHP | Language | `>=8.5` (`composer.json`) | image `dunglas/frankenphp:1-php8.5` (`Dockerfile`) | `src/**` | Observed | Decided (under review) | not-run |
| Symfony | Framework | `8.0.*` | framework-bundle v8.0.11, messenger v8.0.12, runtime v8.0.12 | `config/**`, `src/Kernel.php` | Observed | Decided (under review) | not-run |
| FrankenPHP + Caddy | Runtime, worker mode, Mercure hub, Vulcain | `Dockerfile`, `frankenphp/Caddyfile` | image tag `1-php8.5` (floating minor) | compose services `php`, `worker` | Observed | Decided | not-run |
| PostgreSQL | Only backing service: data, sessions, cache, messenger, scheduler state | `POSTGRES_VERSION=18` (`.env`, `.env.example`) | image `postgres:${POSTGRES_VERSION:-16}-alpine` (`compose.yaml`) | `config/packages/{doctrine,cache,messenger,framework}.yaml`, `migrations/` | Observed | Decided | not-run |
| Doctrine ORM / DBAL / Migrations | Persistence | orm `^3.6`, doctrine-bundle `^3.2`, migrations-bundle `^4.0` | orm 3.6.6, dbal 4.4.3, migrations-bundle 4.0.0 | no entities yet; one infra migration | Observed | Decided | not-run |
| Symfony Messenger (Doctrine transport) | Async queue | `8.0.*` | v8.0.12 | `config/packages/messenger.yaml`, `compose.yaml` worker | Observed | Decided | not-run |
| Symfony Scheduler | Cron-like jobs | `8.0.*` | v8.0.11 | `src/Schedule.php` (hourly `Heartbeat`) | Observed | Decided | not-run |
| Mercure (bundle + Caddy module) | Real-time SSE | mercure-bundle `^0.4.2` | v0.4.2 | `src/Tracking/EventIngestion.php`, `assets/controllers/event-stream.ts`, `Caddyfile` (`anonymous`, `subscriptions`) | Observed | Decided | not-run |
| Twig + twig/extra-bundle | Server rendering | `^3.27.1` | twig v3.27.1 | `templates/**` (atoms/molecules/organisms/pages) | Observed | Decided (under review) | not-run |
| Symfony AssetMapper + sensiolabs/typescript-bundle | Asset pipeline, TS compile | asset-mapper `8.0.*`, typescript-bundle `^0.2.2` | asset-mapper v8.0.11 | `importmap.php`, `frankenphp/docker-entrypoint.sh` (`typescript:build`) | Observed | Decided | not-run |
| TypeScript | Frontend language (no framework) | `^6.0.3` (`package.json`) | 6.0.3 (`yarn.lock`) | `assets/app.ts`, `assets/controllers/*.ts` | Observed | Decided (under review) | not-run |
| Yarn | JS package manager, PnP | `yarn@4.14.1` | `.pnp.cjs` committed | root only; e2e uses npm | Observed | Decided | not-run |
| Prettier | TS/CSS formatting | `^3.8.3` | 3.8.4 | `yarn format`; no `.prettierrc` | Observed | N/A | not-run |
| PHP-CS-Fixer | PHP formatting | `^3.95` | v3.95.2 | `.php-cs-fixer.dist.php` (`@Symfony`, unsupported-PHP allowed) | Observed | N/A | not-run |
| PHPStan + phpstan-symfony | Static analysis level 5 | `^2.2` / `^2.0` | 2.2.2 | `phpstan.neon.dist` (needs warmed dev container) | Observed | N/A | not-run |
| PHPUnit | Unit + functional tests | `^13.1` | 13.1.10 | `tests/**`, `phpunit.dist.xml` (fail on deprecation/notice/warning) | Observed | N/A | not-run |
| Playwright | E2E, headless system Chrome | `^1.56.0` (`tests/e2e/package.json`) | 1.62.1 (`tests/e2e/package-lock.json`) | `tests/e2e/specs/*.spec.ts` | Observed | N/A | not-run |
| Docker Compose | Dev/prod orchestration | `compose.yaml` + `compose.override.yaml` (dev) / `compose.prod.yaml` | — | running prod containers observed via `docker compose ps` | Observed | Decided | verified (ps only) |
| GitHub Actions | CI: build prod image on push to `main` | `.github/workflows/docker-build.yml` | actions checkout@v4, buildx@v3, build-push@v6 | push only, `push: false` | Observed | Decided | not-run |
| Google Fonts (Geist, Geist Mono, Instrument Serif) | Typography via external stylesheet | `templates/layout/base.html.twig` | — | runtime dependency on fonts.googleapis.com | Observed | N/A | not-run |
| frankenphp-hot-reload + idiomorph (jsDelivr) | Dev hot reload | `templates/base.html.twig` (legacy base, only when `FRANKENPHP_HOT_RELOAD`) | — | dev only | Observed | N/A | not-run |

## Compatibility notes

- `composer.json` replaces all `symfony/polyfill-php7x/8x`; PHP < 8.5 cannot run the app.
- PHP-CS-Fixer runs with `setUnsupportedPhpVersionAllowed(true)` because PHP 8.5 is newer than its
  supported range — rules may misbehave on new syntax (Inferred).
- `compose.yaml` falls back to Postgres 15/16 when `POSTGRES_VERSION` is unset; `.env` pins 18 and
  the volume is mounted at `/var/lib/postgresql` (Postgres 18 layout). A mismatch is a startup
  failure (`compose.yaml` comment, memory note).
- Prod image is `debian:13-slim` with binaries copied from the builder; runs as `www-data`.
- The e2e suite is a separate npm project so the root stays on Yarn PnP (`tests/e2e/package.json`).

## Configuration key names (values not recorded)

`.env`: `APP_ENV`, `APP_SECRET`, `APP_SHARE_DIR`, `DEFAULT_URI`, `TRUSTED_PROXIES`, `TRUSTED_HOSTS`,
`DATABASE_URL`, `POSTGRES_VERSION`, `MESSENGER_TRANSPORT_DSN`, `MERCURE_URL`, `MERCURE_PUBLIC_URL`,
`MERCURE_JWT_SECRET`.
`.env.prod.docker.example` (prod compose env): `SERVER_NAME`, `HTTP_PORT`, `HTTPS_PORT`, `HTTP3_PORT`,
`CADDY_MERCURE_PUBLIC_URL`, `CADDY_MERCURE_JWT_SECRET`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
Compose-only: `IMAGES_PREFIX`, `XDEBUG_MODE`, `AUTO_MIGRATE`, `FRANKENPHP_WORKER_CONFIG`, `MERCURE_EXTRA_DIRECTIVES`.
Secret-bearing files present locally and ignored, not read: `.env.local`, `.env.prod.docker`, `.ai/cezar/launch-key`.
<!-- END project-context-initializer:artifact -->
