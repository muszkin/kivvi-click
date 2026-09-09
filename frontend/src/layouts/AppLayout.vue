<script setup lang="ts">
import { onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import AppShell from "@/components/organisms/AppShell.vue";
import { useShellStore } from "@/stores/shell";

const route = useRoute();
const shell = useShellStore();

async function loadForCurrentRoute(): Promise<void> {
    const name = typeof route.name === "string" ? route.name : "";
    await shell.load(name);
}

onMounted(loadForCurrentRoute);
watch(() => route.name, loadForCurrentRoute);
</script>

<template>
    <AppShell>
        <slot />
    </AppShell>
</template>
