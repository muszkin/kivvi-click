<script setup lang="ts">
// MOLECULE · FilterChip — ported from components/molecules/filter-chip.html.twig. Interactive pill
// for list filters and segment rails; active state uses accent-soft + accent border (CSS only).
//
// `count`, if used, must arrive already formatted (the SPA never formats a number itself — see
// eslint.config.js's Intl/toLocaleString ban and domain/Format.java) — this journey's own filter
// chips never pass one, but the molecule keeps the param for parity with filter-chip.html.twig.
import Dot from "@/components/atoms/Dot.vue";
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        label: string;
        icon?: string | null;
        dotColor?: string | null;
        count?: string | null;
        active?: boolean;
        action?: string | null;
        payload?: string | null;
    }>(),
    {
        icon: null,
        dotColor: null,
        count: null,
        active: false,
        action: null,
        payload: "",
    },
);
</script>

<template>
    <button
        class="filter-chip"
        :data-active="active ? 'true' : 'false'"
        :data-action="action ?? undefined"
        :data-payload="action ? (payload ?? '') : undefined"
    >
        <Icon v-if="icon" :name="icon" />
        <Dot v-if="dotColor" :color="dotColor" />
        {{ label }}<template v-if="count !== null"> · {{ count }}</template>
    </button>
</template>
