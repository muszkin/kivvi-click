<!-- BEGIN project-context-initializer:artifact -->
# Git and PR history

## Window

- Repository: `origin git@github.com:muszkin/kivvi-click.git`, branch `main`, HEAD
  `fe9c3fe06b919302d322994438a8fbb177c8b2a0`.
- Full history, not shallow: 179 commits total, 2026-05-21 → 2026-09-09. No tags.
- **Local `main` is 168 commits ahead of `origin/main` and 1 behind** (`git status
  --branch`/`git log HEAD..origin/main` → one commit, `3cf3dda "format"`, exists only on the
  remote). The entire migration — plan, run ledger, squash merge, cutover, CON-1 — has been
  committed locally but **not pushed**. See risk note below.
- Working tree: clean tracked files; untracked `.ai/` (excluded, contains a secret) only.
- Author since `91f8f85`: `Piotr Mucha <muszkin@gmail.com>` for all 168 commits (single
  contributor for this whole window; two earlier author identities from the May 2026 scaffold
  are unchanged from the prior refresh, no `.mailmap`).

## Commits (window since the prior map's baseline `91f8f85`, 168 commits — grouped, not
itemized; full detail in `git log`)

| Phase | Commit range (first…last) | Count (approx.) | Noise class |
| --- | --- | --- | --- |
| Migration planning + oracle capture | early `context/research/*`, `context/plans/*`, `context/migration-oracle/**` commits | ~10 | manual (planning artifacts) |
| Migration run — wave-0..wave-5 + final cohort | `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/**` progress commits (`waves/`, `slices/`, `events.jsonl`, `run.json`) | ~140 | **mega-noise as a group** — almost entirely generated run-ledger JSON/markdown, one append per verifier round; not source-code churn |
| Squash merge | `8b224c5` "feat: migrate the panel to Spring Boot 4.1 + Vue 3 SPA (squash of migration/spring-vue @ 9205294)" | 1 (404 files) | **mega commit** — the entire `backend/`, `frontend/`, `mercure/`, `tools/migration-verify/` trees land in one commit |
| Cutover docs | `24e0327`…`bed6361`, `a385cf3` (RR-1/CUT-1 rehearsal/attempts/observation) | ~6 | manual, ops/docs |
| CON-1 (old-stack removal) | `7287390` (Symfony removal, 15 files, actually a large deletion — the tracked-file count under my filter undercounts because most deleted paths are outside the six directories filtered here), `25329ac` (drop PHP tests), `8c37b92` (plan errata + DEV-13 retirement), `0b46320` (CI path-filter fix) | 4 | manual, deletion-heavy |
| CON-1 (docs rewrite + verification) | `4539828`, `4de4fed` (e2e PASS evidence), `8047d3e` (Linear re-pointing note), `fe9c3fe` (README/CLAUDE/AGENTS rewrite) | 4 | manual |

**Note on the `context/` noise class:** `git log --name-only` over this whole window touches
`context/**` paths ~14,600 times — almost entirely the run ledger's per-round verifier evidence
files (`events.jsonl` appends, one `run.json` diff per state transition, per-round `.md`
snapshots under `waves/`/`slices/`/`final-gates/`/`integration/`). This is expected, intentional
audit-trail churn for an `implementation-orchestrator` run, not a hotspot signal for source code.

## Hotspots — source/config only (excluding `context/**` and the `8b224c5` squash's bulk)

Most-touched paths since `91f8f85` by commit count (≥2): `README.md` (3), `.github/workflows/
build.yml` (3), `tools/migration-verify/{compare,performance}.mjs` (2 each),
`backend/src/main/resources/application.yml` (2),
`backend/src/main/java/click/kivvi/infrastructure/mercure/HttpMercurePublisher.java` (2),
`backend/README.md` (2), `frontend/README.md` (2), `frontend/vite.config.ts` (1, but touched
alongside the vite/tsconfig pair), `.gitignore` (2), `.env.prod.docker.example` (2),
`compose.yaml`/`compose.prod.yaml` (2 each, plus their pre-rename `compose.next*.yaml`
predecessors), `CLAUDE.md`/`AGENTS.md` (2 each — the CON-1 rewrite on top of the migration
plan's original addition). Every scoped `.agents/project-context.md` shows exactly 2 touches
(created mid-migration, refreshed again by CON-1's docs pass before this refresh) — expected for
generated context files, not a code-churn signal.

Historical paths no longer present (deleted by CON-1, `7287390`/`25329ac`): `src/**`,
`templates/**`, `assets/**`, `config/**`, `frankenphp/**`, `migrations/**`, `translations/**`,
`bin/**`, `public/**`, `var/**`, `tests/Controller/**`, `tests/Tracking/**`,
`compose.next.yaml`, `compose.next.prod.yaml` (renamed to `compose.yaml`/`compose.prod.yaml` at
cutover), `.github/workflows/docker-build.yml` and `.github/workflows/next-build.yml` (both
folded into the single `.github/workflows/build.yml`).

## Co-change

- **Squash-merge co-change** (`8b224c5`, 404 files): the entire `backend/`, `frontend/`,
  `mercure/`, `tools/migration-verify/` trees landed together — expected for a squash, not a
  recurring coupling signal.
- **CON-1 removal cluster** (`7287390`): every old-stack top-level directory deleted together in
  one commit, plus `.gitignore` updated to stop ignoring now-nonexistent old-stack paths and
  start ignoring the new stack's build outputs.
- **CON-1 docs cluster** (`fe9c3fe`): `README.md` + `CLAUDE.md` + `AGENTS.md` +
  `.github/workflows/build.yml` + `.env`/`.env.example` + `.gitattributes`/`.gitignore` all
  rewritten together — the instruction-file rewrite the migration plan called for.
- **Ops cluster, pre-migration (unchanged, historical):** `compose.yaml` + `compose.prod.yaml` +
  `.env` + `README.md`/`CLAUDE.md` co-changed 3 times before the migration began — the same
  pattern repeats once more at CON-1.

## Direction of recent work

Scaffold (May 2026) → product shaping docs (June) → complete Symfony UI prototype with e2e (late
August) → production hosting behind a proxy (late August) → **stack migration to Spring Boot +
Vue 3, planned and executed end-to-end in a single `implementation-orchestrator` run (Sept 8-9,
2026)**: research verdict → approved plan → 6 waves / 13 journeys → `CUTOVER_READY` → RR-1
rehearsal → CUT-1 production cutover (live 2026-09-09T14:18:42Z) → CON-1 old-stack removal and
instruction-file rewrite. Next documented step: none formally queued in this repository — Linear
re-pointing (PIO-70..115) is recorded as done in the closeout ledger but not independently
verified by this refresh (Linear was not queried).

## PR and review history

Provider: GitHub via `gh` (already authenticated, read-only). `gh pr list --state all --limit
20` (queried 2026-09-09, this refresh): **0 pull requests**, same as the prior refresh — every
commit above, including the entire migration, landed as direct commits to local `main`, and
local `main` itself has not been pushed to `origin` (1 commit behind, 168 ahead — see Window).
No branch protection or review signals available; `origin/main`'s own tip (`3cf3dda`, "format")
was never fetched into a local branch review. Issue tracking per `CLAUDE.md`/`AGENTS.md` is
GitHub Issues, but the closeout ledger and user memory both reference a Linear backlog — see R8
in `risks-and-unknowns.md`, unresolved.

## Contact signals

Single contributor for the entire repository history (Piotr Mucha, two author identities
pre-migration). Historical activity is not formal ownership.
<!-- END project-context-initializer:artifact -->
