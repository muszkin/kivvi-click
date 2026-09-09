<script setup lang="ts">
// PAGE · Import klientów — ported from pages/import.html.twig. Four-step configurable CSV/XML
// customer import.
//
// Data arrives from GET /api/v1/{locale}/import/{step}, pre-formatted by ImportViewService —
// this view (and every child component under components/import/) never formats a number itself.
// Step-to-step navigation (the stepper, the back/forward buttons, and — since repair-1 — the
// upload's own redirect too) is always a real document request, never an SPA route change: no
// code in this journey ever calls router.push, so this view only ever needs to fetch once, on
// mount — unlike EmailEditorView.vue's own `watch(id, load)`, which reacts to a same-route
// param change that genuinely happens there.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import PageHead from "@/components/organisms/PageHead.vue";
import { type KpiTileData } from "@/components/organisms/KpiGrid.vue";
import Stepper, { type StepperStep } from "@/components/molecules/Stepper.vue";
import StepMapping, {
    type MappingColumn,
    type MappingDetection,
} from "@/components/import/StepMapping.vue";
import StepRules from "@/components/import/StepRules.vue";
import StepRun, { type RunPreviewRow } from "@/components/import/StepRun.vue";
import StepUpload from "@/components/import/StepUpload.vue";
import RecentImports, {
    type RecentImportItem,
} from "@/components/import/RecentImports.vue";
import type { DedupStrategy } from "@/components/import/Dedup.vue";
import type { ValidationCheck } from "@/components/import/ValidationList.vue";

interface ImportPayload {
    step: number;
    steps: StepperStep[];
    file: { name: string; meta: string };
    columns: MappingColumn[];
    detection: MappingDetection;
    validations: ValidationCheck[];
    dedupStrategies: DedupStrategy[];
    summary: KpiTileData[];
    preview: RunPreviewRow[];
    rowCount: number;
    rowCountLabel: string;
    errorCount: number;
    recent: RecentImportItem[];
}

const DEFAULT_STEP = "1";

const { t } = useI18n();
const route = useRoute();

const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const step = computed(() =>
    typeof route.params.step === "string" ? route.params.step : DEFAULT_STEP,
);

const payload = ref<ImportPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/import/${step.value}`,
        {
            headers: { Accept: "application/json" },
        },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as ImportPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('import.title')" :sub="t('import.sub')">
            <template #actions>
                <Button
                    size="sm"
                    icon="list"
                    :label="t('import.history')"
                    action="import-history"
                />
                <Button
                    size="sm"
                    icon="book"
                    :label="t('import.howItWorks')"
                    action="open-docs"
                />
            </template>
        </PageHead>

        <Stepper
            :steps="payload.steps"
            :current="payload.step"
            :locale="locale"
        />

        <div v-if="payload.step === 1" class="wiz-layout">
            <div><StepUpload /></div>
            <div><RecentImports :imports="payload.recent" /></div>
        </div>
        <StepMapping
            v-else-if="payload.step === 2"
            :locale="locale"
            :file="payload.file"
            :columns="payload.columns"
            :detection="payload.detection"
            :validations="payload.validations"
        />
        <StepRules
            v-else-if="payload.step === 3"
            :locale="locale"
            :strategies="payload.dedupStrategies"
        />
        <StepRun
            v-else
            :locale="locale"
            :summary="payload.summary"
            :preview="payload.preview"
            :row-count="payload.rowCount"
            :row-count-label="payload.rowCountLabel"
            :error-count="payload.errorCount"
        />
    </div>
</template>
