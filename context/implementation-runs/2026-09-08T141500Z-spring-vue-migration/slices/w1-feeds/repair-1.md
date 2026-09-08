# Repair packet w1-feeds / repair-1 (independent review FAIL: F1 scope, F3 format:check)

Base: your candidate 81af73fedfc5750d99a665a38bc064695d244279 on branch migration/wave-1/feeds in /home/muszkin/work/kivvi-click-wt/w1-feeds (identity guard first). New candidate commit on top; never amend. HIGH reasoning effort.

## R1-A — canonical `frontend/src/i18n/index.ts`
The orchestrator authorizes `warnHtmlMessage: false` as a cross-cutting change. Make `index.ts` exactly this shape (this text becomes the canonical file every later journey inherits — the landing journey uses the same loader semantics):
```ts
// Each journey ships its own src/i18n/messages/<journey>.pl.ts / <journey>.en.ts, merged into
// the base catalogues below by their filename's locale suffix — see common-journey-rules.md.
const journeyMessages = import.meta.glob("./messages/*.{pl,en}.ts", {
    eager: true,
    import: "default",
}) as Record<string, Record<string, unknown>>;
const plMessages: Record<string, unknown> = { ...pl };
const enMessages: Record<string, unknown> = { ...en };
for (const [path, messages] of Object.entries(journeyMessages)) {
    if (path.endsWith(".pl.ts")) {
        Object.assign(plMessages, messages);
    } else if (path.endsWith(".en.ts")) {
        Object.assign(enMessages, messages);
    }
}
export const i18n = createI18n({
    legacy: false,
    locale: initialLocale(),
    fallbackLocale: DEFAULT_LOCALE,
    messages: { pl: plMessages, en: enMessages },
    // Catalogue strings ported from Twig `|raw` are developer-authored markup bound only with
    // v-html (see eslint.config.js no-v-html exemption); vue-i18n would otherwise warn on each
    // and the fail-on-warning test policy (test/setup.ts) would turn that into a false failure.
    warnHtmlMessage: false,
});
```
(keep the existing imports, SUPPORTED_LOCALES, initialLocale; adjust any type import if `Record<string, unknown>` needs a cast for `createI18n`; run prettier on it).

## R1-B — formatting
`cd frontend && npx prettier --write test/integration/FeedsView.spec.ts test/unit/KpiGrid.spec.ts` then `npm run format:check` must pass. Do not reformat anything under `src/styles/` (prettierignore is in place).

## R1-C (optional, only if ≤ 30 lines) — F2
Stop `web.FeedsController` from importing `fixtures.*` types: define the payload record types in `application` (or map in the service to application-owned records). Skip if larger; report the decision.

## Gates on the NEW candidate SHA (order; logs into evidence/repair-1-gates/)
`./mvnw -q test` → `./mvnw -q verify` → `npm run test -- --run` → `npm run test:integration -- --run` → `npm run lint && npm run typecheck && npm run format:check && npm run build` → compose stack (`kivvi-w-feeds`, 19020/19021) → `compare.mjs --journey feeds` (0 regressions) → `npx playwright test lists.spec.ts -g "product feeds"` → `performance.mjs`; `down -v`, remove image. Append "## Repair-1" to worker-report.md (files changed, gate table, new SHA, clean git status) and return it.
