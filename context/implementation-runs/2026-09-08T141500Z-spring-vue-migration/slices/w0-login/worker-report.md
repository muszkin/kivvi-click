# Worker report — slice w0-login

## 1. Identity guard

Checked before starting, before every commit, and before every gate:

| Check | Expected | Actual |
| --- | --- | --- |
| `git rev-parse --show-toplevel` | `/home/muszkin/work/kivvi-click-wt/w0-login` | `/home/muszkin/work/kivvi-click-wt/w0-login` ✓ |
| `git rev-parse --abbrev-ref HEAD` | `migration/wave-0/login` | `migration/wave-0/login` ✓ |
| `git status --porcelain` | only intentional changes | clean at every check ✓ |
| Parent SHA | `8d3fc320354604b641b44a3043a070f279e6d491` | matched at start ✓ |
| Plan SHA-256 | `84b664562fd00c1de6a2263ee2b32fa48da93d88f989f4aac8d60b46fbd4c017` | verified via `sha256sum` before reading ✓ |

No mismatch at any point. All work stayed in the worktree; nothing was edited under `/home/muszkin/work/kivvi-click` except this report and `slices/w0-login/evidence/`.

## 2. Expected initial RED

Captured before implementing (with the compose stack not yet running — the packet's "before the scaffold exists" case):

- `evidence/red-compare-mjs.txt` — `compare.mjs --journey login` against `https://localhost:19001` fails: `docker compose ... exec database` errors with `service "database" is not running` (exit 1).
- `evidence/red-compare-mjs-visual.txt` — same command with `--dimension visual` (skips the db-count call) fails at the browser layer instead: `page.goto: net::ERR_CONNECTION_REFUSED` (exit 1).
- `evidence/red-playwright.txt` — the four gated Playwright tests (see §6) all fail with `ERR_CONNECTION_REFUSED` against `https://localhost:19001` (exit 1).

I did not additionally stand up the oracle stack on 18080/18443 to prove the harness green (packet §7 says "you may", not "must") — disk headroom on this host was tight (≈9 GB free before starting; `docker system df` showed 28 GB already used by other workers' images), and the RED evidence above already proves the harness fails for the right reason (connection refused / stack absent) rather than a harness bug, which is what that optional step is for. This trade-off is recorded here rather than silently skipped.

## 3. Implementation summary

### Backend (`backend/`) — Spring Boot 4.1.1, Java 25, package root `click.kivvi`

Layered `web` → `application` → `domain`, with `infrastructure` reachable only from `application` — enforced by `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java` (ArchUnit): domain never depends on web, infrastructure is only accessed from application, and the four top-level packages (plus `fixtures`) form no cycle.

- **`domain/`** — pure logic, no framework types except JDK ones: `SupportedLocale`, `Theme`, `SidebarState` (enums with fallback), `Identity` (name-derivation rule, mirrors `PanelIdentity::name()`), `EmailValidation` (mirrors `SecurityController::validate()`, same two literal Polish messages), `NavigationCatalog` (nav structure, mirrors `Navigation::GROUPS`/`INDEX_OF_DETAIL`), `RouteTable` (the 18 locale-prefixed SPA-document patterns + `/`, mirrors `inventory/routes.json` minus the excluded prefixes), `SpaDocument` (pure `<html>`-tag attribute injection with HTML-escaping).
- **`fixtures/`** — `ShellFixtures`: the default identity (`maciej@aureashop.pl` / `Maciej Kowalczyk`) and the workspace card (`aureashop.pl`, `Plan Pro · 3 strony`, `AS`) — sample data, mirrors `Workspace`/`PanelIdentity`'s hard-coded defaults, matches the packet's "fixtures package with the shell data only".
- **`infrastructure/`** — `SessionIdentityStore`, `SessionPreferencesStore` (both accept a **nullable** `HttpSession` for reads, so a GET never forces a session into existence — only `PreferencesController`'s writes and a successful sign-in create one); `NavigationLabels` (wraps a UTF-8 `MessageSource`); `IndexHtmlTemplate` (loads the built SPA's `index.html` once from the classpath, fails fast at startup if missing); `config/MessageSourceConfig` (explicit UTF-8 `ReloadableResourceBundleMessageSource` — Java's classic bundle loading defaults to ISO-8859-1 and would mangle every Polish diacritic).
- **`application/`** — `ShellViewService` (assembles the `GET /api/v1/{locale}/shell` view-model), `LoginService` (validate + sign in only on success, sign out only if a session exists), `PreferencesService`, `SpaDocumentService` (renders the SPA document with locale/theme/sidebar, plus login-error/last-username on a failed POST).
- **`web/`** — `ShellController`, `LoginController`, `PreferencesController`, `SpaDocumentController` (catch-all for `/` and `/{locale:pl|en}/**`, 404 for anything `RouteTable` doesn't recognise), `LandingController` (the `/demo` → dashboard redirect).

**Key decisions:**

- **Edge / Caddyfile approach:** `mercure/Caddyfile` makes the `dunglas/mercure:v0.24.2` image the stack's edge — same shape as `frankenphp/Caddyfile` (same log filter redacting the `authorization` query param, `anonymous` + `subscriptions` on the `mercure` directive), with `reverse_proxy api:8080` in place of FrankenPHP's embedded PHP worker. `/actuator/*` is explicitly blocked at the edge (`respond /actuator/* 404`) so it stays internal-only; the compose healthcheck curls the `api` container directly, not through the edge.
- **SPA document injection:** the built `index.html`'s `<html lang="pl">` tag is located with a regex and replaced with one carrying `lang`, `data-theme`, `data-sidebar`, and — on a failed login POST only — `data-login-error` / `data-last-username`. Discovered mid-implementation: Twig's `default()` filter (used in `last_username|default('maciej@aureashop.pl')`) falls back on an **empty string**, not just an undefined value — so an empty-e-mail submission redisplays the sample address, not a blank field. `LoginController` now resolves this the same way before injecting the attribute (see §9, oracle claim).
- **Session:** Spring Session JDBC, cookie `SESSION`, `HttpOnly`, `SameSite=Lax`. `Secure` is hard-coded true rather than left to `request.isSecure()` via `server.forward-headers-strategy=framework` — empirically, Spring Session's cookie serializer does not consult that strategy for this property (verified: with the header set, the cookie still came back without `Secure`); since the only entry point into this stack is the TLS-terminating Mercure edge, hard-coding it is simpler and correct for the actual topology. Reads never force a session into existence (see infrastructure notes above) — this made the login journey's own db.json delta come out **exactly 0/0**, matching the oracle, with no tolerance needed.
- **404 handling:** `RouteTable.match()` — a path either matches one of the 18 patterns (locale constrained to `pl|en` directly in the `@GetMapping`/regex, so an unsupported prefix never reaches a handler) or gets a minimal literal HTML document at 404. Genuinely unmatched paths (no locale prefix at all, e.g. `/xyz`) fall through to Spring Boot's own 404 handling.
- **i18n source:** `frontend/src/i18n/{pl,en}.ts`, ported key-for-key from `translations/messages.{pl,en}.yaml` and the inline Polish strings in the ported Twig templates. Two strings (`common.collapseSidebar`, `landing.*` a few) have no entry in `messages.en.yaml` in the old stack either — ported as staying Polish on `/en/…` too, matching the old stack's actual fallback behaviour, not a guess.

### Frontend (`frontend/`) — Vue 3.5.42, Vite 8.2.2, vue-router 5.3.1, Pinia 4.0.3, vue-i18n 11.4.10, TypeScript 6.0.3

- `src/styles/{01-tokens,02-base,03-components,04-patterns,app}.css` — copied byte-for-byte (`md5sum` verified identical to `assets/styles/*.css` after every rebuild).
- `src/components/{atoms,molecules,organisms}` — the ported design system; `src/layouts/{PublicLayout,AuthLayout,AppLayout}.vue` mirror `templates/layout/*.twig`; `src/views/{LoginView,DashboardView,EmptyPageView}.vue` — only login and the dashboard page-head have real bodies (DEV-11); every other route mounts `AppLayout`/`PublicLayout` around an empty page.
- `src/router/routes.ts` — the full route table. Navigation is always a real document request (`useIntents`' `navigate` intent, plain `<a>` hrefs — confirmed against the oracle's own `shell-navigation` `http.jsonl`: every sidebar click and the locale-switch link records a `kind:"document"` entry, never client-side-only routing), never `router.push`.
- `src/stores/shell.ts` — Pinia store backing the shell chrome, `theme`/`sidebar` seeded from the server-injected `<html>` attributes before first paint, the rest fetched from `GET /api/v1/{locale}/shell`.
- **Discovered mid-implementation (Vue whitespace quirk):** Vue's default `whitespace: "condense"` compiler mode *removes* (not collapses) a whitespace-only text node between two inline elements when that whitespace spans a newline — `WorkspaceCard.vue`'s `<span class="ws-name">…</span>\n<span class="ws-meta">…</span>` rendered as `"aureashop.plPlan Pro…"` with the space gone entirely, caught by `compare.mjs`'s first real run against the live stack. Fixed with an explicit `{{ " " }}` text-node interpolation (survives Prettier reformatting, unlike a same-line literal space). Every other adjacent-inline-text pair in this slice sits inside a `display:flex` parent (`.btn`, `.crumbs`, `.kbar`, `.row`, `.nav-item`), where flex/grid formatting contexts get their own `innerText` line breaks regardless of source whitespace — `.ws-label` was the one non-flex exception. Locked in with `frontend/test/unit/WorkspaceCard.spec.ts`.
- No `compilerOptions.whitespace: "preserve"` override was needed anywhere: the oracle's text/a11y comparisons are computed from `innerText()`/`ariaSnapshot()`, which already collapse insignificant whitespace themselves — verified empirically against the full login+shell markup (0 regressions once the one real bug above was fixed).

### Compose / edge

- `compose.next.yaml` — `api` (built from `backend/Dockerfile`, three stages: `node:26-alpine` builds the SPA, `maven:3.9-eclipse-temurin-25` packages the jar with the SPA copied into `src/main/resources/static`, `eclipse-temurin:25-jre` runs it as a non-root user with `curl` installed for the healthcheck), `mercure` (the edge, depends on `api` being healthy), `database` (`postgres:18-alpine`, volume at `/var/lib/postgresql`, random published port).
- `compose.next.prod.yaml` — production overlay: `SERVER_NAME=:80` (TLS terminated externally), isolated `*_prod` volumes, `mercure`'s `ports` list rebuilt with `!override` (Compose merges `ports` by target port across files rather than replacing the list — confirmed empirically) so only the plain-HTTP port is published in prod, `database`'s `ports` cleared with `!reset []` (no host port at all in prod). Not deployed — CUT-1 is a separate, explicitly authorized packet.

### Verification tooling (`tools/migration-verify/`)

- `compare.mjs` — replays `capture/scenarios.json`'s DSL against `--base`, using the same normalization logic as `capture/capture.mjs` (copied, not imported — the oracle directory was never touched, confirmed by `git status` inside it showing nothing). Captures desktop+mobile screenshots, desktop-only a11y/texts/http/url/step per the oracle's own asymmetry. Compares per DEV-1..12 (`deviations.json`), writes `report.json`/`report.md`, exits non-zero on any regression.
- `performance.mjs` — initial JS gzip bytes from `frontend/dist/assets`, LCP (via a `PerformanceObserver` registered before navigation — reading `getEntriesByType("largest-contentful-paint")` after the fact was unreliable in headless Chrome, discovered and fixed mid-implementation) and a `domInteractive`-based TTI proxy for `/pl/login`, `/collect` p95 skipped with a note (introduced in wave-2).
- `deviations.json` — mechanical DEV-1..12 encoding; see §9 for the one extension I made to DEV-11's step range.
- `budget.json` — DEV-8 absolute budgets (300 kB gzip JS / 2.0 s LCP / 2.5 s TTI / 500 ms `/collect` p95).

### CI

`.github/workflows/next-build.yml` — two jobs (`backend`: builds the SPA, copies it in, `./mvnw verify`; `frontend`: typecheck, lint, unit+integration tests, build). File only, not executed by me.

## 4. Introduced dependencies

### Backend (Maven, versions from Spring Boot 4.1.1's BOM unless pinned)

| Dependency | Version | License |
| --- | --- | --- |
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | Apache-2.0 |
| org.springframework.boot:spring-boot-starter-web | (BOM) | Apache-2.0 |
| org.springframework.boot:spring-boot-starter-actuator | (BOM) | Apache-2.0 |
| org.springframework.boot:spring-boot-starter-jdbc | (BOM) | Apache-2.0 |
| org.springframework.session:spring-session-jdbc | (BOM, spring-session-bom 4.1.1) | Apache-2.0 |
| org.postgresql:postgresql | 42.7.13 | BSD-2-Clause |
| org.springframework.boot:spring-boot-starter-flyway | (BOM) | Apache-2.0 |
| org.flywaydb:flyway-database-postgresql | 12.4.0 | Apache-2.0 |
| org.springframework.boot:spring-boot-starter-webmvc-test (test) | (BOM) | Apache-2.0 |
| org.springframework.boot:spring-boot-restclient (test) | (BOM) | Apache-2.0 |
| org.springframework.boot:spring-boot-testcontainers (test) | (BOM) | Apache-2.0 |
| org.testcontainers:testcontainers-junit-jupiter (test) | 2.0.5 | MIT |
| org.testcontainers:testcontainers-postgresql (test) | 2.0.5 | MIT |
| com.tngtech.archunit:archunit-junit5 (test) | 1.5.0 | Apache-2.0 |
| com.diffplug.spotless:spotless-maven-plugin (build) | 3.10.2 | Apache-2.0 |
| com.google.googlejavaformat:google-java-format (via spotless) | 1.36.1 | Apache-2.0 |

### Frontend (npm)

| Dependency | Version | License |
| --- | --- | --- |
| vue | 3.5.42 | MIT |
| vue-router | 5.3.1 | MIT |
| pinia | 4.0.3 | MIT |
| vue-i18n | 11.4.10 | MIT |
| vite (dev) | 8.2.2 | MIT |
| @vitejs/plugin-vue (dev) | 6.0.8 | MIT |
| vitest (dev) | 5.0.0 | MIT |
| @vue/test-utils (dev) | 2.5.0 | MIT |
| vue-tsc (dev) | 3.3.11 | MIT (Volar/Vue) |
| typescript (dev) | 6.0.3 | Apache-2.0 |
| prettier (dev) | 3.8.4 | MIT |
| eslint (dev) | 10.10.0 | MIT |
| eslint-plugin-vue (dev) | 10.11.0 | MIT |
| typescript-eslint (dev) | 8.70.0 | MIT |
| @eslint/js (dev) | 10.0.1 | MIT |
| globals (dev) | 17.12.0 | MIT |
| jsdom (dev) | 30.0.1 | MIT |
| @types/node (dev) | 26.5.0 | MIT |

### tools/migration-verify (npm)

| Dependency | Version | License |
| --- | --- | --- |
| pixelmatch | 7.2.0 | ISC |
| pngjs | 7.0.0 | MIT |

### Runtime image (not a code dependency)

`dunglas/mercure:v0.24.2` — **AGPL-3.0** (already flagged in the plan's own Technology decisions table). Used unmodified as a service via its published image; not linked into or redistributed with this codebase's own code.

**Secret scan:** `git diff <parent-sha>..HEAD` grepped for private-key headers, `password=`, `secret=`, `api_key=` patterns outside `${…:-ChangeMe}`-style placeholders — the only hits were the Maven Wrapper script's own `MVNW_USERNAME=''`/`MVNW_PASSWORD=''` boilerplate (a well-known public script, empty-string handling, not a secret).

## 5. Gate table (final candidate SHA `f4ac025ca10a90ab41733a942000dc93769a273c`)

| Gate | Command | cwd | Exit | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| Focused (backend) | `./mvnw -q test` | `backend/` | 0 | `evidence/gates/backend-test.log` (59 tests, 0 failures) | PASS |
| Focused (frontend) | `npm run test -- --run` | `frontend/` | 0 | `evidence/gates/frontend-test.log` (5 files, 32 tests) | PASS |
| Integration (backend) | `./mvnw -q verify` | `backend/` | 0 | `evidence/gates/backend-verify.log` (Testcontainers Postgres 18; `SessionRoundTripIT` 2/2) | PASS |
| Integration (frontend) | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/gates/frontend-test-integration.log` (3 files, 13 tests) | PASS |
| Architecture (backend) | `ArchitectureTest` (inside `mvnw test`, above) | `backend/` | 0 | same as Focused (backend) | PASS |
| Architecture (frontend) | `npm run lint` | `frontend/` | 0 | `evidence/gates/frontend-lint.log` | PASS |
| Static (backend) | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/gates/backend-spotless.log` | PASS |
| Static (frontend) | `npm run lint && npm run typecheck && npm run build` | `frontend/` | 0 | `evidence/gates/frontend-{lint,typecheck,build}.log` | PASS |
| Contract + visual | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19001 --out evidence/compare` | repo root | 0 | `evidence/compare/login/report.md`, `report.json` | PASS — **0 regressions** |
| E2E | `npx playwright test public.spec.ts navigation.spec.ts -g "login\|sidebar collapse\|theme toggle"` | `tests/e2e/` | 0 | `evidence/green-playwright.txt` (4/4 passed) | PASS |
| Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19001` | repo root | 0 | `evidence/performance.txt` | PASS — see §8 |
| Sonar | — | — | — | no `sonar-project.properties`/`.sonarcloud.properties` anywhere in the repo | NOT_APPLICABLE |

Note on the E2E command: the packet's literal `npx playwright test public.spec.ts -g "login" navigation.spec.ts -g "sidebar collapse|theme toggle"` does not do what it reads as — Playwright's `-g`/`--grep` is a single global filter, so the second `-g` silently overrides the first and only `navigation.spec.ts`'s two tests ran (`--list` confirmed this). Combined into one filter across both files instead — `public.spec.ts navigation.spec.ts -g "login|sidebar collapse|theme toggle"` — verified with `--list` to select exactly the intended 4 tests, no more, no less.

## 6. compare.mjs report summary (journey `login`)

All 8 steps: **0 regressions**. Steps 1–3 (fresh login page, filling the two fields) are exact parity on every dimension — url, texts, aria, both screenshots (0.000% pixel difference), contract. Steps 4–8 (dashboard reached, the two rejected-login POSTs, logout, dashboard again) carry `accepted-deviation(DEV-11)` on texts/aria/both screenshots (0.000–0.173% pixel difference, under the 0.5% threshold) and `accepted-deviation(DEV-4,DEV-5,DEV-9)` on contract. The `db.json` comparison: `sessions`→`spring_session` delta 0 vs 0, `cache_items`→`event_dedup+shedlock` delta 0 vs 0 — exact match, no tolerance needed (see §3's session note). Full detail in `evidence/compare/login/report.md`.

## 7. E2E result

4/4 green: `public.spec.ts` → `login signs in and shows the identity in the sidebar`, `login the browser blocks a malformed address before it is sent`; `navigation.spec.ts` → `app shell sidebar collapse survives a reload`, `app shell theme toggle survives a reload`. Full run in `evidence/green-playwright.txt`.

## 8. Performance vs budget

| Metric | Value | Budget | Status |
| --- | --- | --- | --- |
| Initial JS (gzip) | 62,765 B (61.3 kB) | 307,200 B (300 kB) | PASS |
| LCP (`/pl/login`) | 144 ms | 2,000 ms | PASS |
| TTI proxy (`domInteractive`) | 14.1 ms | 2,500 ms | PASS |
| `/collect` p95 | skipped | 500 ms | N/A — `/collect` does not exist until wave-2 |

## 9. Deviations used and oracle claims I had to interpret

**Deviations exercised by this journey:** DEV-4 (document-kind HTTP comparison rule; full parity for `/{locale}/login`, `/{locale}/logout`), DEV-5 (db table mapping), DEV-9 (session cookie name ignored, sessions compared by count), DEV-11 (`.main-scroll` masked on the dashboard steps).

**Interpretations recorded mechanically in `deviations.json` (with reasoning inline there too):**

1. **DEV-11's step range.** The packet's J0 test-cycle bullet says "visual — steps 1–3, 5–7 exact, steps 4 and 8 with DEV-11 mask." But steps 5–7 use `context.request.post()` (out-of-band, confirmed via the oracle's own `http.jsonl` `"kind":"api"` entries) — the visible page never navigates away from the dashboard rendered at step 4, which is why the oracle's own `texts.json` for steps 4–7 are byte-identical. Masking only 4 and 8 while treating 5–7 as "exact" would require the wave-4 dashboard body to already exist, contradicting both DEV-11's own wave-4 expiry and the packet's explicit "`DashboardView` body empty" instruction. I extended DEV-11's step list to `[4,5,6,7,8]` as the only mechanically consistent reading; contract-dimension parity for steps 5–7 (the actual point of those steps — the login POST error responses) is unaffected and verified with full exactness.
2. **DEV-11's dimension scope.** Read as covering screenshot **and** aria **and** texts (not the pixel image alone), by analogy with DEV-12's explicit "skip visual/a11y/text" for the sibling 404 deviation — both are tagged dimension `visual`, and the verifier-contract table itself bundles screenshots+a11y+texts under one "visual" row.
3. **DEV-11's screenshot mechanism.** There is no live oracle page to re-locate `.main-scroll` on at compare time (only a static PNG). Used a fixed pixel rectangle instead, derived from the design tokens (`--sidebar-w: 248px`, `--topbar-h: 56px`), applied identically to both oracle and candidate images before diffing.
4. **`/` root behaviour.** The packet said to check whether `/` redirects to `/pl` or not. `context/migration-oracle/.../journeys/landing/steps/3/http.jsonl` records `GET /` → status **200** (not a redirect). Implemented accordingly: `/` serves the SPA document directly with locale `pl`, no redirect.
5. **Twig's `default()` filter on the login form's redisplay value.** Not something I could have known without capturing the oracle's actual response body: `last_username|default('maciej@aureashop.pl')` falls back on an *empty string*, not only on an undefined value. Oracle step 5 (empty-e-mail submission) shows `value="maciej@aureashop.pl"`, not an empty field — this only surfaced once `compare.mjs` ran for real against the live candidate (see §3).

## 10. Remaining risks

- **`server.forward-headers-strategy=framework` did not drive Spring Session's `Secure` cookie flag** the way I expected from the packet's "Secure=auto behind the proxy" phrasing; I hard-coded `secure: true` instead (see §3) and did not chase why the "auto" path doesn't work for Boot 4.1.1 — worth a closer look if a future wave needs the app to also be reachable over plain HTTP somewhere (it currently isn't, by design).
- **Boot 4.1's module split bit me three times** (`WebMvcTest`/`TestRestTemplate` need `spring-boot-starter-webmvc-test` + `spring-boot-restclient`, not the old `spring-boot-starter-test`; `spring-boot-starter-test-classic` pulls in unwanted `spring-security` test support; Flyway's autoconfiguration is a separate `spring-boot-flyway` module, not bundled with plain `flyway-core`). Later waves adding their own starters should expect the same pattern and verify with a real boot, not just a compile, before trusting a dependency addition.
- **404 for a path matching a known pattern but the wrong HTTP method** (e.g. `GET /pl/logout`) returns 404 here, where Symfony would return 405. Not exercised by any oracle-graded step in this journey; noted rather than fixed, since fixing it isn't in scope for a slice whose gates don't touch it.
- **DEV-2 (cardiogram mask)** is technically superseded by DEV-11 on login steps 4/8 while DEV-11 is active (both target overlapping/nested regions); once DEV-11 expires at wave-4, DEV-2's own mask will need to actually apply on those steps — recorded in `deviations.json`'s note for future workers.
- The oracle-stack GREEN proof (packet §7's optional step) was not performed — see §2.

## 11. Suggested integration test for the orchestrator

A cross-slice smoke test once wave-1 lands: bring up `compose.next.yaml`, run `compare.mjs --journey login` **and** whatever wave-1 introduces in the same process, confirming the `RouteTable`/`routes.ts` route table and the `ShellViewService`/shell store contract this slice froze are still exactly what the next journey's controllers/views build on — i.e. a "did wave-1 change the shell contract by accident" check, since nothing here enforces that beyond this slice's own tests.

## 12. Final candidate SHA and clean-worktree proof

```
$ git rev-parse HEAD
f4ac025ca10a90ab41733a942000dc93769a273c

$ git status --porcelain
(empty — clean)

$ git rev-parse --show-toplevel
/home/muszkin/work/kivvi-click-wt/w0-login

$ git rev-parse --abbrev-ref HEAD
migration/wave-0/login
```

Compose stack `kivvi-w-login` torn down with `down -v` before finishing this report; the built `kivvi-w-login-api` image was also removed to free disk. No changes were made outside this worktree except this report and `slices/w0-login/evidence/`.

## Repair-1

Repair packet: `slices/w0-login/repair-1.md`. Independent review returned FAIL on F1 (HIGH, blocking) plus F4 (LOW) to bundle. F2 and F3 were accepted as documented judgment calls — no change. Identity guard re-checked before starting (toplevel, branch, clean status, HEAD at `f4ac025`) and before this commit; never amended `f4ac025`.

### F1 — Spring Session JDBC persistence: root cause, fix, and proof

**Reproduced first, before touching any code.** Brought `kivvi-w-login` up, signed in via `curl -c jar`, and queried the compose database directly:

```
--- direct DB query ---
 count
-------
     0
(1 row)

 count
-------
     0
(1 row)
--- shell before restart ---
user.email = anna@aureashop.pl
--- shell AFTER restart, same cookie ---
user.email = maciej@aureashop.pl
```

(Full transcript: `evidence/repair-1-session-proof-BEFORE.txt`.) Confirmed the finding exactly: `spring_session`/`spring_session_attributes` both 0 despite a successful sign-in, and the identity was lost across an `api` container restart — proof the session was Tomcat's in-memory store, not Spring Session JDBC, the whole time.

**Root cause.** `backend/pom.xml` declared `spring-boot-starter-jdbc` + the raw `org.springframework.session:spring-session-jdbc` library. That puts `JdbcIndexedSessionRepository` on the classpath but never registers it: Spring Boot 4.1 splits Flyway-style autoconfiguration for Spring Session JDBC into its own module, `spring-boot-session-jdbc` (home of `JdbcSessionAutoConfiguration`), bundled only by the `spring-boot-starter-session-jdbc` starter — the exact same module-split pattern already hit once with Flyway in the original implementation (see worker-report.md §10, "Boot 4.1's module split bit me three times" — apparently a fourth time was still waiting). Confirmed via `mvn dependency:tree` (the autoconfiguration module was genuinely absent) and via `java -jar ... --debug` (no `JdbcSessionAutoConfiguration` match report at all) before the fix, and a matched, active `JdbcSessionAutoConfiguration` after it.

**Fix.** `backend/pom.xml`: replaced `spring-boot-starter-jdbc` + `spring-session-jdbc` with `spring-boot-starter-session-jdbc` (which bundles `spring-boot-starter-jdbc`, `spring-boot-session-jdbc`, and transitively `spring-session-jdbc` itself — no functional dependency lost, the missing autoconfiguration gained). No manual insert, no workaround — the root cause is a missing autoconfiguration module, fixed by adding it.

**Proof after the fix** (`evidence/repair-1-session-proof-AFTER.txt`): signed in, decoded the cookie (`DefaultCookieSerializer` base64-encodes the session id for the cookie value — the `spring_session.session_id` column holds the raw, decoded id; this asymmetry is what tripped up the first version of the strengthened test too, see below), queried directly:

```
--- direct DB query: spring_session ---
 count
-------
     2
(1 row)
 count
-------
     2
(1 row)
--- shell BEFORE restart ---
user.email = anna@aureashop.pl
--- restarting api container ---
--- shell AFTER restart, same cookie ---
user.email = anna@aureashop.pl
```

Identity survives the restart. (The count is 2, not 1, because the earlier BEFORE-fix sign-in on the same running stack also left a session cookie in a separate jar file and both journeys' sign-ins land in the same table across the two curl sequences in this evidence run — both are real JDBC-persisted rows, not a bug.)

**PHP oracle note (packet §F1.4):** the oracle's own `sessions` delta was 0→0 for the whole login journey (`context/migration-oracle/.../journeys/login/db.json`) even though step 4 signs in. I did not chase why inside the read-only oracle (out of scope — "Do not change the oracle"); it may be a genuinely lazier PHP/Symfony session-write path (no row created until something beyond identity is written), a capture-time coincidence, or something else. What changed on this side: before the fix, `compare.mjs`'s `db.json` comparison showed candidate `spring_session` delta 0→0 too — but only because the row was never being written at all, an accidental match for the wrong reason. After the fix, the candidate delta is now genuinely **1** (a real row, created once, for the journey's one sign-in — confirmed by `compare.mjs`'s own re-run, see the gate table below), which is exactly the discrepancy DEV-9's "sessions delta compared by count only" plus the ±1 tolerance already documented in `deviations.json`'s `compareDb()` note (worker-report.md §3, "this made the login journey's own db.json delta come out exactly 0/0" — that specific claim in the original report is superseded by this finding and should be read as "0 vs 1, inside DEV-9's accepted tolerance" now that the real write path is active). I have not changed `deviations.json`'s DEV-5/DEV-9 mapping or `compare.mjs`'s tolerance logic — both already anticipated exactly this delta before I knew it was real. Flagging for the orchestrator to decide whether the oracle capture itself warrants a note; I made no changes to it.

**Strengthened `SessionRoundTripIT`.** Added `signInPersistsThroughJdbcIndexedSessionRepository`: autowires `JdbcTemplate` and `JdbcIndexedSessionRepository` directly, queries `spring_session`/`spring_session_attributes` by session id (base64-decoded from the `Set-Cookie` header — the first version of this test failed with "0 rows" against the *already-fixed* dependency because I queried with the still-encoded cookie value; caught immediately by comparing the cookie against the `psql` output in the AFTER evidence, fixed the decode, re-ran, passed), and reads the session back through `sessionRepository.findById(id)` asserting the `panel.identity` attribute. **Confirmed the test fails for the right reason**: temporarily reverted `pom.xml` to the broken dependency pair and re-ran `SessionRoundTripIT` in isolation — all three tests in the class fail with `UnsatisfiedDependencyException: No qualifying bean of type 'org.springframework.session.jdbc.JdbcIndexedSessionRepository'` (the `@Autowired` field can't even be satisfied, so the Spring context fails to start) — then restored the fix and re-verified all three pass. This is a stronger failure mode than a mere assertion mismatch: the test cannot silently pass if Spring Session JDBC is not active.

### F4 — dead-branch frontend test

`frontend/test/integration/LoginView.spec.ts`'s "renders the empty-username error with an empty (not default) value" test asserted `data-last-username=""`, a state the backend never emits (it resolves a blank submission to `maciej@aureashop.pl` before injecting the attribute — see worker-report.md §9, item 5). Replaced with the real B12 contract: `data-last-username="maciej@aureashop.pl"` + `data-login-error="Podaj adres e-mail."` renders the default address in the field and the error message in the callout. Test count unchanged (7 tests in this file, same as before — replaced, not deleted); B12/B13 behaviour coverage stays meaningful.

### Files changed

- `backend/pom.xml` — F1 fix (see above).
- `backend/src/test/java/click/kivvi/SessionRoundTripIT.java` — F1 strengthened test.
- `frontend/test/integration/LoginView.spec.ts` — F4 fix.

### Gate chain (full rerun, in order, on the new candidate SHA `cfe7b48bedd871c2132b2d62cd215d135e5e3627`)

| # | Gate | Command | cwd | Exit | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Focused (backend) | `./mvnw -q test` | `backend/` | 0 | `evidence/repair-1-gates/backend-test.log` | PASS |
| 1 | Focused (frontend) | `npm run test -- --run` | `frontend/` | 0 | `evidence/repair-1-gates/frontend-test.log` (5 files, 32 tests) | PASS |
| 2 | Integration (backend, incl. strengthened IT) | `./mvnw -q verify` | `backend/` | 0 | `evidence/repair-1-gates/backend-verify.log` (`SessionRoundTripIT` 3/3) | PASS |
| 2 | Integration (frontend) | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/repair-1-gates/frontend-test-integration.log` (3 files, 13 tests) | PASS |
| 3 | Architecture (backend, ArchUnit) | inside `mvnw test` (row 1) | `backend/` | 0 | same as row 1 | PASS |
| 3 | Static (backend) | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/repair-1-gates/backend-spotless.log` | PASS |
| 3 | Architecture/static (frontend lint) | `npm run lint` | `frontend/` | 0 | `evidence/repair-1-gates/frontend-lint.log` | PASS |
| 3 | Static (frontend typecheck) | `npx vue-tsc --noEmit` | `frontend/` | 0 | `evidence/repair-1-gates/frontend-typecheck.log` | PASS |
| 3 | Static (frontend build) | `npm run build` | `frontend/` | 0 | `evidence/repair-1-gates/frontend-build.log` | PASS |
| 4 | Contract + visual | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19001 --out evidence/compare` | repo root | 0 | `evidence/repair-1-gates/compare-mjs.log`, `evidence/compare/login/report.md` | PASS — **0 regressions** (sessions delta now 0→1, `accepted-deviation(DEV-9)`, see F1 note above) |
| 5 | E2E | `npx playwright test public.spec.ts navigation.spec.ts -g "login\|sidebar collapse\|theme toggle"` | `tests/e2e/` | 0 | `evidence/repair-1-gates/playwright.log` (4/4 passed) | PASS |
| 6 | Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19001` | repo root | 0 | `evidence/repair-1-gates/performance.log` (JS 62.8kB/300kB, LCP 152ms/2000ms, TTI 16.2ms/2500ms) | PASS |

Compose stack `kivvi-w-login` torn down (`down -v`) after the gate chain; the rebuilt `kivvi-w-login-api` image removed again to free disk.

### New candidate SHA and clean-worktree proof

```
$ git rev-parse HEAD
cfe7b48bedd871c2132b2d62cd215d135e5e3627

$ git log --oneline -3
cfe7b48 fix: activate Spring Session JDBC autoconfiguration (repair-1 F1)
f4ac025 feat: stand up the Spring Boot + Vue 3 login walking skeleton (wave-0)
8d3fc32 docs: record the operator execution decision for the migration plan

$ git status --porcelain
(empty — clean)

$ git rev-parse --show-toplevel
/home/muszkin/work/kivvi-click-wt/w0-login

$ git rev-parse --abbrev-ref HEAD
migration/wave-0/login
```

`f4ac025` was never amended or rewritten — `cfe7b48` is a new commit on top of it. No changes were made outside this worktree except this report and `slices/w0-login/evidence/`.

## Repair-2

Identity guard first, every time (worktree `/home/muszkin/work/kivvi-click-wt/w0-login`, branch
`migration/wave-0/login`, starting `HEAD` `cfe7b48bedd871c2132b2d62cd215d135e5e3627`, clean
`git status --porcelain`). The wave-0 verifier cohort failed unit, integration and architecture
(contract, visual, e2e passed) — three findings, addressed without touching markup, CSS, routes,
compose or the edge.

### R2-A — `./mvnw test`/`verify` failed on a clean checkout (unit FAIL)

`LoginControllerTest` and `SpaDocumentControllerTest` both build an `IndexHtmlTemplate`, which
reads `classpath:static/index.html` — a file that exists only after the frontend build is copied
into `backend/src/main/resources/static`. On a fresh clone (no frontend build), `./mvnw test`
failed with 27 errors.

Added a test-scoped SPA document fixture at `backend/src/test/resources/static/index.html`:
minimal but structurally real (same `<html lang="pl">` anchor `SpaDocument` injects attributes
into, a real `<title>`, a mount point — no built assets, since no test asserts on them). Maven's
test classpath puts `target/test-classes` ahead of `target/classes`, so this file shadows the
real one whenever both are present, including in a local dev checkout that already ran
`npm run build`. Production packaging (`backend/Dockerfile`) is unaffected — it never runs from
`target/test-classes` — and `IndexHtmlTemplate` still fails application startup fast if the real
built file is missing there. The frontend-maven-plugin alternative (binding the frontend build
into the Maven lifecycle) was considered and rejected: it would make every backend-only test run
pay for a full `npm ci && npm run build`, and would tie backend unit tests to frontend build
health for no benefit these tests actually need. Documented in `backend/README.md`'s "Test"
section.

**Proof**: `git clean -xdf backend/src/main/resources/static frontend/dist` (only those two build
output directories), then `./mvnw -q clean test` and `./mvnw -q verify`, both green —
`evidence/repair-2-gates/gate-01-mvn-test.log`, `evidence/repair-2-gates/gate-02-mvn-verify.log`.

### R2-B — missing integration-level tests (integration FAIL)

Added `backend/src/test/java/click/kivvi/ShellApiIT.java` — full Spring context, real HTTP layer
(`TestRestTemplate`), Testcontainers Postgres 18:

- **B04**: `GET /api/v1/en/shell` returns the English navigation labels (`Dashboard`,
  `Event stream`, `Customers`, `Rules`, `Email campaigns`, `Popups & widgets`, `Product feeds`,
  `Customer import`, `Settings`) and `locale: "en"`; `GET /api/v1/pl/shell` returns the Polish
  labels and `locale: "pl"`.
- **B07**: `GET /de/dashboard` and `GET /pl/nonexistent` both 404 through the real HTTP layer
  (unlike a `@WebMvcTest` slice, this actually boots routing end to end).
- **Direct `ShellController` coverage**: the dashboard route's full payload — every nav group's
  label/route/href/badge, `currentSection`, `crumb`, `workspace` (name/meta/mark), and the default
  `user` identity — asserted against the oracle's exact values from
  `journeys/login/steps/8/a11y.json` (`aureashop.pl` / `Plan Pro · 3 strony` / `AS`; `Maciej
  Kowalczyk` / `maciej@aureashop.pl`; crumb `Pulpit`), not guessed ones.

`SessionRoundTripIT`'s single test labeled `B08/B10` only ever exercised the theme preference —
it overclaimed sidebar (B10) coverage it did not have. Split it: renamed to
`B08 a stored theme preference survives a second request via the JDBC session` (unchanged
behaviour, honest label), and added
`sidebarPreferenceRoundTripsThroughShellAndSpaDocument` (`B10`): `POST /preferences/sidebar
{"state":"collapsed"}`, then `GET /api/v1/pl/shell` → `sidebar: "collapsed"`, **and**
`GET /pl/dashboard` → the SPA document body contains `data-sidebar="collapsed"` — the round trip
the finding asked for, through both places the sidebar state is rendered.

Every value asserted in `ShellApiIT` was cross-checked against `ShellController`,
`ShellViewService`, `NavigationCatalog`, `ShellFixtures`, `SessionIdentityStore`,
`SessionPreferencesStore`, `Theme`/`SidebarState` and `messages_{pl,en}.properties` before
writing the test, so the assertions encode the oracle's values, not an accidental read-back of
whatever the code already returns.

### R2-C — the "fail-on-warning" test policy had no tooling (architecture FAIL)

`architecture/rules-translated.md` row 3 (wave-0) — the analogue of PHPUnit's `failOnWarning`/
`failOnNotice`, restricted to first-party code the same way PHPUnit restricted it to `src`.

**Backend** — `click.kivvi.testsupport.FailOnWarnLogExtension`: attaches a Logback
`ListAppender` to the `click.kivvi` logger for the duration of each test and fails it if
anything ≥ `WARN` was logged by a `click.kivvi.*` logger, or if `System.err` received output that
isn't already-known JVM/agent/library noise (`sun.misc.Unsafe`, Mockito's self-attach notice,
the dynamic-agent-loading warnings, byte-buddy-agent — all observed in this project's own test
runs, none from first-party code). Registered for every test through JUnit 6's extension
auto-detection (`src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`
+ `junit.jupiter.extensions.autodetection.enabled=true` in `junit-platform.properties`) rather
than `@ExtendWith` on individual classes, so a future test cannot opt out by omission — a base
class was considered and rejected for the same reason (a future test could simply not extend
it). The detection logic is exposed as pure static methods (`warningVerdict`/`stderrVerdict`) so
`FailOnWarnLogExtensionTest` can exercise it directly against synthetic log events, rather than
needing a permanently-failing test in the suite to prove the policy works. Additionally proved
against a real `LOG.warn(...)` call: temporarily added a throwaway test with a deliberate warning
under the live extension, confirmed it failed with
`AssertionError: click.kivvi.* logged at WARN or above during ...`, then deleted it and
re-confirmed a clean 66-test pass.

**Frontend** — `frontend/test/setup.ts` (wired in via `vite.config.ts`'s `test.setupFiles`):
`console.warn`/`console.error` are wrapped to fail the current test, but only when the call's
stack trace touches a frame under `src/**` and not `node_modules` — restricted to first-party
code, mirroring the backend's logger-name restriction, since a JS stack trace has no logger-name
equivalent. This distinction is load-bearing, not decorative: the very first run against the
existing suite failed `routes.spec.ts`'s B07 test (`does not match /de/dashboard`) on
vue-router's own `[VUE_ROUTER_R0004] No match found` diagnostic — correct, expected router
behaviour for a test deliberately triggering a no-match, not a first-party bug, and the
unfiltered version would have wrongly failed it. Any Vue runtime warning
(`config.global.config.warnHandler`, wired globally through `@vue/test-utils`' shared `config`
object so every `mount()` picks it up without per-test wiring) fails a test **unconditionally**,
regardless of origin — a Vue warning is always a first-party bug in how a component is built or
used, never third-party diagnostic noise. `test.dangerouslyIgnoreUnhandledErrors` stays `false`
(the default; nothing referenced it before this change).

**Proof**: a throwaway `src/_throwawayPolicyProbe.ts` (`console.warn(...)`) plus a throwaway spec
proved all three behaviours in one run — a `src/**`-origin `console.warn` failed its test, an
unfiltered `console.warn` called directly from the test file itself did **not** fail its test,
and mounting a component with a missing required prop failed its test via the Vue warnHandler —
then both throwaway files were deleted and the full suite re-run clean (32 unit + 13 integration
tests). Documented in `frontend/README.md`'s "Test" section.

### Files changed

- `backend/src/test/resources/static/index.html` — new, R2-A fixture.
- `backend/README.md` — R2-A and R2-C documentation.
- `backend/src/test/java/click/kivvi/ShellApiIT.java` — new, R2-B.
- `backend/src/test/java/click/kivvi/SessionRoundTripIT.java` — R2-B (honest B08/B10 split + new
  sidebar round trip).
- `backend/src/test/java/click/kivvi/testsupport/FailOnWarnLogExtension.java`,
  `FailOnWarnLogExtensionTest.java` — new, R2-C backend.
- `backend/src/test/resources/junit-platform.properties`,
  `backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` — new,
  R2-C backend wiring.
- `frontend/test/setup.ts` — new, R2-C frontend.
- `frontend/vite.config.ts` — wires `setupFiles`, pins `dangerouslyIgnoreUnhandledErrors: false`.
- `frontend/README.md` — R2-C documentation.

### Gate chain (full rerun, in order, on the new candidate SHA `2d2b5a8d25a73bf291394d8b5d9cbcff200f940f`)

| # | Gate | Command | cwd | Exit | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` (clean, no `static/`) | `git clean -xdf backend/src/main/resources/static frontend/dist` then `./mvnw -q clean test` | `backend/` | 0 | `evidence/repair-2-gates/gate-01-mvn-test.log` | PASS |
| 2 | `./mvnw -q verify` | `./mvnw -q verify` | `backend/` | 0 | `evidence/repair-2-gates/gate-02-mvn-verify.log` (`SessionRoundTripIT` 4/4, `ShellApiIT` 5/5) | PASS |
| 3 | Focused (frontend) | `npm run test -- --run` | `frontend/` | 0 | `evidence/repair-2-gates/gate-03-frontend-test.log` (5 files, 32 tests) | PASS |
| 4 | Integration (frontend) | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/repair-2-gates/gate-04-frontend-test-integration.log` (3 files, 13 tests) | PASS |
| 5 | Architecture (backend, ArchUnit) | `./mvnw -q -Dtest=ArchitectureTest test` (confirmed separately; also runs inside rows 1–2) | `backend/` | 0 | inline confirmation, see gate-01/02 | PASS |
| 5 | Static (backend, spotless) | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/repair-2-gates/gate-05-backend-spotless.log` (first run caught formatting violations in the new R2-B/R2-C files, fixed with `spotless:apply`, rows 1–2 rerun after) | PASS |
| 6 | Static (frontend) | `npm run lint && npm run typecheck && npm run build` | `frontend/` | 0 | `evidence/repair-2-gates/gate-06-frontend-static.log` | PASS |
| 7 | Compose stack up | `HTTP_PORT=19000 HTTPS_PORT=19001 HTTP3_PORT=19001 MERCURE_PUBLISHER_JWT_KEY=w-login-publisher MERCURE_SUBSCRIBER_JWT_KEY=w-login-subscriber MERCURE_JWT_SECRET=w-login-subscriber docker compose -p kivvi-w-login -f compose.next.yaml up -d --build --wait` | repo root | 0 | `evidence/repair-2-gates/gate-07-compose-up.log` (all three containers Healthy) | PASS |
| 8 | Contract + visual | `node tools/migration-verify/compare.mjs --journey login --base https://localhost:19001 --out evidence/compare` | repo root | 0 | `evidence/repair-2-gates/gate-08-compare-mjs.log`, `evidence/compare/login/report.md` | PASS — **0 regressions** |
| 9 | E2E — login | `npx playwright test public.spec.ts -g "login"` | `tests/e2e/` | 0 | `evidence/repair-2-gates/gate-09a-playwright-login.log` (2/2 passed) | PASS |
| 9 | E2E — navigation prefs | `npx playwright test navigation.spec.ts -g "sidebar collapse\|theme toggle"` | `tests/e2e/` | 0 | `evidence/repair-2-gates/gate-09b-playwright-navigation.log` (2/2 passed) | PASS |
| 10 | Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19001` | repo root | 0 | `evidence/repair-2-gates/gate-10-performance.log` (JS 61.3kB/300kB, LCP 160ms/2000ms, TTI 14.7ms/2500ms) | PASS |

Ran as two separate Playwright invocations, not a combined `-g` regex, per this packet's explicit
correction — Playwright only honours the *last* `-g` flag when it appears more than once on one
invocation.

Compose stack `kivvi-w-login` torn down (`down -v`) after the gate chain
(`evidence/repair-2-gates/gate-11-teardown.log`); the rebuilt `kivvi-w-login-api` image removed
again to free disk.

### New candidate SHA and clean-worktree proof

```
$ git rev-parse HEAD
2d2b5a8d25a73bf291394d8b5d9cbcff200f940f

$ git log --oneline -4
2d2b5a8 fix: honest integration coverage, fail-on-warning policy, buildable test suite (repair-2)
cfe7b48 fix: activate Spring Session JDBC autoconfiguration (repair-1 F1)
f4ac025 feat: stand up the Spring Boot + Vue 3 login walking skeleton (wave-0)
8d3fc32 docs: record the operator execution decision for the migration plan

$ git status --porcelain
(empty — clean)

$ git rev-parse --show-toplevel
/home/muszkin/work/kivvi-click-wt/w0-login

$ git rev-parse --abbrev-ref HEAD
migration/wave-0/login
```

`cfe7b48` was never amended or rewritten — `2d2b5a8` is a new commit on top of it. No changes
were made outside this worktree except this report and `slices/w0-login/evidence/`.

## Repair-3

Identity guard first (worktree `/home/muszkin/work/kivvi-click-wt/w0-login`, branch
`migration/wave-0/login`, starting `HEAD` `2d2b5a8d25a73bf291394d8b5d9cbcff200f940f`, clean
`git status --porcelain`). Round-2 wave cohort: contract, visual, e2e and architecture passed;
unit and integration failed on coverage gaps. Test-only changes, per the packet, unless a test
proved a real defect.

### R3-A — B12/B13 need IT-level coverage; a sibling test overclaimed B14

Added two tests to `backend/src/test/java/click/kivvi/ShellApiIT.java`, both `POST /pl/login`
through the real HTTP layer (full Spring context, Testcontainers Postgres):

- **B12** (`emptyEmailIsRejectedThroughTheRealHttpLayer`): `_username=` → `200`, SPA document
  carries `data-login-error="Podaj adres e-mail."` and `data-last-username="maciej@aureashop.pl"`.
- **B13** (`malformedEmailIsRejectedThroughTheRealHttpLayer`): `_username=not-an-email` → `200`,
  `data-login-error="To nie wygląda na poprawny adres e-mail."`,
  `data-last-username="not-an-email"`.
- Both additionally assert no session identity is established: whatever `Set-Cookie` (if any —
  neither rejection path ever calls `request.getSession(true)`) came back from the rejected POST
  is carried into a follow-up `GET /api/v1/pl/shell`, which must still report the default
  identity (`maciej@aureashop.pl`).

`SessionRoundTripIT.signInPersistsThroughJdbcIndexedSessionRepository` was `@DisplayName`-tagged
`B11/B14` but only ever signs in, never calls `/logout` — an honest overclaim fix, not a
behaviour change: renamed to just `B11`. `B14` (logout, restores the default identity) is
correctly exercised elsewhere in the same class by `signInThenSignOutRoundTripsTheIdentity`,
unchanged.

**Proof**: `ShellApiIT` run in isolation (`-Dit.test=ShellApiIT verify`), 7/7 green (5 from
repair-2 + 2 new); full `SessionRoundTripIT` 4/4 green with the renamed `@DisplayName`.

### R3-B — B22 needs a unit-dimension test

Added `frontend/test/unit/LoginView.spec.ts` (new file, reachable by `npm run test`'s
`vitest run test/unit`, unlike the existing `test/integration/LoginView.spec.ts`): asserts
`#f-_username` has `type="email"` and `name="_username"`, `#f-_password` has `type="password"`
and `name="_password"`, the form is `method="post"` with `action` ending in `/login`, and a
submit button exists.

**Deviation from the packet's literal wording, evidence-based**: did **not** assert a `required`
attribute on `#f-_username`, and did not add one to `Field.vue`/`LoginView.vue`. Checked the
oracle first: `journeys/login/steps/1/a11y.json`'s captured accessibility tree shows
`textbox "Email"` with no `[required]` marker, and the capture tool does surface that marker
elsewhere in the same oracle when a field genuinely has it (`[disabled]`/`[checked]`/`[expanded]`
appear in `journeys/settings`, `journeys/import-wizard`, `journeys/shell-navigation`) — its
absence here is signal, not a gap in capture. Grepped the entire oracle tree for `[required]`:
zero matches, in any journey, ever — consistent with the old stack validating this form
server-side rather than via native HTML5 constraint validation. This is also the only reading
under which B12 (the empty-e-mail server rejection, itself a captured, real oracle behaviour)
is reachable through an actual browser submission at all: a `required` field would make the
browser block an empty submission before it ever reached the server. Adding `required` was
considered and rejected — it would be a parity regression against the oracle, not a defect fix,
and this slice's own round-2 contract dimension already passed against the current (non-required)
markup.

**Proof**: the new file run in isolation, 4/4 green; full `npm run test -- --run`, 6 files
(was 5), 36 tests (was 32), all green.

### R3-C — bind formatting checks to the build

`spotless-maven-plugin` had no `<executions>` binding — `./mvnw verify` never actually ran
`spotless:check`, contradicting the CI step's own name
(`mvnw verify (unit + Testcontainers integration + ArchUnit + Spotless)`). Bound `check` to the
`verify` phase in `backend/pom.xml`. Added `"format:check": "prettier --check ."` to
`frontend/package.json` and a `Format check (Prettier)` step to the frontend job in
`.github/workflows/next-build.yml`, right after `Lint`. The backend CI step's name is now
accurate as written, so it needed no further correction.

Running `format:check` for the first time surfaced two pre-existing violations never caught
before — `frontend/index.html` and `frontend/src/styles/04-patterns.css`, both 2-space indented
against the project's documented 4-space convention (`frontend/README.md`'s "Test" section) —
plus this repair's own `test/setup.ts`. Fixed all three with `prettier --write`; diffed
`index.html`/the CSS file by hand first to confirm the change is whitespace/line-wrap only (same
tags, attributes, content; same CSS rule, different wrap point) — re-ran
`npm run test -- --run`, `test:integration -- --run`, `lint`, `typecheck`, `build` afterward, all
green, confirming no behavioural change.

**Proof the binding actually works**: injected a deliberate one-line indentation violation into
`ShellApiIT.java`, ran `./mvnw verify`, confirmed it failed with
`Failed to execute goal ... spotless-maven-plugin:...:check ... The following files had format
violations`, reverted the file from a pre-violation backup, re-ran `verify` clean.

### Files changed

- `backend/pom.xml` — R3-C, `spotless:check` bound to `verify`.
- `backend/src/test/java/click/kivvi/ShellApiIT.java` — R3-A, new B12/B13 tests.
- `backend/src/test/java/click/kivvi/SessionRoundTripIT.java` — R3-A, honest B11 rename.
- `frontend/test/unit/LoginView.spec.ts` — new, R3-B.
- `frontend/package.json` — R3-C, new `format:check` script.
- `.github/workflows/next-build.yml` — R3-C, new `Format check (Prettier)` step.
- `frontend/index.html`, `frontend/src/styles/04-patterns.css` — R3-C, whitespace-only
  reformat (pre-existing `format:check` violations, not previously enforced).
- `frontend/test/setup.ts` — R3-C, whitespace-only reformat (this slice's own file).

### Gate chain (in order, on the new candidate SHA `4e0bf5ca6ab163dae4da000783078fdc1f786139`)

| # | Gate | Command | cwd | Exit | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` (clean, no `static/`) | `git clean -xdf backend/src/main/resources/static frontend/dist` then `./mvnw -q clean test` | `backend/` | 0 | `evidence/repair-3-gates/gate-01-mvn-test.log` | PASS |
| 2 | `./mvnw -q verify` | `./mvnw -q verify` | `backend/` | 0 | `evidence/repair-3-gates/gate-02-mvn-verify.log` (`ShellApiIT` 7/7, `SessionRoundTripIT` 4/4, `spotless:check` genuinely ran — see proof above) | PASS |
| 3 | Focused (frontend) | `npm run test -- --run` | `frontend/` | 0 | `evidence/repair-3-gates/gate-03-frontend-test.log` (6 files, 36 tests) | PASS |
| 4 | Integration (frontend) | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/repair-3-gates/gate-04-frontend-test-integration.log` (3 files, 13 tests) | PASS |
| 5 | Architecture (backend, ArchUnit) | `./mvnw -q -Dtest=ArchitectureTest test` | `backend/` | 0 | `evidence/repair-3-gates/gate-05a-archunit.log` | PASS |
| 5 | Static (backend, spotless) | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/repair-3-gates/gate-05b-backend-spotless.log` | PASS |
| 6 | Static (frontend) | `npm run lint && npm run typecheck && npm run build` | `frontend/` | 0 | `evidence/repair-3-gates/gate-06-frontend-static.log` | PASS |
| — | Static (frontend, format — extra, not in the packet's list) | `npm run format:check` | `frontend/` | 0 | `evidence/repair-3-gates/gate-06b-frontend-format-check.log` | PASS |

**compare.mjs / Playwright / performance.mjs were not re-run**, per the packet's explicit
allowance ("because only test files change, you may skip re-running..."). Stated explicitly, as
required: every non-test file this repair touched is provably behaviour-neutral for a running
instance of the app — `backend/pom.xml`'s change only binds an existing static-analysis goal to
an existing build phase (no runtime code, no bytecode change); `.github/workflows/next-build.yml`
and `frontend/package.json`'s new script never execute against a running instance either; and
`frontend/index.html`/`frontend/src/styles/04-patterns.css`'s reformatting was hand-diffed and
confirmed whitespace/line-wrap only (same DOM, same CSS rule). The wave cohort re-runs everything
regardless.

### New candidate SHA and clean-worktree proof

```
$ git rev-parse HEAD
4e0bf5ca6ab163dae4da000783078fdc1f786139

$ git log --oneline -5
4e0bf5c test: honest B12/B13/B14 integration coverage, B22 unit coverage, bind format checks (repair-3)
2d2b5a8 fix: honest integration coverage, fail-on-warning policy, buildable test suite (repair-2)
cfe7b48 fix: activate Spring Session JDBC autoconfiguration (repair-1 F1)
f4ac025 feat: stand up the Spring Boot + Vue 3 login walking skeleton (wave-0)
8d3fc32 docs: record the operator execution decision for the migration plan

$ git status --porcelain
(empty — clean)

$ git rev-parse --show-toplevel
/home/muszkin/work/kivvi-click-wt/w0-login

$ git rev-parse --abbrev-ref HEAD
migration/wave-0/login
```

`2d2b5a8` was never amended or rewritten — `4e0bf5c` is a new commit on top of it. No changes
were made outside this worktree except this report and `slices/w0-login/evidence/`.
