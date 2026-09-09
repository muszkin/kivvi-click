<!-- BEGIN project-context-initializer:artifact -->
# kivvi-click — project context index

| Field | Value |
| --- | --- |
| Project | kivvi-click — Spring Boot 4.1.1 (Java 25) API + Vue 3.5 SPA marketing-automation SaaS |
| Scope | whole repository (`scope_kind: repository`) |
| Operation | refresh |
| Classification | brownfield (design-complete UI prototype + real event-ingestion flow; no domain entities yet) |
| Source | `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main`, clean tracked tree; untracked `.ai/` excluded |
| Generated | 2026-09-08T13:17Z (initial) · refreshed 2026-09-09T16:20Z (this run, post-cutover/CON-1) |
| Coverage / freshness | inventory complete, coverage complete, freshness complete; this refresh did **not** run tests/CI itself — see `delivery-and-verification.md` and manifest verdicts for what a prior verification session already proved green |

**Precedence:** current user instructions, `CLAUDE.md`/`AGENTS.md`, code, runtime behaviour and
canonical docs outrank this generated map. Verify stale or high-risk claims before acting.

## What changed since the last map

The prior map (source `dae1696` on `migration/spring-vue`) described **two coexisting stacks**
mid-migration. Since then: the migration reached `CUTOVER_READY`, `RR-1` (rollback rehearsal) and
`CUT-1` (production cutover, live 2026-09-09T14:18:42Z on port 23456) both passed, and `CON-1`
removed the old Symfony/PHP/Twig stack entirely (commits `7287390`, `25329ac`, `8c37b92`,
`0b46320`, `4539828`, `4de4fed`, `8047d3e`, `fe9c3fe`) and rewrote `AGENTS.md`/`CLAUDE.md`/
`README.md` for the new stack. `src/`, `templates/`, `assets/`, `config/`, `frankenphp/`,
`migrations/`, `translations/`, `bin/`, `public/`, `var/` and `tests/Controller`/`tests/Tracking`
no longer exist on disk. This refresh rebuilds the map for the single remaining stack:
`backend/` (Spring Boot API), `frontend/` (Vue 3 SPA), `mercure/` (edge/hub config),
`tools/migration-verify/` (kept as a post-cutover parity regression tool), `tests/e2e/`
(unchanged Playwright suite), plus `docs/adr/` (two decision records from the migration) and the
federated `context/plans|research|migration-oracle|implementation-runs/**` evidence.

## Task router

| Consumer | Read next |
| --- | --- |
| research | `project-overview.md`, `documentation-index.md` (incl. related 10x foundation docs and migration evidence), relevant scoped context, `risks-and-unknowns.md` |
| implementation planning | overview, `technology.md`, `architecture-and-flows.md`, `dependencies.md`, `delivery-and-verification.md`, `git-and-pr-history.md` (hotspots), risks, scoped contexts of touched directories |
| review | architecture, dependencies (contracts table), risks, git co-change, delivery gates, `docs/adr/`, scoped contexts |
| `implementation-orchestrator` | overview, architecture/ADRs/flows, dependencies/contracts, delivery and quality-gate commands (`./mvnw verify`, frontend gate sequence, e2e), risks, nearest scoped contexts, prior run ledger `../implementation-runs/2026-09-08T141500Z-spring-vue-migration/` for cross-cutting invariants (`common-journey-rules.md`) |
| implementer | scoped context of the directory, dependencies, delivery commands, invariants listed in each scoped context, `docs/adr/` for accepted parity deviations |

## Domains

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, ADRs, product spec | `.` | [`.agents/project-context.md`](../../.agents/project-context.md) |
| Spring Boot API | `backend/` | [`backend/.agents/project-context.md`](../../backend/.agents/project-context.md) |
| Vue 3 SPA | `frontend/` | [`frontend/.agents/project-context.md`](../../frontend/.agents/project-context.md) |
| Mercure edge/hub config | `mercure/` | [`mercure/.agents/project-context.md`](../../mercure/.agents/project-context.md) |
| Post-cutover parity-check tooling | `tools/migration-verify/` | [`tools/migration-verify/.agents/project-context.md`](../../tools/migration-verify/.agents/project-context.md) |
| Playwright E2E (acceptance oracle) | `tests/e2e/` | [`tests/e2e/.agents/project-context.md`](../../tests/e2e/.agents/project-context.md) |

`tests/` itself has no files of its own now that the PHP suite is gone (`tests/Controller`,
`tests/Tracking` deleted by CON-1) — it is `rolled-up` into `tests/e2e/`'s context; see manifest.

## Central artifacts

