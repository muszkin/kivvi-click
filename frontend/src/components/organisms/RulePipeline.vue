<script setup lang="ts">
// ORGANISM · RulePipeline — ported from components/organisms/rule-pipeline.html.twig. Linear rule
// builder: three equal steps (KIEDY → JEŚLI → WTEDY) joined by CSS arrows above 1100px, stacked
// below (CSS only, already byte-identical — nothing here decides that layout).
import RbStep, { type RbStepBlock } from "@/components/molecules/RbStep.vue";

export interface PipelineStep {
    n: number;
    kicker: string;
    title: string;
    addLabel: string;
    blocks: RbStepBlock[];
}

defineProps<{ steps: PipelineStep[] }>();
</script>

<template>
    <div class="rb-pipeline">
        <RbStep
            v-for="step in steps"
            :key="step.n"
            :n="step.n"
            :kicker="step.kicker"
            :title="step.title"
            :add-label="step.addLabel"
            :blocks="step.blocks"
        />
    </div>
</template>
