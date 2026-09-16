import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import AutomationEditorView from "@/views/AutomationEditorView.vue";
import { serveInLocaleOf } from "../support/documentLocale";

const HEADER = {
    id: "a1",
    name: "Powrót do porzuconego koszyka",
    status: "active",
    statusLabel: "Aktywna · Edytuj logikę uruchamiania, warunki i akcje",
};

const TABS = [
    { label: "Budowniczy", active: true },
    { label: "Statystyki", active: false },
    { label: "Wykonania (1 287)", active: false },
    { label: "Historia zmian", active: false },
];

const STEPS = [
    {
        n: 1,
        kicker: "KIEDY · trigger",
        title: "Klient porzuca koszyk",
        addLabel: "Dodaj warunek wyzwalacza",
        blocks: [
            {
                icon: "cart",
                title: "Zdarzenie",
                body: '<span class="pill">cart_abandon</span>',
            },
        ],
    },
    {
        n: 2,
        kicker: "JEŚLI · warunki",
        title: "Wszystkie muszą się zgadzać",
        addLabel: "Dodaj warunek",
        blocks: [
            { icon: "eye", title: "Częstotliwość", body: "a" },
            { icon: "book", title: "Historia zakupów", body: "b" },
            { icon: "mail", title: "Subskrypcja", body: "c" },
        ],
    },
    {
        n: 3,
        kicker: "WTEDY · akcje",
        title: "Sekwencja krok po kroku",
        addLabel: "Dodaj krok",
        blocks: [{ icon: "mail", title: "Wyślij email", body: "d" }],
    },
];

const NODES = [
    {
        x: 30,
        y: 40,
        kind: "trigger",
        kicker: "KIEDY",
        title: "Porzucenie koszyka",
        sub: "cart_abandon · po 8 min",
        icon: "cart",
    },
    {
        x: 320,
        y: 40,
        kind: "cond",
        kicker: "JEŚLI",
        title: "Wartość ≥ 150 PLN",
        sub: "cart.value >= 150",
        icon: "filter",
    },
    {
        x: 320,
        y: 200,
        kind: "cond",
        kicker: "JEŚLI",
        title: "Nie był w segmencie VIP",
        sub: 'customer.segment != "vip"',
        icon: "filter",
    },
    {
        x: 610,
        y: 40,
        kind: "action",
        kicker: "WTEDY · 1",
        title: "Email — szablon A",
        sub: "„Wróć po koszyk”",
        icon: "mail",
    },
    {
        x: 610,
        y: 200,
        kind: "action",
        kicker: "WTEDY · 2",
        title: "Czekaj 24h",
        sub: "delay 24h",
        icon: "pause",
    },
    {
        x: 610,
        y: 360,
        kind: "action",
        kicker: "WTEDY · 3",
        title: "Kupon −10%",
        sub: "coupon: WROCMY-{id}",
        icon: "coupon",
    },
];

const EDGES = [
    { from: 0, to: 1 },
    { from: 1, to: 2 },
    { from: 1, to: 3 },
    { from: 3, to: 4 },
    { from: 4, to: 5 },
];

const SIMULATION = [
    {
        label: "Zdarzeń pasujących",
        value: "2 412",
        note: "w ostatnich 7 dniach",
    },
    {
        label: "Spełniających warunki",
        value: '1 287 <span class="muted">(53,4%)</span>',
        note: "zostałoby uruchomione",
    },
    {
        label: "Estymowany przychód",
        value: "~ 24 800 zł",
        note: "przy 18,4% konwersji",
        color: "var(--good)",
    },
];

function payload(view: "list" | "flow", header = HEADER) {
    return {
        automation: header,
        view,
        tabs: TABS,
        steps: STEPS,
        nodes: NODES,
        edges: EDGES,
        simulation: SIMULATION,
    };
}

async function mountAt(path: string) {
    serveInLocaleOf(path);
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(AutomationEditorView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B26 the rule editor matches the oracle: list view, flow view, simulation, publish", () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and id-scoped payload with no view param by default", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list"))),
        );

        await mountAt("/pl/automations/a1");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/automations/a1",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards ?view=flow to the API call", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("flow"))),
        );

        await mountAt("/pl/automations/a1?view=flow");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/automations/a1?view=flow",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders the list view by default: 3 rb-step sections, KIEDY kicker, 3 blocks in step 2", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list"))),
        );

        const wrapper = await mountAt("/pl/automations/a1");

        const steps = wrapper.findAll(".rb-step");
        expect(steps).toHaveLength(3);
        expect(steps[0]?.find(".rb-step__kicker").text()).toContain("KIEDY");
        expect(steps[1]?.findAll(".rb-block")).toHaveLength(3);
        expect(wrapper.find(".flow-canvas").exists()).toBe(false);
    });

    it("renders the flow view: 6 flow-node elements, 5 svg paths, and no rb-pipeline", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("flow"))),
        );

        const wrapper = await mountAt("/pl/automations/a1?view=flow");

        expect(wrapper.findAll(".flow-canvas .flow-node")).toHaveLength(6);
        expect(wrapper.findAll(".flow-canvas .flow-svg path")).toHaveLength(5);
        expect(wrapper.find(".rb-pipeline").exists()).toBe(false);
    });

    it("the Lista/Diagram switch is a real navigation via the 'navigate' intent", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list"))),
        );

        const wrapper = await mountAt("/pl/automations/a1");

        const options = wrapper.findAll('.seg [data-action="navigate"]');
        expect(options).toHaveLength(2);
        expect(options[0]?.attributes("data-payload")).toBe(
            "/pl/automations/a1?view=list",
        );
        expect(options[1]?.attributes("data-payload")).toBe(
            "/pl/automations/a1?view=flow",
        );
        expect(options[0]?.attributes("data-active")).toBe("true");
        expect(options[1]?.attributes("data-active")).toBe("false");
    });

    it("shows the simulation card and the publish button", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list"))),
        );

        const wrapper = await mountAt("/pl/automations/a1");

        const card = wrapper
            .findAll(".card")
            .find((c) => c.text().includes("Test reguły"));
        expect(card).toBeDefined();
        expect(card?.text()).toContain("Estymowany przychód");
        const publish = wrapper.find('[data-action="publish"]');
        expect(publish.exists()).toBe(true);
        expect(publish.attributes("data-payload")).toBe("a1");
    });

    it("renders the back link as a real <a> to the automations index", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list"))),
        );

        const wrapper = await mountAt("/pl/automations/a1");

        const back = wrapper.find(".page-head a.btn.ghost");
        expect(back.exists()).toBe(true);
        expect(back.attributes("href")).toBe("/pl/automations");
    });

    it("B01/B26 GET /automations/new renders and shows its own headline (the generic draft header)", async () => {
        const draftHeader = {
            id: "new",
            name: "Nowa automatyzacja",
            status: "draft",
            statusLabel:
                "Wersja robocza · Edytuj logikę uruchamiania, warunki i akcje",
        };
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(payload("list", draftHeader))),
        );

        const wrapper = await mountAt("/pl/automations/new");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/automations/new",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
        expect(wrapper.find(".page-title").text()).toBe("Nowa automatyzacja");
    });
});