- [`project-overview.md`](project-overview.md) · [`technology.md`](technology.md) · [`architecture-and-flows.md`](architecture-and-flows.md)
- [`dependencies.md`](dependencies.md) · [`documentation-index.md`](documentation-index.md) · [`delivery-and-verification.md`](delivery-and-verification.md)
- [`git-and-pr-history.md`](git-and-pr-history.md) · [`risks-and-unknowns.md`](risks-and-unknowns.md)
- Diagrams: [`diagrams/module-dependencies.mmd`](diagrams/module-dependencies.mmd), [`diagrams/primary-runtime-flow.mmd`](diagrams/primary-runtime-flow.mmd) (Mermaid source, rendering not run)
- Manifest: [`manifest.json`](manifest.json) — rolled-up and excluded directories, fingerprints, verdicts.

## Decision records and migration evidence

- [`../../docs/adr/0001-serialize-requests-per-session-like-php.md`](../../docs/adr/0001-serialize-requests-per-session-like-php.md) — session-lock parity filter (accepted).
- [`../../docs/adr/0002-spa-reload-scroll-restoration-via-router-scrollbehavior.md`](../../docs/adr/0002-spa-reload-scroll-restoration-via-router-scrollbehavior.md) — scroll restoration via `scrollBehavior` (accepted).
- [`../research/2026-09-08-stack-migration-spring-vue-react.md`](../research/2026-09-08-stack-migration-spring-vue-react.md) — research-spike verdict that led to R1's decision.
- [`../plans/2026-09-08-symfony-to-spring-vue-migration.md`](../plans/2026-09-08-symfony-to-spring-vue-migration.md) — approved migration plan (6 waves, 13 journeys, deviations DEV-1..13, cutover packets RR-1/CUT-1/CON-1); plan errata and DEV-13 retirement recorded in-place (commit `8c37b92`).
- `../migration-oracle/symfony-to-spring-vue/` — immutable pre-migration oracle captured on `5b806ac` (manifest SHA-256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`); do not edit. Still the reference `tools/migration-verify/` replays against.
- `../implementation-runs/2026-09-08T141500Z-spring-vue-migration/` — the migration's run ledger: `closeout.md` (terminal outcome, open obligations), `RUN.md` (state + cutover section), `common-journey-rules.md` (cross-journey invariants, federated into the scoped contexts below), `cutover/{rr1,cut1,con1}` (rehearsal/cutover/removal evidence, including `cutover/con1-e2e.md` — a **read-only post-CON-1 verification** that ran the full Playwright suite against production and passed 66/66; see `delivery-and-verification.md`), `waves/`, `slices/`, `final-gates/`.

## Canonical project documents

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md`, `backend/README.md`,
`frontend/README.md`, `docs/adr/*`; preserved 10x foundation docs
`context/foundation/{prd,roadmap,shape-notes,stack-assessment,health-check}.md` (stale baseline
2026-06-25, describes a homepage-only app — see `documentation-index.md`).

## Unresolved contradictions (details in `risks-and-unknowns.md`)

- R1 Stack: **Decided and executed (2026-09-09)** — migrated to Spring Boot + Vue 3 SPA; old stack removed by CON-1.
- R5 Dev and prod compose stacks can still share the compose project name (`kivvi-click`) unless dev is started with `-p kivvi-dev`; prod is running under that name on this host right now.
- R8 Issue tracker: GitHub Issues (`CLAUDE.md`) vs Linear (memory, closeout ledger mentions re-pointing PIO-70..115) — `CLAUDE.md` still says GitHub Issues; contradiction not resolved by this refresh.
- R12 This refresh did not itself run tests/CI/e2e; it relies on the migration run's own recorded evidence and the separately-run `cutover/con1-e2e.md` verification (2026-09-09T16:04-16:07Z).
- R17 Production was down 2026-09-07 11:00 UTC → 2026-09-08 (database container exited, no restart policy); repaired, `restart: unless-stopped` added. Monitoring still absent (PIO-112).
- R22 (new) `.claude/settings.json`'s post-edit format hook still matches only `*.php`/`*.ts` — stale for this Java/Vue stack (also noted in `AGENTS.md` itself).
- R23 (new) `.env.prod.docker.example` and `compose.yaml`/`compose.prod.yaml`/`mercure/Caddyfile` still carry comments describing the old stack ("both stacks", "old stack only", "mirrors frankenphp/Caddyfile") that no longer apply now that only one stack exists.

## Freshness rule

Refresh when `HEAD`, instruction files, manifests/lockfiles, or any mapped directory changes;
`manifest.json` records the source snapshot and per-artifact input fingerprints used for
comparison.
<!-- END project-context-initializer:artifact -->
