import { createRouter, createWebHistory } from "vue-router";
import { routes } from "./routes";
import { scrollBehavior } from "./scrollRestoration";

export const router = createRouter({
    history: createWebHistory(),
    routes,
    // Hydration-aware equivalent of the browser's own reload/back-forward scroll restoration —
    // see scrollRestoration.ts's own doc comment for how vue-router wires this in (it manages
    // history.scrollRestoration and the popstate/pagehide listeners itself once this is set).
    scrollBehavior,
});
