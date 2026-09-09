import "vue-router";
import type { Component } from "vue";

declare module "vue-router" {
    interface RouteMeta {
        layout: Component;
        section: string;
        /**
         * Path params this route's own URL generator omits when they equal their default
         * value — mirrors Symfony's `path($route, $params)`, which drops a route parameter
         * that already matches its `defaults` entry (e.g. `settings/{tab}` with `defaults:
         * ['tab' => 'account']` generates `/settings`, never `/settings/account`, even while
         * the browser sits on the equivalent explicit URL). Read generically by
         * `router/localeHref.ts` so the locale toggle never needs a per-route special case;
         * a route with no omittable default simply omits this key.
         */
        defaultParams?: Record<string, string>;
    }
}
