<script setup lang="ts">
// MOLECULE · TriggerRow — ported from components/molecules/trigger-row.html.twig. Popup trigger
// line with an optional inline numeric threshold. `value`/`unit` accompany the two numeric
// triggers ("czas na stronie", "scroll"); `note` accompanies the qualitative one ("exit intent") —
// never both on the same row, mirrors the fixture data's own shape.
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";

withDefaults(
    defineProps<{
        label: string;
        tone?:
            | "neutral"
            | "good"
            | "warn"
            | "bad"
            | "info"
            | "accent"
            | "brown";
        value?: string | null;
        unit?: string | null;
        note?: string | null;
    }>(),
    { tone: "neutral" },
);
</script>

<template>
    <div class="trig-row">
        <Chip :label="label" :tone="tone" />
        <input
            v-if="value"
            class="input mono"
            :value="value"
            style="width: 60px; padding: 3px 6px; font-size: 12px"
        />
        <span v-if="unit" class="muted" style="font-size: 12px">{{
            unit
        }}</span>
        <span v-if="note" class="muted" style="font-size: 12px">{{
            note
        }}</span>
        <Button
            variant="ghost"
            size="sm"
            icon="trash"
            action="remove-trigger"
        />
    </div>
</template>
