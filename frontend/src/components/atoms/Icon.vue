<script setup lang="ts">
import { computed } from "vue";
import { ICONS } from "@/lib/icons";

const props = defineProps<{
    name: string;
    class?: string;
    size?: number;
}>();

const path = computed(() => ICONS[props.name] ?? null);
const classes = computed(() => ["icon", props.class].filter(Boolean).join(" "));
</script>

<template>
    <svg
        v-if="path"
        :class="classes"
        :width="size"
        :height="size"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
        v-html="path"
    ></svg>
</template>
