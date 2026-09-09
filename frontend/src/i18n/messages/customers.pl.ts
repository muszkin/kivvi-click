/**
 * Customers index + 360-profile page chrome — ported verbatim from translations/messages.pl.yaml
 * (the `customers:` block and the standalone `|trans`'d Polish strings customers.html.twig /
 * customer.html.twig reference) plus the page-level `|trans` calls themselves. Segment labels,
 * table row content, profile facts, tabs, KPI scores and the timeline are NOT here: the old
 * templates never ran any of that through `|trans` either — it is hard-coded Polish fixture text,
 * reproduced as-is by CustomersFixtures/CustomersViewService and rendered verbatim by
 * CustomersView.vue / CustomerView.vue.
 */
export default {
    customers: {
        title: "Klienci",
        exportCsv: "Eksport CSV",
        segments: "Segmenty",
        newSegment: "Nowy segment",
        filters: "Filtry",
        columns: {
            customer: "Klient",
            email: "Email",
            segment: "Segment",
            orders: "Zamówienia",
            lifetimeValue: "Wartość życiowa",
            lastActivity: "Ostatnia aktywność",
            actions: "Akcje",
        },
        backToList: "← Powrót do listy",
        sendEmail: "Wyślij email",
        grantCoupon: "Daj kupon",
        addToSegment: "Dodaj do segmentu",
        activeAutomations: "Aktywne automatyzacje",
        timeline: "Oś czasu",
        last24h: "ostatnie 24 godziny",
    },
};
