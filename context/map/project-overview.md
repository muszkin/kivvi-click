<!-- BEGIN project-context-initializer:artifact -->
# Project overview — kivvi-click

Source: `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main`, refreshed 2026-09-09. Supersedes the
prior mid-migration snapshot (`dae1696` on `migration/spring-vue`, two coexisting stacks).
Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

kivvi-click is a multi-tenant SaaS marketing-automation platform for e-commerce sites: track
visitor events, configure rule-based automations, and deliver popups / emails / coupons / product
recommendations at the right moment. Canonical product intent:
`.claude/skills/product-spec/SKILL.md` (Observed). Product requirements and the first-slice
roadmap: `context/foundation/prd.md`, `context/foundation/roadmap.md` (Observed, dated
2026-06-25, stale — see `risks-and-unknowns.md` R3).

## One stack, post-migration (Observed)

This repository was rebuilt from Symfony 8/PHP 8.5/Twig onto Spring Boot 4.1.1 (Java 25) +
Vue 3.5 SPA via a planned migration (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
approved 2026-09-08). The migration ran to completion:

| Stage | Result | Evidence |
| --- | --- | --- |
| Feature build (13 journeys, 6 waves) | `CUTOVER_READY` reached on branch `migration/spring-vue` @ `92052947a6238ecf4ee177f4e72493122315ce32` | `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/closeout.md` |
| Squash merge to `main` | `8b224c5` "feat: migrate the panel to Spring Boot 4.1 + Vue 3 SPA (squash of migration/spring-vue @ 9205294)" | `git log` |
| RR-1 (rollback rehearsal, `kivvi-stage`/23458) | PASS | `RUN.md` §"Cutover" |
| CUT-1 (production cutover, port 23456) | Attempts 1-2 rolled back (orchestrator/tooling errors, brief outages); attempt 3 LIVE 2026-09-09T14:18:42Z, 28s outage, 20-min observation clean → `PRODUCTION_GREEN` | `RUN.md` §"Cutover", `cutover/cut1/` |
| CON-1 (old-stack removal + docs rewrite) | Symfony/PHP/Twig stack deleted (`7287390`, `25329ac`), workflow path filters fixed (`0b46320`), plan errata + DEV-13 retirement recorded (`8c37b92`), `AGENTS.md`/`CLAUDE.md`/`README.md` rewritten for the new stack (`fe9c3fe`) | `git log`, `cutover/con1-e2e.md` |

Production (`https://kivvi.click`, compose project `kivvi-click`, port 23456 behind a
TLS-terminating reverse proxy) has run this stack continuously since the CUT-1 cutover
(2026-09-09T14:18:25Z database container start; observed still healthy via `docker ps`,
2026-09-09T16:20Z — `api`/`mercure`/`database` all `Up`, `healthy`). A separate, read-only
verification after CON-1 (`cutover/con1-e2e.md`) ran the full Playwright suite against
`https://kivvi.click` and passed 66/66, confirmed the four DEV-8 performance budgets, and found
zero old-stack-only requests (`.php`, `/_storybook`) in the edge access log — see
`delivery-and-verification.md`.

The repository is now a **single deployable**: one Spring Boot jar (`backend/`) serves a Vue 3
SPA (`frontend/`) it bundles onto its own classpath, fronted by the Mercure Hub (`mercure/`) as
the public edge, backed only by PostgreSQL 18. `tools/migration-verify/` and
`context/migration-oracle/symfony-to-spring-vue/` are kept as a **post-cutover regression tool**
(CLAUDE.md: "safe to keep running after cutover") rather than deleted with the rest of the
migration scaffolding.

## Actors and surfaces (Observed)

| Actor | Surface | Served by |
| --- | --- | --- |
| Store owner (primary persona) | Panel under `/{pl\|en}/…`: dashboard, live event stream, customers + 360 profile, automations + rule editor, campaigns + email editor, popups/widgets + editor, product feeds, 4-step import wizard, 8 settings tabs | `backend/.../web/*Controller.java` (JSON view-models) + `frontend/src/views/*View.vue` (one per journey) |
| Visitor on a tracked store | Tracking event ingestion, `POST /collect` | `backend/.../web/CollectController.java` → `application/tracking/EventIngestionService.java` |
| Public visitor | Landing page `/`, `/pl`, `/en`; `/{locale}/demo` redirects into the panel | `backend/.../web/LandingController.java` + `frontend/src/views/LandingView.vue` |
| Operator | Docker Compose stacks (dev `-p kivvi-dev`, prod `-p kivvi-click`), GitHub Actions CI | `compose.yaml`/`compose.prod.yaml`, `backend/Dockerfile`, `.github/workflows/build.yml` |

