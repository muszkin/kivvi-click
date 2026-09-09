/**
 * Event-stream page copy — ported from translations/messages.pl.yaml's `events:` block (title,
 * sub) plus the root-level literal-Polish-as-key strings pages/events.html.twig ran through
 * `|trans` (Typ/Strona/Okres/"Log zdarzeń"/"Szukaj w logu"/"Załaduj więcej"/"Pokazuję
 * ostatnie"/z/"zdarzeń w wybranym oknie"/Pauza/"Eksport (CSV)"/Webhook, and common.all/common.live
 * for the "Wszystkie" filter default and the "na żywo" chip). Root-level keys are namespaced under
 * `events` here rather than reusing `common`: `src/i18n/index.ts` merges each journey's catalogue
 * into the base one with a shallow `Object.assign`, so a top-level `common` key here would
 * silently replace the *entire* base `common` object (wiping `common.search`, `common.email`, …)
 * instead of adding to it — see common-journey-rules.md's i18n rule. The type labels themselves
 * (Wyświetlenie strony, Zakup, …) are never here: `GET /api/v1/{locale}/events` sends them
 * pre-translated, exactly like `EventStreamController` did server-side.
 */
export default {
    events: {
        title: "Strumień zdarzeń",
        sub: "Wszystkie zdarzenia odebrane ze skryptu trackingowego, z możliwością filtrowania.",
        live: "na żywo",
        pause: "Pauza",
        exportCsv: "Eksport (CSV)",
        webhook: "Webhook",
        typeLabel: "Typ",
        siteLabel: "Strona",
        periodLabel: "Okres",
        log: "Log zdarzeń",
        inWindow: "zdarzeń w wybranym oknie",
        searchLog: "Szukaj w logu",
        loadMore: "Załaduj więcej",
        showingLast: "Pokazuję ostatnie {shown} z {total}",
    },
};
