import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import WaitlistUnsubscribeView from "@/views/WaitlistUnsubscribeView.vue";

/**
 * The page the "unsubscribe" link in a message's footer lands on (PIO-71). Two states, and neither
 * of them asks anybody to log in — an unsubscribe link behind a password is an unsubscribe link
 * that does not work.
 */
const TOKEN = "d".repeat(64);

function reportState(state: string | null) {
    const root = document.documentElement;
    if (state === null) {
        delete root.dataset.waitlistUnsubscribe;
    } else {
        root.dataset.waitlistUnsubscribe = state;
    }
}

async function mountAtRoute(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    return mount(
        { template: "<router-view />" },
        { global: { plugins: [router, i18n] } },
    );
}

describe("PIO-71 the waitlist unsubscribe page", () => {
    beforeEach(() => {
        reportState(null);
    });

    afterEach(() => {
        reportState(null);
        i18n.global.locale.value = "pl";
    });

    it("resolves a token URL to the unsubscribe view", async () => {
        const wrapper = await mountAtRoute(`/pl/waitlist/unsubscribe/${TOKEN}`);

        expect(wrapper.findComponent(WaitlistUnsubscribeView).exists()).toBe(
            true,
        );
    });

    it("confirms the address is off the list and says how to come back", async () => {
        reportState("ok");

        const wrapper = await mountAtRoute(`/pl/waitlist/unsubscribe/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Wypisaliśmy Cię");
        expect(wrapper.text()).toContain("zapisać się ponownie");
        expect(wrapper.find(".confirm-actions a").attributes("href")).toBe(
            "/pl",
        );
    });

    it("an unknown link says so and offers a human to write to", async () => {
        reportState("unknown");

        const wrapper = await mountAtRoute(`/pl/waitlist/unsubscribe/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Nie znamy tego linku");
        expect(wrapper.text()).toContain("piotr@kivvi.click");
    });

    it("a missing attribute is treated as an unknown link rather than as success", async () => {
        // Getting this backwards would tell somebody they are unsubscribed when nothing happened.
        const wrapper = await mountAtRoute(`/pl/waitlist/unsubscribe/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Nie znamy tego linku");
    });

    it("renders in English on an /en link", async () => {
        i18n.global.locale.value = "en";
        reportState("ok");

        const wrapper = await mountAtRoute(`/en/waitlist/unsubscribe/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("You are unsubscribed");
    });
});
