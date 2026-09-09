<!-- BEGIN project-context-initializer:artifact -->
# Technology

Columns: declared (manifest range), locked (lockfile), detected (usage), verified (executed in
this run). Decision state `Decided` = documented in `CLAUDE.md`/`AGENTS.md`. "not-run (this
refresh)" means this context refresh did not execute the command itself; a prior verification
did (migration run's own gates, or the separate `cutover/con1-e2e.md` read-only check) — cited
where applicable. See `delivery-and-verification.md` for the full picture.

Version pins were originally selected against current official sources on 2026-09-08 (the
migration plan's "Target stack" table) and are now confirmed live in `backend/pom.xml` and
`frontend/package.json`.

## Backend

| Technology | Role | Declared/locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- |
| Java (Temurin) | Language / runtime | `25` (`pom.xml` `<java.version>`), pinned toolchain `~/.cache/kivvi-toolchains/jdk-25` (host has no system Java 25) | `backend/src/main/java/**` | Observed | Decided | verified (final gates, `run.json`; not re-run by this refresh) |
| Spring Boot | Backend framework | `4.1.1` (`pom.xml` parent) | `backend/pom.xml`, every `@SpringBootApplication`/starter | Observed | Decided | verified |
| Spring Web MVC (Tomcat) | HTTP layer | bundled via `spring-boot-starter-web` | `backend/.../web/*Controller.java` | Observed | Decided | verified |
| Jackson 3 | JSON | bundled by the Spring Boot 4 BOM | `web/dto/*Response.java` serialization | Observed | Decided | verified |
| Spring Session JDBC | Session store (Postgres-backed) | `spring-boot-starter-session-jdbc` | `application.yml` (`spring.session.store-type: jdbc`), `V1__baseline.sql` (`SPRING_SESSION*`) | Observed | Decided | verified |
| Flyway | Schema migration | `spring-boot-starter-flyway` + `flyway-database-postgresql` | `backend/src/main/resources/db/migration/V1__baseline.sql` | Observed | Decided; schema frozen at V1 since the cutover — extend with new versioned migrations, never edit `V1__baseline.sql` | verified |
| PostgreSQL JDBC driver | DB connectivity | `org.postgresql:postgresql` (runtime scope) | `pom.xml` | Observed | Decided | verified |
| ShedLock (`shedlock-spring` + `shedlock-provider-jdbc-template`) | Distributed scheduler lock | `7.10.0` | `pom.xml`, `infrastructure/scheduling/SchedulingConfig.java` | Observed | Decided | verified |
| ArchUnit | Architecture tests | `archunit-junit5` (`${archunit.version}`) | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` | Observed | Decided | verified |
| Testcontainers (Postgres) | Integration test DB | `spring-boot-testcontainers` + `testcontainers-postgresql` | `backend/src/test/java/click/kivvi/*ApiIT.java` | Observed | Decided | verified |
| Maven (wrapper) | Build tool | `backend/mvnw`, `.mvn/wrapper` | project root of `backend/` | Observed | Decided (no Gradle) | verified |
| Spotless + google-java-format | Java formatting, bound to `verify` | `spotless-maven-plugin` + `google-java-format.version` | `pom.xml` `<build><plugins>` | Observed | Decided | verified |

Artifact version note (Inferred, minor): `pom.xml`'s own `<version>` is still `0.1.0-w0-login` —
a leftover from the first migration slice's name, never bumped after wave-0. Cosmetic; does not
affect the build.

## Frontend

| Technology | Role | Declared/locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- |
| Vue 3 | Frontend framework (SPA, no SSR) | `3.5.42` | `frontend/package.json`, `frontend/src/**` | Observed | Decided | verified |
| vue-router | SPA routing, history mode | `5.3.1` | `frontend/src/router/*.ts` | Observed | Decided | verified |
| Pinia | State store | `4.0.3` | `frontend/src/stores/shell.ts` | Observed | Decided | verified |
| vue-i18n | i18n (PL default, EN toggle) | `11.4.10` | `frontend/src/i18n/**` | Observed | Decided | verified |
| Vite | Build/dev server | `8.2.2` (+ `@vitejs/plugin-vue` `6.0.8`) | `frontend/vite.config.ts` | Observed | Decided | verified |
| Vitest + @vue/test-utils | Unit/integration tests | `5.0.0` / `2.5.0` | `frontend/test/{unit,integration}/**` | Observed | Decided | verified |
| vue-tsc | Typecheck | `3.3.11` | `frontend/package.json` `typecheck` script | Observed | Decided | verified |
| TypeScript | Frontend language | `6.0.3` | `frontend/tsconfig.json` | Observed | Decided | verified |
| ESLint + eslint-plugin-vue + typescript-eslint | Linting, boundary rules | `10.10.0` / `10.11.0` / `8.70.0` | `frontend/eslint.config.js` | Observed | Decided | verified |
| Prettier | Formatting | `3.8.4` | `frontend/package.json` `format`/`format:check` | Observed | Decided | verified |

## Edge, data, tooling

| Technology | Role | Declared/locked | Detected in | Provenance | Decision | Run |
| --- | --- | --- | --- | --- | --- | --- |
| dunglas/mercure (hub image) | Real-time SSE, public edge | `v0.24.2` | `mercure/Caddyfile`, `compose.yaml` `mercure` service | Observed | Decided; AGPL-3.0, used unmodified as a network service | verified (production, `docker ps`) |
| PostgreSQL 18 | Only backing service | `postgres:18-alpine` | `compose.yaml` `database` service | Observed | Decided | verified (production, `docker ps`) |
| Docker Compose (`compose.yaml` / `compose.prod.yaml`) | Dev/prod orchestration | — | root of repo | Observed | Decided | verified (dev shape by prior gates; prod shape live in production) |
| GitHub Actions (`build.yml`) | CI: `mvnw verify` (backend, SPA built in) + `npm run {typecheck,lint,format:check,test,test:integration,build}` (frontend) + build-only backend Docker image check | on push/PR touching `backend/**`, `frontend/**`, `compose*.yaml`, `mercure/**`, `tools/migration-verify/**`, `.github/workflows/build.yml` | `.github/workflows/build.yml` | Observed | Decided | not-run in this refresh (workflow inspected only; no `gh run list` queried) |
| pixelmatch + pngjs | Screenshot diffing for `compare.mjs` | `7.2.0` / `7.0.0` | `tools/migration-verify/package.json` | Observed | Decided | verified |
| Playwright (reused) | E2E + verifier browser automation | `^1.56.0` declared (`tests/e2e/package.json`), reused via `createRequire` in `compare.mjs` | `tools/migration-verify/compare.mjs`, `tests/e2e/` | Observed | Decided | verified (`cutover/con1-e2e.md`, 2026-09-09, against production) |

## Compatibility notes

- `frontend/eslint.config.js` enforces two architectural rules mechanically: no client-side
  number formatting (`Intl`/`toLocaleString`/`toFixed` banned outside three governed exceptions)
  and no client-side Mercure topic construction (`/accounts/` literal banned outside the same
  three files) — see `backend/.agents/project-context.md` and `frontend/.agents/project-context.md`
  for the full invariant text.
- `backend/pom.xml`'s `maven-compiler-plugin` runs `-Xlint:all -Werror` — every Java compiler
  warning is a build failure, the plan's declared equivalent of the old stack's PHPStan level 5.
- Fixtures are implemented as Java classes (`backend/src/main/java/click/kivvi/fixtures/*.java`),
  not JSON resources — an accepted drift from the migration plan's decision ledger, no code
  change needed; see R20 in `risks-and-unknowns.md`.
- Node 26 LTS status and Spring Boot 4.1.1's OSS-support end date were recorded in the plan from
  secondary sources, flagged "verify before CUT-1" — not re-verified by this refresh (Inferred,
  carried over risk).

## Configuration key names (values not recorded)

`compose.yaml`, `compose.prod.yaml`, `backend/src/main/resources/application.yml` (names only,
not read): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
`MERCURE_PUBLISHER_JWT_KEY`, `MERCURE_SUBSCRIBER_JWT_KEY`, `MERCURE_JWT_SECRET`, `MERCURE_URL` /
`MERCURE_INTERNAL_URL` (a deliberately distinct substitution-variable name — `compose.yaml`'s own
comment explains it avoids the repo-root `.env`'s pre-existing `MERCURE_URL` shadowing the
internal publish URL with a public placeholder), `SERVER_NAME`, `HTTP_PORT`/`HTTPS_PORT`/
`HTTP3_PORT`, `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD`, `IMAGES_PREFIX`, `APP_ENV`,
`APP_SECRET`.

`.env.prod.docker.example` (prod-only) additionally declares `DEFAULT_URI`,
`CADDY_MERCURE_PUBLIC_URL`, `CADDY_MERCURE_JWT_SECRET`, `POSTGRES_VERSION` — **these are leftover
old-stack keys** (Symfony's absolute-URL generator, FrankenPHP's Caddy Mercure vars); they are
dead configuration for this stack now that the old stack is gone. See R23 in
`risks-and-unknowns.md`.

Secret-bearing files present locally and ignored, not read: `.env.local`, `.env.prod.docker`,
`.ai/cezar/launch-key`.
<!-- END project-context-initializer:artifact -->
