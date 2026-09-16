import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import LandingView from "@/views/LandingView.vue";

/**
 * The waitlist form in the landing hero (PIO-70): what it renders, what it hides from bots and
 * from assistive technology, and how it comes back up after the server refuses a submission.
 *
 * The refusal path is driven the way the real one is — by data-waitlist-* attributes the server
 * puts on <html> before the SPA boots — so these tests set them on document.documentElement and
 * clear them again, rather than reaching into the component.
 */
const LANDING_PAYLOAD = {
    features: [
        { icon: "activity", title: "Strumień zdarzeń na żywo", body: "..." },
    ],
    steps: [{ number: "01", title: "Wklej snippet", body: "..." }],
    trustPoints: ["Licencja MIT"],
    previewTiles: [{ label: "Zdarzeń / min", value: "847" }],
    previewSeries: [10, 15, 12, 30],
};

const WAITLIST_ATTRIBUTES = [
    "waitlistError",
    "waitlistEmail",
    "waitlistConsent",
] as const;

async function mountAt(path: string) {
    vi.stubGlobal(
        "fetch",
        vi.fn(async () => Response.json(LANDING_PAYLOAD)),
    );
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(LandingView, { global: { plugins: [router, i18n] } });
    await flushPromises();
    return wrapper;
}

describe("PIO-70 the waitlist form on the landing hero", () => {
    i18n.global.warnHtmlMessage = false;

    beforeEach(() => {
        for (const attribute of WAITLIST_ATTRIBUTES) {
            delete document.documentElement.dataset[attribute];
        }
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        i18n.global.locale.value = "pl";
    });

    it("renders an e-mail field, a consent checkbox and a submit button", async () => {
        const wrapper = await mountAt("/pl");

        const email = wrapper.find("#f-email");
        expect(email.attributes("type")).toBe("email");
        expect(email.attributes("name")).toBe("email");
        expect(email.attributes("required")).toBeDefined();
        expect(
            wrapper
                .find('.waitlist-consent input[type="checkbox"]')
                .attributes("name"),
        ).toBe("consent");
        expect(
            wrapper.find('.waitlist-form button[type="submit"]').exists(),
        ).toBe(true);
    });

    it("posts natively to the locale's waitlist endpoint", async () => {
        const wrapper = await mountAt("/pl");

        const form = wrapper.find("form.waitlist-form");
        expect(form.attributes("method")).toBe("post");
        expect(form.attributes("action")).toBe("/pl/waitlist");
    });

    it("posts to the English endpoint on the English landing page", async () => {
        i18n.global.locale.value = "en";
        const wrapper = await mountAt("/en");

        expect(wrapper.find("form.waitlist-form").attributes("action")).toBe(
            "/en/waitlist",
        );
    });

    it("hides the honeypot from the eye, from the tab order and from screen readers", async () => {
        const wrapper = await mountAt("/pl");

        const honeypot = wrapper.find('input[name="website"]');
        expect(honeypot.exists()).toBe(true);
        expect(honeypot.attributes("tabindex")).toBe("-1");
        expect(honeypot.attributes("autocomplete")).toBe("off");

        const container = honeypot.element.closest(".visually-hidden");
        expect(container).not.toBeNull();
        expect(container?.getAttribute("aria-hidden")).toBe("true");
    });

    it("the consent clause links to the privacy policy for this locale", async () => {
        const wrapper = await mountAt("/pl");

        expect(wrapper.find(".waitlist-consent a").attributes("href")).toBe(
            "/pl/privacy",
        );
    });

    it("the consent box starts unticked on a fresh visit", async () => {
        const wrapper = await mountAt("/pl");

        const checkbox = wrapper.find<HTMLInputElement>(
            '.waitlist-consent input[type="checkbox"]',
        );
        expect(checkbox.element.checked).toBe(false);
    });

    it("shows no error and no thank-you on a fresh visit", async () => {
        const wrapper = await mountAt("/pl");

        expect(wrapper.find(".waitlist-thanks").exists()).toBe(false);
        expect(wrapper.find(".field-row [aria-invalid]").exists()).toBe(false);
    });

    it("a refused submission shows the server's message beside the field", async () => {
        document.documentElement.dataset.waitlistError =
            "To nie wygląda na poprawny adres e-mail.";
        document.documentElement.dataset.waitlistEmail = "nie-adres";
        document.documentElement.dataset.waitlistConsent = "true";

        const wrapper = await mountAt("/pl/waitlist");

        expect(wrapper.find(".waitlist-row").text()).toContain(
            "To nie wygląda na poprawny adres e-mail.",
        );
        expect(wrapper.find("#f-email").attributes("aria-invalid")).toBe(
            "true",
        );
    });

    it("a refused submission keeps the address that was typed", async () => {
        document.documentElement.dataset.waitlistError =
            "Zaznacz zgodę, żebyśmy mogli wysłać Ci powiadomienie.";
        document.documentElement.dataset.waitlistEmail = "ala@sklep.pl";
        document.documentElement.dataset.waitlistConsent = "false";

        const wrapper = await mountAt("/pl/waitlist");

        expect(wrapper.find<HTMLInputElement>("#f-email").element.value).toBe(
            "ala@sklep.pl",
        );
    });

    it("a refused submission keeps the consent box as the visitor left it", async () => {
        document.documentElement.dataset.waitlistError =
            "To nie wygląda na poprawny adres e-mail.";
        document.documentElement.dataset.waitlistEmail = "nie-adres";
        document.documentElement.dataset.waitlistConsent = "true";

        const wrapper = await mountAt("/pl/waitlist");

        const checkbox = wrapper.find<HTMLInputElement>(
            '.waitlist-consent input[type="checkbox"]',
        );
        expect(checkbox.element.checked).toBe(true);
    });

    it("?waitlist=ok replaces the form with the thank-you, announced to screen readers", async () => {
        const wrapper = await mountAt("/pl?waitlist=ok");

        expect(wrapper.find("form.waitlist-form").exists()).toBe(false);
        const thanks = wrapper.find(".waitlist-thanks");
        expect(thanks.attributes("role")).toBe("status");
        expect(thanks.text()).toContain("link potwierdzający");
    });

    it("the hero keeps exactly one CTA link — the demo — now that the form replaced the other", async () => {
        const wrapper = await mountAt("/pl");

        const ctaLinks = wrapper.findAll(".hero-cta a");
        expect(ctaLinks).toHaveLength(1);
        expect(ctaLinks[0]?.attributes("href")).toBe("/pl/demo");
    });
});
