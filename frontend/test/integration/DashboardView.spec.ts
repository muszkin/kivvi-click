import { mount, flushPromises } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import DashboardView from "@/views/DashboardView.vue";
import { serveInLocaleOf } from "../support/documentLocale";

class FakeEventSource {
    static instances: FakeEventSource[] = [];
    onopen: (() => void) | null = null;
    onmessage: ((message: MessageEvent<string>) => void) | null = null;
    onerror: (() => void) | null = null;

    constructor() {
        FakeEventSource.instances.push(this);
    }

    close(): void {}
}

function fakeCanvasContext(): Record<string, unknown> {
    return {
        scale: vi.fn(),
        clearRect: vi.fn(),
        beginPath: vi.fn(),
        moveTo: vi.fn(),
        lineTo: vi.fn(),
        closePath: vi.fn(),
        stroke: vi.fn(),
        fill: vi.fn(),
        arc: vi.fn(),
        fillText: vi.fn(),
        strokeStyle: "",
        fillStyle: "",
        lineWidth: 1,
        font: "",
        textAlign: "left",
    };
}

const KPIS = [
    {
        label: "Zdarzeń ostatnia minuta",
        value: "847",
        delta: "+12,4% vs śr.",
        dir: "up",
        series: Array.from({ length: 40 }, (_, i) => i),
    },
    {
        label: "Aktywne sesje",
        value: "312",
        delta: "+4,1%",
        dir: "up",
        series: Array.from({ length: 40 }, (_, i) => i),
    },
    {
        label: "Maile dostarczone (24h)",
        value: "8 410",
        delta: "−2,0%",
        dir: "down",
        series: Array.from({ length: 40 }, (_, i) => i),
    },
    {
        label: "Przypisany przychód (24h)",
        value: "94 200 zł",
        delta: "+22,4%",
        dir: "up",
        series: Array.from({ length: 40 }, (_, i) => i),
    },
];
const LEGEND = [
    { label: "Średnia 5 min:", value: "14,2 ev/s" },
    { label: "Pik:", value: "28 ev/s · 12:42:18" },
    { label: "Aktualizacja:", value: "co 1 s" },
];
const EVENTS = Array.from({ length: 10 }, (_, i) => ({
    time: "14:42:08",
    typeIcon: "cart",
    tone: "accent",
    type: "Dodanie do koszyka",
    detail: `Produkt ${i}`,
    customerName: "Anna K.",
    customerId: "c_1000",
    siteName: "aureashop.pl",
    siteColor: "#7a8763",
}));
const RECENT_CUSTOMERS = [
    {
        id: "c_1000",
        name: "Anna K.",
        email: "anna.k@example.com",
        orders: 0,
        lastSeen: "teraz",
    },
    {
        id: "c_1001",
        name: "Kasia N.",
        email: "kasia.n@example.com",
        orders: 1,
        lastSeen: "1 min temu",
    },
];
const TOP_AUTOMATIONS = [
    {
        id: "a3",
        name: "Rekomendacje „podobne produkty”",
        channels: ["widget"],
        runs: "18 420",
        conversion: "7,9%",
        revenue: "41 200 zł",
    },
    {
        id: "a1",
        name: "Powrót do porzuconego koszyka",
        channels: ["email", "popup"],
        runs: "1 287",
        conversion: "18,4%",
        revenue: "24 800 zł",
    },
];
const PAYLOAD = {
    kpis: KPIS,
    legend: LEGEND,
    events: EVENTS,
    mercureTopic: "/accounts/1/events",
    recentCustomers: RECENT_CUSTOMERS,
    topAutomations: TOP_AUTOMATIONS,
};

async function mountAt(path: string) {
    serveInLocaleOf(path);
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(DashboardView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B23 the dashboard matches the oracle: KPIs, cardiogram, live feed, recent customers, top automations", () => {
    let getContextSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        FakeEventSource.instances = [];
        vi.stubGlobal("EventSource", FakeEventSource);
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
        getContextSpy = vi
            .spyOn(HTMLCanvasElement.prototype, "getContext")
            .mockReturnValue(
                fakeCanvasContext() as unknown as CanvasRenderingContext2D,
            );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        getContextSpy.mockRestore();
    });

    it("fetches the locale-scoped payload", async () => {
        await mountAt("/pl/dashboard");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/dashboard",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders the page title and four KPI tiles", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        expect(wrapper.find(".page-title").text()).toBe("Co dzieje się teraz");
        const labels = wrapper.findAll(".kpi-grid .kpi-label");
        expect(labels).toHaveLength(4);
        expect(labels[0]?.text()).toBe("Zdarzeń ostatnia minuta");
        expect(labels[3]?.text()).toBe("Przypisany przychód (24h)");
    });

    it("renders the cardiogram canvas with the legend", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const canvas = wrapper.get("#cg-main");
        expect(canvas.attributes("role")).toBe("img");
        expect(canvas.attributes("aria-label")).toBe("Pulsacja zdarzeń");
        expect(wrapper.get(".cardiogram-meta").text()).toContain("14,2 ev/s");
    });

    it("server-renders the first page of the live stream, subscribed to the given topic", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const rows = wrapper.findAll("#event-stream .event-row");
        expect(rows).toHaveLength(10);
        expect(
            wrapper.get("#event-stream").attributes("data-event-stream-topic"),
        ).toBe("/accounts/1/events");
    });

    it("the Pauza button carries the pause-stream action", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        expect(wrapper.find('[data-action="pause-stream"]').exists()).toBe(
            true,
        );
    });

    it("recently seen customers are real links to their profile", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const link = wrapper.get('a.event-row[href="/pl/customers/c_1000"]');
        expect(link.text()).toContain("Anna K.");
        expect(link.text()).toContain("anna.k@example.com");
        expect(link.text()).toContain("teraz");
    });

    it("top automations are real links to the rule editor", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const link = wrapper.get('a.event-row[href="/pl/automations/a3"]');
        expect(link.text()).toContain("Rekomendacje „podobne produkty”");
        expect(link.text()).toContain("41 200 zł");
    });

    it("multiple channel chips on one automation are separated by a real text node", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const link = wrapper.get('a.event-row[href="/pl/automations/a1"]');
        const chips = link.findAll(".chip");
        expect(chips).toHaveLength(2);
        // jsdom's textContent-based .text() does not apply CSS whitespace collapsing the way a
        // real browser's rendered/innerText output does (each .chip is its own inline-flex
        // formatting context, so its *own* leading space collapses away at that box's start —
        // only a text node living in the *outer* .muted div, between the two <Chip>s, survives
        // to separate them; see the <template v-if="index > 0"> separator in DashboardView.vue).
        // What jsdom can prove directly is that such a node exists at all: without it, two
        // adjacent inline-flex chips run together with zero DOM whitespace between them.
        expect(link.text().replace(/\s+/g, " ")).toContain("email popup");
        expect(link.text()).not.toContain("emailpopup");
    });

    it("the new-automation button links to /pl/automations/new", async () => {
        const wrapper = await mountAt("/pl/dashboard");

        const link = wrapper.get('a[href="/pl/automations/new"]');
        expect(link.text()).toBe("Nowa automatyzacja");
    });
});
