import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import PublicLayout from "@/layouts/PublicLayout.vue";
import PrivacyView from "@/views/PrivacyView.vue";

/**
 * The privacy policy page (PIO-70, decision D3): it has to resolve in both languages from the
 * route table, render its own copy rather than the fallback locale's, and be reachable from the
 * footer link that used to be href="#".
 */
async function mountAtRoute(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(
        { template: "<router-view />" },
        { global: { plugins: [router, i18n] } },
    );
    return { wrapper, router };
}

describe("PIO-70 the privacy policy page", () => {
    afterEach(() => {
        i18n.global.locale.value = "pl";
    });

    it("resolves '/pl/privacy' to the privacy view", async () => {
        const { wrapper, router } = await mountAtRoute("/pl/privacy");

        expect(router.currentRoute.value.name).toBe("privacy");
        expect(wrapper.findComponent(PrivacyView).exists()).toBe(true);
    });

    it("renders the Polish policy with a heading for every section", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        expect(wrapper.find("h1").text()).toBe("Polityka prywatności");
        const headings = wrapper
            .findAll(".legal-section h2")
            .map((h) => h.text());
        expect(headings).toContain("Kto jest administratorem");
        expect(headings).toContain("Jak wycofać zgodę");
        expect(headings.length).toBeGreaterThanOrEqual(8);
    });

    it("names the data it actually collects, so the page matches what the form stores", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        expect(text).toContain("adres IP");
        expect(text).toContain("user-agent");
        expect(text).toContain("klauzuli zgody");
    });

    it("renders the English policy on '/en/privacy'", async () => {
        i18n.global.locale.value = "en";
        const { wrapper } = await mountAtRoute("/en/privacy");

        expect(wrapper.find("h1").text()).toBe("Privacy policy");
        const headings = wrapper
            .findAll(".legal-section h2")
            .map((h) => h.text());
        expect(headings).toContain("Who the controller is");
        expect(headings.length).toBeGreaterThanOrEqual(8);
    });

    it("'/de/privacy' is unknown, like every other unsupported locale prefix", async () => {
        const router = createRouter({ history: createWebHistory(), routes });

        expect(router.resolve("/de/privacy").matched).toHaveLength(0);
    });

    it("the public footer links to it instead of the old href='#'", async () => {
        const router = createRouter({ history: createWebHistory(), routes });
        await router.push("/pl");
        await router.isReady();
        const wrapper = mount(PublicLayout, {
            global: { plugins: [router, i18n] },
        });

        const privacyLink = wrapper
            .findAll(".landing-foot a")
            .find((a) => a.text() === "Prywatność");
        expect(privacyLink?.attributes("href")).toBe("/pl/privacy");
    });
});
