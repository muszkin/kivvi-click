<script setup lang="ts">
// PAGE · Kampanie email — ported from pages/campaigns.html.twig.
//
// Data arrives from GET /api/v1/{locale}/campaigns?filter=, pre-formatted by
// CampaignsViewService — this view never formats a number itself. `?filter=` only marks a
// filter-rail chip active server-side (CampaignCatalog::rows() never actually filtered the table
// either — see CampaignsViewService's class comment); the same 5 rows render regardless.
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import FilterChip from "@/components/molecules/FilterChip.vue";
import Table, { type TableRowAction } from "@/components/molecules/Table.vue";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";
import PageHead from "@/components/organisms/PageHead.vue";

interface CampaignFilter {
    label: string;
    count: string;
    active: boolean;
    action: string;
    payload: string;
}

interface CampaignColumn {
    label: string;
    align?: "right";
}

interface CampaignRow {
    id: string;
    name: string;
    statusTone: "good" | "warn" | "neutral" | "info";
    statusLabel: string;
    typeLabel: string;
    typeTone: "accent" | "brown";
    sent: string;
    open: string;
    click: string;
    revenue: string;
}

interface CampaignsPayload {
    kpis: KpiTileData[];
    filters: CampaignFilter[];
    columns: CampaignColumn[];
    rows: CampaignRow[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const filter = computed(() =>
    typeof route.query.filter === "string" ? route.query.filter : "all",
);
const newCampaignHref = computed(() => `/${locale.value}/emails/new`);

const payload = ref<CampaignsPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/campaigns?filter=${encodeURIComponent(filter.value)}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as CampaignsPayload;
}

onMounted(load);
watch(filter, load);

const rowActions = computed<TableRowAction[]>(
    () =>
        payload.value?.rows.map((row) => ({
            action: "go-email",
            payload: row.id,
        })) ?? [],
);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('campaigns.title')" :sub="t('campaigns.sub')">
            <template #actions>
                <Button
                    size="sm"
                    icon="layout"
                    :label="t('campaigns.templates')"
                    action="open-templates"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('campaigns.newCampaign')"
                    :href="newCampaignHref"
                />
            </template>
        </PageHead>

        <KpiGrid :tiles="payload.kpis" />

        <div class="row" style="margin-bottom: 14px; gap: 8px">
            <FilterChip
                v-for="(chip, index) in payload.filters"
                :key="index"
                :label="chip.label"
                :count="chip.count"
                :active="chip.active"
                :action="chip.action"
                :payload="chip.payload"
            />
            <div style="flex: 1"></div>
            <Button
                size="sm"
                icon="filter"
                :label="t('campaigns.filters')"
                action="open-filters"
            />
        </div>

        <Card>
            <template #raw>
                <Table
                    :columns="payload.columns"
                    :rows="payload.rows"
                    :row-actions="rowActions"
                >
                    <template #row="{ row }">
                        <td>
                            <span style="font-weight: 500">{{ row.name }}</span>
                        </td>
                        <td>
                            <Chip :label="row.typeLabel" :tone="row.typeTone" />
                        </td>
                        <td>
                            <Chip
                                :label="row.statusLabel"
                                :tone="row.statusTone"
                            />
                        </td>
                        <td>
                            <span class="mono">{{ row.sent }}</span>
                        </td>
                        <td>
                            <span class="mono">{{ row.open }}</span>
                        </td>
                        <td>
                            <span class="mono">{{ row.click }}</span>
                        </td>
                        <td>
                            <span class="mono" style="font-weight: 500">{{
                                row.revenue
                            }}</span>
                        </td>
                        <td><Icon name="chevron_r" /></td>
                    </template>
                </Table>
            </template>
        </Card>
    </div>
</template>
