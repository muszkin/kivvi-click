import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import LoginView from "@/views/LoginView.vue";

async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    return mount(LoginView, { global: { plugins: [router, i18n] } });
}

describe("B22 the login form matches the browser-validated, server-rendered contract", () => {
    afterEach(() => {
        delete document.documentElement.dataset.loginError;
        delete document.documentElement.dataset.lastUsername;
    });

    it("uses type=email on the username field, so the browser blocks a malformed address", async () => {
        const wrapper = await mountAt("/pl/login");
        const input = wrapper.find("#f-_username");
        expect(input.attributes("type")).toBe("email");
        expect(input.attributes("name")).toBe("_username");
    });

    it("posts natively to /{locale}/login", async () => {
        const wrapper = await mountAt("/pl/login");
        const form = wrapper.find("form");
        expect(form.attributes("method")).toBe("post");
        expect(form.attributes("action")).toBe("/pl/login");
    });

    it("pre-fills the sample address on a fresh load with no injected attributes", async () => {
        const wrapper = await mountAt("/pl/login");
        expect(
            (wrapper.find("#f-_username").element as HTMLInputElement).value,
        ).toBe("maciej@aureashop.pl");
        expect(wrapper.find(".feed-card__err").exists()).toBe(false);
    });

    it("renders the injected error and carries back the typed address", async () => {
        document.documentElement.dataset.loginError =
            "To nie wygląda na poprawny adres e-mail.";
        document.documentElement.dataset.lastUsername = "not-an-email";

        const wrapper = await mountAt("/pl/login");

        expect(
            (wrapper.find("#f-_username").element as HTMLInputElement).value,
        ).toBe("not-an-email");
        expect(wrapper.find(".feed-card__err").text()).toContain(
            "To nie wygląda na poprawny adres e-mail.",
        );
    });

    it("B12 renders the empty-e-mail error with the default address redisplayed, not a blank field", async () => {
        // Repair-1 (F4): the backend never emits data-last-username="" — LoginController
        // resolves a blank submission to the sample address BEFORE injecting the attribute
        // (mirroring Twig's default() filter, which falls back on an empty string too, not
        // only on an absent value — see backend/README.md and worker-report.md § Repair-1).
        // This is what a real empty-submission response body actually carries.
        document.documentElement.dataset.loginError = "Podaj adres e-mail.";
        document.documentElement.dataset.lastUsername = "maciej@aureashop.pl";

        const wrapper = await mountAt("/pl/login");

        expect(
            (wrapper.find("#f-_username").element as HTMLInputElement).value,
        ).toBe("maciej@aureashop.pl");
        expect(wrapper.find(".feed-card__err").text()).toContain(
            "Podaj adres e-mail.",
        );
    });
});

describe("B11 the login view shows the welcome copy and submit action", () => {
    beforeEach(() => {
        delete document.documentElement.dataset.loginError;
        delete document.documentElement.dataset.lastUsername;
    });

    afterEach(() => {
        i18n.global.locale.value = "pl";
    });

    it("renders the Polish welcome heading and submit label by default", async () => {
        const wrapper = await mountAt("/pl/login");
        expect(wrapper.find("h1").text()).toBe("Wróć do Kivvi");
        expect(wrapper.find('button[type="submit"]').text()).toContain(
            "Zaloguj się",
        );
    });

    it("renders the English welcome heading once the active locale is English", async () => {
        // Locale switches are always a full document reload in this app (see useIntents /
        // the topbar locale link), which re-reads document.documentElement.lang at bootstrap
        // — this asserts the rendering side directly, independent of that reload wiring.
        i18n.global.locale.value = "en";
        const wrapper = await mountAt("/en/login");
        expect(wrapper.find("h1").text()).toBe("Welcome back to Kivvi");
    });
});
