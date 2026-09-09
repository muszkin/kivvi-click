# Verifier summary — wave FINAL (all journeys, round 2), dimension: architecture

**Dimension status: PASS**, bound to wave SHA `dae169614a52532c130bd34435994d6a914165c0`
(checkout `/home/muszkin/work/kivvi-click-wt/verify-final-architecture`, detached, clean
throughout — `git status --porcelain` empty before/after every probe and at completion).

Scope executed exactly as instructed by
`waves/final/verifier-architecture.md` + plan sections "Verifier contract" and "Global
implementation constraints" (`context/plans/2026-09-08-symfony-to-spring-vue-migration.md`) +
`context/migration-oracle/symfony-to-spring-vue/architecture/rules-translated.md`. Read-only:
no stack started/stopped, no tracked file left modified, no worker reports / review files /
round-1 evidence read.

## Round-2 scope note

Round 2 follows the shell-navigation label repair, commit `dae1696 test: label B02/B03/B21 unit
coverage and pin the main-scroll invariant (#shell-navigation)` — confirmed via `git show --stat
dae1696`: touches only `backend/src/test/java/click/kivvi/application/ShellViewServiceTest.java`,
`frontend/test/unit/AppShell.spec.ts`, `frontend/test/unit/Topbar.spec.ts` (test-only, no
production code, no rule changes). Re-verified these three files individually: `npx eslint
test/unit/AppShell.spec.ts test/unit/Topbar.spec.ts` → exit 0; the backend `ArchitectureTest`
suite (which imports the whole `click.kivvi` package tree, tests included path excluded by
`ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`) is unaffected by a test-only change and reran
green. Architecture verdict for round 2 is unchanged in substance from a from-scratch check of
the SHA — no architecture-relevant file changed.

## rules-translated.md — one row at a time, enforcing gate + proven negative

