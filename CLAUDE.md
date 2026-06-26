# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

kivvi-click-10x is a from-scratch rebuild of the kivvi-click product on a new stack. It is a
multi-tenant SaaS marketing-automation platform for e-commerce sites: track visitor events,
configure rule-based automations, and deliver popups / emails / coupons / product
recommendations at the right moment in the customer journey.

Full product vision, feature list, domain entities, and assumptions live in the `product-spec`
skill — invoke it when planning or building features. Carry over the product goals from the
original, NOT its tech stack.

## Stack — single full-stack app

- PHP 8.5, Symfony 8, Composer. Always use latest stable versions.
- Runtime: FrankenPHP + Caddy. Develop docker-first via dunglas/symfony-docker
  (https://github.com/dunglas/symfony-docker). Do not assume a local PHP/Nginx setup.
- Database: PostgreSQL — the ONLY backing service (see Architecture decisions).
- Frontend: server-rendered Twig + pure CSS + vanilla TypeScript. NO frontend frameworks
  (React/Vue/Svelte) and NO CSS frameworks (Tailwind/Bootstrap). Do not pull in npm UI libs.

## Architecture decisions — do not substitute the usual defaults

Postgres backs everything; there is no Redis, RabbitMQ, or Memcached.

- Sessions: stored in Postgres.
- Cache: Postgres `UNLOGGED TABLE` + connection pooling.
- Messenger: Doctrine transport on Postgres with `auto_setup` (`messenger_dsn` = doctrine).
- Scheduler: Symfony Scheduler, or the `pg_cron` Postgres extension.
- Real-time (live event dashboard): Mercure Hub built into Caddy — not a separate WebSocket server.
- i18n: default language Polish; English is an optional toggle. Build strings PL-first with EN translations.

## Commands

Docker-first — run everything inside the `php` container.

- Start: `HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose up -d --wait` → https://localhost:8443
- Rebuild image after a Dockerfile/extension change: `docker compose build php` (the doctrine recipe adds `pdo_pgsql` there — rebuild if you see "could not find driver")
- Console: `docker compose exec php php bin/console <cmd>`
- Tests: `docker compose exec php composer test`. The script creates the `app_test` database if missing, builds TypeScript assets for AssetMapper, then runs PHPUnit. If migrations are added later, migrate the test DB with `php bin/console --env=test doctrine:migrations:migrate`.
- Format: `vendor/bin/php-cs-fixer fix` (PHP) and `yarn format` (prettier, TS/CSS).
- Static analysis: `docker compose exec php composer phpstan` and `yarn typecheck`.
- CI/CD: GitHub Actions builds the production Docker image on push to `main`.
- Run the scheduler: `php bin/console messenger:consume scheduler_default`.

## Workflow — overrides the global gitflow/Jira rules for this repo

- Trunk-based development: short-lived branches merged frequently to `main`; no long-lived release branches.
- Issue tracking: GitHub Issues (no Jira). Reference issues as `#NN`.
- Commits: Conventional Commits in English, referencing the issue, e.g. `feat: add event ingestion (#42)`.
  No Jira keys, no `CORE-123`-style scopes.
