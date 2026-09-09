<!-- BEGIN project-context-initializer:artifact -->
# Documentation index

Read status: `read-full`, `read-headings`, `indexed`. Authority: `canonical` (governs
behaviour), `supporting`, `advisory`, `historical`.

| Path | Scope | Topic | Authority | Freshness signal | Read | Conflicts | Used by |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `CLAUDE.md` | repo | Stack, architecture decisions, commands, workflow (Claude Code) | canonical | rewritten `fe9c3fe` (2026-09-09, CON-1) | read-full | none open | all artifacts |
| `AGENTS.md` | repo | Same for Codex, incl. "Current Shape" | canonical | rewritten `fe9c3fe` (2026-09-09, CON-1) | read-full | R22 resolved; current formatter prerequisites documented | overview, risks |
| `README.md` | repo | Run, panel, tests, e2e, production/proxy, operations | canonical | rewritten `fe9c3fe` (2026-09-09, CON-1) | read-full | none open | delivery |
| `.claude/skills/product-spec/SKILL.md` | product | Vision, features, entities, constraints | canonical (product) | unchanged since prior refresh | read-headings (unchanged content, not re-read in full this pass) | says original was Java/Spring + Go + Vue + Kafka — explicitly "ignore original stack" per the same doc, not a live contradiction | overview |
| `.claude/settings.json` | tooling | PostToolUse formatter hook | supporting | updated c846294 | read-full | Java and frontend formatting verified; R22 resolved | delivery, risks |
| `backend/README.md` | backend | Build/run/test instructions, endpoint list | canonical | unchanged since wave-0 (`w0-login`-slice framing: "Endpoints introduced in this slice") | read-full | endpoint list is partial (only the login-slice subset), not a full API reference — minor staleness, not corrected here (out of scope for this refresh to edit) | backend context, delivery |
| `frontend/README.md` | frontend | Build/run/test instructions | canonical | current | indexed (not re-read in full this pass; content already reflected in `frontend/.agents/project-context.md`) | none observed | frontend context, delivery |
| `docs/adr/0001-serialize-requests-per-session-like-php.md` | backend | Session-lock parity decision | canonical | 2026-09-09 | read-full | none | architecture, backend context |
| `docs/adr/0002-spa-reload-scroll-restoration-via-router-scrollbehavior.md` | frontend | Scroll restoration decision | canonical | 2026-09-09 | read-full | none | architecture, frontend context |
| `compose.yaml`, `compose.prod.yaml`, `mercure/Caddyfile` (inline comments) | ops | Operational rationale (Mercure internal-URL trick, volume isolation, forwarded headers) | canonical | 2026-09-09 | read-full | current-stack comments; runtime directives preserved (R23 resolved) | architecture, delivery, risks |
| `.env`, `.env.example`, `.env.prod.docker.example` (key names only, values not read) | ops | Compose substitution defaults | canonical | production template cleaned in c846294 | read-full (key names) | R23 resolved | technology, risks |
| `.github/workflows/build.yml` | CI | Backend/frontend/image build gates | canonical | current, path filters fixed by CON-1 (`0b46320`) | read-full | none | delivery |
| `context/foundation/README.md` | context conv. | Foundation-doc conventions (10x tooling) | supporting | 2026-06-25 | read-full | — | related artifacts |
| `context/foundation/prd.md` | product | PRD v1: first slice FR-001..FR-008, non-goals | supporting (draft) | created 2026-06-25 | read-full | says "only localized homepage exists" — stale vs the full panel that now exists in a second stack | overview |
| `context/foundation/roadmap.md` | product | Slices S-01..S-04 | supporting (draft) | 2026-06-25 | read-full | baseline stale; none of the slices formally implemented, yet prototype screens exist | overview, risks |
| `context/foundation/shape-notes.md` | product | Shaping ledger, decisions, competitor notes | supporting | 2026-06-25 | read-full | same staleness | overview |
| `context/foundation/stack-assessment.md` | stack | Agent-readiness assessment (10x) | advisory | 2026-06-25 | indexed | describes the pre-migration PHP stack | technology (historical) |
| `context/foundation/health-check.md` | stack | Dependency/test/CI health snapshot | advisory | 2026-06-25 | indexed | describes the pre-migration PHP stack | delivery (historical) |
| `context/changes/README.md`, `context/archive/README.md` | context conv. | 10x change/archive conventions (empty folders) | supporting | 2026-06-25 | read-full | — | related artifacts |

No `.docx`/`.doc`/`.rst`/`.adoc` documents found. `kivvi-click.zip` (design handoff archive,
gitignored, historical: superseded by the ported design system in `frontend/src/`) was not
opened: binary, superseded per its own `.gitignore` comment.

## Related artifacts from other skills / tools

| Path | Generator / owner | Type | Status | Route to |
| --- | --- | --- | --- | --- |
| `context/foundation/{prd,roadmap,shape-notes}.md` | 10x-dev shaping/roadmap skills | proposal / product record | stale baseline (2026-06-25), roadmap slices `proposed`/`ready`, none formally delivered | research, implementation-planning (verify against current code) |
| `context/foundation/{stack-assessment,health-check}.md` | 10x-dev stack-assess / health-check | generated synthesis | stale, describes the deleted PHP stack | historical only |
| `context/changes/`, `context/archive/` | 10x-dev conventions | convention folders, empty | current | future 10x-flow changes |
| `.ai/cezar/` (untracked, own `.gitignore`) | "cezar" tool state: `runs.json`, `runs/`, `launch-key` (secret) | tool state | unknown generator; excluded, not read beyond structure | none |
| `context/research/2026-09-08-stack-migration-spring-vue-react.md` | research-spike skill | generated synthesis / research verdict | current, verdict fed R1's decision | historical reference for R1 |
| `context/plans/2026-09-08-symfony-to-spring-vue-migration.md` | migration-planning skill | plan (approved, executed) | approved 2026-09-08, errata + DEV-13 retirement recorded in-place (`8c37b92`, 2026-09-09) | implementation-orchestrator/implementer for any parity question |
| `context/migration-oracle/symfony-to-spring-vue/` | migration-planning skill | canonical evidence (immutable oracle) | captured 2026-09-08 on `5b806ac`, manifest sha256 `4945a8deb19ea342…`; still the reference for `tools/migration-verify/` | migration-verify tooling; never edited |
| `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/` | implementation-orchestrator run | execution evidence (run ledger) | terminal outcome reached (`closeout.md`); `RUN.md` carries a later-appended "Outcome"/"Cutover" section but its header table is stale (last edited at wave-2) | delivery-and-verification, risks, backend/frontend/mercure/tools scoped contexts (`common-journey-rules.md` federated into each) |
| `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md` | a separate read-only verification session, 2026-09-09T16:04-16:07Z | execution evidence (post-CON-1 verification) | current, PASS — full Playwright suite (66/66) + 4 performance budgets against production `https://kivvi.click` | delivery-and-verification (cited as the most recent e2e/perf evidence) |
| `docs/adr/0001`, `docs/adr/0002` | implementation-orchestrator run (accepted decision records) | decision record | accepted, 2026-09-09 | architecture-and-flows, backend/frontend scoped contexts |
| `.claude/skills/product-spec/SKILL.md` | project maintainer | canonical product evidence | current | everything |
<!-- END project-context-initializer:artifact -->
