/**
 * Upload — dropzone drag/drop + file selection for the import wizard.
 * Posts to the import endpoint; the server advances the stepper and redirects.
 */
export function registerUpload(zone: HTMLElement): void {
    if (zone.dataset.mounted) return;
    zone.dataset.mounted = "true";

    const input = zone.querySelector<HTMLInputElement>(
        '[data-upload-target="input"]',
    );

    const send = (file: File): void => {
        const body = new FormData();
        body.append("file", file);
        void fetch("/import/upload", { method: "POST", body }).then((r) => {
            if (r.redirected) window.location.href = r.url;
        });
    };

    ["dragenter", "dragover"].forEach((type) =>
        zone.addEventListener(type, (ev) => {
            ev.preventDefault();
            zone.dataset.dragging = "true";
        }),
    );
    ["dragleave", "drop"].forEach((type) =>
        zone.addEventListener(type, () => {
            delete zone.dataset.dragging;
        }),
    );

    zone.addEventListener("drop", (ev) => {
        ev.preventDefault();
        const file = (ev as DragEvent).dataTransfer?.files?.[0];
        if (file) send(file);
    });

    input?.addEventListener("change", () => {
        const file = input.files?.[0];
        if (file) send(file);
    });
}
