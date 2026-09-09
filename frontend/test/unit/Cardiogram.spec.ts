import { mount, VueWrapper } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Cardiogram from "@/components/organisms/Cardiogram.vue";

/**
 * jsdom has no canvas 2D context implementation (HTMLCanvasElement.getContext returns null by
 * default); this fake gives draw() something real to call so the redraw-on-mutation behaviour can
 * be spied on, mirroring how EventStream.spec.ts fakes EventSource for the same reason.
 */
function fakeContext(): Record<string, unknown> {
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

describe("Cardiogram", () => {
    let ctx: Record<string, unknown>;
    let getContextSpy: ReturnType<typeof vi.spyOn>;
    let wrapper: VueWrapper | undefined;

    beforeEach(() => {
        ctx = fakeContext();
        getContextSpy = vi
            .spyOn(HTMLCanvasElement.prototype, "getContext")
            .mockReturnValue(ctx as unknown as CanvasRenderingContext2D);
    });

    afterEach(() => {
        wrapper?.unmount();
        wrapper = undefined;
        getContextSpy.mockRestore();
        document.documentElement.removeAttribute("data-theme");
        vi.useRealTimers();
    });

    it("B23 renders the canvas with role=img and the title as its accessible name", () => {
        wrapper = mount(Cardiogram, {
            props: { id: "cg-main", title: "Pulsacja zdarzeń" },
        });

        const canvas = wrapper.get("canvas");
        expect(canvas.attributes("id")).toBe("cg-main");
        expect(canvas.attributes("role")).toBe("img");
        expect(canvas.attributes("aria-label")).toBe("Pulsacja zdarzeń");
        expect(canvas.classes()).toContain("cardiogram-canvas");
    });

    it("B23 draws once on mount", () => {
        wrapper = mount(Cardiogram, { props: { title: "Pulsacja zdarzeń" } });

        expect(ctx.clearRect).toHaveBeenCalledTimes(1);
    });

    it("B23 redraws when <html data-theme> mutates, without reconnecting anything", async () => {
        wrapper = mount(Cardiogram, { props: { title: "Pulsacja zdarzeń" } });
        const callsBefore = (ctx.clearRect as ReturnType<typeof vi.fn>).mock
            .calls.length;

        document.documentElement.setAttribute("data-theme", "dark");
        // The MutationObserver callback runs as a microtask.
        await Promise.resolve();
        await Promise.resolve();

        expect(
            (ctx.clearRect as ReturnType<typeof vi.fn>).mock.calls.length,
        ).toBeGreaterThan(callsBefore);
    });

    it("B23 redraws once a second", () => {
        vi.useFakeTimers();
        wrapper = mount(Cardiogram, { props: { title: "Pulsacja zdarzeń" } });
        const callsBefore = (ctx.clearRect as ReturnType<typeof vi.fn>).mock
            .calls.length;

        document.dispatchEvent(new CustomEvent("kivvi:event"));
        vi.advanceTimersByTime(1000);

        expect(
            (ctx.clearRect as ReturnType<typeof vi.fn>).mock.calls.length,
        ).toBeGreaterThan(callsBefore);
    });

    it("renders one range button per entry, data-active reflecting the active one", () => {
        wrapper = mount(Cardiogram, {
            props: {
                title: "Pulsacja zdarzeń",
                ranges: [
                    { label: "5 min", active: true },
                    { label: "1 godz." },
                    { label: "24 godz." },
                ],
            },
        });

        const buttons = wrapper.findAll('[data-action="set-range"]');
        expect(buttons).toHaveLength(3);
        expect(buttons[0]?.attributes("data-active")).toBe("true");
        expect(buttons[1]?.attributes("data-active")).toBe("false");
        expect(buttons[1]?.text()).toBe("1 godz.");
        expect(buttons[1]?.attributes("data-payload")).toBe("1 godz.");
    });

    it("renders the legend entries, label and value side by side", () => {
        wrapper = mount(Cardiogram, {
            props: {
                title: "Pulsacja zdarzeń",
                legend: [
                    { label: "Średnia 5 min:", value: "14,2 ev/s" },
                    { label: "Aktualizacja:", value: "co 1 s" },
                ],
            },
        });

        const legend = wrapper.get(".cardiogram-meta");
        // .cardiogram-meta is a flex row with its own `gap` (03-components.css): the visual
        // separation between entries comes from that gap, not a DOM space between the <span>s.
        expect(legend.text()).toBe(
            "Średnia 5 min: 14,2 ev/sAktualizacja: co 1 s",
        );
    });

    it("does not render the ranges row or the legend when neither is given", () => {
        wrapper = mount(Cardiogram, { props: { title: "Pulsacja zdarzeń" } });

        expect(wrapper.find('[data-action="set-range"]').exists()).toBe(false);
        expect(wrapper.find(".cardiogram-meta").exists()).toBe(false);
    });
});
