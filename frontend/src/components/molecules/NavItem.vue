<script setup lang="ts">
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        label: string;
        icon: string;
        href: string;
        route: string;
        active?: boolean;
        badge?: string;
    }>(),
    { active: false },
);
</script>

<template>
    <a
        class="nav-item"
        :href="href"
        :data-route="route"
        :data-active="active ? 'true' : 'false'"
        :aria-current="active ? 'page' : undefined"
        :title="label"
    >
        <Icon :name="icon" class="nav-icon" />
        <span class="nav-label">{{ label }}</span>
        <span v-if="badge" class="nav-badge">{{ badge }}</span>
    </a>
</template>
