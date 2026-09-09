<script setup lang="ts">
// PARTIAL · import step 4 · Podgląd i start — ported from pages/import/step-run.html.twig.
// Summary tiles, the first rows after mapping, final options and the CTA that counts rows.
//
// Preview table's "scroll" wrapper (table.html.twig's own `scroll: true`, an `overflow-x: auto`
// div around the <table>) has no equivalent on the shared Table.vue molecule (its own class
// comment: "no page in this journey needs it, so it is left for whichever later journey does" —
// see worker-report.md). Reproduced by wrapping <Table> in that same div locally rather than
// editing Table.vue.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";
import FinalOptions from "@/components/import/FinalOptions.vue";
import { importStepPath } from "@/components/import/importRoute";

export interface RunPreviewRow {
    status: string;
    statusTone:
        | "neutral"
        | "good"
        | "warn"
        | "bad"
        | "info"
        | "accent"
        | "brown";
    statusLabel: string;
    email: string;
    name: string;
    optIn: string;
    orders: string;
    ltv: string;
    segments: string[];
    error: string;
}

const props = defineProps<{
    locale: string;
    summary: KpiTileData[];
    preview: RunPreviewRow[];
    rowCount: number;
    rowCountLabel: string;
    errorCount: number;
}>();

const { t } = useI18n();

const backHref = computed(() => importStepPath(props.locale, 3));
const editMappingHref = computed(() => importStepPath(props.locale, 2));

const tableColumns = computed<TableColumn[]>(() => [
    { label: t("import.run.colStatus") },
    { label: t("import.run.colEmail") },
    { label: t("import.run.colName") },
    { label: t("import.run.colOptIn") },
    { label: t("import.run.colOrders"), align: "right" },
    { label: t("import.run.colLtv"), align: "right" },
    { label: t("import.run.colSegments") },
    { label: "" },
]);
</script>

<template>
    <div class="wiz-card">
        <div
            class="row"
            style="
                justify-content: space-between;
                align-items: flex-start;
                gap: 16px;
                margin-bottom: 6px;
            "
        >
            <div>
                <h2 class="wiz-title">{{ t("import.run.title") }}</h2>
                <p class="wiz-sub">{{ t("import.run.sub") }}</p>
            </div>
            <Button
                size="sm"
                icon="download"
                :label="t('import.run.downloadReport')"
                action="export"
            />
        </div>

        <div style="margin: 14px 0">
            <KpiGrid :tiles="summary" />
        </div>

        <Card
            :title="t('import.run.previewTitle', { count: preview.length })"
            icon="eye"
            :sub="t('import.run.previewSub')"
        >
            <template #headActions>
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('import.run.all')"
                    action="filter-preview"
                    payload="all"
                    data-active="true"
                />
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('import.run.onlyErrors', { count: errorCount })"
                    action="filter-preview"
                    payload="errors"
                />
            </template>
            <template #raw>
                <div style="overflow-x: auto">
                    <Table :columns="tableColumns" :rows="preview">
                        <template #row="{ row }">
                            <td>
                                <Chip
                                    :label="row.statusLabel"
                                    :tone="row.statusTone"
                                />
                            </td>
                            <td>
                                <span class="mono" style="font-size: 12px">{{
                                    row.email
                                }}</span>
                            </td>
                            <td>{{ row.name }}</td>
                            <td>{{ row.optIn }}</td>
                            <td>
                                <span class="mono">{{ row.orders }}</span>
                            </td>
                            <td>
                                <span class="mono">{{ row.ltv }}</span>
                            </td>
                            <td>
                                <template
                                    v-for="segment in row.segments"
                                    :key="segment"
                                    ><Chip :label="segment" />{{
                                        " "
                                    }}</template
                                >
                            </td>
                            <td>
                                <span
                                    v-if="row.error"
                                    style="color: var(--bad); font-size: 12px"
                                    >{{ row.error }}</span
                                >
                            </td>
                        </template>
                    </Table>
                </div>
            </template>
        </Card>

        <div style="margin-top: 16px">
            <Card>
                <FinalOptions />
            </Card>
        </div>

        <div class="wiz-actions">
            <Button :label="t('import.mapping.back')" :href="backHref" />
            <div style="flex: 1"></div>
            <Button
                icon="edit"
                :label="t('import.run.editMapping')"
                :href="editMappingHref"
            />
            <Button
                variant="primary"
                size="lg"
                icon="upload"
                :label="t('import.run.runImport', { count: rowCountLabel })"
                action="run-import"
            />
        </div>
    </div>
</template>
