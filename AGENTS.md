# AGENTS.md

Guidance for Codex when working in this repository.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

Before repository-wide research, implementation planning, implementation orchestration, review, or implementation, read `context/map/INDEX.md`. For work in a specific directory, follow its mapping to the nearest `.agents/project-context.md`. Treat generated context as navigation evidence and verify stale or high-risk claims against current code, runtime, and canonical documentation.
<!-- END project-context-initializer:router -->

## Product Context

This project is `kivvi-click`, a from-scratch rebuild of the original kivvi-click product.
It is open-source marketing-automation software for e-commerce sites and consumer websites,
MIT-licensed and run on your own infrastructure, with many accounts on one instance: track
visitor behavior, configure rule-based automations, and deliver popups, emails, coupons,
banners, and product recommendations at the right moment.

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
- Default language is English; Polish is a full, equal language behind `/pl`. The privacy
  policy's Polish text stays the binding version.
- Free and Pro tiers are expected, with ML features reserved for Pro.

## Stack

- Backend: Java 25, Spring Boot 4.1.1, Maven (wrapper `./mvnw`).
- Frontend: Vue 3.5 SPA (Vite, no SSR), history-mode vue-router, Pinia, vue-i18n.
- One deployable: the Spring Boot jar serves the built SPA from its own classpath
  (`backend/src/main/resources/static`); `backend/Dockerfile` builds both in one multi-stage
  image, which is what `compose.yaml` runs.
- Database: PostgreSQL 18 is the only backing service.
- Frontend styling: pure CSS design system under `frontend/src/styles/`, byte-identical to the
  original. No React, Svelte, Tailwind, Bootstrap, or other npm UI libraries. No Redis,
  RabbitMQ, Kafka, or Memcached.

Postgres backs:

- Application data.
- Sessions, via Spring Session JDBC (`spring_session` / `spring_session_attributes`).
- Event idempotency, via a dedicated `event_dedup(idempotency_hash, expires_at)` table (24h
  TTL) — there is no generic cache abstraction.
- Scheduling, via Spring `@Scheduled` + ShedLock over a `shedlock` lock table (no separate
  worker container — the job runs inside `api`).
- Schema, via Flyway migrations under `backend/src/main/resources/db/migration`.

There is no message-queue/outbox layer.

Real-time live event dashboard work uses the Mercure Hub (`dunglas/mercure` image, embeds
Caddy) as the stack's public edge — Server-Sent Events, not a separate WebSocket server.

## Development Commands

Run everything Docker-first through `compose.yaml`.

Start locally, with a project name distinct from production's `kivvi-click` (see Current Shape
and the workflow notes in `README.md`'s Production section — this host already runs prod under
that project name, so a bare `docker compose up` here would collide with it):

```bash
HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose -p kivvi-dev up -d --build --wait
```

Open the app at `https://localhost:8443/`.

Common commands:

```bash
docker compose -p kivvi-dev logs -f api
docker compose -p kivvi-dev build api
docker compose -p kivvi-dev port database 5432   # find the random host port Compose assigned
```

Backend build/tests (needs `JAVA_HOME` pointed at a JDK 25 install — on this host,
`export JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25`; Docker-based commands above need no local
JDK):

```bash
cd backend
./mvnw -q test      # fast, no containers: unit tests + ArchitectureTest
./mvnw -q verify     # + Testcontainers Postgres 18 integration tests (*IT.java), needs Docker
./mvnw spotless:apply    # format (google-java-format)
./mvnw spotless:check    # format check
```

Frontend build/tests:

```bash
cd frontend
npm ci
npm run test -- --run              # Vitest unit
npm run test:integration -- --run  # Vitest integration
npm run lint
npm run typecheck
npm run format:check
npm run build
npm run format   # fix formatting
```

The canonical CI-equivalent gate is `./mvnw -q verify` (backend) plus the frontend sequence
above, in the order GitHub Actions (`.github/workflows/build.yml`) runs them.

CI/CD:

- GitHub Actions (`.github/workflows/build.yml`) runs the backend (`mvnw verify`), the frontend
  (typecheck/lint/format/test/build) and a build-only backend Docker image check on push to
  `main` and on pull requests. Nothing is pushed to a registry from CI.

## Current Shape

The full panel is implemented on both sides:

- `backend/src/main/java/click/kivvi/` — `web` (controllers + DTOs) → `application` (view
  services, tracking ingestion) → `domain` (route table, formatting, tracking domain), with
  `infrastructure` (session, scheduling, Mercure publishing, import storage, config) reachable
  only from `application`. Schema in `backend/src/main/resources/db/migration/`.
- `frontend/src/` — `components/{atoms,molecules,organisms}` (ported design system),
  `layouts/` (`PublicLayout`, `AuthLayout`, `AppLayout`), `views/` (one per route), `router/`
  (route table), `stores/` (Pinia, e.g. the shell store), `i18n/{pl,en}.ts`, `styles/`.

Both sides cover the whole panel: landing, login, dashboard, live event stream, customers,
automations, email campaigns, popups/widgets, product feeds, the customer import wizard, and
settings. See `backend/README.md` and `frontend/README.md` for the endpoint list, module
boundaries, and test layout — they're more likely to stay current than a file list here.

Integration coverage lives in `backend/src/test/java/click/kivvi/*IT.java` (one per journey,
e.g. `DashboardApiIT`, `EventsApiIT`, `CollectApiIT`, `ImportApiIT`); frontend coverage lives in
`frontend/test/{unit,integration}/`. The Playwright suite in `tests/e2e/specs/` is unchanged
across the rewrite — it is the parity oracle.

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
- `.claude/settings.json` runs a post-edit formatting hook for backend Java (Spotless,
  requires `JAVA_HOME` pointing to JDK 25) and frontend TS/Vue/CSS/JSON/Markdown (Prettier,
  requires `npm ci` in `frontend`). Prettier runs from `frontend` to respect its config and
  `.prettierignore`, including the preserved design-system CSS.

Codex does not automatically run those Claude hooks, so format edited backend and frontend
files explicitly when making changes (`./mvnw spotless:apply`, `npm run format`).
