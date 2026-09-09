<script setup lang="ts">
// MOLECULE · ProfileFact — label/value row inside the customer profile card, ported from
// components/molecules/profile-fact.html.twig.
withDefaults(
    defineProps<{
        label: string;
        value: string;
        small?: boolean;
    }>(),
    { small: false },
);
</script>

<template>
    <div class="profile-fact">
        <span class="lbl">{{ label }}</span>
        <span class="val" :style="small ? { fontSize: '11px' } : undefined">{{
            value
        }}</span>
    </div>
</template>
