import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import LandingView from "@/views/LandingView.vue";

/**
 * Router-driven coverage complementing test/unit/landing.spec.ts's component-level assertions:
 * this exercises the route table's bare "/" and "/{locale}" matches together with the view, the
 * way App.vue's <router-view> actually resolves them — see routes.ts's own comment on why
 * navigation between panel/public routes is always a real document GET, never router.push:
 * the "demo" link's href must stay a plain URL a browser can follow, not something intercepted
 * by the router.
 */
const LANDING_PAYLOAD = {
    features: [
        { icon: "activity", title: "Strumień zdarzeń na żywo", body: "..." },
    ],
    steps: [{ number: "01", title: "Wklej snippet", body: "..." }],
    plans: [
        {
            tier: "Free",
            price: "0",
            unit: " zł / mies.",
            items: ["x"],
            cta: "Zacznij za darmo",
            featured: false,
        },
    ],
    trustPoints: ["14 dni Pro za darmo"],
    previewTiles: [{ label: "Zdarzeń / min", value: "847" }],
    previewSeries: [10, 15, 12, 30],
};

async function mountAtRoute(path: string) {
    vi.stubGlobal(
        "fetch",
        vi.fn(async (input: RequestInfo | URL) => {
            expect(String(input)).toMatch(/^\/api\/v1\/(pl|en)\/landing$/);
            return Response.json(LANDING_PAYLOAD);
        }),
    );
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(
        { template: "<router-view />" },
        { global: { plugins: [router, i18n] } },
    );
    await flushPromises();
    return { wrapper, router };
}

describe("B22 the home route resolves to the landing view for both the bare and localized paths", () => {
    i18n.global.warnHtmlMessage = false;

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("bare '/' resolves to the home route and fetches the Polish payload", async () => {
        const { router } = await mountAtRoute("/");

        expect(router.currentRoute.value.name).toBe("home");
        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/landing",
            expect.anything(),
        );
    });

    it("'/pl' renders the landing view's hero and demo link", async () => {
        const { wrapper } = await mountAtRoute("/pl");

        expect(wrapper.findComponent(LandingView).exists()).toBe(true);
        expect(wrapper.find(".hero h1").exists()).toBe(true);
        const demoLink = wrapper
            .findAll(".hero-cta a")
            .find((a) => a.attributes("href")?.endsWith("/demo"));
        // A plain href, not a router-link/data-action — the demo click stays a real GET so the
        // server can 302 it into the dashboard, exactly as the oracle records it.
        expect(demoLink?.attributes("href")).toBe("/pl/demo");
        expect(demoLink?.attributes("data-action")).toBeUndefined();
    });

    it("'/en' fetches the English-locale endpoint", async () => {
        await mountAtRoute("/en");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/en/landing",
            expect.anything(),
        );
    });
});
