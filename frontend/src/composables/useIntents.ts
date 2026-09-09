import { onMounted, onUnmounted } from "vue";
import { useShellStore } from "@/stores/shell";

type ActionHandler = (
    payload: string | undefined,
    el: HTMLElement,
    ev: Event,
) => void;

const COMMAND_BAR_OPEN_EVENT = "kivvi:command-bar:open";

/**
 * Declarative click dispatch: every interactive element in the design system declares its
 * behaviour through `data-action`/`data-payload`, one delegated document listener
 * translates that into a named intent — the Vue port of assets/app.ts, kept as a single
 * dispatcher so markup never needs an inline handler wired per component.
 */
export function useIntents(): void {
    const shell = useShellStore();
    const actions = new Map<string, ActionHandler>();

    const on = (action: string, handler: ActionHandler): void => {
        actions.set(action, handler);
    };

    on("toggle-sidebar", () => {
        const next = shell.sidebar === "expanded" ? "collapsed" : "expanded";
        void shell.setSidebar(next);
    });

    on("set-theme", (payload) => {
        if (!payload) return;
        void shell.setTheme(payload);
    });

    on("open-command-bar", () => {
        document.dispatchEvent(new CustomEvent(COMMAND_BAR_OPEN_EVENT));
    });

    // Rows, segmented controls and other non-<a> triggers cannot be real links, so they
    // declare where to go and this turns the declaration into a real, full navigation —
    // history and all, the same document request the oracle records for every transition.
    on("navigate", (payload) => {
        if (payload) {
            window.location.href = payload;
        }
    });

    // A customers-index row declares only the customer id; this builds the same
    // locale-prefixed URL assets/app.ts's own "go-customer" intent always has.
    on("go-customer", (payload) => {
        if (!payload) return;
        window.location.href = `/${shell.locale}/customers/${payload}`;
    });

    // An automations-index card declares only the automation id, mirroring "go-customer"
    // above; assets/app.ts registers this same intent (unused by any of its own templates —
    // automations.html.twig resolves the row's action/payload to "navigate" plus a
    // server-generated URL instead), but the name and the URL it builds are the old stack's
    // own, kept here as the simpler equivalent: same click, same destination.
    on("go-automation", (payload) => {
        if (!payload) return;
        window.location.href = `/${shell.locale}/automations/${payload}`;
    });

    // A campaigns-index row declares only the campaign id; this builds the same
    // locale-prefixed URL assets/app.ts's own "go-email" intent always has.
    on("go-email", (payload) => {
        if (!payload) return;
        window.location.href = `/${shell.locale}/emails/${payload}`;
    });

    on("go-page", (payload) => {
        if (!payload) return;
        const url = new URL(window.location.href);
        url.searchParams.set("page", payload);
        window.location.href = url.toString();
    });

    // The old stack never wired a handler for this action either (assets/app.ts has no
    // "set-segment" entry): the segment rail is visual-only until a later journey adds real
    // filtering. Registered so the intent name exists for that journey to extend, not because
    // it does anything yet — reproduces the old stack's do-nothing click exactly.
    on("set-segment", () => {});

    // Ported verbatim from assets/app.ts's own copy-* intents (settings journey: API keys, DNS
    // records, the tracker snippet).
    on("copy-api-key", (payload) => {
        if (payload) void navigator.clipboard?.writeText(payload);
    });

    // The DNS row's own payload is the record name ("SPF"), but assets/app.ts's handler ignores
    // it and copies the record's *value* instead — reproduced verbatim, quirk and all.
    on("copy-dns", (_payload, el) => {
        const value = el
            .closest(".dns-row")
            ?.querySelector(".mono")
            ?.textContent?.trim();
        if (value) void navigator.clipboard?.writeText(value);
    });

    on("copy-snippet", () => {
        const snippet = document.querySelector(".code-block")?.textContent;
        if (snippet) void navigator.clipboard?.writeText(snippet);
    });

    // Page-local state with nothing else on the page reading it, so it lives on the DOM
    // attribute directly (assets/app.ts's own approach) rather than in a store: the
    // useEventStream composable checks the same `data-paused` attribute at message time, and
    // the oracle's own e2e assertion (`waitAttr: ["#event-stream","data-paused","true"]`)
    // checks that attribute too — see useEventStream.ts's class comment.
    // The e-mail editor's "Zmienne" list copies a placeholder token to the clipboard — mirrors
    // assets/app.ts's own "copy-variable" intent.
    on("copy-variable", (payload) => {
        if (!payload) return;
        void navigator.clipboard?.writeText(payload);
    });

    on("pause-stream", (_payload, el) => {
        const stream = document.querySelector<HTMLElement>(".event-stream");
        if (!stream) return;
        const paused = stream.dataset.paused === "true";
        stream.dataset.paused = paused ? "false" : "true";
        el.dataset.active = paused ? "false" : "true";
    });

    const onClick = (ev: MouseEvent): void => {
        const el = (ev.target as HTMLElement | null)?.closest<HTMLElement>(
            "[data-action]",
        );
        if (!el) return;
        const action = el.dataset.action;
        if (!action) return;
        const handler = actions.get(action);
        if (!handler) return;
        handler(el.dataset.payload, el, ev);
    };

    const onKeydown = (ev: KeyboardEvent): void => {
        if ((ev.metaKey || ev.ctrlKey) && ev.key.toLowerCase() === "k") {
            ev.preventDefault();
            document.dispatchEvent(new CustomEvent(COMMAND_BAR_OPEN_EVENT));
        }
    };

    onMounted(() => {
        document.addEventListener("click", onClick);
        document.addEventListener("keydown", onKeydown);
    });

    onUnmounted(() => {
        document.removeEventListener("click", onClick);
        document.removeEventListener("keydown", onKeydown);
    });
}
