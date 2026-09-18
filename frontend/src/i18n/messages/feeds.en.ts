/**
 * Product-feeds page copy — ported verbatim from translations/messages.en.yaml's `feeds:` block
 * and the page-level `|trans` calls in pages/feeds.html.twig and pages/feeds/matching.html.twig.
 * Keys mirror feeds.pl.ts exactly.
 */
export default {
    feeds: {
        title: "Product feeds",
        sub: 'Sync your catalogue from Google Merchant, Facebook Catalog or your own XML — so Kivvi-click knows prices, availability and images to match against <span class="mono">add_to_cart</span> and <span class="mono">purchase</span> events.',
        availableSources: "Available sources",
        connectedFeeds: "Connected feeds",
        connectFeed: "Connect a feed",
        recommended: "Recommended",
        matchingTitle: "Matching prices to events",
        matchingSub: "how events from your sites match the catalogue",
        report: "Report",
        joinField: "Join field",
        fallbackRules: "Fallback rules",
        matchRate: "Match rate (24h)",
        statusSynced: "Synchronised",
        statusSyncing: "Synchronising…",
        statusError: "Error",
        statusPaused: "Paused",
        resume: "Resume",
        productsLabel: "Products",
        fieldMapping: "Field mapping",
        preview: "Preview",
        removeFeed: "Remove this feed",
        showMismatched: "Show {count} unmatched",
    },
};
