# kivvi-click

Marketing automation for e-commerce: track what visitors do on your shop, build rules from those
events, and deliver popups, e-mails, coupons and product recommendations at the moment they are
worth sending.

The whole thing is open source under the [MIT licence](LICENSE). Clone it, build it, run it on
your own infrastructure — there is no hosted plan to buy and nothing is gated behind a licence key.
If you would rather not run it yourself, see [Getting it deployed](#getting-it-deployed).

> **Status:** the public site and the waitlist/contact flow are production code. The panel behind
> them (dashboard, event stream, customers, automations, campaigns, popups, feeds, import,
> settings) is a complete, navigable UI built against fixture data — the screens and the design
> system are real, the domain persistence behind most of them is not written yet. Treat this as an
> early-stage project you can run and read, not a finished product you can migrate a shop onto.

## What is in the box

- **Event collection** — `POST /collect` takes a tracked event, drops duplicates through a
  24-hour idempotency table, and publishes it for the live stream.
- **Live event stream** — the browser subscribes over Server-Sent Events, so the dashboard shows
  events as they land instead of polling.
- **The panel** — landing, login, dashboard, event stream, customers and a 360 profile,
  automations, e-mail campaigns with a template editor, popups and widgets with an editor,
  product feeds, a four-step customer import wizard, and settings.
- **Two languages** — Polish by default, English available; every route is prefixed `/pl` or
  `/en`.

## Stack

| Layer | Choice |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven (wrapper included — no local Maven needed) |
| Frontend | Vue 3.5 SPA, Vite, vue-router, Pinia, vue-i18n; a hand-written CSS design system, no UI framework |
| Database | PostgreSQL 18 — the only backing service |
| Real-time | Mercure Hub (Server-Sent Events), which also fronts the stack as its HTTP edge |
| Packaging | One image: the Spring Boot jar serves the built SPA from its own classpath |

There is deliberately no Redis, RabbitMQ, Kafka or Memcached. Sessions, scheduling locks and event
de-duplication all live in Postgres. Schema changes are Flyway migrations.

## Requirements

Docker Engine with the Compose plugin (`docker compose`, not the old standalone `docker-compose`).
That is all you need to run it.

To work on the code outside containers you will also want JDK 25 and Node 26+.

## Run it

```bash
git clone https://github.com/muszkin/kivvi-click.git
cd kivvi-click
mkdir -p var/mail && chmod 777 var/mail   # first run only — see "Mail in development"
docker compose up -d --build --wait
```

Then open <https://localhost:8443/> and accept the self-signed certificate.

The first build compiles the SPA and the jar, so it takes a few minutes. After that:

```bash
docker compose logs -f api     # follow application logs
docker compose build api       # rebuild after a dependency or Dockerfile change
docker compose down -v         # stop and wipe the database volume
```

Ports are configurable if 8443 is taken:

```bash
HTTP_PORT=8544 HTTPS_PORT=8543 HTTP3_PORT=8543 docker compose up -d --build --wait
```

If you move off 8443, set `KIVVI_BASE_URL` to the URL you actually serve on — otherwise the links
inside confirmation e-mails will point at the wrong host.

Postgres is not published on a fixed host port. Find it with `docker compose port database 5432`.

### Mail in development

The default `dev` profile never contacts an SMTP relay. Every outgoing message is written to
`./var/mail` as an `.eml` file, which is what lets you (and the end-to-end suite) follow a real
confirmation link without a mail account. The directory is bind-mounted, so it has to exist and be
writable by the container before the stack starts — hence the `chmod 777` on a git-ignored scratch
directory that only ever holds development mail.

## Configuration

Everything is environment variables; nothing is compiled in.

| Variable | Needed when | What it does |
| --- | --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | always | Database name and credentials, shared by `database` and `api` |
| `MERCURE_JWT_SECRET` | always | Signs the Mercure publish/subscribe tokens |
| `SERVER_NAME`, `HTTP_PORT` | deployment | What the edge answers on |
| `KIVVI_BASE_URL` | deployment | Absolute base URL used to build links inside e-mails |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | outside `dev` | Any SMTP relay. The application refuses to start without these rather than silently dropping mail |
| `MAIL_FROM` | outside `dev` | Envelope sender |
| `KIVVI_UNSUBSCRIBE_SECRET` | outside `dev` | At least 32 characters. Every unsubscribe link is an HMAC of a sequential subscriber id, so a missing or guessable secret means anyone can unsubscribe anyone. Rotating it invalidates links in messages already sent |

Copy `.env.prod.docker.example` as a starting point for a real deployment.

## Deploying it

`compose.prod.yaml` layers a built-image, no-bind-mount configuration over `compose.yaml`:

```bash
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --build --wait
```

The stack does **not** terminate TLS. The Mercure edge serves plain HTTP on the published port and
expects a reverse proxy in front of it, forwarding `X-Forwarded-Proto`, `X-Forwarded-Host` and
`X-Forwarded-For`. Spring reads those headers natively.

One thing to get right: the live event stream is Server-Sent Events, so your proxy must not buffer
it. With nginx:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host              $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host  $host;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;

    proxy_http_version 1.1;
    proxy_buffering    off;
    proxy_read_timeout 24h;
}
```

With Caddy, `reverse_proxy` plus `flush_interval -1` does the same job.

The database volume survives redeploys and Flyway applies only new, forward-only migrations on top
of it. `GET /actuator/health` is intentionally not exposed through the public edge.

## Tests

Backend — unit tests and the ArchUnit module-boundary check need nothing but a JDK; `verify` adds
integration tests that start a real Postgres 18 through Testcontainers, so it needs Docker:

```bash
cd backend
./mvnw test      # fast, no containers
./mvnw verify    # + *IT.java integration tests
```

Frontend:

```bash
cd frontend
npm ci
npm run test -- --run              # unit
npm run test:integration -- --run  # integration
npm run lint && npm run typecheck && npm run format:check
```

End-to-end, with Playwright against a running stack in headless Chrome:

```bash
cd tests/e2e
npm install                                              # first run only
E2E_BASE_URL=https://localhost:8443 npx playwright test --workers=1
```

Run it single-worker: several specs share live/SSE state and are flaky in parallel. The suite keeps
its own `package.json` so its Playwright version moves independently of the frontend's toolchain.

`waitlist.spec.ts` reads confirmation links out of `var/mail` (override with `E2E_MAILDROP`). It
also spends part of the hourly per-IP signup allowance, so a second run within the hour starts
seeing HTTP 429 — `docker compose down -v` first and the counter starts empty.

## Formatting

```bash
cd backend && ./mvnw spotless:apply   # google-java-format
cd frontend && npm run format          # Prettier
```

CI runs the same checks on every pull request.

## Project layout

```
backend/    Spring Boot API; package root click.kivvi, layered web -> application -> domain,
            with infrastructure reachable only from application (enforced by ArchitectureTest)
frontend/   Vue 3 SPA; components as atoms -> molecules -> organisms, CSS design system in styles/
mercure/    Mercure Hub / Caddy edge configuration
tests/e2e/  Playwright end-to-end suite
docs/adr/   Architecture decision records
context/    Specifications, plans and migration history
```

One convention worth knowing before you read the frontend: navigation between routes is a full
document request through plain `<a>` hrefs, not `router.push`. That is deliberate and the
end-to-end suite asserts it.

## History

This is a from-scratch rebuild of the original kivvi-click. It ran on Symfony and Twig until
September 2026, when it was migrated to the Spring Boot and Vue stack described above. The
decision records are in `docs/adr/`; the full migration plan and its evidence are under
`context/`.

## Getting it deployed

If you want kivvi-click running but would rather not do it yourself, leave your address on the
site and we will get in touch about deploying it, integrating it with your shop and keeping it
running. That is a paid service; the software itself stays free and MIT-licensed either way.

## Licence

MIT — see [LICENSE](LICENSE). Do what you like with it, including commercially; keep the copyright
notice and understand there is no warranty.
