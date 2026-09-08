<!-- BEGIN project-context-initializer:artifact -->
# Delivery and verification

Labels: `documented` (in README/CLAUDE/AGENTS), `derived` (from config), `verified` (run in this
initialization), `failed`, `not-run`. **Nothing was executed except `docker compose ps`**, because
the dev stack shares the compose project with the running production containers (see below).

## Environments

| Environment | How | Ports / URL | Label |
| --- | --- | --- | --- |
| Dev (Docker) | `HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose up -d --wait` (uses `compose.override.yaml`) | https://localhost:8443 (self-signed) | documented |
| Prod on this host | `docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait` | http://localhost:23456 → https://kivvi.click via external TLS proxy | documented; containers `kivvi-click-php-1`, `kivvi-click-worker-1` (image `app-php-prod`) observed running — verified (ps) |
| Test (PHPUnit) | `APP_ENV=test`, DB `app_test` (`dbname_suffix`), in-memory Messenger, mock-file sessions | inside `php` container | derived |

**Hazard (Inferred):** dev and prod compose stacks share project name `kivvi-click`; the running
containers are prod. `docker compose up` for dev would recreate `php`/`worker` with the dev image
and bind-mount. Use a distinct `-p`/`COMPOSE_PROJECT_NAME` for dev, or confirm with the operator.

## Commands

| Purpose | Command | Label |
| --- | --- | --- |
| Build images | `docker compose build php` | documented |
| Console | `docker compose exec php php bin/console <cmd>` | documented |
| Migrate test DB | `php bin/console --env=test doctrine:migrations:migrate` | documented |
| Unit + functional tests | `docker compose exec php composer test` (creates `app_test`, `typescript:build`, then `bin/phpunit`) | documented, not-run |
| Static analysis PHP | `docker compose exec php composer phpstan` (warms dev cache first; level 5, `src` + `tests`) | documented, not-run |
| Type-check TS | `yarn typecheck` (`tsc --noEmit`, strict) | documented, not-run |
| Format | `vendor/bin/php-cs-fixer fix`, `yarn format` (`prettier --write assets`) | documented, not-run; also auto via `.claude/settings.json` hook |
| E2E | `cd tests/e2e && npm install && npx playwright test` (headless system Chrome, `E2E_BASE_URL` default `https://localhost:8543`, override to 8443) | documented, not-run |
| Storybook | https://localhost:8443/_storybook (dev/test only) | documented |
| Scheduler/worker manual | `php bin/console messenger:consume scheduler_default` | documented |
| Prod migrations | one-shot `migrations` service in `compose.prod.yaml` | derived |
| Rollback | none documented; migrations have `down()`; images not tagged/pushed | Unknown |
| Smoke | Docker healthcheck on `http://localhost:2019/metrics` (Caddy admin) | derived |

## Test surfaces (Observed)

| Suite | Files | Covers |
| --- | --- | --- |
| PHPUnit functional | `tests/Controller/PanelPagesTest.php` (25 page cases + shell/locale/404), `PreferencesControllerTest.php`, `SecurityControllerTest.php` | every panel screen renders, PL/EN nav, preferences round-trip, login/logout identity |
| PHPUnit integration | `tests/Tracking/EventIngestionTest.php` (`MockHub`), `tests/CacheTest.php` | validation, dedup, rendered row publish, `/collect` 202/200/400; Postgres cache round-trip |
| Playwright E2E | `tests/e2e/specs/{automations,customers,dashboard,editors,events,import,lists,navigation,public,settings}.spec.ts` | real browser on the running stack, including `POST /collect` → SSE row appears, replay not duplicated, upload advances wizard, theme/sidebar persistence |

PHPUnit fails on any deprecation/notice/warning (`phpunit.dist.xml`), so dependency bumps surface as
test failures. Playwright uses `channel: "chrome"` — the host must have Google Chrome.

## CI/CD

`.github/workflows/docker-build.yml`: on push to `main`, build `Dockerfile` target `frankenphp_prod`
with `push: false`. No test, lint, phpstan, or e2e job in CI (health-check doc calls this intentional).
Current CI run status: not queried (documented only). Deployment to this host is manual compose.

## Missing harnesses (Observed gaps)

- No CI test gate; no image registry push; no rollback procedure.
- No test for the missing editor `/blocks` endpoint (`assets/controllers/editor.ts` posts to it).
- No load/perf test for `/collect` although PRD sets p95 < 500 ms.
<!-- END project-context-initializer:artifact -->
