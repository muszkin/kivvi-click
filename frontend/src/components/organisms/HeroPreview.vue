<script setup lang="ts">
import SparklineSvg from "@/components/atoms/SparklineSvg.vue";

export interface PreviewTile {
    label: string;
    value: string;
    unit?: string;
}

const props = defineProps<{ tiles: PreviewTile[]; series: number[] }>();

/** Mirrors preview-chart.html.twig's `series|last|round`. */
const lastEventRate = Math.round(props.series[props.series.length - 1] ?? 0);
</script>

<template>
    <section class="hero-preview">
        <div class="frame">
            <div
                class="frame-inner"
                style="padding: 0; overflow: hidden; background: var(--bg-card)"
            >
                <div
                    class="row"
                    style="
                        padding: 10px 14px;
                        background: var(--bg-2);
                        border-bottom: 1px solid var(--line);
                        gap: 6px;
                    "
                >
                    <div
                        style="
                            width: 11px;
                            height: 11px;
                            border-radius: 999px;
                            background: oklch(0.78 0.12 28);
                        "
                    ></div>
                    <div
                        style="
                            width: 11px;
                            height: 11px;
                            border-radius: 999px;
                            background: oklch(0.85 0.12 80);
                        "
                    ></div>
                    <div
                        style="
                            width: 11px;
                            height: 11px;
                            border-radius: 999px;
                            background: oklch(0.78 0.1 150);
                        "
                    ></div>
                    <div
                        class="mono"
                        style="
                            margin-left: 14px;
                            font-size: 11px;
                            color: var(--fg-muted);
                        "
                    >
                        app.kivvi-click.io/aureashop/dashboard
                    </div>
                </div>
                <div
                    style="
                        display: grid;
                        grid-template-columns: repeat(4, minmax(0, 1fr));
                        gap: 14px;
                        padding: 24px;
                    "
                >
                    <div class="kpi" v-for="tile in tiles" :key="tile.label">
                        <div class="kpi-label">{{ tile.label }}</div>
                        <div class="kpi-value">
                            {{ tile.value
                            }}<span v-if="tile.unit" class="unit">{{
                                tile.unit
                            }}</span>
                        </div>
                    </div>
                </div>
                <div style="padding: 0 24px 24px">
                    <div style="position: relative; height: 120px">
                        <SparklineSvg :values="series" :w="900" :h="120" />
                        <span
                            class="mono"
                            style="
                                position: absolute;
                                top: 4px;
                                right: 8px;
                                font-size: 11px;
                                color: var(--fg-subtle);
                            "
                            >{{ lastEventRate }} ev/s</span
                        >
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>
