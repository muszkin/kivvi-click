import { mount, flushPromises } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import EventsView from "@/views/EventsView.vue";

class FakeEventSource {
    static instances: FakeEventSource[] = [];
    onopen: (() => void) | null = null;
    onmessage: ((message: MessageEvent<string>) => void) | null = null;
    onerror: (() => void) | null = null;

    constructor() {
        FakeEventSource.instances.push(this);
    }

    close(): void {}

    emitOpen(): void {
        this.onopen?.();
    }

    emitMessage(data: unknown): void {
        this.onmessage?.({
            data: JSON.stringify(data),
        } as MessageEvent<string>);
    }

    emitError(): void {
        this.onerror?.();
    }
}

const TYPE_FILTERS = [
    {
        label: "Wszystkie",
        active: true,
        action: "set-event-type",
        payload: "all",
    },
    {
        label: "Wyświetlenie strony",
        icon: "eye",
        active: false,
        action: "set-event-type",
        payload: "pageview",
    },
    {
        label: "Dodanie do koszyka",
        icon: "cart",
        active: false,
        action: "set-event-type",
        payload: "add_to_cart",
    },
    {
        label: "Zakup",
        icon: "money",
        active: false,
        action: "set-event-type",
        payload: "purchase",
    },
    {
        label: "Zalogowanie",
        icon: "user",
        active: false,
        action: "set-event-type",
        payload: "login",
    },
    {
        label: "Rejestracja",
        icon: "user",
        active: false,
        action: "set-event-type",
        payload: "signup",
    },
    {
        label: "Wyszukiwanie",
        icon: "search",
        active: false,
        action: "set-event-type",
        payload: "search",
    },
    {
        label: "Porzucony koszyk",
        icon: "cart",
        active: false,
        action: "set-event-type",
        payload: "cart_abandon",
    },
    {
        label: "Lista życzeń",
        icon: "heart",
        active: false,
        action: "set-event-type",
        payload: "wishlist",
    },
];
const SITE_FILTERS = [
    {
        label: "Wszystkie",
        icon: "globe",
        active: true,
        action: "set-event-site",
        payload: "all",
    },
    {
        label: "aureashop.pl",
        dotColor: "#7a8763",
        active: false,
        action: "set-event-site",
        payload: "aurea",
    },
    {
        label: "mlot-narzedzia.pl",
        dotColor: "#a3825b",
        active: false,
        action: "set-event-site",
        payload: "mlot",
    },
    {
        label: "polna-bistro.pl",
        dotColor: "#8b6f53",
        active: false,
        action: "set-event-site",
        payload: "pol",
    },
];
const RANGES = [
    { value: "5m", label: "5 min", active: false },
    { value: "1h", label: "1 godz.", active: true },
    { value: "24h", label: "24 godz.", active: false },
    { value: "7d", label: "7 dni", active: false },
];
const EVENTS = Array.from({ length: 30 }, (_, i) => ({
    time: "14:42:08",
    typeIcon: "eye",
    tone: "",
    type: "Wyświetlenie strony",
    detail: `/produkt/${i}`,
    customerName: "Anna K.",
    customerId: "c_1000",
    siteName: "aureashop.pl",
    siteColor: "#7a8763",
}));
const PAYLOAD = {
    typeFilters: TYPE_FILTERS,
    siteFilters: SITE_FILTERS,
    ranges: RANGES,
    events: EVENTS,
    total: "9 360",
    shown: 30,
    mercureTopic: "/accounts/1/events",
};

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(EventsView, { global: { plugins: [router, i18n] } });
    await flushPromises();
    return wrapper;
}

