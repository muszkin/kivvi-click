# Verifier report — wave wave-2, dimension architecture

Wave SHA: `22d7fcb6380723728a33fc21fda22a92594a2e88` (checkout HEAD confirmed detached at this SHA, clean working tree).
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture`.

## Dimension status: **FAIL** (bound to `22d7fcb6380723728a33fc21fda22a92594a2e88`)

Two wave-2 rows of `architecture/rules-translated.md` have **no enforcing tooling** in this
checkout and **no matching accepted deviation** in the plan's "Accepted deviations" table
(DEV-1..DEV-13 reviewed; none covers the architecture dimension for these rules — DEV-3 covers
only the *contract*-dimension payload-shape difference, not the ArchUnit/ESLint tooling itself).
Per the packet instruction, absence of tooling + absence of deviation = FAIL.

## Rule → tooling table (wave-0/1/2 rows of rules-translated.md)

| Rule | Wave | Required tooling | Found in checkout | Verdict |
| --- | --- | --- | --- | --- |
| Layering (`web`→`domain`, `infrastructure`, no cycles) | wave-0 | ArchUnit `@AnalyzeClasses` | `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` — 3 tests present, all pass | present |
| Static typing gate (Error Prone/`-Xlint:all`; `vue-tsc --noEmit` strict) | wave-0 | Maven compiler config; vue-tsc | `vue-tsc --noEmit` wired as `npm run typecheck`, exit 0 | present |
| Formatting (Spotless/google-java-format; Prettier) | wave-0 | spotless-maven-plugin; prettier | `pom.xml` has `spotless-maven-plugin` 3.10.2 bound into `verify` (ran clean); `npm run format:check` exit 0 | present |
| Test policy fail-on-warning | wave-0 | JUnit 6 / Vitest 5 | out of scope for this packet (unit/integration dimensions) | not verified here |
| Formatting decided server-side (`format.ts` sole `Intl` importer) | wave-1 | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` | present in `frontend/eslint.config.js:24-67`, scoped away only for `src/format.ts` | present |
| **Mercure topic built only server-side** | **wave-2** | ArchUnit: only `..tracking.stream..` may reference `MercurePublisher`; ESLint `no-restricted-syntax` on `/accounts/` literals | **absent** — `ArchitectureTest.java` has no such rule; no `..tracking.stream..` package even exists (`MercurePublisher` is used from `application.tracking.EventIngestionService`, one level up); `eslint.config.js` has no `/accounts/` restriction | **FAIL — no tooling, no deviation** |
| **Row markup in one place** | **wave-2** | ESLint `vue/no-restricted-class` for `event-row` outside `EventRow.vue` | **absent** — no such rule in `eslint.config.js`; only the pre-existing `no-restricted-imports`/`globals`/`syntax` (Intl) rules exist | **FAIL — no tooling, no deviation** |
| Storybook dev-only bundle exclusion | out of scope | Vite config | not evaluated (explicitly out of scope) | n/a |

Negative-probe evidence (since static reading could be disputed): copied `frontend/` to a scratch
directory (`eslint-probe/`, outside any tracked checkout) at this same SHA and added two files
exercising exactly the two missing rules — a module composing `` `/accounts/${id}/events` `` outside
any tracking/stream module, and a second Vue component rendering `class="event-row"`. Ran
`npx eslint <probe files>` with the real `eslint.config.js`: **exit 0, zero diagnostics** — neither
violation is caught. See `architecture/eslint-negative-probe/`.

