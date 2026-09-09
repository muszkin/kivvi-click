<script setup lang="ts">
// MOLECULE · KpiTile — single metric tile, ported from components/molecules/kpi-tile.html.twig.
// Sparkline bleeds into the bottom 38px at 45% opacity (.kpi-spark, see 03-components.css).
import { computed } from "vue";
import Icon from "@/components/atoms/Icon.vue";
import Sparkline from "@/components/atoms/Sparkline.vue";

const props = defineProps<{
    label: string;
    value: string;
    unit?: string | null;
    delta?: string | null;
    dir?: "up" | "down" | "flat" | null;
    deltaIcon?: string | null;
    series?: number[];
}>();

const deltaTone = computed(() => props.dir ?? "flat");
const resolvedDeltaIcon = computed(
    () => props.deltaIcon ?? (props.dir === "down" ? "arrow_down" : "arrow_up"),
);
</script>

<template>
    <div class="kpi">
        <div class="kpi-label">{{ label }}</div>
        <div class="kpi-value">
            {{ value }}<span v-if="unit" class="unit">{{ unit }}</span>
        </div>
        <div v-if="delta" :class="['kpi-delta', deltaTone]">
            <Icon :name="resolvedDeltaIcon" />
            <span>{{ delta }}</span>
        </div>
        <div v-if="series" class="kpi-spark">
            <Sparkline :values="series" />
        </div>
    </div>
</template>
