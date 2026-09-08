<!-- BEGIN project-context-initializer:artifact -->
# Dependencies

Edge status: `static` = observed in source/config, `runtime` = observed in a running system (only
`docker compose ps` was run), `inferred`, `unknown`.

## Package / workspace lane

```
kivvi/kivvi-click (composer, PHP >=8.5)
├── symfony/* 8.0.x (framework, twig, asset-mapper, messenger, scheduler, translation, …)
├── doctrine/orm 3.6 + doctrine-bundle 3.2 + migrations-bundle 4.0 + doctrine-messenger
├── symfony/mercure-bundle 0.4
├── sensiolabs/typescript-bundle 0.2
└── dev: phpunit 13, phpstan 2 (+symfony ext), php-cs-fixer 3.95, maker-bundle, browser-kit, css-selector
kivvi-click (yarn 4 PnP, root)      → typescript 6, prettier 3   (typecheck/format only)
kivvi-click-e2e (npm, tests/e2e)    → @playwright/test 1.62      (isolated on purpose)
```

## Source-module lane (static, `src/`)

```
Controller/*  ──► Panel/Content/*  ──► Panel/{Workspace,Format}
Controller/*  ──► Panel/{EventStreamTopic,ImportUploadStorage,PanelIdentity,PanelPreferences}
Controller/EventIngestionController ──► Tracking/{TrackedEvent,EventIngestion,InvalidEventPayload}
Tracking/EventIngestion ──► Panel/{EventStreamTopic,Workspace}, Panel/Content/EventFeed (TYPES), Twig env, Mercure HubInterface, cache.app
Twig/PanelExtension ──► Panel/PanelContext ──► Panel/{Navigation,Workspace,PanelPreferences,PanelIdentity}
Twig/IconExtension, Twig/CodeHighlightExtension ──► templates (globals/filters)
Controller/StorybookController ──► Storybook/StoryRegistry ──► config/storybook.php
Schedule ──► Message/Heartbeat ──► MessageHandler/HeartbeatHandler
```

| From | To | Contract / reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `templates/layout/app.html.twig` | Twig global `panel` | `PanelContext` methods (`theme`, `sidebarState`, `navGroups`, `currentSection`, `crumb`, `workspace`, `user`) | template → PHP | `src/Twig/PanelExtension.php` | static |
| `templates/components/atoms/icon.html.twig` | Twig global `icons` | name → SVG path map | template → PHP | `src/Twig/IconExtension.php` | static |
| `assets/app.ts` | `POST /preferences/theme`, `/preferences/sidebar` | JSON `{theme}` / `{state}` | browser → app | `src/Controller/PreferencesController.php` | static |
| `assets/controllers/upload.ts` | `POST /import/upload` | multipart `file`, follows redirect | browser → app | `src/Controller/ImportController.php` | static |
| `assets/controllers/editor.ts` | `POST <editor-endpoint>/blocks` | expects HTML | browser → app | **no route** | unknown (missing) |
| `assets/controllers/event-stream.ts` | Mercure hub `/.well-known/mercure?topic=` | SSE, JSON `{html}` | browser → Caddy | `frankenphp/Caddyfile`, `templates/components/organisms/event-stream.html.twig` | static |
| `Tracking\EventIngestion` | Mercure hub (`MERCURE_URL`) | publish JWT, topic `/accounts/{id}/events` | app → Caddy | `config/packages/mercure.yaml` | static |
| `Tracking\EventIngestion` | `cache.app` → Postgres `cache_items` | dedup key, 24 h TTL | app → DB | `config/packages/cache.yaml`, `migrations/Version20260521120000.php` | static |
| `PdoSessionHandler` | Postgres `sessions` | native PDO from DBAL | app → DB | `config/services.yaml` | static |
| Messenger `async`/`failed` | Postgres `messenger_messages` | Doctrine transport, auto_setup | app/worker → DB | `config/packages/messenger.yaml` | static |
| Tracker snippet (`cdn.kivvi-click.io/k.js`) | `POST /collect` | JSON event | store → app | `src/Panel/Content/SettingsCatalog.php` (`TRACKER_SNIPPET`) | inferred (script not in repo) |
| `templates/layout/base.html.twig` | fonts.googleapis.com / fonts.gstatic.com | CSS + fonts | browser → external | template | static |
| `templates/base.html.twig` | cdn.jsdelivr.net (idiomorph, frankenphp-hot-reload) | dev hot reload | browser → external | template (unused legacy base) | static |
| GitHub Actions | Docker build `frankenphp_prod` | push to `main`, `push: false` | CI | `.github/workflows/docker-build.yml` | static |
| Prod stack | external reverse proxy (kivvi.click TLS) | `X-Forwarded-*` headers | proxy → app:23456 | `README.md`, `compose.prod.yaml` | runtime (containers up), proxy config not in repo |

## Hubs and risk

- **High fan-in:** `Panel\Workspace` (sites, tenant name), `Panel\Format`, Twig global `panel`,
  `layout/app.html.twig`, `assets/styles/04-patterns.css` (1981 lines).
- **High fan-out:** `assets/app.ts` (all intents), `config/storybook.php` (257 variants of every component).
- **Cycles:** none observed in `src/` (Tracking → Panel is one-way).
- **Shared mutable state:** session (`panel.*`, `import.*` keys), `cache.app` (dedup + scheduler state
  share one pool).

## Unknown edges

- Tracking script `k.js` source and CDN: not in repository.
- E-mail providers, product-feed sources, webhooks, OAuth: shown in settings catalogues only, no
  integration code.
- External reverse proxy config for kivvi.click: outside the repo (`README.md` gives an example).
<!-- END project-context-initializer:artifact -->
