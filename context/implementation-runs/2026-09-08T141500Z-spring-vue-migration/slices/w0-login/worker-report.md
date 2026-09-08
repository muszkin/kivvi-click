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
