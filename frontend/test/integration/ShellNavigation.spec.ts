import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { createRouter, createWebHistory, type Router } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import AppLayout from "@/layouts/AppLayout.vue";
import { routes } from "@/router/routes";
import { useShellStore } from "@/stores/shell";

/**
 * w5-shell-navigation: the real router → `AppLayout` → shell-store → `AppShell`/`Sidebar`/`Topbar`
 * chain, exercised for EVERY panel route in `routes.ts` — not just the one or two routes each
 * earlier journey's own repair covered (`CustomerDetailSidebarSection.spec.ts`'s `customer_show`
 * for B03, `LocaleToggle.spec.ts`'s `settings`/`customers` for the locale-toggle repair). Proves
 * B02 ("panel pages carry sidebar, topbar, .main-scroll and mark the current nav item
 * aria-current=page") end to end, driven by the shell API payload the way a real page load is,
 * plus the store's own re-hydration on a simulated reload (B08/B10's persistence, exercised here
 * through the same AppLayout mount a browser actually uses, not by calling the store directly —
 * see `shellStore.spec.ts` for that lower-level coverage). Mirrors the mounting pattern
 * `LocaleToggle.spec.ts`/`CustomerDetailSidebarSection.spec.ts` already established: a fake page
 * body slot stands in for the route's own view (its own API calls are that view's own journey's
 * concern), and a stubbed `GET /api/v1/{locale}/shell` stands in for `ShellController` (the
 * server side of every assertion here is `ShellPagesIT`/`ShellViewServiceTest`).
 */

// The full navigation catalogue (NavigationCatalog.java ported to a test fixture) — every
// section's own nav item must be present so any route's currentSection can resolve to a real,
// highlightable entry, the way the real GET /api/v1/{locale}/shell payload always does.
const NAV_GROUPS = [
    {
        label: "Główne",
        items: [
            {
                label: "Pulpit",
                icon: "dashboard",
                route: "dashboard",
                href: "/pl/dashboard",
            },
            {
                label: "Strumień zdarzeń",
                icon: "activity",
                route: "events",
                href: "/pl/events",
                badge: "·",
            },
            {
                label: "Klienci",
                icon: "users",
                route: "customers",
                href: "/pl/customers",
            },
        ],
    },
    {
        label: "Automatyzacja",
        items: [
            {
                label: "Reguły",
                icon: "bolt",
                route: "automations",
                href: "/pl/automations",
            },
            {
                label: "Kampanie email",
                icon: "mail",
                route: "campaigns",
                href: "/pl/campaigns",
            },
            {
                label: "Popupy i widgety",
                icon: "layout",
                route: "popups",
                href: "/pl/popups",
            },
        ],
    },
    {
        label: "Dane",
        items: [
            {
                label: "Feedy produktów",
                icon: "cart",
                route: "feeds",
                href: "/pl/feeds",
            },
            {
                label: "Import klientów",
                icon: "upload",
                route: "import",
                href: "/pl/import",
            },
        ],
    },
    {
        label: "Konfiguracja",
        items: [
            {
                label: "Ustawienia",
                icon: "settings",
                route: "settings",
                href: "/pl/settings",
            },
        ],
    },
];

const WORKSPACE = {
    name: "aureashop.pl",
    meta: "3 strony",
    mark: "AS",
};
const USER = { name: "Maciej Kowalczyk", email: "maciej@aureashop.pl" };

function shellPayload(overrides: {
    currentSection: string;
    crumb: string;
    theme?: string;
    sidebar?: string;
}) {
    return {
        locale: "pl",
        theme: overrides.theme ?? "light",
        sidebar: overrides.sidebar ?? "expanded",
        navGroups: NAV_GROUPS,
        currentSection: overrides.currentSection,
        crumb: overrides.crumb,
        workspace: WORKSPACE,
        user: USER,
    };
}

/** Every route in `routes.ts` that uses `AppLayout` (carries the shell), with a representative
 * URL and the currentSection/crumb its real ShellViewService/NavigationCatalog resolve to
 * (verified server-side by ShellPagesIT/ShellViewServiceTest) — home/login are excluded, since
 * their routes use PublicLayout/AuthLayout and never render a sidebar or breadcrumb. */
const PANEL_ROUTES: Array<{ path: string; section: string; crumb: string }> = [
    { path: "/pl/dashboard", section: "dashboard", crumb: "Pulpit" },
    { path: "/pl/events", section: "events", crumb: "Strumień zdarzeń" },
    { path: "/pl/customers", section: "customers", crumb: "Klienci" },
    { path: "/pl/customers/c_1001", section: "customers", crumb: "Klienci" },
    { path: "/pl/automations", section: "automations", crumb: "Reguły" },
    { path: "/pl/automations/new", section: "automations", crumb: "Reguły" },
    { path: "/pl/automations/a1", section: "automations", crumb: "Reguły" },
    { path: "/pl/campaigns", section: "campaigns", crumb: "Kampanie email" },
    { path: "/pl/emails/new", section: "campaigns", crumb: "Kampanie email" },
    { path: "/pl/emails/k1", section: "campaigns", crumb: "Kampanie email" },
    { path: "/pl/popups", section: "popups", crumb: "Popupy i widgety" },
    { path: "/pl/popups/new", section: "popups", crumb: "Popupy i widgety" },
    { path: "/pl/popups/p1", section: "popups", crumb: "Popupy i widgety" },
    { path: "/pl/feeds", section: "feeds", crumb: "Feedy produktów" },
    { path: "/pl/import", section: "import", crumb: "Import klientów" },
    { path: "/pl/import/2", section: "import", crumb: "Import klientów" },
    { path: "/pl/settings", section: "settings", crumb: "Ustawienia" },
    { path: "/pl/settings/gdpr", section: "settings", crumb: "Ustawienia" },
];

