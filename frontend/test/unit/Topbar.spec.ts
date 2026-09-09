import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import Topbar from "@/components/organisms/Topbar.vue";

/**
 * B02 (repair-1: no unit-level test existed for "the breadcrumb names the current section" —
 * only integration-level coverage via ShellNavigation.spec.ts/LocaleToggle.spec.ts, neither
 * reachable from `npm run test -- --run`). `ShellViewServiceTest`'s own B02 test (backend) proves
 * the server resolves the right crumb text per route; this proves the component renders whatever
 * crumb text it is given into `.crumbs .now`, verbatim — the other half of the same behaviour,
 * mirroring the old stack's `topbar.html.twig`: `<span class="now">{{ crumb }}</span>`.
 */
async function mountTopbarWithCrumb(crumb: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push("/pl/dashboard");
    await router.isReady();
    return mount(Topbar, {
        global: { plugins: [router, i18n] },
        props: {
            siteName: "aureashop.pl",
            crumb,
            locale: "pl",
            theme: "light",
        },
    });
}

describe("B02 the breadcrumb names the current section", () => {
    it("renders the given crumb text into .crumbs .now, alongside the site name", async () => {
        const wrapper = await mountTopbarWithCrumb("Feedy produktów");

        expect(wrapper.find(".crumbs .now").text()).toBe("Feedy produktów");
        expect(wrapper.find(".crumbs").text()).toContain("aureashop.pl");
    });

    it(
        "re-renders a different crumb verbatim for a different section — mutation-detectable: " +
            "a hard-coded crumb string would pass the test above alone",
        async () => {
            const wrapper = await mountTopbarWithCrumb("Ustawienia");

            expect(wrapper.find(".crumbs .now").text()).toBe("Ustawienia");
        },
    );
});
