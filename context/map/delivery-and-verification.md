<!-- BEGIN project-context-initializer:artifact -->
# Delivery and verification

Labels: `documented` (in README/CLAUDE/AGENTS/plan), `derived` (from config), `verified` (executed
— by this context refresh, or Observed as executed by the migration run itself, cited either way),
`failed`, `not-run`. This refresh executed nothing itself except read-only inspection
(`docker ps`, `df -h`, `git log`); "verified" entries below cite the migration run's own evidence
(`context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/run.json`) as the source.

## Environments

| Environment | How | Ports / URL | Label |
| --- | --- | --- | --- |
| Old-stack dev (Docker) | `HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose up -d --wait` | https://localhost:8443 (self-signed) | documented |
| Old-stack prod on this host | `docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait` | http://localhost:23456 → https://kivvi.click via external TLS proxy | documented; containers `kivvi-click-php-1`, `kivvi-click-database-1` observed running (`docker ps`, 2026-09-09) |
| Old-stack test (PHPUnit) | `APP_ENV=test`, DB `app_test`, in-memory Messenger, mock-file sessions | inside `php` container | derived |
| Next-stack dev/verification (Docker) | `docker compose -p <lease> -f compose.next.yaml up -d --build --wait` (leased HTTP/HTTPS ports from the `19000 + 10·n` pool) | `https://localhost:<leased-port>` | documented (plan §"Worktree resource lease"); observed running as `kivvi-int-*` during this refresh (`docker ps`) |
| Next-stack prod overlay | `docker compose -p kivvi-click --env-file .env.prod.docker -f compose.next.prod.yaml up -d --build --wait` | http://localhost:23456 (same port, after CUT-1) | documented (plan §"Delivery stages", CUT-1 row); **not deployed** — CUT-1 requires separate authorization |
| Next-stack backend test (Testcontainers) | `./mvnw verify` spins up Postgres 18 via Testcontainers | inside the Maven build | verified (Observed, `run.json`) |
| Next-stack frontend test (Vitest) | `npm run test` / `npm run test:integration` | Node process, jsdom | verified (Observed, `run.json`) |

**Hazard (Inferred, unchanged from prior refresh):** old-stack dev and prod compose stacks share
project name `kivvi-click`; running the documented dev command without a distinct `-p` would
recreate the running prod containers. The next stack's leases already use distinct project names
(`kivvi-w-<journey>`, `kivvi-int`, `kivvi-final`) by design — this hazard is specific to the old
stack and unaffected by the migration until `CUT-1`, which explicitly reuses project name
`kivvi-click` at that point (plan §"Delivery stages" CUT-1 row) — a deliberate, authorized reuse,
not the same accidental hazard.

## Commands — old stack (unchanged)

| Purpose | Command | Label |
| --- | --- | --- |
| Build images | `docker compose build php` | documented |
| Console | `docker compose exec php php bin/console <cmd>` | documented |
| Unit + functional tests | `docker compose exec php composer test` | documented, not-run (this refresh) |
| Static analysis PHP | `docker compose exec php composer phpstan` | documented, not-run |
| Type-check TS | `yarn typecheck` | documented, not-run |
| Format | `vendor/bin/php-cs-fixer fix`, `yarn format` | documented, not-run |
| E2E | `cd tests/e2e && npm install && npx playwright test` (`E2E_BASE_URL` default `https://localhost:8543`) | documented, not-run |
| Storybook | https://localhost:8443/_storybook (dev/test only) | documented |
| Prod migrations | one-shot `migrations` service in `compose.prod.yaml` | derived |

## Commands — next stack (Observed, `backend/pom.xml`, `frontend/package.json`, `common-journey-rules.md`)

| Purpose | Command | Label |
| --- | --- | --- |
| Java toolchain | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` | documented (host has no system Java 25) |
| Backend unit tests | `cd backend && ./mvnw -q test` | verified (Observed, `run.json`: unit dimension in the final cohort) |
| Backend integration + ArchUnit + Spotless | `cd backend && ./mvnw -q verify` | verified |
| Backend format check/fix | `./mvnw spotless:check` / `./mvnw spotless:apply` | verified (bound to `verify` phase) |
| Backend local dev run | `./mvnw spring-boot:run` (against the compose database) | documented — fast iteration only, gates still run against the compose-built image |
| Frontend unit tests | `cd frontend && npm run test -- --run` | verified |
| Frontend integration tests | `npm run test:integration -- --run` | verified |
| Frontend lint | `npm run lint` | verified |
| Frontend typecheck | `npm run typecheck` (`vue-tsc --noEmit`) | verified |
| Frontend format check | `npm run format:check` | verified |
| Frontend build | `npm run build` | verified |
| Stack up (per-journey lease) | `docker compose -p <lease> -f compose.next.yaml up -d --build --wait` | documented |
| Contract/visual verification | `node tools/migration-verify/compare.mjs --journey <id> --base https://localhost:<port> --out <dir> [--dimension contract\|visual\|performance\|all]` | verified |
| Performance verification | `node tools/migration-verify/performance.mjs --base https://localhost:<port>` | verified |
| E2E (shared oracle suite) | `cd tests/e2e && E2E_BASE_URL=https://localhost:<port> npx playwright test <spec>` (one invocation per `-g` filter; `events.spec.ts` with `--workers=1`) | verified |
| CI | `.github/workflows/next-build.yml` (`backend` + `frontend` jobs) | documented, not queried in this refresh (`gh run list` not run) |

