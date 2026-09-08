# Slice packet w1-feeds — wave-1, journey J2 "feeds"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w1-feeds · branch `migration/wave-1/feeds` · parent SHA 6a53642c9c5ec9c256625854e6670f035e0292be (valid only once wave-0 reaches WAVE_INTEGRATED on this SHA). **Lease:** compose project `kivvi-w-feeds`, HTTP_PORT=19020, HTTPS_PORT=19021. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can see product feed sources, sync state and the price-matching diagnostic.
**Oracle:** journeys/feeds/steps/1 (`/pl/feeds`). Behaviours: B30, B01 (feeds row).
**Old-stack sources:** `src/Controller/ProductFeedController.php`, `src/Panel/Content/ProductFeedCatalog.php`, `templates/pages/feeds.html.twig`, `templates/pages/feeds/matching.html.twig`, organisms `kpi-grid`, `feed-card`, `page-head` (exists), molecules `kpi-tile`, `card`, `bar`(atom), `chip` (exists), `dot`, `sparkline` (atom), `callout` (exists); `tests/e2e/specs/lists.spec.ts` ("product feeds").

## In scope
- Backend: `GET /api/v1/{locale}/feeds` → `{ kpis, sources, feeds, coverage, fallbackRules, mismatched }` exactly as the controller passes them (`ProductFeedCatalog` port as `FeedsFixtures`; numbers pre-formatted via the ported `Format`).
- Frontend: `FeedsView.vue` reproducing `pages/feeds.html.twig`: `.feed-source` ×4, `.feed-card` ×4 with one `.feed-card__err` containing "HTTP 503", `.feed-card .chip.info .dot.live` ×1, the matching card "Dopasowanie cen do zdarzeń" with `.bar-row` ×4, texts `event.product_id`, `feed.id`, `[data-action=show-mismatched]` containing "142"; page title "Feedy produktów". Owned components (first owner — later journeys consume them): `KpiGrid`, `KpiTile`, `Sparkline` (atom, SVG), `Card`, `FeedCard`, `Bar`/`BarRow`, `Dot`, `ListCard` is NOT yours (dashboard). Route `feeds` → `FeedsView`.
- i18n: `src/i18n/messages/feeds.{pl,en}.ts`.
- Tests: Vitest for `KpiGrid`/`FeedCard` counts and the 503 error rendering (B30); JUnit for the payload.

## Out of scope
Any other page; editing shared shell components; CSS.

## Deviations in scope
DEV-4.

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19021 npx playwright test lists.spec.ts -g "product feeds"` and `navigation.spec.ts -g "feeds"` if such a test name matches (check; otherwise skip and note).

## Parallel-safety obligations
Shared wave with `landing` and `scheduler-heartbeat`. Touch only: `backend/src/main/java/click/kivvi/{web/FeedsController.java,web/dto/Feeds*.java,application/FeedsViewService.java,fixtures/FeedsFixtures.java,domain/Format.java (create only if absent — if landing also needs it, both create the identical port from src/Panel/Format.php; keep the class name `Format` and method names number/money/percent/initials/timeAgo)}`, `backend/src/test/**/feeds*`, `frontend/src/views/FeedsView.vue`, `frontend/src/components/**/{KpiGrid,KpiTile,Sparkline,Card,FeedCard,Bar,BarRow,Dot}.vue`, `frontend/src/i18n/messages/feeds.*.ts`, `frontend/src/i18n/index.ts` (loader lines only), `frontend/src/router/routes.ts` (your one `component:` line + import), `frontend/test/**/feeds*`, your report/evidence.
