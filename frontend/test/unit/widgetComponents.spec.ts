import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import PopupStage from "@/components/organisms/PopupStage.vue";
import PopupWidget from "@/components/organisms/PopupWidget.vue";
import PwTypeList from "@/components/organisms/PwTypeList.vue";
import ShopMock from "@/components/organisms/ShopMock.vue";
import PositionGrid from "@/components/molecules/PositionGrid.vue";
import SwatchGrid from "@/components/molecules/SwatchGrid.vue";
import TriggerRow from "@/components/molecules/TriggerRow.vue";
import AddSlot from "@/components/molecules/AddSlot.vue";
import { i18n } from "@/i18n";

describe("B29 PopupStage", () => {
    it("carries data-type and data-device, and renders the shop mock plus the slotted widget", () => {
        const wrapper = mount(PopupStage, {
            props: { type: "modal", device: "desktop" },
            slots: { default: '<div class="pw pw--modal">widget</div>' },
        });

        expect(wrapper.find(".pw-stage").attributes("data-type")).toBe("modal");
        expect(wrapper.find(".pw-stage").attributes("data-device")).toBe(
            "desktop",
        );
        expect(wrapper.find(".shop-mock").exists()).toBe(true);
        expect(wrapper.find(".pw-dim").exists()).toBe(true);
        expect(wrapper.find(".pw--modal").exists()).toBe(true);
    });

    it("defaults device to desktop when omitted", () => {
        const wrapper = mount(PopupStage, { props: { type: "banner" } });

        expect(wrapper.find(".pw-stage").attributes("data-device")).toBe(
            "desktop",
        );
    });
});

describe("B29 ShopMock", () => {
    it("renders the static, aria-hidden storefront backdrop", () => {
        const wrapper = mount(ShopMock);

        expect(wrapper.find(".shop-mock").attributes("aria-hidden")).toBe(
            "true",
        );
        expect(wrapper.findAll(".shop-mock__nav")).toHaveLength(3);
        expect(wrapper.findAll(".shop-mock__grid span")).toHaveLength(4);
    });
});

describe("B29 PopupWidget", () => {
    it("renders the modal shape with title, body and CTA form", () => {
        const wrapper = mount(PopupWidget, {
            props: {
                type: "modal",
                kicker: "CZEKAJ —",
                title: "Zostań na 10% taniej",
                body: "Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie. Trwa to 30 sekund.",
                placeholder: "twoj@email.pl",
                cta: "Wyślij mi kupon →",
                fine: "Bez spamu. Wypisujesz się w 1 kliknięciu.",
            },
        });

        expect(wrapper.find(".pw").classes()).toContain("pw--modal");
        expect(wrapper.find(".pw-title").text()).toBe("Zostań na 10% taniej");
        expect(wrapper.find(".pw-sub").text()).toContain("newslettera");
        expect(wrapper.find(".pw-input").attributes("placeholder")).toBe(
            "twoj@email.pl",
        );
        expect(wrapper.find(".pw-cta").text()).toBe("Wyślij mi kupon →");
        expect(wrapper.find(".pw-fine").exists()).toBe(true);
        expect(wrapper.find('[role="dialog"]').attributes("aria-label")).toBe(
            "Zostań na 10% taniej",
        );
    });

    it("renders the banner shape as a kicker/title row plus an inline CTA, no form", () => {
        const wrapper = mount(PopupWidget, {
            props: {
                type: "banner",
                kicker: "DARMOWA DOSTAWA",
                title: "Od 199 zł wysyłamy na nasz koszt",
                cta: "Do zakupów →",
            },
        });

        expect(wrapper.find(".pw").classes()).toContain("pw--banner");
        expect(wrapper.find(".pw-banner-row").exists()).toBe(true);
        expect(wrapper.find(".pw-banner-row .pw-title").text()).toContain(
            "Od 199 zł",
        );
        expect(wrapper.find(".pw-form").exists()).toBe(false);
        expect(wrapper.find(".pw-cta").text()).toBe("Do zakupów →");
    });

    it("renders the toast shape as a dot plus title/body, no form", () => {
        const wrapper = mount(PopupWidget, {
            props: {
                type: "toast",
                title: "Ktoś właśnie kupił",
                body: "Zielona herbata Sencha 100g · Kraków, 4 min temu",
                cta: "Zobacz",
            },
        });

        expect(wrapper.find(".pw").classes()).toContain("pw--toast");
        expect(wrapper.find(".pw-toast-dot").exists()).toBe(true);
        expect(wrapper.text()).toContain("Ktoś właśnie kupił");
        expect(wrapper.find(".pw-form").exists()).toBe(false);
    });

    it("omits the body paragraph for slide-in even when a body string is given", () => {
        const wrapper = mount(PopupWidget, {
            props: {
                type: "slide-in",
                title: "Zapisz się",
                body: "To nie powinno się wyświetlić",
                cta: "Zapisz",
            },
        });

        expect(wrapper.find(".pw-sub").exists()).toBe(false);
    });

    it("omits the email input when no placeholder is given", () => {
        const wrapper = mount(PopupWidget, {
            props: { type: "modal", title: "Tytuł", cta: "OK" },
        });

        expect(wrapper.find(".pw-input").exists()).toBe(false);
    });
});

