/**
 * Dashboard page copy — ported from translations/messages.pl.yaml's `dashboard:` block (title,
 * sub, liveFeed(Sub), cardiogram(Sub), topRules(Sub), recentCustomers) plus the root-level
 * literal-Polish-as-key strings pages/dashboard.html.twig ran through `|trans` (Eksport, Pauza,
 * "Cały log", Wszyscy, Wszystkie, zam., uruchomień, konwersja, przychód) and the two
 * `common.*` keys the page-head actions block used (common.live, common.new_automation). Root-level
 * keys are namespaced under `dashboard` here rather than reusing `common`: `src/i18n/index.ts`
 * merges each journey's catalogue into the base one with a shallow `Object.assign`, so a top-level
 * `common` key here would silently replace the *entire* base `common` object — see
 * common-journey-rules.md's i18n rule and events.pl.ts's own copy of this note.
 */
export default {
    dashboard: {
        title: "Co dzieje się teraz",
        sub: "Strumień zdarzeń z Twoich stron, na żywo, oraz wpływ uruchomionych automatyzacji.",
        liveFeed: "Strumień na żywo",
        liveFeedSub: "wszystkie strony",
        cardiogram: "Pulsacja zdarzeń",
        cardiogramSub: "liczba zdarzeń / sekundę",
        topRules: "Najskuteczniejsze automatyzacje",
        topRulesSub: "wg konwersji w ostatnich 7 dniach",
        recentCustomers: "Ostatnio widziani",
        live: "na żywo",
        newAutomation: "Nowa automatyzacja",
        export: "Eksport",
        pause: "Pauza",
        fullLog: "Cały log",
        allCustomers: "Wszyscy",
        allAutomations: "Wszystkie",
        orders: "zam.",
        runs: "uruchomień",
        conversion: "konwersja",
        revenue: "przychód",
    },
};
