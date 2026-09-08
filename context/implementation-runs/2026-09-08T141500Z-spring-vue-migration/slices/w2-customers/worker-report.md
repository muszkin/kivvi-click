# Worker report — w2-customers (wave-2, journey J5 "customers")

## Identity guard

Checked before starting, before every commit and before every gate run:

- Worktree: `/home/muszkin/work/kivvi-click-wt/w2-customers`
- Branch: `migration/wave-2/customers`
- Parent SHA (verified against packet): `4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a`
- Lease: compose project `kivvi-w-customers`, `HTTP_PORT=19050`, `HTTPS_PORT=19051`, `HTTP3_PORT=19051` — used for every `docker compose`/`compare.mjs`/`performance.mjs`/Playwright invocation below; no other port/project touched.

## RED evidence (captured before implementing)

Procedure (mirrors the w1-feeds precedent, since the parent SHA already serves `GET /pl/customers[...]` as a 200 SPA document — the wave-0 `RouteTable` already lists `customers`/`customer_show` — so the RED signal is a content/behaviour regression, not a connection failure): `git stash push -u` to return the worktree to the parent commit, brought the lease stack up from that state, captured RED, tore it down and removed the image, then `git stash pop` to restore the implementation before the first candidate commit.

- `evidence/red-compare-summary.txt` + `evidence/red-compare/customers/steps/{1,2,3}/*` — `compare.mjs --journey customers`: steps 1–3 captured an empty `EmptyPageView` shell (no regressions reported for those, since nothing meaningful renders to diff yet); step 4 (`click .table tbody tr`) **times out after 30s** and the run aborts — there is no table to click, confirming the RED state directly.
- `evidence/red-playwright-customers.txt` — `customers.spec.ts`: **4/4 failed** — `.page-sub` never appears, `.table tbody tr` never appears (30s timeout), `.profile-card .profile-fact` count 0 vs 7, `.page-head a.btn.ghost` never appears (30s timeout).

## Implementation summary

**Backend** (`click.kivvi.{fixtures,application,web,web.dto}`), following the `web → application → fixtures` pattern from wave-1:

- `fixtures/CustomersFixtures.java` — ports `CustomerDirectory` 1:1: the 15 first names / 12 last initials / 4 segment tags, the `seed → {id, name, email, initials, orders, revenue, lastSeenMinutes, segment}` formula (`c_1000+seed`, `(seed*137)%1900+49` revenue, `seed%12` minutes, `seed%7` orders), `byId` throwing `NoSuchElementException` for `CustomersController`/`CustomersViewService` to catch (mirrors `CustomerDirectory::byId`'s `InvalidArgumentException`). Verified against the oracle: seed 0 → `c_1000`/"Anna K."/`anna.k@example.com`/49 zł/VIP (row 1), seed 1 → `c_1001`/"Kasia N."/186 zł/Powracający (row 2).
- `fixtures/AutomationsFixtures.java` — created per the packet with `activeForCustomer()` only (the 3 hard-coded automations from `AutomationCatalog::activeForCustomer`); kept the method name so the automations journey (wave-3) extends this class instead of replacing it.
- `application/CustomersViewService.java` — assembles the index (`subtitle`, 6 segment tiles, 24 rows, `page`/`pages=192`) and the 360 profile (`customer`+tags, `profileSub`, 7 facts, 3 automations, 5 tabs, 3 KPI scores, 9 timeline entries) from the fixtures + `Format`. `lastSeen` computed from the request-scoped `Instant.now()` passed down from the controller, exactly like `Format::timeAgo($now->modify('-N minutes'), $now)`. Two separate number-grouping conventions reproduced verbatim, not reconciled (same pattern `FeedsViewService.groupWithSpace` already documents): the subtitle uses `Format.number` (U+202F narrow no-break space — oracle bytes confirmed: `4 218 zidentyfikowanych klientów`), segment-tile counts use a plain-ASCII-space `groupWithSpace` (oracle bytes confirmed: `Wszyscy · 4 218`, plain space). The profile's 3 tags (`VIP`/`accent`, `subskrybent`/`brown`, `PL`/no tone) are hard-coded exactly like `CustomerController::show`, independent of the customer's own list-page segment — verified by test against `c_1001` (segment "Powracający" on the list, still "VIP" first tag on the profile).
- `web/dto/CustomerListResponse.java`, `web/dto/CustomerDetailResponse.java` — the two JSON wire contracts, nested records mirroring `FeedsResponse`'s style.
- `web/CustomersController.java` — three endpoints:
  - `GET /api/v1/{locale:pl|en}/customers?page=` → `CustomerListResponse`.
  - `GET /api/v1/{locale:pl|en}/customers/{id}` → `CustomerDetailResponse`, or `404` (no body) for an unknown id.
  - `GET /{locale:pl|en}/customers/{id:c_\d+}` → the SPA document (200, via `SpaDocumentService`) for a known id, or a minimal 404 HTML document for an unknown one. **This is the one deliberate design decision beyond a straight port**: the wave-0 `RouteTable`/`SpaDocumentController` only validate a request's URL *shape* (`customer_show`'s pattern is `c_\d+`, matching `c_9999` too), so serving `/pl/customers/c_9999` would 200 through the generic `/{locale}/**` wildcard exactly like any other numerically-shaped id. Symfony's old behaviour additionally checks *existence* (`CustomerDirectory::byId` throwing → `createNotFoundException`) before it can render anything, so `/pl/customers/c_9999` 404s there too. Reproduced by adding a third, more specific mapping in `CustomersController` (`/{locale:pl|en}/customers/{id:c_\d+}`) that Spring's handler-mapping comparator always prefers over `SpaDocumentController`'s `/{locale:pl|en}/**` wildcard for any request matching both — verified two ways: `CustomersControllerTest` (sliced, only `CustomersController` registered) and, more importantly, `CustomersApiIT` (full Spring context, `SpaDocumentController` *also* registered), which is the only test in the codebase that can actually prove the two controllers' mappings don't collide the wrong way. No file in `SpaDocumentController`'s own ownership was touched.

