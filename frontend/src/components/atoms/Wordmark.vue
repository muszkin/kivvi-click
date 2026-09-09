<script setup lang="ts">
import Logo from "./Logo.vue";

withDefaults(defineProps<{ size?: number; fontSize?: number }>(), {
    size: 22,
    fontSize: 22,
});
</script>

<template>
    <div class="row" style="gap: 10px">
        <span
            style="
                color: var(--accent);
                display: inline-flex;
                align-items: center;
            "
        >
            <Logo :size="size" />
        </span>
        <span class="brand-name" :style="{ fontSize: `${fontSize}px` }"
            >kivvi<span class="brand-dot"></span>click</span
        >
    </div>
</template>
