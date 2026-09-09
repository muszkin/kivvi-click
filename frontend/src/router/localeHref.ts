import type { Router, RouteLocationNormalizedLoaded } from "vue-router";

/**
 * Builds the current page's URL under a different locale — the Topbar "PL"/"EN" toggle's
 * href. Generic port of Symfony's `path($route, $routeParams)`: the current route's own PATH
 * PARAMETERS ONLY (never the query string or hash — a query like `?page=2` on the customers
 * index is not a `_route_params` entry either, so it never survives a locale switch), with any
 * parameter that already equals its route's declared default OMITTED, exactly like Symfony's
 * URL generator drops a route parameter equal to its `defaults` entry (repair-1, R1-B: settings'
 * own `defaults: ['tab' => 'account']` is why the "PL" link at `/pl/settings/account` targets
 * `/en/settings`, not `/en/settings/account` — see the oracle's
 * `journeys/settings/steps/1/a11y.json`). A route with no `meta.defaultParams` behaves exactly
 * like the old plain locale-prefix swap.
 *
 * Journey-agnostic on purpose: a later journey (e.g. import-wizard's `defaults: ['step' =>
 * ImportWizard::FIRST_STEP]`) only needs to add its own `meta.defaultParams` entry in
 * `routes.ts` — this function and Topbar.vue never need a second special case.
 */
export function buildLocaleHref(
    router: Router,
    route: RouteLocationNormalizedLoaded,
    targetLocale: string,
): string {
    const defaults = route.meta.defaultParams ?? {};
    const params: Record<string, string> = { locale: targetLocale };
    for (const [key, rawValue] of Object.entries(route.params)) {
        if (key === "locale") continue;
        const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
        if (value === undefined || value === "") continue;
        if (defaults[key] === value) continue;
        params[key] = value;
    }
    return router.resolve({ name: route.name ?? undefined, params }).path;
}
