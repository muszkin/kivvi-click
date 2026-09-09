import { defineStore } from "pinia";

export interface NavItem {
    label: string;
    icon: string;
    route: string;
    href: string;
    badge?: string;
}

export interface NavGroup {
    label: string;
    items: NavItem[];
}

export interface ShellState {
    locale: string;
    theme: string;
    sidebar: string;
    navGroups: NavGroup[];
    currentSection: string;
    crumb: string;
    workspace: { name: string; meta: string; mark: string };
    user: { name: string; email: string };
    loaded: boolean;
}

const initial = (): ShellState => ({
    locale: document.documentElement.lang || "pl",
    theme: document.documentElement.dataset.theme ?? "light",
    sidebar: document.documentElement.dataset.sidebar ?? "expanded",
    navGroups: [],
    currentSection: "",
    crumb: "",
    workspace: { name: "", meta: "", mark: "" },
    user: { name: "", email: "" },
    loaded: false,
});

/**
 * The panel shell view-model: navigation, workspace, identity and view preferences.
 * Theme and sidebar start from the attributes the server injected into <html> before
 * first paint (no flash); {@link ShellStore.load} then fills in the rest from
 * GET /api/v1/{locale}/shell and reconciles theme/sidebar with the session's own record.
 */
export const useShellStore = defineStore("shell", {
    state: initial,
    actions: {
        async load(routeName: string) {
            const params = new URLSearchParams();
            if (routeName) {
                params.set("route", routeName);
            }
            const response = await fetch(
                `/api/v1/${this.locale}/shell?${params.toString()}`,
                {
                    headers: { Accept: "application/json" },
                },
            );
            if (!response.ok) {
                return;
            }
            const data = (await response.json()) as Omit<ShellState, "loaded">;
            this.locale = data.locale;
            this.theme = data.theme;
            this.sidebar = data.sidebar;
            this.navGroups = data.navGroups;
            this.currentSection = data.currentSection;
            this.crumb = data.crumb;
            this.workspace = data.workspace;
            this.user = data.user;
            this.loaded = true;
        },

        // keepalive: true (wave-3 repair, w3-shell-preferences) — this POST must survive an
        // immediate `page.reload()`/navigation right after it starts. A normal, non-keepalive
        // fetch is a "fetch" request per the Fetch standard, which the browser aborts on unload
        // if it hasn't finished; `keepalive: true` marks it non-abortable on unload instead (the
        // same mechanism `navigator.sendBeacon` uses internally), with no effect on the request
        // itself — method, headers and body are unchanged, so this cannot and does not change the
        // wire bytes compare.mjs's contract dimension asserts for /preferences/theme and
        // /preferences/sidebar. The request stays capped well under the keepalive 64 KB budget
        // (a JSON body of one short field).
        //
        // Wave-4 repair-2 (w3-shell-preferences) — the DOM/state update below happens only
        // *after* the POST settles, not before it starts: a `keepalive` request is scheduled at
        // the browser's lowest fetch priority, so on a reload whose page has real weight (SSE
        // subscription, canvas, KPI payloads) the request can still reach the server *after* the
        // reload's own `GET /api/v1/{locale}/shell` — an arrival-order race the per-session lock
        // (`SessionRequestSerializationFilter`, wave-3 repair-1) cannot fix, since it only
        // orders requests that have already arrived; PHP's session-lock parity covered arrival
        // order, not dispatch priority. Awaiting first makes the reload's read causally follow
        // this write by construction: `data-theme`/`data-sidebar` cannot flip until the fetch
        // promise settles, so a reload issued after that point always observes the committed
        // value. A rejected/aborted fetch still applies the DOM/state update in the `catch`, so a
        // network failure never leaves the UI stuck on the old value — nothing is logged, by
        // design, so this never trips a no-console lint rule.
        async setTheme(theme: string) {
            try {
                await fetch("/preferences/theme", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ theme }),
                    keepalive: true,
                });
            } catch {
                // network failure or abort — still applied below, see class comment above.
            }
            this.theme = theme;
            document.documentElement.dataset.theme = theme;
        },

        async setSidebar(state: string) {
            try {
                await fetch("/preferences/sidebar", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ state }),
                    keepalive: true,
                });
            } catch {
                // network failure or abort — still applied below, see class comment above.
            }
            this.sidebar = state;
            // Wave-4 repair-2: kept in sync with <html> too, not just .app (AppShell.vue's own
            // reactive :data-sidebar binding) — 03-components.css's collapse rules key off a
            // bare `[data-sidebar="collapsed"] .nav-label` selector (frozen, byte-identical to
            // the old stack's own assets/styles/03-components.css), which matches *any*
            // ancestor carrying that attribute, not just .app. SpaDocumentController stamps
            // <html data-sidebar="..."> once, at SSR time; without this line it never changes
            // again for the rest of the page's life, so reload-while-collapsed (leaving <html>
            // at "collapsed") followed by a plain toggle back to "expanded" left .app correctly
            // "expanded" while <html> stayed stale at "collapsed" — the selector still matched
            // on <html>, so nav labels stayed display:none despite .app already reporting
            // "expanded" (compare.mjs shell-navigation visual dimension, steps 13/14). Mirrors
            // setTheme's existing document.documentElement write above, which never had this gap
            // because theme has no separate .app-local attribute to fall out of sync with.
            document.documentElement.dataset.sidebar = state;
        },
    },
});
