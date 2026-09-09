<!-- BEGIN project-context-initializer:artifact -->
# Technology

Columns: declared (manifest range), locked (lockfile), detected (usage), verified (executed in
this run). Decision state `Decided` = documented in `CLAUDE.md`/`AGENTS.md`/the migration plan;
"not-run" here means not executed by this context refresh (the migration run itself has executed
most of these — see `delivery-and-verification.md`).

## Old stack (Symfony) — unchanged since `91f8f85`, still serves production

| Technology | Role | Declared | Locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PHP | Language | `>=8.5` (`composer.json`) | image `dunglas/frankenphp:1-php8.5` (`Dockerfile`) | `src/**` | Observed | Decided — being replaced (R1/R19) | not-run |
| Symfony | Framework | `8.0.*` | framework-bundle v8.0.11, messenger v8.0.12, runtime v8.0.12 | `config/**`, `src/Kernel.php` | Observed | Decided — being replaced (R1/R19); 8.0 unmaintained since 2026-07-31 (R16) | not-run |
| FrankenPHP + Caddy | Runtime, worker mode, Mercure hub, Vulcain | `Dockerfile`, `frankenphp/Caddyfile` | image tag `1-php8.5` (floating minor) | compose services `php`, `worker` | Observed | Decided | not-run |
| PostgreSQL | Only backing service: data, sessions, cache, messenger, scheduler state | `POSTGRES_VERSION=18` (`.env`, `.env.example`) | image `postgres:${POSTGRES_VERSION:-16}-alpine` (`compose.yaml`) | `config/packages/{doctrine,cache,messenger,framework}.yaml`, `migrations/` | Observed | Decided | not-run |
| Doctrine ORM / DBAL / Migrations | Persistence | orm `^3.6`, doctrine-bundle `^3.2`, migrations-bundle `^4.0` | orm 3.6.6, dbal 4.4.3, migrations-bundle 4.0.0 | no entities yet; one infra migration | Observed | Decided | not-run |
| Symfony Messenger (Doctrine transport) | Async queue | `8.0.*` | v8.0.12 | `config/packages/messenger.yaml`, `compose.yaml` worker | Observed | Decided | not-run |
| Symfony Scheduler | Cron-like jobs | `8.0.*` | v8.0.11 | `src/Schedule.php` (hourly `Heartbeat`) | Observed | Decided | not-run |
| Mercure (bundle + Caddy module) | Real-time SSE | mercure-bundle `^0.4.2` | v0.4.2 | `src/Tracking/EventIngestion.php`, `assets/controllers/event-stream.ts`, `Caddyfile` (`anonymous`, `subscriptions`) | Observed | Decided | not-run |
| Twig + twig/extra-bundle | Server rendering | `^3.27.1` | twig v3.27.1 | `templates/**` (atoms/molecules/organisms/pages) | Observed | Decided — being replaced by Vue SFCs | not-run |
| Symfony AssetMapper + sensiolabs/typescript-bundle | Asset pipeline, TS compile | asset-mapper `8.0.*`, typescript-bundle `^0.2.2` | asset-mapper v8.0.11 | `importmap.php`, `frankenphp/docker-entrypoint.sh` (`typescript:build`) | Observed | Decided | not-run |
| TypeScript (vanilla, no framework) | Frontend language | `^6.0.3` (`package.json`) | 6.0.3 (`yarn.lock`) | `assets/app.ts`, `assets/controllers/*.ts` | Observed | Decided — being replaced by Vue 3 | not-run |
| Yarn | JS package manager, PnP | `yarn@4.14.1` | `.pnp.cjs` committed | root only; e2e uses npm | Observed | Decided | not-run |
| PHPStan + phpstan-symfony | Static analysis level 5 | `^2.2` / `^2.0` | 2.2.2 | `phpstan.neon.dist` | Observed | N/A | not-run |
| PHPUnit | Unit + functional tests | `^13.1` | 13.1.10 | `tests/**`, `phpunit.dist.xml` (fail on deprecation/notice/warning) | Observed | N/A | not-run |
| Docker Compose | Dev/prod orchestration | `compose.yaml` + `compose.override.yaml` (dev) / `compose.prod.yaml` | — | prod containers running on this host (`docker ps`, 2026-09-09) | Observed | Decided | verified (ps only) |
| GitHub Actions | CI: build prod image on push to `main` | `.github/workflows/docker-build.yml` | actions checkout@v4, buildx@v3, build-push@v6 | push only, `push: false` | Observed | Decided | not-run |

