<script setup lang="ts">
// MOLECULE · CouponCode — ported from components/molecules/coupon-code.html.twig. Dashed brown
// ticket showing a discount code; appears in the e-mail document (and, from wave-4, the popup
// widget editor). Deliberately no theme-variable colours in its CSS class — see EmailDocument.vue.
withDefaults(defineProps<{ code: string; note?: string | null }>(), {
    note: null,
});
</script>

<template>
    <div class="coupon-code">{{ code }}</div>
    <div v-if="note" style="font-size: 12px; color: oklch(0.5 0.025 150)">
        {{ note }}
    </div>
</template>
