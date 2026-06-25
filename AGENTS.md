# AGENTS.md

Guidance for Codex when working in this repository.

## Product Context

This project is `kivvi-click`, a from-scratch rebuild of the original kivvi-click product.
It is a multi-tenant SaaS marketing-automation platform for e-commerce sites and consumer
websites: track visitor behavior, configure rule-based automations, and deliver popups,
emails, coupons, banners, and product recommendations at the right moment.

Use the local product spec in `.claude/skills/product-spec/SKILL.md` when planning or
building features, deciding scope, or answering domain-model questions. Reuse the product
intent from the original project, not its old tech stack.

Core product concepts include:

- Account: company tenant using the platform.
- User: authenticated user within an account.
- Webpage: tracked site or URL under an account.
- AccountEvent / tracking event: raw visitor event with event type, customer id, event time,
  custom data, and idempotency id.
- Customer / Visitor: profile derived from tracked events.
- Email template and email campaign.
- Widget: popup, banner, web layer, recommendation carousel.
- Coupon.
- Rule / Automation: conditions mapping events to actions.

Important product constraints:

- Tracking script should stay lightweight, about 2KB minified, with no external dependencies.
- Tracking events must be idempotent and deduplicated by idempotency id.
- Multi-tenancy is fundamental.
- Default language is Polish; English is optional.
- Free and Pro tiers are expected, with ML features reserved for Pro.

## Stack

- PHP 8.5, Symfony 8, Composer.
- Single full-stack app.
- Runtime: FrankenPHP + Caddy, developed Docker-first via dunglas/symfony-docker.
- Database: PostgreSQL is the only backing service.
- Frontend: server-rendered Twig, pure CSS, vanilla TypeScript through AssetMapper.
- No React, Vue, Svelte, Tailwind, Bootstrap, npm UI libraries, Redis, RabbitMQ, or Memcached.

Postgres backs:

- Application data.
- Sessions.
- Cache via Postgres `UNLOGGED TABLE` and connection pooling.
- Messenger through Doctrine transport.
- Scheduling through Symfony Scheduler or `pg_cron`.

Real-time live event dashboard work should use Mercure Hub built into Caddy.

## Development Commands

Run commands Docker-first inside the `php` container.

Start locally:

```bash
HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose up -d --wait
```

Open the app at `https://localhost:8443/`.

Common commands:

```bash
docker compose exec php php bin/console <command>
docker compose exec php php bin/phpunit
docker compose exec php composer require <package>
docker compose logs -f php
```

If Dockerfile or PHP extension setup changes:

```bash
docker compose build php
```

Test database setup, when needed:

```bash
docker compose exec php php bin/console --env=test doctrine:database:create
docker compose exec php php bin/console --env=test doctrine:migrations:migrate
```

Formatting:

```bash
docker compose exec php vendor/bin/php-cs-fixer fix
npm run format
```

## Current Shape

The app currently has a localized homepage:

- `src/Controller/HomeController.php`
- `templates/home/index.html.twig`
- `translations/messages.pl.yaml`
- `translations/messages.en.yaml`

Existing tests cover the homepage locale behavior and cache behavior:

- `tests/Controller/HomeControllerTest.php`
- `tests/CacheTest.php`

## Workflow

- Trunk-based development with short-lived branches merged frequently to `main`.
- Issue tracking uses GitHub Issues. Reference issues as `#NN`.
- Commits use Conventional Commits in English, for example:

```text
feat: add event ingestion (#42)
```

Do not use Jira keys or `CORE-123`-style scopes.

## Local Claude Files

The `.claude` directory is part of the project context:

- `.claude/skills/product-spec/SKILL.md` is the authoritative product/domain spec.
- `.claude/settings.json` formats edited PHP and TypeScript files after Claude tool edits.

Codex does not automatically run those Claude hooks, so format edited PHP and frontend files
explicitly when making changes.
