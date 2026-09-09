<script setup lang="ts">
// ATOM · Bar — horizontal usage/coverage bar, ported from components/atoms/bar.html.twig (the
// old stack has no separate "bar row" component: this one atom renders the whole `.bar-row`).
// Used in the feeds price-matching diagnostic; later journeys reuse it for API limits and plan
// usage (see pages/settings/limit-bars.html.twig, which is the same atom).
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
        label: string;
        pct: number;
        value?: string;
        tone?: "accent" | "brown" | "bad";
        labelWidth?: number;
    }>(),
    { tone: "accent", labelWidth: 170 },
);

const TONE_COLOR: Record<string, string> = {
    accent: "var(--accent)",
    brown: "var(--brown)",
    bad: "var(--bad)",
};

const isBad = computed(() => props.tone === "bad");
const fillColor = computed(() => TONE_COLOR[props.tone]);
</script>

<template>
    <div class="bar-row">
        <span
            :style="{
                width: `${labelWidth}px`,
                fontSize: '12.5px',
                color: isBad ? 'var(--bad)' : undefined,
            }"
            >{{ label }}</span
        >
        <div class="bar">
            <div
                class="bar-fill"
                :style="{ width: `${pct}%`, background: fillColor }"
            ></div>
        </div>
        <span
            v-if="value"
            class="mono"
            :style="{
                fontSize: '12px',
                minWidth: '90px',
                textAlign: 'right',
                color: isBad ? 'var(--bad)' : undefined,
            }"
            >{{ value }}</span
        >
    </div>
</template>
