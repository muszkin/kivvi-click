<script setup lang="ts">
import Icon from "@/components/atoms/Icon.vue";

defineProps<{ icon: string; title: string; body: string }>();
</script>

<template>
    <div class="feat">
        <div class="feat-icon"><Icon :name="icon" /></div>
        <h3>{{ title }}</h3>
        <p>{{ body }}</p>
    </div>
</template>