const ALL_ROUTES = NAV_GROUPS.flatMap((group) =>
    group.items.map((item) => item.route),
);

function freshRouter(): Router {
    return createRouter({ history: createWebHistory(), routes });
}

async function mountAppLayoutAt(router: Router, path: string, payload: object) {
    vi.stubGlobal(
        "fetch",
        vi.fn(async () => Response.json(payload)),
    );
    await router.push(path);
    await router.isReady();
    const wrapper = mount(AppLayout, {
        global: { plugins: [router, createPinia(), i18n] },
        slots: { default: "<div>page body</div>" },
    });
    await flushPromises();
    return wrapper;
}

beforeEach(() => {
    document.documentElement.lang = "pl";
    document.documentElement.dataset.theme = "light";
    document.documentElement.dataset.sidebar = "expanded";
});

afterEach(() => {
    vi.unstubAllGlobals();
});

describe("B02 every panel route carries the shell, with the right nav item and crumb active", () => {
    for (const { path, section, crumb } of PANEL_ROUTES) {
        it(`${path} highlights "${section}", shows crumb "${crumb}", and carries sidebar/topbar/main-scroll`, async () => {
            const wrapper = await mountAppLayoutAt(
                freshRouter(),
                path,
                shellPayload({ currentSection: section, crumb }),
            );

            // Structural chrome every panel page carries (testPanelPagesCarryTheShell's
            // new-stack equivalent — the raw SPA document never has this markup, since Vue
            // renders it client-side from this same shell payload; see ShellPagesIT's own
            // class comment for the server-side half).
            expect(wrapper.find(".app .sidebar").exists()).toBe(true);
            expect(wrapper.find(".main .topbar").exists()).toBe(true);
            expect(wrapper.find(".main-scroll").exists()).toBe(true);

            const active = wrapper.find(`.nav-item[data-route="${section}"]`);
            expect(active.exists()).toBe(true);
            expect(active.attributes("aria-current")).toBe("page");
            expect(active.attributes("data-active")).toBe("true");

            // Every OTHER nav item must stay inactive — a hard-coded highlight (or a highlight
            // that never clears) would pass the assertion above alone.
            for (const route of ALL_ROUTES) {
                if (route === section) continue;
                const other = wrapper.find(`.nav-item[data-route="${route}"]`);
                expect(other.attributes("aria-current")).toBeUndefined();
                expect(other.attributes("data-active")).toBe("false");
            }

            expect(wrapper.find(".crumbs .now").text()).toBe(crumb);
            expect(wrapper.find(".crumbs").text()).toContain("aureashop.pl");
        });
    }
});

describe("B08/B10 the shell store re-hydrates sidebar/theme from the shell API on a simulated reload", () => {
    it("a fresh mount (simulated reload) reflects a session that was left collapsed/dark", async () => {
        // Simulates the server having already rendered <html data-theme="dark"
        // data-sidebar="collapsed"> from the persisted session (SpaDocument.inject, before Vue
        // ever mounts) and the shell API confirming the same values — exactly what a real reload
        // after a toggle looks like, without needing a real browser navigation.
        document.documentElement.dataset.theme = "dark";
        document.documentElement.dataset.sidebar = "collapsed";

        const wrapper = await mountAppLayoutAt(
            freshRouter(),
            "/pl/dashboard",
            shellPayload({
                currentSection: "dashboard",
                crumb: "Pulpit",
                theme: "dark",
                sidebar: "collapsed",
            }),
        );

        expect(wrapper.find(".app").attributes("data-sidebar")).toBe(
            "collapsed",
        );
        // Topbar's theme button offers to switch TO light, i.e. it knows the current theme is
        // dark (see Topbar.vue: themePayload is the opposite of the theme it mounted with).
        expect(
            wrapper
                .find('[data-action="set-theme"]')
                .attributes("data-payload"),
        ).toBe("light");
    });

    it("a fresh mount of a DIFFERENT session (never toggled) still reflects expanded/light", async () => {
        const wrapper = await mountAppLayoutAt(
            freshRouter(),
            "/pl/dashboard",
            shellPayload({ currentSection: "dashboard", crumb: "Pulpit" }),
        );

        expect(wrapper.find(".app").attributes("data-sidebar")).toBe(
            "expanded",
        );
        expect(
            wrapper
                .find('[data-action="set-theme"]')
                .attributes("data-payload"),
        ).toBe("dark");
    });

    it("the store itself re-hydrates from the API payload, not from whatever it held before load()", async () => {
        setActivePinia(createPinia());
        vi.stubGlobal(
            "fetch",
            vi.fn(async () =>
                Response.json(
                    shellPayload({
                        currentSection: "dashboard",
                        crumb: "Pulpit",
                        theme: "dark",
                        sidebar: "collapsed",
                    }),
                ),
            ),
        );
        const shell = useShellStore();
        expect(shell.theme).toBe("light"); // initial(): read from <html> before load()
        expect(shell.sidebar).toBe("expanded");

        await shell.load("dashboard");

        expect(shell.theme).toBe("dark");
        expect(shell.sidebar).toBe("collapsed");
    });
});
