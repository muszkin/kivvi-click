import { createRouter, createWebHistory } from "vue-router";
import { describe, expect, it } from "vitest";
import { routes } from "@/router/routes";

const router = createRouter({ history: createWebHistory(), routes });

describe("B01 every panel/public route in the table resolves", () => {
    it.each([
        "/",
        "/pl",
        "/pl/privacy",
        "/en/privacy",
        // PIO-70: the waitlist form's POST target resolves too — a refused submission is
        // answered with the landing document, leaving the browser on this URL.
        "/pl/waitlist",
        "/pl/login",
        "/pl/dashboard",
        "/pl/events",
        "/pl/customers",
        "/pl/customers/c_1",
        "/pl/automations",
        "/pl/automations/new",
        "/pl/automations/a1",
        "/pl/campaigns",
        "/pl/emails/new",
        "/pl/emails/k1",
        "/pl/popups",
        "/pl/popups/new",
        "/pl/popups/p1",
        "/pl/feeds",
        "/pl/import",
        "/pl/import/2",
        "/pl/settings",
        "/en/dashboard",
    ])("%s matches a named route", async (path) => {
        const resolved = router.resolve(path);
        expect(resolved.name).toBeDefined();
        expect(resolved.matched.length).toBeGreaterThan(0);
    });
});

describe("B07 an unsupported locale prefix is unknown to the router", () => {
    it("does not match /de/dashboard", () => {
        const resolved = router.resolve("/de/dashboard");
        expect(resolved.matched.length).toBe(0);
    });
});
