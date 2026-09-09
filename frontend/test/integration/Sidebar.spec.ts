import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Sidebar from "@/components/organisms/Sidebar.vue";

const groups = [
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
];

describe("Sidebar navigation", () => {
    it("marks the current section's nav item aria-current=page and data-active=true", () => {
        const wrapper = mount(Sidebar, {
            props: {
                groups,
                current: "events",
                workspace: {
                    name: "aureashop.pl",
                    meta: "Plan Pro · 3 strony",
                    mark: "AS",
                },
                user: {
                    name: "Maciej Kowalczyk",
                    email: "maciej@aureashop.pl",
                },
            },
        });

        const active = wrapper.find('.nav-item[data-route="events"]');
        expect(active.attributes("aria-current")).toBe("page");
        expect(active.attributes("data-active")).toBe("true");

        const inactive = wrapper.find('.nav-item[data-route="dashboard"]');
        expect(inactive.attributes("aria-current")).toBeUndefined();
        expect(inactive.attributes("data-active")).toBe("false");
    });

    it("keeps a detail route's index section highlighted (customer_show -> customers)", () => {
        const wrapper = mount(Sidebar, {
            props: {
                groups,
                current: "customers",
                workspace: {
                    name: "aureashop.pl",
                    meta: "Plan Pro · 3 strony",
                    mark: "AS",
                },
                user: {
                    name: "Maciej Kowalczyk",
                    email: "maciej@aureashop.pl",
                },
            },
        });

        expect(
            wrapper
                .find('.nav-item[data-route="customers"]')
                .attributes("aria-current"),
        ).toBe("page");
    });

    it("renders the user's identity in the sidebar footer", () => {
        const wrapper = mount(Sidebar, {
            props: {
                groups,
                current: "dashboard",
                workspace: {
                    name: "aureashop.pl",
                    meta: "Plan Pro · 3 strony",
                    mark: "AS",
                },
                user: { name: "Anna", email: "anna@aureashop.pl" },
            },
        });

        expect(wrapper.find(".sb-foot").text()).toContain("Anna");
        expect(wrapper.find(".sb-foot").text()).toContain("anna@aureashop.pl");
    });
});
