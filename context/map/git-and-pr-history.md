<!-- BEGIN project-context-initializer:artifact -->
# Git and PR history

## Window

- Repository: `origin git@github.com:muszkin/kivvi-click.git`, branch `main`, HEAD `91f8f85`.
- Full history, not shallow: 11 commits, 2026-05-21T13:28+02:00 → 2026-08-26T18:27Z. No tags, no merges.
- Working tree: clean tracked files; untracked `.ai/` only.
- Authors: `piotr <piotr@magellanerp.com>` (6 commits, May 2026) and `Piotr Mucha <muszkin@gmail.com>`
  (5 commits, June–August 2026). Same person under two identities (Inferred from name/email; no `.mailmap`).

## Commits (all)

| SHA | Date | Files | Summary | Noise class |
| --- | --- | --- | --- | --- |
| `069bc7c` | 2026-05-21 | 4 | chore: project guidance + Claude config | docs |
| `e18c20e` | 2026-05-21 | 55 | feat: scaffold Symfony 8 on FrankenPHP + Postgres | scaffold (generated recipe files, `composer.lock`) |
| `1efa95d` | 2026-05-21 | 22 | feat: Mercure, scheduler, TypeScript, Postgres cache | manual + lock |
| `f7c33cb` | 2026-05-21 | 2 | fix: php `>=8.5`, project name | manual |
| `ab2b6fa` | 2026-05-21 | 2 | feat: worker service | manual |
| `0851737` | 2026-05-21 | 4 | feat: prod worker + one-shot migrations | manual |
| `356a9e4` | 2026-06-25 | 1 | docs: AGENTS.md | docs |
| `d02a3b3` | 2026-06-26 | 24 | "update" (`.pnp.cjs` 7.8k lines, `context/foundation/*`, tsconfig, typecheck) | mega/generated (Yarn PnP) |
| `bdf120e` | 2026-08-26 | 215 | feat: full panel design system, controllers, e2e suite | **mega commit** — whole prototype in one change |
| `812db3f` | 2026-08-26 | 3 | fix: gate live-row prepends on open Mercure subscription, honour pause | manual |
| `91f8f85` | 2026-08-26 | 10 | feat: run behind TLS-terminating proxy on port 23456 | manual, ops |

## Hotspots (commits touching, excluding the two mega commits' bulk)

Most-touched by commit count: `.gitignore` (7), `composer.json`/`composer.lock`/`CLAUDE.md` (5),
`README.md` (4), `compose.yaml`, `compose.prod.yaml`, `.env`, `frankenphp/docker-entrypoint.sh`,
`compose.override.yaml`, `assets/styles/app.css`, `config/reference.php` (3). All still tracked.
Historical paths no longer present: `src/Controller/HomeController.php`, `templates/home/index.html.twig`,
`tests/Controller/HomeControllerTest.php`, `assets/app.js`, `package-lock.json` (root).

## Co-change

- Ops cluster (recurring, 3 commits): `compose.yaml` + `compose.prod.yaml` + `compose.override.yaml`
  + `frankenphp/docker-entrypoint.sh` + `.env` + `README.md`/`CLAUDE.md`.
- Everything under `src/`, `templates/`, `assets/`, `tests/`, `config/storybook.php`, translations
  co-changed only inside `bdf120e` — one giant commit, so no recurring coupling signal yet.
- Live-stream trio (`812db3f`): `assets/controllers/event-stream.ts`, `templates/.../event-stream.html.twig`,
  `tests/e2e/specs/events.spec.ts`.

## Direction of recent work

Scaffold (May) → product shaping docs (June) → complete UI prototype with e2e (late August) →
production hosting behind proxy (late August). Next documented step: roadmap slice S-01
(account/login/tenant shell) — not started. Conversation on 2026-09-08 raises a stack migration
question (Java/Spring Boot + Vue/React) before executing Linear tasks.

## PR and review history

Provider: GitHub via `gh` (already authenticated, read-only `gh pr list --state all --limit 20`,
queried 2026-09-08). Result: **0 pull requests**. No branch protection or review signals available
locally. Issue tracking per `CLAUDE.md` is GitHub Issues, but memory notes a Linear backlog
("project kivvi, P0–P5 labels") — Contradiction recorded in `risks-and-unknowns.md`. Linear was not
queried in this run.

## Contact signals

Single contributor (Piotr Mucha). Historical activity is not formal ownership.
<!-- END project-context-initializer:artifact -->
