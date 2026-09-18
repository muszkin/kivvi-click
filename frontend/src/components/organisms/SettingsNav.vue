<script setup lang="ts">
import { useI18n } from "vue-i18n";
// ORGANISM · SettingsNav — 220px settings sidebar, ported from
// components/organisms/settings-nav.html.twig. Every link is a real navigation (never
// router.push): the oracle records a full GET document request for each tab switch.
import Icon from "@/components/atoms/Icon.vue";

export interface SettingsNavTab {
    id: string;
    icon: string;
    label: string;
    href: string;
    active: boolean;
}

defineProps<{ tabs: SettingsNavTab[] }>();

const { t } = useI18n();
</script>

<template>
    <nav class="settings-nav" :aria-label="t('common.settingsSections')">
        <a
            v-for="tab in tabs"
            :key="tab.id"
            :href="tab.href"
            :aria-current="tab.active ? 'true' : 'false'"
            ><Icon :name="tab.icon" />{{ " " }}<span>{{ tab.label }}</span></a
        >
    </nav>
</template>
