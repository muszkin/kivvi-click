import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import PopupEditorView from "@/views/PopupEditorView.vue";

const TYPES = [
    { id: "modal", label: "Modal", icon: "layout" },
    { id: "slide-in", label: "Slide-in", icon: "arrow_right" },
    { id: "banner", label: "Pasek", icon: "minus" },
    { id: "fullscreen", label: "Pełny ekran", icon: "grid" },
    { id: "toast", label: "Toast", icon: "bell" },
];

const BLOCKS = [
    { icon: "list", label: "Nagłówek", type: "heading" },
    { icon: "list", label: "Tekst", type: "text" },
    { icon: "eye", label: "Obraz", type: "image" },
    { icon: "mail", label: "Pole e-mail", type: "email-field" },
    { icon: "user", label: "Pole tekstowe", type: "text-field" },
    { icon: "play", label: "Przycisk CTA", type: "cta" },
    { icon: "coupon", label: "Kod kuponu", type: "coupon" },
    { icon: "cart", label: "Karuzela produktów", type: "carousel" },
    { icon: "check", label: "Checkbox zgody", type: "consent" },
    { icon: "minus", label: "Licznik czasu", type: "countdown" },
];

const VARIABLES = [
    "{{customer.first_name}}",
    "{{cart.value}}",
    "{{coupon.code}}",
    "{{product.last_viewed}}",
];

const TRIGGERS = [
    { label: "exit intent", tone: "accent", note: "kursor opuszcza okno" },
    { label: "czas na stronie", value: "20", unit: "sek." },
    { label: "scroll", value: "60", unit: "% strony" },
];

const AUDIENCE = [
    { label: "Tylko niezalogowani", checked: true },
    {
        label: 'Nie widzieli w ostatnich <span class="mono">14 dniach</span>',
        checked: true,
    },
    { label: "Tylko ruch z kampanii płatnych", checked: false },
    { label: "Pomiń, jeśli koszyk jest pusty", checked: true },
];

const ACCENT_COLORS = [
    "oklch(0.42 0.06 150)",
    "oklch(0.55 0.07 55)",
    "oklch(0.52 0.12 32)",
    "oklch(0.22 0.02 150)",
];

