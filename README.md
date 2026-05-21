# kivvi-click

Marketing-automation platform for e-commerce: track visitor events, configure rule-based
automations, and deliver popups / emails / coupons / product recommendations at the right
moment. This repository is a from-scratch rebuild of the original kivvi-click on a new stack.

See `CLAUDE.md` for architecture decisions and the `product-spec` skill (`.claude/skills/`) for
the full product vision.

## Stack

- PHP 8.5, Symfony 8 — single full-stack app
- FrankenPHP + Caddy runtime (Mercure hub built into Caddy), Docker-first via
  [dunglas/symfony-docker](https://github.com/dunglas/symfony-docker)
- PostgreSQL 18 — backs everything: application data, sessions, cache (Doctrine DBAL adapter),
  Messenger transport (Doctrine), and scheduling. No Redis/RabbitMQ/Memcached.
- Frontend: server-rendered Twig + pure CSS + vanilla TypeScript (AssetMapper). No JS/CSS frameworks.
- i18n: Polish by default, English available (`/`, `/pl`, `/en`)

## Run locally

```bash
docker compose build
HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose up -d --wait
```

Open https://localhost:8443/ (self-signed certificate in dev).

This also starts a `worker` container that runs `messenger:consume async scheduler_default`,
so async (Doctrine transport) messages are processed and scheduled tasks fire automatically.
Migrations run on the `php` container's start; the `worker` skips them (`AUTO_MIGRATE=0`).

## Common commands

```bash
docker compose exec php php bin/console <command>   # Symfony console
docker compose exec php php bin/phpunit              # run tests
docker compose exec php composer require <package>   # add a dependency
docker compose logs -f php                           # follow app logs
```

## Tests

```bash
docker compose exec php php bin/phpunit
```

## Production

Production uses `compose.prod.yaml` (built prod image, no bind-mount). A one-shot `migrations`
job applies migrations once and exits; `php` and `worker` wait for it to succeed
(`service_completed_successfully`) and run with `AUTO_MIGRATE=0`, so no two containers race on
migrations.

```bash
export APP_SECRET=... CADDY_MERCURE_JWT_SECRET=...
docker compose -f compose.yaml -f compose.prod.yaml build
docker compose -f compose.yaml -f compose.prod.yaml up -d --wait
```

