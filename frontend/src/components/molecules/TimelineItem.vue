<script setup lang="ts">
// MOLECULE · TimelineItem — one node on the customer activity axis, ported from
// components/molecules/timeline-item.html.twig. The 12px ring is drawn by ::before against the
// 1px rail on .timeline (see 04-patterns.css) — this component only renders one .tl-item.
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        time: string;
        title: string;
        detail?: string;
        icon?: string;
    }>(),
    { icon: "eye" },
);
</script>

<template>
    <div class="tl-item">
        <div class="tl-time">{{ time }}</div>
        <div class="tl-title"><Icon :name="icon" />{{ " " }}{{ title }}</div>
        <div v-if="detail" class="tl-detail">{{ detail }}</div>
    </div>
</template>
