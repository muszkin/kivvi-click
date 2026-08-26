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

Open https://localhost:8443/ (self-signed certificate in dev). The container compiles the
TypeScript bundle on start, so a fresh `var/` needs no extra step.

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

## Panel

The whole panel is server-rendered from `templates/pages/` and composed of the design system in
`templates/components/` (atoms → molecules → organisms). Screens: landing, login, dashboard,
event stream, customers + 360 profile, automations + rule editor (list and diagram), email
campaigns + template editor, popups/widgets + widget editor, product feeds, the four-step
customer import and eight settings tabs.

Interaction is declared, never wired by hand: one delegated listener in `assets/app.ts` turns
`data-action` / `data-payload` attributes into named intents, and `[data-controller]` mounts the
TypeScript controllers in `assets/controllers/`.

The live event stream subscribes to Mercure; `POST /collect` takes a tracked event, drops
duplicates by `idempotency_id` and publishes the **server-rendered row**, so the markup of a row
exists in exactly one place.

### Component storybook

Every component renders from its production Twig template at https://localhost:8443/_storybook
(dev and test environments only). Stories live in `config/storybook.php` as plain data.

## Tests

```bash
docker compose exec php php bin/phpunit          # functional + unit
docker compose exec php composer phpstan         # static analysis
```

End-to-end tests drive the running stack with Playwright, headless, using the system Chrome:

```bash
cd tests/e2e && npm install                      # first run only
npx playwright test                              # against https://localhost:8543
E2E_BASE_URL=https://localhost:8443 npx playwright test
```

The e2e suite has its own `package.json` so the app's yarn/PnP setup stays untouched.

## Production

Production uses `compose.prod.yaml` (built prod image, no bind-mount). A one-shot `migrations`
job applies migrations once and exits; `php` and `worker` wait for it to succeed
(`service_completed_successfully`) and run with `AUTO_MIGRATE=0`, so no two containers race on
migrations.

TLS is **not** terminated here. Caddy serves plain HTTP on the published port and a reverse proxy
in front of the stack answers for https://kivvi.click. The proxy must forward `X-Forwarded-Proto`,
`X-Forwarded-Host` and `X-Forwarded-For` — Symfony trusts them (`TRUSTED_PROXIES`, default
`private_ranges`) and generates `https://kivvi.click/...` URLs and secure session cookies from
them. `TRUSTED_HOSTS` is the allow-list of names the app answers for.

Runtime configuration and secrets live in `.env.prod.docker` (git- and docker-ignored):
`APP_SECRET`, `CADDY_MERCURE_JWT_SECRET`, `POSTGRES_PASSWORD`, the published port and the public
address. Copy `.env.prod.docker.example`, fill in the secrets, then:

```bash
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml build
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait
```

The app is then reachable on http://localhost:23456 and, through the proxy,
on https://kivvi.click. Example nginx server block:

```nginx
server {
    server_name kivvi.click;
    listen 443 ssl http2;

    location / {
        proxy_pass http://127.0.0.1:23456;
        proxy_set_header Host              $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host  $host;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;

        # The live event stream is Server-Sent Events: no buffering, no read timeout.
        proxy_http_version 1.1;
        proxy_buffering    off;
        proxy_read_timeout 24h;
    }
}
```

Caddy in front instead:

```caddyfile
kivvi.click {
	reverse_proxy 127.0.0.1:23456 {
		flush_interval -1   # stream SSE straight through
	}
}
```
