<!-- BEGIN project-context-initializer:artifact -->
# Delivery and verification

Labels: `documented` (in README/CLAUDE/AGENTS), `derived` (from config), `verified` (executed —
either by a prior verification session, cited by evidence path, or Observed as executed by the
migration run itself), `failed`, `not-run`. Historical evidence below retains its original
revision/time. The post-migration follow-up added fresh local and GitHub Actions verification.

## R22/R23 follow-up (2026-09-09)

- `c846294`: Java/frontend hook corrected; Compose/Caddy comments and production template
  cleaned. Real formatter probes passed for relative and absolute Java/Vue paths, protected
  CSS stayed byte-identical, empty tool payload was a no-op. JDK 25 and installed frontend
  dependencies are prerequisites (`AGENTS.md`).
- Compose dev/prod normalized JSON matched the prior configuration using identical env input;
  Caddy non-comment directives matched exactly. No production deployment was performed.
- `c696ef6`: remote history integrated and migration pushed to `origin/main`; the remote
  runner choice `[self-hosted, home]` is preserved for all three jobs in `build.yml`.
- First CI run `34379318052` exposed formatting violations after migration: frontend generated
  context Markdown and one Java Javadoc paragraph. Local gates reproduced both. `de073ee`
  formats those files; application logic is unchanged.
- Local frontend: typecheck PASS, lint PASS (12 existing warnings), Prettier PASS after the
  Markdown repair, unit 169/169 PASS, integration 152/152 PASS, production build PASS.
- Local backend: unit 299/299 PASS, integration 141 PASS and one deliberately disabled RED
  proof; zero test failures/errors. Initial verify failed on Javadoc formatting; subsequent
  Spotless apply/check PASS. Full verify passed in CI for `de073ee`.
