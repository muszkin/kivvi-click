import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Icon from "@/components/atoms/Icon.vue";

describe("Icon", () => {
    it("renders the stroke path for a known name", () => {
        const wrapper = mount(Icon, { props: { name: "dashboard" } });
        expect(wrapper.find("svg.icon").exists()).toBe(true);
        expect(wrapper.find("rect").exists()).toBe(true);
    });

    it("renders nothing for an unknown name", () => {
        const wrapper = mount(Icon, { props: { name: "does-not-exist" } });
        expect(wrapper.find("svg").exists()).toBe(false);
    });

    it("applies an explicit pixel size", () => {
        const wrapper = mount(Icon, { props: { name: "bell", size: 18 } });
        expect(wrapper.find("svg").attributes("width")).toBe("18");
    });
});
