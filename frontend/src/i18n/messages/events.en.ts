/**
 * Event-stream page copy — English counterpart of events.pl.ts, ported from
 * translations/messages.en.yaml's `events:` block plus its root-level Typ/Strona/Okres/etc.
 * translations. Keys mirror events.pl.ts exactly.
 */
export default {
    events: {
        title: "Event stream",
        sub: "Every event received from the tracking script, filterable by type and site.",
        live: "live",
        pause: "Pause",
        exportCsv: "Export (CSV)",
        webhook: "Webhook",
        typeLabel: "Type",
        siteLabel: "Site",
        periodLabel: "Period",
        log: "Event log",
        inWindow: "events in the selected window",
        searchLog: "Search the log",
        loadMore: "Load more",
        showingLast: "Showing the last {shown} of {total}",
    },
};
