<!-- BEGIN project-context-initializer:context -->
# Context: `templates/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; all subdirectories rolled-up.

## Purpose
Server-rendered Twig: design system (atoms → molecules → organisms), layouts, page templates, storybook chrome.

## Structure (rolled-up)
- `layout/base.html.twig` — real document root: `<html lang data-theme>`, Google Fonts (Geist, Geist Mono, Instrument Serif), `styles/app.css`, importmap `app`. `layout/app.html.twig` — authenticated shell (`.app[data-sidebar]`, sidebar + topbar organisms, `.main-scroll` is the scroll container, not window). `layout/auth.html.twig`, `layout/public.html.twig`.
- `base.html.twig` — legacy Symfony recipe base with jsDelivr hot-reload scripts; unused by pages (R15).
- `components/atoms` (13), `components/molecules` (27), `components/organisms` (18) — every component is included with explicit params (`with_context=false` convention per memory), documented header comment listing params.
- `pages/*.html.twig` + subfolders (`automations`, `customers`, `dashboard`, `emails`, `feeds`, `import`, `landing`, `popups`, `settings` (30 partials), `widgets`) — one template per controller action, partials per section.
- `storybook/{index,shell,story,frame}.html.twig` — story browser and isolated iframe frame.

## Contracts
- Interaction is declarative: `data-action`/`data-payload` intents and `data-controller` mounts consumed by `assets/app.ts`. Never inline handlers.
- Twig globals: `panel` (PanelContext), `icons`; filters `kivvi_highlight`; `app.request.locale`.
- `components/molecules/event-row.html.twig` is also rendered server-side by `Tracking\\EventIngestion` and pushed over Mercure — its params are a cross-boundary contract.
- `organisms/event-stream.html.twig` exposes `data-event-stream-topic`, `data-event-stream-hub`, `data-stream-state`, `data-paused`.
- Test selectors relied on by PHPUnit/e2e: `.page-title`, `.wiz-title`, `.card-title`, `.profile-name`, `.auth-form h1`, `.nav-item[data-route]`, `.sb-foot`, `.app[data-sidebar]`, `.main-scroll`, `.callout`.

## Commands
Storybook at `/_storybook` (dev/test). Tests: `composer test` (strict variables), e2e suite.

## Invariants
No JS/CSS frameworks; PL default strings; email document keeps literal colours (e2e "never render it dark"); one place per markup fragment.

## Risks
Stack migration would replace this layer entirely (R1); Google Fonts external dependency; PL strings hard-coded (R10).

## Evidence
`templates/**` (layout files read in full, components/pages indexed), `tests/Controller/PanelPagesTest.php`, `tests/e2e/specs/*.ts`.
<!-- END project-context-initializer:context -->
