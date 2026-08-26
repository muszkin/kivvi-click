/**
 * Editor — shared drag-and-drop for the email and popup composers.
 * Blocks are dragged from .ee-block-lib into the canvas; the server returns
 * re-rendered canvas HTML so Twig stays the single source of markup.
 */
export function registerEditor(root: HTMLElement): void {
    if (root.dataset.mounted) return;
    root.dataset.mounted = "true";

    const canvas = root.querySelector<HTMLElement>(".ee-canvas-wrap");

    root.querySelectorAll<HTMLElement>("[data-block-type]").forEach(
        (source) => {
            source.addEventListener("dragstart", (ev) => {
                (ev as DragEvent).dataTransfer?.setData(
                    "text/plain",
                    source.dataset.blockType!,
                );
            });
        },
    );

    canvas?.addEventListener("dragover", (ev) => ev.preventDefault());
    canvas?.addEventListener("drop", (ev) => {
        ev.preventDefault();
        const type = (ev as DragEvent).dataTransfer?.getData("text/plain");
        if (!type) return;
        const endpoint =
            (root.dataset.editorEndpoint ?? window.location.pathname) +
            "/blocks";
        void fetch(endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ type, index: null }),
        })
            .then((r) => r.text())
            .then((html) => {
                canvas.innerHTML = html;
            });
    });
}
