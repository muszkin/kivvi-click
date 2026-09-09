<!-- BEGIN project-context-initializer:artifact -->
# Dependencies

Edge status: `static` = observed in source/config, `runtime` = observed in a running system,
`inferred`, `unknown`. Two independent dependency graphs coexist during the migration — old stack
(unchanged) and next stack (this refresh's new evidence).

## Package / workspace lane — old stack (unchanged)

```
kivvi/kivvi-click (composer, PHP >=8.5)
├── symfony/* 8.0.x (framework, twig, asset-mapper, messenger, scheduler, translation, …)
├── doctrine/orm 3.6 + doctrine-bundle 3.2 + migrations-bundle 4.0 + doctrine-messenger
├── symfony/mercure-bundle 0.4
├── sensiolabs/typescript-bundle 0.2
└── dev: phpunit 13, phpstan 2 (+symfony ext), php-cs-fixer 3.95, maker-bundle, browser-kit, css-selector
kivvi-click (yarn 4 PnP, root)      → typescript 6, prettier 3   (typecheck/format only)
kivvi-click-e2e (npm, tests/e2e)    → @playwright/test 1.62      (isolated on purpose; shared oracle for both stacks)
```

## Package / workspace lane — next stack (Observed, `backend/pom.xml`, `frontend/package.json`, `tools/migration-verify/package.json`)

```
backend (Maven, click.kivvi, Java 25)
├── spring-boot-starter-parent 4.1.1
│   ├── spring-boot-starter-web (Tomcat + Jackson 3)
│   ├── spring-boot-starter-actuator
│   ├── spring-boot-starter-session-jdbc
│   ├── spring-boot-starter-flyway + flyway-database-postgresql
│   └── org.postgresql:postgresql (runtime)
├── net.javacrumbs.shedlock:{shedlock-spring, shedlock-provider-jdbc-template} 7.10.0
└── test: spring-boot-starter-webmvc-test, spring-boot-restclient, spring-boot-testcontainers,
    testcontainers-{junit-jupiter,postgresql}, archunit-junit5 1.5.0
frontend (npm, Vite)
├── vue 3.5.42, vue-router 5.3.1, pinia 4.0.3, vue-i18n 11.4.10
└── dev: vite 8.2.2, @vitejs/plugin-vue 6.0.8, vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11,
    typescript 6.0.3, eslint 10.10.0 (+ eslint-plugin-vue, typescript-eslint), prettier 3.8.4
tools/migration-verify (npm, standalone)
└── pixelmatch 7.2.0, pngjs 7.0.0 — reuses Playwright from tests/e2e/node_modules via createRequire
mercure/ — no package manager; dunglas/mercure:v0.24.2 image consumed as-is (AGPL-3.0, unmodified)
```

## Source-module lane — old stack (static, `src/`, unchanged)

```
Controller/*  ──► Panel/Content/*  ──► Panel/{Workspace,Format}
Controller/*  ──► Panel/{EventStreamTopic,ImportUploadStorage,PanelIdentity,PanelPreferences}
Controller/EventIngestionController ──► Tracking/{TrackedEvent,EventIngestion,InvalidEventPayload}
Tracking/EventIngestion ──► Panel/{EventStreamTopic,Workspace}, Panel/Content/EventFeed (TYPES), Twig env, Mercure HubInterface, cache.app
Twig/PanelExtension ──► Panel/PanelContext ──► Panel/{Navigation,Workspace,PanelPreferences,PanelIdentity}
Schedule ──► Message/Heartbeat ──► MessageHandler/HeartbeatHandler
```

## Source-module lane — next stack (static, `backend/src/main/java/click/kivvi/`, `frontend/src/`)

```
backend: web/*Controller ──► application/*ViewService ──► fixtures/*Fixtures + domain/Format
web/CollectController ──► application/tracking/EventIngestionService ──► domain/tracking/{TrackedEvent,EventStreamTopic},
                                                                          infrastructure/tracking/EventDedupStore,
                                                                          infrastructure/mercure/HttpMercurePublisher
web/LoginController, web/PreferencesController ──► application/{LoginService,PreferencesService} ──► infrastructure/session/*
web/SpaDocumentController ──► domain/RouteTable, application/SpaDocumentService
infrastructure/scheduling/SchedulingConfig ──► infrastructure/scheduling/{HeartbeatJob,HeartbeatTrigger} (ShedLock)
[ArchUnit-enforced: domain ↛ web; infrastructure only reached from application; no top-level cycle]

frontend: router/routes.ts ──► views/*View.vue ──► components/{atoms,molecules,organisms}
views/*View.vue ──► composables/useIntents ──► stores/shell.ts ──► fetch GET/POST /api/v1/...
views/EventsView.vue ──► composables/useEventStream ──► EventSource /.well-known/mercure
i18n/index.ts ──► import.meta.glob("./messages/*.{pl,en}.ts") (per-journey catalogues, merged)
[ESLint-enforced: no client-side Intl/toLocaleString/toFixed; no client-side "/accounts/" literal
 outside EventRow.vue/ListCard.vue/RecentImports.vue]
```

## Contract table — old stack (unchanged, browser/CI-facing edges)

| From | To | Contract / reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `templates/layout/app.html.twig` | Twig global `panel` | `PanelContext` methods | template → PHP | `src/Twig/PanelExtension.php` | static |
| `assets/app.ts` | `POST /preferences/theme`, `/preferences/sidebar` | JSON `{theme}` / `{state}` | browser → app | `src/Controller/PreferencesController.php` | static |
| `assets/controllers/upload.ts` | `POST /import/upload` | multipart `file`, follows redirect | browser → app | `src/Controller/ImportController.php` | static |
| `assets/controllers/editor.ts` | `POST <editor-endpoint>/blocks` | expects HTML | browser → app | **no route** (R6) | unknown (missing); the next-stack SPA editor must not call it either (DEV-7) |
| `assets/controllers/event-stream.ts` | Mercure hub `/.well-known/mercure?topic=` | SSE, JSON `{html}` | browser → Caddy | `frankenphp/Caddyfile` | static |
| `Tracking\EventIngestion` | Mercure hub (`MERCURE_URL`) | publish JWT, topic `/accounts/{id}/events` | app → Caddy | `config/packages/mercure.yaml` | static |
| `Tracking\EventIngestion` | `cache.app` → Postgres `cache_items` | dedup key, 24 h TTL | app → DB | `config/packages/cache.yaml` | static |
| GitHub Actions | Docker build `frankenphp_prod` | push to `main`, `push: false` | CI | `.github/workflows/docker-build.yml` | static |
| Prod stack | external reverse proxy (kivvi.click TLS) | `X-Forwarded-*` headers | proxy → app:23456 | `README.md`, `compose.prod.yaml` | runtime (containers up) |

## Contract table — next stack (Observed, browser/CI-facing edges)

| From | To | Contract / reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `frontend/src/stores/shell.ts` | `GET /api/v1/{locale}/shell` | nav/workspace/user/theme/sidebar JSON | browser → api (via edge) | `backend/.../web/ShellController.java` | static |
| `frontend/src/views/*View.vue` | `GET /api/v1/{locale}/<page>` | one JSON view-model per journey (mirrors old `render()` params) | browser → api | `backend/.../web/dto/*Response.java` | static |
| `frontend/src/stores/shell.ts` | `POST /preferences/theme`, `/preferences/sidebar` | JSON, `keepalive: true`, awaited before reload | browser → api | `backend/.../web/PreferencesController.java` | static — **preserved contract, DEV-4 exempt** |
| `frontend/src/composables/useIntents.ts` | `POST /collect` | preserved 202/200/400 contract | browser/tracker → api | `backend/.../web/CollectController.java` | static — preserved contract |
| import view `Dropzone` | `POST /import/upload` | preserved multipart → 302 contract | browser → api | `backend/.../web/ImportUploadController.java` | static — preserved contract |
| `frontend/src/composables/useEventStream.ts` | Mercure hub `/.well-known/mercure?topic=` | SSE, **JSON event** (DEV-3, was `{html}`) | browser → mercure edge | `mercure/Caddyfile`, `backend/.../infrastructure/mercure/HttpMercurePublisher.java` | static — deviated payload shape, same topic |
| `HttpMercurePublisher` | Mercure hub (`kivvi.mercure.url`) | publish JWT, plain HTTP inside compose network | app → mercure edge | `backend/src/main/resources/application.yml` | static |
| `EventDedupStore` | Postgres `event_dedup` | insert-or-conflict, 24 h TTL | app → DB | `V1__baseline.sql` | static — DEV-5 table-name mapping |
| Spring Session JDBC | Postgres `spring_session`/`spring_session_attributes` | session persistence | app → DB | `V1__baseline.sql` | static — DEV-9 cookie/table-name deviation |
| ShedLock | Postgres `shedlock` | scheduler distributed lock | app → DB | `V1__baseline.sql` | static — DEV-5 mapping |
| `backend/Dockerfile` build | `frontend/dist/` | copies built SPA into `src/main/resources/static/` | build-time | `backend/Dockerfile` stage 1→3 | static |
| `mercure` (edge) | `api:8080` | `reverse_proxy`, all paths except `/.well-known/mercure*` and blocked `/actuator/*` | proxy → app | `mercure/Caddyfile` | static |
| `tools/migration-verify/compare.mjs` | candidate stack `--base <url>` + oracle (`context/migration-oracle/**`, read-only) | contract/visual replay | verifier → app + evidence | `tools/migration-verify/compare.mjs` | static |
| GitHub Actions (`next-build.yml`) | `mvnw verify` + `npm run build` | on push/PR touching next-stack paths | CI | `.github/workflows/next-build.yml` | static |

## Cross-stack contracts (the seam the migration must not break)

| Preserved item | Old-stack side | Next-stack side | Deviation |
| --- | --- | --- | --- |
| `POST /collect` request/response bodies | `EventIngestionController` | `CollectController` | none (byte-equal after normalization, DEV-4 exempt) |
| `POST /preferences/theme\|sidebar` | `PreferencesController` | `PreferencesController` | none |
| `POST /import/upload` (multipart → 302) | `ImportController` | `ImportUploadController` | none |
| `/.well-known/mercure` SSE subscription, topic `/accounts/1/events` | Caddy-embedded hub | standalone `mercure` edge container | payload shape only (DEV-3) |
| Locale-prefixed URLs `/{pl\|en}/…`, 404 for unknown locale/customer/tab/step | Symfony routing | `RouteTable.java` + `SpaDocumentController` | document body not diffed (DEV-4), 404 page body not diffed (DEV-12) |
| `tests/e2e/specs/*.spec.ts` | exercises old stack at its own port | exercises next stack via `E2E_BASE_URL` override, specs unchanged | none — a spec change is itself a regression signal (K8) |

## Hubs and risk

- **High fan-in (old stack, unchanged):** `Panel\Workspace`, `Panel\Format`, Twig global `panel`,
  `layout/app.html.twig`, `assets/styles/04-patterns.css`.
- **High fan-in (next stack):** `backend/.../domain/Format.java` (every `*ViewService`),
  `frontend/src/stores/shell.ts` (every view), `frontend/src/composables/useIntents.ts` (every
  interactive component), `mercure/Caddyfile` (the only public entry point for the whole next stack).
- **High fan-out:** `assets/app.ts` (old) / `frontend/src/composables/useIntents.ts` (next) — both
  are the single dispatcher for every `data-action` in their stack.
- **Cycles:** none observed in either stack's source graph; ArchUnit enforces this for the next
  stack (`ArchitectureTest.topLevelPackagesFormNoCycle`).
- **Shared mutable state:** old stack's session/cache keys vs. next stack's `spring_session`/
  `event_dedup`/`shedlock` tables — entirely separate Postgres databases (`compose.yaml`'s
  `database` vs. `compose.next.yaml`'s `database`, no shared volume), so there is no runtime
  coupling between the two stacks today.

## Unknown edges

- Tracking script `k.js` source and CDN: not in repository (either stack).
- E-mail providers, product-feed sources, webhooks, OAuth: shown in settings catalogues only, no
  integration code (either stack).
- External reverse proxy config for kivvi.click: outside the repo.
- Whether `frontend`'s SPA editor components (`EmailEditorView.vue`, `PopupEditorView.vue`) call any
  network endpoint for block drops: the plan records DEV-7 — the old stack's dead
  `POST <editor>/blocks` call (404 today, R6) is deliberately **not** reproduced; drag/drop stays
  local with no request on either stack.
<!-- END project-context-initializer:artifact -->
