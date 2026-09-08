# Worker report — w1-landing (wave-1, journey J1 "landing")

## Identity guard

Checked before starting, before every gate run, and before the final commit:

- `git rev-parse --show-toplevel` → `/home/muszkin/work/kivvi-click-wt/w1-landing` ✓ (matches worktree)
- `git branch --show-current` → `migration/wave-1/landing` ✓
- `git status --porcelain` → clean at start, and only intentional files staged before commit ✓
- Never worked in `/home/muszkin/work/kivvi-click` (main checkout) or another worktree.

No mismatch encountered; no abort needed.

## RED evidence

Before implementing, brought the compose stack up from the worktree at parent SHA
`887af4543c5a51255735f1ddfc0d8860d11551c0` (project `kivvi-w-landing`, HTTP 19010 / HTTPS 19011)
and confirmed the landing route still served `EmptyPageView` (`GET /api/v1/pl/landing` → 404).

- `VERIFY_COMPOSE="docker compose -p kivvi-w-landing -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19011 --out .../evidence/compare-red` → **exit 1**, failed at step 5 (`.hero-cta a` with text "demo" never appears — timeout). Evidence: `evidence/red-compare.txt`.
- `cd tests/e2e && E2E_BASE_URL=https://localhost:19011 npx playwright test public.spec.ts -g "landing"` → **3/3 failed** (hero/features/steps/pricing never render; the demo button doesn't exist; English hero heading missing). Evidence: `evidence/red-playwright.txt`.

Both failures are exactly the expected "empty page body" shape, not connection errors — the stack itself was reachable and serving other wave-0 routes correctly.

## Implementation summary

**Backend** (`backend/src/main/java/click/kivvi/`):
- `web/LandingController.java` (existing file, extended) — added `GET /api/v1/{locale:pl|en}/landing` returning `LandingView` directly (mirrors `ShellController`'s convention); the existing `/{locale}/demo` → 302 `/{locale}/dashboard` redirect was verified against the oracle's `journeys/landing/steps/5/http.jsonl` and needed no change.
- `application/LandingView.java` (new) — the wire-shape record: `features`, `steps`, `plans`, `trustPoints`, `previewTiles`, `previewSeries`, with nested `FeatureView`/`StepView`/`PlanView`/`PreviewTileView` records.
- `application/LandingViewService.java` (new) — maps `fixtures.LandingFixtures`' own record types into `LandingView`'s wire records.
- `fixtures/LandingFixtures.java` (new) — ported verbatim from `src/Panel/Content/LandingContent.php`: 6 features, 3 onboarding steps, Free/Pro plans, 3 trust points, 4 preview tiles (2 pre-formatted with `Format.number`), and the 60-point traffic sparkline (`sin`-based formula, rounded half-away-from-zero to 2 decimals, ported 1:1 from the PHP loop). **Key decision**: `LandingFixtures` returns its *own* `Feature`/`Step`/`Plan`/`PreviewTile` records rather than `application.LandingView`'s nested ones — an early attempt to have fixtures return `LandingView`'s types directly created an ArchUnit-flagged `application → fixtures → application` package cycle (fixtures depending on application's return types while application depends on fixtures' methods). Mirrors the existing `ShellFixtures.Workspace` / `ShellView.WorkspaceView` split; `LandingViewService` does the mapping.
- `domain/Format.java` (new) — full port of `src/Panel/Format.php` (not just `number()`, since common-journey-rules.md calls for the whole class and later slices will share it): `number` (grouped with the narrow no-break space U+202F, verified byte-for-byte against the oracle's `8 410`/`94 200zł`), `money`, `percent`, `initials`, `timeAgo`.
- Tests: `fixtures/LandingFixturesTest.java` (B22, 5 tests incl. the exact `Format.number` output), `web/LandingControllerTest.java` (B22, 4 tests: PL payload shape, EN same content, unsupported-locale 404, demo redirect).

**Frontend** (`frontend/src/`):
- `views/LandingView.vue` (new) — fetches `GET /api/v1/{locale}/landing` on mount (plain `fetch`, same pattern as `stores/shell.ts`), renders nothing until the payload arrives, then composes `.hero`, `.hero-preview` (via `HeroPreview`), `#features`, `#how`, `#pricing` reproducing `pages/landing.html.twig`'s DOM exactly (classes, inline styles, `id`s). Sets `document.title` to `kivvi·click — {kicker}` once at setup (the only route in this slice whose oracle title differs from the SPA's static default).
- `components/atoms/SparklineSvg.vue` (new) — port of `components/atoms/sparkline.html.twig`'s path-generation algorithm (same rounding-to-1-decimal, same two-path area+line SVG shape).
- `components/organisms/HeroPreview.vue` (new) — the `.hero-preview` frame (browser-chrome dots, KPI grid, sparkline + "N ev/s" overlay), port of the inline preview markup + `preview-chart.html.twig`.
- `components/molecules/Feat.vue`, `PriceCard.vue` (new) — the feature-grid item and pricing-card components.
- `router/routes.ts` — **only** the `home` route's `component:` swapped `EmptyPageView` → `LandingView`, plus the one import line.
- `i18n/messages/landing.pl.ts` / `landing.en.ts` (new) — the `landing.*` YAML block ported under a **`landingPage`** top-level key (not `landing`, which `PublicLayout.vue` already owns for the nav/footer labels this page's section eyebrows reuse via `t("landing.features")` etc. — same underlying Symfony message ids, so reusing rather than duplicating is faithful to the old stack, not a shortcut).
- `i18n/index.ts` — added the glob loader (verbatim text from common-journey-rules.md) merging `src/i18n/messages/*.{pl,en}.ts` into the base catalogues; the merge is intentionally loosely typed (`Record<string, unknown>`) and cast back to `{pl: typeof pl; en: typeof en}` only at the `createI18n` call site, since vue-i18n's overload resolution needs a concrete literal shape to infer the locale keys from.
- `tools/migration-verify/deviations.json` — DEV-11's `journeys` gained `"landing"`; `steps` changed from a flat `[4,5,6,7,8]` (which would have wrongly applied login's step numbers to landing too) to a per-journey object `{"login": [4,5,6,7,8], "landing": [5]}`. Landing step 5 (the demo click) lands on `/pl/dashboard`, the same still-empty wave-1 page login's steps 4–8 mask — same mechanism, same rationale, extended note in the deviation row.

**Markup-parity method used**: read `journeys/landing/steps/{1..5}/{a11y.json,texts.json,http.jsonl,step.json}` and the Twig sources side by side; reproduced every inline `style=`, every `{{ " " }}` glue point (icon+label, value+unit), and confirmed via `compare.mjs`'s pixel/text/aria dimensions (all `parity`, 0.000% pixel diff) rather than by inspection alone.

**Two notable findings during implementation**:
1. `@vue/test-utils` 2.5.0's `findAll()`/`find()` silently mismatch CSS selectors that combine an id or a bare tag name with a descendant class (`"#features .feat"`, `".hero-preview svg path"` both returned 0 elements) even though the same DOM queried with the platform's own `element.querySelectorAll(...)` returns the correct nodes. Confirmed with raw DOM calls before concluding it was a VTU quirk, not a component defect; worked around in `landing.spec.ts` with a small `within()` helper that scopes the id/tag part via VTU then queries the rest with raw `querySelectorAll`.
2. vue-i18n's dev-only `warnHtmlMessage` advisory fires for every `v-html`-rendered message containing HTML (`landingPage.headline`/`featuresTitle`/`howTitle`/`pricingTitle`) — an already-accepted pattern in this codebase (`AuthLayout.vue`'s `auth.headline`, never previously exercised by a test). Muted once, locally, in `landing.spec.ts` (`i18n.global.warnHtmlMessage = false`) rather than in the shared `src/i18n/index.ts`, which this slice may only touch for the messages loader.

## Introduced dependencies

None. No new Maven or npm dependencies — the implementation uses only what wave-0 already declared (Spring Web/Test, AssertJ, JUnit 6 on the backend; Vue 3.5.42, vue-i18n 11.4.10, vue-router 5.3.1, @vue/test-utils 2.5.0 on the frontend, all already in `package.json`/`pom.xml`).

## Gate table

All commands run from the worktree (`/home/muszkin/work/kivvi-click-wt/w1-landing`) on final candidate SHA `c8a5a9e84fe354caa13c38ed21f47da5309820d4` unless noted (gates 1–4 were also green on intermediate states of the same uncommitted tree; the numbers below are the final, post-commit confirmation is the same tree).

| # | Gate | Command | cwd | Exit | Evidence | Status |
|---|------|---------|-----|------|----------|--------|
| 1a | Backend focused | `./mvnw -q test` | `backend/` | 0 | (console; 75 tests, 0 failures) | PASS |
| 1b | Frontend focused | `npm run test -- --run` | `frontend/` | 0 | (console; 7 files / 45 tests) | PASS |
| 2a | Backend integration | `./mvnw -q verify` (Testcontainers Postgres 18) | `backend/` | 0 | `/tmp/verify.log` (not archived; console captured) | PASS |
| 2b | Frontend integration | `npm run test:integration -- --run` | `frontend/` | 0 | (console; 4 files / 16 tests) | PASS |
| 3a | ArchUnit | inside `./mvnw test` (`ArchitectureTest`) | `backend/` | 0 | same as 1a | PASS |
| 3b | ESLint | `npm run lint` | `frontend/` | 0 | (console; 0 errors, 0 warnings) | PASS |
| 3c | vue-tsc | `npm run typecheck` | `frontend/` | 0 | (console) | PASS |
| 4a | Spotless | `./mvnw -q spotless:check` | `backend/` | 0 | (console, after one `spotless:apply`) | PASS |
| 4b | Vite build | `npm run build` | `frontend/` | 0 | dist 66.58 kB gzip JS | PASS |
| 5 | Contract+visual | `VERIFY_COMPOSE="docker compose -p kivvi-w-landing -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19011 --out evidence/compare` | repo root | 0 | `evidence/compare/landing/report.md`, `evidence/compare-run.txt` | PASS — 0 regressions |
| 6 | E2E | `E2E_BASE_URL=https://localhost:19011 npx playwright test public.spec.ts -g "landing"` | `tests/e2e/` | 0 | `evidence/e2e-landing.txt` | PASS — 3/3 |
| 7 | Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19011` | repo root | 0 | `evidence/performance.txt` | PASS — all under budget |
| — | Sonar | not configured in repo | — | — | — | NOT_APPLICABLE |
| — | Secret scan | `git diff` / new files grepped for secret/password/token/key patterns | — | — | (inline check, no matches) | PASS |

Additional (not required by my packet's gate list, run as a regression guard since I touched shared `i18n/index.ts` and `router/routes.ts`): `compare.mjs --journey login` → 0 regressions; `public.spec.ts -g "login"` (2/2) and `navigation.spec.ts -g "sidebar collapse|theme toggle"` (2/2) all green against the same candidate build.

## compare.mjs report summary (journey landing)

Steps 1–4 (`/pl`, `/en`, `/`, `/pl` again): **exact parity** on url, texts, aria, desktop screenshot (0.000% pixel diff / 1,296,000 px), mobile screenshot (0.000% / 329,160 px), contract (`accepted-deviation(DEV-4)` — the standard document-vs-API baseline rule, not a real deviation from this slice).

Step 5 (click `.hero-cta a` "demo" → `/pl/dashboard`): contract parity on the `/pl/demo` 302 and the `/pl/dashboard` 200 (`accepted-deviation(DEV-4)`); visual dimensions `accepted-deviation(DEV-11)` as scoped — the dashboard body is still the wave-1 empty placeholder, masked the same way login's steps 4–8 already are.

DB deltas: `sessions`→`spring_session` and `cache_items`→`event_dedup+shedlock` both 0/0 (`accepted-deviation(DEV-9)`/`DEV-5)`), as expected for a read-only journey.

**Regressions: 0.**

## E2E result

`public.spec.ts -g "landing"`: 3/3 passed (hero/preview/features/steps/pricing structure; demo button → dashboard; English structure). Specs unchanged.

## Performance numbers vs budget

| Metric | Value | Budget | Pass |
|---|---|---|---|
| Initial JS (gzip) | 65,856 B | 307,200 B | ✓ |
| LCP (`/pl/login`, per performance.mjs's fixed target) | 196 ms | 2000 ms | ✓ |
| TTI | 17.9 ms | 2500 ms | ✓ |
| `/collect` p95 | skipped (introduced wave-2) | 500 ms | n/a |

## Deviations used and oracle interpretation notes

- **DEV-4** (document-vs-API HTTP comparison rule) — applies to every step, standard.
- **DEV-11** (mask `.main-scroll`) — extended to `landing` step 5 only, converting the `steps` field from a flat array (which, read literally, would have applied login's steps 4/6/7/8 to the landing journey too — those step indices don't exist in `journeys/landing/scenario.md`, which only defines steps 1–5) to a per-journey object. This is the one substantive interpretation call in this slice: the packet's "Out of scope" bullet says to add `landing: [5]` to DEV-11, and the mechanical way to do that without corrupting login's own step list was the object form `compare.mjs`'s `devAppliesToStep` already supports (see `DEV-12`'s existing use of the same shape).
- Oracle read: `journeys/landing/steps/3/http.jsonl` records `GET /` → **200** (not a redirect) — confirmed this before touching anything, since `RouteTable.match("/")` (built in wave-0) already resolves to `home`/PL/PUBLIC without a redirect, exactly matching. No backend routing change was needed for step 3.
- Oracle read: none of `LandingContent`'s returned arrays (`features`, `steps`, `plans`, `trustPoints`, `previewTiles`) ever pass through Symfony's translator — confirmed by grepping `LandingContent.php` for `trans(` (no hits) and cross-checking `journeys/landing/steps/2/texts.json` (EN): feature/step/plan copy stays Polish even on `/en`. The `GET /api/v1/{locale}/landing` payload is therefore locale-invariant by design, matching the old stack's actual behaviour rather than "translating everything because it's the SPA now."

## Remaining risks

- The two `@vue/test-utils` / vue-i18n quirks documented above (selector mismatch, HTML-message warning) are workarounds local to this slice's tests; any later journey doing similar `id`+class or `svg`+class VTU queries, or any new `v-html`-rendered translation, will hit the same things and should reuse the `within()` pattern / know to mute `warnHtmlMessage` locally rather than rediscover it.
- `LandingFixtures`' non-`number()` `Format` methods (`money`, `percent`, `initials`, `timeAgo`) are ported but not yet exercised by any test in this slice (landing only needed `number`) — per the parallel-safety file-touch restriction (`backend/src/test/**/landing*` only), a dedicated `FormatTest.java` wasn't created here; whichever slice first consumes those methods should add coverage for them at that point.

## Suggested integration test for the orchestrator

An end-to-end check that walks `/pl` → click "Zobacz panel demo" → lands on `/pl/dashboard` with the *actual* (not empty) dashboard body once wave-4 ships, confirming DEV-11's landing-step-5 mask can be retired at the same time login's is.

## Final candidate SHA and clean worktree

```
$ git rev-parse HEAD
c8a5a9e84fe354caa13c38ed21f47da5309820d4
$ git status --porcelain
(empty)
$ git branch --show-current
migration/wave-1/landing
```

Compose stack torn down (`docker compose -p kivvi-w-landing -f compose.next.yaml down -v`) and the built image (`kivvi-w-landing-api`) removed before writing this report.
