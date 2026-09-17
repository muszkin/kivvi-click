import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import PublicLayout from "@/layouts/PublicLayout.vue";
import { serveInLocaleOf } from "../support/documentLocale";

/**
 * The public shell's nav and footer (PIO-121).
 *
 * The footer used to claim the system was built with Symfony and PHP — untrue since the 2026-09-09
 * migration — and it was hard-coded straight into the template, which is why the English site
 * rendered that sentence in Polish. Both halves are asserted here: the claim itself, and the fact
 * that it now comes from the message catalogues, so neither can regress silently.
 */
async function mountLayout(path: string) {
    serveInLocaleOf(path);
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

/** Letters only Polish uses — none belongs in the English header or footer. */
const POLISH_LETTERS = /[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/;

describe("PIO-117 the English public header and footer carry no Polish", () => {
    it("the footer's legal links are in English", async () => {
        const wrapper = await mountLayout("/en");

        const footerLinks = wrapper
            .findAll(".landing-foot a")
            .map((a) => a.text());
        expect(footerLinks).toEqual(
            expect.arrayContaining([
                "Terms",
                "Privacy",
                "GDPR / DPA",
                "Status",
            ]),
        );
        const footer = wrapper.find(".landing-foot").text();
        expect(footer).not.toContain("Regulamin");
        expect(footer).not.toContain("Prywatność");
        expect(footer).not.toContain("RODO");
    });

    it("the Polish footer keeps its Polish legal links", async () => {
        const wrapper = await mountLayout("/pl");

        const footerLinks = wrapper
            .findAll(".landing-foot a")
            .map((a) => a.text());
        expect(footerLinks).toEqual(
            expect.arrayContaining(["Regulamin", "Prywatność", "RODO / DPA"]),
        );
    });

    it("no Polish letter appears anywhere in the English header or footer", async () => {
        for (const path of ["/", "/en", "/en/privacy"]) {
            const wrapper = await mountLayout(path);

            expect(wrapper.find(".landing-nav").text(), path).not.toMatch(
                POLISH_LETTERS,
            );
            expect(wrapper.find(".landing-foot").text(), path).not.toMatch(
                POLISH_LETTERS,
            );
        }
    });
});

describe("PIO-125 English by default, Polish one click away", () => {
    it("bare '/' is English and every public link it builds stays in English", async () => {
        const wrapper = await mountLayout("/");

        const login = wrapper
            .findAll(".landing-nav a")
            .find((a) => a.text() === "Sign in");
        expect(login?.attributes("href")).toBe("/en/login");
        const privacy = wrapper
            .findAll(".landing-foot a")
            .find((a) => a.text() === "Privacy");
        expect(privacy?.attributes("href")).toBe("/en/privacy");
    });

    it("the English header offers Polish, named in Polish, leading to /pl", async () => {
        const wrapper = await mountLayout("/");

        const toggle = wrapper.find(".landing-nav-locale");
        expect(toggle.text()).toBe("Polski");
        expect(toggle.attributes("href")).toBe("/pl");
        expect(toggle.attributes("hreflang")).toBe("pl");
        expect(toggle.attributes("lang")).toBe("pl");
        expect(toggle.attributes("title")).toBe("Change language");
    });

    it("the Polish header offers English back, leading to /en", async () => {
        const wrapper = await mountLayout("/pl");

        const toggle = wrapper.find(".landing-nav-locale");
        expect(toggle.text()).toBe("English");
        expect(toggle.attributes("href")).toBe("/en");
        expect(toggle.attributes("hreflang")).toBe("en");
        expect(toggle.attributes("title")).toBe("Zmień język");
    });

    it("the switch keeps the page: each privacy policy leads to the other language's", async () => {
        const english = await mountLayout("/en/privacy");
        expect(english.find(".landing-nav-locale").attributes("href")).toBe(
            "/pl/privacy",
        );

        const polish = await mountLayout("/pl/privacy");
        expect(polish.find(".landing-nav-locale").attributes("href")).toBe(
            "/en/privacy",
        );
    });
});
