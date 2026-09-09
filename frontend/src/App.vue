<script setup lang="ts">
import { useIntents } from "@/composables/useIntents";

useIntents();
</script>

<template>
    <router-view v-slot="{ Component, route }">
        <component :is="route.meta.layout">
            <component :is="Component" />
        </component>
    </router-view>
</template>
