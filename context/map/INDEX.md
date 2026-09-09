<!-- BEGIN project-context-initializer:artifact -->
# kivvi-click — project context index

| Field | Value |
| --- | --- |
| Project | kivvi-click — Symfony 8/PHP 8.5 marketing-automation SaaS, **mid-migration** to Spring Boot 4.1.1 (Java 25) + Vue 3 SPA |
| Scope | whole repository (`scope_kind: repository`), both coexisting stacks |
| Operation | refresh |
| Classification | brownfield (design-complete UI prototype on both stacks; no domain entities yet on either) |
| Source | `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue` (worktree `/home/muszkin/work/kivvi-click-wt/integration`), refreshed on the feature branch mid-run |
| Generated | 2026-09-08T13:17Z (initial) · refreshed 2026-09-09 (this run, Phase 5 context reconciliation) |
| Coverage / freshness | inventory complete, coverage complete, freshness complete for both stacks; old-stack tests/CI/e2e **not re-run** by this refresh; next-stack final verification cohort **still running** (contract dimension) — see manifest verdicts and R1/R12 |

**Precedence:** current user instructions, `CLAUDE.md`/`AGENTS.md`, code, runtime behaviour and
canonical docs outrank this generated map. `CLAUDE.md`/`AGENTS.md` describe only the old stack and
are **stale for the next stack by design** until the `CON-1` cutover packet rewrites them — see R19
in `risks-and-unknowns.md`. Verify stale or high-risk claims before acting.

## Two stacks, one repository

The old Symfony stack (`src/`, `templates/`, `assets/`, `config/`, `frankenphp/`) is unchanged and
still serves production on port 23456. The next Spring Boot + Vue 3 SPA stack (`backend/`,
`frontend/`, `mercure/`, `tools/migration-verify/`) exists on this branch, behind
`compose.next.yaml`/`compose.next.prod.yaml`, not yet cut over. See `project-overview.md` and
`architecture-and-flows.md` for the full picture, and the migration plan/oracle below for how the
two are being reconciled.

## Task router

| Consumer | Read next |
| --- | --- |
| research | `project-overview.md`, `documentation-index.md` (incl. related 10x foundation docs), relevant scoped context, `risks-and-unknowns.md` |
| implementation planning | overview, `technology.md`, `architecture-and-flows.md`, `dependencies.md`, `delivery-and-verification.md`, `git-and-pr-history.md` (hotspots), risks, scoped contexts of touched directories |
| migration execution (this run, R1 IN EXECUTION) | `../plans/2026-09-08-symfony-to-spring-vue-migration.md` (approved plan), `../implementation-runs/2026-09-08T141500Z-spring-vue-migration/{RUN.md,run.json,common-journey-rules.md}` (live run state), `../migration-oracle/symfony-to-spring-vue/` (oracle, immutable), `backend/.agents/project-context.md`, `frontend/.agents/project-context.md`, `tools/migration-verify/.agents/project-context.md`, `mercure/.agents/project-context.md`, architecture + next-stack diagrams, risks R1/R19/R20/R21 |
| review | architecture, dependencies (contracts table), risks, git co-change, delivery gates, scoped contexts of both stacks |
| `implementation-orchestrator` | approved plan: `../plans/2026-09-08-symfony-to-spring-vue-migration.md`, run ledger `../implementation-runs/2026-09-08T141500Z-spring-vue-migration/`, overview, architecture, dependencies, delivery commands, risks, nearest scoped contexts (old- and next-stack) |
| implementer | approved plan, scoped context of the directory (old or next stack), dependencies, delivery commands, invariants listed in each scoped context |

## Domains

| Domain | Path | Scoped context |
| --- | --- | --- |
| Root, ops, docs, translations, migrations | `.` | [`.agents/project-context.md`](../../.agents/project-context.md) |
| Old-stack application code | `src/` | [`src/.agents/project-context.md`](../../src/.agents/project-context.md) |
| Old-stack panel view-model + sample content | `src/Panel/` | [`src/Panel/.agents/project-context.md`](../../src/Panel/.agents/project-context.md) |
| Old-stack event tracking / ingestion | `src/Tracking/` | [`src/Tracking/.agents/project-context.md`](../../src/Tracking/.agents/project-context.md) |
| Old-stack Symfony configuration | `config/` | [`config/.agents/project-context.md`](../../config/.agents/project-context.md) |
| Old-stack Twig design system + pages | `templates/` | [`templates/.agents/project-context.md`](../../templates/.agents/project-context.md) |
| Old-stack TypeScript + CSS | `assets/` | [`assets/.agents/project-context.md`](../../assets/.agents/project-context.md) |
| Old-stack FrankenPHP/Caddy runtime | `frankenphp/` | [`frankenphp/.agents/project-context.md`](../../frankenphp/.agents/project-context.md) |
| Old-stack PHPUnit suite | `tests/` | [`tests/.agents/project-context.md`](../../tests/.agents/project-context.md) |
| Shared Playwright E2E (acceptance oracle for both stacks) | `tests/e2e/` | [`tests/e2e/.agents/project-context.md`](../../tests/e2e/.agents/project-context.md) |
| **Next-stack Spring Boot API** | `backend/` | [`backend/.agents/project-context.md`](../../backend/.agents/project-context.md) |
| **Next-stack Vue 3 SPA** | `frontend/` | [`frontend/.agents/project-context.md`](../../frontend/.agents/project-context.md) |
| **Next-stack six-dimension verifier tooling** | `tools/migration-verify/` | [`tools/migration-verify/.agents/project-context.md`](../../tools/migration-verify/.agents/project-context.md) |
| **Next-stack Mercure edge config** | `mercure/` | [`mercure/.agents/project-context.md`](../../mercure/.agents/project-context.md) |

