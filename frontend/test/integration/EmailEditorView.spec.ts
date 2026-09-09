import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import EmailEditorView from "@/views/EmailEditorView.vue";

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

const VARIABLES = [
    { token: "{{customer.first_name}}", description: "Imię klienta" },
    { token: "{{cart.value}}", description: "Wartość koszyka" },
];

const SECTIONS = [
    {
        type: "hero",
        kicker: "AUREASHOP · ZIELONE HERBATY",
        title: "Hania, Twój koszyk czeka.",
        body: "Zostawiłaś u nas <strong>2 produkty</strong> warte <strong>88,90 zł</strong>.",
    },
    {
        type: "coupon",
        code: "WROCMY-A8F2",
        note: "Ważny do 14 maja 2026, 23:59",
        cta: "Wróć do koszyka →",
        href: "#",
    },
    {
        type: "products",
        kicker: "W TWOIM KOSZYKU",
        items: [
            { name: "Zielona herbata Sencha 100g", price: "38,90 zł" },
            { name: "Filiżanka porcelanowa Nora", price: "50,00 zł" },
        ],
    },
    {
        type: "footer",
        body: 'Dostajesz tę wiadomość. <a href="#">Wypisz się</a>',
    },
];

const SELECTED_BLOCK = {
    blockName: "Hero — tytuł + opis",
    blockId: "hero_1",
    title: "Hania, Twój koszyk czeka.",
    placeholders: [
        "{{customer.first_name}}, Twój koszyk czeka.",
        "Twój koszyk czeka.",
    ],
    backgrounds: ["oklch(0.94 0.02 85)", "oklch(0.90 0.04 150)"],
    alignment: "center",
    padding: "36px 32px",
    visibility: "customer.has_orders > 0",
};

function payloadFor(id: string) {
    return {
        template:
            id === "k1"
                ? {
                      id: "k1",
                      name: "Powrót do koszyka — wariant A",
                      meta: "Szablon wyzwalany · 3 produkty placeholders · 412 wysyłek (7d)",
                      subject: "Hania, Twój koszyk czeka — wróć i odbierz −10%",
                      sender: "sklep@aureashop.pl",
                  }
                : {
                      id,
                      name: "Nowy szablon email",
                      meta: "Szkic · nigdy nie wysłany",
                      subject: "Temat wiadomości",
                      sender: "sklep@aureashop.pl",
                  },
        blocks: BLOCKS,
        variables: VARIABLES,
        sections: SECTIONS,
        selectedBlock: SELECTED_BLOCK,
    };
}

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(EmailEditorView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B28 the e-mail editor matches the oracle: library, envelope, document, inspector", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async (input: string) => {
                const id = input.split("/").pop() ?? "new";
                return Response.json(payloadFor(id));
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and id-scoped payload for a known campaign", async () => {
        await mountAt("/pl/emails/k1");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/emails/k1",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it('fetches the "new" id for the blank-draft route', async () => {
        await mountAt("/pl/emails/new");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/emails/new",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("B01/B28 renders the back link, title and 10 block-library buttons", async () => {
        const wrapper = await mountAt("/pl/emails/k1");

        const back = wrapper.find(".page-head a.btn.ghost");
        expect(back.attributes("href")).toBe("/pl/campaigns");
        expect(wrapper.find(".page-title").text()).toBe(
            "Powrót do koszyka — wariant A",
        );
        expect(wrapper.findAll(".ee-block-lib button")).toHaveLength(10);
    });

    it("renders the document: hero title, coupon code, 2+2 products, footer copy", async () => {
        const wrapper = await mountAt("/pl/emails/k1");

        expect(wrapper.find(".ee-canvas-wrap").text()).toContain(
            "Hania, Twój koszyk czeka",
        );
        expect(wrapper.find(".ee-doc .doc-hero").exists()).toBe(true);
        expect(wrapper.find(".ee-doc .coupon-code").text()).toBe("WROCMY-A8F2");
        expect(wrapper.findAll(".ee-doc .doc-prod .item")).toHaveLength(2);
        expect(wrapper.find(".ee-doc .doc-foot").text()).toContain(
            "Wypisz się",
        );
    });

    it("renders the inspector with the selected block's title as the hero_title value", async () => {
        const wrapper = await mountAt("/pl/emails/k1");

        expect(wrapper.find(".email-right").text()).toContain(
            "Warunki widoczności",
        );
        const titleInput = wrapper.find<HTMLInputElement>(
            'input[name="hero_title"]',
        );
        expect(titleInput.element.value).toBe("Hania, Twój koszyk czeka.");
    });

    it('B01/B28 the "new" route shows the blank draft template', async () => {
        const wrapper = await mountAt("/pl/emails/new");

        expect(wrapper.find(".page-title").text()).toBe("Nowy szablon email");
    });

    it("dragging a library block onto the canvas never issues a network request (DEV-7)", async () => {
        const wrapper = await mountAt("/pl/emails/k1");
        const fetchCallsBeforeDrop = (fetch as ReturnType<typeof vi.fn>).mock
            .calls.length;

        const source = wrapper.find(".ee-block-lib button").element;
        const canvas = wrapper.find(".ee-canvas-wrap").element;
        const dataTransfer = { setData: vi.fn(), getData: () => "hero" };
        source.dispatchEvent(
            Object.assign(new Event("dragstart", { bubbles: true }), {
                dataTransfer,
            }),
        );
        canvas.dispatchEvent(
            Object.assign(
                new Event("drop", { bubbles: true, cancelable: true }),
                {
                    dataTransfer,
                },
            ),
        );
        await flushPromises();

        expect(dataTransfer.setData).toHaveBeenCalledWith("text/plain", "hero");
        expect((fetch as ReturnType<typeof vi.fn>).mock.calls.length).toBe(
            fetchCallsBeforeDrop,
        );
    });
});
