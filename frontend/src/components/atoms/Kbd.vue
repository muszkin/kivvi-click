<script setup lang="ts">
defineProps<{ keys: string }>();
</script>

<template>
    <kbd>{{ keys }}</kbd>
</template>
