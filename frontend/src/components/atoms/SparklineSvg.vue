<script setup lang="ts">
import { computed } from "vue";

/**
 * Inline SVG trend line with area fill, computed from a value array — no chart library, no
 * JS-driven animation. Port of components/atoms/sparkline.html.twig.
 */
const props = withDefaults(
    defineProps<{
        values: number[];
        w?: number;
        h?: number;
        stroke?: string;
        fill?: string;
    }>(),
    { w: 240, h: 38, stroke: "var(--accent)", fill: "var(--accent-soft)" },
);

function round1(value: number): number {
    return Math.round(value * 10) / 10;
}

const linePath = computed(() => {
    const max = Math.max(...props.values);
    const min = Math.min(...props.values);
    const range = max - min || 1;
    const step = props.w / (props.values.length - 1);
    const points = props.values.map((value, i) => {
        const x = round1(i * step);
        const y = round1(props.h - ((value - min) / range) * (props.h - 4) - 2);
        return `${x},${y}`;
    });
    return `M${points.join(" L")}`;
});

const areaPath = computed(
    () => `${linePath.value} L${props.w},${props.h} L0,${props.h} Z`,
);
</script>

<template>
    <svg
        :viewBox="`0 0 ${w} ${h}`"
        preserveAspectRatio="none"
        width="100%"
        height="100%"
        aria-hidden="true"
    >
        <path :d="areaPath" :fill="fill" opacity="0.5" />
        <path
            :d="linePath"
            :stroke="stroke"
            stroke-width="1.6"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
        />
    </svg>
</template>
