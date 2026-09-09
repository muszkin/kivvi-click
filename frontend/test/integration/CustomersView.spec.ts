import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import CustomersView from "@/views/CustomersView.vue";

function customerRow(seed: number) {
    return {
        id: `c_${1000 + seed}`,
        name: `Klient ${seed}`,
        initials: "K" + seed,
        email: `klient${seed}@example.com`,
        segment: { label: "VIP", tone: "accent" as const },
        orders: String(seed % 7),
        revenue: "49 zł",
        lastSeen: "teraz",
    };
}

const PAYLOAD = {
    subtitle: "4 218 zidentyfikowanych klientów · 7 632 anonimowych sesji",
    segments: [
        {
            label: "Wszyscy",
            icon: "users",
            count: "4 218",
            active: true,
            action: "set-segment",
            payload: "all",
        },
        {
            label: "VIP",
            icon: null,
            count: "142",
            active: false,
            action: "set-segment",
            payload: "vip",
        },
    ],
    customersRows: Array.from({ length: 24 }, (_, i) => customerRow(i)),
    page: 1,
    pages: 192,
};

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(CustomersView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B25 the customers index matches the oracle: 24 rows, 7 columns, page 1 of 192", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and page-scoped payload", async () => {
        await mountAt("/pl/customers");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/customers?page=1",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("reads ?page= from the URL", async () => {
        await mountAt("/pl/customers?page=2");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/customers?page=2",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders the subtitle and 24 rows across 7 columns", async () => {
        const wrapper = await mountAt("/pl/customers");

        expect(wrapper.find(".page-sub").text()).toContain(
            "zidentyfikowanych klientów",
        );
        expect(wrapper.findAll(".table tbody tr")).toHaveLength(24);
        expect(wrapper.findAll(".table thead th")).toHaveLength(7);
    });

    it("wires each row to the go-customer intent with the row's own id", async () => {
        const wrapper = await mountAt("/pl/customers");

        const firstRow = wrapper.findAll(".table tbody tr")[0];
        expect(firstRow?.attributes("data-action")).toBe("go-customer");
        expect(firstRow?.attributes("data-payload")).toBe("c_1000");
    });

    it("shows the pager readout and disables 'previous' on page 1", async () => {
        const wrapper = await mountAt("/pl/customers");

        expect(wrapper.find(".row.mono.muted, .mono.muted").text()).toBe(
            "Strona 1 z 192",
        );
        expect(
            wrapper
                .find('button[data-action="go-page"]')
                .attributes("disabled"),
        ).toBeDefined();
    });

    it("renders the segment rail from the payload, not hard-coded", async () => {
        const wrapper = await mountAt("/pl/customers");

        const chips = wrapper.findAll(".filter-chip");
        expect(chips).toHaveLength(2);
        expect(chips[0]?.attributes("data-active")).toBe("true");
        expect(chips[0]?.text()).toContain("Wszyscy · 4 218");
    });
});
