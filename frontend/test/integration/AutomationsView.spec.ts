import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import AutomationsView from "@/views/AutomationsView.vue";

const PAYLOAD = {
    filters: [
        {
            label: "Wszystkie",
            icon: "grid",
            count: "6",
            active: true,
            action: "set-automation-status",
            payload: "all",
        },
        {
            label: "Aktywne",
            icon: null,
            count: "4",
            active: false,
            action: "set-automation-status",
            payload: "active",
        },
        {
            label: "Wstrzymane",
            icon: null,
            count: "1",
            active: false,
            action: "set-automation-status",
            payload: "paused",
        },
        {
            label: "Szkice",
            icon: null,
            count: "1",
            active: false,
            action: "set-automation-status",
            payload: "draft",
        },
    ],
    automations: [
        {
            title: "Powrót do porzuconego koszyka",
            chips: [
                { label: "Aktywna", tone: "good" },
                { label: "Porzucenie koszyka", tone: "accent" },
                { label: "email", tone: "brown" },
                { label: "popup", tone: "brown" },
            ],
            metrics: [
                { value: "1 287", label: "uruchomień (7d)", width: 90 },
                {
                    value: "18,4%",
                    label: "konwersja",
                    width: 80,
                    color: "var(--good)",
                },
                { value: "24 800 zł", label: "przychód (7d)", width: 110 },
            ],
            action: "go-automation",
            payload: "a1",
        },
        ...Array.from({ length: 5 }, (_, i) => ({
            title: `Automatyzacja ${i + 2}`,
            chips: [{ label: "Aktywna", tone: "good" }],
            metrics: [
                { value: "0", label: "uruchomień (7d)", width: 90 },
                {
                    value: "—",
                    label: "konwersja",
                    width: 80,
                    color: "var(--fg-muted)",
                },
                { value: "—", label: "przychód (7d)", width: 110 },
            ],
            action: "go-automation",
            payload: `a${i + 2}`,
        })),
    ],
};

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(AutomationsView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B26 the automations index matches the oracle: 6 cards, 4 filter chips", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale-scoped payload with no status by default", async () => {
        await mountAt("/pl/automations");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/automations",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("forwards ?status= to the API call", async () => {
        await mountAt("/pl/automations?status=active");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/automations?status=active",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders 6 cards and 4 filter chips", async () => {
        const wrapper = await mountAt("/pl/automations");

        expect(wrapper.findAll(".auto-card")).toHaveLength(6);
        expect(wrapper.findAll(".filter-chip")).toHaveLength(4);
    });

    it("the first card shows the oracle's title, status chip and money metric", async () => {
        const wrapper = await mountAt("/pl/automations");

        const first = wrapper.findAll(".auto-card")[0];
        expect(first?.find(".auto-card__title").text()).toBe(
            "Powrót do porzuconego koszyka",
        );
        expect(first?.find(".chip").text()).toBe("Aktywna");
        expect(first?.findAll(".auto-card__num").at(-1)?.text()).toContain(
            "zł",
        );
    });

    it("wires each card to the go-automation intent with its own id", async () => {
        const wrapper = await mountAt("/pl/automations");

        const first = wrapper.findAll(".auto-card")[0];
        expect(first?.attributes("data-action")).toBe("go-automation");
        expect(first?.attributes("data-payload")).toBe("a1");
    });

    it("renders the 'Nowa automatyzacja' link to /automations/new", async () => {
        const wrapper = await mountAt("/pl/automations");

        const link = wrapper.find(".page-actions a.btn.primary");
        expect(link.exists()).toBe(true);
        expect(link.attributes("href")).toBe("/pl/automations/new");
    });
});
