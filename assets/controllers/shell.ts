/**
 * Shell — the app frame. Keeps canvas-based children sized when the
 * sidebar grid column animates (220ms), since canvas does not reflow.
 */
export function registerShell(app: HTMLElement): void {
    if (app.dataset.mounted) return;
    app.dataset.mounted = "true";

    app.addEventListener("transitionend", (ev) => {
        if ((ev as TransitionEvent).propertyName !== "grid-template-columns")
            return;
        window.dispatchEvent(new Event("resize"));
    });
}
