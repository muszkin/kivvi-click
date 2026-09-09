import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, h } from "vue";
import { useIntents } from "@/composables/useIntents";

/**
 * w3-shell-preferences (wave-3 repair round 2): `navigation.spec.ts`'s "theme toggle/sidebar
 * collapse survives a reload" lost the preference POST under host load — a plain, non-`keepalive`
 * fetch is aborted by the browser on unload if it has not finished, and the click handler that
 * fires it (`useIntents.ts`'s `set-theme`/`toggle-sidebar`) is only reachable through this
 * dispatch path, never called directly. `keepalive: true` (asserted below) is w3's fix for that
 * half; `stores/shell.ts`'s own `setTheme`/`setSidebar` additionally await the POST before
 * applying the DOM/store update (wave-4 repair-2), closing the other half — an arrival-order race
 * between the POST and a reload's own shell fetch that `keepalive` alone does not fix (see that
 * file's class comment for the full account) — which is why this suite only asserts the request
 * shape (`keepalive: true`, method, body), not DOM-update ordering: that belongs to
 * `shellStore.spec.ts`, which calls `setTheme`/`setSidebar` directly. The old stack this ports
 * from never had this race: `assets/app.ts` (not `assets/controllers/shell.ts`, which only
 * forwards `transitionend` into a `resize` dispatch — see `AppShell.vue`'s own citation) updates
 * the DOM synchronously and fires the POST after, un-awaited, because a full MPA reload only ever
 * happens on an explicit navigation the user triggers well after the click, never milliseconds
 * after it the way this suite's own `page.reload()` step can. This exercises the real dispatch
 * path `useIntents.ts` installs (`document.addEventListener("click", …)`, `data-action`
 * delegation) rather than calling `useShellStore().setTheme()` directly (see `shellStore.spec.ts`
 * for that, store-level, coverage) — a harness component is mounted to `document.body` so a click
 * on its `[data-action]` buttons bubbles to the real `document` listener `useIntents()` installs.
 */
const Harness = defineComponent({
    setup() {
        useIntents();
        return () =>
            h("div", [
                h(
                    "button",
                    { "data-action": "set-theme", "data-payload": "dark" },
                    "theme",
                ),
                h("button", { "data-action": "toggle-sidebar" }, "sidebar"),
            ]);
    },
});

describe("useIntents keeps preference POSTs alive across an immediate reload", () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        document.documentElement.lang = "pl";
        document.documentElement.dataset.theme = "light";
        document.documentElement.dataset.sidebar = "expanded";
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => new Response("{}", { status: 200 })),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("the set-theme intent's POST carries keepalive: true (mutation must fail)", async () => {
        const wrapper = mount(Harness, { attachTo: document.body });

        await wrapper.find('[data-action="set-theme"]').trigger("click");

        expect(fetch).toHaveBeenCalledWith(
            "/preferences/theme",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ theme: "dark" }),
                keepalive: true,
            }),
        );
        wrapper.unmount();
    });

    it("the toggle-sidebar intent's POST carries keepalive: true (mutation must fail)", async () => {
        const wrapper = mount(Harness, { attachTo: document.body });

        await wrapper.find('[data-action="toggle-sidebar"]').trigger("click");

        expect(fetch).toHaveBeenCalledWith(
            "/preferences/sidebar",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ state: "collapsed" }),
                keepalive: true,
            }),
        );
        wrapper.unmount();
    });
});
