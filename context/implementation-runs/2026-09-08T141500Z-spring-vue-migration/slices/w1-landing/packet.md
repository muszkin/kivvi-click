# Slice packet w1-landing — wave-1, journey J1 "landing"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w1-landing · branch `migration/wave-1/landing` · parent SHA 887af4543c5a51255735f1ddfc0d8860d11551c0 (feature HEAD after wave-0). **Lease:** compose project `kivvi-w-landing`, HTTP_PORT=19010, HTTPS_PORT=19011. **Model:** sonnet, effort HIGH.

**Capability:** A visitor can read the public landing page in Polish or English and enter the panel demo.
**Oracle:** journeys/landing/steps/1..5 (`/pl`, `/en`, `/` (reproduce the recorded status/redirect of step 3 exactly — read step.json/http.jsonl), `/pl` again, click `.hero-cta a` with text "demo" → `/pl/dashboard`). Behaviours: B22 (landing part), B01 (landing row).
**Old-stack sources:** `src/Controller/LandingController.php`, `src/Panel/Content/LandingContent.php`, `templates/pages/landing.html.twig`, `templates/pages/landing/preview-chart.html.twig`, `templates/layout/public.html.twig`, the components they include (`components/atoms/sparkline`, `wordmark`, `logo`, `button`, molecules `kpi-tile`?, check the template), `translations/*.yaml` landing keys, `tests/e2e/specs/public.spec.ts` (landing tests).

## In scope
- Backend: `GET /api/v1/{locale}/landing` → `{ features, steps, plans, trustPoints, previewTiles, previewSeries }` exactly as `LandingContent` returns them (translated where the Twig used `|trans`); `LandingController` already handles `/demo` → 302 dashboard (verify against oracle step 5 http.jsonl; adjust only if it differs).
- Frontend: `LandingView.vue` under `PublicLayout` reproducing `pages/landing.html.twig` DOM: `.hero h1` (PL "Widzisz…", EN "See…"), `.hero-cta a` ×2, `.hero-preview .kpi` ×4, `.hero-preview svg path` (server-side SVG preview series → render the same path data from `previewSeries`), `#features .feat` ×6, `#how .feat` ×3, `.price-card` ×2 with `.price-card.pro .tier` = "Pro"; owned components: `Feat`, `PriceCard`, `HeroPreview`/`SparklineSvg`, any landing-only molecule. Route `home` in routes.ts → `LandingView`.
- i18n: `src/i18n/messages/landing.{pl,en}.ts`.
- Tests: Vitest snapshot/assert on section counts and texts (B22), JUnit on the view payload; ArchUnit unchanged.

## Out of scope
Dashboard body (DEV-11 masks it on step 5's landing page? No — step 5 lands on the dashboard: apply the same DEV-11 mask rule as login steps 4/8 by adding `landing: [5]` to DEV-11 in `tools/migration-verify/deviations.json`), any panel page, feeds, heartbeat.

## Deviations in scope
DEV-4, DEV-11 (step 5).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19011 npx playwright test public.spec.ts -g "landing"`

## Parallel-safety obligations
You share the wave with `feeds` and `scheduler-heartbeat`. Touch only: `backend/src/main/java/click/kivvi/{web/LandingController.java (existing),web/dto/Landing*.java,application/LandingViewService.java,fixtures/LandingFixtures.java,domain/* new types only}`, `backend/src/test/**/landing*`, `frontend/src/views/LandingView.vue`, `frontend/src/components/**/{Feat,PriceCard,HeroPreview,Sparkline*}.vue`, `frontend/src/i18n/messages/landing.*.ts`, `frontend/src/i18n/index.ts` (loader lines only, exact text from common rules), `frontend/src/router/routes.ts` (your one `component:` line + import), `frontend/test/**/landing*`, `tools/migration-verify/deviations.json` (DEV-11 row only), your report/evidence.
