# Repair packet w0-login / repair-2 (wave-0 verifier cohort FAIL: unit, integration, architecture)

Base: your candidate cfe7b48bedd871c2132b2d62cd215d135e5e3627 on branch migration/wave-0/login in /home/muszkin/work/kivvi-click-wt/w0-login (identity guard first). New candidate commit on top; never amend. HIGH reasoning effort.

Contract PASS, visual PASS (0.000 % on steps 1–3 without masks), e2e PASS — do not touch markup, CSS, routes, compose or the edge. Three findings:

## R2-A (unit FAIL) — `./mvnw -q test` must pass from a clean checkout
27 errors: `LoginControllerTest` and `SpaDocumentControllerTest` import `IndexHtmlTemplate`, which reads `classpath:static/index.html` that only exists after the frontend build is copied in. Required: the plan's verifier command `cd backend && ./mvnw -q test` (and `verify`) passes on a fresh clone with no frontend build. Preferred fix: a test-scoped SPA document fixture at `backend/src/test/resources/static/index.html` (minimal, realistic `<html lang="pl">…` shell with the same injection anchor the production file has) — production keeps failing fast at startup when the real file is missing. Alternative if you find the fixture approach unsound: bind the frontend build+copy into the Maven lifecycle (frontend-maven-plugin pinned from Maven Central, versions recorded). Document the choice in backend/README.md "Test" section. Prove: `git clean -xdf backend/src/main/resources/static frontend/dist` (only those build outputs) then `./mvnw -q test` and `./mvnw -q verify` green; keep the logs.

## R2-B (integration FAIL) — missing integration-level tests
- B04: `GET /api/v1/en/shell` returns English navigation labels (`Event stream`, `Customers`, …) and `locale: "en"`; `GET /api/v1/pl/shell` Polish — through the real HTTP layer (MockMvc/`WebTestClient`/`TestRestTemplate` with the Spring context; name it `@DisplayName("B04 …")`).
- B07: `GET /de/dashboard` → 404 through the real HTTP layer (`@DisplayName("B07 …")`), and `GET /pl/nonexistent` → 404.
- `ShellController` needs direct coverage: the full payload shape for the dashboard route (navGroups labels/hrefs/badge, currentSection, crumb, workspace, user default identity) asserted against the oracle values (journeys/login/steps/8 texts/a11y are the source).
- `SessionRoundTripIT`'s B08/B10 test overclaims: add the sidebar round-trip (`POST /preferences/sidebar {state:collapsed}` then `GET /api/v1/pl/shell` → `sidebar: "collapsed"`, and the SPA document carries `data-sidebar="collapsed"`) or split the names so each behaviour id is honest.

## R2-C (architecture FAIL) — "test policy fail-on-warning" rule has no tooling
`context/migration-oracle/symfony-to-spring-vue/architecture/rules-translated.md` row 3 (wave-0). Implement the analogue of PHPUnit's `failOnWarning`/`failOnNotice` restricted to first-party code (PHPUnit restricted it to `src`):
- Backend: a JUnit 6 extension (registered globally via `src/test/resources/junit-platform.properties` `junit.jupiter.extensions.autodetection.enabled=true` + `META-INF/services/org.junit.jupiter.api.extension.Extension`, or a base class if autodetection proves unreliable — record which) that attaches a Logback `ListAppender` to the `click.kivvi` logger for the duration of each test and fails the test if any event ≥ WARN was logged by `click.kivvi.*` loggers. Also fail on `System.err` writes from first-party code if cheap. Prove it works with a deliberate `LOG.warn` in a throwaway test (kept as a real test asserting the extension reports the failure, e.g. by testing the extension's core in isolation).
- Frontend: Vitest `setupFiles` that turns `console.warn`/`console.error` from `src/**` into test failures and fails on unhandled errors/rejections (`vitest` `dangerouslyIgnoreUnhandledErrors` must stay false); Vue warnings (`app.config.warnHandler` in tests) count as failures.
- Update `rules-translated.md`? NO — the oracle is immutable. Instead add a short note in `backend/README.md`/`frontend/README.md` naming the enforcing tooling.

## Gate chain on the NEW candidate SHA (in order; logs into evidence/repair-2-gates/)
`./mvnw -q test` (clean, no static/) → `./mvnw -q verify` → `npm run test -- --run` → `npm run test:integration -- --run` → ArchUnit + `spotless:check` → `npm run lint && npm run typecheck && npm run build` → compose stack (`kivvi-w-login`, 19000/19001) → `compare.mjs --journey login` (0 regressions) → Playwright login and navigation prefs specs as SEPARATE invocations (Playwright keeps only the last `-g`: `npx playwright test public.spec.ts -g "login"` and `npx playwright test navigation.spec.ts -g "sidebar collapse|theme toggle"`) → `performance.mjs`. Tear down (`down -v`), remove the image.

Report: append "## Repair-2" to worker-report.md (what changed per R2-A/B/C, proof logs, gate table, new candidate SHA, clean `git status`) and return it.
