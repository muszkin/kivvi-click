<!-- BEGIN project-context-initializer:context -->
# Context: `tests/e2e/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `specs/` rolled-up; `node_modules/` excluded.

## Purpose
Playwright end-to-end suite against the running Docker stack — the real-surface oracle for the panel. Isolated npm project (`package.json`, `package-lock.json`, `@playwright/test` 1.62.1 locked) so the root stays on Yarn PnP.

## Configuration
`playwright.config.ts`: `testDir ./specs`, fully parallel, retries 1 in CI, `baseURL` = `E2E_BASE_URL` or `https://localhost:8543` (README dev port is 8443 — pass the env), `ignoreHTTPSErrors`, trace/screenshot on failure, single project `chrome` using system Google Chrome (`channel: "chrome"`), viewport 1440×900.

## Specs (rolled-up)
`automations`, `customers`, `dashboard` (KPIs, cardiogram redraw on theme change, live stream first page, pause toggle), `editors` (email + popup editors, literal colours), `events` (30 rows, filters, **POST /collect → SSE row appears**, replay not duplicated), `import` (4 steps, stepper, upload advances), `lists` (campaigns, widgets preview, feeds), `navigation` (every sidebar entry, only `.main-scroll` scrolls, sidebar/theme survive reload, locale switch keeps page, breadcrumb), `public` (landing PL/EN, demo button, login), `settings` (every tab URL, highlighted snippet, DNS state, failing webhook, notification matrix).

## Commands
`cd tests/e2e && npm install && npx playwright test` (headless) — documented, not-run here. Global rule: e2e must run locally headless before finishing a task.

## Invariants
Suite is the behavioural oracle for any UI rewrite or stack migration (capture before changing).

## Risks
Requires Chrome on host and a dev stack isolated from the prod compose project (R5); not run in this initialization (R12).

## Evidence
`tests/e2e/playwright.config.ts`, `tests/e2e/specs/*.spec.ts`, `tests/e2e/package*.json`, `README.md`.
<!-- END project-context-initializer:context -->
