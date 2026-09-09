<script setup lang="ts">
import { computed } from "vue";
import Icon from "@/components/atoms/Icon.vue";

const props = withDefaults(
    defineProps<{
        text: string;
        icon?: string;
        tone?: "info" | "accent" | "warn" | "bad";
    }>(),
    { icon: "info", tone: "info" },
);

const TONE_CLASS: Record<string, string> = {
    info: "callout",
    accent: "auto-detect",
    warn: "callout callout--warn",
    bad: "feed-card__err",
};

const rootClass = computed(() => TONE_CLASS[props.tone] ?? TONE_CLASS.info);
</script>

<template>
    <div :class="rootClass">
        <Icon :name="icon" />
        <span v-html="text"></span>
    </div>
</template>
