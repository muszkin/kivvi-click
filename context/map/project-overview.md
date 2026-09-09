<!-- BEGIN project-context-initializer:artifact -->
# Project overview — kivvi-click

Source: `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue`
(worktree `/home/muszkin/work/kivvi-click-wt/integration`), refreshed 2026-09-09. The prior snapshot
(`91f8f85` on `main`, 2026-09-08) is preserved below for the old stack, which is unchanged and still
serves production.
Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

kivvi-click is a from-scratch rebuild of a multi-tenant SaaS marketing-automation platform for
e-commerce sites: track visitor events, configure rule-based automations, deliver popups / emails /
coupons / product recommendations. Canonical product intent: `.claude/skills/product-spec/SKILL.md`
(Observed). Product requirements and the first-slice roadmap: `context/foundation/prd.md`,
`context/foundation/roadmap.md` (Observed, dated 2026-06-25, stale — see `risks-and-unknowns.md`).

## Two stacks, one repository, one product (Observed)

The repository currently carries **both** implementations side by side, on purpose
(`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`, coexistence strategy "parallel
rewrite"):

| Stack | Where | Status | Serves |
| --- | --- | --- | --- |
| Old — Symfony 8 / PHP 8.5 / Twig / vanilla TS | `src/`, `templates/`, `assets/`, `config/`, `frankenphp/`, `compose.yaml`, `compose.prod.yaml`, `Dockerfile` | **Read-only** for every migration journey worker; unchanged since `91f8f85` | Production, `https://kivvi.click` via port 23456, on this host, right now |
| Next — Spring Boot 4.1.1 (Java 25) API + Vue 3.5.42 SPA | `backend/`, `frontend/`, `mercure/`, `tools/migration-verify/`, `compose.next.yaml`, `compose.next.prod.yaml` | In active development on branch `migration/spring-vue`; final all-journey verification cohort running on this SHA (Observed, `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/run.json`: most dimensions PASS, contract dimension still RUNNING; not yet `CUTOVER_READY`) | Nothing yet — runs only behind `compose.next*.yaml` in worker/verification worktrees; production cutover (`CUT-1`) is a separately authorized packet, not reached |

The migration reproduces the old stack's 13 journeys (12 browser journeys + the scheduler
heartbeat) contract-, visual- and behaviour-for-behaviour, using the old stack itself and its
Playwright suite (`tests/e2e/specs/*.spec.ts`, unchanged) as the acceptance oracle, plus a frozen
pre-migration oracle capture (`context/migration-oracle/symfony-to-spring-vue/`). See
`architecture-and-flows.md` for both stacks' runtime shapes and `technology.md` for both stacks'
dependency tables. `CLAUDE.md`/`AGENTS.md` still describe only the old stack as canonical — see R19
in `risks-and-unknowns.md`.

## Actors and surfaces (both stacks serve the same actors)

| Actor | Surface | Old stack | Next stack |
| --- | --- | --- | --- |
| Store owner (primary persona) | Panel under `/{pl\|en}/…`: dashboard, event stream, customers + profile, automations + rule editor, campaigns + email editor, popups/widgets + editor, product feeds, 4-step import wizard, 8 settings tabs | `src/Controller/*.php` renders `templates/pages/**` server-side | `backend/src/.../web/*Controller.java` serves JSON to `frontend/src/views/*View.vue` |
| Visitor on a tracked store | Tracking script (planned, ~2 KB) → `POST /collect` | `src/Controller/EventIngestionController.php` | `backend/.../web/CollectController.java` — same external contract |
| Public visitor | Landing page `/`, `/pl`, `/en`; `/{locale}/demo` redirects into the panel | `src/Controller/LandingController.php` | `backend/.../web/LandingController.java` + `frontend/src/views/LandingView.vue` |
| Developer | Component storybook `/_storybook` (dev/test only) | `src/Controller/StorybookController.php` | Out of scope for the migration (plan §"Out of scope") — no Vue equivalent yet |
| Operator | Docker compose stacks, GitHub Actions image build | `compose*.yaml`, `Dockerfile`, `.github/workflows/docker-build.yml` | `compose.next*.yaml`, `backend/Dockerfile`, `.github/workflows/next-build.yml` |

## Current maturity — old stack (Observed, unchanged since `91f8f85`)

- **Design-complete prototype, domain-model absent.** Every panel screen renders from hard-coded
  catalogues in `src/Panel/Content/*.php`. `src/Entity/` and `src/Repository/` contain only
  `.gitignore`. No Doctrine entities, no `security.yaml`, no user/account persistence.
- Real behaviour that exists end to end: event ingestion with dedup and Mercure publish
  (`src/Tracking/EventIngestion.php`), live-stream subscription (`assets/controllers/event-stream.ts`),
  session-backed identity/theme/sidebar, import upload storage, Postgres-backed sessions/cache/
  messenger, hourly scheduler heartbeat.
