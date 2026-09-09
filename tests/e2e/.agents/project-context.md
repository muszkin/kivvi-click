<!-- BEGIN project-context-initializer:context -->
# Context: `tests/e2e/`

Source `fe9c3fe06b919302d322994438a8fbb177c8b2a0` (main), refreshed 2026-09-09. Index:
`../../context/map/INDEX.md`, manifest: `../../context/map/manifest.json`. Coverage: `own`;
`specs/` rolled-up; `node_modules/`, `test-results/`, `playwright-report/` excluded.

`tests/` itself has no `own` context — the old PHP suite that used to live directly under
`tests/Controller`/`tests/Tracking` was deleted by the migration's CON-1 cleanup
(`25329ac`); `tests/e2e/` is now the only thing under `tests/`, so it is rolled up here in the
manifest rather than given a separate parent context.

## Purpose

Playwright end-to-end suite against the running Docker stack — the real-surface acceptance
oracle for the panel, and the parity oracle the entire Symfony→Spring Boot/Vue migration was
verified against. Isolated npm project (`package.json`, `package-lock.json`, `@playwright/test`
`^1.56.0`) so the root frontend's own npm/Vite toolchain stays independently versioned.

**These spec files are unchanged since before the migration began.** `git diff --stat HEAD~10 --
tests/e2e/specs` over the full RR-1/CUT-1/CON-1 window shows zero changes; the last real edit was
`812db3f` (2026-08-26), two weeks before the migration started — confirmed by the CON-1 exit
verification (`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/
cutover/con1-e2e.md`). A spec change is itself meant to be a regression signal, not a routine
edit.

## Configuration

`playwright.config.ts`: `testDir ./specs`, fully parallel, retries 1 in CI, `baseURL` =
`E2E_BASE_URL` or `https://localhost:8543` (dev stack's documented port is 8443 — pass the env
explicitly), `ignoreHTTPSErrors`, trace/screenshot on failure, single project `chrome` using
system Google Chrome (`channel: "chrome"`), viewport 1440×900.

## Specs (rolled-up)

`automations`, `customers`, `dashboard` (KPIs, cardiogram redraw on theme change, live stream
first page, pause toggle), `editors` (email + popup editors, literal colours), `events` (30 rows,
filters, **POST /collect → SSE row appears**, replay not duplicated), `import` (4 steps, stepper,
upload advances), `lists` (campaigns, widgets preview, feeds), `navigation` (every sidebar entry,
only `.main-scroll` scrolls, sidebar/theme survive reload, locale switch keeps page, breadcrumb),
`public` (landing PL/EN, demo button, login), `settings` (every tab URL, highlighted snippet, DNS
state, failing webhook, notification matrix).

## Commands

`cd tests/e2e && npm install && E2E_BASE_URL=<url> npx playwright test` (headless Chrome).
`events.spec.ts`, `dashboard.spec.ts` and `navigation.spec.ts` share live/SSE state and should
run with `--workers=1`; run each spec file as its own invocation when doing this deliberately
(`navigation.spec.ts` is even run twice in the documented gate sequence for stability).

**Most recent run, against production** (2026-09-09T16:04-16:07Z, `E2E_BASE_URL=https://
kivvi.click`): 66 unique tests (80 executions counting `navigation` twice), all PASS, 0 failures,
0 retries — `../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/
cutover/con1-e2e.md`. Not re-run by this context refresh — see R12 in
`../../context/map/risks-and-unknowns.md`.

## Invariants

Suite is the behavioural oracle for any UI rewrite or stack migration going forward too
(capture/verify before changing broad UI behaviour). It is deliberately excluded from
`.github/workflows/build.yml` — no CI job runs it; someone must run it by hand (or via
`tools/migration-verify/`) after a change that could affect user-visible behaviour.

## Risks

Requires Chrome on host; needs a dev stack isolated from the running prod compose project
(`docker compose -p kivvi-dev`, not the bare project name — see R5 in
`../../context/map/risks-and-unknowns.md`); not run by this context refresh, though a
recent independent run against production is federated as evidence (R12).

## Evidence

`tests/e2e/playwright.config.ts`, `tests/e2e/specs/*.spec.ts`, `tests/e2e/package*.json`,
`../../README.md`,
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md`.
<!-- END project-context-initializer:context -->
