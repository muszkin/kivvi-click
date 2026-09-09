<script setup lang="ts">
import { computed } from "vue";
import Icon from "./Icon.vue";

const props = withDefaults(
    defineProps<{
        label: string;
        tone?:
            | "neutral"
            | "good"
            | "warn"
            | "bad"
            | "info"
            | "accent"
            | "brown";
        icon?: string;
        live?: boolean;
        mono?: boolean;
    }>(),
    { tone: "neutral" },
);

const classes = computed(() =>
    [
        "chip",
        props.tone !== "neutral" ? props.tone : "",
        props.mono ? "mono" : "",
    ]
        .filter(Boolean)
        .join(" "),
);
</script>

<template>
    <span :class="classes">
        <span v-if="live" class="dot-wrap"><span class="dot live"></span></span>
        <Icon v-if="icon" :name="icon" />
        {{ label }}
    </span>
</template>
