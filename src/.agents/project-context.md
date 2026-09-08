<!-- BEGIN project-context-initializer:context -->
# Context: `src/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own` (children `Panel/` and `Tracking/` have their own contexts).

## Purpose
All PHP application code, namespace `App\\` (PSR-4), autowired/autoconfigured via `config/services.yaml` (`App\\: resource ../src/`).

## Layout and rolled-up directories
- `Kernel.php` — MicroKernel. `Schedule.php` — `#[AsSchedule]`, hourly `Heartbeat`, state in cache.
- `Controller/` (rolled-up) — 14 controllers. Panel controllers share `#[Route('/{_locale}', requirements pl|en, default pl)]`; each renders `pages/*.html.twig` from `Panel\\Content` catalogues. Non-localised: `EventIngestionController` (`POST /collect`), `PreferencesController` (`POST /preferences/theme|sidebar`), `ImportController::upload` (`POST /import/upload`), `StorybookController` (`/_storybook`, dev/test only via route condition).
- `Message/Heartbeat.php`, `MessageHandler/HeartbeatHandler.php` (rolled-up) — only message/handler pair; logs a line.
- `Twig/` (rolled-up) — `PanelExtension` (global `panel` = `PanelContext`), `IconExtension` (global `icons`, ~50 stroke icons, add icons here never inline SVG), `CodeHighlightExtension` (`kivvi_highlight` filter, server-side).
- `Storybook/StoryRegistry.php` (rolled-up) — loads `config/storybook.php` (257 stories).
- `Entity/`, `Repository/` (rolled-up) — empty except `.gitignore`. Doctrine mapping points at `src/Entity` (attributes).
- `Panel/`, `Tracking/` — see child contexts.

## Contracts
- Route names used by `Panel\\Navigation` and templates: `dashboard`, `events`, `customers`, `customer_show`, `automations`, `automation_new|edit`, `campaigns`, `email_new|edit`, `popups`, `popup_new|edit`, `feeds`, `import`, `settings`, `login`, `logout`, `home`, `demo`.
- Id patterns: customers `c_\d+`, automations `a\d+`, emails `k\d+`, popups `p\d+`; import steps 1–4; settings tabs validated by `SettingsCatalog::isKnownTab`.
- Twig templates expect pre-formatted strings (`Panel\\Format`), not raw numbers.

## Conventions (Observed)
`declare(strict_types=1)`, `final`/`final readonly` classes, constants instead of magic values, docblock explaining *why*, PL user-facing strings inline (translation coverage partial), exceptions for invalid input (`InvalidEventPayload`), 404 via `createNotFoundException`.

## Commands
`docker compose exec php composer test`, `composer phpstan` (level 5), `vendor/bin/php-cs-fixer fix`.

## Invariants
- Every panel route keeps the `{_locale}` prefix and default `pl`.
- Storybook must never be reachable in prod (route condition on `APP_ENV`).
- Messages are handled synchronously unless routed to `async` in `messenger.yaml`.

## Risks
Missing editor `/blocks` route (R6); no security firewall, CSRF or authorization (R4); no entities (R3/R7).

## Evidence
`src/Controller/*.php`, `src/Twig/*.php`, `src/Schedule.php`, `config/services.yaml`, `config/packages/doctrine.yaml`.
<!-- END project-context-initializer:context -->