Compatibility notes for the old stack are unchanged from the prior refresh: `composer.json`
replaces all `symfony/polyfill-*`; PHP-CS-Fixer runs with `setUnsupportedPhpVersionAllowed(true)`;
`compose.yaml` needs `POSTGRES_VERSION=18` matched to the volume layout; prod image is
`debian:13-slim`; `tests/e2e` is an isolated npm project. Configuration key names: unchanged (see
prior section below, still accurate — Observed).

## Next stack (Spring Boot + Vue) — `backend/`, `frontend/`, `mercure/`, `tools/migration-verify/`

Version pins are from the migration plan's "Target stack" table (retrieved 2026-09-08 from GitHub
releases / Maven Central BOM / Adoptium / endoflife.date) and confirmed against `backend/pom.xml`
and `frontend/package.json` in this worktree (Observed).

| Technology | Role | Declared/locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- |
| Java (Temurin) | Language / runtime | `25` (`pom.xml` `<java.version>`), pinned toolchain `/home/muszkin/.cache/kivvi-toolchains/jdk-25` (host has no system Java 25) | `backend/src/main/java/**` | Observed | Decided | verified (`./mvnw` gates run by the migration workers — see `run.json`) |
| Spring Boot | Backend framework | `4.1.1` (`pom.xml` parent) | `backend/pom.xml`, every `@SpringBootApplication`/starter | Observed | Decided | verified |
| Spring Web MVC (Tomcat) | HTTP layer | bundled by `spring-boot-starter-web` | `backend/.../web/*Controller.java` | Observed | Decided | verified |
| Jackson 3 | JSON | bundled by Spring Boot 4 BOM | `web/dto/*Response.java` serialization | Observed | Decided | verified |
| Spring Session JDBC | Session store (Postgres-backed) | `spring-boot-starter-session-jdbc` | `application.yml` (`spring.session.store-type: jdbc`), `V1__baseline.sql` (`SPRING_SESSION*`) | Observed | Decided | verified |
| Spring Data / Flyway | Schema migration | `spring-boot-starter-flyway` + `flyway-database-postgresql` | `backend/src/main/resources/db/migration/V1__baseline.sql` | Observed | Decided; schema frozen at V1 until CON-1 | verified |
| PostgreSQL JDBC driver | DB connectivity | `org.postgresql:postgresql` (runtime scope) | `pom.xml` | Observed | Decided | verified |
| ShedLock (`shedlock-spring` + `shedlock-provider-jdbc-template`) | Distributed scheduler lock | `7.10.0` | `pom.xml`, `infrastructure/scheduling/SchedulingConfig.java` | Observed | Decided | verified |
| ArchUnit | Architecture tests | `archunit-junit5` `1.5.0` | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` | Observed | Decided | verified |
| Testcontainers (Postgres) | Integration test DB | `spring-boot-testcontainers` + `testcontainers-postgresql` | `backend/src/test/java/click/kivvi/*ApiIT.java` | Observed | Decided | verified |
| Maven (wrapper) | Build tool | `backend/mvnw`, `.mvn/wrapper` | project root of `backend/` | Observed | Decided (no Gradle on host) | verified |
| Spotless + google-java-format | Java formatting, bound to `verify` | `spotless-maven-plugin` `3.10.2`, `google-java-format.version` `1.36.1` | `pom.xml` `<build><plugins>` | Observed | Decided | verified |
| Vue 3 | Frontend framework (SPA, no SSR) | `3.5.42` | `frontend/package.json`, `frontend/src/**` | Observed | Decided | verified |
| vue-router | SPA routing, history mode | `5.3.1` | `frontend/src/router/*.ts` | Observed | Decided | verified |
| Pinia | State store | `4.0.3` | `frontend/src/stores/shell.ts` | Observed | Decided | verified |
| vue-i18n | i18n (PL default, EN toggle) | `11.4.10` | `frontend/src/i18n/**` | Observed | Decided | verified |
| Vite | Build/dev server | `8.2.2` (+ `@vitejs/plugin-vue` `6.0.8`) | `frontend/vite.config.ts` | Observed | Decided | verified |
| Vitest + @vue/test-utils | Unit/integration tests | `5.0.0` / `2.5.0` | `frontend/test/{unit,integration}/**` | Observed | Decided | verified |
| vue-tsc | Typecheck | `3.3.11` | `frontend/package.json` `typecheck` script | Observed | Decided | verified |
| TypeScript | Frontend language | `6.0.3` (kept, existing pin) | `frontend/tsconfig.json` | Observed | Decided | verified |
| ESLint + eslint-plugin-vue + typescript-eslint | Linting, boundary rules | `10.10.0` / `10.11.0` / `8.70.0` | `frontend/eslint.config.js` | Observed | Decided | verified |
| Prettier | Formatting | `3.8.4` (kept, existing pin) | `frontend/package.json` `format`/`format:check` | Observed | Decided | verified |
| dunglas/mercure (hub image) | Real-time SSE, stack edge | `v0.24.2` | `mercure/Caddyfile` mounted into the container, `compose.next.yaml` `mercure` service | Observed | Decided; AGPL-3.0, used unmodified as a network service | verified |
| PostgreSQL 18 | Only backing service | `postgres:18-alpine` | `compose.next.yaml` `database` service | Observed | Decided (unchanged from old stack) | verified |
| Docker Compose (`compose.next.yaml` / `compose.next.prod.yaml`) | Dev/verification/prod-shape orchestration | — | root of repo | Observed | Decided; prod overlay delivered but not deployed | verified (dev/verification) |
| GitHub Actions (`next-build.yml`) | CI: `mvnw verify` (backend, with SPA built in) + `npm run {typecheck,lint,format:check,test,test:integration,build}` (frontend) | on push/PR touching `backend/**`, `frontend/**`, `compose.next*.yaml`, `mercure/**`, `tools/migration-verify/**` | `.github/workflows/next-build.yml` | Observed | Decided | not-run in this context refresh (workflow inspected only) |
| pixelmatch + pngjs | Screenshot diffing for `compare.mjs` | `7.2.0` / `7.0.0` | `tools/migration-verify/package.json` | Observed | Decided | verified |
| Playwright (reused) | E2E + verifier browser automation | `1.62.1` (kept — `tests/e2e/package-lock.json`), reused via `createRequire` in `compare.mjs` | `tools/migration-verify/compare.mjs` | Observed | Decided | verified |

## Compatibility notes — next stack

- Node 26 LTS status and Spring Boot 4.1.1 OSS-support end date are recorded in the plan from
  secondary sources — "verify before CUT-1" (plan §"Accepted risks", Inferred).
- `frontend/eslint.config.js` enforces two architectural rules mechanically: no client-side number
  formatting (`Intl`/`toLocaleString`/`toFixed` banned outside three governed exceptions) and no
  client-side Mercure topic construction (`/accounts/` literal banned outside the same three files)
  — see `backend/.agents/project-context.md` and `frontend/.agents/project-context.md` for the
  full invariant text.
- `backend/pom.xml`'s `maven-compiler-plugin` runs `-Xlint:all -Werror` — every Java compiler
  warning is a build failure, the plan's declared equivalent of the old stack's PHPStan level 5.
- Fixtures are implemented as Java classes (`backend/src/main/java/click/kivvi/fixtures/*.java`),
  not the JSON resources the plan's decision ledger described — accepted drift, see R20 in
  `risks-and-unknowns.md`.

## Configuration key names (values not recorded)

Old stack `.env`: `APP_ENV`, `APP_SECRET`, `APP_SHARE_DIR`, `DEFAULT_URI`, `TRUSTED_PROXIES`,
`TRUSTED_HOSTS`, `DATABASE_URL`, `POSTGRES_VERSION`, `MESSENGER_TRANSPORT_DSN`, `MERCURE_URL`,
`MERCURE_PUBLIC_URL`, `MERCURE_JWT_SECRET`. `.env.prod.docker.example` (old prod compose env):
`SERVER_NAME`, `HTTP_PORT`, `HTTPS_PORT`, `HTTP3_PORT`, `CADDY_MERCURE_PUBLIC_URL`,
`CADDY_MERCURE_JWT_SECRET`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

Next stack (`compose.next.yaml`, `compose.next.prod.yaml`, `backend/src/main/resources/application.yml`
— names only, not read): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `MERCURE_PUBLISHER_JWT_KEY`, `MERCURE_SUBSCRIBER_JWT_KEY`,
`MERCURE_JWT_SECRET`, `MERCURE_URL` / `MERCURE_INTERNAL_URL` (note the deliberate substitution-
variable rename to avoid Symfony Flex's own `.env` shadowing `MERCURE_URL` — see `compose.next.yaml`
comment), `TRUSTED_PROXIES`, `SERVER_NAME`, `HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT`, `POSTGRES_DB`/
`POSTGRES_USER`/`POSTGRES_PASSWORD`, `IMAGES_PREFIX`, `KIVVI_IMPORT_UPLOAD_DIRECTORY`.

Secret-bearing files present locally and ignored, not read: `.env.local`, `.env.prod.docker`,
`.ai/cezar/launch-key`.
<!-- END project-context-initializer:artifact -->
