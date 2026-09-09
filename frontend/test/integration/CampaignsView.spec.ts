import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import CampaignsView from "@/views/CampaignsView.vue";

const PAYLOAD = {
    kpis: [
        {
            label: "Wysłane (30 dni)",
            value: "142 410",
            delta: "+18% vs poprzedni okres",
            dir: "up",
        },
        {
            label: "Średni open rate",
            value: "38,4",
            unit: "%",
            delta: "+2,1pp",
            dir: "up",
        },
        {
            label: "Średni CTR",
            value: "7,8",
            unit: "%",
            delta: "−0,4pp",
            dir: "down",
        },
        {
            label: "Przychód z kampanii",
            value: "184 230 zł",
            delta: "+24%",
            dir: "up",
        },
    ],
    filters: [
        {
            label: "Wszystkie",
            count: "18",
            active: true,
            action: "set-campaign-filter",
            payload: "all",
        },
        {
            label: "Wyzwalane",
            count: "12",
            active: false,
            action: "set-campaign-filter",
            payload: "trigger",
        },
        {
            label: "Jednorazowe",
            count: "6",
            active: false,
            action: "set-campaign-filter",
            payload: "broadcast",
        },
        {
            label: "Wstrzymane",
            count: "2",
            active: false,
            action: "set-campaign-filter",
            payload: "paused",
        },
    ],
    columns: [
        { label: "Kampania" },
        { label: "Typ" },
        { label: "Status" },
        { label: "Wysłane", align: "right" },
        { label: "Otwarcia", align: "right" },
        { label: "Kliknięcia", align: "right" },
        { label: "Przychód", align: "right" },
        { label: "" },
    ],
    rows: [
        {
            id: "k1",
            name: "Powrót do koszyka — wariant A",
            statusTone: "good",
            statusLabel: "Aktywna",
            typeLabel: "Wyzwalana",
            typeTone: "accent",
            sent: "1 287",
            open: "62,4%",
            click: "18,4%",
            revenue: "24 800 zł",
        },
        {
            id: "k4",
            name: "Black weekend — VIP",
            statusTone: "info",
            statusLabel: "Zaplanowana",
            typeLabel: "Masowa",
            typeTone: "brown",
            sent: "—",
            open: "—",
            click: "—",
            revenue: "—",
        },
    ],
};

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(CampaignsView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B27 the campaigns index matches the oracle: 4 KPIs, 4 filters, the performance table", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('fetches the locale-scoped payload, defaulting the filter to "all"', async () => {
        await mountAt("/pl/campaigns");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/campaigns?filter=all",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards ?filter= from the URL unchanged", async () => {
        await mountAt("/pl/campaigns?filter=trigger");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/campaigns?filter=trigger",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("B01/B27 renders 4 KPI tiles and 4 filter chips from the payload", async () => {
        const wrapper = await mountAt("/pl/campaigns");

        expect(wrapper.findAll(".kpi-grid .kpi")).toHaveLength(4);
        const chips = wrapper.findAll(".filter-chip");
        expect(chips).toHaveLength(4);
        expect(chips[0]?.attributes("data-active")).toBe("true");
        expect(chips[0]?.text()).toContain("Wszystkie · 18");
    });

    it("renders one table row per campaign, wired to the go-email intent with its own id", async () => {
        const wrapper = await mountAt("/pl/campaigns");

        const rows = wrapper.findAll(".table tbody tr");
        expect(rows).toHaveLength(2);
        expect(rows[0]?.attributes("data-action")).toBe("go-email");
        expect(rows[0]?.attributes("data-payload")).toBe("k1");
        expect(rows[0]?.text()).toContain("Wyzwalana");
        expect(rows[1]?.findAll("td")[3]?.text()).toBe("—");
    });

    it('renders the "Nowa kampania" action as a real link to /emails/new, not an intent', async () => {
        const wrapper = await mountAt("/pl/campaigns");

        const link = wrapper.find("a.btn.primary");
        expect(link.exists()).toBe(true);
        expect(link.attributes("href")).toBe("/pl/emails/new");
    });
});
