<script setup lang="ts">
// PAGE · Reguły i automatyzacje — ported from pages/automations.html.twig.
// Route: GET /{locale}/automations
// Data arrives from GET /api/v1/{locale}/automations?status=, pre-formatted by
// AutomationsViewService — this view never formats a number itself. The status query parameter
// only marks a filter chip active server-side (see AutomationsViewService's own doc): it never
// filters the card list, and this view forwards it unchanged, per common-journey-rules.md's
// query-string-state rule.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import FilterChip from "@/components/molecules/FilterChip.vue";
import AutoCard, {
    type AutoCardChip,
    type AutoCardMetric,
} from "@/components/organisms/AutoCard.vue";
import PageHead from "@/components/organisms/PageHead.vue";

interface Filter {
    label: string;
    icon: string | null;
    count: string;
    active: boolean;
    action: string;
    payload: string;
}

interface AutomationCard {
    title: string;
    chips: AutoCardChip[];
    metrics: AutoCardMetric[];
    action: string;
    payload: string;
}

interface AutomationsPayload {
    filters: Filter[];
    automations: AutomationCard[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const newAutomationHref = computed(() => `/${locale.value}/automations/new`);

const payload = ref<AutomationsPayload | null>(null);

function queryParam(name: string): string | undefined {
    const value = route.query[name];
    return typeof value === "string" ? value : undefined;
}

async function load(): Promise<void> {
    const status = queryParam("status");
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    const response = await fetch(
        `/api/v1/${locale.value}/automations${query}`,
        {
            headers: { Accept: "application/json" },
        },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as AutomationsPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('automations.title')" :sub="t('automations.sub')">
            <template #actions>
                <Button
                    size="sm"
                    icon="book"
                    :label="t('automations.templates')"
                    action="open-templates"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('automations.newAutomation')"
                    :href="newAutomationHref"
                />
            </template>
        </PageHead>

        <div class="row" style="margin-bottom: 18px; gap: 8px">
            <FilterChip
                v-for="(filter, index) in payload.filters"
                :key="index"
                :label="filter.label"
                :icon="filter.icon"
                :count="filter.count"
                :active="filter.active"
                :action="filter.action"
                :payload="filter.payload"
            />
            <div style="flex: 1"></div>
            <Button
                size="sm"
                icon="filter"
                :label="t('automations.filters')"
                action="open-filters"
            />
        </div>

        <div class="col" style="gap: 12px">
            <AutoCard
                v-for="(automation, index) in payload.automations"
                :key="index"
                :title="automation.title"
                :chips="automation.chips"
                :metrics="automation.metrics"
                :action="automation.action"
                :payload="automation.payload"
            />
        </div>
    </div>
</template>
