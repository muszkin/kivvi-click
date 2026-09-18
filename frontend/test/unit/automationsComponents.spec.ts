import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import { describe, expect, it } from "vitest";
import AutoCard from "@/components/organisms/AutoCard.vue";
import FlowCanvas from "@/components/organisms/FlowCanvas.vue";
import { i18n } from "@/i18n";
import RulePipeline, {
    type PipelineStep,
} from "@/components/organisms/RulePipeline.vue";

const NODES = [
    {
        x: 30,
        y: 40,
        kind: "trigger" as const,
        kicker: "KIEDY",
        title: "Porzucenie koszyka",
        sub: "cart_abandon · po 8 min",
        icon: "cart",
    },
    {
        x: 320,
        y: 40,
        kind: "cond" as const,
        kicker: "JEŚLI",
        title: "Wartość ≥ 150 PLN",
        sub: "cart.value >= 150",
        icon: "filter",
    },
    {
        x: 320,
        y: 200,
        kind: "cond" as const,
        kicker: "JEŚLI",
        title: "Nie był w segmencie VIP",
        sub: 'customer.segment != "vip"',
        icon: "filter",
    },
    {
        x: 610,
        y: 40,
        kind: "action" as const,
        kicker: "WTEDY · 1",
        title: "Email — szablon A",
        sub: "„Wróć po koszyk”",
        icon: "mail",
    },
    {
        x: 610,
        y: 200,
        kind: "action" as const,
        kicker: "WTEDY · 2",
        title: "Czekaj 24h",
        sub: "delay 24h",
        icon: "pause",
    },
    {
        x: 610,
        y: 360,
        kind: "action" as const,
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

const STEPS: PipelineStep[] = [
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

describe("B26 FlowCanvas", () => {
    it("renders one FlowNode per node and one dashed path per edge", () => {
        const wrapper = mount(FlowCanvas, {
            global: { plugins: [i18n] },
            props: { nodes: NODES, edges: EDGES },
        });

        expect(wrapper.findAll(".flow-node")).toHaveLength(6);
        expect(wrapper.findAll(".flow-svg path")).toHaveLength(5);
    });

    // PIO-129: the footer and the two canvas buttons were template-literal Polish, outside the
    // translator on the old stack and in the port. They follow the locale now, so this asserts
    // both halves rather than only the Polish one.
    it("reports the node/edge counts in its footer, in the active language", async () => {
        const wrapper = mount(FlowCanvas, {
            global: { plugins: [i18n] },
            props: { nodes: NODES, edges: EDGES },
        });

        expect(wrapper.text()).toContain("6 nodes · 5 connections");

        i18n.global.locale.value = "pl";
        await nextTick();
        expect(wrapper.text()).toContain("6 węzłów · 5 połączeń");

        i18n.global.locale.value = "en";
    });

    it("computes an edge path from the tail node's right edge to the head node's left edge", () => {
        const wrapper = mount(FlowCanvas, {
            global: { plugins: [i18n] },
            props: { nodes: NODES, edges: EDGES },
        });

        const firstPath = wrapper.find(".flow-svg path");
        // node 0 at (30,40), nodeWidth default 220 -> start (250, 72); node 1 at (320,40) -> end (320, 72)
        expect(firstPath.attributes("d")).toBe(
            "M 250 72 C 310 72 260 72 320 72",
        );
    });
});

describe("B26 RulePipeline", () => {
    it("renders 3 steps, the second with 3 blocks", () => {
        const wrapper = mount(RulePipeline, { props: { steps: STEPS } });

        const stepEls = wrapper.findAll(".rb-step");
        expect(stepEls).toHaveLength(3);
        expect(stepEls[1]?.findAll(".rb-block")).toHaveLength(3);
    });

    it("shows the KIEDY kicker on the first step", () => {
        const wrapper = mount(RulePipeline, { props: { steps: STEPS } });

        expect(wrapper.findAll(".rb-step__kicker")[0]?.text()).toContain(
            "KIEDY",
        );
    });
});

describe("B26 AutoCard", () => {
    it("carries the row intent as data-action/data-payload", () => {
        const wrapper = mount(AutoCard, {
            props: {
                title: "Powrót do porzuconego koszyka",
                chips: [{ label: "Aktywna", tone: "good" }],
                metrics: [{ value: "1 287", label: "uruchomień (7d)" }],
                action: "go-automation",
                payload: "a1",
            },
        });

        expect(wrapper.attributes("data-action")).toBe("go-automation");
        expect(wrapper.attributes("data-payload")).toBe("a1");
        expect(wrapper.find(".auto-card__title").text()).toBe(
            "Powrót do porzuconego koszyka",
        );
        expect(wrapper.find(".chip").text()).toBe("Aktywna");
    });
});
