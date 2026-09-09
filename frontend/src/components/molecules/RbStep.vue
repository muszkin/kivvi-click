<script setup lang="ts">
// MOLECULE · RbStep — one KIEDY/JEŚLI/WTEDY section of the rule pipeline, ported from
// rule-pipeline.html.twig's per-step markup (`<section class="rb-step">`, inlined there rather
// than its own partial — factored out here only because Vue needs a v-for body). The dashed
// "add another" affordance is add-slot.html.twig's own single-use markup: not worth a dedicated
// component for one button shape used only here.
import Icon from "@/components/atoms/Icon.vue";
import RbBlock from "@/components/molecules/RbBlock.vue";

export interface RbStepBlock {
    icon: string;
    title: string;
    body: string;
}

defineProps<{
    n: number;
    kicker: string;
    title: string;
    addLabel: string;
    blocks: RbStepBlock[];
}>();
</script>

<template>
    <section class="rb-step">
        <div class="rb-step__kicker">
            <span class="num">{{ n }}</span> {{ kicker }}
        </div>
        <h3 class="rb-step__title">{{ title }}</h3>
        <RbBlock
            v-for="(block, index) in blocks"
            :key="index"
            :icon="block.icon"
            :title="block.title"
            :body="block.body"
        />
        <button class="rb-add" data-action="add-block" :data-payload="n">
            <Icon name="plus" /> {{ addLabel }}
        </button>
    </section>
</template>