Positive-side facts consistent with product intent (not a substitute for the missing rule):
`grep -rn accounts frontend/src` finds no topic-string composition anywhere in the SPA today, and
`.event-row` is in fact only rendered by `EventRow.vue` today (CSS selectors in
`03-components.css`/`04-patterns.css` are stylesheet rules, not renders). The code is currently
compliant; the migration plan's own promise of an *enforced* rule (rules-translated.md, "Tooling
(introduced)" column) is not delivered.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| event-stream | **regression** | Consumes `MercurePublisher`/`.event-row` — the two unenforced wave-2 rules are journey-scoped to this journey. No deviation ID applies. |
| customers | **regression** | No journey-specific architecture rule of its own in `rules-translated.md`, but the dimension is FAIL at the wave level (contract requires "every row of `rules-translated.md` for the wave has a rule"), and customers shares the wave; no deviation ID applies. |

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; ./mvnw -q test -Dtest=ArchitectureTest` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/backend` | 0 (3/3 pass — but only wave-0 rules exist to run) |
| `./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/frontend` | 0 |
| `npm run lint` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/frontend` | 0 (2 pre-existing formatting warnings, 0 errors — unrelated to the missing wave-2 rules) |
| `npm run typecheck` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/frontend` | 0 |
| `npm run format:check` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture/frontend` | 0 |
| `npx eslint src/components/molecules/__ProbeTopicBuilder.ts src/components/molecules/__ProbeSecondEventRow.vue` (negative probe) | scratch copy `eslint-probe/` | 0, no diagnostics (expected: should have flagged both files) |
| `git diff --quiet 8d3fc320354604b641b44a3043a070f279e6d491 -- src/ templates/ assets/ translations/ migrations/ 'tests/**/*.php' tests/e2e/specs/ composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp/ config/ public/` | checkout root | 0 (empty diff — global constraint holds) |
| `git diff 4f74907 -- backend/pom.xml` | checkout root | 0 output (no dependency drift — global constraint holds) |

Green commands above do **not** overturn the FAIL: they confirm the *code that exists* is clean,
not that the wave-2 architecture rules are enforced (the packet's rule is explicit: a rule with no
tooling and no deviation is itself a FAIL, independent of whether today's code happens to comply).

## Global constraints checked

- Diff vs base `8d3fc320354604b641b44a3043a070f279e6d491` under the frozen-path list: **empty** (confirmed via `git diff --quiet`, no output from `--stat`).
- Flyway migrations: only `backend/src/main/resources/db/migration/V1__baseline.sql` exists (no `V2` for the `event_dedup` table) — satisfies "the dedup table must NOT have required a new migration".
- CSS parity: `assets/styles/{01-tokens,02-base,03-components,04-patterns,app}.css` are byte-identical (`cmp`) to the same five files under `frontend/src/styles/`; `storybook.css` correctly has no SPA counterpart (out of scope, DEV-10).
- Pinned versions vs plan: Spring Boot 4.1.1, ArchUnit 1.5.0, Spotless 3.10.2, Vue 3.5.42, Vite 8.2.2, vue-tsc 3.3.11, Vitest 5.0.0, eslint-plugin-vue 10.11.0, typescript-eslint 8.70.0 — all match the plan's "Version pinned" table (lines ~201-219).
- Backend dependencies vs `4f74907`: `git diff 4f74907 -- backend/pom.xml` is empty — no new dependency introduced.

## Evidence paths

- `architecture/archunit-test.log`
- `architecture/mvnw-verify.log`
- `architecture/npm-ci.log`
- `architecture/npm-lint.log`
- `architecture/npm-typecheck.log`
- `architecture/npm-format-check.log`
- `architecture/eslint-negative-probe/__ProbeTopicBuilder.ts`
- `architecture/eslint-negative-probe/__ProbeSecondEventRow.vue`
- `architecture/eslint-negative-probe/README.txt`
- Source inspected: `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`, `frontend/eslint.config.js`, `backend/src/main/java/click/kivvi/application/tracking/EventIngestionService.java`, `backend/src/main/java/click/kivvi/infrastructure/mercure/*.java`, `frontend/src/components/molecules/EventRow.vue` (all in `/home/muszkin/work/kivvi-click-wt/verify-wave-2-architecture`).

## Remediation needed for PASS

Add to `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` (or a sibling test)
an ArchUnit rule restricting `MercurePublisher` access to a defined tracking/stream slice, and add
two ESLint rules to `frontend/eslint.config.js`: a `no-restricted-syntax` rule catching `/accounts/`
template-literal/string topic construction, and a `vue/no-restricted-class` (or equivalent
`no-restricted-syntax` on `class` attributes) rule confining the literal `event-row` class to
`EventRow.vue`. Alternatively, record both as accepted deviations with a reason, owner and expiry
in the plan's "Accepted deviations" table.
