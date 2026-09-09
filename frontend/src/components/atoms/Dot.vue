<script setup lang="ts">
// ATOM · Dot — 6px status dot, ported from components/atoms/dot.html.twig. `live` wraps it so
// the CSS ::after pulse ring (1.6s ease-in-out infinite) has a positioning context.
withDefaults(defineProps<{ color?: string; live?: boolean }>(), {
    live: false,
});
</script>

<template>
    <span v-if="live" class="dot-wrap"
        ><span
            class="dot live"
            :style="color ? { background: color } : undefined"
        ></span
    ></span>
    <span
        v-else
        class="dot"
        :style="color ? { background: color } : undefined"
    ></span>
</template>
