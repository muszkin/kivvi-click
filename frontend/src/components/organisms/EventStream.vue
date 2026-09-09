<script setup lang="ts">
// ORGANISM · EventStream — ported from components/organisms/event-stream.html.twig. Server-render
// the first page (the `events` prop); useEventStream subscribes to Mercure and prepends rows with
// class "new" (800ms flash-in), trimming past the 80-row cap.
import { ref } from "vue";
import { useEventStream, type StreamEvent } from "@/composables/useEventStream";
import EventRow from "@/components/molecules/EventRow.vue";

const props = defineProps<{ events: StreamEvent[]; topic?: string | null }>();

const container = ref<HTMLElement | null>(null);
const { rows } = useEventStream(
    container,
    props.topic ?? undefined,
    props.events,
);
</script>

<template>
    <div
        id="event-stream"
        ref="container"
        class="event-stream"
        :data-controller="topic ? 'event-stream' : undefined"
        :data-event-stream-topic="topic ?? undefined"
        aria-live="polite"
        aria-relevant="additions"
    >
        <EventRow
            v-for="row in rows"
            :key="row.key"
            :time="row.time"
            :type-icon="row.typeIcon"
            :tone="row.tone"
            :type="row.type"
            :detail="row.detail"
            :customer-name="row.customerName"
            :customer-id="row.customerId"
            :site-name="row.siteName"
            :site-color="row.siteColor"
            :is-new="row.isNew"
        />
    </div>
</template>
