# Wave-1 verifier: architecture

Wave SHA: `9427fdb1d92e4e606e67471638031d1bc0fb73d4` (checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-1-architecture`, HEAD confirmed at this SHA, working tree clean).

**Dimension status: PASS**, bound to `9427fdb1d92e4e606e67471638031d1bc0fb73d4`.

## Per-journey verdicts

| Journey | Verdict | Deviation |
| --- | --- | --- |
| landing | parity | — |
| feeds | parity | — |
| scheduler-heartbeat | parity | — |

No journey needed an accepted deviation on this dimension; no regression found.

## Rule → tooling table (`rules-translated.md`, wave-0 and wave-1 rows)

| Rule | Tooling | Result |
| --- | --- | --- |
| Static typing gate | `-Xlint:all -Werror` (maven-compiler-plugin, `pom.xml:166-173`); `vue-tsc --noEmit` with `strict: true` + `noUncheckedIndexedAccess` etc. (`tsconfig.json`) | 0 warnings/errors, both ran clean |
| Formatting `@Symfony` | Spotless (google-java-format 1.36.1) bound to `verify`; Prettier `format:check` | both clean |
| Test policy fail-on-warning | `FailOnWarnLogExtension` auto-registered via `junit-platform.properties` (backend); Vitest `dangerouslyIgnoreUnhandledErrors: false` + `test/setup.ts` console.warn/error capture restricted to first-party stack frames (frontend) | present and exercised by the suite (0 failures) |
| Layering convention | `ArchitectureTest` (ArchUnit 1.5.0): domain↛web, infrastructure only from infrastructure/application, no package cycle | 3/3 rules pass, 0 violations |
| Formatting decided server-side (wave-1) | ESLint `no-restricted-imports` (`*Intl*` pattern) + grep audit of `frontend/src` | see finding below — substance compliant, tooling has a documented gap |

Rows "Mercure topic built only server-side" and "Row markup in one place" are wave-2 — out of scope. "Storybook dev-only" is marked "Out of scope for parity" — excluded per its own row.

## Finding: wave-1 formatting rule — tooling gap, not a violation

`grep -rn "Intl\.|toLocaleString|NumberFormat|toFixed(" frontend/src/` returns zero matches. All number/percent/money/relative-time formatting for landing and feeds lives server-side in `backend/src/main/java/click/kivvi/domain/Format.java` and `application/FeedsViewService.java`; `FeedCard.vue` receives pre-formatted strings (`products`, `mapped`, `mappedPercent`) and says so in its own header comment. **No violation of the underlying rule exists in this wave.**

However, `frontend/src/format.ts` — the file `rules-translated.md` names as the sanctioned single importer — does not exist, and the ESLint `no-restricted-imports` rule (pattern `*Intl*`) only matches `ImportDeclaration` sources. Probed in an isolated copy outside the checkout (`/tmp/.../scratchpad/probe-eslint/frontend`, not committed): a literal `import ... from "some-Intl-polyfill"` is flagged; a direct `new Intl.NumberFormat(...)` call as the global (the actual violation shape, since `Intl` is a JS global and nobody imports it) is **not** flagged. There is no `no-restricted-globals` rule covering `Intl`. This means the introduced tooling cannot catch the real violation pattern it names.

Not scored as a FAIL for wave-1: the packet's own check method for this row is the grep above (zero hits), the actual behaviour is compliant, and no deviation is needed since nothing deviates. Flagged as a risk for wave-2+: add a `no-restricted-globals` rule for `Intl` and create `src/format.ts` when the SPA first needs inline formatting.

## New-code layering check

- `infrastructure/scheduling/{HeartbeatJob,HeartbeatTrigger,HeartbeatLockHistory,JdbcHeartbeatLockHistory}.java` + `infrastructure/config/SchedulingConfig.java` — all scheduling code confined to `infrastructure`.
- Landing/feeds: `fixtures/{LandingFixtures,FeedsFixtures}.java`, `application/{LandingView,LandingViewService,FeedsViewService}.java`, `web/{LandingController,FeedsController,dto/FeedsResponse}.java` — confined to fixtures/application/web/domain, no reverse dependency (`grep -rl infrastructure backend/src/main/java/click/kivvi/web/` → empty).
- Confirmed globally by `ArchitectureTest` (0 failures): domain↛web, infrastructure only reachable from infrastructure/application, no cycles.

## ShedLock dependency

`shedlock-spring` / `shedlock-provider-jdbc-template` pinned `7.10.0` in `backend/pom.xml` — matches the plan's Technology decisions table row exactly. License: Apache-2.0, confirmed both in the pom.xml comment and in the resolved artifact's own POM (`~/.m2/.../shedlock-parent/7.10.0/*.pom` → "The Apache Software License, Version 2.0"). No plan constraint conflicts (only Mercure's AGPL-3.0 required an explicit note; Apache-2.0 needs none).

## Global constraints

- `git diff --name-only 8d3fc320354604b641b44a3043a070f279e6d491 -- src templates assets translations migrations tests/e2e/specs composer.json composer.lock compose.yaml compose.override.yaml compose.prod.yaml Dockerfile frankenphp config public` → empty. `'tests/**/*.php'` also empty. Overall diff vs base: 166 files changed, all additive (`backend/`, `frontend/`, `mercure/`, `tools/migration-verify/`, `compose.next*.yaml`).
- Flyway: only `backend/src/main/resources/db/migration/V1__baseline.sql` exists.
- CSS byte-identity (`cmp`): `01-tokens.css`, `02-base.css`, `03-components.css`, `04-patterns.css`, `app.css` — all 5 identical between `assets/styles/` and `frontend/src/styles/`.
- Pinned versions vs plan's Technology decisions table: Spring Boot 4.1.1, Java 25 (Temurin 25.0.4.1+1), ArchUnit 1.5.0, JUnit 6.0.3, Testcontainers 2.0.5 (all via `mvn dependency:tree -o`), Maven wrapper 3.9.11, Vue 3.5.42, vue-router 5.3.1, Pinia 4.0.3, Vite 8.2.2, @vitejs/plugin-vue 6.0.8, Vitest 5.0.0, @vue/test-utils 2.5.0, vue-tsc 3.3.11, vue-i18n 11.4.10, TypeScript 6.0.3, Prettier 3.8.4, Node 26.8.1 (host), Mercure `dunglas/mercure:v0.24.2` — every value matches exactly, no drift.

## Commands run

| Command | cwd | Exit |
| --- | --- | --- |
| `export JAVA_HOME=.../jdk-25; ./mvnw -q test -Dtest=ArchitectureTest` | `backend/` | 0 |
| `./mvnw verify -o` (spotless bound to `verify`) | `backend/` | 0 (BUILD SUCCESS; `Tests run: 15, Failures: 0`; spotless "keeping 63 files clean") |
| `npm ci` | `frontend/` | 0 |
| `npm run lint` (ESLint) | `frontend/` | 0 (0 errors, 2 pre-existing style warnings in `FeedCard.vue`) |
| `npm run typecheck` (`vue-tsc --noEmit`) | `frontend/` | 0 |
| `npm run format:check` (`prettier --check .`) | `frontend/` | 0 |
| `mvn dependency:tree -o` | `backend/` | 0 (version audit) |
| `git diff --name-only <base> -- <protected paths>` | checkout root | 0 (empty) |
| `cmp assets/styles/*.css frontend/src/styles/*.css` (×5) | checkout root | 0 (all identical) |
| ESLint probe (`Intl.NumberFormat` global vs. import) | isolated `/tmp` copy, not the checkout | see finding above |

## Evidence paths

- `waves/wave-1/architecture/archunit-test.log`
- `waves/wave-1/architecture/mvnw-verify.log`
- `waves/wave-1/architecture/frontend-checks.log`
- `waves/wave-1/architecture/global-constraints.log`
- `waves/wave-1/architecture/pinned-versions.log`
- `waves/wave-1/architecture/wave1-formatting-rule.log`
- `waves/wave-1/architecture/layering.log`
