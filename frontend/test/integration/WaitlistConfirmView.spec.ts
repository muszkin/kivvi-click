import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import WaitlistConfirmView from "@/views/WaitlistConfirmView.vue";
import { serveInLocaleOf } from "../support/documentLocale";

/**
 * The page a confirmation link lands on (PIO-71).
 *
 * Every state is decided by the server before the document is written and handed down as
 * data-waitlist-confirm on <html> — so these tests set that attribute and assert what the visitor
 * then reads, which is exactly the contract WaitlistConfirmationControllerTest pins on the other
 * side of the wire.
 */
const TOKEN = "a".repeat(64);

function reportState(state: string | null, token?: string) {
    const root = document.documentElement;
    if (state === null) {
        delete root.dataset.waitlistConfirm;
    } else {
        root.dataset.waitlistConfirm = state;
    }
    if (token === undefined) {
        delete root.dataset.waitlistToken;
    } else {
        root.dataset.waitlistToken = token;
    }
}

async function mountAtRoute(path: string) {
    serveInLocaleOf(path);
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    return mount(
        { template: "<router-view />" },
        { global: { plugins: [router, i18n] } },
    );
}

describe("PIO-71 the waitlist confirmation page", () => {
    beforeEach(() => {
        reportState(null);
    });

    afterEach(() => {
        reportState(null);
        i18n.global.locale.value = "pl";
    });

    it("resolves a token URL to the confirmation view", async () => {
        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.findComponent(WaitlistConfirmView).exists()).toBe(true);
    });

    it("reports a confirmed address and offers the way back to the home page", async () => {
        reportState("ok");

        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe(
            "Gotowe — potwierdziliśmy adres",
        );
        expect(wrapper.find(".confirm-page").attributes("data-state")).toBe(
            "ok",
        );
        expect(wrapper.find("form").exists()).toBe(false);
        expect(wrapper.find(".confirm-actions a").attributes("href")).toBe(
            "/pl",
        );
    });

    it("reassures rather than errors when the link has already been used", async () => {
        reportState("already");

        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe(
            "Ten adres jest już potwierdzony",
        );
        expect(wrapper.find("form").exists()).toBe(false);
    });

    it("an expired link offers a new one, posting the token it was given", async () => {
        reportState("expired", TOKEN);

        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Ten link już wygasł");
        const form = wrapper.find("form");
        expect(form.attributes("method")).toBe("post");
        expect(form.attributes("action")).toBe("/pl/waitlist/confirm/resend");
        expect(form.find('input[name="token"]').attributes("value")).toBe(
            TOKEN,
        );
        expect(form.find('button[type="submit"]').text()).toContain(
            "Wyślij nowy link",
        );
    });

    it("an unknown link says so without hinting whether the address exists", async () => {
        reportState("unknown");

        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Nie znamy tego linku");
        expect(wrapper.text()).not.toContain("@");
        expect(wrapper.find("form").exists()).toBe(false);
    });

    it("a state the page does not recognise falls back to the unknown link, never to a blank page", async () => {
        reportState("something-else");

        const wrapper = await mountAtRoute(`/pl/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("Nie znamy tego linku");
    });

    it("the 'check your inbox' page is driven by the route, not by an attribute", async () => {
        // Post/redirect/get: the resend lands here with no data-* of its own, and a reload must
        // keep showing the same thing rather than queueing a second message.
        const wrapper = await mountAtRoute("/pl/waitlist/confirm/sent");

        expect(wrapper.find("h1").text()).toBe("Sprawdź skrzynkę");
        expect(wrapper.find(".confirm-page").attributes("data-state")).toBe(
            "sent",
        );
    });

    it("renders in English on an /en link, links included", async () => {
        i18n.global.locale.value = "en";
        reportState("expired", TOKEN);

        const wrapper = await mountAtRoute(`/en/waitlist/confirm/${TOKEN}`);

        expect(wrapper.find("h1").text()).toBe("This link has expired");
        expect(wrapper.find("form").attributes("action")).toBe(
            "/en/waitlist/confirm/resend",
        );
    });

    it("a link that is not a token shape matches no route at all", async () => {
        const router = createRouter({ history: createWebHistory(), routes });

        expect(
            router.resolve("/pl/waitlist/confirm/nonsense").matched,
        ).toHaveLength(0);
        expect(
            router.resolve(`/pl/waitlist/confirm/${"a".repeat(63)}`).matched,
        ).toHaveLength(0);
    });
});
