<!-- BEGIN project-context-initializer:artifact -->
# Dependencies

Edge status: `static` = observed in source/config, `runtime` = observed in a running system,
`inferred`, `unknown`.

## Package / workspace lane (Observed, `backend/pom.xml`, `frontend/package.json`,
`tools/migration-verify/package.json`)

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
    testcontainers-{junit-jupiter,postgresql}, archunit-junit5
frontend (npm, Vite)
├── vue 3.5.42, vue-router 5.3.1, pinia 4.0.3, vue-i18n 11.4.10
└── dev: vite 8.2.2, @vitejs/plugin-vue 6.0.8, vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11,
    typescript 6.0.3, eslint 10.10.0 (+ eslint-plugin-vue, typescript-eslint), prettier 3.8.4
tools/migration-verify (npm, standalone)
└── pixelmatch 7.2.0, pngjs 7.0.0 — reuses Playwright from tests/e2e/node_modules via createRequire
tests/e2e (npm, standalone — isolated on purpose)
└── @playwright/test ^1.56.0
mercure/ — no package manager; dunglas/mercure:v0.24.2 image consumed as-is (AGPL-3.0, unmodified)
```

## Source-module lane (static, `backend/src/main/java/click/kivvi/`, `frontend/src/`)

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

## Contract table (Observed, browser/CI-facing edges)

| From | To | Contract / reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `frontend/src/stores/shell.ts` | `GET /api/v1/{locale}/shell` | nav/workspace/user/theme/sidebar JSON | browser → api (via edge) | `backend/.../web/ShellController.java` | static |
| `frontend/src/views/*View.vue` | `GET /api/v1/{locale}/<page>` | one JSON view-model per journey | browser → api | `backend/.../web/dto/*Response.java` | static |
| `frontend/src/stores/shell.ts` | `POST /preferences/theme`, `/preferences/sidebar` | JSON, `keepalive: true`, awaited before reload | browser → api | `backend/.../web/PreferencesController.java` | static — preserved contract, ADR 0001 |
| `frontend/src/composables/useIntents.ts` | `POST /collect` | 202/200/400 contract | browser/tracker → api | `backend/.../web/CollectController.java` | static |
| import view `Dropzone` | `POST /import/upload` | multipart → 302 contract | browser → api | `backend/.../web/ImportUploadController.java` | static |
| `frontend/src/composables/useEventStream.ts` | Mercure hub `/.well-known/mercure?topic=` | SSE, JSON event payload (DEV-3) | browser → mercure edge | `mercure/Caddyfile`, `backend/.../infrastructure/mercure/HttpMercurePublisher.java` | static |
| `HttpMercurePublisher` | Mercure hub (`kivvi.mercure.url`) | publish JWT, plain HTTP inside compose network | app → mercure edge | `backend/src/main/resources/application.yml` | static |
| `EventDedupStore` | Postgres `event_dedup` | insert-or-conflict, 24h TTL | app → DB | `V1__baseline.sql` | static |
| Spring Session JDBC | Postgres `spring_session`/`spring_session_attributes` | session persistence | app → DB | `V1__baseline.sql` | static |
| ShedLock | Postgres `shedlock` | scheduler distributed lock | app → DB | `V1__baseline.sql` | static |
| `backend/Dockerfile` build | `frontend/dist/` | copies built SPA into `src/main/resources/static/` | build-time | `backend/Dockerfile` stage 1→3 | static |
| `mercure` (edge) | `api:8080` | `reverse_proxy`, all paths except `/.well-known/mercure*` and blocked `/actuator/*` | proxy → app | `mercure/Caddyfile` | static |
| `tools/migration-verify/compare.mjs` | candidate `--base <url>` + oracle (`context/migration-oracle/**`, read-only) | contract/visual regression replay | verifier → app + evidence | `tools/migration-verify/compare.mjs` | static; last exercised as part of `cutover/con1-e2e.md`'s performance check |
| GitHub Actions (`build.yml`) | `mvnw verify` + `npm run build` | on push/PR touching `backend/**`, `frontend/**`, `compose*.yaml`, `mercure/**`, `tools/migration-verify/**` | CI | `.github/workflows/build.yml` | static |
| External reverse proxy | `mercure` on `localhost:23456` | `X-Forwarded-Proto/Host/For`, TLS termination for `https://kivvi.click` | proxy → edge | `README.md` "Production" | runtime — production live |
| Playwright suite (`tests/e2e/specs/*.spec.ts`) | this stack via `E2E_BASE_URL` | full-suite acceptance oracle, unchanged specs across the whole migration | test → app | `tests/e2e/playwright.config.ts` | static/runtime — last run against production 2026-09-09T16:04-16:07Z, 66/66 PASS (`cutover/con1-e2e.md`) |

## Hubs and risk

- **High fan-in:** `backend/.../domain/Format.java` (every `*ViewService`),
  `frontend/src/stores/shell.ts` (every view), `frontend/src/composables/useIntents.ts` (every
  interactive component), `mercure/Caddyfile` (the only public entry point for the whole stack).
- **High fan-out:** `frontend/src/composables/useIntents.ts` — the single dispatcher for every
  `data-action` in the SPA.
- **Cycles:** none observed in the source graph; ArchUnit enforces this
  (`ArchitectureTest.topLevelPackagesFormNoCycle`).
- **Single point of public ingress:** the `mercure` container is the only container with a
  published host port in both dev and prod compose files — `api` and `database` are reachable
  only over the internal compose network.

## Unknown edges

- Tracking script `k.js` source and CDN: not in repository — deferred to post-cutover Linear
  work (R11).
- E-mail providers, product-feed sources, webhooks, OAuth: shown in settings catalogues only, no
  integration code.
- External reverse proxy config for `kivvi.click`: outside the repo.
- Whether the SPA editor components (`EmailEditorView.vue`, `PopupEditorView.vue`) call any
  network endpoint for block drops: deliberately not reproduced (DEV-7) — drag/drop stays local,
  no request.

## Historical: the migration's own cross-stack seam (no longer live, kept for context)

Before CUT-1, the same contracts above had to match a now-deleted Symfony implementation
byte-for-byte except for the accepted deviations (DEV-1..13, DEV-13 retired unused). That
old-stack side of the dependency graph — `Controller/*` → `Panel/Content/*` → `Panel/{Workspace,
Format}`, Twig templates, `frankenphp/Caddyfile` — no longer exists on disk; see
`git-and-pr-history.md` for the deletion commits if a historical comparison is ever needed.
<!-- END project-context-initializer:artifact -->
