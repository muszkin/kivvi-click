<script setup lang="ts">
// ATOM · Sparkline — inline SVG trend line with area fill, ported from
// components/atoms/sparkline.html.twig. Client-computed from a value array (no formatting: this
// is geometry, not a number the SPA is putting on screen as text).
import { computed } from "vue";

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

const path = computed(() => {
    const max = Math.max(...props.values);
    const min = Math.min(...props.values);
    const range = max - min || 1;
    const step = props.w / (props.values.length - 1);
    const points = props.values.map((value, index) => {
        const x = round1(index * step);
        const y = round1(props.h - ((value - min) / range) * (props.h - 4) - 2);
        return `${x},${y}`;
    });
    return `M${points.join(" L")}`;
});

const areaPath = computed(
    () => `${path.value} L${props.w},${props.h} L0,${props.h} Z`,
);

function round1(value: number): number {
    return Math.round(value * 10) / 10;
}
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
            :d="path"
            :stroke="stroke"
            stroke-width="1.6"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
        />
    </svg>
</template>
