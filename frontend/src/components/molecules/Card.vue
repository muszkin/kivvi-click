<script setup lang="ts">
// MOLECULE · Card — standard surface container, ported from components/molecules/card.html.twig.
// Head/body/foot are all optional. `class` needs no explicit prop: a caller's `class="..."`
// attribute falls through and merges onto the root `.card` div automatically, same as Twig's
// `class` param.
import Dot from "@/components/atoms/Dot.vue";
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        title?: string;
        sub?: string;
        icon?: string;
        live?: boolean;
    }>(),
    {
        live: false,
    },
);
</script>

<template>
    <div class="card">
        <div v-if="title" class="card-head">
            <Dot v-if="live" live />
            <h3 class="card-title">
                <Icon v-if="icon" :name="icon" />
                {{ title }}
            </h3>
            <span v-if="sub" class="card-sub">{{ sub }}</span>
            <div
                v-if="$slots.headActions"
                style="margin-left: auto; display: flex; gap: 6px"
            >
                <slot name="headActions" />
            </div>
        </div>
        <div v-if="$slots.default" class="card-body"><slot /></div>
        <slot name="raw" />
        <div v-if="$slots.foot" class="card-foot"><slot name="foot" /></div>
    </div>
</template>
