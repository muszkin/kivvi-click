# kivvi-click

Marketing-automation platform for e-commerce: track visitor events, configure rule-based
automations, and deliver popups / emails / coupons / product recommendations at the right
moment. This repository is a from-scratch rebuild of the original kivvi-click on a new stack —
migrated from Symfony/Twig on 2026-09-09, see `docs/adr` and `context/plans`.

See `CLAUDE.md` for architecture decisions and the `product-spec` skill (`.claude/skills/`) for
the full product vision.

## Stack

- Backend: Java 25, Spring Boot 4.1.1, Maven (wrapper `./mvnw`). Package root `click.kivvi`,
  layered `web` → `application` → `domain`, with `infrastructure` reachable only from
  `application` (enforced by `ArchitectureTest`).
- Frontend: Vue 3.5 SPA (Vite build, no SSR), history-mode vue-router, Pinia, vue-i18n. Pure
  CSS design system under `frontend/src/styles/`, byte-identical to the original. No JS/CSS
  frameworks (React, Svelte, Tailwind, Bootstrap).
- One deployable: the Spring Boot jar serves the built SPA from its own classpath
  (`backend/src/main/resources/static`); `backend/Dockerfile` builds both stages into one image.
- PostgreSQL 18 — the only backing service: application data, Spring Session JDBC sessions,
  `event_dedup` idempotency, ShedLock scheduling, all via Flyway-managed schema. No
  Redis/RabbitMQ/Kafka/Memcached.
- Real-time: the Mercure Hub (`dunglas/mercure`, embeds Caddy) is the stack's public edge —
  Server-Sent Events, not a separate WebSocket server.
- i18n: Polish by default, English available (`/pl`, `/en`), via vue-i18n.

## Requirements

- Docker Engine with the Compose plugin (`docker compose`, not the standalone v1
  `docker-compose`) — this is the only thing required for the commands below.
- For host-local backend work (outside Docker): JDK 25 (Temurin). On this host, set
  `export JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25` before running `./mvnw`.
- For host-local frontend work (outside Docker): Node 26+.

## Run locally

Production already runs on this host under the Compose project `kivvi-click` (see Production
below). Always give the dev stack its own project name — a bare `docker compose up` here
defaults to this directory's name (`kivvi-click`) and would collide with prod's containers and
volumes:

```bash
mkdir -p var/mail && chmod 777 var/mail   # first run only: the dev maildrop, see below
HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose -p kivvi-dev up -d --build --wait
```

The dev stack runs under the `dev` Spring profile, which writes every outgoing message to
`var/mail` as an `.eml` instead of contacting an SMTP relay. That directory is bind-mounted from
the host so the Playwright suite can read it, which means it has to exist before the stack starts
and be writable by the container's unprivileged `kivvi` user — hence the world-writable mode on a
git-ignored scratch directory that only ever holds development mail. Neither production path
mounts it. If the stack runs on a port other than 8443, set `KIVVI_BASE_URL` too, or the links
inside those messages will point at the wrong host.

Open https://localhost:8443/ (self-signed certificate in dev). This builds the `api` image
(Spring Boot jar with the Vue SPA baked into its classpath), starts `mercure` as the public
edge and `database` (Postgres 18, no fixed host port — find it with
`docker compose -p kivvi-dev port database 5432`).

## Common commands

```bash
docker compose -p kivvi-dev logs -f api                # follow app logs
docker compose -p kivvi-dev build api                   # rebuild after a Dockerfile/dependency change
cd backend && ./mvnw -q test                             # unit tests + ArchitectureTest, no containers
cd backend && ./mvnw -q verify                           # + Testcontainers Postgres 18 integration tests
cd backend && ./mvnw spotless:apply                      # format (google-java-format)
cd frontend && npm ci && npm run dev                      # Vite dev server
cd frontend && npm run format                             # format (Prettier)
```

## Panel

The whole panel is a Vue 3 SPA (`frontend/src/`) served by the Spring Boot API (`backend/`)
from its own classpath — one process, one deployable jar. Screens: landing, login, dashboard,
live event stream, customers + 360 profile, automations, email campaigns + template editor,
popups/widgets + widget editor, product feeds, the four-step customer import wizard, and eight
settings tabs. Components are organized atoms → molecules → organisms in
`frontend/src/components/`, ported 1:1 from the original design system; the CSS itself was
copied byte-for-byte into `frontend/src/styles/`.