There is **no** `/_storybook` route or developer-facing component gallery in this stack — it was
explicitly out of scope for the migration (plan §"Out of scope") and was never ported.

## Current maturity (Observed, unchanged in shape by the migration's own "zero-change" rule)

- **Design-complete prototype, domain-model still absent.** Every panel screen renders from
  hard-coded sample data (`backend/src/main/java/click/kivvi/fixtures/*.java` — Java
  records/static lists, not JSON resources; see R20 in `risks-and-unknowns.md`). No persisted
  accounts, customers, or automations; no Doctrine/JPA entity model was ever introduced by the
  migration (the plan's "nothing new is added during the migration" constraint).
- Real behaviour that exists end to end: event ingestion with dedup (`event_dedup` Postgres
  table, 24h TTL) and Mercure publish; live-stream subscription over SSE
  (`frontend/src/composables/useEventStream.ts`); session-backed identity/theme/sidebar
  (Spring Session JDBC + a custom session-lock filter, ADR 0001); import upload storage;
  Postgres-backed sessions/scheduling; hourly scheduler heartbeat (Spring `@Scheduled` + ShedLock,
  inside the single `api` process — no separate worker container).
- Tenant boundary is still a constant (`EventStreamTopic`'s account id); multi-tenancy is a
  product constraint, not yet implemented — explicitly carried over unchanged (every
  auth/tenant Linear task starts only after CON-1, per the plan's "Out of scope").
- Java layering: `backend/src/main/java/click/kivvi/{web,application,domain,infrastructure,
  fixtures}`, enforced by an ArchUnit test (`ArchitectureTest.java`): `domain` never depends on
  `web`; `infrastructure` reachable only from `application`; no top-level cycle.
- Frontend: Vue 3 SPA, one view per journey, Pinia shell store, vue-i18n PL(default)/EN, CSS
  copied byte-identical from the old design system into `frontend/src/styles/`. Navigation is
  always a real document request (`window.location.href`), never `router.push` — this matches
  every transition the old server-rendered app made, and the unchanged Playwright suite still
  asserts it.
- Two accepted, documented parity deviations became formal ADRs during the migration: session
  request serialization to reproduce PHP's per-session lock (`docs/adr/0001`), and vue-router
  `scrollBehavior`-based scroll restoration to reproduce the old stack's reload behaviour
  (`docs/adr/0002`).

## Repository boundaries

`scope_kind: repository`, single stack. Domains by directory:

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, ADRs, product spec | `.` | `.agents/project-context.md` |
| Spring Boot API | `backend/` | `backend/.agents/project-context.md` |
| Vue 3 SPA | `frontend/` | `frontend/.agents/project-context.md` |
| Mercure edge/hub configuration | `mercure/` | `mercure/.agents/project-context.md` |
| Post-cutover parity-check tooling | `tools/migration-verify/` | `tools/migration-verify/.agents/project-context.md` |
| Playwright E2E suite (acceptance oracle) | `tests/e2e/` | `tests/e2e/.agents/project-context.md` |

## Primary flows

See `architecture-and-flows.md` for the full breakdown. In summary: (1) panel page render — the
SPA document is served by `SpaDocumentController`, then the view fetches its own JSON
view-model from `GET /api/v1/{locale}/<page>`; (2) event ingestion and live stream — `POST
/collect` dedups and publishes to the Mercure hub over SSE, the SPA renders the row client-side;
(3) scheduled work — `@Scheduled` + ShedLock inside the single `api` process, no separate worker.

## Explicit non-goals (Observed from docs and the plan)

- No platform plugins (PrestaShop, Magento, Shoper, WooCommerce, Shoplo), no platform autodetect,
  no newsletter builder in v1 (`context/foundation/prd.md`).
- The migration itself introduced nothing new: "Nothing new is added during the migration; every
  improvement waits for the contract stage" (plan §"Outcome"). Auth, tenancy, persisted events,
  the tracking script (`k.js`), and real data are all deferred to post-CON-1 work.
- `/_storybook` parity was explicitly out of scope for the migration and was not built.
- SSR of any page is out of scope by owner decision (plan §"Out of scope") — the SPA has no SSR.

## Evidence map

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md`, `compose.yaml`,
`compose.prod.yaml`, `backend/**`, `frontend/**`, `mercure/**`, `tools/migration-verify/**`,
`tests/e2e/**`, `docs/adr/*`, `context/foundation/*.md`,
`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/{closeout.md,RUN.md,
common-journey-rules.md,cutover/con1-e2e.md}`, `git log`.
<!-- END project-context-initializer:artifact -->
