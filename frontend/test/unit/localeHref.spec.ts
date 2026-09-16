import { createRouter, createWebHistory } from "vue-router";
import { describe, expect, it } from "vitest";
import { buildLocaleHref } from "@/router/localeHref";
import { routes } from "@/router/routes";

const router = createRouter({ history: createWebHistory(), routes });

describe("B32/repair-1 buildLocaleHref — generic Symfony-style default-param omission", () => {
    it("settings' own default tab (account) is omitted from the toggled URL", () => {
        const route = router.resolve("/pl/settings/account");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/settings");
    });

    it("settings' bare route (tab already omitted) stays omitted", () => {
        const route = router.resolve("/pl/settings");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/settings");
    });

    it("a non-default settings tab keeps its URL segment", () => {
        const route = router.resolve("/pl/settings/gdpr");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/settings/gdpr");
    });

    it("a route with no meta.defaultParams (customer detail) is unaffected", () => {
        const route = router.resolve("/pl/customers/c_1000");

        expect(buildLocaleHref(router, route, "en")).toBe(
            "/en/customers/c_1000",
        );
    });

    it("a route with no params at all (dashboard) is unaffected", () => {
        const route = router.resolve("/pl/dashboard");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/dashboard");
    });

    it("import-wizard's own default step (1) is omitted from the toggled URL — wave-4's repair of the gap this test used to document (see git history: it previously asserted the step was NOT omitted, pending routes.ts's own `defaultParams: { step: '1' }`)", () => {
        const route = router.resolve("/pl/import/1");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/import");
    });

    it("import-wizard's bare route (step already omitted) stays omitted", () => {
        const route = router.resolve("/pl/import");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/import");
    });

    it("a non-default import step keeps its URL segment", () => {
        const route = router.resolve("/pl/import/2");

        expect(buildLocaleHref(router, route, "en")).toBe("/en/import/2");
    });

    it("round-trips back to pl the same way", () => {
        const route = router.resolve("/en/settings/account");

        expect(buildLocaleHref(router, route, "pl")).toBe("/pl/settings");
    });
});
