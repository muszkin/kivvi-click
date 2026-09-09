<script setup lang="ts">
import Icon from "@/components/atoms/Icon.vue";

defineProps<{ name: string; meta: string; mark: string }>();
</script>

<template>
    <button class="ws-card" data-action="open-workspace-switcher" :title="name">
        <span class="ws-mark">{{ mark }}</span>
        <span class="ws-label">
            <span class="ws-name">{{ name }}</span
            >{{ " " }}<span class="ws-meta">{{ meta }}</span>
        </span>
        <Icon name="chevron_d" class="nav-icon" />
    </button>
</template>