## Central artifacts

- [`project-overview.md`](project-overview.md) · [`technology.md`](technology.md) · [`architecture-and-flows.md`](architecture-and-flows.md)
- [`dependencies.md`](dependencies.md) · [`documentation-index.md`](documentation-index.md) · [`delivery-and-verification.md`](delivery-and-verification.md)
- [`git-and-pr-history.md`](git-and-pr-history.md) · [`risks-and-unknowns.md`](risks-and-unknowns.md)
- Diagrams — old stack: [`diagrams/module-dependencies.mmd`](diagrams/module-dependencies.mmd), [`diagrams/primary-runtime-flow.mmd`](diagrams/primary-runtime-flow.mmd)
- Diagrams — next stack: [`diagrams/next-stack-module-dependencies.mmd`](diagrams/next-stack-module-dependencies.mmd), [`diagrams/next-stack-event-flow.mmd`](diagrams/next-stack-event-flow.mmd)
  (all Mermaid source, rendering not run)
- Manifest: [`manifest.json`](manifest.json) — rolled-up and excluded directories, fingerprints, verdicts.

## Research artifacts

- [`../research/2026-09-08-stack-migration-spring-vue-react.md`](../research/2026-09-08-stack-migration-spring-vue-react.md) — research-spike verdict on R1 (Spring Boot + Vue/React migration): adopt-with-constraints (flip condition met — see the plan); also finds Symfony 8.0 unmaintained since 2026-07-31 (R16).

## Plan, oracle and run ledger

- [`../plans/2026-09-08-symfony-to-spring-vue-migration.md`](../plans/2026-09-08-symfony-to-spring-vue-migration.md) — migration plan (approved 2026-09-08): 6 waves, 13 journeys, deviations DEV-1..13, cutover packets RR-1/CUT-1/CON-1.
- `../migration-oracle/symfony-to-spring-vue/` — immutable pre-migration oracle captured on `5b806ac` (manifest SHA-256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`); do not edit.
- `../implementation-runs/2026-09-08T141500Z-spring-vue-migration/` — live run ledger (not tracked by
  Git — local to the main checkout `/home/muszkin/work/kivvi-click`, outside this worktree):
  `run.json` (machine-readable state, current), `RUN.md` (human summary, **stale** — last updated at
  wave-2), `common-journey-rules.md` (cross-journey invariants, federated into `backend/`, `frontend/`
  and `tools/migration-verify/` scoped contexts), `waves/`, `slices/`, `events.jsonl`. As of this
  refresh: final all-journey cohort PASSED 6/6 on `dae1696` (2026-09-09); FEATURE_LOCAL_GREEN pending the final gates, not yet
  `CUTOVER_READY`.

## Canonical project documents

`README.md`, `CLAUDE.md`, `AGENTS.md`, `.claude/skills/product-spec/SKILL.md` — **stale for the next
stack** until `CON-1` (R19); preserved 10x foundation docs
`context/foundation/{prd,roadmap,shape-notes,stack-assessment,health-check}.md` (stale baseline
2026-06-25, see `documentation-index.md`).

## Unresolved contradictions (details in `risks-and-unknowns.md`)

- R1 Stack: **Decided 2026-09-08, IN EXECUTION** — migrating to Spring Boot + Vue 3 SPA; final cohort running on `dae1696`, contract dimension pending, not yet `CUTOVER_READY`.
- R2 `AGENTS.md` "Current Shape" lists deleted files.
- R3 Foundation docs describe a homepage-only baseline; a full prototype exists (on both stacks now).
- R5 Old-stack dev and prod compose stacks share one project name; prod containers run on this host (next-stack leases are already isolated).
- R8 Issue tracker: GitHub Issues (`CLAUDE.md`) vs Linear (memory, user).
- R17 Production was down 2026-09-07 → 2026-09-08 (database container exited, no restart policy); repaired, policy added. Monitoring still absent (PIO-112).
- **R19 (new)** `AGENTS.md`/`CLAUDE.md` still describe only the old stack; stale by design until `CON-1`.
- **R20 (new)** Fixtures implemented as Java classes (`backend/.../fixtures/*.java`) vs. the plan's stated JSON resources — accepted drift, no code change needed.
- **R21 (new)** Host disk/CPU contention from unrelated projects' containers (including self-hosted GitHub Actions runners) affects verification runs — re-run ambiguous failures in isolation.

## Freshness rule

Refresh when `HEAD`, instruction files, manifests/lockfiles, or any mapped directory changes (either
stack); `manifest.json` records the source snapshot and per-artifact input fingerprints used for
comparison. This refresh's own trigger: Phase 5 (context reconciliation) of the
`implementation-orchestrator` run for `migration/spring-vue`.
<!-- END project-context-initializer:artifact -->
