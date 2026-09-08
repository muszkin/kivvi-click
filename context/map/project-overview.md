<!-- BEGIN project-context-initializer:artifact -->
# Project overview — kivvi-click

Source: `91f8f85` on `main`, worktree clean except untracked `.ai/` (excluded). Generated 2026-09-08.
Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

kivvi-click is a from-scratch rebuild of a multi-tenant SaaS marketing-automation platform for
e-commerce sites: track visitor events, configure rule-based automations, deliver popups / emails /
coupons / product recommendations. Canonical product intent: `.claude/skills/product-spec/SKILL.md`
(Observed). Product requirements and the first-slice roadmap: `context/foundation/prd.md`,
`context/foundation/roadmap.md` (Observed, dated 2026-06-25, partly stale — see contradictions).

## Actors and surfaces

| Actor | Surface | Evidence |
| --- | --- | --- |
| Store owner (primary persona) | Server-rendered panel under `/{pl\|en}/…`: dashboard, event stream, customers + profile, automations + rule editor, campaigns + email editor, popups/widgets + editor, product feeds, 4-step import wizard, 8 settings tabs | `src/Controller/*.php`, `templates/pages/**` |
| Visitor on a tracked store | Tracking script (planned, ~2KB) → `POST /collect` | `src/Controller/EventIngestionController.php`, `src/Panel/Content/SettingsCatalog.php` (`TRACKER_SNIPPET`) |
| Public visitor | Landing page `/`, `/pl`, `/en`; `/{locale}/demo` redirects into the panel | `src/Controller/LandingController.php` |
| Developer | Component storybook `/_storybook` (dev/test only) | `src/Controller/StorybookController.php`, `config/storybook.php` |
| Operator | Docker compose stacks (dev + prod on this host), GitHub Actions image build | `compose*.yaml`, `Dockerfile`, `.github/workflows/docker-build.yml` |

## Current maturity (Observed)

- **Design-complete prototype, domain-model absent.** Every panel screen renders from hard-coded
  catalogues in `src/Panel/Content/*.php` (customers, automations, campaigns, widgets, feeds,
  settings, import, landing). `src/Entity/` and `src/Repository/` contain only `.gitignore`.
  No Doctrine entities, no `security.yaml`, no user/account persistence.
- **Real behaviour that exists end to end:**
  - event ingestion: `POST /collect` → `App\Tracking\TrackedEvent` validation → dedup by
    `idempotency_id` in the Postgres-backed cache (24 h TTL) → Mercure publish of a
    server-rendered `event-row` fragment on topic `/accounts/1/events` (`src/Tracking/EventIngestion.php`);
  - live stream subscription in the browser (`assets/controllers/event-stream.ts`);
  - session-backed panel identity (login form validates e-mail only), theme and sidebar
    preferences (`src/Panel/PanelIdentity.php`, `PanelPreferences.php`, `PreferencesController.php`);
  - import upload stored under `var/import` with a random name (`src/Panel/ImportUploadStorage.php`);
  - Postgres-backed sessions, UNLOGGED cache table, Doctrine Messenger transport, Symfony
    Scheduler heartbeat every hour (`migrations/Version20260521120000.php`, `src/Schedule.php`).
- **Tenant boundary is a constant** (`EventStreamTopic::CURRENT_ACCOUNT = '1'`, `Workspace::NAME`).
  Multi-tenancy is a product constraint, not yet implemented.

## Repository boundaries

Single Symfony application (`scope_kind: repository`). Domains by directory:

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, translations, migrations | `.` | `.agents/project-context.md` |
| Application code | `src/` | `src/.agents/project-context.md` |
| Panel view-model and sample content | `src/Panel/` | `src/Panel/.agents/project-context.md` |
| Event tracking / ingestion | `src/Tracking/` | `src/Tracking/.agents/project-context.md` |
| Symfony configuration | `config/` | `config/.agents/project-context.md` |
| Twig design system and pages | `templates/` | `templates/.agents/project-context.md` |
| TypeScript + CSS assets | `assets/` | `assets/.agents/project-context.md` |
| Runtime image config (FrankenPHP/Caddy) | `frankenphp/` | `frankenphp/.agents/project-context.md` |
| PHPUnit suite | `tests/` | `tests/.agents/project-context.md` |
| Playwright E2E suite | `tests/e2e/` | `tests/e2e/.agents/project-context.md` |

## Primary flows

1. Panel page render: request → `{_locale}` route → controller → `Panel\Content\*` catalogue →
   `templates/pages/*.html.twig` extending `layout/app.html.twig` (shell from Twig global `panel`).
2. Event ingestion and live stream: see `architecture-and-flows.md`.
3. Async/scheduled work: `worker` container runs `messenger:consume async scheduler_default`;
   only the `Heartbeat` message exists.

## Explicit non-goals (Observed from docs)

- No platform plugins (PrestaShop, Magento, Shoper, WooCommerce, Shoplo), no platform autodetect,
  no newsletter builder in v1 (`context/foundation/prd.md`).
- No React/Vue/Svelte, Tailwind/Bootstrap, Redis/RabbitMQ/Memcached (`CLAUDE.md`, `AGENTS.md`).
  **Decision state: under review** — the user is considering a Java/Spring Boot + Vue/React
  migration (2026-09-08 conversation, `User-confirmed` intent, `Undecided`). See `risks-and-unknowns.md`.

## Evidence map

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md`, `composer.json`,
`compose.yaml`, `compose.prod.yaml`, `src/**`, `templates/**`, `assets/**`, `tests/**`,
`context/foundation/*.md`.
<!-- END project-context-initializer:artifact -->
