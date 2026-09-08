<!-- BEGIN project-context-initializer:context -->
# Context: `assets/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `controllers/`, `js/`, `styles/` rolled-up.

## Purpose
Vanilla TypeScript behaviour and pure CSS design tokens/components, delivered through Symfony AssetMapper (importmap entry `app` → `assets/app.ts`, compiled by `typescript:build` into `var/typescript`).

## Files
- `app.ts` — single delegated click listener: `data-action` → handler registry (`on(action, handler)`); global intents (`toggle-sidebar`, `set-theme` → POST `/preferences/*`, `open-command-bar`, Ctrl/Cmd+K), navigation intents (`navigate`, `go-customer|automation|email|popup`, `go-page`, `set-import-step` — build locale-prefixed URLs), list/stream intents (`pause-stream`, `copy-*`); `mount()` calls a registrar per `[data-controller]` name.
- `controllers/event-stream.ts` — Mercure `EventSource` subscriber, prepends `{html}` rows, max 80, honours `data-paused`, publishes `data-stream-state`.
- `controllers/cardiogram.ts` — canvas events/second chart, colours from CSS custom properties, redraw on theme change.
- `controllers/editor.ts` — drag/drop blocks, POSTs to `<data-editor-endpoint or path>/blocks` expecting HTML (**no server route exists**).
- `controllers/modal.ts` (focus trap, Escape), `controllers/shell.ts` (resize after sidebar transition), `controllers/upload.ts` (dropzone → POST `/import/upload`, follows redirect).
- `global.d.ts` — `declare module "*.css"`.
- `styles/01-tokens.css`, `02-base.css`, `03-components.css` (889 lines), `04-patterns.css` (1981 lines), `app.css` (imports), `storybook.css`.
- `js/storybook-controls.js` — storybook-only controls (plain JS).

## Tooling
`tsconfig.json`: strict, `noUncheckedIndexedAccess`, ES2023, bundler resolution, `allowImportingTsExtensions`. `yarn typecheck`, `yarn format` (prettier, no config file). Edit hook in `.claude/settings.json` runs prettier on `.ts`.

## Invariants
- No framework, no npm UI libs, no bundler; imports use `.ts` extensions.
- Row/markup never constructed in TS — server renders fragments.
- Preferences must be POSTed so the next full load matches (no flash).

## Risks
Editor endpoint missing (R6); whole layer replaced if the stack migrates (R1); `04-patterns.css` is a 2k-line hub.

## Evidence
`assets/app.ts`, `assets/controllers/*.ts`, `importmap.php`, `tsconfig.json`, `package.json`, `frankenphp/docker-entrypoint.sh`.
<!-- END project-context-initializer:context -->
