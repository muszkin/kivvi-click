<script setup lang="ts">
// MOLECULE · FlowNode — ported from components/molecules/flow-node.html.twig. Absolutely
// positioned node on the rule flow canvas; border colour encodes the node class (`kind`), CSS only.
import Icon from "@/components/atoms/Icon.vue";

defineProps<{
    x: number;
    y: number;
    kind: "trigger" | "cond" | "action";
    kicker: string;
    title: string;
    sub?: string | null;
    icon?: string | null;
}>();
</script>

<template>
    <div
        class="flow-node"
        :class="kind"
        :style="{ left: `${x}px`, top: `${y}px` }"
        data-flow-node
    >
        <div class="flow-node__kicker">
            <Icon v-if="icon" :name="icon" />
            {{ kicker }}
        </div>
        <div class="flow-node__title">{{ title }}</div>
        <div v-if="sub" class="flow-node__sub">{{ sub }}</div>
    </div>
</template>
