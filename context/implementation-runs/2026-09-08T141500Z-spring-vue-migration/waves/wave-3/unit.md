# Wave-3 verification — dimension: unit (round 4)

**Wave SHA:** `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
**Journeys:** automations, settings, campaigns-email-editor
**Checkout:** `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit` (detached HEAD at the wave SHA, clean tree at inspection time)
**Oracle:** `/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue`, manifest sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` — verified matching (`sha256sum manifest.json`).

## Dimension status: **PASS**

Bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |
| `npm run test -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-3-unit/frontend` | 0 |

Java: Temurin 25.0.4.1 LTS (`java -version` confirmed before running mvnw). Node: v26.8.1 (host).

## Results

- Backend (Surefire, `*Test.java` only — confirmed no `*IT.java`/`*ApiIT.java` file appears among the 40 executed classes; those are Failsafe-bound, out of unit scope): **40 test classes, 236 tests, 0 failures, 0 errors, 0 skipped.**
- Frontend (`vitest run test/unit`, 22 spec files under `frontend/test/unit` only — `frontend/test/integration/*` is a separate npm script, out of unit scope): **22 test files, 128 tests, 0 failures, 0 skipped.**

## Session-lock repair (round-4) — infrastructure.session unit coverage

Confirmed both new classes have dedicated `*Test.java` unit tests, both plain-JUnit5 (no `@SpringBootTest`, no Testcontainers, no `@Tag`), so both run under the plain `test` phase, not `verify -Pintegration`:

- `backend/src/test/java/click/kivvi/infrastructure/session/SessionLockRegistryTest.java` — 6 tests, 0 failures (same-id serialization, different-id independence, eviction on release, tested against `SessionLockRegistry` in isolation with a `ReentrantLock` + `CountDownLatch`/`ExecutorService`).
- `backend/src/test/java/click/kivvi/infrastructure/session/SessionRequestSerializationFilterTest.java` — 5 tests, 0 failures (session-less pass-through, lock-timeout 503, lock release/eviction on success and on exception, tested against `SessionRequestSerializationFilter` with servlet mocks, no Spring context).

Evidence: `unit/backend-session-tests.txt`.

## Behaviour → named-test mapping (behaviours.json, wave-3 scope)

Every behaviour in scope has at least one named unit-scope test (`@DisplayName("Bnn …")` in a `backend/src/test/java/**/*Test.java`, and/or `describe("Bnn …")`/`it("Bnn …")` in `frontend/test/unit/**/*.spec.ts`), and every located test is green.

| Behaviour | Journey | Unit-scope tests found (file — count) | Verdict |
| --- | --- | --- | --- |
| B01 (automations/settings/campaigns/email-editor rows) | automations, settings, campaigns-email-editor | `backend/.../web/SpaDocumentControllerTest.java` (parameterized `everyKnownRouteRenders200`, includes `/pl/automations`, `/pl/automations/new`, `/pl/automations/a1`, `/pl/campaigns`, `/pl/emails/new`, `/pl/emails/k1`, `/pl/settings`); `backend/.../domain/RouteTableTest.java` (same URL set, route-table match); `frontend/test/unit/routes.spec.ts` (same URL set, router resolve) | named, green |
| B06 | settings | `backend/.../fixtures/SettingsFixturesTest.java`; `backend/.../web/SettingsControllerTest.java` (incl. `B06/DEV-12` 404-document test); `backend/.../application/SettingsViewServiceTest.java` | named, green |
| B26 | automations | `backend/.../web/AutomationsControllerTest.java` (6); `backend/.../application/AutomationsViewServiceTest.java` (8); `backend/.../fixtures/AutomationsFixturesTest.java` (6); `frontend/test/unit/automationsComponents.spec.ts` (`FlowCanvas`, `RulePipeline`, `AutoCard`) | named, green |
| B27 | campaigns-email-editor | `backend/.../web/CampaignsControllerTest.java` (2); `backend/.../application/CampaignsViewServiceTest.java` (4) | named, green |
| B28 | campaigns-email-editor | `backend/.../web/CampaignsControllerTest.java` (3, incl. `B28/DEV-7` 404-shape test); `backend/.../application/CampaignsViewServiceTest.java` (3); `frontend/test/unit/campaignsComponents.spec.ts` (`BlockLibrary`, `CouponCode`); `frontend/test/unit/emailDocument.spec.ts` (white-background-in-dark-theme) | named, green |
| B32 | settings | `backend/.../web/SettingsControllerTest.java` (3); `backend/.../fixtures/SettingsFixturesTest.java` (7); `backend/.../application/SettingsViewServiceTest.java` (3); `frontend/test/unit/settingsComponents.spec.ts` (`SettingsNav`, `ToggleRow`, `DnsRow`, `HookRow`, `CodeBlock`); `frontend/test/unit/localeHref.spec.ts`; `frontend/test/unit/highlight.spec.ts` | named, green |

