import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import PublicLayout from "@/layouts/PublicLayout.vue";

/**
 * The public shell's nav and footer (PIO-121).
 *
 * The footer used to claim the system was built with Symfony and PHP — untrue since the 2026-09-09
 * migration — and it was hard-coded straight into the template, which is why the English site
 * rendered that sentence in Polish. Both halves are asserted here: the claim itself, and the fact
 * that it now comes from the message catalogues, so neither can regress silently.
 */
async function mountLayout(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    return mount(PublicLayout, { global: { plugins: [router, i18n] } });
}

describe("PIO-121 the public nav and footer", () => {
    afterEach(() => {
        i18n.global.locale.value = "pl";
    });

    it("the Polish footer names Java and Vue 3 and the MIT licence", async () => {
        const wrapper = await mountLayout("/pl");

        const footer = wrapper.find(".landing-foot").text();
        expect(footer).toContain("Zbudowane w Polsce na Javie i Vue 3.");
        expect(footer).toContain("Kod na licencji MIT.");
    });

    it("the footer no longer claims Symfony or PHP", async () => {
        const wrapper = await mountLayout("/pl");

        const footer = wrapper.find(".landing-foot").text();
        expect(footer).not.toContain("Symfony");
        expect(footer).not.toContain("PHP");
    });

    it("the footer links the public repository", async () => {
        const wrapper = await mountLayout("/pl");

        const repoLink = wrapper.find(".landing-foot-repo");
        expect(repoLink.attributes("href")).toBe(
            "https://github.com/muszkin/kivvi-click",
        );
        expect(repoLink.attributes("rel")).toContain("noopener");
    });

    it("the English footer is in English, not the Polish fallback the hard-coded string was", async () => {
        i18n.global.locale.value = "en";
        const wrapper = await mountLayout("/en");

        const footer = wrapper.find(".landing-foot").text();
        expect(footer).toContain("Built in Poland with Java and Vue 3.");
        expect(footer).not.toContain("Zbudowane w Polsce");
    });

    it("the header's primary button points at the repository, not the login form", async () => {
        const wrapper = await mountLayout("/pl");

        const cta = wrapper.find(".landing-nav-repo");
        expect(cta.text()).toBe("Kod na GitHubie →");
        expect(cta.attributes("href")).toBe(
            "https://github.com/muszkin/kivvi-click",
        );
        // An outbound link now, so it gets the treatment one needs.
        expect(cta.attributes("target")).toBe("_blank");
        expect(cta.attributes("rel")).toContain("noopener");
    });

    it("the header no longer offers to create an account there is no way to create", async () => {
        const wrapper = await mountLayout("/pl");

        const header = wrapper.find(".landing-nav").text();
        expect(header).not.toContain("Załóż konto");
    });

    it("the English header is in English, both buttons", async () => {
        i18n.global.locale.value = "en";
        const wrapper = await mountLayout("/en");

        const header = wrapper.find(".landing-nav").text();
        expect(header).toContain("Sign in");
        expect(header).toContain("Source on GitHub →");
        expect(header).not.toContain("Logowanie");
    });

    it("the login button still leads to the panel, because the demo behind it is real", async () => {
        const wrapper = await mountLayout("/pl");

        const login = wrapper
            .findAll(".landing-nav a")
            .find((a) => a.text() === "Logowanie");
        expect(login?.attributes("href")).toBe("/pl/login");
    });

    it("the nav points at the open-source section, not a dead pricing anchor", async () => {
        const wrapper = await mountLayout("/pl");

        const hrefs = wrapper
            .findAll(".nav-links a")
            .map((a) => a.attributes("href"));
        expect(hrefs).toContain("#open-source");
        expect(hrefs).not.toContain("#pricing");
    });
});