- Follow-up [Build run 34379637919](https://github.com/muszkin/kivvi-click/actions/runs/34379637919)
  on `de073eee2e9a0c516734ae4bb9afe70b0e6d194a`: **backend PASS, frontend PASS,
  backend-image PASS; overall SUCCESS**. This includes the full backend `mvnw verify`,
  frontend typecheck/lint/format/unit/integration/build, and the actual Docker image build.
- Self-hosted runners provision automatically: the repository runner list was temporarily
  empty while jobs queued, then jobs started. An empty list alone is not an outage signal.

## Environments

| Environment | How | Ports / URL | Label |
| --- | --- | --- | --- |
| Dev (Docker) | `docker compose -p kivvi-dev up -d --build --wait` (`HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT` env vars) | https://localhost:8443 (self-signed) | documented |
| Prod on this host | `docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait`, run from this directory only | http://localhost:23456 → https://kivvi.click via external TLS proxy | documented; **observed running and healthy** — `api`/`mercure`/`database` all `Up (healthy)` (`docker ps`, 2026-09-09T16:20Z; this refresh ran `ps` only, no `up`/`down`) |
| Backend test (Testcontainers) | `cd backend && ./mvnw -q verify` spins up Postgres 18 via Testcontainers | inside the Maven build | verified (final gates, `run.json`; also `cutover/con1-e2e.md` ran a fresh `frontend` build+e2e pass post-CON-1, not a backend test re-run) |
| Frontend test (Vitest) | `cd frontend && npm run test -- --run` / `npm run test:integration -- --run` | Node process, jsdom | verified (final gates, `run.json`) |
| E2E against production | `cd tests/e2e && E2E_BASE_URL=https://kivvi.click npx playwright test` | production, port 23456 via the proxy | **verified 2026-09-09T16:04-16:07Z**, `cutover/con1-e2e.md`: 66/66 unique tests PASS across 11 invocations (`navigation.spec.ts` run twice, `--workers=1` for `events`/`dashboard`/`navigation`) |

**Hazard (unchanged since the prior refresh):** dev and prod compose stacks can still collide on
the default project name — omitting `-p` on a dev `docker compose up` here defaults to this
directory's own name (`kivvi-click`), which is exactly prod's running project name on this host.
Always pass `-p kivvi-dev` for dev work (`AGENTS.md`/`CLAUDE.md` both say this explicitly). See
R5 in `risks-and-unknowns.md`.

## Commands (Observed, `backend/pom.xml`, `frontend/package.json`, `AGENTS.md`, `CLAUDE.md`)

| Purpose | Command | Label |
| --- | --- | --- |
| Java toolchain | `export JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25` (host has no system Java 25) | documented |
| Backend unit tests | `cd backend && ./mvnw -q test` (unit + `ArchitectureTest`, no containers) | verified (final gates) |
| Backend integration + ArchUnit + Spotless | `cd backend && ./mvnw -q verify` (Testcontainers Postgres 18) | verified (final gates: 299 unit, 142 IT, spotless, ArchUnit) |
| Backend format check/fix | `./mvnw spotless:check` / `./mvnw spotless:apply` | verified (bound to `verify` phase) |
| Backend local dev run | `./mvnw spring-boot:run` (against the compose database) | documented — fast iteration only; every gate still runs against the compose-built image |
| Frontend install | `cd frontend && npm ci` | verified (`cutover/con1-e2e.md` step1) |
| Frontend unit tests | `npm run test -- --run` | verified (final gates: 169) |
| Frontend integration tests | `npm run test:integration -- --run` | verified (final gates: 152) |
| Frontend lint | `npm run lint` | verified (final gates) |
| Frontend typecheck | `npm run typecheck` (`vue-tsc --noEmit`) | verified (final gates) |
| Frontend format check | `npm run format:check` | verified (final gates) |
| Frontend build | `npm run build` | verified (final gates; also re-run 2026-09-09 for the post-CON-1 performance check) |
| Migration parity check (still supported post-cutover) | `node tools/migration-verify/compare.mjs --journey <id> --base <url> [--dimension contract\|visual\|performance\|all]` | verified pre-cutover; not re-run by this refresh |
| Performance budget check | `node tools/migration-verify/performance.mjs --base <url>` | **verified against production 2026-09-09**, `cutover/con1-e2e.md` §3: all 4 budgets PASS (`initialJsGzipBytes` 101,990 B ≤ 307,200 B; `lcpMillis` 200 ≤ 2000; `ttiMillis` 57.8 ≤ 2500; `collectP95Millis` 39.7 ≤ 500) |
| E2E (full suite) | `cd tests/e2e && npm install && E2E_BASE_URL=<url> npx playwright test` (`events`/`dashboard`/`navigation` with `--workers=1`, `navigation` run twice) | **verified against production 2026-09-09T16:04-16:07Z**, 66/66 PASS |
| CI | `.github/workflows/build.yml` (`backend`, `frontend`, `backend-image` jobs) | documented, not queried in this refresh (`gh run list` not run; `gh pr list` was run and returned 0 open/closed PRs — see `git-and-pr-history.md`) |

## Test surfaces

| Suite | Files | Covers |
| --- | --- | --- |
| JUnit unit | `backend/src/test/java/click/kivvi/{application,domain,infrastructure,fixtures,web}/**` | one `*ViewServiceTest`/`*ControllerTest` per page, domain logic, session lock, heartbeat, mercure publisher request shape |
| JUnit integration (`*IT`, Testcontainers Postgres) | `backend/src/test/java/click/kivvi/*ApiIT.java`, `SessionRoundTripIT`, `SessionRequestSerializationIT`/`...RedProofIT`, `ShellPagesIT` | end-to-end API behaviour against a real Postgres 18 |
| ArchUnit | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` | layering (`domain`↛`web`, `infrastructure` only from `application`, no cycle), Mercure-publisher access, topic-literal locality |
| Vitest unit | `frontend/test/unit/*.spec.ts` (28 files) | component/structural invariants (e.g. exactly one `.main-scroll`) |
| Vitest integration | `frontend/test/integration/*.spec.ts` (21 files) | one per view/journey + shell store, locale toggle, scroll restoration |
| Playwright E2E | `tests/e2e/specs/{automations,customers,dashboard,editors,events,import,lists,navigation,public,settings}.spec.ts` | the whole panel's browser-level acceptance oracle — unchanged specs since `812db3f` (2026-08-26), verified untouched by `git diff --stat` over the full CON-1 window |
| Migration verifier | `tools/migration-verify/{compare,performance}.mjs` | contract/visual/performance parity between the frozen pre-migration oracle capture and this stack; kept running post-cutover as a regression tool |

Both `./mvnw` and the frontend gate treat a warning as a build failure by design: Maven's
compiler plugin runs `-Xlint:all -Werror`; a JUnit extension
(`testsupport.FailOnWarnLogExtension`) fails any test that logs at `WARN` or above.

## CI/CD

`.github/workflows/build.yml` — triggers on push to `main` and PRs touching `backend/**`,
`frontend/**`, `compose*.yaml`, `mercure/**`, `tools/migration-verify/**`, or the workflow file
itself (path filters fixed by CON-1 commit `0b46320`, which dropped an old-stack-only path).
Three jobs: `backend` (builds the SPA, copies it into the jar's static resources, `./mvnw -B -q
verify`), `frontend` (typecheck/lint/format-check/unit/integration/build), `backend-image`
(build-only Docker image check, no push). **Neither the shared Playwright suite nor
`tools/migration-verify` runs in CI** — both ran inside the migration orchestrator's own
worker/verification loop and, since cutover, only in ad-hoc sessions like
`cutover/con1-e2e.md` — a regression introduced after this refresh would not be caught by CI
alone until someone runs the e2e suite by hand.

## Missing harnesses (Observed gaps)

- No CI job runs the shared Playwright suite or the migration verifier.
- No metrics/tracing/error-tracking harness (see `architecture-and-flows.md` Observability).
- No load/perf test for `/collect` runs in CI, although `tools/migration-verify/performance.mjs`
  can check it manually against `budget.json`'s `collectP95Millis: 500` (last checked and PASSing
  against production, see above).
- No rollback automation beyond the documented `git checkout <sha>` + redeploy sequence in
  `README.md`'s "Production operations" — there is no second stack to fall back to any more
  (the old stack was deleted by CON-1).
<!-- END project-context-initializer:artifact -->
