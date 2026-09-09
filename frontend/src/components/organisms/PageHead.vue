<script setup lang="ts">
defineProps<{ title: string; sub?: string }>();
</script>

<template>
    <div class="page-head">
        <div>
            <h1 class="page-title">{{ title }}</h1>
            <p v-if="sub" class="page-sub" v-html="sub"></p>
        </div>
        <div v-if="$slots.actions" class="page-actions">
            <slot name="actions" />
        </div>
    </div>
</template>
