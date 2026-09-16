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

/**
 * PIO-121 changed the purpose of processing from a launch notification to contact about a
 * deployment. The first pass rewrote only the three sections the spec named, which left the
 * document stating one purpose in three sections and a waiting list in four — a reader could not
 * tell which one they had consented to, and the consent proof stored in `waitlist_subscriber`
 * attests to a clause the surrounding document contradicted.
 *
 * These are the phrases that carried the old narrative. None of them may come back: a single one
 * reappearing means the policy has gone internally inconsistent again, which is worse than either
 * version on its own.
 */
const STALE_LAUNCH_PHRASES_PL = [
    "listę oczekujących",
    "liście oczekujących",
    "powiadomienie o starcie",
    "powiadomienia o starcie",
    "jedno powiadomienie",
];

const STALE_LAUNCH_PHRASES_EN = [
    "waiting list",
    "launch notification",
    "one notification",
];
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

    it("identifies the controller, so the page is not signed by an anonymous 'site owner'", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        expect(text).toContain("Fairydeck Piotr Mucha");
        expect(text).toContain("7321962870");
        expect(text).toContain("Niepołomice");
    });

    it("gives a contact address that actually works, in every section that promises one", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        // The consent-withdrawal route, the rights section and the contact section all promise an
        // address. A placeholder here would describe a mechanism nobody can use — which is exactly
        // what this page shipped with before the owner supplied the real one.
        expect(text).toContain("piotr@kivvi.click");
        expect(text).not.toMatch(/do uzupełnienia|\[.*—.*\]/);
    });

    it("PIO-121 states the new purpose — contact about a deployment, not a launch notification", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        expect(text).toContain("w sprawie wdrożenia kivvi·click");
        expect(text).not.toContain(
            "jedno powiadomienie, kiedy kivvi·click ruszy",
        );
    });

    it("PIO-121 ties retention to the deployment conversation, not to a launch that will not happen", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        expect(text).toContain("Do zakończenia rozmowy o wdrożeniu");
        expect(text).not.toContain("Do czasu wysłania powiadomienia o starcie");
    });

    it("PIO-121 says the consequence of withholding data is no contact, not a missed list", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        expect(wrapper.text()).toContain("nie skontaktujemy się z Tobą");
    });

    it("PIO-121 carries the same three changes in English", async () => {
        i18n.global.locale.value = "en";
        const { wrapper } = await mountAtRoute("/en/privacy");

        const text = wrapper.text();
        expect(text).toContain("about deploying kivvi·click");
        expect(text).toContain(
            "Until the conversation about your deployment has ended",
        );
        expect(text).toContain("we will not contact you");
    });

    it("PIO-121 dates both language versions to the day the purpose changed", async () => {
        const polish = await mountAtRoute("/pl/privacy");
        expect(polish.wrapper.text()).toContain("16 września 2026");

        i18n.global.locale.value = "en";
        const english = await mountAtRoute("/en/privacy");
        expect(english.wrapper.text()).toContain("16 September 2026");
    });

    it("PIO-121 carries no waiting-list language left anywhere in the document", async () => {
        const polish = await mountAtRoute("/pl/privacy");
        const polishText = polish.wrapper.text();
        for (const stale of STALE_LAUNCH_PHRASES_PL) {
            expect(polishText).not.toContain(stale);
        }

        i18n.global.locale.value = "en";
        const english = await mountAtRoute("/en/privacy");
        const englishText = english.wrapper.text();
        for (const stale of STALE_LAUNCH_PHRASES_EN) {
            expect(englishText).not.toContain(stale);
        }
    });

    it("PIO-118 states plainly that nothing leaves the EEA, now that the fonts are self-hosted", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        // This assertion is the inverse of the one it replaced. Until PIO-118 index.html pulled
        // the typefaces from Google's CDN, so the policy had to disclose that transfer and the
        // test held it to that disclosure. The transfer is gone, so naming Google here would now
        // describe something the site does not do — and the plain statement below would be a
        // lie the moment any third-party asset came back. fonts.spec.ts and the e2e network
        // assertion in tests/e2e/specs/public.spec.ts guard the other half of that pair.
        expect(text).not.toContain("Google Fonts");
        expect(text).toContain(
            "Nie przekazujemy Twoich danych poza Europejski Obszar Gospodarczy",
        );
    });

    it("PIO-118 carries the same statement in English", async () => {
        i18n.global.locale.value = "en";
        const { wrapper } = await mountAtRoute("/en/privacy");

        const text = wrapper.text();
        expect(text).not.toContain("Google Fonts");
        expect(text).toContain(
            "We transfer no data outside the European Economic Area",
        );
    });

    it("PIO-118 leaves the rest of the policy exactly as it was", async () => {
        const { wrapper } = await mountAtRoute("/pl/privacy");

        const text = wrapper.text();
        // The EEA section was the only one this change was allowed to touch. These are the
        // statements around it that a careless rewrite would take with it: the controller, the
        // absence of a DPO, the rights, the e-mail subprocessor, the cookie statement and the
        // Article 22 statement.
        expect(text).toContain("Fairydeck Piotr Mucha");
        expect(text).toContain("Nie powołaliśmy inspektora ochrony danych");
        expect(text).toContain("Prezesa Urzędu Ochrony Danych Osobowych");
        expect(text).toContain("Brevo");
        expect(text).toContain("nie zapisują żadnych ciasteczek");
        expect(text).toContain("art. 22 RODO");
    });

    it("keeps the Polish and English policies in step", async () => {
        const polish = await mountAtRoute("/pl/privacy");
        const polishHeadings =
            polish.wrapper.findAll(".legal-section h2").length;

        i18n.global.locale.value = "en";
        const english = await mountAtRoute("/en/privacy");
        const englishHeadings =
            english.wrapper.findAll(".legal-section h2").length;

        expect(englishHeadings).toBe(polishHeadings);
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