Navigation between routes is always a full document request (plain `<a>` hrefs through
`useIntents`'s `navigate` intent), never `router.push` — this matches every transition the
original server-rendered app made, which the Playwright e2e suite still asserts against.

The live event stream subscribes to the Mercure Hub over Server-Sent Events; `POST /collect`
takes a tracked event, drops duplicates via the `event_dedup` table (24h TTL), and publishes it
for the SPA's live rows to pick up.

There is no component storybook route in this stack (`/_storybook` is intentionally excluded
from the route table).

## Tests

Backend:

```bash
cd backend
./mvnw -q test      # unit tests + ArchitectureTest, no containers
./mvnw -q verify     # + Testcontainers Postgres 18 integration tests (*IT.java), needs Docker
```

Frontend:

```bash
cd frontend
npm ci
npm run test -- --run              # Vitest unit
npm run test:integration -- --run  # Vitest integration
npm run lint
npm run typecheck
npm run format:check
```

End-to-end tests drive the running stack with Playwright, headless Chrome — unchanged across
the rewrite, this suite is the migration's parity oracle:

```bash
cd tests/e2e && npm install                              # first run only
E2E_BASE_URL=https://localhost:8443 npx playwright test  # default base is https://localhost:8543
```

`events.spec.ts`, `dashboard.spec.ts` and `navigation.spec.ts` share live/SSE state and should
run serialized: add `--workers=1` when running them. The e2e suite has its own `package.json`,
independent of `frontend/`'s.

`waitlist.spec.ts` reads the confirmation link out of `var/mail` — set `E2E_MAILDROP` if the stack
writes somewhere else. It also spends 6 of the hourly 10 signups allowed per IP, so a second run
inside the hour starts seeing 429s; bring the stack up with `down -v` first and the counter starts
empty.

Migration parity/regression check (optional; replays the pre-migration oracle capture):

```bash
node tools/migration-verify/compare.mjs --journey <id> --base <url> --dimension all
```

## Production

Production uses `compose.prod.yaml` layered on `compose.yaml` (built image, no bind-mount):
`api` builds from `backend/Dockerfile` (Node stage → Maven stage → JRE runtime, one jar serving
the SPA), `mercure` is the public edge, `database` is Postgres 18 on its own volume.

TLS is **not** terminated here. The `mercure` edge serves plain HTTP on the published port and
a reverse proxy in front of the stack answers for https://kivvi.click, forwarding
`X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-For`. Spring reads them natively
(`server.forward-headers-strategy: native`, see `backend/src/main/resources/application.yml`);
no explicit trusted-proxy CIDR list is needed because `api` publishes no host port of its own —
the Mercure edge is the only path in.

> **Where production actually reads its configuration.** The live stack is deployed by Portainer
> from `compose.portainer.yaml` (see CLAUDE.md), and its values come from that stack's own
> environment variables — not from `.env.prod.docker`, which now only serves a local
> production-shaped run through the `compose.yaml + compose.prod.yaml` overlay. Three files spell
> the same variable names on purpose — `compose.portainer.yaml`, `compose.prod.yaml` and
> `.env.prod.docker.example`. A variable added to one belongs in all three, under the same name.

Runtime configuration and secrets live in `.env.prod.docker` (git- and docker-ignored; copy
from `.env.prod.docker.example`): `SERVER_NAME`, `HTTP_PORT`, `POSTGRES_DB`/`POSTGRES_USER`/
`POSTGRES_PASSWORD` (shared by `database` and `api`), `MERCURE_JWT_SECRET` (derives all three
Mercure JWT env vars for both `api` and `mercure`), the transactional-mail settings
`SMTP_HOST`/`SMTP_PORT`/`SMTP_USERNAME`/`SMTP_PASSWORD` plus `KIVVI_BASE_URL`, `MAIL_FROM` and
`KIVVI_UNSUBSCRIBE_SECRET`, and an optional `IMAGES_PREFIX` for the built image tag.

Mail goes out over Brevo's plain SMTP relay; the password is Brevo's **SMTP key**, not the account
password and not an API v3 key. Two of these stop the application from starting if they are
missing, deliberately — not starting is the honest failure, where starting up and silently dropping
every confirmation is not:

- `SMTP_HOST`, outside the `dev` profile, because there would be nowhere to send — and
  `SMTP_USERNAME`/`SMTP_PASSWORD` with it whenever the relay wants authentication, which Brevo
  does. The host has a working default, so a half-configured stack never shows up as a missing
  host: it shows up as a relay that answers and rejects every message at AUTH, which is why the
  credentials are the pair actually worth refusing over.
- `KIVVI_UNSUBSCRIBE_SECRET`, at least 32 characters, because every unsubscribe link is an HMAC of
  the subscriber's id and those ids are sequential: a missing or guessable secret is a list anyone
  can unsubscribe. Rotating it invalidates the links in messages already delivered.

Under the `dev` profile (`compose.yaml`'s default) no relay is contacted at all: every message is
written to `./var/mail` as an `.eml`, which is what lets the Playwright suite follow a real
confirmation link. Both production paths pin `SPRING_PROFILES_ACTIVE=prod` and neither mounts it.

```bash
docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --build --wait
```

Run this **from this directory only** — the Compose project name (`-p kivvi-click`) must match
the checkout's own directory name; a git worktree has a different directory name and would
start a second, colliding stack rather than update this one.

The app is then reachable on http://localhost:23456 and, through the proxy, on
https://kivvi.click. Example nginx server block:

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

## Production operations

Redeploy (pull latest, rebuild, restart in place):

```bash
git pull
docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --build --wait
docker compose -p kivvi-click -f compose.yaml -f compose.prod.yaml ps   # expect all services healthy
curl -I http://localhost:23456/pl
```

Roll back to a previous known-good commit (plain git + redeploy — there is no second stack to
fall back to any more):

```bash
git checkout <previous-good-sha>
docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --build --wait
```

Smoke-test against the public edge after either operation:

```bash
cd tests/e2e && E2E_BASE_URL=https://kivvi.click npx playwright test
```

The database volume persists across redeploys; Flyway applies only new, forward-only
migrations on top of it — never edit `V1__baseline.sql`. `GET /actuator/health` is deliberately
not reachable through the public edge (`mercure/Caddyfile` returns 404 for `/actuator/*`); use
`docker compose -p kivvi-click -f compose.yaml -f compose.prod.yaml ps` or a real route like
`/pl` to check health from outside the host.

Full history of the cutover itself (the rollback rehearsal, the CUT-1 attempts and their
outages, and the decision records) is preserved in
`context/plans/2026-09-08-symfony-to-spring-vue-migration.md` and `docs/adr/` — this section
only covers day-to-day operation of the stack that is live now.
