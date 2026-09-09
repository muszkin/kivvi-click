<!-- BEGIN project-context-initializer:context -->

# `frontend/` — project context

| Field           | Value                                                                                                                                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Path            | `frontend/`                                                                                                                                                                                                         |
| Scope           | Vue 3 SPA for the Symfony→Spring Boot + Vue migration (`migration/spring-vue`)                                                                                                                                      |
| Source revision | `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue` (worktree `/home/muszkin/work/kivvi-click-wt/integration`)                                                                                     |
| Refreshed       | 2026-09-09                                                                                                                                                                                                          |
| Coverage role   | `own` (rolled up: `src/components/{atoms,molecules,organisms,import,settings}`, `src/{layouts,views,router,stores,i18n,composables,lib,styles}`, `test/{unit,integration}` — structural leaves under this boundary) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.
Precedence: current user instructions, `CLAUDE.md`/`AGENTS.md` (old-stack "no frontend framework"
rule, stale for this directory — see R19 in `../../context/map/risks-and-unknowns.md`), the
migration plan, code and runtime behaviour outrank this file for anything stale.

## Purpose

Vue 3.5.42 single-page application (no SSR — owner decision, plan §"Global implementation
constraints") that reproduces every screen of the old Twig panel (`../../templates/pages/**`) pixel-
and text-for-text against a JSON API served by `../backend/`. History-mode routing, one view per
old-stack page, Pinia for shell state, vue-i18n for PL(default)/EN. Existing golden test surface:
the unmodified Playwright suite in `../../tests/e2e/specs/*.spec.ts` must pass against this SPA
with only `E2E_BASE_URL` changed — the plan calls the e2e suite the acceptance oracle for every
wave, and a spec change is itself a regression signal (K8 kill criterion).

## Structure (Observed)

```
src/
├── components/{atoms,molecules,organisms}/   design-system components, ported 1:1 from templates/components/**
│   ├── import/                                import-wizard-only components
│   └── settings/                              settings-tab-only components
├── layouts/          AppLayout, AuthLayout, PublicLayout — chosen per route via meta.layout
├── views/             one *View.vue per old-stack page/journey
├── router/            index.ts, routes.ts, meta.d.ts, localeHref.ts, scrollRestoration.ts
├── stores/            shell.ts (Pinia) — nav, workspace, user, theme, sidebar
├── composables/       useIntents (data-action dispatcher), useEventStream, useEditorDrag
├── i18n/              index.ts (canonical loader), pl.ts, en.ts, messages/<journey>.{pl,en}.ts
├── lib/               shared helpers (format.ts etc.)
└── styles/            CSS — byte-identical to ../../assets/styles/*.css (never edited)
test/
├── unit/              Vitest component/unit specs
└── integration/        Vitest + @vue/test-utils + router/pinia integration specs, one per view/journey
```

## Invariants (do not break — sourced from the plan and `common-journey-rules.md`)

- **CSS is byte-identical to `../../assets/styles/*.css`.** Never edit CSS here; if a class looks
  missing, the Twig template was read wrong, not the CSS.
- **Markup parity method:** components are built from the oracle's rendered `a11y.json`/`texts.json`
  and Twig _output_, not from Twig source — same elements, classes, `data-*` attributes, ids, text
  nodes.
- **Whitespace text nodes between adjacent atoms:** Twig's compiled includes end with a trailing
  newline, so two adjacent `include()`s inside a plain (non-flex/gap) container render a visible
  whitespace text node between them. Vue's default `whitespace: 'condense'` strips that. Where the
  Twig parent isn't flex/gap, insert `{{ " " }}` between adjacent atom/molecule components — a
  whole-row visual diff with identical text is a missing-whitespace bug until proven otherwise
  (diff DOM text nodes against the old stack, not just screenshots).
- **Navigation is always a real document request**, never `router.push`/`router.replace` — the
  oracle records a full `GET` for every sidebar/topbar/row transition. `useIntents`'s `navigate`,
  `go-customer`, `go-automation`, `go-email`, `go-page` intents all set `window.location.href`.
  `vue-router`'s `routes.ts` only matches whatever URL the browser already navigated to.
- **i18n loader is canonical after wave-1 — do not edit `src/i18n/index.ts`, `pl.ts` or `en.ts`
  directly.** Each journey owns `src/i18n/messages/<journey>.pl.ts` / `<journey>.en.ts` (default-
  exported plain objects, top-level key = journey name, never colliding with an existing key);
  `index.ts`'s `import.meta.glob("./messages/*.{pl,en}.ts", { eager: true })` merges them into
  copies of the base catalogues. `warnHtmlMessage: false` is deliberate (authorized 2026-09-08):
  several catalogue strings carry developer-authored markup ported from Twig `|raw`, bound only via
  `v-html` (see `eslint.config.js`'s narrow `no-v-html` exemptions) — do not silence that warning
  any other way, and do not add a new `v-html` binding without a matching eslint exemption.
- **Numbers are never formatted client-side.** `Format`/`domain/Format.java` on the backend is the
  only formatter (PL number/money/percent/`timeAgo`); `eslint.config.js` enforces this with
  `no-restricted-imports`/`no-restricted-globals` on `Intl` and `no-restricted-syntax` blocking
  `Intl.*`, `.toLocaleString()`, `.toFixed()` anywhere under `src/**/*.{ts,vue}`.
- **The `/accounts/` Mercure topic literal is server-side only** — read the topic from the API
  payload; `eslint.config.js`'s `no-restricted-syntax` blocks any string/template literal matching
  `/accounts/` in application code (governed exemptions exist only for the three components that
  legitimately reuse `.event-row` styling — see next point).
- **`.event-row` class is governed:** only `components/molecules/EventRow.vue`,
  `components/organisms/ListCard.vue` (dashboard's recent-customers/top-automations partials) and
  `components/import/RecentImports.vue` (recent-imports list) may render it — `eslint.config.js`'s
  `vue/no-restricted-class` blocks it everywhere else, with those three files carrying an explicit,
  documented override. Do not bypass the rule with a computed class string; use the literal, as all
  three files already do.
- **Preference POSTs use `keepalive: true` and are `await`ed before any DOM/navigation change**
  (`stores/shell.ts`, wave-3 repair `w3-shell-preferences`): a plain `fetch` is abortable on unload,
  so a theme/sidebar toggle followed immediately by a reload could lose the write; `keepalive`
  fixes that with no change to method/headers/body.
- **Session lock parity note:** the backend serializes concurrent requests to the same session
  (`../backend/.agents/project-context.md`'s "Session-lock parity"). A frontend action that fires a
  preference POST and then immediately reloads relies on that backend lock, not on anything in this
  directory — do not "fix" a perceived race here by adding client-side delays.
- **Scroll restoration is `scrollBehavior` in `router/index.ts` + `router/scrollRestoration.ts`,
  not a hand-rolled `pagehide` listener** (repair-1's original approach was reverted — it broke
  back/forward). `history.state.scroll` written on `pagehide` does not survive an actual reload in
  the verified Chromium build, so `scrollRestoration.ts` adds a narrow, same-href-only
  `sessionStorage` fallback gated on `isReloadOfAnAlreadyVisitedEntry()` (a `history.state` marker,
  not the Performance Timing API — `compare.mjs`/`capture.mjs` freeze the page clock, under which
  `performance.getEntriesByType("navigation")` is permanently empty). Never reintroduce a
  Navigation-Timing-based reload detector; never bypass `waitForStableLayout()`'s frame-count
  settle before applying a restored scroll position.
- **`meta.defaultParams`** (`router/meta.d.ts`, consumed by `router/localeHref.ts`): a route
  parameter equal to its declared default is omitted from a generated URL — mirrors Symfony's own
  URL generator dropping a route param equal to its `defaults` entry (e.g. settings' `tab: 'account'`
  default means the PL/EN toggle at `/pl/settings/account` targets `/en/settings`, not
  `/en/settings/account`). A new route with an omittable default must add its own
  `meta.defaultParams` entry — `localeHref.ts` itself must stay journey-agnostic.

## Commands (Observed, `package.json`, `common-journey-rules.md`)

- `npm run dev` — Vite dev server (fast iteration only; every gate still runs against the compose-built image)
- `npm run test` → `vitest run test/unit` — unit gate
- `npm run test:integration` → `vitest run test/integration` — integration gate (router + Pinia + component composition)
- `npm run lint` → `eslint .`
- `npm run typecheck` → `vue-tsc --noEmit`
- `npm run format` / `npm run format:check` → Prettier
- `npm run build` → `vue-tsc --noEmit && vite build`
- CI: `.github/workflows/next-build.yml` job `frontend` — `npm ci`, typecheck, lint, format:check, `npm run test -- --run`, `npm run test:integration -- --run`, `npm run build`

## Tests (Observed)

- `test/unit/*.spec.ts` (28 files): component-level Vitest specs (e.g. `AppShell.spec.ts`,
  `Topbar.spec.ts` — pin structural invariants like "exactly one `.main-scroll` element, the router
  view renders inside it and nowhere else").
- `test/integration/*.spec.ts` (21 files): one per view/journey plus `ShellNavigation.spec.ts`,
  `LocaleToggle.spec.ts`, `ScrollRestoration.spec.ts`, `shellStore.spec.ts`,
  `CustomerDetailSidebarSection.spec.ts` — mount real components with router + Pinia.
- Tests are named after `behaviours.json` ids (`describe("Bnn …")`) — every behaviour a journey
  claims needs at least one test that fails if the behaviour regresses.
- Real-surface acceptance: `../../tests/e2e/specs/*.spec.ts` (unchanged, `E2E_BASE_URL` pointed at
  this stack); `events.spec.ts` and any spec asserting exact row counts on the shared Mercure topic
  runs with `--workers=1`.

## Ports / leases / hazards

- No fixed port of its own — served as static assets baked into the backend jar
  (`../backend/Dockerfile` stage 1 builds `frontend/dist` and copies it into
  `backend/src/main/resources/static/`) in the compose image, reached through the `mercure` edge.
- Per-journey-worker isolation during the migration run: `node_modules` per worktree, Vite cache in
  worktree, Playwright browser context per run (plan §"Worktree resource lease"). `~/.npm` cache
  grows ~3 GB per wave of `npm ci` runs — clean it before every verification cohort
  (`common-journey-rules.md` "Disk"); see R21 in `../../context/map/risks-and-unknowns.md` for host
  disk/CPU contention from unrelated containers on this host.
- The old stack (`../../templates/`, `../../assets/`, `../../tests/e2e/specs/`) is read-only from
  here — a Playwright spec change is itself a regression signal (K8), not a fix.

## Evidence paths

`frontend/package.json`, `frontend/eslint.config.js`, `frontend/src/**`, `frontend/test/**`,
`frontend/README.md`, `../backend/.agents/project-context.md` (API contract this consumes),
`../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`../../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/common-journey-rules.md`,
`../../context/migration-oracle/symfony-to-spring-vue/`.

<!-- END project-context-initializer:context -->
