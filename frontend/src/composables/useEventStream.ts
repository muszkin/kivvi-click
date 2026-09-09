import { onMounted, onUnmounted, ref, type Ref } from "vue";

/**
 * One event as it arrives over the Mercure SSE payload (DEV-3: JSON, not server-rendered HTML —
 * see backend EventIngestionService's class comment for why). Mirrors the fields {@code
 * EventRow.vue} renders.
 */
export interface StreamEvent {
    time: string;
    typeIcon: string;
    tone: string;
    type: string;
    detail?: string | null;
    customerId?: string | null;
    customerName?: string | null;
    siteName?: string | null;
    siteColor?: string | null;
}

/** A row in the rendered list: a {@link StreamEvent} plus a stable v-for key and flash-in flag. */
export interface StreamRow extends StreamEvent {
    key: string;
    isNew: boolean;
}

const MAX_ROWS = 80;

/**
 * Subscribes the given element to its Mercure topic and prepends arriving rows, capped at {@link
 * MAX_ROWS} — the Vue-reactive port of assets/controllers/event-stream.ts's imperative DOM
 * prepend/trim. Pause is read directly from the element's `data-paused` attribute at message time
 * (not a Vue ref): the `pause-stream` intent (useIntents.ts) sets that attribute exactly like the
 * old stack did, and the oracle's own e2e assertion (`waitAttr: ["#event-stream","data-paused",
 * "true"]`) checks the DOM attribute itself, not any internal state — reading it back the same way
 * keeps this composable and that intent in lockstep by construction, and needs no shared store for
 * page-local state that nothing else on the page reads.
 */
export function useEventStream(
    container: Ref<HTMLElement | null>,
    topic: string | undefined,
    initialEvents: StreamEvent[],
) {
    const rows = ref<StreamRow[]>(
        initialEvents.map((event, index) => ({
            ...event,
            key: `initial-${index}`,
            isNew: false,
        })),
    );
    let source: EventSource | null = null;
    let liveCounter = 0;

    function connect(): void {
        const el = container.value;
        if (!el || !topic) return;

        const hub = new URL(
            el.dataset.eventStreamHub ?? "/.well-known/mercure",
            window.location.origin,
        );
        hub.searchParams.set("topic", topic);

        el.dataset.streamState = "connecting";
        source = new EventSource(hub, { withCredentials: true });

        source.onopen = (): void => {
            el.dataset.streamState = "live";
        };

        source.onmessage = (message: MessageEvent<string>): void => {
            if (el.dataset.paused === "true") return;

            const parsed = JSON.parse(message.data) as { event: StreamEvent };
            const row: StreamRow = {
                ...parsed.event,
                key: `live-${liveCounter++}`,
                isNew: true,
            };
            rows.value = [row, ...rows.value].slice(0, MAX_ROWS);

            document.dispatchEvent(new CustomEvent("kivvi:event"));
        };

        source.onerror = (): void => {
            el.dataset.streamState = "reconnecting";
        };
    }

    onMounted(connect);
    onUnmounted(() => source?.close());

    return { rows };
}