| # | Rule | Enforcing test/rule/gate | Result on real tree | Negative proven |
| --- | --- | --- | --- | --- |
| 1 | Static typing gate | `mvnw compile` (`-Xlint:all -Werror`, `backend/pom.xml`); `npx vue-tsc --noEmit` (`frontend/package.json` `typecheck`) | both clean (`frontend-typecheck.log`; compile succeeds inside every ArchitectureTest run) | yes — raw-type probe → `COMPILATION ERROR`/`-Werror`; TS type-mismatch probe → exit 2 `TS2322` (`negative-probes.md` §1) |
| 2 | Formatting | `mvnw spotless:check` (bound to `verify` phase, `check-formatting` execution); `npx prettier --check .` | both clean (`backend-spotless-check.log`, `frontend-format-check.log`) | yes — badly-indented Java probe → spotless diff, exit 1; mis-spaced TS probe → prettier `[warn]`, exit 1 (`negative-probes.md` §2) |
| 3 | Test policy fail-on-warning | `FailOnWarnLogExtension` (JUnit, auto-registered via `META-INF/services`); `frontend/test/setup.ts` (console.warn/error + Vue `warnHandler`) + Vitest `dangerouslyIgnoreUnhandledErrors: false` | `FailOnWarnLogExtensionTest` 7/7 pass | yes — end-to-end backend probe (`LOG.warn` inside a real `@Test`) → test failure with the exact policy message; frontend probe (missing required prop → Vue runtime warning) → test failure; unhandled-rejection probe → process exit 1 (`negative-probes.md` §3) |
| 4 | Layering convention | `ArchitectureTest` (`domainDoesNotDependOnWeb`, `infrastructureIsOnlyUsedFromApplication`, `topLevelPackagesFormNoCycle`) | 5/5 pass (`backend-archunit-surefire.txt`) | yes — three independent probes, one per sub-rule, each reverted (`negative-probes.md` §4) |
| 5 | Mercure topic built only server-side | `ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`; `ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic`; ESLint `/accounts\//` literal/template selectors | all pass; frontend `grep` for the literal is empty on the real tree | yes — both ArchUnit tests and the ESLint rule independently probed and reverted (`negative-probes.md` §5) |
| 6 | Row markup in one place | ESLint `vue/no-restricted-class` (`event-row`), exempting only `EventRow.vue`, `ListCard.vue`, `RecentImports.vue` | real tree: exactly those three files render `.event-row` | yes — fourth-file probe → lint error (`negative-probes.md` §6) |
| 7 | Formatting decided server-side | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl`, scoped to everywhere except (unused) `src/format.ts` | clean; `format.ts` not yet created (no client-side formatting introduced by any journey to date) — consistent with `domain/Format.java` being the sole formatter | yes — bare `Intl.NumberFormat` probe → 2 lint errors (`negative-probes.md` §7) |
| 8 | Storybook dev-only | Out of scope for parity (plan Wave column) / DEV-10 | no `stories/`, no storybook tooling on this SHA — nothing to violate | n/a — accepted deviation DEV-10, not a gap |

No row of `rules-translated.md` lacks an equivalent and no row is violated. 0 architecture
violations.

## Global implementation constraints

Full detail and evidence trail: `architecture/global-constraints.md`. Summary:

- **Layering**: proven via `ArchitectureTest`, see row 4 above.
- **Flyway baseline unchanged**: one migration file (`V1__baseline.sql`), one commit in its
  `git log --follow` history (its wave-0 introduction), never touched since.
- **compose.next\*.yaml service scope**: `compose.next.yaml` and `compose.next.prod.yaml` each
  declare exactly `api`, `mercure`, `database` — no fourth backing service.
- **Pinned versions vs. Target stack table**: Spring Boot 4.1.1, Vue 3.5.42, Maven wrapper 3.9.11
  (within `3.9.x`), Mercure `v0.24.2` all match exactly. Java: the build toolchain used is exactly
  Temurin 25.0.4.1+1 as pinned, but `backend/Dockerfile`'s runtime image tags
  (`maven:3.9-eclipse-temurin-25`, `eclipse-temurin:25-jre`) pin only the major version, not the
  exact patch — a pre-existing wave-0 characteristic, not touched by round 2's test-only repair,
  and not affecting any journey's observable behaviour. **Flagged as a non-blocking observation,
  not scored as a FAIL.**
- **No Intl outside format.ts**: enforced and clean; `format.ts` doesn't exist yet (nothing to
  violate).
- **`/accounts/` only in `EventStreamTopic`**: enforced (backend source-scan test) and clean on
  the frontend (`grep` empty).
- **`event-row` only in the three allowed files**: confirmed by direct `grep` on the real tree.
- **i18n pl/en key parity**: the shipped `i18n.spec.ts` covers the base catalogue (3/3 pass). The
  ten per-journey `messages/*.{pl,en}.ts` pairs have **no standing automated gate** for key
  parity — verified by hand with a throwaway Vitest spec (10/10 pairs passed key-parity). Not a
  regression (parity holds today) but worth flagging to the run owner: nothing currently prevents
  a future journey PR from letting a per-journey catalogue drift out of parity undetected.
- **Protected old-stack paths since `5b806ac`**: exactly one commit touches any of
  `src/ templates/ assets/ composer.* compose.yaml Dockerfile frankenphp/`, and it is exactly the
  documented `compose.yaml` restart-policy fix (R17) — no other line in any protected path
  changed.

## Per-journey verdicts (13/13 — architecture is a codebase-wide gate; no journey-specific
regression found)

| Journey | Verdict | Note |
| --- | --- | --- |
| login | parity | no rule scoped to this journey specifically beyond the global layering/typing/formatting gates, all green |
| landing | parity | same |
| feeds | parity | same |
| scheduler-heartbeat | parity | same |
| event-stream | parity | the two wave-2 rows (Mercure topic server-side, row markup in one place) are directly tied to this journey — both enforced and clean, both probed |
| customers | parity | same as login |
| automations | parity | same as login |
| settings | parity | same as login |
| campaigns-email-editor | parity | same as login |
| popups-widget-editor | parity | same as login |
| import-wizard | parity | `RecentImports.vue`'s governed `event-row` exemption (repair-3 history) checked; no bypass present |
| dashboard | parity | `ListCard.vue`'s governed `event-row` exemption checked; no bypass present |
| shell-navigation | parity | round-2 repair (test-only, see above) re-checked; no architecture-relevant file touched |

No journey has an architecture regression and none needed an accepted-deviation id (DEV-10 covers
the one out-of-scope row, "Storybook dev-only", which is not tied to any journey).

## Commands run (this checkout, cwd as shown)

| Command | cwd | Exit |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` | n/a | n/a |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` (`eslint .`) | `frontend/` | 0 (12 pre-existing style warnings, 0 errors) |
| `npm run typecheck` (`vue-tsc --noEmit`) | `frontend/` | 0 |
| `npx prettier --check .` | `frontend/` | 0 |
| `./mvnw -q -o test -Dtest=ArchitectureTest` | `backend/` | 0 (5/5 tests) |
| `./mvnw -q -o test -Dtest=FailOnWarnLogExtensionTest` | `backend/` | 0 (7/7 tests) |
| `./mvnw -q -o spotless:check` | `backend/` | 0 |
| `./mvnw -q -o compile` | `backend/` | 0 |
| plus one command per negative probe in `architecture/negative-probes.md` (each documented with
  its own exit code inline) | `backend/` or `frontend/` as noted | see that file |

## Evidence paths

- `waves/final/architecture/backend-archunit-test.log`, `backend-archunit-surefire.txt` —
  ArchitectureTest final clean run (5/5).
- `waves/final/architecture/backend-spotless-check.log` — Spotless clean run.
- `waves/final/architecture/frontend-eslint.log`, `frontend-typecheck.log`,
  `frontend-format-check.log` — ESLint / vue-tsc / Prettier clean runs.
- `waves/final/architecture/negative-probes.md` — every rules-translated.md row's negative-fires
  probe, command, and result.
- `waves/final/architecture/global-constraints.md` — full detail and command trail for every
  global implementation constraint listed above.

## Disk

`backend/target` and `frontend/node_modules` deleted from this checkout immediately after the
runs above completed (see final line of this session).
