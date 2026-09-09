# Wave-3 verifier — dimension: unit (round 3)

**Dimension status: PASS**, bound to wave SHA `60296f16250c6ebd25f7b4435c795268772fb6be`
(checkout `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit`, detached HEAD confirmed at this SHA
before running anything).

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | B26 fully named and green (backend: AutomationsViewServiceTest, AutomationsControllerTest, AutomationsFixturesTest; frontend: automationsComponents.spec.ts) |
| settings | parity | B06, B32 fully named and green (backend: SettingsFixturesTest, SettingsControllerTest, SettingsViewServiceTest; frontend: settingsComponents.spec.ts, highlight.spec.ts, localeHref.spec.ts) |
| campaigns-email-editor | parity | B27, B28 fully named and green (backend: CampaignsViewServiceTest, CampaignsControllerTest; frontend: campaignsComponents.spec.ts, emailDocument.spec.ts) |
| login, landing, feeds, event-stream, customers, scheduler-heartbeat (regression guards) | parity | All earlier-wave *Test.java/unit specs still green in the same full-suite run; no red test, no missing behaviour test |
| shell changes this wave (scrollRestoration, generic locale toggle) | parity | frontend/test/unit/scrollRestoration.spec.ts (6 describe blocks) and localeHref.spec.ts green, part of the same passing run |

No deviation from `deviations.json` changes a unit-dimension outcome this wave (DEV-4 is contract-only,
DEV-12 is visual-only — settings' B06 404 unit tests assert plain behaviour, untouched by DEV-12's
visual skip-on-404 rule). No `accepted-deviation` or `regression` verdicts required.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `git rev-parse HEAD` (sanity check) | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit` | 0 (`60296f16250c6ebd25f7b4435c795268772fb6be`, matches packet) |
| 2 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q -Dmaven.repo.local=/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/.m2-repo test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/backend` | 0 |
| 3 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |
| 4 | `npm run test -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |

(`-Dmaven.repo.local` scoped to the checkout to avoid touching any shared `~/.m2` write path, per the
plan's worktree-isolation rule; read cache still resolved through the default local/remote repos.)

## Results

- Backend (Surefire `*Test.java` only, via `./mvnw -q test`): **225 tests, 0 failures, 0 errors, 0 skipped**
  across 38 test classes, including `ArchitectureTest` (5/5), `AutomationsViewServiceTest` (9/9),
  `CampaignsViewServiceTest` (7/7), `SettingsViewServiceTest` (4/4), `SettingsFixturesTest` (8/8),
  `AutomationsFixturesTest` (7/7), `AutomationsControllerTest` (7/7), `CampaignsControllerTest` (7/7),
  `SettingsControllerTest` (6/6), `RouteTableTest` (3/3), `SpaDocumentControllerTest` (23/23). No
  `*IT.java` (Failsafe integration test) class appears in the surefire-reports output — the Surefire
  default exclusion held, confirming this run stayed in-scope for the unit dimension only.
  Full per-class breakdown: `unit/backend-surefire-summary.txt`.
- Frontend (`npm run test -- --run` → `vitest run test/unit --run`): **22 test files, 128 tests, all
  passed**, including `automationsComponents.spec.ts`, `campaignsComponents.spec.ts`, `emailDocument.spec.ts`,
  `settingsComponents.spec.ts`, `highlight.spec.ts`, `localeHref.spec.ts`, `scrollRestoration.spec.ts`,
  `routes.spec.ts`.
- Behaviour-to-test mapping for the wave's in-scope behaviours (B01 automations/settings/
  campaigns-email-editor rows, B06, B26, B27, B28, B32): every one resolved to a named
  `@DisplayName("Bnn …")` (backend) or `describe("Bnn …")` (frontend) test, and every one of those
  tests is inside the green runs above. Full mapping: `unit/behaviour-mapping.txt`.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/unit/backend-mvnw-test.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/unit/backend-surefire-summary.txt`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/unit/frontend-npm-ci.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/unit/frontend-npm-test.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-3/unit/behaviour-mapping.txt`
