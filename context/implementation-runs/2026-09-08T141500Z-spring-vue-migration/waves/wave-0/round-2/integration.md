# Wave-0 verifier — dimension: integration (round 2)

- Wave SHA: `6a53642c9c5ec9c256625854e6670f035e0292be`
- Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int` (HEAD confirmed == wave SHA, clean tree)
- Journeys in scope: `login`
- Oracle manifest sha256 confirmed: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (matches packet, matches oracle copy inside the checkout)

## Dimension status: **FAIL**

## Journey verdict

| Journey | Verdict | Reason |
| --- | --- | --- |
| login | **regression** (coverage gap, not a red test) | B12 (empty e-mail) and B13 (malformed e-mail) have no integration-level test — only `@WebMvcTest`-sliced (unit-dimension) and, for B12, a frontend test that fakes server output instead of exercising `POST /pl/login`. Every test that does exist is green and Testcontainers ran correctly. |

## Commands run

| Command | cwd | Exit code |
| --- | --- | --- |
| `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/backend` | 0 |
| `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/frontend` | 0 |
| `npm run test:integration -- --run` | `/home/muszkin/work/kivvi-click-wt/verify-wave-0-int/frontend` | 0 |

## Backend — Testcontainers confirmation

`backend-mvnw-verify.log` shows two independent `postgres:18-alpine` Testcontainers containers
started (one per `@SpringBootTest` class, `ServiceConnection`-wired), plus the shared `ryuk`
reaper — Docker socket `unix:///var/run/docker.sock`, Testcontainers 2.0.5:

- `click.kivvi.SessionRoundTripIT` — container `f10ecfd8edfd…`, started in 1.36s, JDBC URL `jdbc:postgresql://localhost:33025/test`
- `click.kivvi.ShellApiIT` — container `28910e9457ab…`, started in 1.85s, JDBC URL `jdbc:postgresql://localhost:33026/test`

Failsafe per-class summaries (`target/failsafe-reports/*.txt`, copied to this evidence dir):

- `ShellApiIT`: Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
- `SessionRoundTripIT`: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

No `*IT.java` test was skipped. `./mvnw -q verify` (which runs `test` + Failsafe `integration-test`
+ `verify` phases) exited 0 — unit tests (Surefire, out of scope for this dimension) also passed as
a side effect of `verify`, not separately asserted here.

## Frontend — integration suite

`npm run test:integration -- --run` (Vitest): **3 files, 13 tests, all passed** — `test/integration/LoginView.spec.ts`, `test/integration/shellStore.spec.ts`, `test/integration/Sidebar.spec.ts`.

## Behaviour → test mapping

See `integration/behaviour-mapping.md` for the full B04/B07/B08/B10/B11–B14 → named-test table,
per-behaviour assertion concreteness, and the overclaim check. Summary:

- B04, B07: covered — `ShellApiIT` (real HTTP, real Spring context, Testcontainers Postgres), full oracle nav-label lists and 404 status asserted.
- B08, B10: covered — `SessionRoundTripIT`, real HTTP two-request round trip through the JDBC-backed session store, concrete `theme`/`sidebar` values and `data-sidebar` document attribute asserted.
- B11, B14: covered — `SessionRoundTripIT.signInThenSignOutRoundTripsTheIdentity`, real HTTP sign-in (302 to `/pl/dashboard`, shell shows signed-in e-mail) then sign-out (shell reverts to default identity). One sibling test's `@DisplayName` also cites B14 without exercising logout — noted as a minor overclaim, not fatal since the behaviour is genuinely covered elsewhere.
- **B12, B13: not covered at the integration level.** Only unit-dimension `@WebMvcTest` coverage (`LoginControllerTest`) and, for B12 only, a frontend component test that replays a pre-set `data-login-error` attribute rather than obtaining it from a real `POST /pl/login` response.

## Evidence paths

- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/backend-mvnw-verify.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/backend-failsafe-ShellApiIT.txt`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/backend-failsafe-SessionRoundTripIT.txt`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/frontend-npm-ci.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/frontend-npm-test-integration.log`
- `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/behaviour-mapping.md`

## Source evidence (checkout, read-only)

- `backend/src/test/java/click/kivvi/ShellApiIT.java` (B04, B07)
- `backend/src/test/java/click/kivvi/SessionRoundTripIT.java` (B08, B10, B11, B14)
- `backend/src/test/java/click/kivvi/web/LoginControllerTest.java` (B11–B14, `@WebMvcTest` slice — unit dimension only)
- `frontend/test/integration/LoginView.spec.ts` (B12 tag; B13 behaviour rendered but untagged)
- `context/migration-oracle/symfony-to-spring-vue/behaviours.json` (B04, B07, B08, B10, B11–B14 definitions)
