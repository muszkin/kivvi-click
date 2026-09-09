# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@context/map/INDEX.md
<!-- END project-context-initializer:router -->

## What this is

kivvi-click-10x is a from-scratch rebuild of the kivvi-click product on a new stack. It is a
multi-tenant SaaS marketing-automation platform for e-commerce sites: track visitor events,
configure rule-based automations, and deliver popups / emails / coupons / product
recommendations at the right moment in the customer journey.

Full product vision, feature list, domain entities, and assumptions live in the `product-spec`
skill — invoke it when planning or building features. Carry over the product goals from the
original, NOT its tech stack.

## Stack — Spring Boot API + Vue 3 SPA, one deployable

- Backend: Java 25, Spring Boot 4.1.1, Maven (wrapper `./mvnw`, no local Maven install needed).
  Package root `click.kivvi`, layered `web` → `application` → `domain`, with `infrastructure`
  reachable only from `application` (enforced by `ArchitectureTest`, an ArchUnit test). Always
  use latest stable versions.
- Frontend: Vue 3.5 SPA (Vite build, no SSR), history-mode vue-router, Pinia, vue-i18n
  (Polish default, English toggle). Pure CSS design system — byte-identical to the original —
  under `frontend/src/styles/`. NO CSS frameworks (Tailwind/Bootstrap) and no other frontend
  framework (React/Svelte). Do not pull in npm UI libs beyond what's already declared.
- One deployable artifact: the Spring Boot jar serves the built SPA from its own classpath
  (`backend/src/main/resources/static`); `backend/Dockerfile` builds both in one multi-stage
  image (Node stage → Maven stage → JRE runtime) — that image is what `compose.yaml` runs.
- Database: PostgreSQL 18 — the ONLY backing service (see Architecture decisions).
- Edge/runtime: the Mercure Hub (`dunglas/mercure` image, embeds Caddy) is the stack's public
  edge — it terminates the outward connection, serves `/.well-known/mercure*` itself, and
  reverse-proxies everything else to the Spring Boot `api` service. Develop docker-first via
  `compose.yaml`. Do not assume a local Postgres/edge setup outside Docker.

## Architecture decisions — do not substitute the usual defaults

Postgres backs everything; there is no Redis, RabbitMQ, Kafka, or Memcached.

- Sessions: Spring Session JDBC (`spring_session` / `spring_session_attributes` tables).
- Schema: Flyway migrations under `backend/src/main/resources/db/migration`. `V1__baseline.sql`
  is the cutover baseline (`spring_session*`, `shedlock`, `event_dedup`) — extend it with new
  versioned migrations, never edit it.
- Event idempotency: a dedicated `event_dedup(idempotency_hash, expires_at)` table (24h TTL),
  not a generic cache abstraction — there is nothing else to cache yet.
- Scheduler: Spring `@Scheduled` + ShedLock (`shedlock-spring` / `shedlock-provider-jdbc-template`,
  JDBC provider) over a `shedlock` lock table, so a restart of the single `api` process resumes
  correctly instead of double-firing. No separate worker container — the scheduled job runs
  inside `api`.
- Messaging: no message-queue/outbox layer exists — there is nothing to replace it with yet.
- Real-time (live event dashboard): the Mercure Hub is the edge (see Stack) — Server-Sent
  Events (SSE), not a separate WebSocket server. `api` publishes over the internal Docker
  network; the browser subscribes through the public Mercure edge.
- i18n: vue-i18n message catalogues in `frontend/src/i18n/{pl,en}.ts` — default language
  Polish; English is an optional toggle. Build strings PL-first with EN translations.

## Commands

Docker-first — run everything through `compose.yaml`.

- Start (dev): production already runs on this host under the `kivvi-click` Compose project
  (see Production below), so always give the dev stack its own project name — omitting `-p`
  defaults to this directory's name (`kivvi-click`) and would collide with prod's containers
  and volumes:
  `HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose -p kivvi-dev up -d --build --wait`
  → https://localhost:8443. See `compose.yaml` for the full port/variable list; the `database`
  service publishes no fixed host port — find it with `docker compose -p kivvi-dev port database 5432`.
- Rebuild after a `backend/Dockerfile`/dependency change: `docker compose -p kivvi-dev build api`.
- Logs: `docker compose -p kivvi-dev logs -f api`.
- Backend build/tests: `cd backend && ./mvnw -q verify` (Testcontainers Postgres 18, needs
  Docker; runs unit tests, `ArchitectureTest`, and every `*IT.java`). Needs `JAVA_HOME` pointed
  at a JDK 25 install — on this host: `export JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25`
  (Docker-based commands need no local JDK). `./mvnw -q test` alone runs the fast, no-container
  subset.
- Frontend build/tests: `cd frontend && npm ci && npm run test -- --run && npm run test:integration -- --run && npm run lint && npm run typecheck && npm run format:check && npm run build`.
- Format: `cd backend && ./mvnw spotless:apply` (google-java-format) and
  `cd frontend && npm run format` (Prettier, TS/CSS).
- Static analysis: `cd backend && ./mvnw spotless:check` (module-boundary check is
  `ArchitectureTest`, part of `./mvnw test`) and `cd frontend && npm run typecheck && npm run lint`.
- CI/CD: GitHub Actions (`.github/workflows/build.yml`) runs the backend (`./mvnw verify`), the
  frontend (typecheck/lint/format/test/build) and a build-only backend Docker image check on
  push to `main` and on pull requests; nothing is pushed to a registry from CI.
- E2E: `cd tests/e2e && npm install && E2E_BASE_URL=https://localhost:8443 npx playwright test`
  — headless Chrome against the running stack (`E2E_BASE_URL` overrides the default
  `https://localhost:8543` in `playwright.config.ts`). Run `events.spec.ts`, `dashboard.spec.ts`
  and `navigation.spec.ts` with `--workers=1` — they share live/SSE state and are flaky in
  parallel. Isolated `package.json` on purpose: this suite's Playwright toolchain is versioned
  independently of `frontend/`'s Vite/Vitest one.
- Migration parity check (oracle regression, safe to keep running after cutover):
  `node tools/migration-verify/compare.mjs --journey <id> --base <url> [--dimension contract|visual|performance|all]`.
- Production on this host: run from this directory only —
  `docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait`
  → http://localhost:23456, fronted by a reverse proxy that terminates TLS for
  https://kivvi.click. The project name (`-p kivvi-click`) must match this directory's own
  name; NEVER run the prod compose from a git worktree, whose different directory name would
  start a second, colliding stack. Secrets and ports live in `.env.prod.docker` (git- and
  docker-ignored).

## Workflow — overrides the global gitflow/Jira rules for this repo

- Trunk-based development: short-lived branches merged frequently to `main`; no long-lived release branches.
- Issue tracking: GitHub Issues (no Jira). Reference issues as `#NN`.
- Commits: Conventional Commits in English, referencing the issue, e.g. `feat: add event ingestion (#42)`.
  No Jira keys, no `CORE-123`-style scopes.
