<!-- BEGIN project-context-initializer:context -->

# `frontend/` — project context

| Field           | Value                                                                                                                                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Path            | `frontend/`                                                                                                                                                                                                         |
| Scope           | Vue 3 SPA for kivvi-click, post-migration (single stack)                                                                                                                                                            |
| Source revision | `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main`                                                                                                                                                                |
| Refreshed       | 2026-09-09                                                                                                                                                                                                          |
| Coverage role   | `own` (rolled up: `src/components/{atoms,molecules,organisms,import,settings}`, `src/{layouts,views,router,stores,i18n,composables,lib,styles}`, `test/{unit,integration}` — structural leaves under this boundary) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.
Precedence: current user instructions, `CLAUDE.md`/`AGENTS.md`, code and runtime behaviour
outrank this file for anything stale.

## Purpose

Vue 3.5.42 single-page application (no SSR — owner decision) that reproduces every screen of the
original Twig panel pixel- and text-for-text against a JSON API served by `../backend/`.
History-mode routing, one view per page, Pinia for shell state, vue-i18n for PL(default)/EN. The
migration that built this SPA cut production over to it on 2026-09-09; this is now the **only**
frontend — the old Twig templates were deleted by the migration's CON-1 cleanup and no longer
exist. The unmodified Playwright suite in `../tests/e2e/specs/*.spec.ts` is still the acceptance
oracle for any further change here — it was re-run against production after the old-stack
removal and passed 66/66 (`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/
cutover/con1-e2e.md`).

## Structure (Observed)

```
src/
├── components/{atoms,molecules,organisms}/   design-system components
│   ├── import/                                import-wizard-only components
│   └── settings/                              settings-tab-only components
├── layouts/          AppLayout, AuthLayout, PublicLayout — chosen per route via meta.layout
├── views/             one *View.vue per page/journey
├── router/            index.ts, routes.ts, meta.d.ts, localeHref.ts, scrollRestoration.ts
├── stores/            shell.ts (Pinia) — nav, workspace, user, theme, sidebar
├── composables/       useIntents (data-action dispatcher), useEventStream, useEditorDrag
├── i18n/              index.ts (canonical loader), pl.ts, en.ts, messages/<journey>.{pl,en}.ts
├── lib/               shared helpers (format.ts etc.)
└── styles/            CSS — byte-identical to the original design system, never edited
test/
├── unit/              Vitest component/unit specs
└── integration/        Vitest + @vue/test-utils + router/pinia integration specs, one per view/journey
```

## Invariants (do not break)

- **CSS is byte-identical to the original design system.** Never edit CSS on a hunch; if a class
  looks missing, check the a11y/text evidence before assuming the CSS is wrong.
- **Whitespace text nodes between adjacent atoms:** the original Twig compiled includes ended
  with a trailing newline, so two adjacent components inside a plain (non-flex/gap) container
  can carry a visible whitespace text node between them that Vue's default `whitespace:
