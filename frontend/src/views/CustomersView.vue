<script setup lang="ts">
// PAGE · Klienci — ported from pages/customers.html.twig.
//
// Data arrives from GET /api/v1/{locale}/customers?page=, pre-formatted by
// CustomersViewService — this view never formats a number or a relative time itself.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Avatar from "@/components/atoms/Avatar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import Pagination from "@/components/molecules/Pagination.vue";
import SegmentStrip, {
    type SegmentTile,
} from "@/components/molecules/SegmentStrip.vue";
import Table, { type TableRowAction } from "@/components/molecules/Table.vue";
import PageHead from "@/components/organisms/PageHead.vue";

interface CustomerRow {
    id: string;
    name: string;
    initials: string;
    email: string;
    segment: { label: string; tone: "accent" | "good" | "brown" | "warn" };
    orders: string;
    revenue: string;
    lastSeen: string;
}

interface CustomersPayload {
    subtitle: string;
    segments: SegmentTile[];
    customersRows: CustomerRow[];
    page: number;
    pages: number;
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const page = computed(() => {
    const requested = Number(route.query.page);
    return Number.isFinite(requested) && requested > 0 ? requested : 1;
});

const payload = ref<CustomersPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/customers?page=${page.value}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as CustomersPayload;
}

onMounted(load);

const tableColumns = computed(() => [
    { label: t("customers.columns.customer") },
    { label: t("customers.columns.email") },
    { label: t("customers.columns.segment") },
    { label: t("customers.columns.orders"), align: "right" as const },
    { label: t("customers.columns.lifetimeValue"), align: "right" as const },
    { label: t("customers.columns.lastActivity") },
    { label: t("customers.columns.actions"), align: "right" as const },
]);

const rowActions = computed<TableRowAction[]>(
    () =>
        payload.value?.customersRows.map((row) => ({
            action: "go-customer",
            payload: row.id,
        })) ?? [],
);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('customers.title')" :sub="payload.subtitle">
            <template #actions>
                <Button
                    size="sm"
                    icon="download"
                    :label="t('customers.exportCsv')"
                    action="export"
                />
                <Button
                    size="sm"
                    icon="bookmark"
                    :label="t('customers.segments')"
                    action="open-segments"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('customers.newSegment')"
                    action="new-segment"
                />
            </template>
        </PageHead>

        <div class="row" style="margin-bottom: 18px; gap: 8px; flex-wrap: wrap">
            <SegmentStrip :segments="payload.segments" />
            <div style="flex: 1"></div>
            <Button
                size="sm"
                icon="filter"
                :label="t('customers.filters')"
                action="open-filters"
            />
        </div>

        <Card>
            <template #raw>
                <Table
                    :columns="tableColumns"
                    :rows="payload.customersRows"
                    :row-actions="rowActions"
                >
                    <template #row="{ row }">
                        <td>
                            <div class="row">
                                <Avatar :name="row.name" :size="28" />
                                <span style="font-weight: 500">{{
                                    row.name
                                }}</span>
                            </div>
                        </td>
                        <td>
                            <span
                                class="mono"
                                style="font-size: 12px; color: var(--fg-muted)"
                                >{{ row.email }}</span
                            >
                        </td>
                        <td>
                            <Chip
                                :label="row.segment.label"
                                :tone="row.segment.tone"
                            />
                        </td>
                        <td>
                            <span class="mono">{{ row.orders }}</span>
                        </td>
                        <td>
                            <span class="mono">{{ row.revenue }}</span>
                        </td>
                        <td>
                            <span class="muted" style="font-size: 12px">{{
                                row.lastSeen
                            }}</span>
                        </td>
                        <td><Icon name="chevron_r" /></td>
                    </template>
                </Table>
            </template>
        </Card>

        <Pagination :page="payload.page" :pages="payload.pages" />
    </div>
</template>
