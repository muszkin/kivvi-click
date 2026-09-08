# Symfony/Twig → Spring Boot + Vue 3 SPA Migration Plan

**Status:** approved (owner, 2026-09-08; coexistence strategy parallel rewrite confirmed)
**Date:** 2026-09-08
**Request:** owner decision of 2026-09-08 ("nie chcę utrzymywać PHP długoterminowo, Java to język, w którym to będzie żyć"; Vue 3 SPA, no SSR), following `context/research/2026-09-08-stack-migration-spring-vue-react.md` (verdict adopt-with-constraints).
**Plan location:** `context/plans/2026-09-08-symfony-to-spring-vue-migration.md` (tracked by Git; `context/plans/` is a proposed convention beside `context/research/`).
**Plan producer:** `migration-planning` skill run.

## Outcome

The store owner (and the public visitor) get exactly the panel and landing they have today — same URLs, same visible texts, same behaviour, same look — served by a Spring Boot 4.1 backend over a JSON API and a Vue 3 single-page application, with Postgres as the only backing service and the Mercure hub kept for the live event stream. After cutover the PHP/Twig stack is removed and the 46 Linear tasks are executed on the Java stack. Nothing new is added during the migration; every improvement waits for the contract stage.

## Scope

### In scope

- Reproduce all 13 journeys captured in the oracle (12 browser journeys + the scheduler heartbeat) on the new stack.
- Preserve the external contracts: `POST /collect` (202/200/400 + JSON body), `POST /preferences/theme|sidebar`, `POST /import/upload` (302 to step 2), `/.well-known/mercure` SSE subscription on `/accounts/1/events`, locale-prefixed URLs `/{pl|en}/…`, 404 behaviour for unknown locale/customer/tab/step.
- Port the design system 1:1: the four CSS files unchanged, every Twig component as a Vue SFC with identical markup, class names and `data-*` attributes, so the existing Playwright suite runs unchanged against the SPA.
- Sample content (`src/Panel/Content/*`) becomes seed fixtures served by the Java API — the same numbers, names and strings.
- New dev/prod Compose stacks for the Java/Vue runtime; cutover on this host on port 23456 behind the unchanged external proxy.

### Out of scope

- Any Linear task (PIO-70…PIO-115): auth with passwords, tenants, persistence of events, k.js, real data. They start after `CON-1`.
- `/_storybook` (dev tooling) — parity not required; a Vue equivalent is a later task.
- The dead `POST <editor>/blocks` call from `assets/controllers/editor.ts` (no route exists today, R6) — the SPA editor must not call it either; nothing to reproduce.
- `templates/base.html.twig` (unused legacy base, R15).
- SSR of any page (owner decision).
- Migrating session rows, cache rows or messenger rows — there is no domain data to migrate (DEV-9).

## Evidence map

| Evidence | What it establishes | Provenance |
| --- | --- | --- |
| `context/map/INDEX.md`, `context/map/manifest.json` (snapshot `5b806ac`, product code `91f8f85`, freshness complete) | Repository structure, flows, risks R1–R16 | Observed |
| `context/research/2026-09-08-stack-migration-spring-vue-react.md` (status decided) | Target technology direction and version evidence | Observed / User-confirmed |
| `context/migration-oracle/symfony-to-spring-vue/manifest.json` | Pre-migration truth: 13 journeys, inventory, behaviours, rules, performance baseline | Observed (captured 2026-09-08 on `5b806ac`) |
| `src/Controller/*.php`, `context/migration-oracle/symfony-to-spring-vue/inventory/routes.json` | 27 routes, none behind a firewall, locale prefix `pl|en` | Observed |
| `templates/components/**` (56 templates), `inventory/components.json` | Component tiers and usage counts | Observed |
| `assets/styles/*.css` (3,162 lines), `assets/app.ts`, `assets/controllers/*.ts` | Pure CSS tokens/components; declarative `data-action` intents; six controllers | Observed |
| `tests/e2e/specs/*.ts` (52 tests), `tests/**/*.php` (5 classes), `behaviours.json` (33 behaviours) | The behaviour list the new stack must keep green | Observed |
| `compose.yaml`, `compose.prod.yaml`, `README.md` Production section | Single host, port 23456, external TLS proxy, Postgres 18, dedicated prod volumes | Observed |
| `docker inspect kivvi-click-php-1` (RestartCount 1499), `kivvi-click-database-1` exited 2026-09-07 11:00 UTC, restart policy `no` | Production is currently down; the database service has no restart policy | Observed (2026-09-08) |
| Linear project "kivvi" (46 issues, all Backlog) | Backlog to re-point after cutover | Observed |
| Version pins table below | Current stable versions from GitHub releases API, Maven Central BOM, Adoptium API, retrieved 2026-09-08 | Observed |
| Host: 16 CPU, 125 GB RAM, 11 GB free disk, Chrome 152, Node 26.8.1, OpenJDK 21.0.12, no Maven/Gradle | Capacity for parallel worktrees; toolchain gaps | Observed |

## Current state

See `context/map/architecture-and-flows.md`. In one paragraph: FrankenPHP serves a Symfony 8 app whose 14 controllers render Twig pages from hard-coded catalogues; the only persisted state is the PHP session (identity e-mail, theme, sidebar, uploaded-file pointer), the cache table (dedup keys, scheduler state) and the messenger table; `POST /collect` deduplicates by idempotency id and pushes a server-rendered row through the Caddy-embedded Mercure hub; a worker container runs the hourly heartbeat. There is no authentication, no entity, no CSRF. Production runs the same code on port 23456 and has been crash-looping for ~27 h because its database container stopped.

## Migration kind and trigger

| Trigger | Evidence | Provenance | Kind decided |
| --- | --- | --- | --- |
| Whole-stack replacement: PHP 8.5 / Symfony 8 / Twig / vanilla TS → Java 25 / Spring Boot 4.1 / Vue 3 SPA | `composer.json`, `package.json`, `Dockerfile`, owner decision 2026-09-08 | Observed + User-confirmed | primary `backend-swap`, secondary `frontend-swap` |

**Coexistence strategy: parallel rewrite** (the new stack grows in `backend/` and `frontend/` beside the old one; the old stack stays the oracle; one cutover at the end).

Challenge to the default (`backend-swap` defaults to strangler at the edge), raised once: strangler needs a shared session across PHP and Java through an edge proxy and a frozen shared schema. Here (1) the PHP session is a PHP-serialized blob in `sessions` that Spring Session cannot read; (2) there is no domain schema to share — only `sessions`, `cache_items`, `messenger_messages`; (3) there are no real users (production was down for a day without anyone noticing) so per-wave production cutover buys nothing; (4) one developer would maintain two live stacks plus proxy rules. Parallel rewrite removes the session-sharing seam and the proxy work; its cost is that nothing ships to production until the last wave. **Operator decision 2026-09-08: parallel rewrite confirmed.**

## Inventory coverage ledger

Every item ends in exactly one row. Full lists live in `context/migration-oracle/symfony-to-spring-vue/inventory/*.json`.

