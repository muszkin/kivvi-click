<!-- BEGIN project-context-initializer:artifact -->
# kivvi-click — project context index

| Field | Value |
| --- | --- |
| Project | kivvi-click (Symfony 8 / PHP 8.5 rebuild of a marketing-automation SaaS) |
| Scope | whole repository (`scope_kind: repository`) |
| Operation | initialize |
| Classification | brownfield (design-complete UI prototype + real event-ingestion flow; no domain entities yet) |
| Source | `91f8f85` on `main`, clean tracked tree; untracked `.ai/` excluded |
| Generated | 2026-09-08T13:17Z |
| Coverage / freshness | inventory complete, coverage complete, freshness complete; tests/CI/e2e **not run** (see manifest verdicts) |

**Precedence:** current user instructions, `CLAUDE.md`/`AGENTS.md`, code, runtime behaviour and
canonical docs outrank this generated map. Verify stale or high-risk claims before acting.

## Task router

| Consumer | Read next |
| --- | --- |
| research | `project-overview.md`, `documentation-index.md` (incl. related 10x foundation docs), relevant scoped context, `risks-and-unknowns.md` |
| implementation planning | overview, `technology.md`, `architecture-and-flows.md`, `dependencies.md`, `delivery-and-verification.md`, `git-and-pr-history.md` (hotspots), risks, scoped contexts of touched directories |
| migration planning (stack change under review, R1) | `../research/2026-09-08-stack-migration-spring-vue-react.md` (verdict), architecture + diagrams, `tests/e2e/.agents/project-context.md` (oracle), `technology.md`, risks R1/R3/R8 |
| review | architecture, dependencies (contracts table), risks, git co-change, delivery gates, scoped contexts |
| `implementation-orchestrator` | approved plan (none registered yet), overview, architecture, dependencies, delivery commands, risks, nearest scoped contexts |
| implementer | approved plan, scoped context of the directory, dependencies, delivery commands, invariants listed in each scoped context |

## Domains

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, translations, migrations | `.` | [`.agents/project-context.md`](../../.agents/project-context.md) |
| Application code | `src/` | [`src/.agents/project-context.md`](../../src/.agents/project-context.md) |
| Panel view-model + sample content | `src/Panel/` | [`src/Panel/.agents/project-context.md`](../../src/Panel/.agents/project-context.md) |
| Event tracking / ingestion | `src/Tracking/` | [`src/Tracking/.agents/project-context.md`](../../src/Tracking/.agents/project-context.md) |
| Symfony configuration | `config/` | [`config/.agents/project-context.md`](../../config/.agents/project-context.md) |
| Twig design system + pages | `templates/` | [`templates/.agents/project-context.md`](../../templates/.agents/project-context.md) |
| TypeScript + CSS | `assets/` | [`assets/.agents/project-context.md`](../../assets/.agents/project-context.md) |
| FrankenPHP/Caddy runtime | `frankenphp/` | [`frankenphp/.agents/project-context.md`](../../frankenphp/.agents/project-context.md) |
| PHPUnit suite | `tests/` | [`tests/.agents/project-context.md`](../../tests/.agents/project-context.md) |
| Playwright E2E | `tests/e2e/` | [`tests/e2e/.agents/project-context.md`](../../tests/e2e/.agents/project-context.md) |

## Central artifacts

- [`project-overview.md`](project-overview.md) · [`technology.md`](technology.md) · [`architecture-and-flows.md`](architecture-and-flows.md)
- [`dependencies.md`](dependencies.md) · [`documentation-index.md`](documentation-index.md) · [`delivery-and-verification.md`](delivery-and-verification.md)
- [`git-and-pr-history.md`](git-and-pr-history.md) · [`risks-and-unknowns.md`](risks-and-unknowns.md)
- Diagrams: [`diagrams/module-dependencies.mmd`](diagrams/module-dependencies.mmd), [`diagrams/primary-runtime-flow.mmd`](diagrams/primary-runtime-flow.mmd) (Mermaid source, rendering not run)
- Manifest: [`manifest.json`](manifest.json) — rolled-up and excluded directories, fingerprints, verdicts.

## Research artifacts

- [`../research/2026-09-08-stack-migration-spring-vue-react.md`](../research/2026-09-08-stack-migration-spring-vue-react.md) — research-spike verdict on R1 (Spring Boot + Vue/React migration): **reject now**, flip condition recorded; also finds Symfony 8.0 unmaintained since 2026-07-31.

## Canonical project documents

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md`; preserved 10x
foundation docs `context/foundation/{prd,roadmap,shape-notes,stack-assessment,health-check}.md`
(stale baseline 2026-06-25, see `documentation-index.md`).

## Unresolved contradictions (details in `risks-and-unknowns.md`)

- R1 Stack: **Decided 2026-09-08** — migrate to Spring Boot + Vue 3 SPA (no SSR); `CLAUDE.md`/`AGENTS.md` stack rules are stale until `migration-planning` rewrites them.
- R2 `AGENTS.md` "Current Shape" lists deleted files.
- R3 Foundation docs describe a homepage-only baseline; a full prototype exists.
- R5 Dev and prod compose stacks share one project name; prod containers are running on this host.
- R8 Issue tracker: GitHub Issues (`CLAUDE.md`) vs Linear (memory, user).

## Freshness rule

Refresh when `HEAD`, instruction files, manifests/lockfiles, or any mapped directory changes;
`manifest.json` records the source snapshot and per-artifact input fingerprints used for comparison.
<!-- END project-context-initializer:artifact -->
