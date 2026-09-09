<script setup lang="ts">
// MOLECULE · MapRow — ported from components/molecules/map-row.html.twig. One row of the import
// column-mapping table: source column, sample values, target select, confidence. First (and
// only, so far) owner: import-wizard.
import { computed } from "vue";

export interface MapRowTarget {
    value: string;
    label: string;
}

const props = withDefaults(
    defineProps<{
        letter: string;
        name: string;
        samples: string[];
        targets: MapRowTarget[];
        mapped?: string;
        confidence?: number;
        skipped?: boolean;
    }>(),
    { mapped: "", confidence: 0, skipped: false },
);

const confidenceColor = computed(() => {
    const c = props.confidence ?? 0;
    if (c === 0) return "var(--fg-muted)";
    if (c >= 90) return "var(--good)";
    if (c >= 70) return "oklch(0.55 0.10 70)";
    return "var(--bad)";
});
</script>

<template>
    <div class="map-row" :data-skipped="skipped ? 'true' : 'false'">
        <div class="map-col-name">
            <div class="row" style="gap: 8px">
                <span class="col-letter mono">{{ letter }}</span>
                <strong style="font-size: 13px">{{ name }}</strong>
            </div>
        </div>
        <div class="map-col-sample">
            <span
                v-for="(sample, index) in samples"
                :key="index"
                class="sample-pill mono"
                >{{ sample }}</span
            >
        </div>
        <div class="map-col-target">
            <select class="select" :name="`map[${letter}]`">
                <option
                    v-for="target in targets"
                    :key="target.value"
                    :value="target.value"
                    :selected="target.value === mapped"
                >
                    {{ target.label }}
                </option>
            </select>
        </div>
        <div class="map-col-conf right">
            <span
                v-if="(confidence ?? 0) > 0"
                class="mono"
                :style="{ color: confidenceColor, fontWeight: 500 }"
                >{{ confidence }}%</span
            >
            <span v-else class="muted mono">—</span>
        </div>
    </div>
</template>
