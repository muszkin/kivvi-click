import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Segmented from "@/components/molecules/Segmented.vue";

const RANGES = [
    { value: "5m", label: "5 min", active: false },
    { value: "1h", label: "1 godz.", active: true },
    { value: "24h", label: "24 godz.", active: false },
    { value: "7d", label: "7 dni", active: false },
];

describe("Segmented", () => {
    it("B24 renders one tab per option with role=tablist/tab and aria-selected", () => {
        const wrapper = mount(Segmented, {
            props: { options: RANGES, action: "set-range" },
        });

        expect(wrapper.attributes("role")).toBe("tablist");
        const tabs = wrapper.findAll('[role="tab"]');
        expect(tabs).toHaveLength(4);
        expect(tabs[1]?.attributes("aria-selected")).toBe("true");
        expect(tabs[0]?.attributes("aria-selected")).toBe("false");
    });

    it("each tab carries the action and its own value as the payload", () => {
        const wrapper = mount(Segmented, {
            props: { options: RANGES, action: "set-range" },
        });

        const tabs = wrapper.findAll('[role="tab"]');
        expect(tabs[2]?.attributes("data-action")).toBe("set-range");
        expect(tabs[2]?.attributes("data-payload")).toBe("24h");
    });
});
