import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Avatar from "@/components/atoms/Avatar.vue";

describe("Avatar", () => {
    it("derives initials from the first two words of the name", () => {
        const wrapper = mount(Avatar, { props: { name: "Maciej Kowalczyk" } });
        expect(wrapper.text()).toBe("MK");
    });

    it("derives a single initial from a one-word name", () => {
        const wrapper = mount(Avatar, { props: { name: "Anna" } });
        expect(wrapper.text()).toBe("A");
    });

    it("accepts an explicit initials override", () => {
        const wrapper = mount(Avatar, {
            props: { name: "Anna Kowalska", text: "AS" },
        });
        expect(wrapper.text()).toBe("AS");
    });
});
