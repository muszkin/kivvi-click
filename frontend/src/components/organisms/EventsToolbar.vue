<script setup lang="ts">
// ORGANISM · EventsToolbar — the two filter rows atop pages/events.html.twig (type chips; site
// chips + range segmented), extracted from the page so EventsView.vue stays a plain composition
// of API payload -> markup. Inline styles are copied verbatim from the Twig source (CSS is
// byte-identical and never edited — see common-journey-rules.md).
import { useI18n } from "vue-i18n";
import FilterChip from "@/components/molecules/FilterChip.vue";
import Segmented from "@/components/molecules/Segmented.vue";

export interface ToolbarFilter {
    label: string;
    icon?: string | null;
    dotColor?: string | null;
    active: boolean;
    action: string;
    payload: string;
}

export interface ToolbarRange {
    value: string;
    label: string;
    active: boolean;
}

defineProps<{
    typeFilters: ToolbarFilter[];
    siteFilters: ToolbarFilter[];
    ranges: ToolbarRange[];
}>();

const { t } = useI18n();
</script>

<template>
    <div class="events-toolbar">
        <span
            class="muted"
            style="
                font-size: 11.5px;
                text-transform: uppercase;
                letter-spacing: 0.06em;
            "
            >{{ t("events.typeLabel") }}:</span
        >
        <FilterChip
            v-for="filter in typeFilters"
            :key="filter.action + ':' + filter.payload"
            :label="filter.label"
            :icon="filter.icon"
            :active="filter.active"
            :action="filter.action"
            :payload="filter.payload"
        />
    </div>
    <div class="events-toolbar" style="margin-bottom: 18px">
        <span
            class="muted"
            style="
                font-size: 11.5px;
                text-transform: uppercase;
                letter-spacing: 0.06em;
            "
            >{{ t("events.siteLabel") }}:</span
        >
        <FilterChip
            v-for="filter in siteFilters"
            :key="filter.action + ':' + filter.payload"
            :label="filter.label"
            :icon="filter.icon"
            :dot-color="filter.dotColor"
            :active="filter.active"
            :action="filter.action"
            :payload="filter.payload"
        />
        <div style="flex: 1"></div>
        <span class="muted" style="font-size: 12px"
            >{{ t("events.periodLabel") }}:</span
        >
        <Segmented action="set-range" :options="ranges" />
    </div>
</template>
