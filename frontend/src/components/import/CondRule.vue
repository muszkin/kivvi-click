<script setup lang="ts">
// MOLECULE · CondRule — editable condition line "JEŚLI <field> <op> <value> → <target>", ported
// from components/molecules/cond-rule.html.twig. Journey-scoped for now (see worker-report.md):
// automations (its originally intended first owner) shipped without needing it, so import-wizard
// produces it here rather than in the shared molecules/ tree; promote it there if a later journey
// needs the same component.
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";

withDefaults(
    defineProps<{
        field: string;
        operators: string[];
        value: string;
        suffix?: string;
        targetLabel?: string;
        targetTone?:
            | "neutral"
            | "good"
            | "warn"
            | "bad"
            | "info"
            | "accent"
            | "brown";
    }>(),
    { targetTone: "accent" },
);
</script>

<template>
    <div class="cond-rule">
        <span class="muted">JEŚLI</span>
        <Chip :label="field" mono />
        <select class="select" style="width: auto">
            <option v-for="operator in operators" :key="operator">
                {{ operator }}
            </option>
        </select>
        <input class="input mono" :value="value" style="width: 80px" />
        <span v-if="suffix" class="muted">{{ suffix }}</span>
        <Chip v-if="targetLabel" :label="targetLabel" :tone="targetTone" />
        <Button variant="ghost" size="sm" icon="trash" action="remove-rule" />
    </div>
</template>
