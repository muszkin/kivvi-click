# Worker report — w1-feeds (wave-1, journey J2 "feeds")

## Identity guard

Checked before every gate and before the commit:

- Worktree: `/home/muszkin/work/kivvi-click-wt/w1-feeds`
- Branch: `migration/wave-1/feeds`
- Parent SHA (verified against packet): `887af4543c5a51255735f1ddfc0d8860d11551c0`
- Lease: compose project `kivvi-w-feeds`, `HTTP_PORT=19020`, `HTTPS_PORT=19021`, `HTTP3_PORT=19021` — used for every `docker compose` and `compare.mjs`/`performance.mjs` invocation below; no other port/project touched.

## RED evidence (captured before implementing)

The parent SHA already serves `GET /pl/feeds` as a 200 SPA document (the wave-0 `RouteTable` already lists the `feeds` route), so the RED signal is a **content** regression, not a connection failure. Procedure: `git stash push -u` to return the worktree to the parent commit, brought the lease stack up from that state, captured RED, tore it down and removed the image, then `git stash pop` to restore the implementation before any gate ran.

- `evidence/red-compare/feeds/report.md` (+ `report.json`, steps/1/*) — `compare.mjs --journey feeds`: **4 regressions** (`visual.texts`, `visual.aria`, `visual.screenshotDesktop` 3.627%, `visual.screenshotMobile` 2.210%); `contract` itself is `accepted-deviation(DEV-4)` since the document-only comparison already passes on an empty page.
- `evidence/red-playwright-lists.txt` — `lists.spec.ts -g "product feeds"`: **2/2 failed** (`.feed-source` count 0 vs expected 4; `.bar-row` count 0 vs expected 4).
- `evidence/red-playwright-navigation.txt` — `navigation.spec.ts -g "feeds"`: **1/1 failed** (`.page-title` never appears — `EmptyPageView` has no heading).
- `evidence/red-compare-summary.txt` — stdout of the compare.mjs RED run, for a quick read.

## Implementation summary

**Backend** (`click.kivvi.{domain,fixtures,application,web,web.dto}`), following the wave-1 `web → application → fixtures` pattern:

- `domain/Format.java` — did not exist yet; ported verbatim from `src/Panel/Format.php` (`number`/`money`/`percent`/`initials`/`timeAgo`), including the U+202F narrow-no-break-space thousands separator. Per the packet, the landing worker may create an identical file independently in its own worktree; this port is the reference to reconcile against at merge.
- `fixtures/FeedsFixtures.java` — ports `ProductFeedCatalog` 1:1: the 4 KPIs, 4 sources, 4 raw connected feeds (unformatted `products`/`mapped`/`lastSyncMinutes`), 4 coverage bars and 3 fallback rules, plus the 142 mismatched count.
- `application/FeedsViewService.java` — the one allowed application file; assembles a `Payload` record from `FeedsFixtures`, computing per-feed display strings that used to be computed by `feed-card.html.twig` itself: `lastSync` (mirrors the old `intdiv`/`min temu`/`godz. temu` branch), `products`/`mapped` grouped with a **plain ASCII space** (Twig's own `number_format(0, ',', ' ')` — a different, deliberately un-reconciled convention from `Format.number`'s narrow-no-break space), and `mappedPercent` via `Format.percent` (safe to reuse: the value never reaches 1000, so the separator choice never actually triggers), `null` when `products == 0` (mirrors the Twig `{% if products %}` guard).
- `web/dto/FeedsResponse.java` — the JSON wire contract (`kpis`, `sources`, `feeds`, `coverage`, `fallbackRules`, `mismatched`), nested records mirroring `ShellView`'s style.
- `web/FeedsController.java` — `GET /api/v1/{locale:pl|en}/feeds`; maps `FeedsViewService.Payload` (which itself references `FeedsFixtures`' pass-through record types for the parts that need no computation) into `FeedsResponse`. **Architecture note:** `FeedsViewService` cannot return `web.dto.FeedsResponse` directly — that would make `application` depend on `web`, which combined with the existing `web → application` edge (the controller calling the service) is a 2-node package cycle that `ArchitectureTest`'s `topLevelPackagesFormNoCycle` rule (`SlicesRuleDefinition.slices().matching("click.kivvi.(*)..")`, which treats `web.dto` as part of the `web` slice) rejects. The mapping therefore lives in the controller — verified by running `ArchitectureTest` green, not just by reasoning about it.
- Payload is **locale-invariant** (mirrors the old page: `ProductFeedCatalog` never translated its fixture text); `{locale:pl|en}` only gates the route the same way `ShellController` does.

**Frontend** (`frontend/src/{components,views,i18n,router}`):

- First-owner components, each a direct 1:1 port of its Twig source: `atoms/Dot.vue`, `atoms/Sparkline.vue` (SVG trend line, same point-generation algorithm), `atoms/Bar.vue` (the packet's "Bar/BarRow" — the old stack has exactly one atom, `bar.html.twig`, that renders the whole `.bar-row`; no separate row wrapper exists to port, so I did not invent one), `molecules/KpiTile.vue`, `molecules/Card.vue`, `organisms/KpiGrid.vue`, `organisms/FeedCard.vue` (owns the `badges`/`states` lookup maps, exactly as `feed-card.html.twig` does internally).
- `views/FeedsView.vue` — ports `pages/feeds.html.twig` **and** the page-specific partial `pages/feeds/matching.html.twig` (inlined: it lives under `templates/pages/`, not `templates/components/`, so it was never a reusable design-system component and gets no Vue component of its own). Fetches `GET /api/v1/{locale}/feeds` on mount and renders from the response; never formats a number itself — `products`/`mapped`/`mappedPercent` arrive as pre-formatted strings, `mismatched` as a raw int (the Twig source never ran that particular field through `number_format` either, so neither does this).
- `router/routes.ts` — only the `feeds` route's `component:` line switched from `EmptyPageView` to `FeedsView`, plus the one import line.
- `i18n/messages/feeds.{pl,en}.ts` — the page-chrome copy that Twig ran through `|trans` (title/sub, section headings, button labels, the `showMismatched` interpolated count). The feed-source/feed-card/matching-diagnostic labels are **not** translated here because the old templates never ran them through `|trans` either (hard-coded Polish fixture text) — reproduced verbatim in `FeedCard.vue`/`FeedsView.vue` instead of inventing translation keys that didn't exist.
- `i18n/index.ts` — added the common-rules glob loader (identical text) since it wasn't present yet, plus one real fix discovered while writing the first HTML-bearing, `t()`-and-`v-html`-rendered message ever actually mounted in a test in this codebase (`feeds.sub` — `auth.headline` on `AuthLayout` carries the same shape but is never mounted by any existing test): vue-i18n's `warnHtmlMessage` (default `true`) logs a console.warn for any translated string containing HTML, which the frontend's fail-on-warning test policy (`test/setup.ts`) turns into a hard test failure. Set `warnHtmlMessage: false` on `createI18n` — the same "developer-authored, fixed strings only, never user input" justification already used to disable `vue/no-v-html` in `eslint.config.js`. This is shared infrastructure that benefits every journey with `|raw` Twig content, not just this one.

## Introduced dependencies

None. No `backend/pom.xml`, `frontend/package.json` or lockfile changes — everything is built from what wave-0 already ships (Spring Boot/MVC/Jackson, JUnit/AssertJ/MockMvc/Testcontainers on the backend; Vue/vue-router/vue-i18n/Pinia/Vitest on the frontend).

## Gate table (all on final candidate SHA `81af73fedfc5750d99a665a38bc064695d244279`)

| # | Gate | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- | --- |
| 1 | Backend unit | `./mvnw -q test` | `backend/` | 0 | `evidence/gate1-backend-unit.txt` |
| 1 | Frontend unit | `npm run test -- --run` | `frontend/` | 0 | `evidence/gate1-frontend-unit.txt` (8 files / 42 tests) |
| 2 | Backend integration | `./mvnw -q verify` | `backend/` | 0 | `evidence/gate2-backend-verify.txt` (Testcontainers Postgres 18; `FeedsApiIT`, `ShellApiIT`, `SessionRoundTripIT` all boot real Spring contexts) |
| 2 | Frontend integration | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/gate2-frontend-integration.txt` (4 files / 17 tests) |
| 3 | ArchUnit | inside gate 1/2 backend runs (`ArchitectureTest`) | `backend/` | 0 | `evidence/gate1-backend-unit.txt`, `evidence/gate2-backend-verify.txt` |
| 3 | Frontend lint | `npm run lint` | `frontend/` | 0 | `evidence/gate3-frontend-lint.txt` (0 errors, 2 pre-existing-style warnings on `FeedCard.vue`'s deliberately whitespace-tight `<template>` wrapper — see markup-parity note below) |
| 3 | Frontend typecheck | `npm run typecheck` | `frontend/` | 0 | `evidence/gate3-frontend-typecheck.txt` |
| 4 | Backend static | `./mvnw -q spotless:check` | `backend/` | 0 | `evidence/gate4-backend-spotless.txt` |
| 4 | Frontend build | `npm run build` | `frontend/` | 0 | `evidence/gate4-frontend-build.txt` (JS entry 188.18 kB / gzip 67.32 kB) |
| 5 | compare.mjs | `node tools/migration-verify/compare.mjs --journey feeds --base https://localhost:19021 --out .../evidence/compare` | repo root | 0 regressions | `evidence/compare/feeds/report.md` |
| 6 | Playwright `lists.spec.ts` | `E2E_BASE_URL=https://localhost:19021 npx playwright test lists.spec.ts -g "product feeds"` | `tests/e2e/` | 0 (2/2 passed) | `evidence/green-playwright-lists.txt` |
| 6 | Playwright `navigation.spec.ts` | `E2E_BASE_URL=https://localhost:19021 npx playwright test navigation.spec.ts -g "feeds"` | `tests/e2e/` | 0 (1/1 passed) | `evidence/green-playwright-navigation.txt` |
| 7 | performance.mjs | `node tools/migration-verify/performance.mjs --base https://localhost:19021` | repo root | within budget | `evidence/performance.txt` |
| — | Sonar | N/A | — | — | NOT_APPLICABLE — no Sonar config anywhere in the repo (confirmed by absence of `sonar-project.properties`/`.sonarcloud.properties`) |
| — | Secrets | `git diff HEAD~1 HEAD \| grep -iE "password\|secret\|api[_-]?key\|token\|private[_-]?key\|BEGIN (RSA\|EC\|PGP)"` | repo root | none found (only the pre-existing `!ChangeMe!`/`!ChangeThisMercure...!` compose placeholders, untouched by this diff) | — |

## compare.mjs report summary (candidate)

Step 1 (`GET /pl/feeds`): `contract` `accepted-deviation(DEV-4)` (document-kind comparison only — the data itself arrives via `GET /api/v1/pl/feeds`, recorded as new baseline per DEV-4, not compared against the oracle's server-rendered HTML). Every visual dimension is **exact parity**: `texts` parity, `aria` parity, `screenshotDesktop` 0.000% pixel difference (0/1,296,000), `screenshotMobile` 0.000% pixel difference (0/329,160). `db` deltas: `sessions`→`spring_session` 0 vs 0 (`accepted-deviation(DEV-9)`), `cache_items`→`event_dedup+shedlock` 0 vs 0 (`accepted-deviation(DEV-5)`). **0 regressions.**

## E2E result

- `lists.spec.ts -g "product feeds"`: 2/2 passed — `.feed-source` ×4, `.feed-card` ×4, `.feed-card__err` ×1 containing "HTTP 503", `.feed-card .chip.info .dot.live` ×1, matching-diagnostic `.bar-row` ×4, `event.product_id`/`feed.id` text present, `[data-action=show-mismatched]` containing "142".
- `navigation.spec.ts -g "feeds"` matched exactly one test (`app shell > sidebar entry "feeds" opens its page`, since the spec interpolates the route name into the title and "feeds" only appears in that one generated title) — 1/1 passed: sidebar click navigates to `/pl/feeds`, `.page-title` contains "Feedy produktów", `aria-current="page"` on the active nav item.
- Both specs are byte-identical to what shipped in wave-0 — nothing under `tests/e2e/specs/` was touched.

## Performance numbers vs budget

| Metric | Value | Budget | Pass |
| --- | --- | --- | --- |
| Initial JS (gzip) | 66,586 B | 307,200 B | yes |
| LCP | 164 ms | 2,000 ms | yes |
| TTI | 16.7 ms | 2,500 ms | yes |
| `/collect` p95 | skipped | 500 ms | N/A — `/collect` doesn't exist until wave-2, as documented by `performance.mjs` itself |

## Deviations used

**DEV-4** (in scope per the packet) — the only one exercised: `GET /pl/feeds` is compared as a document request (method/path/status), not by HTML body; the actual page data travels over `GET /api/v1/pl/feeds`, which is outside the oracle's HTTP recording and is treated as a new baseline, not a regression. No oracle claim required reinterpretation beyond what DEV-4 and the common rules already spell out.

## Remaining risks

- **`Format.java` merge**: created identically to the port described in the common rules (`number`/`money`/`percent`/`initials`/`timeAgo`, same signatures). If the parallel `landing` worker's independently-created copy differs in any byte (e.g. rounding mode, separator character), the wave integrator will see a real conflict on this file — by construction, not by omission, since both workers were told to create it "identically" from the same source (`src/Panel/Format.php`).
- **`frontend/src/i18n/index.ts`**: I made two changes here (the glob loader, required by every wave-1+ journey, and `warnHtmlMessage: false`, a genuine bug-in-waiting fix). Both are additive and small; a concurrent edit from `landing`/`scheduler-heartbeat` touching the same file's loader block is the expected, common-rules-sanctioned merge point — but if another worker also independently discovers the same `warnHtmlMessage` issue and fixes it differently (e.g. per-message `escapeParameterHtml` instead), that specific line is a second, unplanned collision point worth the integrator's attention.
- **`FeedCard.vue`'s two lint warnings** (`vue/multiline-html-element-content-newline` on the `<template v-if="mappedPercent">` wrapper) are Prettier's own whitespace-preserving output fighting eslint-plugin-vue's formatting preference on a spot that is *deliberately* whitespace-tight (the `>{{ … }}<` construct that keeps `"1 284 (100,0%)"` from losing its connecting space to Vue's newline-collapsing, exactly the pattern the common rules flagged from `WorkspaceCard.vue` in wave-0). Non-blocking (0 errors, gate exits 0) but flagged here so a reviewer doesn't mistake it for sloppiness.

## Suggested integration test for the orchestrator

A cross-journey smoke check once `landing` (and later journeys reusing `KpiGrid`/`Card`/`Bar`/`Dot`) lands: mount `KpiGrid`/`Card` with the *other* journey's fixture shapes and assert no prop-shape drift, since this slice is the sole owner of those components' contracts and nothing currently guards against a later journey silently depending on an undocumented prop.

## Final candidate SHA and clean worktree

```
$ git log -1 --format='%H %s'
81af73fedfc5750d99a665a38bc064695d244279 feat: add product feeds page (#feeds)

$ git status
On branch migration/wave-1/feeds
nothing to commit, working tree clean
```

Stack torn down (`docker compose -p kivvi-w-feeds -f compose.next.yaml down -v`) and the built image (`kivvi-w-feeds-api`) removed after the last gate run.