**Frontend** (`frontend/src/{components,views,i18n,router,composables}`):

- First-owner components, each a direct 1:1 port of its Twig source: `molecules/Table.vue` (generic over the row type via `<script setup generic="Row">`, so a caller's `#row` scoped slot gets a typed row instead of casting `unknown` — chosen since I'm the first and likely not last owner of this component), `molecules/Pagination.vue`, `molecules/ProfileFact.vue`, `molecules/TimelineItem.vue`, `molecules/Tabs.vue`. `molecules/SegmentStrip.vue` reproduces `filter-chip.html.twig`'s markup (`.filter-chip` button, `data-active`/`data-action`/`data-payload`) for the customers segment rail specifically — named for its one caller rather than `FilterChip`, since the general-purpose filter-chip/segmented-control molecules are owned by the parallel `event-stream` slice in the same wave (per the packet: "do NOT create FilterChip/Segmented").
- `views/CustomersView.vue` — ports `pages/customers.html.twig`. Fetches `GET /api/v1/{locale}/customers?page=` on mount (page read from `route.query.page`, clamped to ≥1 client-side the same way the backend clamps it), renders the segment rail, the table (7 columns; row click via a `go-customer` intent carrying just the row's id) and pagination.
- `views/CustomerView.vue` — ports `pages/customer.html.twig` + its two page-specific partials `pages/customers/{timeline,active-automations}.html.twig` (inlined, same reasoning as `w1-feeds`'s matching-diagnostic partial: they live under `templates/pages/`, never earned their own design-system component). The back link (`.page-head a.btn.ghost`) is hand-built rather than routed through the shared `PageHead.vue` organism, since that component has no `back` slot yet and is outside this slice's file allowance — `CustomerView.vue` reproduces `page-head.html.twig`'s exact DOM (`.page-head > div > (back link, h1.page-title, p.page-sub)` + `.page-actions`) directly instead.
- `router/routes.ts` — only the `customers`/`customer_show` routes' `component:` lines switched from `EmptyPageView` to `CustomersView`/`CustomerView`, plus the two import lines.
- `composables/useIntents.ts` — added `go-customer` (builds `/{locale}/customers/{id}` from just the id, mirroring the OLD stack's own generic `assets/app.ts` `go-customer` intent — the customers-index row could also have used the more literal `navigate` intent with a pre-built path like the Twig source did, but the packet explicitly asked for `go-customer`, and it already existed as a general intent in the old stack for exactly this shape of payload), `go-page` (same `?page=` URL-rewrite logic as the old stack's `go-page`), and `set-segment` — registered as an intentional no-op: the old stack's own `assets/app.ts` never wired a handler for `set-segment` either (the segment rail is visual-only there too), so this reproduces that exactly rather than inventing filtering behaviour the packet marks out of scope.
- `i18n/messages/customers.{pl,en}.ts` — the page-chrome copy the old templates ran through `|trans` (title, table column headers, action-button labels, back link, card titles/subs). Segment labels, table row content, profile facts, tabs, KPI scores and the timeline are **not** i18n keys: the old templates never ran any of that through `|trans` either (hard-coded Polish fixture text, confirmed by grepping `messages.pl.yaml`/`messages.en.yaml` for zero hits on those strings) — reproduced verbatim by the backend fixtures/view-service instead. Pagination's own text (`"← Poprzednia"`, `"Strona … z …"`, `"Następna →"`) is hard-coded in `Pagination.vue`, matching `pagination.html.twig`'s own lack of `|trans` (stays Polish even on `/en/…`, same PL-fallback pattern noted elsewhere in this codebase's `en.ts`).
- `src/i18n/index.ts`, `src/i18n/pl.ts`, `src/i18n/en.ts` — **not edited**, per the canonical-loader rule; the loader was already present in the parent SHA.

**Two cross-cutting fixes**, both discovered only because `compare.mjs --journey customers` is the first gate run to exercise a page with real query-string state and a card using the `body` slot (see "Gate 5" below for the before/after evidence):

- `frontend/src/components/organisms/Topbar.vue` — the locale-switch link (`localeHref`) was computed from `route.fullPath` (path + query string), so `/pl/customers?page=2`'s "PL" link pointed at `/en/customers?page=2`. The old stack builds this from Symfony's `path($route, $routeParams)` — the route's *path* parameters only, never the query string (confirmed via `topbar.html.twig`'s `path(app.request.attributes.get('_route'), ...)` call, and via the oracle's own step-2 `a11y.json`: `/en/customers`, no `?page=2`). Fixed to use `route.path`. Not exercised by any earlier journey's own gate run, since none of login/dashboard/landing/feeds ever carries meaningful query-string state.
- `frontend/src/views/CustomerView.vue` — the "Oś czasu" (timeline) card was built with `<template #raw>`; `customer.html.twig` actually passes the timeline through the card's `body:` param (the padded `.card-body`), not `raw:` — only the "Aktywne automatyzacje" card next to it uses `raw:`. Fixed to use `Card`'s default slot.

## Introduced dependencies

None. No `backend/pom.xml`, `frontend/package.json`, or lockfile changes — everything is built from what wave-0/wave-1 already ship (Spring Boot MVC/Jackson, JUnit/AssertJ/MockMvc/Testcontainers on the backend; Vue/vue-router/vue-i18n/Pinia/Vitest on the frontend). `tools/migration-verify` and `tests/e2e` `node_modules` were installed locally to run the gates (`npm install` in each; both already had `package.json`/lockfiles from wave-0, nothing added).

## Gate table (all on final candidate SHA `0cb6baa8a71d765b09034b92db2003ee45de34e7`)

| # | Gate | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- | --- |
| 1 | Backend unit | `./mvnw -q test` | `backend/` | 0 | `evidence/gate1-backend-unit.txt` (105 tests) |
| 1 | Frontend unit | `npm run test -- --run` | `frontend/` | 0 | `evidence/gate1-frontend-unit.txt` (10 files / 57 tests) |
| 2 | Backend integration | `./mvnw -q verify` | `backend/` | 0 | `evidence/gate2-backend-verify.txt` (Testcontainers Postgres 18; 25 IT tests incl. `CustomersApiIT` 6/6 — the routing-precedence proof) |
| 2 | Frontend integration | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/gate2-frontend-integration.txt` (7 files / 31 tests) |
| 3 | ArchUnit | inside gate-1/2 backend runs (`ArchitectureTest`) | `backend/` | 0 | `evidence/gate1-backend-unit.txt`, `evidence/gate2-backend-verify.txt` |
| 3 | Frontend lint | `npm run lint` | `frontend/` | 0 | `evidence/gate3-frontend-lint.txt` (0 errors; 2 pre-existing warnings on `FeedCard.vue`, not touched by this slice) |
| 3 | Frontend typecheck | `npm run typecheck` | `frontend/` | 0 | `evidence/gate3-frontend-typecheck.txt` |
| 4 | Backend static | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/gate4-backend-spotless.txt` |
| 4 | Frontend build | `npm run build` | `frontend/` | 0 | `evidence/gate4-frontend-build.txt` (JS entry 206.38 kB / gzip 72.05 kB) |
| 5 | compare.mjs | `VERIFY_COMPOSE="docker compose -p kivvi-w-customers -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey customers --base https://localhost:19051 --out .../evidence/compare` | repo root | 0 regressions (first run: 9 — see below) | `evidence/gate5-compare.txt`, `evidence/compare/customers/report.md` |
| 6 | Playwright `customers.spec.ts` | `E2E_BASE_URL=https://localhost:19051 npx playwright test customers.spec.ts` | `tests/e2e/` | 0 (4/4 passed) | `evidence/gate6-playwright-customers.txt` |
| 7 | performance.mjs | `node tools/migration-verify/performance.mjs --base https://localhost:19051` | repo root | within budget | `evidence/gate7-performance.txt` |
| — | Sonar | N/A | — | — | NOT_APPLICABLE — no `sonar-project.properties`/`.sonarcloud.properties` anywhere in the repo |
| — | Secrets | `git diff HEAD~1 HEAD \| grep -iE "password\|secret\|api[_-]?key\|token\|private[_-]?key\|BEGIN (RSA\|EC\|PGP)"` | repo root | none found | — |
| — | Sanity (beyond the packet's own scope) | `npx playwright test navigation.spec.ts` | `tests/e2e/` | 6 passed / 8 failed | `evidence/sanity-playwright-navigation.txt` — the 6 sidebar-entry cases for already-implemented pages (`dashboard`, `customers`, `feeds`) pass; the 6 failing "opens its page" cases are unimplemented routes (`events`/`automations`/`campaigns`/`popups`/`import`/`settings`, still `EmptyPageView` — out of scope for every wave-0..2 slice); "only .main-scroll scrolls" fails on `/pl/events` for the same reason; "sidebar collapse survives a reload" failed only under 4-way parallel workers and passed in isolation (`--workers=1`) — a pre-existing cross-test flake unrelated to anything in this diff, not chased further since it's outside this packet's scope |

## Gate 5 detail: before/after the two cross-cutting fixes

First `compare.mjs` run (before the Topbar/Card fixes) reported **9 regressions**:

- Step 1: `texts`/`aria`/`screenshotDesktop` (5.895%) regressions — root-caused to a **capture-timing race**, not a product defect: the candidate `desktop.png`/`a11y.json`/`texts.json` were entirely blank (verified — `a11y.json` is `{"aria": ""}`, the screenshot is solid white). Step 1 is the very first navigation in a cold browser context against a freshly-started container; `compare.mjs` uses `waitUntil: "domcontentloaded"` plus a fixed settle delay with no `waitText`/`waitAttr` follow-up for a plain `goto` step, so it can capture before the SPA's `onMounted` fetch resolves on a cold load. Steps 2/3 (same page, warm cache) had full parity even before any fix, confirming this diagnosis. Not chased further (no product code changed for this one) — re-ran green in the same session with no environment change, so treated as this specific fresh-container run's own warm-up cost, not something to encode as a new deviation.
- Step 2: `aria` regression — the **real** Topbar bug (`/en/customers?page=2` vs oracle `/en/customers`).
- Step 4: `screenshotDesktop` (1.040%) regression — the **real** Card `raw`-vs-`body` bug (timeline items rendering measurably more compact without the card-body's 16px padding context).
- Step 6: `texts`/`aria`/`screenshotDesktop`/`screenshotMobile` regressions — **not a product defect**: `deviations.json`'s DEV-12 already lists `"customers"` in its `journeys` array, but its `steps` field (once object-shaped) is looked up per-journey by `compare.mjs`'s `devAppliesToStep()`, and only had a `"shell-navigation": [21]` entry — `"customers"` had no steps entry at all, so the skip-on-404 mechanism never activated for step 6. The oracle's step 6 visual capture is the old stack's Symfony dev-mode exception page (confirmed by reading `steps/6/a11y.json`/`texts.json`), which the new stack's minimal 404 document is never meant to reproduce — exactly DEV-12's documented purpose. Fixed by adding `"customers": [6]` to DEV-12's `steps` object (see `tools/migration-verify/deviations.json`).

Second run (final candidate): **0 regressions** — every step exact parity (0.000% pixel difference on every screenshot, texts/aria byte-for-byte), contract `accepted-deviation(DEV-4)` on every step, step 6 `accepted-deviation(DEV-12)`, `db` deltas `accepted-deviation(DEV-9)`/`accepted-deviation(DEV-5)`.

## E2E result

`customers.spec.ts` — 4/4 passed: 24 rows / 7 columns / `.page-sub` contains "zidentyfikowanych klientów" / `.row .mono.muted` reads "Strona 1 z 192" / first `go-page` button disabled; a row click navigates to `/pl/customers/c_\d+` and `.profile-name` matches the clicked row's name; the profile shows 7 `.profile-fact`s, first `.profile-card .chip` is "VIP", 3 `.kpi-grid .kpi` tiles, 9 `.timeline .tl-item`s, 5 `.tab-strip .tab`s with the first `aria-selected="true"`; the back link (`.page-head a.btn.ghost`) returns to `/pl/customers`. Spec is byte-identical to what shipped in the parent SHA — nothing under `tests/e2e/specs/` was touched.

## Performance numbers vs budget

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| Initial JS (gzip) | 71,289 B | 307,200 B | yes |
| LCP | 164 ms | 2,000 ms | yes |
| TTI | 14.9 ms | 2,500 ms | yes |
| `/collect` p95 | skipped | 500 ms | N/A — `/collect` doesn't exist until a later wave, as documented by `performance.mjs` itself |

## Deviations used

- **DEV-4** (in scope per the packet) — every document-kind step (`GET /pl/customers[...]`) compared as method/path/status only; the actual page data travels over `GET /api/v1/{locale}/customers[...]`, outside the oracle's HTTP recording, treated as a new baseline per DEV-4.
- **DEV-12** (in scope per the packet, "step 6 is a 404 document") — extended its `steps` object with `"customers": [6]` in `tools/migration-verify/deviations.json`, since the mechanism existed but this journey's step mapping was missing (see "Gate 5 detail" above). This is the one edit outside `tools/migration-verify` that isn't `compare.mjs`/`performance.mjs` itself — permitted explicitly by the common rules ("deviations.json — extend ONLY with rows for your journey's DEV ids if the mechanism is missing").
- **DEV-9**/**DEV-5** — `db` dimension, unchanged mechanism, both `accepted-deviation` with delta 0/0.

No oracle claim required reinterpretation beyond the above three items (all backed by direct oracle bytes: `texts.json`, `a11y.json`, `step.json`, and the Twig sources).

## Remaining risks

- **Topbar.vue and CustomerView.vue's Card usage are outside this packet's nominal file list.** Both fixes were necessary to reach 0 regressions on `compare.mjs`, are minimal (one-line/one-slot-name changes), objectively correct against the oracle and the old Twig sources, and have no existing test coverage that could conflict with a parallel worker's own changes — `grep -rl "localeHref\|Topbar" frontend/test` returned nothing before this slice. Still: `Topbar.vue` is shared shell chrome any later wave could also touch; flagged here for the orchestrator/integrator's attention rather than left as a silent scope expansion.
- **`tools/migration-verify/deviations.json`** is shared verification infrastructure. The edit is additive only (one new key inside one existing deviation's `steps` object) and cannot change any other journey's `devAppliesToStep()` result (object-shaped `steps` is looked up strictly by journey id) — but a parallel worker independently discovering the same DEV-12 gap for a different journey (`settings`, `import-wizard` are also listed in DEV-12's `journeys` but have no `steps` entries yet either) is a plausible, low-risk merge point.
- **Step 1's capture-timing race** (see "Gate 5 detail") reproduced blank on the first run of a freshly-built image and did not reproduce on the rebuilt image's run — worth the orchestrator's attention if it recurs for another journey's very first `compare.mjs` step, since the fix (if one is ever needed) belongs in `compare.mjs` itself (a `waitText`/settle-time increase for cold `goto` steps), not in any slice's product code.

## Suggested integration test for the orchestrator

Once a later wave adds a `back` slot to `PageHead.vue` (or a shared `back`-link pattern), re-point `CustomerView.vue`'s hand-built page-head markup at it rather than leaving two independent implementations of the same DOM shape. Also worth a cross-journey smoke check once `settings`/`import-wizard` land: both are listed in DEV-12's `journeys` array without a `steps` entry yet, so their own 404 steps (if any) will hit the same gap this report's Gate-5 section found and fixed for `customers`.

## Final candidate SHA and clean worktree

```
$ git log --format='%H %s' -2
0cb6baa8a71d765b09034b92db2003ee45de34e7 fix: customers gate-5 regressions found by compare.mjs (#customers)
f84ee18d2f2056340310c39825ce048ee9e4cff5 feat: add customers index and 360 profile (#customers)

$ git status
On branch migration/wave-2/customers
nothing to commit, working tree clean
```

Stack torn down (`docker compose -p kivvi-w-customers -f compose.next.yaml down -v`) and the built image (`kivvi-w-customers-api`) removed after the last gate run — confirmed absent from both `docker compose ls` and `docker images`.
