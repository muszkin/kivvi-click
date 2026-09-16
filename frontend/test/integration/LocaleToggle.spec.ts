import { flushPromises, mount } from "@vue/test-utils";
import { createPinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import AppLayout from "@/layouts/AppLayout.vue";
import { routes } from "@/router/routes";

/**
 * Repair-3 (R-B): the settings journey's own repair-1 replaced Topbar.vue's settings-only
 * locale-toggle special case with a generic mechanism (`route.meta.defaultParams`, read by
 * `router/localeHref.ts`), and repair-1's own unit test (`localeHref.spec.ts`) already pins that
 * helper's behaviour called directly. What was still missing is the chain a browser actually
 * exercises: router → AppLayout → AppShell → Topbar's `localeHref` computed, reading
 * `useRoute()`/`useRouter()` from the SAME real router instance the app navigated with — not a
 * hand-built `RouteLocationNormalizedLoaded`-like object. Mirrors
 * CustomerDetailSidebarSection.spec.ts's mounting pattern (real AppLayout + real router + a
 * stubbed GET /api/v1/{locale}/shell standing in for the document route).
 */
const SHELL_PAYLOAD = {
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
            ],
        },
    ],
    currentSection: "settings",
    crumb: "Ustawienia",
    workspace: {
        name: "aureashop.pl",
        meta: "3 strony",
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

function localeToggleHref(
    wrapper: Awaited<ReturnType<typeof mountAppLayoutAt>>,
): string | undefined {
    return wrapper.find("a.tb-btn").attributes("href");
}

describe("repair-3 (R-B) locale toggle through the real router/AppLayout/Topbar chain", () => {
    beforeEach(() => {
        document.documentElement.lang = "pl";
        document.documentElement.dataset.theme = "light";
        document.documentElement.dataset.sidebar = "expanded";
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => Response.json(SHELL_PAYLOAD)),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("at /pl/settings/account (settings' own default tab), the toggle omits the segment: /en/settings", async () => {
        const wrapper = await mountAppLayoutAt("/pl/settings/account");

        expect(localeToggleHref(wrapper)).toBe("/en/settings");
    });

    it("at /pl/settings/gdpr (not the default tab), the toggle keeps the segment: /en/settings/gdpr", async () => {
        const wrapper = await mountAppLayoutAt("/pl/settings/gdpr");

        expect(localeToggleHref(wrapper)).toBe("/en/settings/gdpr");
    });

    it("at /pl/customers/c_1001 (a route with no meta.defaultParams), the toggle is an unmodified prefix swap: /en/customers/c_1001", async () => {
        const wrapper = await mountAppLayoutAt("/pl/customers/c_1001");

        expect(localeToggleHref(wrapper)).toBe("/en/customers/c_1001");
    });
});
