<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(defineProps<{ size?: number; mono?: boolean }>(), {
    size: 22,
    mono: false,
});

const width = computed(() => Math.round(props.size * 1.4));
</script>

<template>
    <svg :width="width" :height="size" viewBox="0 0 56 40" aria-hidden="true">
        <ellipse cx="34" cy="24" rx="18" ry="14" fill="currentColor" />
        <polygon
            points="16,25 -2,21 16,29"
            :fill="mono ? 'currentColor' : 'var(--brown)'"
        />
        <circle cx="40" cy="20" r="1.8" fill="var(--bg-card)" />
        <template v-if="!mono">
            <circle cx="40.4" cy="19.7" r="0.7" fill="currentColor" />
            <rect
                x="28"
                y="36"
                width="1.6"
                height="4"
                rx="0.6"
                fill="var(--brown)"
            />
            <rect
                x="36"
                y="36"
                width="1.6"
                height="4"
                rx="0.6"
                fill="var(--brown)"
            />
            <circle cx="48" cy="13" r="2.2" fill="var(--accent)" />
        </template>
    </svg>
</template>