describe("B29 PositionGrid", () => {
    it("renders 9 cells with the centre one checked by default", () => {
        const wrapper = mount(PositionGrid);

        const cells = wrapper.findAll(".pos-cell");
        expect(cells).toHaveLength(9);
        const checked = wrapper.findAll('.pos-cell[aria-checked="true"]');
        expect(checked).toHaveLength(1);
        expect(cells[4]?.attributes("aria-checked")).toBe("true");
        expect(cells[4]?.text()).toBe("●");
        expect(wrapper.find(".pos-grid").attributes("role")).toBe("radiogroup");
        expect(wrapper.find(".pos-grid").attributes("aria-label")).toBe(
            "Pozycja widgetu",
        );
    });

    it("marks a different cell as checked when selected is overridden", () => {
        const wrapper = mount(PositionGrid, { props: { selected: 0 } });

        expect(wrapper.findAll('.pos-cell[aria-checked="true"]')).toHaveLength(
            1,
        );
        expect(
            wrapper.findAll(".pos-cell")[0]?.attributes("aria-checked"),
        ).toBe("true");
    });
});

describe("B29 SwatchGrid", () => {
    const COLORS = [
        "oklch(0.42 0.06 150)",
        "oklch(0.55 0.07 55)",
        "oklch(0.52 0.12 32)",
        "oklch(0.22 0.02 150)",
    ];

    it("renders one swatch per colour with the first pressed by default", () => {
        const wrapper = mount(SwatchGrid, {
            props: { colors: COLORS },
            global: { plugins: [i18n] },
        });

        const swatches = wrapper.findAll("button");
        expect(swatches).toHaveLength(4);
        expect(swatches[0]?.attributes("aria-pressed")).toBe("true");
        expect(swatches[0]?.attributes("aria-label")).toBe("Kolor 1");
        expect(swatches[1]?.attributes("aria-pressed")).toBe("false");
        expect(swatches[1]?.attributes("aria-label")).toBe("Kolor 2");
        expect(swatches[0]?.attributes("data-action")).toBe(
            "set-widget-accent",
        );
        expect(swatches[0]?.attributes("data-payload")).toBe("0");
    });
});

describe("B29 TriggerRow", () => {
    it("renders the qualitative trigger with a note and no numeric input", () => {
        const wrapper = mount(TriggerRow, {
            props: {
                label: "exit intent",
                tone: "accent",
                note: "kursor opuszcza okno",
            },
        });

        expect(wrapper.find(".chip").text()).toBe("exit intent");
        expect(wrapper.find(".chip").classes()).toContain("accent");
        expect(wrapper.text()).toContain("kursor opuszcza okno");
        expect(wrapper.find("input").exists()).toBe(false);
    });

    it("renders a numeric trigger with its value input and unit", () => {
        const wrapper = mount(TriggerRow, {
            props: { label: "czas na stronie", value: "20", unit: "sek." },
        });

        expect(wrapper.find(".chip").text()).toBe("czas na stronie");
        expect(wrapper.find<HTMLInputElement>("input").element.value).toBe(
            "20",
        );
        expect(wrapper.text()).toContain("sek.");
    });

    it("always renders the remove-trigger button", () => {
        const wrapper = mount(TriggerRow, { props: { label: "scroll" } });

        const button = wrapper.find('[data-action="remove-trigger"]');
        expect(button.exists()).toBe(true);
    });
});

describe("B29 AddSlot", () => {
    it("renders the icon, label and data-action", () => {
        const wrapper = mount(AddSlot, {
            props: { label: "Dodaj wyzwalacz", action: "add-trigger" },
        });

        expect(wrapper.find(".rb-add").text()).toBe("Dodaj wyzwalacz");
        expect(wrapper.find(".rb-add").attributes("data-action")).toBe(
            "add-trigger",
        );
    });
});

describe("B29 PwTypeList", () => {
    const TYPES = [
        {
            id: "modal",
            label: "Modal",
            icon: "layout",
            href: "/pl/popups/p1?type=modal",
            active: true,
        },
        {
            id: "banner",
            label: "Pasek",
            icon: "minus",
            href: "/pl/popups/p1?type=banner",
            active: false,
        },
    ];

    it("renders one real link per type, marking the active one", () => {
        const wrapper = mount(PwTypeList, { props: { types: TYPES } });

        const links = wrapper.findAll("a.pw-type");
        expect(links).toHaveLength(2);
        expect(links[0]?.attributes("href")).toBe("/pl/popups/p1?type=modal");
        expect(links[0]?.attributes("data-active")).toBe("true");
        expect(links[1]?.attributes("data-active")).toBe("false");
        expect(links[1]?.text()).toBe("Pasek");
    });
});