## Test surfaces

| Suite | Files | Covers | Stack |
| --- | --- | --- | --- |
| PHPUnit functional | `tests/Controller/PanelPagesTest.php`, `PreferencesControllerTest.php`, `SecurityControllerTest.php` | every panel screen renders, PL/EN nav, preferences round-trip, login/logout identity | old |
| PHPUnit integration | `tests/Tracking/EventIngestionTest.php`, `tests/CacheTest.php` | validation, dedup, `/collect` 202/200/400; Postgres cache round-trip | old |
| JUnit unit | `backend/src/test/java/click/kivvi/{application,domain,infrastructure,fixtures,web}/**` | one `*ViewServiceTest`/`*ControllerTest` per page, domain logic, session lock, heartbeat, mercure publisher request shape | next |
| JUnit integration (`*IT`, Testcontainers Postgres) | `backend/src/test/java/click/kivvi/*ApiIT.java`, `SessionRoundTripIT`, `SessionRequestSerializationIT`/`...RedProofIT`, `ShellPagesIT` | end-to-end API behaviour against a real Postgres 18 | next |
| ArchUnit | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` | layering (`domain`↛`web`, `infrastructure` only from `application`, no cycle), Mercure-publisher access, topic-literal locality | next |
| Vitest unit | `frontend/test/unit/*.spec.ts` (28 files) | component/structural invariants (e.g. exactly one `.main-scroll`) | next |
| Vitest integration | `frontend/test/integration/*.spec.ts` (21 files) | one per view/journey + shell store, locale toggle, scroll restoration | next |
| Playwright E2E | `tests/e2e/specs/{automations,customers,dashboard,editors,events,import,lists,navigation,public,settings}.spec.ts` | **shared acceptance oracle for both stacks** — unchanged specs, `E2E_BASE_URL` selects the target | old + next |
| Migration verifier | `tools/migration-verify/{compare,performance}.mjs` | contract/visual/performance parity between the frozen oracle capture and a candidate next-stack build | next (compares against old-stack-derived oracle) |

PHPUnit (old) fails on any deprecation/notice/warning; `backend`'s Maven compiler runs `-Xlint:all
-Werror` (next) — both stacks treat a warning as a build failure, by design (Observed, plan
§"Technology decisions" Architecture-tests row and `pom.xml`).

## CI/CD

- Old stack: `.github/workflows/docker-build.yml` — build-only on push to `main`, `push: false`, no
  test/lint/phpstan/e2e job.
- Next stack: `.github/workflows/next-build.yml` — separate workflow, triggered on push/PR touching
  `backend/**`, `frontend/**`, `compose.next*.yaml`, `mercure/**`, `tools/migration-verify/**`;
  `backend` job builds the SPA first and copies it into the jar's resources, then `./mvnw -B -q
  verify`; `frontend` job runs typecheck/lint/format-check/unit/integration/build. Neither workflow
  currently runs the shared Playwright suite or `tools/migration-verify` in CI (Observed, workflow
  file content) — these run inside the migration orchestrator's own worker/verification loop instead
  (`context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/`), not as a GitHub Actions
  job.

## Migration run status (Observed, `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/run.json`, `RUN.md` — RUN.md is stale, run.json is current)

- `feature_head_sha`: `dae169614a52532c130bd34435994d6a914165c0` (this refresh's source revision).
- `final_cohort.state`: `RUNNING` on this SHA — described in `run.json` as also serving as the
  wave-5 cohort (a superset). Round 1 of the final cohort `FAIL`ed on unit coverage labelling
  (B02/B03/B21 not named after `behaviours.json` ids); fixed in the commits leading to this SHA.
- Per-dimension state at this SHA (Observed, `run.json`): unit/integration/architecture/e2e/visual
  `PASS`; contract dimension `PASS` (final cohort 6/6 on `dae1696`, 2026-09-09).
- `staging`/`production`/`production_pr`: all `NOT_APPLICABLE`/`NOT_RUN` — `CUTOVER_READY` has not
  been reached; `RR-1`/`CUT-1`/`CON-1` have not started.
- `open_obligations`: one recorded item, `OBL-fixtures-drift` — "plan decision ledger described
  fixtures as JSON resources; implemented as Java classes under `click.kivvi.fixtures`... record in
  the plan/context refresh, no code change" — addressed by this refresh as R20 in
  `risks-and-unknowns.md`.

## Missing harnesses (Observed gaps, both stacks)

- Old stack: no CI test gate; no image registry push; no rollback procedure; no test for the
  missing editor `/blocks` endpoint; no load/perf test for `/collect` although PRD sets p95 < 500 ms.
- Next stack: no CI job runs the shared Playwright suite or the migration verifier (both run inside
  the orchestrator loop, outside GitHub Actions) — a regression introduced after the migration run
  closes would not be caught by `next-build.yml` alone. `/collect` p95 < 500 ms is checked by
  `tools/migration-verify/performance.mjs` (`budget.json` `collectP95Millis: 500`), not by CI.
<!-- END project-context-initializer:artifact -->
