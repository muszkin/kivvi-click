/**
 * Event stream — Mercure subscriber.
 *
 * The server renders the first page of rows; this only PREPENDS.
 * Rows arrive as ready-made HTML fragments so the row markup lives in
 * exactly one place (event-row.html.twig) instead of being duplicated in TS.
 */

const MAX_ROWS = 80;

export function registerEventStream(el: HTMLElement): void {
    if (el.dataset.mounted) return;
    el.dataset.mounted = "true";

    const topic = el.dataset.eventStreamTopic;
    if (!topic) return;

    const hub = new URL(
        el.dataset.eventStreamHub ?? "/.well-known/mercure",
        window.location.origin,
    );
    hub.searchParams.append("topic", topic);

    const source = new EventSource(hub, { withCredentials: true });

    source.onmessage = (message: MessageEvent<string>): void => {
        const { html } = JSON.parse(message.data) as { html: string };
        const tpl = document.createElement("template");
        tpl.innerHTML = html.trim();
        const row = tpl.content.firstElementChild;
        if (!row) return;

        row.classList.add("new"); // 800ms flash-in
        el.prepend(row);
        while (el.children.length > MAX_ROWS) el.lastElementChild?.remove();

        document.dispatchEvent(new CustomEvent("kivvi:event")); // feeds the cardiogram
    };

    source.onerror = (): void => {
        // EventSource reconnects on its own; surface prolonged outages in the UI.
        el.dataset.streamState = "reconnecting";
    };
}
