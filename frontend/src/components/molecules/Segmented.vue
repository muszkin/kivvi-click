<script setup lang="ts">
// MOLECULE · Segmented — ported from components/molecules/segmented.html.twig. A mutually
// exclusive control, 2-4 short options (the event stream's range picker: 5m/1h/24h/7d).
import Icon from "@/components/atoms/Icon.vue";

export interface SegmentedOption {
    value: string;
    label: string;
    icon?: string;
    active?: boolean;
}

withDefaults(
    defineProps<{ options: SegmentedOption[]; action?: string | null }>(),
    {
        action: null,
    },
);
</script>

<template>
    <div class="seg" role="tablist">
        <button
            v-for="option in options"
            :key="option.value"
            role="tab"
            :aria-selected="option.active ? 'true' : 'false'"
            :data-active="option.active ? 'true' : 'false'"
            :data-action="action ?? undefined"
            :data-payload="action ? option.value : undefined"
        >
            <Icon v-if="option.icon" :name="option.icon" />
            {{ option.label }}
        </button>
    </div>
</template>
