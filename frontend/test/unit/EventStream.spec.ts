import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import EventStream from "@/components/organisms/EventStream.vue";
import type { StreamEvent } from "@/composables/useEventStream";

/**
 * jsdom has no EventSource implementation; this fake gives useEventStream something real to
 * subscribe to so its onopen/onmessage/onerror handlers can be driven directly from the test,
 * mirroring how the oracle's own Playwright suite drives the real one over the wire.
 */
class FakeEventSource {
    static instances: FakeEventSource[] = [];
    url: string;
    onopen: (() => void) | null = null;
    onmessage: ((message: MessageEvent<string>) => void) | null = null;
    onerror: (() => void) | null = null;

    constructor(url: string | URL) {
        this.url = url.toString();
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

const INITIAL_EVENTS: StreamEvent[] = Array.from({ length: 30 }, (_, i) => ({
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

function liveEvent(marker: string): { event: StreamEvent } {
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

describe("EventStream", () => {
    beforeEach(() => {
        FakeEventSource.instances = [];
        vi.stubGlobal("EventSource", FakeEventSource);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("B24 renders the initial 30 server-provided rows and subscribes to the given topic", () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });

        expect(wrapper.findAll(".event-row")).toHaveLength(30);
        expect(wrapper.attributes("data-controller")).toBe("event-stream");
        expect(wrapper.attributes("data-event-stream-topic")).toBe(
            "/accounts/1/events",
        );
        expect(wrapper.attributes("data-stream-state")).toBe("connecting");
        expect(FakeEventSource.instances).toHaveLength(1);
        expect(FakeEventSource.instances[0]?.url).toContain(
            "topic=%2Faccounts%2F1%2Fevents",
        );
    });

    it("B24 flips data-stream-state to 'live' once the subscription opens", () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });

        FakeEventSource.instances[0]?.emitOpen();

        expect(wrapper.attributes("data-stream-state")).toBe("live");
    });

    it("B24 prepends a row from an SSE message and flags it 'new'", async () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });

        FakeEventSource.instances[0]?.emitMessage(
            liveEvent("412,00 PLN · zamówienie X"),
        );
        await wrapper.vm.$nextTick();

        const rows = wrapper.findAll(".event-row");
        expect(rows).toHaveLength(31);
        expect(rows[0]?.classes()).toContain("new");
        expect(rows[0]?.find(".event-row__detail").text()).toBe(
            "412,00 PLN · zamówienie X",
        );
    });

    it("caps the row list at 80, dropping the oldest", async () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });
        const source = FakeEventSource.instances[0]!;

        for (let i = 0; i < 60; i++) {
            source.emitMessage(liveEvent(`marker-${i}`));
        }
        await wrapper.vm.$nextTick();

        expect(wrapper.findAll(".event-row")).toHaveLength(80);
        expect(
            wrapper.findAll(".event-row")[0]?.find(".event-row__detail").text(),
        ).toBe("marker-59");
    });

    it("does not render a message received while data-paused is 'true'", async () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });
        wrapper.element.setAttribute("data-paused", "true");

        FakeEventSource.instances[0]?.emitMessage(
            liveEvent("should-not-appear"),
        );
        await wrapper.vm.$nextTick();

        expect(wrapper.findAll(".event-row")).toHaveLength(30);
    });

    it("flips data-stream-state to 'reconnecting' on an error", () => {
        const wrapper = mount(EventStream, {
            props: { events: INITIAL_EVENTS, topic: "/accounts/1/events" },
        });

        FakeEventSource.instances[0]?.emitError();

        expect(wrapper.attributes("data-stream-state")).toBe("reconnecting");
    });

    it("does not subscribe when there is no topic (a static, non-live feed)", () => {
        mount(EventStream, { props: { events: INITIAL_EVENTS } });

        expect(FakeEventSource.instances).toHaveLength(0);
    });
});
