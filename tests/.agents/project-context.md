<!-- BEGIN project-context-initializer:context -->
# Context: `tests/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `Controller/`, `Tracking/` rolled-up; `e2e/` has its own context.

## Purpose
PHPUnit 13 suite (`App\\Tests\\`), bootstrapped by `tests/bootstrap.php` (Dotenv, `APP_ENV=test`).

## Files
- `CacheTest.php` — `cache.app` round-trip through Postgres (needs `app_test` DB).
- `Controller/PanelPagesTest.php` — data provider of 25 URLs → selector/text; shell presence; detail route keeps section active; EN nav; 404 for unknown customer/tab/locale.
- `Controller/PreferencesControllerTest.php` — theme/sidebar persisted and rendered; unknown theme → light.
- `Controller/SecurityControllerTest.php` — login redirects to dashboard and shows e-mail in `.sb-foot`; empty/malformed rejected (PL messages); logout restores default identity.
- `Tracking/EventIngestionTest.php` — payload validation, rendered-row publish via `MockHub`, duplicate suppressed, HTTP 202/200/400 on `/collect`.

## Configuration
`phpunit.dist.xml`: `failOnDeprecation/Notice/Warning`, source `src`, cache `.phpunit.cache`. Test env: in-memory Messenger, mock-file sessions, strict Twig variables, DB suffix `_test`.

## Commands
`docker compose exec php composer test` (creates DB, builds TS, runs PHPUnit) — documented, not-run in this initialization. `composer phpstan` also analyses `tests/`.

## Invariants
Tests assert Polish copy and CSS selectors; changing wording or class names breaks them (intentional coupling to the design system).

## Risks
Not executed here (R12); no tests for editor blocks endpoint, import parsing, scheduler.

## Evidence
`tests/**/*.php`, `phpunit.dist.xml`, `composer.json` scripts.
<!-- END project-context-initializer:context -->
