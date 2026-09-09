import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import CustomerView from "@/views/CustomerView.vue";

const PAYLOAD = {
    customer: {
        id: "c_1000",
        name: "Anna K.",
        initials: "AK",
        email: "anna.k@example.com",
        tags: [
            { label: "VIP", tone: "accent" },
            { label: "subskrybent", tone: "brown" },
            { label: "PL", tone: null },
        ],
    },
    profileSub:
        "anna.k@example.com · klient od 14 stycznia 2024 · ostatnia aktywność teraz",
    facts: [
        { label: "Zamówienia", value: "0", small: false },
        { label: "Wartość życiowa", value: "196 zł", small: false },
        { label: "Średnia wartość koszyka", value: "49 zł", small: false },
        { label: "Pierwsze zdarzenie", value: "14 sty 2024", small: false },
        { label: "Liczba sesji", value: "28", small: false },
        { label: "Liczba zdarzeń", value: "412", small: false },
        { label: "customer_id", value: "c_1000", small: true },
    ],
    automations: [
        { name: "Powitanie po rejestracji" },
        { name: "Rekomendacje „podobne produkty”" },
        { name: "Newsletter — Tydzień smaków" },
    ],
    tabs: [
        { label: "Aktywność", active: true },
        { label: "Zamówienia (0)", active: false },
        { label: "Wysłane maile (12)", active: false },
        { label: "Otrzymane kupony (3)", active: false },
        { label: "Atrybuty", active: false },
    ],
    scores: [
        {
            label: "Wskaźnik zaangażowania",
            value: "82",
            unit: "/100",
            delta: "+12 vs miesiąc temu",
            dir: "up",
            deltaIcon: null,
        },
        {
            label: "Prawd. zakupu (30d)",
            value: "68",
            unit: "%",
            delta: "silny sygnał",
            dir: "up",
            deltaIcon: "spark",
        },
        {
            label: "Open rate (90d)",
            value: "74",
            unit: "%",
            delta: "+6,2pp",
            dir: "up",
            deltaIcon: null,
        },
    ],
    timeline: Array.from({ length: 9 }, (_, i) => ({
        time: `Dziś · 14:${40 - i}`,
        title: "Wyświetlenie produktu",
        detail: "/produkt/przyklad",
        icon: "eye",
    })),
};

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(CustomerView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B25 the 360 profile matches the oracle: facts, scores, timeline, tabs, back link", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and id-scoped payload", async () => {
        await mountAt("/pl/customers/c_1000");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/customers/c_1000",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders the customer's name as the title and 7 profile facts", async () => {
        const wrapper = await mountAt("/pl/customers/c_1000");

        expect(wrapper.find(".page-title").text()).toBe("Anna K.");
        expect(wrapper.find(".profile-name").text()).toBe("Anna K.");
        expect(wrapper.findAll(".profile-card .profile-fact")).toHaveLength(7);
        expect(wrapper.find(".profile-card .chip").text()).toBe("VIP");
    });

    it("renders 3 KPI tiles, 9 timeline entries and 5 tabs with the first selected", async () => {
        const wrapper = await mountAt("/pl/customers/c_1000");

        expect(wrapper.findAll(".kpi-grid .kpi")).toHaveLength(3);
        expect(wrapper.findAll(".timeline .tl-item")).toHaveLength(9);
        const tabs = wrapper.findAll(".tab-strip .tab");
        expect(tabs).toHaveLength(5);
        expect(tabs[0]?.attributes("aria-selected")).toBe("true");
        expect(tabs[1]?.attributes("aria-selected")).toBe("false");
    });

    it("renders the back link as a real <a> to the customers index", async () => {
        const wrapper = await mountAt("/pl/customers/c_1000");

        const back = wrapper.find(".page-head a.btn.ghost");
        expect(back.exists()).toBe(true);
        expect(back.attributes("href")).toBe("/pl/customers");
    });

    it("renders the 3 active automations", async () => {
        const wrapper = await mountAt("/pl/customers/c_1000");

        expect(wrapper.text()).toContain("Powitanie po rejestracji");
        expect(wrapper.text()).toContain("Newsletter — Tydzień smaków");
    });
});
