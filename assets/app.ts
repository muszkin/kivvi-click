/**
 * Kivvi-click — application entry point.
 * Pure TypeScript, no framework. Loaded via AssetMapper importmap.
 *
 * Architecture: ONE delegated click listener on document translates
 * [data-action] / [data-payload] attributes into named intents. Every
 * interactive component in the design system declares its behaviour
 * through those two attributes — never inline handlers.
 */

import { registerCardiogram } from "./controllers/cardiogram.ts";
import { registerEventStream } from "./controllers/event-stream.ts";
import { registerModal } from "./controllers/modal.ts";
import { registerShell } from "./controllers/shell.ts";
import { registerUpload } from "./controllers/upload.ts";
import { registerEditor } from "./controllers/editor.ts";

type ActionHandler = (
    payload: string | undefined,
    el: HTMLElement,
    ev: Event,
) => void;

const actions = new Map<string, ActionHandler>();

export function on(action: string, handler: ActionHandler): void {
    actions.set(action, handler);
}

document.addEventListener("click", (ev) => {
    const el = (ev.target as HTMLElement | null)?.closest<HTMLElement>(
        "[data-action]",
    );
    if (!el) return;
    const handler = actions.get(el.dataset.action!);
    if (!handler) return;
    handler(el.dataset.payload, el, ev);
});

/* --- global intents --------------------------------------------------- */

on("toggle-sidebar", () => {
    const app = document.querySelector<HTMLElement>(".app");
    if (!app) return;
    const next = app.dataset.sidebar === "expanded" ? "collapsed" : "expanded";
    app.dataset.sidebar = next;
    // Persist server-side so the next full page load matches.
    void fetch("/preferences/sidebar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ state: next }),
    });
});

on("set-theme", (payload) => {
    if (!payload) return;
    document.documentElement.dataset.theme = payload;
    void fetch("/preferences/theme", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ theme: payload }),
    });
});

on("open-command-bar", () =>
    document.dispatchEvent(new CustomEvent("kivvi:command-bar:open")),
);

document.addEventListener("keydown", (ev) => {
    if ((ev.metaKey || ev.ctrlKey) && ev.key.toLowerCase() === "k") {
        ev.preventDefault();
        document.dispatchEvent(new CustomEvent("kivvi:command-bar:open"));
    }
});

/* --- navigation intents ------------------------------------------------ */
// Rows and segmented controls cannot be links, so they declare where to go and
// this turns the declaration into a real navigation — history and all.

const localePrefix = (): string => `/${document.documentElement.lang || "pl"}`;

const goTo = (url: string): void => {
    window.location.href = url;
};

on("navigate", (payload) => payload && goTo(payload));
on(
    "go-customer",
    (payload) => payload && goTo(`${localePrefix()}/customers/${payload}`),
);
on(
    "go-automation",
    (payload) => payload && goTo(`${localePrefix()}/automations/${payload}`),
);
on(
    "go-email",
    (payload) => payload && goTo(`${localePrefix()}/emails/${payload}`),
);
on(
    "go-popup",
    (payload) => payload && goTo(`${localePrefix()}/popups/${payload}`),
);
on("go-page", (payload) => {
    if (!payload) return;
    const url = new URL(window.location.href);
    url.searchParams.set("page", payload);
    goTo(url.toString());
});
on(
    "set-import-step",
    (payload) => payload && goTo(`${localePrefix()}/import/${payload}`),
);

/* --- list and stream intents ------------------------------------------ */

on("pause-stream", (_payload, el) => {
    const stream = document.querySelector<HTMLElement>(".event-stream");
    if (!stream) return;
    const paused = stream.dataset.paused === "true";
    stream.dataset.paused = paused ? "false" : "true";
    el.dataset.active = paused ? "false" : "true";
});

on(
    "copy-variable",
    (payload) => payload && void navigator.clipboard?.writeText(payload),
);
on("copy-dns", (payload, el) => {
    const value = el
        .closest(".dns-row")
        ?.querySelector(".mono")
        ?.textContent?.trim();
    if (value) void navigator.clipboard?.writeText(value);
});
on(
    "copy-api-key",
    (payload) => payload && void navigator.clipboard?.writeText(payload),
);
on("copy-snippet", () => {
    const snippet = document.querySelector(".code-block")?.textContent;
    if (snippet) void navigator.clipboard?.writeText(snippet);
});

/* --- controller mounting ---------------------------------------------- */
// Every [data-controller="x"] element gets its registrar called once.
const registrars: Record<string, (el: HTMLElement) => void> = {
    cardiogram: registerCardiogram,
    "event-stream": registerEventStream,
    modal: registerModal,
    shell: registerShell,
    upload: registerUpload,
    editor: registerEditor,
};

export function mount(root: ParentNode = document): void {
    root.querySelectorAll<HTMLElement>("[data-controller]").forEach((el) => {
        el.dataset
            .controller!.split(" ")
            .forEach((name) => registrars[name]?.(el));
    });
}

mount();
