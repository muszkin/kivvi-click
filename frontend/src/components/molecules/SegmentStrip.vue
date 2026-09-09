<script setup lang="ts">
// MOLECULE · SegmentStrip — the customers-index segment rail: one filter-chip-shaped button per
// segment (see components/molecules/filter-chip.html.twig, whose markup this reproduces). Named
// for its one caller rather than "FilterChip": the general-purpose filter-chip/segmented-control
// molecules belong to the event-stream slice landing in the same wave, so this component only
// covers what the customers index needs, not the shared molecule those other pages will reuse.
import Icon from "@/components/atoms/Icon.vue";

export interface SegmentTile {
    label: string;
    icon?: string | null;
    count?: string | null;
    active?: boolean;
    action?: string;
    payload?: string;
}

const props = defineProps<{ segments: SegmentTile[] }>();

// filter-chip.html.twig: "{{ label }}{% if count is defined %} · {{ count }}{% endif %}" — built
// as one string here rather than adjacent interpolations, so no whitespace-condensing rule can
// swallow (or invent) the space around the middot.
function segmentText(segment: SegmentTile): string {
    return segment.count != null
        ? `${segment.label} · ${segment.count}`
        : segment.label;
}
</script>

<template>
    <button
        v-for="(segment, index) in props.segments"
        :key="index"
        class="filter-chip"
        :data-active="segment.active ? 'true' : 'false'"
        :data-action="segment.action"
        :data-payload="segment.action ? (segment.payload ?? '') : undefined"
    >
        <Icon v-if="segment.icon" :name="segment.icon" />{{
            segmentText(segment)
        }}
    </button>
</template>
