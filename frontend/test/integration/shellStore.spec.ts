import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useShellStore } from "@/stores/shell";

describe("B08/B10 shell store preference toggles", () => {
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

    it("B08 setTheme updates the document attribute and posts the new value", async () => {
        const shell = useShellStore();

        await shell.setTheme("dark");

        expect(shell.theme).toBe("dark");
        expect(document.documentElement.dataset.theme).toBe("dark");
        expect(fetch).toHaveBeenCalledWith(
            "/preferences/theme",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ theme: "dark" }),
            }),
        );
    });

    it("B10 setSidebar updates the store, the <html> attribute and posts the new value", async () => {
        const shell = useShellStore();

        await shell.setSidebar("collapsed");

        expect(shell.sidebar).toBe("collapsed");
        expect(document.documentElement.dataset.sidebar).toBe("collapsed");
        expect(fetch).toHaveBeenCalledWith(
            "/preferences/sidebar",
            expect.objectContaining({
                method: "POST",
                body: JSON.stringify({ state: "collapsed" }),
            }),
        );
    });

    // w3-shell-preferences (wave-3 repair round 2): these two POSTs are fired from a click
    // handler that never awaits them (useIntents' set-theme/toggle-sidebar are both
    // `void`-fired, on purpose — the DOM update must happen synchronously, before the POST even
    // starts). A plain fetch is aborted by the browser if a navigation/reload happens before it
    // finishes; under host load that lost the request outright (navigation.spec.ts "theme
    // toggle/sidebar collapse survives a reload", wave-3 round-2 e2e). `keepalive: true` is the
    // fix — request bytes (method/headers/body, already asserted above) are unaffected.
    it("B08 setTheme's POST survives an immediate reload (keepalive: true)", async () => {
        const shell = useShellStore();

        await shell.setTheme("dark");

        expect(fetch).toHaveBeenCalledWith(
            "/preferences/theme",
            expect.objectContaining({ keepalive: true }),
        );
    });

    it("B10 setSidebar's POST survives an immediate reload (keepalive: true)", async () => {
        const shell = useShellStore();

        await shell.setSidebar("collapsed");

        expect(fetch).toHaveBeenCalledWith(
            "/preferences/sidebar",
            expect.objectContaining({ keepalive: true }),
        );
    });

    // Wave-4 repair-2 (w3-shell-preferences): a `keepalive` POST is dispatched at the browser's
    // lowest fetch priority, so on a heavier reload target it can reach the server AFTER the
    // reload's own shell GET — an arrival-order race the per-session lock cannot fix, since it
    // only orders requests that have already arrived. Awaiting the POST before applying the DOM
    // attribute/store state makes a later reload causally observe the write by construction:
    // dropping the `await` (mutation) makes this test fail, since the attribute would already be
    // "dark" before the deferred fetch promise ever resolves.
    it("B08 setTheme applies the DOM attribute and store state only after the POST resolves", async () => {
        let resolveFetch!: (response: Response) => void;
        const pendingFetch = new Promise<Response>((resolve) => {
            resolveFetch = resolve;
        });
        vi.stubGlobal(
            "fetch",
            vi.fn(() => pendingFetch),
        );
        const shell = useShellStore();

        const settled = shell.setTheme("dark");

        expect(shell.theme).toBe("light");
        expect(document.documentElement.dataset.theme).toBe("light");

        resolveFetch(new Response("{}", { status: 200 }));
        await settled;

        expect(shell.theme).toBe("dark");
        expect(document.documentElement.dataset.theme).toBe("dark");
    });

    it("B10 setSidebar applies the store state and <html> attribute only after the POST resolves", async () => {
        let resolveFetch!: (response: Response) => void;
        const pendingFetch = new Promise<Response>((resolve) => {
            resolveFetch = resolve;
        });
        vi.stubGlobal(
            "fetch",
            vi.fn(() => pendingFetch),
        );
        const shell = useShellStore();

        const settled = shell.setSidebar("collapsed");

        expect(shell.sidebar).toBe("expanded");
        expect(document.documentElement.dataset.sidebar).toBe("expanded");

        resolveFetch(new Response("{}", { status: 200 }));
        await settled;

        expect(shell.sidebar).toBe("collapsed");
        expect(document.documentElement.dataset.sidebar).toBe("collapsed");
    });

    it("B08 setTheme still applies the DOM attribute when the POST rejects, and logs nothing", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(() => Promise.reject(new Error("network down"))),
        );
        const consoleSpy = vi
            .spyOn(console, "error")
            .mockImplementation(() => {});
        const shell = useShellStore();

        await shell.setTheme("dark");

        expect(shell.theme).toBe("dark");
        expect(document.documentElement.dataset.theme).toBe("dark");
        expect(consoleSpy).not.toHaveBeenCalled();
        consoleSpy.mockRestore();
    });

    it("B10 setSidebar still applies the store state when the POST rejects, and logs nothing", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(() => Promise.reject(new Error("network down"))),
        );
        const consoleSpy = vi
            .spyOn(console, "error")
            .mockImplementation(() => {});
        const shell = useShellStore();

        await shell.setSidebar("collapsed");

        expect(shell.sidebar).toBe("collapsed");
        expect(document.documentElement.dataset.sidebar).toBe("collapsed");
        expect(consoleSpy).not.toHaveBeenCalled();
        consoleSpy.mockRestore();
    });

    it("load() populates navigation, workspace and identity from the shell API", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () =>
                Response.json({
                    locale: "pl",
                    theme: "light",
                    sidebar: "expanded",
                    navGroups: [{ label: "Główne", items: [] }],
                    currentSection: "dashboard",
                    crumb: "Pulpit",
                    workspace: {
                        name: "aureashop.pl",
                        meta: "3 strony",
                        mark: "AS",
                    },
                    user: { name: "Anna", email: "anna@aureashop.pl" },
                }),
            ),
        );

        const shell = useShellStore();
        await shell.load("dashboard");

        expect(shell.crumb).toBe("Pulpit");
        expect(shell.user.email).toBe("anna@aureashop.pl");
        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/shell?route=dashboard",
            expect.anything(),
        );
    });
});
