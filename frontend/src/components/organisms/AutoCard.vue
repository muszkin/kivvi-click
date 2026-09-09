<script setup lang="ts">
// ORGANISM · AutoCard — ported from components/organisms/list-card.html.twig. The 4-column summary
// row used by automation and popup lists: title + meta chips, then up to three right-aligned
// metrics. Only the automations journey renders one so far; the `actions` slot exists for parity
// with the Twig contract's own optional `actions` param, unused here (see FilterChip.vue's `count`
// prop for the same kept-for-parity reasoning).
import Chip from "@/components/atoms/Chip.vue";

export interface AutoCardChip {
    label: string;
    tone?: "neutral" | "good" | "warn" | "bad" | "info" | "accent" | "brown";
}

export interface AutoCardMetric {
    value: string;
    label: string;
    width?: number;
    color?: string | null;
}

withDefaults(
    defineProps<{
        title: string;
        chips?: AutoCardChip[];
        metrics?: AutoCardMetric[];
        action?: string | null;
        payload?: string | null;
    }>(),
    {
        chips: () => [],
        metrics: () => [],
        action: null,
        payload: null,
    },
);
</script>

<template>
    <div
        class="auto-card"
        :data-action="action ?? undefined"
        :data-payload="action ? (payload ?? '') : undefined"
    >
        <div>
            <div class="auto-card__title">{{ title }}</div>
            <div v-if="chips.length" class="auto-card__meta">
                <Chip
                    v-for="(chip, index) in chips"
                    :key="index"
                    :label="chip.label"
                    :tone="chip.tone"
                />
            </div>
        </div>
        <div
            v-for="(metric, index) in metrics"
            :key="index"
            class="auto-card__num right"
            :style="{
                minWidth: `${metric.width ?? 90}px`,
                color: metric.color ?? undefined,
            }"
        >
            {{ metric.value }}<span class="sub">{{ metric.label }}</span>
        </div>
        <div v-if="$slots.actions" class="row" style="gap: 4px">
            <slot name="actions" />
        </div>
    </div>
</template>
