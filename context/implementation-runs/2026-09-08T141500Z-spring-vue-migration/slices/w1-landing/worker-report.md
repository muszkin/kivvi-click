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

## Update-1

Rebased the reviewed candidate `c8a5a9e84fe354caa13c38ed21f47da5309820d4` onto `migration/spring-vue`'s new
feature HEAD `6e7a847c5bcda11149585f7007bf3f2f349b0394` (feeds journey + its repair-1, "canonical i18n loader,
format:check clean") per `update-1.md`.

### Identity guard (re-checked before starting)

`git rev-parse --show-toplevel` → `/home/muszkin/work/kivvi-click-wt/w1-landing` ✓; `git branch --show-current`
→ `migration/wave-1/landing` ✓; `git status --porcelain` → clean ✓; `git merge-base c8a5a9e 6e7a847` →
`887af4543c5a51255735f1ddfc0d8860d11551c0` (confirms both branches share the same wave-0 root, so the
rebase target is a genuine fast-forward-of-history situation, not an unrelated tree).

### `git rebase 6e7a847c5bcda11149585f7007bf3f2f349b0394`

Exactly the three conflicts update-1.md predicted, nothing else:

- `backend/src/main/java/click/kivvi/domain/Format.java` — add/add conflict.
- `frontend/src/i18n/index.ts` — content conflict.
- `frontend/src/router/routes.ts` — content conflict.

`tools/migration-verify/deviations.json` auto-merged cleanly (feeds never touched it, confirmed by
`git diff 887af45 6e7a847 -- tools/migration-verify/deviations.json` being empty before starting) — the
per-journey DEV-11 object (`{"login": [...], "landing": [5]}`) survived untouched.

### Conflict resolution

**Format.java** — inspected both versions method-by-method before resolving. Every method the two
implement has the *same signature* in both (`number`, `money`, `percent(double)`,
`percent(double,int)`, `initials`, `timeAgo`) — feeds' version implements each one differently
(`BigDecimal`+`RoundingMode.HALF_UP`+a `group()` helper vs. my `Math.round`+a
`formatNumber`/`groupThousands` pair) but there is no method present in one and absent from the
other, so the "union" is the feeds (HEAD) file verbatim — copied via `git show
6e7a847...:...Format.java > ...Format.java` (byte-for-byet, not retyped, to guarantee the U+202F
narrow-no-break-space literal survived — a first retype attempt silently produced a plain ASCII
space instead, caught by re-diffing against the git blob before moving on). Representative diff
(mine → HEAD, full diff in `evidence/update-1/format-diff.txt`):

```diff
   public static String number(double value) {
-    return formatNumber(value, 0);
+    BigDecimal rounded = BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
+    return group(rounded.toBigInteger().toString());
   }
```

Proved equivalence with the landing tests rather than by inspection alone: `./mvnw -q
-Dtest=LandingFixturesTest,LandingControllerTest test` (exit 0) — `LandingFixturesTest`'s
`previewTilesArePreFormatted` asserts `Format.number(8410)` = `"8 410"` and
`Format.number(94200)` = `"94 200"` against the feeds implementation and still passes. Both
test classes (landing's and feeds') green in the same `./mvnw -q test`/`verify` run — see gate
table below.

**i18n/index.ts** — took HEAD verbatim (`git show 6e7a847...:...index.ts > ...index.ts`,
diffed byte-identical against the blob). It already carries the exact loader
common-journey-rules.md specifies, plus `warnHtmlMessage: false` set globally in the
`createI18n` call — the centralized fix I could not make myself in the original candidate
(restricted to "loader lines only"; I muted it locally in `landing.spec.ts` instead, see the
original report's "Two notable findings"). `landing.pl.ts`/`landing.en.ts` need no change: the
glob picks them up exactly as before.

**routes.ts** — one conflict, at the import block (both my `LandingView` import and feeds'
`FeedsView` import were inserted at the same line, right after `EmptyPageView`). Resolved to
keep both, alphabetized: `EmptyPageView`, `FeedsView`, `LandingView`, `LoginView`. The two
`component:` swaps (feeds' `feeds` route, mine on `home`) sit on non-adjacent lines and had
already auto-merged correctly before I even opened the file — verified by grep for conflict
markers (`<<<<<<<`/`=======`/`>>>>>>>`) returning nothing after the import-line fix.

No other conflicts encountered — nothing to stop and report on.

### Post-rebase state

```
$ git log --oneline 6e7a847c5bcda11149585f7007bf3f2f349b0394..HEAD
0e84986 feat: add public landing page
$ git merge-base --is-ancestor 6e7a847c5bcda11149585f7007bf3f2f349b0394 HEAD && echo ok
ok
```

Exactly my one landing commit on top of `6e7a847`, as required.

### Gate table (new candidate SHA `0e8498663ac0517c9c80a3baa69cea33d076f0a4`)

| # | Gate | Command | cwd | Exit | Evidence | Status |
|---|------|---------|-----|------|----------|--------|
| 1a | Backend focused | `./mvnw -q test` | `backend/` | 0 | console | PASS |
| 1b | Format equivalence proof | `./mvnw -q -Dtest=LandingFixturesTest,LandingControllerTest test` | `backend/` | 0 | `evidence/update-1/landing-tests.txt` | PASS |
| 2a | Backend integration | `./mvnw -q verify` (Testcontainers Postgres 18) | `backend/` | 0 | console | PASS |
| 1c | Frontend unit | `npm run test -- --run` | `frontend/` | 0 | console; 9 files / 51 tests | PASS |
| 2b | Frontend integration | `npm run test:integration -- --run` | `frontend/` | 0 | console; 5 files / 20 tests | PASS |
| 3a | ArchUnit | inside `./mvnw test` | `backend/` | 0 | same as 1a | PASS |
| 3b | ESLint | `npm run lint` | `frontend/` | 0 | console; 0 errors, 2 warnings (pre-existing, in feeds' `FeedCard.vue`, not mine) | PASS |
| 3c | vue-tsc | `npm run typecheck` | `frontend/` | 0 | console | PASS |
| 4a | Spotless | `./mvnw -q spotless:check` | `backend/` | 0 | console | PASS |
| 4b | Prettier | `npm run format:check` | `frontend/` | 0 | console | PASS |
| 4c | Vite build | `npm run build` | `frontend/` | 0 | dist 69.75 kB gzip JS | PASS |
| 5a | Contract+visual (landing) | `VERIFY_COMPOSE="docker compose -p kivvi-w-landing -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey landing --base https://localhost:19011 --out evidence/update-1/compare-landing` | repo root | 0 | `evidence/update-1/compare-landing/landing/report.md` | PASS — 0 regressions |
| 5b | Contract+visual (feeds, merged-tree guard) | same, `--journey feeds --out evidence/update-1/compare-feeds` | repo root | 0 | `evidence/update-1/compare-feeds/feeds/report.md` | PASS — 0 regressions |
| 6 | E2E | `E2E_BASE_URL=https://localhost:19011 npx playwright test public.spec.ts -g "landing"` | `tests/e2e/` | 0 | `evidence/update-1/e2e-landing.txt` | PASS — 3/3 |
| 7 | Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19011` | repo root | 0 | `evidence/update-1/performance.txt` — JS 69,006 B / 307,200 B, LCP 160 ms / 2000 ms, TTI 16.6 ms / 2500 ms | PASS |

Stack: compose project `kivvi-w-landing`, HTTP 19010 / HTTPS 19011, built fresh from the rebased tree.
Torn down (`down -v`) and the image (`kivvi-w-landing-api`) removed after the gate run.

### Final candidate SHA and clean worktree (post-rebase)

```
$ git rev-parse HEAD
0e8498663ac0517c9c80a3baa69cea33d076f0a4
$ git status --porcelain
(empty)
$ git branch --show-current
migration/wave-1/landing
$ git log --oneline 6e7a847c5bcda11149585f7007bf3f2f349b0394..HEAD
0e84986 feat: add public landing page
```

## Repair-1

Repair for the wave-1 verifier cohort: integration FAILed because `GET /api/v1/{locale}/landing`
had only `@WebMvcTest`-slice coverage, and architecture noted the "Intl only in `format.ts`" rule
had no enforcing lint (`no-restricted-imports` cannot catch a bare global reference — no `import`
statement is involved). Test-only change per `repair-1.md`.

### Identity guard (re-checked before starting)

`git rev-parse --show-toplevel` → `/home/muszkin/work/kivvi-click-wt/w1-landing` ✓;
`git branch --show-current` → `migration/wave-1/landing` ✓; `git status --porcelain` → clean ✓.

### Rebase onto the wave SHA

`git rebase 9427fdb1d92e4e606e67471638031d1bc0fb73d4` — **conflict-free**, exactly as the packet
predicted: my rebased candidate `0e8498663ac0517c9c80a3baa69cea33d076f0a4` was already an ancestor
of `9427fdb` (`git merge-base --is-ancestor 0e84986 9427fdb` confirmed this *before* rebasing), so
the rebase left the branch with **zero commits of its own** — `HEAD` landed exactly on `9427fdb`
(`feat: add ShedLock-backed scheduler heartbeat (wave-1)`), with my `feat: add public landing page`
commit already present in that history at `0e84986`. The repair commit below was created fresh on
top of `9427fdb`, never by amending.

### R1-A — `LandingApiIT`

Added `backend/src/test/java/click/kivvi/LandingApiIT.java`, modelled on `FeedsApiIT`
(`@Testcontainers` + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ServiceConnection`
Postgres 18 + `TestRestTemplate`, same package/style as `FeedsApiIT`/`ShellApiIT`). Four tests:

- `landingPayloadMatchesTheOracleThroughTheRealHttpLayer` (**B01**) — `GET /api/v1/pl/landing` →
  200; `features` size 6, `steps` size 3, `plans` size 2 with `plans[1]` = Pro/`featured=true`,
  `trustPoints` size 3, `previewTiles` size 4 containing `"8 410"` and `"94 200"` (the narrow
  no-break-space-grouped values — copied via literal string, then re-verified byte-for-byte with
  a Python check against U+202F, the same trap hit twice already in this slice's Format.java
  history), `previewSeries` non-empty.
- `englishRouteReturnsTheSamePolishCopyThroughTheRealHttpLayer` (**B22**) — `GET
  /api/v1/en/landing` → 200 with the *same Polish copy* as `/pl` (feature title, plan CTA) —
  deliberate, citing `journeys/landing/steps/2/texts.json` (the oracle's own English capture,
  whose feature/step/plan text stays Polish): the old stack's `LandingContent` never ran these
  arrays through the translator either.
- `unsupportedLocaleIsNotFoundThroughTheRealHttpLayer` (**B07**) — `GET /api/v1/de/landing` → 404.
- `demoRedirectsToDashboardThroughTheRealHttpLayer` (**B22**) — `GET /pl/demo` via
  `restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW)` (the same pattern
  `SessionRoundTripIT` already uses) → 302, `Location: /pl/dashboard`.

All 4 pass: `backend/target/failsafe-reports/click.kivvi.LandingApiIT.txt` — "Tests run: 4,
Failures: 0, Errors: 0, Skipped: 0".

### R1-B — Intl-formatting lint rule

Added to `frontend/eslint.config.js`, alongside the existing (now-proven-insufficient-alone)
`no-restricted-imports`:

- `no-restricted-globals`: bans the bare `Intl` identifier — catches `Intl.NumberFormat(...)`
  with no import needed, which `no-restricted-imports` structurally cannot see.
- `no-restricted-syntax`, three selectors: `MemberExpression[object.name='Intl']` (`Intl.`
  member access), `CallExpression[...][callee.property.name='toLocaleString']`
  (`.toLocaleString(`), `CallExpression[...][callee.property.name='toFixed']` (`.toFixed(`).
- All three carry the message `"numbers are formatted server-side (domain/Format.java); see
  rules-translated.md"`.
- Extended the existing `files: ["src/format.ts"]` override block to also turn off
  `no-restricted-globals`/`no-restricted-syntax` there (that module still doesn't exist — not
  created by this repair, per the packet's "test-only change" scope; the override is inert until
  some later slice adds the file).

**Proof — positive** (`evidence/repair-1-gates/eslint-positive-proof.txt`): `npm run lint` on the
real tree → exit 0, same 2 pre-existing warnings in feeds' `FeedCard.vue` as before this repair,
0 errors.

**Proof — negative** (`evidence/repair-1-gates/eslint-intl-negative-proof.txt`): added a throwaway
`frontend/src/throwaway-intl-check.ts` with three violations (`new Intl.NumberFormat(...)`,
`value.toLocaleString(...)`, `value.toFixed(2)`), ran `npm run lint` → **exit 1**, 4 errors (the
`Intl.NumberFormat` reference trips both `no-restricted-globals` *and*
`no-restricted-syntax`'s member-access selector; each of the other two calls trips its own
selector) — then deleted the file (`git status --porcelain` showed it as the only untracked
change, confirming a clean revert) before re-running the positive proof above.

### Gates on the new candidate SHA (`evidence/repair-1-gates/`)

| # | Gate | Command | cwd | Exit | Evidence | Status |
|---|------|---------|-----|------|----------|--------|
| 1 | Backend focused | `./mvnw -q test` | `backend/` | 0 | `mvnw-test.txt` | PASS (LandingApiIT correctly skipped by Surefire — `*IT.java` is Failsafe's job) |
| 2 | Backend integration | `./mvnw -q verify` (Testcontainers Postgres 18) | `backend/` | 0 | `mvnw-verify.txt`, `backend/target/failsafe-reports/click.kivvi.LandingApiIT.txt` | PASS — first run caught a Spotless formatting nit in the new file, fixed with `spotless:apply`, re-verified clean before the final run recorded here |
| 3 | Frontend unit | `npm run test -- --run` | `frontend/` | 0 | `npm-test.txt` — 9 files / 51 tests | PASS |
| 4 | Frontend integration | `npm run test:integration -- --run` | `frontend/` | 0 | `npm-test-integration.txt` — 5 files / 20 tests | PASS |
| 5 | Lint + typecheck + format:check + build | `npm run lint && npm run typecheck && npm run format:check && npm run build` | `frontend/` | 0 | `npm-lint-typecheck-format-build.txt` — 0 errors/2 pre-existing warnings, typecheck clean, Prettier clean, dist 69.75 kB gzip JS | PASS |

**compare.mjs / Playwright: skipped**, as authorized — this is a test-only repair (no
production/view code changed; `Format.java`, `LandingController`, `LandingView`, `LandingView.vue`
etc. are byte-identical to the already-verified `0e84986` candidate). The wave cohort's own
verifier run re-covers the contract/visual/E2E dimensions.

### New candidate SHA and clean worktree

```
$ git rev-parse HEAD
4f74907ec64cb55ed8b1654f0ebda0ffab0d3f8a
$ git status --porcelain
(empty)
$ git branch --show-current
migration/wave-1/landing
$ git log --oneline 9427fdb1d92e4e606e67471638031d1bc0fb73d4..HEAD
4f74907 test: real-HTTP landing coverage and lint-enforced Intl ban (repair-1)
```

Files changed: `backend/src/test/java/click/kivvi/LandingApiIT.java` (new, 4 tests),
`frontend/eslint.config.js` (2 new rule entries + extended `src/format.ts` override). No
production code touched; no compose stack brought up for this repair (not required — test-only
change, gates run against the built Maven/Vite artifacts directly).
