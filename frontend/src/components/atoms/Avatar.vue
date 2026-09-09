<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{ name: string; size?: number; text?: string }>(),
    {
        size: 30,
    },
);

const initials = computed(
    () =>
        props.text ??
        props.name
            .split(" ")
            .slice(0, 2)
            .map((word) => word.charAt(0))
            .join("")
            .toUpperCase(),
);
const fontSize = computed(() => Math.round(props.size * 0.4));
</script>

<template>
    <span
        class="avatar"
        :style="{
            width: `${size}px`,
            height: `${size}px`,
            fontSize: `${fontSize}px`,
        }"
        >{{ initials }}</span
    >
</template>
