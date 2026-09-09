<script setup lang="ts">
// MOLECULE · SimulationCard — ported from pages/automations/simulation.html.twig. Three read-outs
// from the rule simulation over historical events; the middle value carries inline HTML (a muted
// percentage span), hence v-html (see eslint.config.js's vue/no-v-html exemption) — never
// reflected user input, always AutomationsFixtures' own fixed demo text.
export interface SimulationReadout {
    label: string;
    value: string;
    note: string;
    color?: string | null;
}

defineProps<{ items: SimulationReadout[] }>();
</script>

<template>
    <div
        style="
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 14px;
        "
    >
        <div v-for="(item, index) in items" :key="index">
            <div
                class="muted"
                style="
                    font-size: 11.5px;
                    margin-bottom: 4px;
                    text-transform: uppercase;
                    letter-spacing: 0.06em;
                "
            >
                {{ item.label }}
            </div>
            <div
                class="mono"
                style="
                    font-size: 24px;
                    font-weight: 500;
                    letter-spacing: -0.02em;
                "
                :style="{ color: item.color ?? undefined }"
                v-html="item.value"
            ></div>
            <div class="muted" style="font-size: 11.5px">{{ item.note }}</div>
        </div>
    </div>
</template>