describe("B24 the event stream page matches the oracle: 30 rows, 9+4 filters, 4 ranges", () => {
    beforeEach(() => {
        FakeEventSource.instances = [];
        vi.stubGlobal("EventSource", FakeEventSource);
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale-scoped payload", async () => {
        await mountAt("/pl/events");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/events",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards type/site/range query parameters to the API call", async () => {
        await mountAt("/pl/events?type=purchase&site=aurea&range=24h");

        const calledUrl = (fetch as unknown as { mock: { calls: unknown[][] } })
            .mock.calls[0]?.[0];
        expect(calledUrl).toContain("/api/v1/pl/events?");
        expect(calledUrl).toContain("type=purchase");
        expect(calledUrl).toContain("site=aurea");
        expect(calledUrl).toContain("range=24h");
    });

    it("renders 30 event rows, 9 type filter chips and 4 site filter chips", async () => {
        const wrapper = await mountAt("/pl/events");

        expect(wrapper.findAll("#event-stream .event-row")).toHaveLength(30);
        expect(
            wrapper.findAll(".events-toolbar").at(0)?.findAll(".filter-chip"),
        ).toHaveLength(9);
        expect(
            wrapper.findAll(".events-toolbar").at(1)?.findAll(".filter-chip"),
        ).toHaveLength(4);
        expect(wrapper.findAll('.seg [role="tab"]')).toHaveLength(4);
    });

    it("renders the page title and the total/shown counts", async () => {
        const wrapper = await mountAt("/pl/events");

        expect(wrapper.find(".page-title").text()).toBe("Strumień zdarzeń");
        expect(wrapper.find(".card-sub").text()).toContain("9 360");
        expect(wrapper.find(".card-foot").text()).toContain(
            "Pokazuję ostatnie 30 z 9 360",
        );
    });

    it("the Pauza button carries the pause-stream action", async () => {
        const wrapper = await mountAt("/pl/events");

        expect(wrapper.find('[data-action="pause-stream"]').exists()).toBe(
            true,
        );
    });
});

describe("B24 the live event stream: connecting -> live, a DEV-3 JSON message renders a new row, the 80-row cap, and pause", () => {
    beforeEach(() => {
        FakeEventSource.instances = [];
        vi.stubGlobal("EventSource", FakeEventSource);
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    function liveEvent(marker: string) {
        return {
            event: {
                time: "14:42:09",
                typeIcon: "money",
                tone: "good",
                type: "Zakup",
                detail: marker,
                customerName: "Hania Kowalska",
                customerId: "c_1001",
                siteName: "aureashop.pl",
                siteColor: "#7a8763",
            },
        };
    }

    it("data-stream-state goes connecting -> live once the subscription opens", async () => {
        const wrapper = await mountAt("/pl/events");
        const stream = wrapper.get("#event-stream");
        expect(stream.attributes("data-stream-state")).toBe("connecting");
        expect(stream.attributes("data-event-stream-topic")).toBe(
            "/accounts/1/events",
        );

        FakeEventSource.instances[0]?.emitOpen();
        await wrapper.vm.$nextTick();

        expect(stream.attributes("data-stream-state")).toBe("live");
    });

    it("a dispatched DEV-3 JSON message prepends a translated .event-row.new", async () => {
        const wrapper = await mountAt("/pl/events");
        FakeEventSource.instances[0]?.emitOpen();

        const marker = "412,00 PLN · zamówienie ORACLE-b24";
        FakeEventSource.instances[0]?.emitMessage(liveEvent(marker));
        await wrapper.vm.$nextTick();

        const rows = wrapper.findAll("#event-stream .event-row");
        expect(rows).toHaveLength(31);
        const newRow = rows[0]!;
        expect(newRow.classes()).toContain("new");
        expect(newRow.find(".event-row__type").text()).toBe("Zakup");
        expect(newRow.find(".event-row__detail").text()).toBe(marker);
        expect(newRow.find(".event-row__customer").text()).toContain(
            "Hania Kowalska",
        );
    });

    it("caps the row list at 80, dropping the oldest", async () => {
        const wrapper = await mountAt("/pl/events");
        const source = FakeEventSource.instances[0]!;
        source.emitOpen();

        for (let i = 0; i < 60; i++) {
            source.emitMessage(liveEvent(`marker-${i}`));
        }
        await wrapper.vm.$nextTick();

        expect(wrapper.findAll("#event-stream .event-row")).toHaveLength(80);
        expect(
            wrapper
                .findAll("#event-stream .event-row")[0]
                ?.find(".event-row__detail")
                .text(),
        ).toBe("marker-59");
    });

    it('data-paused="true" suppresses new rows without closing the subscription', async () => {
        const wrapper = await mountAt("/pl/events");
        const source = FakeEventSource.instances[0]!;
        source.emitOpen();
        wrapper
            .get("#event-stream")
            .element.setAttribute("data-paused", "true");

        source.emitMessage(liveEvent("should-not-appear"));
        await wrapper.vm.$nextTick();

        expect(wrapper.findAll("#event-stream .event-row")).toHaveLength(30);
        expect(
            wrapper.get("#event-stream").attributes("data-stream-state"),
        ).toBe("live");
    });
});
