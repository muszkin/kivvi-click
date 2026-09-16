import { createRouter, createWebHistory } from "vue-router";
import { describe, expect, it } from "vitest";
import { routes } from "@/router/routes";
import { otherLocale, routeLocale } from "@/router/routeLocale";

const router = createRouter({ history: createWebHistory(), routes });

describe("PIO-125 routeLocale — the language a public page is read in", () => {
    it("bare '/' has no locale segment, so it is the English default", () => {
        expect(routeLocale(router.resolve("/"))).toBe("en");
    });

    it("a locale segment always wins over the default", () => {
        expect(routeLocale(router.resolve("/pl"))).toBe("pl");
        expect(routeLocale(router.resolve("/en"))).toBe("en");
        expect(routeLocale(router.resolve("/pl/privacy"))).toBe("pl");
    });

    it("the switch offers the other language, in both directions", () => {
        expect(otherLocale("en")).toBe("pl");
        expect(otherLocale("pl")).toBe("en");
    });
});
