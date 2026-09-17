import { mount, flushPromises } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import FeedsView from "@/views/FeedsView.vue";
import { serveInLocaleOf } from "../support/documentLocale";

const PAYLOAD = {
    kpis: [
        {
            label: "Aktywne feedy",
            value: "3",
            unit: "/4",
            delta: "1 z błędem",
            dir: "down",
        },
        {
            label: "Produktów w katalogu",
            value: "2 648",
            delta: "+42 w tym tyg.",
            dir: "up",
        },
        {
            label: "Dopasowanie zdarzeń → produkty",
            value: "94,8",
            unit: "%",
            delta: "ostatnia doba",
            dir: "up",
            deltaIcon: "spark",
        },
        {
            label: "Wartość koszyków (24h)",
            value: "382 140 zł",
            delta: "z dopasowanymi cenami",
            dir: "up",
        },
    ],
    sources: [
        {
            id: "google",
            letter: "G",
            color: "oklch(0.62 0.16 28)",
            bg: "oklch(0.93 0.04 28)",
            title: "Google Merchant Center",
            sub: "OAuth · automatyczna synchronizacja co 6h",
            recommended: true,
        },
        {
            id: "facebook",
            letter: "f",
            color: "oklch(0.55 0.13 250)",
            bg: "oklch(0.91 0.04 250)",
            title: "Facebook Catalog (Meta)",
            sub: "Catalog API · synchronizacja co 1h",
            recommended: false,
        },
        {
            id: "xml",
            letter: "×",
            color: "oklch(0.45 0.07 60)",
            bg: "oklch(0.91 0.04 60)",
            title: "XML / RSS własny",
            sub: "Dowolny URL — np. PrestaShop, Shoper, WooCommerce",
            recommended: false,
        },
        {
            id: "csv",
            letter: "↧",
            color: "oklch(0.42 0.06 150)",
            bg: "oklch(0.91 0.04 150)",
            title: "Plik CSV / arkusz Google",
            sub: "Upload pojedynczego pliku lub link do arkusza",
            recommended: false,
        },
    ],
    feeds: [
        {
            name: "aureashop.pl — Google Merchant",
            url: "https://aureashop.pl/feeds/google.xml",
            source: "google",
            status: "synced",
            products: "1 284",
            mapped: "1 284",
            mappedPercent: "100,0%",
            mismatched: 0,
            lastSync: "12 min temu",
            schedule: "co 6 godzin",
        },
        {
            name: "aureashop.pl — Facebook Catalog",
            url: "https://aureashop.pl/feeds/facebook.xml",
            source: "facebook",
            status: "syncing",
            products: "1 280",
            mapped: "1 278",
            mappedPercent: "99,8%",
            mismatched: 2,
            lastSync: "1 min temu",
            schedule: "co 1 godzinę",
        },
        {
            name: "mlot-narzedzia.pl — Google Merchant",
            url: "https://mlot-narzedzia.pl/feed/google",
            source: "google",
            status: "error",
            error: "HTTP 503 — Service Unavailable",
            products: "0",
            mapped: "0",
            mappedPercent: null,
            mismatched: 0,
            lastSync: "3 godz. temu",
            schedule: "co 6 godzin",
        },
        {
            name: "polna-bistro.pl — XML własny",
            url: "https://polna-bistro.pl/produkty.xml",
            source: "xml",
            status: "synced",
            products: "84",
            mapped: "84",
            mappedPercent: "100,0%",
            mismatched: 0,
            lastSync: "28 min temu",
            schedule: "codziennie",
        },
    ],
    coverage: [
        { label: "Po id", pct: 78, value: "78%", tone: "accent" },
        { label: "Po sku (fallback)", pct: 12, value: "12%", tone: "brown" },
        { label: "Po URL", pct: 4.8, value: "4,8%", tone: "brown" },
        { label: "Niedopasowane", pct: 5.2, value: "5,2%", tone: "bad" },
    ],
    fallbackRules: [
        { text: "Jeśli brak id → spróbuj", field: "sku" },
        { text: "Jeśli brak sku → spróbuj", field: "gtin" },
        { text: "Jeśli nadal brak → użyj", field: "URL produktu" },
    ],
    mismatched: 142,
};

async function mountAt(path: string) {
    serveInLocaleOf(path);
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(FeedsView, { global: { plugins: [router, i18n] } });
    await flushPromises();
    return wrapper;
}

describe("B30 the product-feeds page matches the oracle: 4 sources, 4 feeds, one failing with HTTP 503, matching diagnostic with 142 mismatched", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale-scoped payload", async () => {
        await mountAt("/pl/feeds");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/feeds",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders 4 sources, 4 feeds and the one failing feed's HTTP 503 strip", async () => {
        const wrapper = await mountAt("/pl/feeds");

        expect(wrapper.findAll(".feed-source")).toHaveLength(4);
        expect(wrapper.findAll(".feed-card")).toHaveLength(4);
        expect(wrapper.findAll(".feed-card__err")).toHaveLength(1);
        expect(wrapper.find(".feed-card__err").text()).toContain("HTTP 503");
        expect(wrapper.findAll(".feed-card .chip.info .dot.live")).toHaveLength(
            1,
        );
    });

    it("the matching diagnostic explains the coverage", async () => {
        const wrapper = await mountAt("/pl/feeds");
        const card = wrapper
            .findAll(".card")
            .find((c) => c.text().includes("Dopasowanie cen do zdarzeń"));

        expect(card).toBeDefined();
        expect(card?.findAll(".bar-row")).toHaveLength(4);
        expect(card?.text()).toContain("event.product_id");
        expect(card?.text()).toContain("feed.id");
        expect(card?.find('[data-action="show-mismatched"]').text()).toContain(
            "142",
        );
    });

    it("renders the page title", async () => {
        const wrapper = await mountAt("/pl/feeds");

        expect(wrapper.find(".page-title").text()).toBe("Feedy produktów");
    });
});