'condense'` would otherwise strip. Where the parent isn't flex/gap, a literal `{{ " " }}`
  between adjacent atom/molecule components may be load-bearing — a whole-row visual diff with
  identical text is a missing-whitespace bug until proven otherwise.
- **Navigation is always a real document request**, never `router.push`/`router.replace` —
  `useIntents`'s `navigate`, `go-customer`, `go-automation`, `go-email`, `go-page` intents all
  set `window.location.href`. `vue-router`'s `routes.ts` only matches whatever URL the browser
  already navigated to. The Playwright suite still asserts this.
- **i18n loader is canonical — do not edit `src/i18n/index.ts`, `pl.ts` or `en.ts` directly.**
  Each journey owns `src/i18n/messages/<journey>.pl.ts` / `<journey>.en.ts` (default-exported
  plain objects, top-level key = journey name, never colliding with an existing key);
  `index.ts`'s `import.meta.glob("./messages/*.{pl,en}.ts", { eager: true })` merges them into
  copies of the base catalogues. `warnHtmlMessage: false` is deliberate: several catalogue
  strings carry developer-authored markup, bound only via `v-html` (see `eslint.config.js`'s
  narrow `no-v-html` exemptions) — do not silence that warning any other way, and do not add a
  new `v-html` binding without a matching eslint exemption.
- **Numbers are never formatted client-side.** `domain/Format.java` on the backend is the only
  formatter (PL number/money/percent/`timeAgo`); `eslint.config.js` enforces this with
  `no-restricted-imports`/`no-restricted-globals` on `Intl` and `no-restricted-syntax` blocking
  `Intl.*`, `.toLocaleString()`, `.toFixed()` anywhere under `src/**/*.{ts,vue}`.
- **The `/accounts/` Mercure topic literal is server-side only** — read the topic from the API
  payload; `eslint.config.js`'s `no-restricted-syntax` blocks any string/template literal
  matching `/accounts/` in application code (governed exemptions exist only for the three
  components that legitimately reuse `.event-row` styling — see next point).
- **`.event-row` class is governed:** only `components/molecules/EventRow.vue`,
  `components/organisms/ListCard.vue` (dashboard's recent-customers/top-automations partials)
  and `components/import/RecentImports.vue` (recent-imports list) may render it —
  `eslint.config.js`'s `vue/no-restricted-class` blocks it everywhere else, with those three
  files carrying an explicit, documented override. Do not bypass the rule with a computed class
  string; use the literal, as all three files already do.
- **Preference POSTs use `keepalive: true` and are `await`ed before any DOM/navigation change**
  (`stores/shell.ts`): a plain `fetch` is abortable on unload, so a theme/sidebar toggle followed
  immediately by a reload could lose the write; `keepalive` fixes that with no change to
  method/headers/body.
- **Session lock parity note:** the backend serializes concurrent requests to the same session
  (`../backend/.agents/project-context.md`'s "Session-lock parity"; formalized in
  `../docs/adr/0001-serialize-requests-per-session-like-php.md`). A frontend action that fires a
  preference POST and then immediately reloads relies on that backend lock, not on anything in
  this directory — do not "fix" a perceived race here by adding client-side delays.
- **Scroll restoration is `scrollBehavior` in `router/index.ts` + `router/scrollRestoration.ts`**
  (formalized in `../docs/adr/0002-spa-reload-scroll-restoration-via-router-scrollbehavior.md`),
  not a hand-rolled `pagehide` listener — an earlier approach broke back/forward and was
  reverted. `history.state.scroll` written on `pagehide` does not survive an actual reload in the
  verified Chromium build, so `scrollRestoration.ts` adds a narrow, same-href-only
  `sessionStorage` fallback gated on `isReloadOfAnAlreadyVisitedEntry()` (a `history.state`
  marker, not the Performance Timing API — the migration's own verifier tooling freezes the page
  clock, under which `performance.getEntriesByType("navigation")` is permanently empty). Never
  reintroduce a Navigation-Timing-based reload detector; never bypass
  `waitForStableLayout()`'s frame-count settle before applying a restored scroll position.
- **`meta.defaultParams`** (`router/meta.d.ts`, consumed by `router/localeHref.ts`): a route
  parameter equal to its declared default is omitted from a generated URL — mirrors the
  original's own URL generator dropping a route param equal to its default (e.g. settings'
  `tab: 'account'` default means the PL/EN toggle at `/pl/settings/account` targets
  `/en/settings`, not `/en/settings/account`). A new route with an omittable default must add its
  own `meta.defaultParams` entry — `localeHref.ts` itself must stay journey-agnostic.

## Commands (Observed, `package.json`)

- `npm run dev` — Vite dev server (fast iteration only; every gate still runs against the
  compose-built image)
- `npm run test` → `vitest run test/unit` — unit gate
- `npm run test:integration` → `vitest run test/integration` — integration gate (router + Pinia +
  component composition)
- `npm run lint` → `eslint .`
- `npm run typecheck` → `vue-tsc --noEmit`
- `npm run format` / `npm run format:check` → Prettier
- `npm run build` → `vue-tsc --noEmit && vite build`
- CI: `.github/workflows/build.yml` job `frontend` — `npm ci`, typecheck, lint, format:check,
  `npm run test -- --run`, `npm run test:integration -- --run`, `npm run build`

## Tests (Observed)

- `test/unit/*.spec.ts` (28 files): component-level Vitest specs (e.g. `AppShell.spec.ts`,
  `Topbar.spec.ts` — pin structural invariants like "exactly one `.main-scroll` element, the
  router view renders inside it and nowhere else").
- `test/integration/*.spec.ts` (21 files): one per view/journey plus `ShellNavigation.spec.ts`,
  `LocaleToggle.spec.ts`, `ScrollRestoration.spec.ts`, `shellStore.spec.ts`,
  `CustomerDetailSidebarSection.spec.ts` — mount real components with router + Pinia.
- Real-surface acceptance: `../tests/e2e/specs/*.spec.ts` — last run against **production**
  2026-09-09T16:04-16:07Z, 66/66 PASS (`../context/implementation-runs/
2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md`); `events.spec.ts` and any spec
  asserting exact row counts on the shared Mercure topic runs with `--workers=1`.
- Last known-good unit/integration result (final gates, pre-cutover): 169 unit / 152 integration.
  Not re-run by this context refresh — see R12 in `../context/map/risks-and-unknowns.md`.

## Deployment (current, single stack)

No fixed port of its own — served as static assets baked into the backend jar
(`../backend/Dockerfile` stage 1 builds `frontend/dist` and copies it into
`backend/src/main/resources/static/`), reached through the `mercure` edge container in both dev
(`docker compose -p kivvi-dev`) and production (`docker compose -p kivvi-click`, live on this
host since 2026-09-09T14:18:42Z).

## Evidence paths

`frontend/package.json`, `frontend/eslint.config.js`, `frontend/src/**`, `frontend/test/**`,
`frontend/README.md`, `../backend/.agents/project-context.md` (API contract this consumes),
`../docs/adr/0002-spa-reload-scroll-restoration-via-router-scrollbehavior.md`,
`../context/plans/2026-09-08-symfony-to-spring-vue-migration.md`,
`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/{closeout.md,
common-journey-rules.md,cutover/con1-e2e.md}`,
`../context/migration-oracle/symfony-to-spring-vue/`.

<!-- END project-context-initializer:context -->
