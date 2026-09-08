<!-- BEGIN project-context-initializer:artifact -->
# Documentation index

Read status: `read-full`, `read-headings`, `indexed`. Authority: `canonical` (governs behaviour),
`supporting`, `advisory`, `historical`.

| Path | Scope | Topic | Authority | Freshness signal | Read | Conflicts | Used by |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `CLAUDE.md` | repo | Stack, architecture decisions, commands, workflow (Claude Code) | canonical | edited in `91f8f85` (2026-08-26) | read-full | framework decision under review (user, 2026-09-08) | all artifacts |
| `AGENTS.md` | repo | Same for Codex + "Current Shape" section | canonical | last edited `356a9e4` (2026-06-25) | read-full | **stale**: lists `HomeController`, `templates/home`, `tests/Controller/HomeControllerTest.php`, `tests/CacheTest.php` — first three no longer exist | overview, risks |
| `README.md` | repo | Run, panel, storybook, tests, e2e, production/proxy | canonical | `91f8f85` | read-full | e2e default port 8543 vs README dev port 8443 (documented override) | delivery |
| `.claude/skills/product-spec/SKILL.md` | product | Vision, features, entities, constraints; says original was Java/Spring + Go + Vue + Kafka | canonical (product) | `069bc7c`, unchanged | read-full | "ignore original stack" vs current migration idea | overview, risks |
| `.claude/settings.json` | tooling | PostToolUse formatter hook (php-cs-fixer / prettier) | supporting | `069bc7c` | read-full | — | delivery |
| `context/foundation/README.md` | context conv. | Foundation-doc conventions (10x tooling) | supporting | 2026-06-25 | read-full | — | related artifacts |
| `context/foundation/prd.md` | product | PRD v1: first slice FR-001..FR-008, non-goals | supporting (draft) | created 2026-06-25 | read-full | says "only localized homepage exists" — stale vs full panel prototype | overview |
| `context/foundation/roadmap.md` | product | Slices S-01..S-04 (account, website, first event audit, manual e-mail queue) | supporting (draft) | 2026-06-25 | read-full | baseline stale; none of the slices implemented, yet prototype screens exist | overview, risks |
| `context/foundation/shape-notes.md` | product | Shaping ledger, decisions, competitor notes | supporting | 2026-06-25 | read-full | same staleness | overview |
| `context/foundation/stack-assessment.md` | stack | Agent-readiness assessment (10x) | advisory | 2026-06-25 | read-full | mentions 4 tests; now 5 test classes | technology |
| `context/foundation/health-check.md` | stack | Dependency/test/CI health snapshot | advisory | 2026-06-25 | read-full | test counts stale | delivery |
| `context/changes/README.md`, `context/archive/README.md` | context conv. | 10x change/archive conventions (empty folders) | supporting | 2026-06-25 | read-full | — | related artifacts |
| `frankenphp/*` comments, `compose*.yaml` comments, `Dockerfile` comments | ops | Inline operational rationale (migrations ownership, volumes, proxy) | canonical | 2026-08-26 | read-full | — | architecture, delivery |
| `config/storybook.php` header | dev | Story registry conventions | supporting | 2026-08-26 | read-headings | header mentions a `storybook.yaml` wiring that is not used (wired in `services.yaml`) | assets/templates contexts |
| Docblocks in `src/**` | code | Design intent per class (e.g. `PanelIdentity` not a security identity) | canonical | 2026-08-26 | read-full | — | architecture |
| `translations/messages.{pl,en}.yaml` | i18n | UI strings; EN 250 lines vs PL 104 lines | canonical | 2026-08-26 | indexed | PL file shorter than EN — many PL strings are hard-coded in PHP/Twig (Inferred) | risks |

No `.docx`/`.doc`/`.rst`/`.adoc` documents found. `kivvi-click.zip` (design handoff archive, ignored)
was not opened: binary, contents stated to live in `assets/`, `templates/`, `config/` (`.gitignore` comment).

## Related artifacts from other skills / tools

| Path | Generator / owner | Type | Status | Route to |
| --- | --- | --- | --- | --- |
| `context/foundation/{prd,roadmap,shape-notes}.md` | 10x-dev shaping/roadmap skills (frontmatter `project`, `/10x-plan` references) | proposal / product record | stale baseline (2026-06-25), roadmap slices `proposed`/`ready`, none delivered | research, implementation-planning (as intent, verify against current code) |
| `context/foundation/{stack-assessment,health-check}.md` | 10x-dev stack-assess / health-check | generated synthesis | stale (test counts, CI unchanged) | review, delivery planning |
| `context/changes/`, `context/archive/` | 10x-dev conventions | convention folders, empty | current | implementation-orchestrator (change folders would land here under 10x flow) |
| `.ai/cezar/` (untracked, own `.gitignore`) | "cezar" tool state: `runs.json` (`[]`), `runs/`, `launch-key` (secret) | tool state | unknown generator; excluded, not read beyond structure | none |
| `context/research/2026-09-08-stack-migration-spring-vue-react.md` | research-spike skill (this pipeline) | generated synthesis / research verdict | current (2026-09-08), ready-for-approval, verdict reject-now with flip condition | implementation-planning (Symfony 8.1 bump, P0 tasks), migration-planning (only if flipped) |
| `context/implementation-runs/**` | — | — | absent | — |
| `.claude/skills/product-spec/SKILL.md` | project maintainer | canonical product evidence | current | everything |
<!-- END project-context-initializer:artifact -->
