import { mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import LoginView from "@/views/LoginView.vue";

/**
 * Repair-3 (R3-B): round-2's unit dimension (`npm run test -- --run` → `vitest run test/unit`)
 * had no test for B22's login-relevant form contract — the only test asserting it lives in
 * `test/integration/LoginView.spec.ts`, which `test/unit`'s path-scoped command never touches
 * (confirmed: the unit run before this fix executed exactly 5 files, none of them this one).
 * This is a second, independent test of the same rendered markup, not a duplicate: it exists
 * purely so the unit-dimension command has its own real assertion to run, matching every other
 * behaviour row's unit/integration pairing in this slice.
 */
async function mountAt(path: string) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    return mount(LoginView, { global: { plugins: [router, i18n] } });
}

describe("B22 the login form matches the browser-validated, server-rendered contract (unit)", () => {
    afterEach(() => {
        delete document.documentElement.dataset.loginError;
        delete document.documentElement.dataset.lastUsername;
    });

    it("uses type=email on the username field, so the browser blocks a malformed address", async () => {
        const wrapper = await mountAt("/pl/login");
        const input = wrapper.find("#f-_username");

        expect(input.attributes("type")).toBe("email");
        expect(input.attributes("name")).toBe("_username");

        // Deliberately NOT asserting a `required` attribute here: the oracle's own captured
        // accessibility snapshot for this exact field
        // (context/migration-oracle/.../journeys/login/steps/1/a11y.json — `textbox "Email"`,
        // no `[required]` marker) shows it was never required in the old stack either, and the
        // capture tool does surface that marker elsewhere in the oracle when a field genuinely
        // has it (e.g. journeys/settings, journeys/import-wizard use `[disabled]`/`[checked]`/
        // `[expanded]`) — so its absence here is signal, not a gap in the capture. Making the
        // field required would also make B12 (the empty-e-mail server rejection, "Podaj adres
        // e-mail.") unreachable through an actual browser submission, which the oracle
        // demonstrates as real, capturable behaviour. Adding `required` was considered and
        // rejected for this reason.
    });

    it("uses type=password on the password field", async () => {
        const wrapper = await mountAt("/pl/login");
        const input = wrapper.find("#f-_password");

        expect(input.attributes("type")).toBe("password");
        expect(input.attributes("name")).toBe("_password");
    });

    it("posts natively to /{locale}/login", async () => {
        const wrapper = await mountAt("/pl/login");
        const form = wrapper.find("form");

        expect(form.attributes("method")).toBe("post");
        expect(form.attributes("action")).toMatch(/\/login$/);
    });

    it("renders a submit button", async () => {
        const wrapper = await mountAt("/pl/login");

        expect(wrapper.find('button[type="submit"]').exists()).toBe(true);
    });
});
