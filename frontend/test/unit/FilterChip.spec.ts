import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import FilterChip from "@/components/molecules/FilterChip.vue";

describe("FilterChip", () => {
    it("B24 renders the label, data-active, data-action and data-payload", () => {
        const wrapper = mount(FilterChip, {
            props: {
                label: "Zakup",
                icon: "money",
                active: true,
                action: "set-event-type",
                payload: "purchase",
            },
        });

        expect(wrapper.text()).toBe("Zakup");
        expect(wrapper.attributes("data-active")).toBe("true");
        expect(wrapper.attributes("data-action")).toBe("set-event-type");
        expect(wrapper.attributes("data-payload")).toBe("purchase");
        expect(wrapper.find(".icon").exists()).toBe(true);
    });

    it("the 'all' chip carries no icon and is inactive by default", () => {
        const wrapper = mount(FilterChip, {
            props: {
                label: "Wszystkie",
                action: "set-event-type",
                payload: "all",
            },
        });

        expect(wrapper.attributes("data-active")).toBe("false");
        expect(wrapper.find(".icon").exists()).toBe(false);
    });

    it("a site chip renders a colour dot instead of an icon", () => {
        const wrapper = mount(FilterChip, {
            props: {
                label: "aureashop.pl",
                dotColor: "#7a8763",
                action: "set-event-site",
                payload: "aurea",
            },
        });

        expect(wrapper.find(".dot").exists()).toBe(true);
        expect(wrapper.find(".icon").exists()).toBe(false);
    });
});
