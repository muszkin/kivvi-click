import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import EventRow from "@/components/molecules/EventRow.vue";

const PURCHASE = {
    time: "14:42:08",
    typeIcon: "money",
    tone: "good",
    type: "Zakup",
    detail: "412,00 PLN · 4 produkty",
    customerName: "Hania Kowalska",
    customerId: "c_1001",
    siteName: "aureashop.pl",
    siteColor: "#7a8763",
};

describe("EventRow", () => {
    it("B24 renders time, icon tone, type, detail, customer and site", () => {
        const wrapper = mount(EventRow, { props: PURCHASE });

        expect(wrapper.find(".event-row__time").text()).toBe("14:42:08");
        expect(wrapper.find(".event-row__icon").classes()).toContain("good");
        expect(wrapper.find(".event-row__type").text()).toBe("Zakup");
        expect(wrapper.find(".event-row__detail").text()).toBe(
            "412,00 PLN · 4 produkty",
        );
        expect(wrapper.find(".event-row__customer").text()).toContain(
            "Hania Kowalska",
        );
        expect(wrapper.find(".event-row__site").text()).toContain(
            "aureashop.pl",
        );
    });

    it('carries data-action="go-customer" and the customer id as its payload', () => {
        const wrapper = mount(EventRow, { props: PURCHASE });

        expect(wrapper.attributes("data-action")).toBe("go-customer");
        expect(wrapper.attributes("data-payload")).toBe("c_1001");
    });

    it("omits data-action when there is no customer to link to", () => {
        const wrapper = mount(EventRow, {
            props: { ...PURCHASE, customerId: null, customerName: null },
        });

        expect(wrapper.attributes("data-action")).toBeUndefined();
        expect(wrapper.attributes("data-payload")).toBeUndefined();
        expect(wrapper.find(".event-row__customer").text()).toBe("");
    });

    it("carries the 'new' class only when isNew is set (the 800ms flash-in)", () => {
        const fresh = mount(EventRow, { props: { ...PURCHASE, isNew: true } });
        const stale = mount(EventRow, { props: PURCHASE });

        expect(fresh.classes()).toContain("new");
        expect(stale.classes()).not.toContain("new");
    });

    it("omits the site block when there is no site", () => {
        const wrapper = mount(EventRow, {
            props: { ...PURCHASE, siteName: null },
        });

        expect(wrapper.find(".event-row__site").text()).toBe("");
    });
});