- Tenant boundary is a constant (`EventStreamTopic::CURRENT_ACCOUNT = '1'`); multi-tenancy is a
  product constraint, not yet implemented — carried over unchanged into the next stack (plan
  "Out of scope": every Linear task, including auth/tenants, starts only after `CON-1`).

## Current maturity — next stack (Observed, this refresh)

- **Parity rebuild, same maturity ceiling as the old stack by design.** The plan's "zero-change"
  rule means the next stack is not more feature-complete than the old one — it reproduces the same
  sample-content prototype, the same absence of real auth/tenancy, on a different runtime.
- Java layering: `backend/src/main/java/click/kivvi/{web,application,domain,infrastructure,fixtures}`,
  enforced by an ArchUnit test (`backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`).
- One recorded, accepted implementation drift from the plan: fixtures were built as Java classes
  under `click.kivvi.fixtures` (records/static lists), not the JSON resources the plan's decision
  ledger described — see R20 in `risks-and-unknowns.md` and the run ledger's
  `open_obligations` entry `OBL-fixtures-drift`.
- Frontend: Vue 3 SPA, one view per old-stack page, Pinia shell store, vue-i18n PL/EN, CSS copied
  byte-identical from `assets/styles/*.css`. The existing Playwright suite is the acceptance oracle,
  run unchanged against this stack.
- Verification tooling (`tools/migration-verify/`) replays the frozen oracle capture against a
  candidate build across six dimensions (unit, integration, architecture, contract, visual, e2e)
  plus a performance budget check.

## Repository boundaries

Multi-stack repository during coexistence (`scope_kind: repository`). Domains by directory:

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, translations, migrations | `.` | `.agents/project-context.md` |
| Old-stack application code | `src/` | `src/.agents/project-context.md` |
| Old-stack panel view-model and sample content | `src/Panel/` | `src/Panel/.agents/project-context.md` |
| Old-stack event tracking / ingestion | `src/Tracking/` | `src/Tracking/.agents/project-context.md` |
| Old-stack Symfony configuration | `config/` | `config/.agents/project-context.md` |
| Old-stack Twig design system and pages | `templates/` | `templates/.agents/project-context.md` |
| Old-stack TypeScript + CSS assets | `assets/` | `assets/.agents/project-context.md` |
| Old-stack runtime image config (FrankenPHP/Caddy) | `frankenphp/` | `frankenphp/.agents/project-context.md` |
| Old-stack PHPUnit suite | `tests/` | `tests/.agents/project-context.md` |
| Shared Playwright E2E suite (both stacks' acceptance oracle) | `tests/e2e/` | `tests/e2e/.agents/project-context.md` |
| Next-stack Spring Boot API | `backend/` | `backend/.agents/project-context.md` |
| Next-stack Vue 3 SPA | `frontend/` | `frontend/.agents/project-context.md` |
| Next-stack six-dimension verifier tooling | `tools/migration-verify/` | `tools/migration-verify/.agents/project-context.md` |
| Next-stack Mercure edge configuration | `mercure/` | `mercure/.agents/project-context.md` |

## Primary flows

See `architecture-and-flows.md` for the full per-stack breakdown. In summary: (1) panel page
render — server-rendered Twig on the old stack, JSON API + Vue SPA on the next stack; (2) event
ingestion and live stream — the only real domain flow on both stacks, contracts preserved except
the Mercure payload shape (DEV-3: HTML row → JSON event); (3) scheduled work — Symfony Scheduler +
Messenger worker (old) vs. `@Scheduled` + ShedLock inside the single `api` process (next, DEV-6: no
separate worker container).

## Explicit non-goals (Observed from docs and the plan)

- No platform plugins (PrestaShop, Magento, Shoper, WooCommerce, Shoplo), no platform autodetect,
  no newsletter builder in v1 (`context/foundation/prd.md`).
- The migration itself introduces nothing new: "Nothing new is added during the migration; every
  improvement waits for the contract stage" (plan §"Outcome"). Every Linear task (auth, tenants,
  persisted events, `k.js`, real data) starts only after `CON-1`.
- `/_storybook` parity is explicitly out of scope for the migration (plan §"Out of scope") — the
  next stack has no Storybook equivalent yet.
- SSR of any page is out of scope by owner decision (plan §"Out of scope").

## Evidence map

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md`, `composer.json`,
`compose.yaml`, `compose.prod.yaml`, `src/**`, `templates/**`, `assets/**`, `tests/**`,
`context/foundation/*.md`, `backend/**`, `frontend/**`, `mercure/**`, `tools/migration-verify/**`,
`compose.next.yaml`, `compose.next.prod.yaml`, `.github/workflows/next-build.yml`,
`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/{common-journey-rules.md,run.json,RUN.md}`.
<!-- END project-context-initializer:artifact -->
