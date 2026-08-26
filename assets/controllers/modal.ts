/**
 * Modal — scrim dismissal, Escape, focus trap.
 * The scrim itself carries data-action="close-modal"; clicks inside the
 * panel must not bubble to it.
 */
export function registerModal(scrim: HTMLElement): void {
    if (scrim.dataset.mounted) return;
    scrim.dataset.mounted = "true";

    const panel = scrim.querySelector<HTMLElement>(
        '[data-modal-target="panel"]',
    );
    panel?.addEventListener("click", (ev) => ev.stopPropagation());

    const previouslyFocused = document.activeElement as HTMLElement | null;
    const focusables = (): HTMLElement[] =>
        Array.from(
            panel?.querySelectorAll<HTMLElement>(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
            ) ?? [],
        ).filter((n) => !n.hasAttribute("disabled"));

    focusables()[0]?.focus();

    const close = (): void => {
        scrim.remove();
        previouslyFocused?.focus();
    };

    document.addEventListener("keydown", function onKey(ev) {
        if (!scrim.isConnected) {
            document.removeEventListener("keydown", onKey);
            return;
        }
        if (ev.key === "Escape") {
            close();
            return;
        }
        if (ev.key !== "Tab") return;

        const items = focusables();
        const first = items[0];
        const last = items[items.length - 1];
        if (!first || !last) return;
        if (ev.shiftKey && document.activeElement === first) {
            ev.preventDefault();
            last.focus();
        } else if (!ev.shiftKey && document.activeElement === last) {
            ev.preventDefault();
            first.focus();
        }
    });
}
