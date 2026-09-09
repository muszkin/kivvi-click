<script setup lang="ts">
// MOLECULE · Tabs — underline tab strip used inside detail pages, ported from
// components/molecules/tabs.html.twig.
export interface TabItem {
    label: string;
    active?: boolean;
    action?: string;
    payload?: string;
}

defineProps<{ tabs: TabItem[] }>();
</script>

<template>
    <div class="tab-strip" role="tablist">
        <button
            v-for="(tab, index) in tabs"
            :key="index"
            class="tab"
            role="tab"
            :aria-selected="tab.active ? 'true' : 'false'"
            :data-active="tab.active ? 'true' : 'false'"
            :data-action="tab.action"
            :data-payload="tab.action ? (tab.payload ?? '') : undefined"
        >
            {{ tab.label }}
        </button>
    </div>
</template>
