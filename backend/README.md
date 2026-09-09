# kivvi-click — backend

Spring Boot 4.1.1 API behind the Vue 3 SPA (see `../frontend/`). Package root `click.kivvi`,
layered `web` → `application` → `domain`, with `infrastructure` reachable only from
`application` (enforced by `ArchitectureTest`, an ArchUnit test). Postgres is the only
backing service; Spring Session JDBC keeps sessions there, Flyway owns the schema
(`src/main/resources/db/migration`).

## Build

The jar serves the built SPA from its own classpath (`src/main/resources/static`), so the
frontend must be built and copied in before packaging or running this module standalone:

```sh
cd frontend && npm ci && npm run build && cd ..
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/. backend/src/main/resources/static/
```

`backend/Dockerfile` does this automatically in a multi-stage build (Node stage → Maven
stage → JRE runtime) — use it for anything running through `compose.yaml`.

Requires Java 25 (Temurin) on `PATH`/`JAVA_HOME` and Docker for the Testcontainers-backed
integration test.

## Run

```sh
export JAVA_HOME=/path/to/jdk-25
./mvnw spring-boot:run
```

Needs a reachable Postgres 18 at the `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` env
vars (defaults point at `localhost:5432`) — bring one up with `compose.yaml`, or run
the whole stack through Docker Compose instead of this module directly.

## Test

- Focused (no containers): `./mvnw test` — plain unit tests plus `ArchitectureTest`.
- Integration (Testcontainers Postgres 18, needs Docker): `./mvnw verify` — also runs every
  `*IT.java` class via the `maven-failsafe-plugin` binding.
- Static: `./mvnw spotless:check` (google-java-format); `./mvnw spotless:apply` to fix.

Both `test` and `verify` run on a clean checkout with **no frontend build** —
`src/test/resources/static/index.html` is a minimal, structurally-real stand-in for
`frontend/dist/index.html` (same `<html lang="pl">` anchor `SpaDocument` injects into),
scoped to `target/test-classes`, which sits ahead of `target/classes` on the test classpath
and so shadows the real file whenever both are present. Production packaging
(`backend/Dockerfile`) is unaffected — it never runs from `target/test-classes` — and
`IndexHtmlTemplate` still fails application startup fast if the real built file is missing
there. Verified by `git clean -xdf backend/src/main/resources/static frontend/dist` followed
by `./mvnw test` and `./mvnw verify`, both green.

**Fail-on-warning test policy** (mirrors the old stack's PHPUnit `failOnWarning`, restricted
to first-party code): `FailOnWarnLogExtension` attaches a Logback `ListAppender` to the
`click.kivvi` logger for the duration of each test and fails it if anything ≥ `WARN` was
logged. Registered automatically for every test via JUnit 6's extension auto-detection
(`src/test/resources/junit-platform.properties`); see
`click.kivvi.testsupport.FailOnWarnLogExtensionTest` for a self-test that proves the
extension actually fails on a deliberate `LOG.warn(...)`.

Integration-level coverage lives in `src/test/java/click/kivvi/*IT.java`, each booting the
full Spring context against the Testcontainers Postgres: `SessionRoundTripIT` (JDBC session
persistence, theme/sidebar preference round trips, sign-in/sign-out) and `ShellApiIT`
(locale-specific navigation labels through the real HTTP layer, 404 routing for an
unsupported locale prefix and an unknown path, and the dashboard shell payload asserted
against the oracle's `journeys/login/steps/8` values).

## Endpoints introduced in this slice

- `GET /api/v1/{locale}/shell` — the panel shell view-model (nav, workspace, identity, theme, sidebar).
- `GET|POST /{locale}/login`, `POST /{locale}/logout`.
- `POST /preferences/theme`, `POST /preferences/sidebar`.
- Every other known panel/public route (`RouteTable`) — serves the SPA document; anything
  else, or an unsupported locale prefix, 404s.
- `GET /actuator/health` — kept off the Mercure edge; internal only.
