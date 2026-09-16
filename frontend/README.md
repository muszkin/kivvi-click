# kivvi-click — frontend

Vue 3.5 SPA (no SSR), Vite build, history-mode vue-router, Pinia, vue-i18n (PL default, EN
toggle). Ported design system (atoms/molecules/organisms) and the four CSS files copied
byte-for-byte from the old stack's `assets/styles/`. Served by the Spring Boot API in
`../backend/` from its own classpath — see that module's README for how the two combine
into one jar.

## Typefaces

Geist, Geist Mono and Instrument Serif are **self-hosted** (PIO-118): the `.woff2` files live in
`src/assets/fonts/` and the `@font-face` declarations that point at them are in
`src/assets/fonts/fonts.css`, imported from `main.ts`. They used to come from Google's CDN, which
disclosed every visitor's IP address to Google on every page view — a transfer outside the EEA
that the privacy policy had to disclose. It no longer happens, and
`src/i18n/messages/privacy.{pl,en}.ts` now states plainly that nothing leaves the EEA.

Two rules follow from that, and both are enforced by tests rather than by review:

- **Nothing the public pages load may come from another origin.** `test/unit/fonts.spec.ts`
  fails if `index.html` gains any absolute URL; `../tests/e2e/specs/public.spec.ts` fails if
  loading `/pl` or `/en` issues a single request off-host. Adding a hosted font, script, or
  analytics pixel means amending the privacy policy in the same change.
- **Both the `latin` and `latin-ext` subsets ship for every face.** `ó` comes from `latin`, and
  `ą ć ę ł ń ś ź ż` from `latin-ext`; dropping either leaves English pages perfect and renders
  half of every Polish word in the fallback face. Both test files above check this explicitly.

All three families are under the SIL Open Font License 1.1, which permits self-hosting on
condition the licence travels with the files: `src/assets/fonts/OFL-*.txt`.

## Build

```sh
npm ci
npm run build   # vue-tsc --noEmit && vite build -> dist/
```

## Develop

```sh
npm run dev
```

Talks to whatever API the Vite dev server proxies to in `vite.config.ts`; for the full
authenticated flow (sessions, preferences) run the whole stack through
`compose.yaml` instead.

## Test

- `npm run test` — Vitest unit tests (`test/unit`): pure components, the route table, the
  i18n catalogues.
- `npm run test:integration` — Vitest integration tests (`test/integration`): components
  mounted with real router/i18n/pinia plugins (`LoginView`, `Sidebar`, the shell store).
- `npm run typecheck` — `vue-tsc --noEmit`.
- `npm run lint` — ESLint (Vue + TypeScript rules, plus the `format.ts`-only
  `Intl.NumberFormat` boundary rule from `architecture/rules-translated.md`).
- `npm run format` — Prettier, matching the repository's root `.editorconfig` (4-space
  indent) since no `.prettierrc` is checked in, mirroring the old stack's own convention.

**Fail-on-warning test policy** (the frontend analogue of the backend's PHPUnit-style
`failOnWarning`, restricted to first-party code): `test/setup.ts` (wired in via
`vite.config.ts`'s `test.setupFiles`) fails a test if `console.warn`/`console.error` is
called from a stack frame under `src/**` — a third-party library's own diagnostic output for
something a test deliberately triggers (e.g. vue-router logging "no match found" for the
B07 unsupported-locale-prefix test) is not flagged, only first-party code is. Any Vue
runtime warning (`app.config.warnHandler`, wired globally through `@vue/test-utils`'
`config.global.config`) fails a test unconditionally, regardless of which file the warning
traces back to. Unhandled errors/rejections already fail a test on their own as long as
`test.dangerouslyIgnoreUnhandledErrors` stays `false` in `vite.config.ts`.

## Structure

- `src/components/{atoms,molecules,organisms}` — the ported design system.
- `src/layouts/{PublicLayout,AuthLayout,AppLayout}.vue` — mirror `templates/layout/*.twig`.
- `src/views/*.vue` — one view per route in `src/router/routes.ts`; the wave-0 `EmptyPageView`
  placeholder every not-yet-built route mounted around `AppLayout` (DEV-11) was removed once the
  last journey (wave-4) replaced it with a real view.
- `src/router/routes.ts` — the full panel/public route table. Navigation between routes is
  always a real document request (`useIntents`' `navigate` intent, plain `<a>` hrefs), never
  `router.push` — matches every transition the oracle recorded.
- `src/stores/shell.ts` — Pinia store backing the shell chrome, fetched from
  `GET /api/v1/{locale}/shell`.
- `src/i18n/{pl,en}.ts` — message catalogues ported from `translations/messages.*.yaml` and
  the inline Polish strings in the old templates.
