<script setup lang="ts">
// ORGANISM · KpiGrid — 4-up metric row (2-up under 1100px, see 03-components.css), ported from
// components/organisms/kpi-grid.html.twig.
import KpiTile from "@/components/molecules/KpiTile.vue";

export interface KpiTileData {
    label: string;
    value: string;
    unit?: string | null;
    delta?: string | null;
    dir?: "up" | "down" | "flat" | null;
    deltaIcon?: string | null;
    series?: number[];
}

const props = defineProps<{ tiles: KpiTileData[]; columns?: number }>();
</script>

<template>
    <div
        class="kpi-grid"
        :style="
            props.columns
                ? {
                      gridTemplateColumns: `repeat(${props.columns}, minmax(0, 1fr))`,
                  }
                : undefined
        "
    >
        <KpiTile
            v-for="(tile, index) in tiles"
            :key="index"
            :label="tile.label"
            :value="tile.value"
            :unit="tile.unit"
            :delta="tile.delta"
            :dir="tile.dir"
            :delta-icon="tile.deltaIcon"
            :series="tile.series"
        />
    </div>
</template>
