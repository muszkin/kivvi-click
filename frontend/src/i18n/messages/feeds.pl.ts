/**
 * Product-feeds page copy — ported verbatim from translations/messages.pl.yaml's `feeds:` block
 * and the page-level `|trans` calls in pages/feeds.html.twig and pages/feeds/matching.html.twig.
 * The feed-source/feed-card/matching-diagnostic labels are NOT here: the old templates never ran
 * them through `|trans` either (they are hard-coded Polish fixture text, reproduced as-is by
 * FeedCard.vue and FeedsView.vue).
 */
export default {
    feeds: {
        title: "Feedy produktów",
        sub: 'Synchronizuj katalog z Google Merchant, Facebook Catalog lub własnego XML — żeby Kivvi-click znał ceny, dostępność i obrazy do dopasowania w zdarzeniach <span class="mono">add_to_cart</span> i <span class="mono">purchase</span>.',
        availableSources: "Dostępne źródła",
        connectedFeeds: "Podłączone feedy",
        connectFeed: "Podłącz feed",
        recommended: "Polecane",
        matchingTitle: "Dopasowanie cen do zdarzeń",
        matchingSub: "jak zdarzenia z Twoich stron pasują do katalogu",
        report: "Raport",
        joinField: "Pole łączące",
        fallbackRules: "Reguły fallback",
        matchRate: "Stan dopasowania (24h)",
        showMismatched: "Pokaż {count} niedopasowanych",
    },
};
