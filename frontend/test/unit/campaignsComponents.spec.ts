import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import BlockLibrary from "@/components/organisms/BlockLibrary.vue";
import CouponCode from "@/components/molecules/CouponCode.vue";

const BLOCKS = [
    { icon: "layout", label: "Nagłówek", type: "hero" },
    { icon: "list", label: "Tekst", type: "text" },
    { icon: "eye", label: "Obraz", type: "image" },
    { icon: "cart", label: "Produkty", type: "products" },
    { icon: "coupon", label: "Kupon", type: "coupon" },
    { icon: "play", label: "Przycisk CTA", type: "cta" },
    { icon: "users", label: "Recenzje", type: "reviews" },
    { icon: "minus", label: "Separator", type: "divider" },
    { icon: "mail", label: "Stopka", type: "footer" },
    { icon: "code", label: "HTML własny", type: "html" },
];

describe("B28 BlockLibrary", () => {
    it("renders one draggable button per block, in order, with its own data-block-type", () => {
        const wrapper = mount(BlockLibrary, { props: { blocks: BLOCKS } });

        const buttons = wrapper.findAll(".ee-block-lib button");
        expect(buttons).toHaveLength(10);
        expect(buttons[0]?.attributes("draggable")).toBe("true");
        expect(buttons[0]?.attributes("data-block-type")).toBe("hero");
        expect(buttons[0]?.text()).toBe("Nagłówek");
        expect(buttons[9]?.attributes("data-block-type")).toBe("html");
        expect(buttons[9]?.text()).toBe("HTML własny");
    });
});

describe("B28 CouponCode", () => {
    it("renders the code and, when given one, the note below it", () => {
        const wrapper = mount(CouponCode, {
            props: {
                code: "WROCMY-A8F2",
                note: "Ważny do 14 maja 2026, 23:59",
            },
        });

        expect(wrapper.find(".coupon-code").text()).toBe("WROCMY-A8F2");
        expect(wrapper.text()).toContain("Ważny do 14 maja 2026, 23:59");
    });

    it("omits the note entirely when none is given", () => {
        const wrapper = mount(CouponCode, { props: { code: "WROCMY-A8F2" } });

        expect(wrapper.find(".coupon-code").text()).toBe("WROCMY-A8F2");
        expect(wrapper.text()).toBe("WROCMY-A8F2");
    });
});
