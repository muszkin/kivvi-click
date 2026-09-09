import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import { beforeEach, describe, expect, it } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import AppShell from "@/components/organisms/AppShell.vue";

/**
 * B21 (repair-1: no unit-level test existed for "only .main-scroll scrolls, never the window" —
 * only the e2e `navigation.spec.ts` test, which measures real pixel scroll behaviour and is not
 * reachable from `npm run test -- --run`). Pins the DOM shape the frozen stylesheet
 * (`assets/styles/03-components.css`, never edited here — only `.main-scroll` gets
 * `overflow-y: auto`) relies on: exactly one `.main-scroll` element, and it is the ONLY place page
 * content (the router view, stood in for here by a slot) ever renders. The pixel-level "does the
 * window actually scroll" behaviour stays proven by the real browser in
 * `tests/e2e/specs/navigation.spec.ts`; this proves the structural precondition that behaviour
 * depends on.
 *
 * Selectors below never prefix a query with `.app`: `wrapper.find()`/`wrapper.get()` are already
 * scoped to `AppShell`'s own root element, which IS `.app` (asserted once, explicitly, below) —
 * `@vue/test-utils` does not treat a wrapper's own root as a valid ancestor for a leading class in
 * a compound descendant selector passed to a later `.find()` call, so `.app .main .main-scroll`
 * silently finds nothing even though the DOM shape is correct (verified against this exact
 * component; native `Element.querySelector` on the same root does find it, confirming this is a
 * `find()`-specific scoping rule, not a markup bug).
 */
async function mountAppShell() {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push("/pl/dashboard");
    await router.isReady();
    return mount(AppShell, {
        global: { plugins: [router, i18n] },
        slots: { default: '<div class="page-body">page content</div>' },
    });
}

describe("B21 only .main-scroll scrolls — the app shell's structural invariant", () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        document.documentElement.lang = "pl";
        document.documentElement.dataset.theme = "light";
        document.documentElement.dataset.sidebar = "expanded";
    });

    it(
        "renders exactly one .main-scroll, nested inside .main, and the slot (router view) " +
            "renders inside it — never bare in .app or inside .sidebar",
        async () => {
            const wrapper = await mountAppShell();

            expect(wrapper.element.classList.contains("app")).toBe(true);
            expect(wrapper.findAll(".main-scroll")).toHaveLength(1);
            expect(wrapper.find(".main .main-scroll").exists()).toBe(true);
            expect(wrapper.find(".main-scroll .page-body").exists()).toBe(true);
            expect(wrapper.find(".sidebar .page-body").exists()).toBe(false);
            // Not a bare child of .app alongside .sidebar/.main: the slot content's own parent
            // must be exactly the .main-scroll element, not .app itself.
            expect(wrapper.find(".page-body").element.parentElement).toBe(
                wrapper.find(".main-scroll").element,
            );
        },
    );

    it(
        "sets no inline overflow/scroll style anywhere in the shell — the invariant comes " +
            "entirely from the frozen stylesheet, never an inline workaround",
        async () => {
            const wrapper = await mountAppShell();

            expect(wrapper.element.getAttribute("style")).toBeNull();
            expect(wrapper.find(".main").attributes("style")).toBeUndefined();
            expect(
                wrapper.find(".main-scroll").attributes("style"),
            ).toBeUndefined();
        },
    );
});
