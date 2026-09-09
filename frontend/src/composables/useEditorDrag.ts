import { onMounted, onUnmounted, type Ref } from "vue";

/**
 * Wires the block-library → canvas drag-and-drop shared by the e-mail and popup editors — the
 * Vue port of assets/controllers/editor.ts's registerEditor(), event-delegated off the given
 * container so BlockLibrary's buttons and the canvas wrapper need no refs of their own.
 *
 * DEV-7: the old stack's drop handler POSTs to `.../blocks` and swaps the canvas's innerHTML with
 * the response — a call that 404s today (a dead endpoint, R6). The migration plan's accepted
 * deviation is that the SPA makes NO request at all here: a block dropped on the canvas visibly
 * does nothing, exactly like the old stack's own (broken) observable behaviour.
 */
export function useEditorDrag(container: Ref<HTMLElement | null>): void {
    let root: HTMLElement | null = null;

    function onDragStart(ev: DragEvent): void {
        const source = (ev.target as HTMLElement | null)?.closest<HTMLElement>(
            "[data-block-type]",
        );
        if (!source) return;
        ev.dataTransfer?.setData("text/plain", source.dataset.blockType ?? "");
    }

    function onDragOver(ev: DragEvent): void {
        if ((ev.target as HTMLElement | null)?.closest(".ee-canvas-wrap")) {
            ev.preventDefault();
        }
    }

    function onDrop(ev: DragEvent): void {
        if (!(ev.target as HTMLElement | null)?.closest(".ee-canvas-wrap"))
            return;
        ev.preventDefault();
    }

    onMounted(() => {
        root = container.value;
        root?.addEventListener("dragstart", onDragStart);
        root?.addEventListener("dragover", onDragOver);
        root?.addEventListener("drop", onDrop);
    });

    onUnmounted(() => {
        root?.removeEventListener("dragstart", onDragStart);
        root?.removeEventListener("dragover", onDragOver);
        root?.removeEventListener("drop", onDrop);
    });
}