| Item | Type | Journey or packet | Status |
| --- | --- | --- | --- |
| `GET /{locale}/login`, `POST /{locale}/login`, `POST /{locale}/logout` | route | login | mapped |
| `GET /{locale}` (home), `GET /{locale}/demo`, `GET /` | route | landing | mapped |
| `GET /{locale}/dashboard` | route | dashboard | mapped |
| `GET /{locale}/events`, `POST /collect`, Mercure topic `/accounts/1/events` | route / endpoint | event-stream | mapped |
| `GET /{locale}/customers`, `GET /{locale}/customers/{id}` | route | customers | mapped |
| `GET /{locale}/automations`, `/automations/new`, `/automations/{id}` (`?view=`) | route | automations | mapped |
| `GET /{locale}/campaigns`, `/emails/new`, `/emails/{id}` | route | campaigns-email-editor | mapped |
| `GET /{locale}/popups` (`?preview=`), `/popups/new`, `/popups/{id}` (`?type=&device=`) | route | popups-widget-editor | mapped |
| `GET /{locale}/feeds` | route | feeds | mapped |
| `GET /{locale}/import/{step}`, `POST /import/upload` | route / endpoint | import-wizard | mapped |
| `GET /{locale}/settings/{tab}` | route | settings | mapped |
| `POST /preferences/theme`, `POST /preferences/sidebar`, locale switch, unknown-locale 404 | endpoint / behaviour | shell-navigation (implementation folded into login's shell chrome) | mapped |
| `/_storybook`, `/_storybook/{id}`, `/_storybook/{id}/{variant}/frame` | route | — | Out of scope (dev tooling) |
| `POST <editor>/blocks` (no route) | endpoint | — | Out of scope (R6; SPA must not call it) |
| Session keys `panel.identity`, `panel.theme`, `panel.sidebar` | store | login (identity), shell chrome (prefs) | mapped (DEV-9) |
| Session keys `import.file_name`, `import.file_path`, `var/import/*` | store | import-wizard | mapped |
| `cache.app` dedup keys `event.seen.*` | store | event-stream | mapped (DEV-5) |
| `cache.app` scheduler state, `messenger_messages` | store | scheduler-heartbeat | mapped (DEV-5) |
| `translations/messages.pl.yaml` (77 keys), `messages.en.yaml` (220 keys), hard-coded PL strings | i18n | every journey owns the keys its pages use; catalogue plumbing in login | mapped |
| 13 atoms, 27 molecules, 18 organisms (`inventory/components.json`) | component | first journey to render each (see journey packets "Produces") | mapped |
| `assets/styles/{01-tokens,02-base,03-components,04-patterns,app}.css` | asset | login (copied verbatim in wave 0) | mapped |
| `assets/styles/storybook.css`, `assets/js/storybook-controls.js` | asset | — | Out of scope |
| `assets/controllers/{shell,modal}.ts`, `app.ts` intents | behaviour | login/shell chrome | mapped |
| `assets/controllers/event-stream.ts`, `cardiogram.ts` | behaviour | event-stream, dashboard | mapped |
| `assets/controllers/upload.ts` | behaviour | import-wizard | mapped |
| `assets/controllers/editor.ts` (drag/drop) | behaviour | campaigns-email-editor (drag source/drop target only, no request) | mapped |
| Flags | flag | — | none exist |
| PHPStan/php-cs-fixer/PHPUnit policy (no boundary rules) | rule | wave-0 introduces translated rules (`architecture/rules-translated.md`) | mapped |
| 33 behaviours (`behaviours.json`) | test behaviour | listed per journey | mapped |
| `compose.yaml` + overrides, `Dockerfile`, `frankenphp/*`, GitHub Actions build | deploy | CUT-1 / CON-1 | mapped |
| `AGENTS.md`, `CLAUDE.md`, `README.md` stack sections | docs | CON-1 (rewrite) | mapped |
| `context/foundation/*`, `context/map/*` | docs | CON-1 (refresh via `project-context-initializer`) | mapped |

## Decision and assumption ledger

| Item | Provenance | Decision state | Evidence or rationale | Consequence |
| --- | --- | --- | --- | --- |
| Migrate to Java/Spring Boot; Vue 3 SPA; no SSR | User-confirmed (2026-09-08) | Decided | research verdict flip condition met | this plan |
| Parallel rewrite, not strangler | User-confirmed (2026-09-08) | Decided | see challenge above | single cutover; nothing in prod until wave-5 |
| Keep Mercure hub (dunglas/mercure, AGPL-3.0) as a separate container | Observed contract + Inferred | Proposed | browser contract `/.well-known/mercure` stays identical; unmodified AGPL binary used as a network service | one more container; AGPL obligations limited to the unmodified hub |
| Mercure payload changes from `{html}` to a JSON event | Inferred | Proposed → DEV-3 | the SPA renders rows from data; the only consumer is our own JS | verifier compares event data, not HTML |
| Same CSS and markup, existing Playwright suite runs unchanged (only `E2E_BASE_URL`) | Inferred | Proposed | `tests/e2e/specs/*` select by class/data attributes; a11y/text oracle ignores classes | e2e suite is the acceptance oracle for every wave |
| Sample catalogues become seed fixtures in the Java API | Inferred | Proposed | zero-change rule; real data is Linear work | fixtures are JSON resources under `backend/src/main/resources/fixtures/` |
| Repository layout during coexistence: `backend/`, `frontend/`, `compose.next.yaml`, `compose.next.prod.yaml`; PHP stays at root until CON-1 | Inferred | Proposed | one repo, one CI; old stack untouched | CON-1 deletes PHP and renames compose files |
| Session: Spring Session JDBC, cookie `SESSION`; identity/theme/sidebar stored in session | Inferred | Proposed | same UX; no user table exists yet | DEV-9 at cutover (everyone logged out — nobody is logged in) |
| Dedup: persistent `event_dedup(idempotency_hash, expires_at)` table with 24 h TTL cleanup | Inferred | Proposed | reproduces behaviour (202 then 200) with a durable store | DEV-5 (table names differ) |
| Build tool Maven with wrapper | Inferred | Proposed | Spring Initializr default; no daemon; no Gradle on host | `./mvnw` |
| Java 25 (Temurin) | Observed (Adoptium: most recent LTS 25) | Proposed | Spring Boot 4.1.1 supports 17–26; LTS line | Dockerfile `eclipse-temurin:25-jre` |
| Prod host stays this machine, port 23456, external proxy unchanged | Observed | Decided (existing) | `README.md` | CUT-1 only swaps the compose stack |
| Production was down (database stopped) | Observed | Decided (owner, 2026-09-08): old prod restored, `restart: unless-stopped` added to `database` in `compose.yaml` | restart count 1499; restored 2026-09-08 | CUT-1 entry condition satisfied; the new stack's database service inherits the same policy |
| Disk: 11 GB free | Observed | Accepted risk needed | JVM images + node_modules per worktree ≈ 1–1.5 GB each | max 2 parallel journeys per wave; kill criterion on disk < 3 GB |

## Socratic challenge ledger

| Risk rank | Assumption | Strongest counterexample or failure mode | Evidence | Consequence if false | Decision and accountable owner |
| --- | --- | --- | --- | --- | --- |
| high | The existing e2e suite can run unchanged against an SPA | SPA routing may not produce a full document for `page.goto('/de/dashboard')` → 404 status; `expectStatus 404` steps need the server to answer 404 for unknown locale/ids | `PanelPagesTest`, oracle steps with `expectStatus` | contract/e2e FAIL on 404 behaviours | Backend serves the SPA shell only for known locale prefixes and returns real 404 documents for `/de/*`, unknown ids resolved server-side by a `HEAD`-style resolver endpoint before shell delivery (`GET /api/v1/resolve?path=`) — owned by login (wave-0) |
| high | Visual parity per step with masks is achievable for an SPA | fonts/antialiasing identical (same host, same Chrome); initial-paint differences hidden by settle; sub-pixel layout shifts from Vue's rendering of whitespace-sensitive inline markup (`{%- -%}` trimming in Twig) | oracle screenshots; Twig uses whitespace control in many components | visual FAIL on text-baseline shifts | SFC templates copy the Twig output markup (not the Twig source), including whitespace; threshold 0.5 % with masks; first wave proves it on login — kill criterion if >3 rounds |
| high | Sample data can be moved to Java fixtures without drift | `EventFeed::rows()` derives times from `now`, `CustomerController` uses `timeAgo` deltas — deterministic given a clock; the Java side must compute identical relative strings | `src/Panel/Content/EventFeed.php`, `Panel\Format` | text diffs like "12 min temu" | Port `Format` to Java + TS with unit tests against the oracle texts; DEV-1 masks only absolute clock times |
| medium | Mercure hub as a separate container keeps the same-origin URL | the hub must sit on the same origin as the SPA; dunglas/mercure image is Caddy — it can front the API | `frankenphp/Caddyfile` (embedded hub today) | CORS/cookie issues on `EventSource withCredentials` | wave-0: `mercure` container is the edge (`reverse_proxy api:8080` for everything except `/.well-known/mercure`) |
| medium | One developer + AI workers can run 2–3 parallel journeys | disk 11 GB; each worktree needs `node_modules` (~300 MB) + Maven target + Docker images (JRE image ~250 MB shared) | `df -h` | worktree provisioning fails mid-wave | resource table caps parallelism at 2; shared `~/.m2` read-only cache; `npm ci --prefer-offline`; kill criterion on disk |
| medium | Owner will re-point 46 Linear tasks after cutover | tasks name PHP paths; executed by agents that read only the issue | memory `kivvi-linear-backlog` | agents implement against deleted code | task-closeout after CON-1 rewrites "Punkt startu w repo" for every issue (post-plan action, owner) |
| low | Google Fonts availability during verification | offline verification host | `templates/layout/base.html.twig` | text metrics differ | verification runs on this host with network; documented in verifier contract |

## Solution contract

### Actors and permissions

Unchanged: anyone can open every URL; the login form only sets the displayed identity; `/collect` is unauthenticated. (Security is Linear P0 work after cutover.)

### Happy path (per journey)

Defined by the oracle scenarios `context/migration-oracle/symfony-to-spring-vue/capture/scenarios.json`; each journey packet lists its steps.

### Edge cases and failure behaviour

| Case | Required behaviour | Recovery or fallback | Journey |
| --- | --- | --- | --- |
| Unknown locale `/de/…` | 404 document | — | login (shell chrome) |
| Unknown customer id, settings tab, import step | 404 document | — | customers, settings, import-wizard |
| `/collect` without `idempotency_id`, unknown `type`, invalid `occurred_at`, non-JSON body | 400 JSON `{error}` with the same Polish messages | — | event-stream |
| Replayed `idempotency_id` within 24 h | 200 `{status:"duplicate"}`, no publish | — | event-stream |
| Publish before subscriber connected | event lost (Mercure has no replay) — parity with today | UI shows `reconnecting` on hub outage | event-stream |
| Empty / malformed e-mail on login | form re-rendered with `Podaj adres e-mail.` / `To nie wygląda na poprawny adres e-mail.` | — | login |
| Unknown theme / sidebar value | falls back to `light` / `expanded` | — | login (shell chrome) |
| Upload without file | 404 as today | — | import-wizard |
| Uploaded file name with path characters | stored as `<random>.<whitelisted ext>` | — | import-wizard |
| Worker restart after missed heartbeat | exactly one catch-up run | — | scheduler-heartbeat |

### Data, state and contracts

- **Preserved external contracts:** listed under "In scope". Request/response bodies for `/collect` and `/preferences/*` byte-equal after normalization.
- **New internal contract (introduced):** JSON API `GET /api/v1/{locale}/…` per page view-model, mirroring the arrays the Twig pages receive today (the `render()` parameters in each controller are the schema). Documented per journey packet; frozen once a wave integrates.
- **Mercure event:** topic unchanged; data becomes `{ "event": { time, type, typeIcon, tone, detail, customerId?, customerName?, siteName?, siteColor? } }` (DEV-3).
- **Persistence:** Flyway baseline `V1__baseline.sql` creating `spring_session*`, `shedlock`, `event_dedup`. No other tables. Schema frozen until CON-1.
- **Session cookie:** `SESSION` (Spring Session default), `SameSite=Lax`, `Secure` behind the proxy via `server.forward-headers-strategy=framework` and `TRUSTED_PROXIES` equivalent.

### Threat model

Triggers checked: file upload (yes — `/import/upload`), public network boundary (yes), user input reaching a sink (yes — `/collect`). No auth, secrets or payments are added.

| Asset | Threat | Entry point | Existing control | Gap | Slice that closes it |
| --- | --- | --- | --- | --- | --- |
| Server filesystem | tampering via upload path | `POST /import/upload` | random stored name, whitelisted extension (`ImportUploadStorage`) | none new | import-wizard reproduces the same control (unit test) |
| Hub / panel | DoS by unauthenticated `/collect` flood | `/collect` | none today | none today (parity) | Out of scope — Linear PIO-79/114 |
| Session | fixation / theft | cookie | `HttpOnly`, `Lax`, `Secure` auto | none new | login: Spring Session defaults equal or stronger |
| Anyone can subscribe to any topic | information disclosure | Mercure `anonymous` | none today | parity | Out of scope — PIO-75 |

- **Trust boundaries crossed:** browser → edge (mercure/Caddy) → api; api → Postgres; api → hub (publish JWT).
- **Secrets introduced or moved:** `MERCURE_PUBLISHER_JWT_KEY`, `MERCURE_SUBSCRIBER_JWT_KEY` (hub), `MERCURE_JWT_SECRET` (api), `SPRING_DATASOURCE_PASSWORD` (was `POSTGRES_PASSWORD` via `DATABASE_URL`); names only, defined in `.env.prod.docker`.
- **Abuse cases promoted to acceptance:** upload path traversal (import-wizard unit test), duplicate replay (event-stream oracle step 3).

### Non-functional constraints

- `/collect` p95 < 500 ms on the oracle stack shape (PRD) — measured by the `e2e` verifier with 50 sequential requests.
- SPA JavaScript budget: ≤ 300 kB gzip total initial JS; first route LCP ≤ 2.0 s and TTI ≤ 2.5 s on localhost Chrome (DEV-8 absolute budget replacing the relative 20 % rule, because the baseline is an importmap of ~80 kB of sources).
- No new backing service besides the Mercure hub container.

### Rollout and rollback

Parallel rewrite: nothing ships until CUT-1. CUT-1 swaps the prod compose stack on port 23456; rollback = `compose down` new stack, `compose up` old stack (old images and volumes retained). Observation window 7 days. CON-1 removes the PHP stack after the window.

## Technology decisions

| Decision | Version or policy | Requirement driving it | Rationale | Rejected alternatives | Source |
| --- | --- | --- | --- | --- | --- |
| Java runtime | Temurin 25 (`jdk-25.0.4.1+1` current GA) | owner: Java long-term | current LTS; Spring Boot 4.1.1 supports 17–26 | Java 21 (host default; older LTS, shorter runway) | api.adoptium.net `available_releases` (most_recent_lts 25), `release_names` 2026-09-08 |
| Backend framework | Spring Boot 4.1.1 (Spring Framework 7.0.9) | research verdict | current GA 2026-08-20; OSS support to 2027-07-31 (secondary source) | 4.0.x (EOL 2026-12-31), 3.5.x (EOL) | GitHub releases, `spring-boot-dependencies-4.1.1.pom`, endoflife.date |
| Web stack | Spring Web MVC (servlet, Tomcat 11) + Jackson 3.1.5 | simple request/response API | blocking API is enough; SSE handled by Mercure | WebFlux (no need) | BOM |
| Persistence | Spring Data JPA (Hibernate 7.4.5) + Flyway 12.4.0 + PostgreSQL JDBC 42.7.13 | Postgres-only rule | managed by BOM | Liquibase (no advantage), jOOQ (extra codegen) | BOM |
| Sessions | Spring Session JDBC 4.1.1 | Postgres-only rule | managed by BOM | Redis session store (forbidden) | BOM |
| Scheduling | Spring `@Scheduled` + ShedLock 7.10.0 (JDBC provider) | hourly heartbeat, single run across replicas | Postgres lock table | Quartz (heavier) | GitHub tags `lukas-krecan/ShedLock` 2026-09-08 |
| Architecture tests | ArchUnit 1.5.0 | translated boundary rules | standard JVM tool | jQAssistant | GitHub releases 2026-09-08 |
| Backend tests | JUnit 6.0.3, Spring Boot Test, Testcontainers 2.0.5 (Postgres) | integration parity | managed by BOM | H2 (not Postgres) | BOM |
| Build | Maven 3.9 wrapper, Spotless + google-java-format | no Gradle on host; Initializr default | simplest | Gradle 9 | docs.spring.io system requirements |
| Frontend framework | Vue 3.5.42 | owner decision | current stable; 3.6 still rc | React (rejected in research) | GitHub releases 2026-09-08 |
| Router / state | vue-router 5.3.1, Pinia 4.0.3 | SPA with history mode | official | hand-rolled | GitHub releases 2026-09-08 |
| Build / test | Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11 | Vue default toolchain | official | webpack, Jest | GitHub releases 2026-09-08 |
| i18n | vue-i18n 11.4.10 | PL default, EN toggle | official Intlify | custom | GitHub releases 2026-09-08 |
| TypeScript / formatting | TypeScript 6.0.3, Prettier 3.8.4 (existing pins) | keep repo tooling | already locked | — | `yarn.lock` |
| Node | 26.8.1 (host) — LTS status Unknown | Vite ≥ 20.19 | present on host | — | `node --version`; Vite docs not fetched |
| Real-time hub | dunglas/mercure v0.24.2 (AGPL-3.0) as edge + hub | preserve `/.well-known/mercure` contract | unmodified binary as a service; Caddy inside fronts the API | Spring SSE endpoint (breaks contract), Caddy + separate hub (two containers) | GitHub releases 2026-09-08; licence via GitHub API |
| Database | PostgreSQL 18 (existing) | existing | — | — | `.env` |
| E2E | Playwright 1.62.1 (existing `tests/e2e`), Chrome 152 | oracle and verification | already locked | — | `tests/e2e/package-lock.json` |

## Target stack

| Decision | Version pinned | Source and retrieval date | Research-spike verdict |
| --- | --- | --- | --- |
| Spring Boot | 4.1.1 | github.com/spring-projects/spring-boot/releases, 2026-09-08 | adopt-with-constraints (research artifact) |
| Vue 3 SPA | 3.5.42 | github.com/vuejs/core/releases, 2026-09-08 | adopt-with-constraints (research artifact; SSR rejected) |
| Java | Temurin 25.0.4.1+1 | api.adoptium.net, 2026-09-08 | n/a — settled by reading (LTS list) |
| Maven wrapper | 3.9.x | docs.spring.io/spring-boot/system-requirements (Maven ≥ 3.6.3), 2026-09-08 | n/a — no credible alternative on this host |
| Mercure hub | 0.24.2 | github.com/dunglas/mercure/releases, 2026-09-08 | n/a — contract preservation leaves one option |
| Everything else | see Technology decisions | see table | n/a |

## Global implementation constraints

- Zero-change: reproduce oracle texts, a11y trees, URLs, status codes and contracts; every difference is a `DEV-n` or a regression.
- Postgres is the only datastore; the Mercure hub is the only additional container.
- PL default, EN toggle; locale in URL prefix.
- The old stack (`src/`, `templates/`, `assets/`, `composer.*`, `compose.yaml`, `Dockerfile`, `frankenphp/`) is read-only for every journey worker; only CON-1 touches it.
- `tests/e2e` specs are read-only for journey workers; a spec change is a regression signal, not a fix.
- Conventional Commits in English; no AI co-author trailers; trunk-based on `main`.
- Commands run inside containers or the worktree; nothing is installed globally on the host.

## Oracle

- **Path:** `context/migration-oracle/symfony-to-spring-vue/`
- **Manifest SHA-256:** `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`
- **Source SHA:** `5b806ac` (docs-only commits after `91f8f85`; product code identical to `91f8f85`, recorded in the manifest as `product_code_sha`)
- **Capture command (introduced):** from the repository root with the oracle stack up:
  `COMPOSE_PROJECT_NAME=kivvi-oracle HTTP_PORT=18080 HTTPS_PORT=18443 HTTP3_PORT=18443 docker compose -p kivvi-oracle up -d --build --wait` then
  `ORACLE_RUN_ID=<id> ORACLE_BASE_URL=https://localhost:18443 ORACLE_COMPOSE="docker compose -p kivvi-oracle" node context/migration-oracle/symfony-to-spring-vue/capture/capture.mjs` then `node …/capture/manifest.mjs`
- **Viewports:** desktop 1440×900, mobile 390×844; browser clock fixed at 2026-09-08T12:00:00Z; reduced motion; transitions disabled; Chrome 152 channel.
- **Thresholds:** perceptual diff ≤ 0.5 % per step after masks; `/collect` p95 < 500 ms; JS budget per DEV-8.
- **Completeness verdict:** all 13 journeys `complete` in the manifest (12 captured, 1 static contract); see the manifest `journeys[]`.

## Breaking-change catalog

| Change | Source | Affected items | Wave | Proving verifier |
| --- | --- | --- | --- | --- |
| Server-rendered HTML pages → SPA shell + JSON API | this plan | every page route | every wave | contract (DEV-4), visual, e2e |
| Mercure payload `{html}` → JSON event | this plan (DEV-3) | event-stream, dashboard | wave-2 | contract, e2e |
| PHP session → Spring Session JDBC (`SESSION` cookie) | Spring Session docs (managed 4.1.1) | login, shell chrome, import-wizard | wave-0 | contract (DEV-9), e2e |
| `cache_items` dedup keys → `event_dedup` table; scheduler state → `shedlock` | this plan (DEV-5) | event-stream, scheduler-heartbeat | wave-2, wave-1 | contract |
| Caddy-embedded hub → dunglas/mercure container as edge | Mercure docs (image = Caddy + hub) | all pages (same origin), SSE | wave-0 | e2e |
| Twig whitespace control → SFC templates | Vue SFC compiler keeps whitespace by default (`compilerOptions.whitespace: 'condense'` is default; set `'preserve'` where Twig output kept spaces) | every component | every wave | visual |
| Import upload redirect handled by SPA router after `302` | this plan | import-wizard | wave-4 | contract, e2e |
| `vue-i18n` message catalogue replaces YAML + inline PL strings | this plan | all pages | every wave | visual (texts) |

No framework-major upgrade guides apply (the old stack is replaced, not upgraded).

## Wave map

| Wave | Journeys | Prerequisites | Parallel group | Oracle status | Kill criteria | Extra exit condition |
| --- | --- | --- | --- | --- | --- | --- |
| wave-0 | login (walking skeleton: backend + SPA scaffolds, compose stacks, CSS port, shell chrome, session, i18n, 404 resolver, six-dimension harness, compare tooling) | none | single | complete | default set + K5 | none |
| wave-1 | landing, feeds, scheduler-heartbeat | wave-0 | A (landing ∥ feeds; heartbeat backend-only) — max 2 concurrent (disk) | complete | default set | none |
| wave-2 | event-stream, customers | wave-1 | B (proven disjoint) | complete | default set + K6 | none |
| wave-3 | automations, settings, campaigns-email-editor | wave-2 | C (proven disjoint; max 2 concurrent) | complete | default set | none |
| wave-4 | popups-widget-editor, import-wizard, dashboard | wave-3 | D (proven disjoint; max 2 concurrent) | complete | default set + K7 | none |
| wave-5 | shell-navigation | wave-4 | single | complete | default set + K7 | none |

Then the final all-journey cohort → `CUTOVER_READY` → CUT-1.

## Journey packets

Common to every packet (not repeated below):

- **Global constraints:** see above. **Old-stack prohibition:** no edits under `src/`, `templates/`, `assets/`, `tests/**/*.php`, `tests/e2e/specs/`, `composer.*`, `compose.yaml`, `compose.override.yaml`, `compose.prod.yaml`, `Dockerfile`, `frankenphp/`. Integration seams allowed: none (parallel rewrite).
- **Verifier applicability (primary `backend-swap` + secondary `frontend-swap`):** `unit` required (JUnit + Vitest), `integration` required (Spring Boot Test with Testcontainers + Vitest component tests with router/pinia), `architecture` required (ArchUnit + ESLint boundary rules from `architecture/rules-translated.md`), `contract` required (HTTP recording parity for preserved endpoints; method/path/status parity for document requests; DB-write delta parity per `db.json` mapped by DEV-5), `visual` required on every SPA step (0.5 % after masks; a11y tree and texts exact after `normalize.json`), `e2e` required (the journey's Playwright specs from `tests/e2e/specs` run unchanged with `E2E_BASE_URL` pointing at the worktree stack, plus budget checks per DEV-8).
- **Commands (introduced in wave-0 unless marked confirmed):** backend `cd backend && ./mvnw -q verify` (unit+integration+ArchUnit); frontend `cd frontend && npm run test` (Vitest), `npm run typecheck` (`vue-tsc --noEmit`), `npm run lint`, `npm run build`; stack `docker compose -p <lease> -f compose.next.yaml up -d --build --wait`; e2e `cd tests/e2e && E2E_BASE_URL=https://localhost:<port> npx playwright test <spec>` (confirmed command, new base URL); compare `node context/migration-oracle/symfony-to-spring-vue/capture/compare.mjs --journey <id> --base https://localhost:<port>` (introduced in wave-0: replays `scenarios.json`, writes wave evidence, applies masks and `normalize.json`, exits non-zero on regression).
- **Static analysis:** `./mvnw -q spotless:check compile` and `npm run lint && npm run typecheck`. **Sonar:** not configured (evidence: no `sonar-project.properties`, no CI job) — recorded as not configured. **Independent review focus:** markup parity, i18n completeness, contract shape, secrets in compose.
- **Expected initial RED:** for every journey, the `compare.mjs --journey <id>` run and the journey's Playwright spec fail on the new stack before implementation (route returns the SPA shell with an empty page), while the same commands pass against the oracle stack — this separates missing behaviour from harness failure.
- **Worktree resource lease:** compose project `kivvi-w-<journey>`, edge port from the pool `19000 + 10·n` (n = journey index in this plan), Postgres published on a random port, Mercure JWT `w-<journey>`, Playwright browser context per run, `~/.m2` shared read-mostly with `-Dmaven.repo.local=$WORKTREE/.m2` on write, `node_modules` per worktree.

### J0 — login (wave-0 walking skeleton)

**Capability:** The store owner can sign in with an e-mail address and see their identity in the panel shell.
**Why now:** smallest real journey that forces the whole path: SPA shell → API → session → Postgres, plus the chrome every other journey renders inside.
**Prerequisites:** none. **Parallel-safe with:** none (single).

- **In scope:** `backend/` Spring Boot 4.1.1 app (Maven wrapper, Flyway baseline, Spring Session JDBC, `GET /api/v1/{locale}/shell` view-model: nav groups, workspace card, user, theme, sidebar; `POST /{locale}/login` (form → 302 or 200 with error), `POST /{locale}/logout`, `POST /preferences/theme|sidebar` exact contracts; 404 for unknown locale; SPA shell delivery for known routes); `frontend/` Vite + Vue 3 app with router (history mode, `/{locale}/…`), Pinia shell store, vue-i18n with PL/EN catalogues seeded from the oracle texts and `translations/*.yaml`, the four CSS files copied verbatim, components: `AppShell`, `Sidebar`, `Topbar`, `NavItem`, `WorkspaceCard`, `Avatar`, `Icon` (icon map from `IconExtension`), `Wordmark`, `Logo`, `Button`, `Field`, `Callout`, `Kbd`, `Modal`, layouts `auth`, `app`, `public` (shell only), `LoginView`, empty `DashboardView` placeholder (body masked by DEV-11); `data-action` intent dispatcher equivalent (`useIntents`), theme applied before first paint from the session-backed shell payload plus a cookie mirror to avoid flash; `compose.next.yaml` (api, mercure edge, database) and `compose.next.prod.yaml`; verifier harness for all six dimensions; `compare.mjs`.
- **Out of scope:** any page body; Mercure publishing; upload.
- **Likely change surface:** `backend/**`, `frontend/**`, `compose.next*.yaml`, `context/migration-oracle/symfony-to-spring-vue/capture/compare.mjs`, `.github/workflows/next-build.yml` (introduced CI job: `mvnw verify` + `npm run build`).
- **Conflict footprint:** everything in wave-0 is owned by this journey.
- **Consumes:** oracle `journeys/login`, `journeys/shell-navigation` steps 11–16 (prefs) and 21 (404) as its chrome oracle. **Produces:** the shell API and components, session, i18n plumbing, the route table, the compose stacks, the harness — every later journey consumes them without re-migrating.
- **Behaviour and acceptance:** oracle steps 1–8 of `login`; B01 (login/dashboard rows), B04, B07, B08, B09, B10, B11–B14, B22 (login part); `.sb-foot` shows `anna@aureashop.pl` after sign-in and `maciej@aureashop.pl` after logout; empty/malformed e-mail return the two Polish messages; `/de/dashboard` returns a 404 document.
- **Test cycle:** unit — `Format` port (PL number/money/timeAgo) against oracle strings, identity name derivation (`anna.kowalska@…` → `Anna Kowalska`), theme/sidebar fallbacks; integration — Spring Boot Test + Testcontainers for session round-trip, `POST /preferences/theme` then `GET /api/v1/pl/shell` reflects it; Vitest component tests for `Sidebar` active state and `NavItem` aria-current; architecture — ArchUnit layering test and ESLint restricted-imports rule pass; contract — `compare.mjs --journey login` parity on preserved endpoints, DEV-4 for document requests; visual — steps 1–3, 5–7 exact, steps 4 and 8 with DEV-11 mask; e2e — `npx playwright test public.spec.ts -g login` and `navigation.spec.ts -g "sidebar collapse|theme toggle"` green against the worktree stack.
- **Deviations in scope:** DEV-4, DEV-9, DEV-11.
- **Behaviours:** B01 (subset), B04, B07–B14, B22 (login).
- **Architecture rules:** all wave-0 rows of `rules-translated.md`.
- **Delivery safety:** observability — Spring Boot Actuator health at `/actuator/health` (internal) and JSON logs; rollback — nothing shipped.
- **Executor handoff:** Implement only J0. Read `context/map/INDEX.md`, this packet, the oracle `login` and `shell-navigation` scenarios, `src/Panel/*.php`, `templates/layout/*.twig`, `templates/components/organisms/{sidebar,topbar}.html.twig`, `assets/app.ts`. Copy markup from the *rendered* oracle a11y/texts and Twig output, not from Twig source. Do not touch the old stack. Run the six verifier commands and `compare.mjs --journey login`; report changed files, commands, results, evidence paths, and any oracle claim the implementation disproved.

### J1 — landing (wave-1, group A)

**Capability:** A visitor can read the public landing page in Polish or English and enter the panel demo.
**Prerequisites:** wave-0. **Parallel-safe with:** feeds, scheduler-heartbeat (disjoint files: `frontend/src/views/LandingView.vue`, `frontend/src/components/landing/**`, backend `LandingContentFixture` + `GET /api/v1/{locale}/landing`).
- **In scope:** landing view, `PublicLayout` content, `/{locale}/demo` → 302 to dashboard, `/` → 302 `/pl` (as today: oracle step 3 records the status), features/steps/plans/trust points/preview tiles + SVG preview chart from fixture data. **Out of scope:** dashboard body.
- **Produces:** `PriceCard`, `Feat`, hero components; `SparklineSvg` (landing preview) — owned here.
- **Acceptance:** oracle `landing` steps 1–5; B22 (landing part); `.hero-cta a` count 2, `#features .feat` 6, `#how .feat` 3, `.price-card` 2, EN variant.
- **Tests:** Vitest snapshot of hero/pricing against oracle texts; contract parity for `/`, `/pl`, `/en`, `/pl/demo` statuses; visual exact; e2e `public.spec.ts -g landing`.
- **Deviations:** DEV-4.

### J2 — feeds (wave-1, group A)

**Capability:** The store owner can see product feed sources, sync state and the price-matching diagnostic.
**Prerequisites:** wave-0. **Parallel-safe with:** landing, scheduler-heartbeat.
- **Produces (first owner):** `KpiGrid`, `KpiTile`, `Sparkline`, `Card`, `FeedCard`, `Bar`/`BarRow`, `Chip`, `Dot`, `PageHead`, `ListCard`? (no — dashboard owns `ListCard`).
- **Acceptance:** oracle `feeds` step 1; B30: 4 sources, 4 feeds, one `.feed-card__err` with `HTTP 503`, `.chip.info .dot.live` once, matching card with 4 `.bar-row`, `142`.
- **Tests:** Vitest for `KpiGrid`/`FeedCard`; JUnit fixture test for `GET /api/v1/pl/feeds`; e2e `lists.spec.ts -g "product feeds"`.

### J3 — scheduler-heartbeat (wave-1, backend only)

**Capability:** The system emits a scheduler heartbeat every hour through the worker and logs it.
**Prerequisites:** wave-0. **Parallel-safe with:** landing, feeds (backend `scheduling` package only; Flyway `V1` already contains `shedlock`).
- **In scope:** `@Scheduled(fixedRate = 1h)` job guarded by ShedLock (`lockAtMostFor = 55m`), logs `Scheduler heartbeat tick.` at INFO; missed-run semantics: ShedLock + `initialDelay` computed from the last lock timestamp so at most one catch-up run happens after downtime; runs inside the `api` container (no separate worker container — DEV-6).
- **Acceptance:** contract in `journeys/scheduler-heartbeat/contract.md`; B20 (Postgres-backed lock/state round-trip), B33.
- **Tests:** unit with a mutable `Clock`; integration with Testcontainers asserting one lock row and one log line via a Logback list appender; contract — `db.json` mapping via DEV-5.
- **Deviations:** DEV-5, DEV-6.

### J4 — event-stream (wave-2, group B)

**Capability:** The store owner can watch tracked events arrive live, filter the log, and a replayed event is never shown twice.
**Prerequisites:** wave-1. **Parallel-safe with:** customers (disjoint: `EventRow`, `EventStream`, `FilterChip`, `Segmented`, `EventsToolbar`, backend `tracking` package; customers owns `Table`, `Pagination`, profile components).
- **In scope:** `POST /collect` exact contract; `TrackedEvent` validation with the same PL messages; `event_dedup` insert-or-conflict (24 h TTL, hourly cleanup piggybacking on J3's scheduler); Mercure publish with JWT (`publish: *`) of the JSON event (DEV-3); SPA `EventStream` subscribing to `/.well-known/mercure?topic=/accounts/1/events` with `withCredentials`, `data-stream-state` connecting/live/reconnecting, prepend/cap 80, `data-paused`; `EventsView` with 30 seeded rows and query-string filters (`type`, `site`, `range`) rendered as today (filters are links today — keep them links).
- **Produces:** `EventRow`, `EventStream`, `FilterChip`, `Segmented`; `GET /api/v1/{locale}/events` view-model; Mercure publisher.
- **Acceptance:** oracle `event-stream` steps 1–7; B15–B19, B24; `.event-row` 30, toolbar chips 9 + 4, `.seg [data-action=set-range]` 4; 202 → row `.event-row.new` with `Zakup`/`Hania Kowalska` within 10 s; replay 200 and no second row; 400 cases.
- **Tests:** JUnit for `TrackedEvent` (13 types, PL messages), dedup repository conflict; integration `/collect` 202/200/400 with a mock hub; Vitest for `EventStream` with a fake `EventSource`; contract — full parity for `/collect` bodies and statuses, `db.json` delta (+1 dedup row) via DEV-5; e2e `events.spec.ts` (all four tests).
- **Deviations:** DEV-1, DEV-3, DEV-4, DEV-5.
- **Kill criterion K6:** SSE row not visible within 10 s in 3 consecutive repair rounds → planning revision (hub/edge topology).

### J5 — customers (wave-2, group B)

**Capability:** The store owner can browse customers, open a 360 profile and return to the list.
**Prerequisites:** wave-1. **Parallel-safe with:** event-stream.
- **Produces:** `Table`, `Pagination`, `ProfileFact`, `TimelineItem`, `Tabs`, `Segmented`? (no — event-stream owns `Segmented`; customers' segments use `FilterChip`? — the customers page uses its own segment chips: check `pages/customers.html.twig`; the packet owner records the exact list in its handoff), `AddSlot` not needed. Also owns the `?page=` pagination behaviour and `go-customer`/`go-page` intents.
- **Acceptance:** oracle `customers` steps 1–6; B03, B05, B25: 24 rows, 7 columns, `Strona 1 z 192`, first page button disabled, row click → profile, 7 facts, `VIP` chip, 3 KPIs, 9 timeline items, 5 tabs, back link, `c_9999` → 404 document.
- **Tests:** JUnit fixture `byId` throws → 404 mapping; Vitest `Table` row intent; contract statuses; visual exact (timeAgo strings deterministic under the fixed clock — the API must compute `lastSeen` from the request clock as today); e2e `customers.spec.ts`.
- **Deviations:** DEV-4.

### J6 — automations (wave-3, group C)

**Capability:** The store owner can review automation rules and edit one as a list or a diagram with its simulation.
**Prerequisites:** wave-2. **Parallel-safe with:** settings, campaigns-email-editor (disjoint components: `RulePipeline`, `RbBlock`, `CondRule`, `FlowCanvas`, `FlowNode`, `AutoCard`, `TriggerRow`? (popups owns `TriggerRow`), `Stepper`? (import owns)).
- **Acceptance:** oracle steps 1–7; B26: 6 cards, `?status=` filter chips 4, editor `a1` list (3 `.rb-step`, kicker `KIEDY`, 3 blocks in step 2), `?view=flow` (6 nodes, 5 paths) and back, simulation card `Test reguły` with `2` and `Estymowany przychód`, `[data-action=publish]`, `/automations/new`.
- **Tests:** Vitest for `FlowCanvas` edge count; JUnit fixture `header('new')`; e2e `automations.spec.ts`.

### J7 — settings (wave-3, group C)

**Capability:** The store owner can open each of the eight settings tabs at its own URL.
**Prerequisites:** wave-2 (consumes `Table` from customers, `Chip`, `Callout`). **Parallel-safe with:** automations, campaigns-email-editor.
- **Produces:** `SettingsNav`, `ToggleRow`, `DnsRow`, `HookRow`, `CodeBlock` (server-side highlighting today → the SPA reproduces the same `span.s/.k/.c` markup from a port of `CodeHighlightExtension` in TS, unit-tested against the oracle HTML), `Field` variants, `SwatchGrid`? (popups owns), 30 tab partials.
- **Acceptance:** oracle steps 1–10; B06, B32: 8 tabs with markers, `aria-current=true`, tracker snippet highlighted, 4 DNS rows (3 good, 1 warn), 3 hook rows with `410`, callout `410 od 3 godzin`, notification matrix 8 rows / 24 checkboxes / 15 checked, `/pl/settings` default tab, unknown tab 404.
- **Tests:** Vitest highlight port; contract statuses; e2e `settings.spec.ts`.

### J8 — campaigns-email-editor (wave-3, group C)

**Capability:** The store owner can review e-mail campaigns and open the template editor, whose document keeps literal colours in dark mode.
**Prerequisites:** wave-2 (consumes `Table`, `KpiGrid`, `FilterChip`). **Parallel-safe with:** automations, settings.
- **Produces:** `EditorShell`, `BlockLibrary`, `EmailDocument`, `CouponCode`, `EmailEnvelope`, `Inspector` partials, drag/drop source+target behaviour (no request — parity with the 404 today means: no call at all; DEV-7).
- **Acceptance:** oracle steps 1–8; B27, B28: KPIs 4, filters 4, 5 rows, last row `—`, row click → `/pl/emails/k1`, 10 library buttons, canvas text, `.doc-hero`, coupon `WROCMY-A8F2`, 4 products, footer `Wypisz się`, inspector `Warunki widoczności`, `hero_title` value, `.ee-doc` background `rgb(255, 255, 255)` in dark theme, `/emails/new`.
- **Tests:** Vitest computed-style test for `.ee-doc` under `data-theme=dark`; e2e `lists.spec.ts -g campaigns` and `editors.spec.ts -g "email editor"`.
- **Deviations:** DEV-7.

### J9 — popups-widget-editor (wave-4, group D)

**Capability:** The store owner can preview on-site widgets and compose one for desktop or mobile.
**Prerequisites:** wave-3 (consumes `EditorShell`, `BlockLibrary`, `Segmented`). **Parallel-safe with:** import-wizard, dashboard.
- **Produces:** `PopupStage`, `PopupWidget`, `ShopMock`, `PositionGrid`, `SwatchGrid`, `TriggerRow`, `AddSlot`, `CheckboxRow`, `PwType` list.
- **Acceptance:** oracle steps 1–7; B29: 5 cards, stage `data-type=modal`, `?preview=p2` → banner, editor `p1` modal/desktop with title `Zostań na 10% taniej`, 5 types, `?type=banner` shows `Od 199 zł`, viewport chip `1440 × 900` → `390 × 844` with `?device=mobile`, inspector 3 triggers / 9 cells / 1 checked / 4 checkbox rows, `/popups/new`.
- **Tests:** e2e `lists.spec.ts -g widgets`, `editors.spec.ts -g "popup editor"`.

### J10 — import-wizard (wave-4, group D)

**Capability:** The store owner can walk the four-step customer import and upload a file that advances the wizard.
**Prerequisites:** wave-3 (consumes `CondRule` from automations, `KpiGrid`, `Table`). **Parallel-safe with:** popups-widget-editor, dashboard.
- **In scope:** `POST /import/upload` multipart → stores `var/import/<32 hex>.<ext>` (extension whitelist `[a-z0-9]{1,8}`, default `csv`), session keys for name/path, 302 to `/{locale}/import/2`; SPA `Dropzone` posts with `fetch` and follows the redirect (as `upload.ts` does today); steps 1–4 views, `Stepper`, `MapRow`, validations list, dedup radios, summary, recent imports; unknown step 404.
- **Produces:** `Stepper`, `Dropzone`, `MapRow`, `FilePill`, wizard partials.
- **Acceptance:** oracle steps 1–10; B31.
- **Tests:** JUnit path-traversal test (`../../x.sh` → random name, ext `sh` allowed only if whitelisted pattern matches — reproduce exactly); contract: 302 target, `db.json` delta (+1 file in `var/import` inside the api container); e2e `import.spec.ts`.
- **Deviations:** DEV-4.

### J11 — dashboard (wave-4, group D)

**Capability:** The store owner can see the operational overview and jump from it into a customer profile or a rule.
**Prerequisites:** wave-3 (consumes `EventStream`, `KpiGrid`, profile and editor routes). **Parallel-safe with:** popups-widget-editor, import-wizard.
- **Produces:** `Cardiogram` (canvas, redraw on theme change, listens to `kivvi:event`), `ListCard`, recent customers / top automations partials, `pause-stream` intent shared with events (already in `EventStream`).
- **Acceptance:** oracle steps 1–4; B23; also closes DEV-11 (login steps 4 and 8 unmasked from this wave on).
- **Tests:** Vitest for `Cardiogram` redraw on `data-theme` mutation; e2e `dashboard.spec.ts`; visual exact except DEV-2 mask.
- **Deviations:** DEV-1, DEV-2, DEV-4.

### J12 — shell-navigation (wave-5, single)

**Capability:** The store owner can move between every panel section and keep sidebar, theme and locale choices across reloads.
**Prerequisites:** wave-4 (every page exists). **Parallel-safe with:** none.
- **In scope:** verification-heavy journey: all 21 oracle steps exact; only-`.main-scroll`-scrolls layout invariant; breadcrumb; `open-command-bar` intent and Ctrl/Cmd+K dispatch; any residual chrome gap found by the full oracle.
- **Acceptance:** oracle `shell-navigation` steps 1–21; B01 (all rows), B02, B21.
- **Tests:** e2e `navigation.spec.ts` (all), plus `PanelPagesTest` equivalents as JUnit `@ParameterizedTest` over the 25 URLs asserting 200 and the resolved view-model marker text.
- **Deviations:** DEV-4.

## Accepted deviations

| ID | Journey and step | Dimension | Description | Reason | Verifier mask | Owner | Expires |
| --- | --- | --- | --- | --- | --- | --- | --- |
| DEV-1 | event-stream all steps, dashboard all steps, customers (none) | visual | absolute clock strings `HH:MM:SS` in `.event-row__time` differ between capture and verification | server clock, not frozen | screenshot mask `.event-row__time`; text rule `\d{2}:\d{2}:\d{2}` → `<HH:MM:SS>` (`normalize.json`) | Piotr Mucha | never (inherent) |
| DEV-2 | dashboard step 1, login steps 4/8 (after DEV-11 expires) | visual | cardiogram canvas pixels depend on random events/second | canvas is driven by randomness | mask `#cg-main, .cardiogram-canvas` | Piotr Mucha | never (inherent) |
| DEV-3 | event-stream steps 2–3 | contract | Mercure `data` payload is a JSON event instead of `{html}` | the SPA renders rows from data | compare topic, event count and the fields `type`, `detail`, `customerName`, `customerId`, `siteName`; ignore payload shape | Piotr Mucha | contract stage (becomes the contract) |
| DEV-4 | every journey, document requests | contract | page `GET` requests return the SPA shell and the data arrives via `GET /api/v1/...`; HTML bodies are not compared | backend-swap to SPA | for `kind=document` compare method, path, status and final URL only; full parity for `/collect`, `/preferences/*`, `/import/upload` (status + `Location`), `/{locale}/login` (status + `Location`), `/{locale}/logout`; API calls recorded per wave as the new baseline | Piotr Mucha | contract stage |
| DEV-5 | event-stream, scheduler-heartbeat, login, import-wizard (`db.json`) | contract | table names differ: `sessions`→`spring_session`, `cache_items` dedup keys→`event_dedup`, scheduler state→`shedlock`, `messenger_messages`→none | different frameworks' storage | compare deltas after mapping; `messenger_messages` delta is always 0 in the oracle and has no counterpart | Piotr Mucha | contract stage |
| DEV-6 | scheduler-heartbeat | contract/e2e | no separate `worker` container; the job runs inside `api` | Spring scheduling + ShedLock needs no consumer process | topology check accepts one process | Piotr Mucha | contract stage |
| DEV-7 | campaigns-email-editor, popups-widget-editor | contract | the SPA editor makes no `POST …/blocks` call (today the call 404s) | dead call today (R6) | ignore absence of a 404 request in HTTP recording | Piotr Mucha | contract stage |
| DEV-8 | all journeys | e2e (performance) | bundle-size/LCP/TTI compared to absolute budgets (≤ 300 kB gzip initial JS, LCP ≤ 2.0 s, TTI ≤ 2.5 s on localhost) instead of ±20 % of the importmap baseline | baseline is ~80 kB of sources with no bundler; Lighthouse baseline not captured | budget file `performance/budget.json` (introduced wave-0) | Piotr Mucha | contract stage (re-baseline on the new stack) |
| DEV-9 | login, shell-navigation (session) | contract | session cookie name `SESSION` instead of `PHPSESSID`; sessions not migrated at cutover | Spring Session default; no real users | ignore `Set-Cookie` name; sessions delta compared by count only | Piotr Mucha | cutover |
| DEV-10 | (none — `/_storybook` Out of scope) | — | recorded for completeness: storybook not reproduced | dev tooling | — | Piotr Mucha | n/a |
| DEV-11 | login steps 4 and 8 | visual | dashboard page body not implemented until wave-4 | walking skeleton lands on the dashboard | mask `.main-scroll` region on these two steps | Piotr Mucha | wave-4 `WAVE_INTEGRATED` (dashboard) |

## Kill criteria

Defaults from `wave-design.md`:

- K1: three whole-wave repair rounds without `WAVE_PARITY_GREEN` (exit: `migration-planning` revision);
- K2: a required capability missing from the target stack with no accepted deviation (exit: `research-spike`);
- K3: the oracle is invalidated — manifest hash mismatch (exit: recapture and re-approval);
- K4: a performance regression above the DEV-8 budget that two repair rounds do not reduce (exit: planning revision).

Added:

- K5 (wave-0): visual parity on login steps 1–3 cannot reach ≤ 0.5 % after 3 rounds → SFC markup strategy is wrong (exit: planning revision: consider server-rendered shell or different whitespace strategy).
- K6 (wave-2): SSE row not visible within 10 s after `202` in 3 consecutive rounds (exit: planning revision on edge/hub topology).
- K7 (wave-4/5): host free disk < 3 GB or a worktree cannot be provisioned (exit: `BLOCKED_EXTERNAL`; operator frees space; no sharing of stacks).
- K8 (any): a journey needs a change to `tests/e2e/specs/*` to pass (exit: planning revision — the spec is the oracle).

Repair budget: three whole-wave repair rounds per wave.

## Verifier contract

| Dimension | Applicability | Command (confirmed or introduced) | Threshold | Evidence path |
| --- | --- | --- | --- | --- |
| unit | required | `cd backend && ./mvnw -q test` (introduced); `cd frontend && npm run test -- --run` (introduced) | 0 failures; every behaviour in `behaviours.json` for the wave's journeys has a named test (`@DisplayName("Bnn …")` / `describe("Bnn …")`) | `<run>/waves/<wave>/unit.md` |
| integration | required | `cd backend && ./mvnw -q verify -Pintegration` (Testcontainers Postgres 18) (introduced); `cd frontend && npm run test:integration` (router + pinia + components) (introduced) | 0 failures | `<run>/waves/<wave>/integration.md` |
| architecture | required | `cd backend && ./mvnw -q test -Dtest=ArchitectureTest` (ArchUnit) and `cd frontend && npm run lint` (ESLint boundary rules) (introduced) | 0 violations; every row of `rules-translated.md` for the wave has a rule | `<run>/waves/<wave>/architecture.md` |
| contract | required (leading) | `node context/migration-oracle/symfony-to-spring-vue/capture/compare.mjs --journey <id> --base https://localhost:<port> --dimension contract` (introduced wave-0) | 0 unmasked differences after `normalize.json` + DEV masks; `db.json` deltas equal after DEV-5 mapping | `<run>/waves/<wave>/contract.md` + `contract/<journey>/step-<n>.diff.json` |
| visual | required on every SPA step (no server-rendered pages remain) | `… compare.mjs --dimension visual` (screenshots via `pixelmatch`-equivalent from Playwright `toHaveScreenshot` engine; a11y via `ariaSnapshot`; texts) | ≤ 0.5 % pixels after antialiasing tolerance and masks; a11y tree and texts exact | `<run>/waves/<wave>/visual.md` + `visual/<journey>/step-<n>/{diff.png,a11y.diff,texts.diff}` |
| e2e | required | `cd tests/e2e && E2E_BASE_URL=https://localhost:<port> npx playwright test <journey specs>` (confirmed command, new base URL) + `… compare.mjs --dimension performance` (DEV-8 budgets; `/collect` p95 over 50 requests) | all tests green; budgets met | `<run>/waves/<wave>/e2e.md` + Playwright report |

Extra exit conditions: none for either kind.

## Execution topology and gate contract

| Concern | Repository-proven value | Evidence | Execution consequence |
| --- | --- | --- | --- |
| Approved base and integration target | `main` at the approved SHA; feature branch `migration/spring-vue` | `CLAUDE.md` workflow (trunk-based) | waves integrate into `migration/spring-vue`; PR to `main` at CUTOVER_READY |
| Feature branch and merge policy | short-lived branches, squash to `main`, Conventional Commits, no AI trailers | `CLAUDE.md`, `~/.claude/CLAUDE.md` | journey branches `migration/<wave>/<journey>` squash into the feature branch |
| Required PR checks | `Docker Build` workflow only (builds PHP image) | `.github/workflows/docker-build.yml` | wave-0 adds `next-build.yml` (mvnw verify, npm build) — must be green on the PR head |
| Staging | none exists | `README.md` | rollback rehearsal packet RR-1 uses a second compose project on this host as staging |
| Production target | this host, `compose.prod` on port 23456 behind the external proxy | `README.md`, memory `kivvi-prod-stack` | CUT-1 packet; no inferred authority |
| Focused tests | see verifier contract | — | per journey |
| Static analysis | `./mvnw spotless:check`, `npm run lint`, `npm run typecheck` (introduced) | — | per journey |
| Dependency, secret and license scanning | none configured; diff-level secret check by reviewer; `npm audit` and `./mvnw dependency-check` not configured | `composer audit` exists for the old stack only | reviewer checks introduced dependencies and compose secrets; AGPL note for Mercure recorded |
| Sonar | not configured | no config/CI | recorded as not configured |
| Real-surface E2E | Playwright headless system Chrome (`tests/e2e`) | `README.md`, `playwright.config.ts` | headless is the repository rule (server without display) |
| Full feature verification | final all-journey cohort + full `npx playwright test` + `./mvnw verify` + `npm run build` | migration profile | before CUTOVER_READY |

### Worktree resource isolation

| Resource | Isolation or serialization rule | Evidence |
| --- | --- | --- |
| Application/edge ports | pool `19000 + 10·n` per journey lease; oracle stack fixed at 18080/18443; prod 23456/23457 untouched | `ss -ltnp` (8080/8443/5432 already used by other projects) |
| Database | one Postgres container per compose project, random published port | `compose.override.yaml` pattern |
| Container/project namespace | `kivvi-w-<journey>`; never the default project name (prod) | `docker compose ps` (prod is the default project) |
| Mercure JWT / topics | per-project secret; topics identical (`/accounts/1/events`) — isolated by hub instance | `compose.yaml` |
| Cache/temp | Maven `-Dmaven.repo.local` per worktree for writes, `~/.m2` read cache; `node_modules` per worktree; Vite cache in worktree | host has no `~/.m2` yet |
| Browser profile / downloads | Playwright context per run; downloads to worktree `tests/e2e/test-results` | `playwright.config.ts` |
| Test accounts / fixtures | none (fixtures are code); upload directory inside the api container | — |
| Sonar keys | not configured | — |
| Disk | ≤ 2 concurrent journeys; K7 | `df -h` |

### Authorization boundaries

Local implementation in worktrees: expected once the operator confirms the execution decision. Remote push / PR to `main`: separate confirmation. Integration merge to `main`: separate. RR-1 (staging-like second stack on this host): separate. CUT-1 (production swap on port 23456): separate, including the decision on the currently stopped old database. CON-1 (delete PHP stack): separate. Data migration: not applicable. This plan authorizes none of them.

## Dependency and concurrency audit

DAG: J0 → {J1, J2, J3} → {J4, J5} → {J6, J7, J8} → {J9, J10, J11} → J12 → final cohort → CUT-1 → RR-1 → (window) → CON-1.

- Group A (J1 ∥ J2 ∥ J3): disjoint Vue components (landing vs feeds), disjoint backend packages (`landing`, `feeds`, `scheduling`), no shared generated outputs except `frontend/src/router/routes.ts` — each journey adds its own route file under `frontend/src/router/routes/<journey>.ts` auto-collected, so no shared edit; Flyway: only J3 adds nothing (baseline already has `shedlock`).
- Group B (J4 ∥ J5): `EventRow/EventStream/FilterChip/Segmented` vs `Table/Pagination/Profile*`; backend `tracking` vs `customers`; both consume `KpiGrid` (J2) read-only.
- Group C (J6 ∥ J7 ∥ J8): rule/flow components vs settings components vs editor components; `CondRule` owned by J6 and consumed by J10 later; `EditorShell` owned by J8 and consumed by J9 later.
- Group D (J9 ∥ J10 ∥ J11): popup components vs wizard components vs cardiogram/list-card; J11 consumes J4's `EventStream` read-only.
- Integration order inside a wave: alphabetical by journey id, one at a time under the integration lock; each integrated journey refreshes the others' parent SHA.
- Concurrency cap: 2 (disk). With three journeys in a group, the third starts when the first integrates.

## Delivery stages

| Packet | Stage | Scope | Actor-visible change | Invariant | Operational proof | Rollback | Entry and exit | Authorization |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| RR-1 | rollback rehearsal | bring the new stack up as `kivvi-stage` on ports 23458/23459 from the assembled feature SHA with a copy of `.env.prod.docker` values (names only reused); run the full e2e suite and the all-journey cohort against it; then tear it down and prove the old prod compose can be brought back (`compose up -d database php worker` on the old files) | none | frozen contract | cohort green on the stage stack; old stack `docker compose ps` healthy after restore | n/a | enter: CUTOVER_READY; exit: evidence stored | Piotr Mucha |
| CUT-1 | cutover | on this host: `docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml down` (old), then `docker compose -p kivvi-click --env-file .env.prod.docker -f compose.next.prod.yaml up -d --build --wait` publishing port 23456 (api via mercure edge) and 23457; external proxy unchanged | https://kivvi.click serves the SPA | frozen contract; port 23456; `X-Forwarded-*` trusted | `curl -I http://localhost:23456/pl` 200, SSE probe, full e2e against https://kivvi.click (public) | `down` new, `up` old (old images retained; `database_data_prod` volume untouched because the new stack uses a new volume `database_data_next`) | enter: RR-1 done, owner decided the old-prod DB question; exit: 7-day observation window starts | Piotr Mucha |
| CON-1 | contract | delete `src/`, `templates/`, `assets/`, `translations/`, `migrations/`, `composer.*`, `symfony.lock`, `importmap.php`, `.pnp.cjs`, `yarn.lock`, `package.json` (root), `Dockerfile`, `frankenphp/`, `compose.yaml`, `compose.override.yaml`, `compose.prod.yaml`, `phpunit.dist.xml`, `phpstan.neon.dist`, `.php-cs-fixer.dist.php`, `config/`, `public/`, `bin/`, `tests/**/*.php`, `.github/workflows/docker-build.yml`; rename `compose.next*.yaml` → `compose*.yaml`; rewrite stack sections of `CLAUDE.md`, `AGENTS.md`, `README.md`, `.claude/skills/product-spec/SKILL.md`; refresh `context/map` (`project-context-initializer` refresh); re-point the 46 Linear issues' "Punkt startu w repo" (task-closeout) | none | contract holds; `tests/e2e` unchanged and green | zero requests to old-only paths during the window (edge access log); full e2e green after deletion | git revert of the contract commit | enter: window closed clean; exit: old stack removed, docs refreshed | Piotr Mucha |

## Implementation-orchestrator handoff

- **Canonical plan identity:** `context/plans/2026-09-08-symfony-to-spring-vue-migration.md`; SHA-256 recorded in `context/map/manifest.json` (`related_artifacts`); source revision `5b806ac` (product `91f8f85`); approval status: approved (2026-09-08).
- **Project-context entrypoint:** `context/map/INDEX.md`, `context/map/manifest.json` (freshness complete for `5b806ac`; refresh after this plan is committed).
- **Oracle:** path above; manifest SHA-256 above; `source_sha` in manifest must equal `5b806ac`.
- **Execution DAG:** J0 → {J1,J2,J3} → {J4,J5} → {J6,J7,J8} → {J9,J10,J11} → J12; cohorts A–D with cap 2; integration order alphabetical.
- **Feature integration:** base `main@<approved SHA>`, feature branch `migration/spring-vue`, journey branches `migration/<wave>/<journey>`, squash merges, post-integration check = wave cohort.
- **Gate coverage:** per journey the full `standard` chain; per wave the six-dimension cohort; final all-journey cohort + build/static/e2e; Sonar not configured.
- **Final delivery:** `CUTOVER_READY` → RR-1 → CUT-1 → window → CON-1, each separately authorized.
- **Expected authorization envelope:** local implementation in worktrees + local compose stacks on the port pool; no push, no prod action.
- **Reconciliation record:** produced after the plan hash is computed (see handoff message).

## Execution recommendation

| Decision | Recommendation | Evidence and trade-off |
| --- | --- | --- |
| Profile | `migration` (only valid profile) | plan produced by `migration-planning`; oracle captured |
| Orchestration | `fire-and-forget` | single operator, long run; pauses only for authority (push, RR-1, CUT-1, CON-1) |
| Terminal outcome | `local-green` for the run (assembled `migration/spring-vue` passes the final cohort and gates, CUTOVER_READY recorded); `ready-pr` requires separate push authority | no remote CI for the new stack exists yet |
| Model policy | `daily-coding` defaults: workers and reviewer on the current general-purpose coding tier (Claude Code: `sonnet` alias resolved by the host), medium reasoning | routine porting work; escalate only on repeated cohort failure |
| Automatic escalation | not applicable | `migration` takes part in no transition |

### Quick-profile eligibility

- **Verdict:** not eligible — migration profile.

### Operator execution decision

**Status:** confirmed (operator, 2026-09-08)

- **Selected profile:** migration (fixed)
- **Selected orchestration:** fire-and-forget
- **Terminal outcome:** local-green — the assembled `migration/spring-vue` branch passes the final all-journey cohort and the combined gate chain; `CUTOVER_READY` recorded. The operator additionally asked for the new stack to run on the same port (23456) at the end for personal verification: that is packet CUT-1 and is executed only after `CUTOVER_READY` and one explicit confirmation; the old stack stays available for rollback.
- **Selected worker/reviewer model policy:** daily-coding tier (Claude Code `sonnet` alias resolved by the host) for worker and reviewer, **reasoning effort high** (operator override of the medium default)
- **Automatic profile escalation:** not applicable
- **Challenge raised:** parallel rewrite instead of strangler (see "Migration kind and trigger"); production is down and its database has no restart policy — CUT-1 entry condition; disk headroom caps parallelism at 2.
- **Operator override:** reasoning effort high for worker and reviewer (cost accepted); CUT-1 pre-signalled as desired end state (still separately confirmed)
- **Implementation authorization:** granted 2026-09-08 for local implementation in isolated worktrees and local compose stacks on the port pool; no push, no PR, no production action inside the envelope; RR-1/CUT-1/CON-1 need their own confirmation

## Coverage matrix

| Requirement, happy-path step, edge case, or risk | Journey | Focused test | Verification evidence |
| --- | --- | --- | --- |
| B01–B14, B21, B22 (login) | J0, J12 | JUnit shell/session tests, Vitest shell components, `PanelPagesTest` port | oracle login/shell-navigation steps; e2e navigation/public |
| B22 (landing) | J1 | Vitest landing snapshot | oracle landing; e2e public |
| B30 | J2 | Vitest FeedCard | oracle feeds; e2e lists |
| B20, B33 | J3 | JUnit clock/lock tests | contract.md; db delta |
| B15–B19, B24 | J4 | JUnit TrackedEvent/dedup; Vitest EventStream | oracle event-stream; e2e events |
| B03, B05, B25 | J5 | JUnit 404 mapping; Vitest Table | oracle customers; e2e customers |
| B26 | J6 | Vitest FlowCanvas | oracle automations; e2e automations |
| B06, B32 | J7 | Vitest highlight port | oracle settings; e2e settings |
| B27, B28 | J8 | Vitest `.ee-doc` colour | oracle campaigns; e2e lists/editors |
| B29 | J9 | Vitest stage attributes | oracle popups; e2e lists/editors |
| B31 + upload traversal | J10 | JUnit upload storage | oracle import; e2e import |
| B23 | J11 | Vitest Cardiogram | oracle dashboard; e2e dashboard |
| `/collect` p95 < 500 ms | J4 | — | e2e performance run |
| JS budget (DEV-8) | J0, every wave | — | `compare.mjs --dimension performance` |
| Mercure AGPL usage | J0 | — | reviewer note; unmodified image tag pinned |

## Accepted risks

- Production outage (R17) was repaired on 2026-09-08 (database restarted, restart policy added). Residual risk: no monitoring until PIO-112. Owner: Piotr Mucha.
- Mercure hub is AGPL-3.0; used unmodified as a network service. Owner: Piotr Mucha. Revisit if the hub is ever modified.
- Node 26 LTS status and Spring Boot OSS support dates are from secondary sources or unknown; verify before CUT-1. Owner: maintainer.
- Disk headroom 11 GB; parallelism capped at 2; K7. Owner: Piotr Mucha.
- The 46 Linear tasks stay Symfony-worded until CON-1 re-points them; no task should start before then. Owner: Piotr Mucha.
- Visual parity of an SPA against server-rendered pages is proven only in wave-0 (K5); the whole plan depends on it.

## Approval

Requesting plan approval. On approval the operator answers one compact execution question (orchestration mode, terminal outcome, model policy) and the run starts under `implementation-orchestrator` in the `migration` profile.
