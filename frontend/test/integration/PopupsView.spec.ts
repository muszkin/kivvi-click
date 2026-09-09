import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import PopupsView from "@/views/PopupsView.vue";

function cardFor(
    id: string,
    title: string,
    type: string,
    impressions: string,
    conversion: string,
) {
    return {
        title,
        chips: [
            { label: "Aktywny", tone: "good" },
            { label: type, tone: "brown" },
            { label: "na wszystkich stronach" },
            { label: "exit intent" },
        ],
        metrics: [
            { value: impressions, label: "wyświetleń", width: 90 },
            {
                value: conversion,
                label: "konwersja",
                width: 80,
                color: "var(--good)",
            },
        ],
        action: "go-popup",
        payload: id,
    };
}

const CARDS = [
    cardFor("p1", "Exit intent — 10% rabatu", "Modal", "48 210", "7,4%"),
    cardFor(
        "p2",
        "Pasek darmowej dostawy od 199 zł",
        "Pasek banner",
        "182 400",
        "2,1%",
    ),
];

function selectedFor(id: string, name: string, type: string) {
    return {
        id,
        name,
        type,
        content:
            type === "banner"
                ? {
                      type: "banner",
                      kicker: "DARMOWA DOSTAWA",
                      title: "Od 199 zł wysyłamy na nasz koszt",
                      cta: "Do zakupów →",
                  }
                : {
                      type,
                      kicker: "CZEKAJ —",
                      title: "Zostań na 10% taniej",
                      body: "Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie. Trwa to 30 sekund.",
                      placeholder: "twoj@email.pl",
                      cta: "Wyślij mi kupon →",
                      fine: "Bez spamu. Wypisujesz się w 1 kliknięciu.",
                  },
    };
}

const TYPES = [
    { id: "modal", label: "Modal", icon: "layout" },
    { id: "slide-in", label: "Slide-in", icon: "arrow_right" },
    { id: "banner", label: "Pasek", icon: "minus" },
    { id: "fullscreen", label: "Pełny ekran", icon: "grid" },
    { id: "toast", label: "Toast", icon: "bell" },
];

function payloadFor(preview: string | null) {
    return {
        cards: CARDS,
        selected:
            preview === "p2"
                ? selectedFor(
                      "p2",
                      "Pasek darmowej dostawy od 199 zł",
                      "banner",
                  )
                : selectedFor("p1", "Exit intent — 10% rabatu", "modal"),
        types: TYPES,
    };
}

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(PopupsView, { global: { plugins: [router, i18n] } });
    await flushPromises();
    return wrapper;
}

describe("B29 the popup index matches the oracle: 5 cards, a live preview", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async (input: string) => {
                const url = new URL(input, "https://example.test");
                return Response.json(
                    payloadFor(url.searchParams.get("preview")),
                );
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale-scoped payload with no preview by default", async () => {
        await mountAt("/pl/popups");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/popups",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards ?preview= to the API call", async () => {
        await mountAt("/pl/popups?preview=p2");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/popups?preview=p2",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders one .auto-card per widget, wired to a real ?preview= navigation", async () => {
        const wrapper = await mountAt("/pl/popups");

        const cards = wrapper.findAll(".auto-card");
        expect(cards).toHaveLength(2);
        expect(cards[0]?.attributes("data-action")).toBe("navigate");
        expect(cards[0]?.attributes("data-payload")).toBe(
            "/pl/popups?preview=p1",
        );
        expect(cards[1]?.attributes("data-payload")).toBe(
            "/pl/popups?preview=p2",
        );
    });

    it("each card carries an eye-preview link and an edit link", async () => {
        const wrapper = await mountAt("/pl/popups");

        const first = wrapper.findAll(".auto-card")[0];
        const links = first?.findAll("a.btn.ghost");
        expect(links?.[0]?.attributes("href")).toBe("/pl/popups?preview=p1");
        expect(links?.[1]?.attributes("href")).toBe("/pl/popups/p1");
    });

    it("previews the selected widget on the popup stage, type=modal by default", async () => {
        const wrapper = await mountAt("/pl/popups");

        expect(wrapper.find(".pw-stage").attributes("data-type")).toBe("modal");
        expect(wrapper.find(".pw-stage").attributes("data-device")).toBe(
            "desktop",
        );
        expect(wrapper.find(".pw-title").text()).toBe("Zostań na 10% taniej");
    });

    it("?preview=p2 shows the banner widget's own shape", async () => {
        const wrapper = await mountAt("/pl/popups?preview=p2");

        expect(wrapper.find(".pw-stage").attributes("data-type")).toBe(
            "banner",
        );
        expect(wrapper.find(".pw--banner").exists()).toBe(true);
    });

    it('the section-title reads "Podgląd: <name>" and links Edytuj to the edit route', async () => {
        const wrapper = await mountAt("/pl/popups");

        expect(wrapper.find(".section-title").text()).toContain(
            "Podgląd: Exit intent — 10% rabatu",
        );
        const editLink = wrapper.find(".section-title a.btn");
        expect(editLink.attributes("href")).toBe("/pl/popups/p1");
    });

    it("renders the trigger-summary card below the stage", async () => {
        const wrapper = await mountAt("/pl/popups");

        expect(wrapper.text()).toContain("Pokazuje się gdy");
        expect(wrapper.text()).toContain("14 dniach");
        expect(wrapper.findAll(".card .chip")).toHaveLength(2);
    });

    it('renders the "Nowy popup" link to /popups/new', async () => {
        const wrapper = await mountAt("/pl/popups");

        const link = wrapper.find(".page-actions a.btn.primary");
        expect(link.exists()).toBe(true);
        expect(link.attributes("href")).toBe("/pl/popups/new");
    });
});
