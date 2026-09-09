import { flushPromises, mount } from "@vue/test-utils";
import { createPinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import AppLayout from "@/layouts/AppLayout.vue";
import { routes } from "@/router/routes";

/**
 * Repair-1 (R1-A): B03 "detail routes keep their index section active" was only ever proved at
 * the Sidebar-unit level (Sidebar.spec.ts passes `current: "customers"` as a hand-written prop),
 * never through the real chain a browser actually exercises: router → AppLayout reading
 * route.name → the shell store's GET /api/v1/{locale}/shell?route= call → the response's
 * currentSection → Sidebar's `current` prop. This mounts the real AppLayout + Sidebar with the
 * real router already navigated to a customer profile URL, and a stubbed shell API standing in
 * for CustomersController's document route (the API contract, not the document — see
 * CustomersApiIT's B03 case for the server side of this same behaviour).
 */
const SHELL_PAYLOAD_FOR_CUSTOMER_SHOW = {
    locale: "pl",
    theme: "light",
    sidebar: "expanded",
    navGroups: [
        {
            label: "Główne",
            items: [
                {
                    label: "Pulpit",
                    icon: "dashboard",
                    route: "dashboard",
                    href: "/pl/dashboard",
                },
                {
                    label: "Strumień zdarzeń",
                    icon: "activity",
                    route: "events",
                    href: "/pl/events",
                    badge: "·",
                },
                {
                    label: "Klienci",
                    icon: "users",
                    route: "customers",
                    href: "/pl/customers",
                },
            ],
        },
    ],
    // The server derives this from NavigationCatalog.currentSection("customer_show") ==
    // "customers" (INDEX_OF_DETAIL) — this stub stands in for that real response shape.
    currentSection: "customers",
    crumb: "Klienci",
    workspace: {
        name: "aureashop.pl",
        meta: "Plan Pro · 3 strony",
        mark: "AS",
    },
    user: { name: "Maciej Kowalczyk", email: "maciej@aureashop.pl" },
};

async function mountAppLayoutAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(AppLayout, {
        global: { plugins: [router, createPinia(), i18n] },
        slots: { default: "<div>page body</div>" },
    });
    await flushPromises();
    return wrapper;
}

describe("B03 detail routes keep their index section active (integration)", () => {
    beforeEach(() => {
        document.documentElement.lang = "pl";
        document.documentElement.dataset.theme = "light";
        document.documentElement.dataset.sidebar = "expanded";
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(SHELL_PAYLOAD_FOR_CUSTOMER_SHOW)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("requests the shell with route=customer_show for /pl/customers/c_1001", async () => {
        await mountAppLayoutAt("/pl/customers/c_1001");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/shell?route=customer_show",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("highlights the customers nav item from the fetched currentSection, not a hand-written prop", async () => {
        const wrapper = await mountAppLayoutAt("/pl/customers/c_1001");

        const customersItem = wrapper.find('.nav-item[data-route="customers"]');
        expect(customersItem.attributes("data-active")).toBe("true");
        expect(customersItem.attributes("aria-current")).toBe("page");

        const dashboardItem = wrapper.find('.nav-item[data-route="dashboard"]');
        expect(dashboardItem.attributes("data-active")).toBe("false");
        expect(dashboardItem.attributes("aria-current")).toBeUndefined();
    });

    it("the same chain still highlights the dashboard item on the (non-detail) dashboard route", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () =>
                Response.json({
                    ...SHELL_PAYLOAD_FOR_CUSTOMER_SHOW,
                    currentSection: "dashboard",
                    crumb: "Pulpit",
                }),
            ),
        );

        const wrapper = await mountAppLayoutAt("/pl/dashboard");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/shell?route=dashboard",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
        expect(
            wrapper
                .find('.nav-item[data-route="dashboard"]')
                .attributes("data-active"),
        ).toBe("true");
        expect(
            wrapper
                .find('.nav-item[data-route="customers"]')
                .attributes("data-active"),
        ).toBe("false");
    });
});