function payloadFor(id: string, type: string | null, device: string | null) {
    const known = id === "p1";
    const resolvedType = type ?? (known ? "modal" : "modal");
    const content =
        resolvedType === "banner"
            ? {
                  type: "banner",
                  kicker: "DARMOWA DOSTAWA",
                  title: "Od 199 zł wysyłamy na nasz koszt",
                  cta: "Do zakupów →",
              }
            : {
                  type: resolvedType,
                  kicker: "CZEKAJ —",
                  title: "Zostań na 10% taniej",
                  body: "Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie. Trwa to 30 sekund.",
                  placeholder: "twoj@email.pl",
                  cta: "Wyślij mi kupon →",
                  fine: "Bez spamu. Wypisujesz się w 1 kliknięciu.",
              };
    return {
        widget: known
            ? {
                  id: "p1",
                  name: "Exit intent — 10% rabatu",
                  meta: "Aktywny na aureashop.pl · 48 210 wyświetleń · 7,4% konwersji",
                  type: resolvedType,
                  content,
              }
            : {
                  id,
                  name: "Nowy widget",
                  meta: "Szkic · nieopublikowany",
                  type: resolvedType,
                  content,
              },
        device: device === "mobile" ? "mobile" : "desktop",
        types: TYPES,
        blocks: BLOCKS,
        variables: VARIABLES,
        triggers: TRIGGERS,
        audience: AUDIENCE,
        accentColors: ACCENT_COLORS,
        viewport: device === "mobile" ? "390 × 844" : "1440 × 900",
    };
}

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(PopupEditorView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B29 the popup editor matches the oracle: type list, stage, inspector", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async (input: string) => {
                const url = new URL(input, "https://example.test");
                const id = url.pathname.split("/").pop() ?? "new";
                return Response.json(
                    payloadFor(
                        id,
                        url.searchParams.get("type"),
                        url.searchParams.get("device"),
                    ),
                );
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and id-scoped payload for a known widget", async () => {
        await mountAt("/pl/popups/p1");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/popups/p1",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it('fetches the "new" id for the blank-draft route', async () => {
        await mountAt("/pl/popups/new");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/popups/new",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards ?type= and ?device= to the API call", async () => {
        await mountAt("/pl/popups/p1?type=banner&device=mobile");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/popups/p1?type=banner&device=mobile",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("B01/B29 renders the back link, title and 5 widget-type links", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        const back = wrapper.find(".page-head a.btn.ghost");
        expect(back.attributes("href")).toBe("/pl/popups");
        expect(wrapper.find(".page-title").text()).toBe(
            "Exit intent — 10% rabatu",
        );
        expect(wrapper.findAll(".pw-type")).toHaveLength(5);
    });

    it("previews the widget on the mock storefront: modal, desktop, 1440 × 900", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        expect(wrapper.find(".pw-stage").attributes("data-type")).toBe("modal");
        expect(wrapper.find(".pw-stage").attributes("data-device")).toBe(
            "desktop",
        );
        expect(wrapper.find(".pw.pw--modal .pw-title").text()).toBe(
            "Zostań na 10% taniej",
        );
        expect(wrapper.find(".shop-mock").exists()).toBe(true);
        const monoChips = wrapper.findAll(".chip.mono");
        expect(monoChips[1]?.text()).toBe("1440 × 900");
    });

    it("each widget-type link points at this same route with only ?type= set", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        const bannerLink = wrapper
            .findAll(".pw-type")
            .find((link) => link.text() === "Pasek");
        expect(bannerLink?.attributes("href")).toBe(
            "/pl/popups/p1?type=banner",
        );
        const modalLink = wrapper
            .findAll(".pw-type")
            .find((link) => link.text() === "Modal");
        expect(modalLink?.attributes("data-active")).toBe("true");
    });

    it("the device switch links at this same route with only ?device= set", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        const mobileOption = wrapper
            .findAll(".seg [data-action='navigate']")
            .find((el) => el.text().includes("Mobile"));
        expect(mobileOption?.attributes("data-payload")).toBe(
            "/pl/popups/p1?device=mobile",
        );
    });

    it("?type=banner reshapes the preview", async () => {
        const wrapper = await mountAt("/pl/popups/p1?type=banner");

        expect(wrapper.find(".pw-stage").attributes("data-type")).toBe(
            "banner",
        );
        expect(wrapper.find(".pw--banner")).toBeTruthy();
        expect(wrapper.find(".pw-banner-row .pw-title").text()).toContain(
            "Od 199 zł",
        );
    });

    it("?device=mobile switches the viewport readout", async () => {
        const wrapper = await mountAt("/pl/popups/p1?device=mobile");

        expect(wrapper.find(".pw-stage").attributes("data-device")).toBe(
            "mobile",
        );
        const monoChips = wrapper.findAll(".chip.mono");
        expect(monoChips[1]?.text()).toBe("390 × 844");
    });

    it("the inspector carries 3 triggers, a 9-cell position grid with 1 checked, and 4 checkbox rows", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        const inspector = wrapper.find(".email-right");
        expect(inspector.findAll(".trig-row")).toHaveLength(3);
        expect(inspector.findAll(".pos-grid .pos-cell")).toHaveLength(9);
        expect(
            inspector.findAll('.pos-cell[aria-checked="true"]'),
        ).toHaveLength(1);
        expect(inspector.findAll(".checkbox-row")).toHaveLength(4);
    });

    it("the audience rule with embedded markup renders through v-html", async () => {
        const wrapper = await mountAt("/pl/popups/p1");

        expect(wrapper.find(".email-right").html()).toContain(
            '<span class="mono">14 dniach</span>',
        );
    });

    it('B01/B29 the "new" route shows the blank-draft widget', async () => {
        const wrapper = await mountAt("/pl/popups/new");

        expect(wrapper.find(".page-title").text()).toBe("Nowy widget");
    });

    it("dragging a library block onto the canvas never issues a network request (DEV-7)", async () => {
        const wrapper = await mountAt("/pl/popups/p1");
        const fetchCallsBeforeDrop = (fetch as ReturnType<typeof vi.fn>).mock
            .calls.length;

        const source = wrapper.find(".ee-block-lib button").element;
        const canvas = wrapper.find(".ee-canvas-wrap").element;
        const dataTransfer = { setData: vi.fn(), getData: () => "heading" };
        source.dispatchEvent(
            Object.assign(new Event("dragstart", { bubbles: true }), {
                dataTransfer,
            }),
        );
        canvas.dispatchEvent(
            Object.assign(
                new Event("drop", { bubbles: true, cancelable: true }),
                { dataTransfer },
            ),
        );
        await flushPromises();

        expect(dataTransfer.setData).toHaveBeenCalledWith(
            "text/plain",
            "heading",
        );
        expect((fetch as ReturnType<typeof vi.fn>).mock.calls.length).toBe(
            fetchCallsBeforeDrop,
        );
    });
});
