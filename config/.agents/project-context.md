<!-- BEGIN project-context-initializer:context -->
# Context: `config/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `packages/`, `routes/` rolled-up.

## Purpose
Symfony configuration: bundles, services, per-package config, storybook catalogue.

## Files
- `bundles.php` — Framework, Doctrine, Migrations, Twig, TwigExtra, Maker (dev), SensiolabsTypeScript, Mercure. **No SecurityBundle, no Monolog config file.**
- `services.yaml` — autowire `App\\`; `session.pdo` factory from DBAL native connection → `PdoSessionHandler`; `ImportUploadStorage` upload dir `%kernel.project_dir%/var/import`; `StoryRegistry` file `config/storybook.php`.
- `routes.yaml` — attribute routing only; `routes/framework.yaml` dev error pages.
- `packages/framework.yaml` — `trusted_proxies`/`trusted_headers`/`trusted_hosts` from env; session handler Pdo, `cookie_secure: auto`, `samesite: lax`; test uses mock-file sessions.
- `packages/cache.yaml` — `app: cache.adapter.doctrine_dbal`, `system: cache.adapter.system`.
- `packages/doctrine.yaml` — `DATABASE_URL`, underscore naming, identity generation for Postgres, attribute mapping in `src/Entity`; test `dbname_suffix _test`; prod query/result cache pools.
- `packages/messenger.yaml` — `async` = `MESSENGER_TRANSPORT_DSN`, `failed` = doctrine queue; no routing rules; in-memory in test.
- `packages/mercure.yaml` — hub `default`, JWT secret env, publish `*`.
- `packages/translation.yaml` — default `pl`, enabled `pl,en`, fallback `pl`.
- `packages/asset_mapper.yaml` — paths `assets/`, strict missing imports (warn in prod). `packages/twig.yaml` — strict variables in test. `packages/routing.yaml` — `default_uri` from env. `packages/doctrine_migrations.yaml` — `migrations/` dir.
- `storybook.php` — 257 story entries (title, group, template, doc, variants = include params).
- `preload.php`, `reference.php` — generated (opcache preload, config array shapes); excluded from cs-fixer.

## Configuration key names
`APP_SECRET`, `DATABASE_URL`, `MESSENGER_TRANSPORT_DSN`, `MERCURE_URL`, `MERCURE_PUBLIC_URL`, `MERCURE_JWT_SECRET`, `DEFAULT_URI`, `TRUSTED_PROXIES`, `TRUSTED_HOSTS`, `TEST_TOKEN`.

## Invariants
Postgres-only backing services; no Redis/Memcached adapters; strict Twig variables in tests mean every template param must be passed explicitly.

## Risks
Missing security config (R4); cache/dedup share one pool (R7); `storybook.php` header describes wiring that differs from `services.yaml` (doc drift).

## Evidence
`config/**` (all read), `migrations/Version20260521120000.php`.
<!-- END project-context-initializer:context -->