Full grep-based mapping evidence: `unit/behaviour-test-mapping.txt`.

No behaviour in scope was found without a named unit test, and no located test was red.

## Wave-3 shell-change regression guard (unit dimension)

- `frontend/test/unit/scrollRestoration.spec.ts` (20 `it` cases) — covers `frontend/src/router/scrollRestoration.ts` (`scrollBehavior`, `saveScrollPositionForReload`/`consumeScrollPositionForReload`, `isReloadOfAnAlreadyVisitedEntry`, `waitForStableLayout`). Green, part of the 128 passing frontend tests.
- `frontend/test/unit/localeHref.spec.ts` (7 `it` cases) — covers `frontend/src/router/localeHref.ts` (generic `route.meta.defaultParams` locale-toggle href builder). Green, part of the 128 passing frontend tests.

(`LocaleToggle.spec.ts` and `ScrollRestoration.spec.ts` live under `frontend/test/integration/`, run by `npm run test:integration`, not `npm run test` — out of scope for the unit dimension; that is the integration verifier's evidence.)

## Deviations in scope (unit dimension relevance)

- **DEV-4** (contract dimension, full-parity HTTP rule) — not applicable to unit; no unit-command output references it directly.
- **DEV-7** (campaigns: no `POST …/blocks`, contract dimension) — reflected indirectly in `backend/.../web/CampaignsControllerTest.java`'s `"B28/DEV-7 GET /api/v1/pl/emails/{id} 404s for an id shape outside \"new\"/\"k\\d+\""` test (green); no unit-dimension action needed beyond that.
- **DEV-12** (settings step 10 = 404 document, visual dimension) — the underlying backend behaviour is unit-tested: `backend/.../web/SettingsControllerTest.java`'s `"B06/DEV-12 GET /pl/settings/nonexistent is a 404 document, not the 200 SPA shell"` (green) and `SettingsApiIT.java`'s equivalent (integration dimension, not run here). No unit-dimension deviation needed; parity holds outright.

## Per-journey verdicts

| Journey | Verdict | Notes |
| --- | --- | --- |
| automations | parity | B26 and its B01 row fully mapped to green unit tests, backend + frontend |
| settings | parity | B06, B32 and its B01 row fully mapped to green unit tests; DEV-12's backend behaviour is unit-tested and green (no deviation needed at this dimension) |
| campaigns-email-editor | parity | B27, B28 and its B01 row fully mapped to green unit tests; DEV-7's backend 404-shape behaviour is unit-tested and green |

## Evidence paths

- `unit/backend-mvnw-test.log` — full `./mvnw -q test` output
- `unit/backend-surefire-summary.txt` — per-class `Tests run` lines for all 40 executed classes
- `unit/backend-session-tests.txt` — Surefire report detail for `SessionLockRegistryTest` and `SessionRequestSerializationFilterTest`
- `unit/frontend-npm-test.log` — full `npm run test -- --run` output
- `unit/behaviour-test-mapping.txt` — grep evidence for the behaviour → named-test table above
