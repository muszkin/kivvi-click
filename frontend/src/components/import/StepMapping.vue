<script setup lang="ts">
// PARTIAL · import step 2 · Mapowanie kolumn — ported from pages/import/step-mapping.html.twig.
// Auto-detection summary, one MapRow per file column, and what validation will run.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import FilePill from "@/components/molecules/FilePill.vue";
import MapRow, { type MapRowTarget } from "@/components/molecules/MapRow.vue";
import ValidationList, {
    type ValidationCheck,
} from "@/components/import/ValidationList.vue";
import { importStepPath } from "@/components/import/importRoute";

export interface MappingColumn {
    letter: string;
    name: string;
    samples: string[];
    targets: MapRowTarget[];
    mapped: string;
    confidence: number;
    skipped: boolean;
}

export interface MappingDetection {
    recognised: number;
    total: number;
    sure: number;
    unsure: number;
    skipped: number;
}

const props = defineProps<{
    locale: string;
    file: { name: string; meta: string };
    columns: MappingColumn[];
    detection: MappingDetection;
    validations: ValidationCheck[];
}>();

const { t } = useI18n();
const changeHref = computed(() => importStepPath(props.locale, 1));
const backHref = computed(() => importStepPath(props.locale, 1));
const nextHref = computed(() => importStepPath(props.locale, 3));
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
                <h2 class="wiz-title">{{ t("import.mapping.title") }}</h2>
                <p class="wiz-sub">{{ t("import.mapping.sub") }}</p>
            </div>
            <FilePill
                :name="file.name"
                :meta="file.meta"
                :change-label="t('import.mapping.change')"
                :change-href="changeHref"
            />
        </div>

        <div class="auto-detect">
            <div class="row" style="gap: 8px">
                <Icon name="spark" />
                <span>
                    <strong>{{
                        t("import.mapping.autoDetected", {
                            recognised: detection.recognised,
                            total: detection.total,
                        })
                    }}</strong>
                    {{ t("import.mapping.checkColors") }}
                </span>
                <div style="margin-left: auto; display: flex; gap: 6px">
                    <Chip
                        :label="
                            t('import.mapping.sureChip', {
                                count: detection.sure,
                            })
                        "
                        tone="good"
                    />
                    <Chip
                        :label="
                            t('import.mapping.unsureChip', {
                                count: detection.unsure,
                            })
                        "
                        tone="warn"
                    />
                    <Chip
                        :label="
                            t('import.mapping.skippedChip', {
                                count: detection.skipped,
                            })
                        "
                    />
                </div>
            </div>
        </div>

        <div class="map-table">
            <div class="map-row map-row--head">
                <div>{{ t("import.mapping.colFile") }}</div>
                <div>{{ t("import.mapping.colSample") }}</div>
                <div>{{ t("import.mapping.colTarget") }}</div>
                <div class="right">{{ t("import.mapping.colConfidence") }}</div>
            </div>
            <MapRow
                v-for="column in columns"
                :key="column.letter"
                :letter="column.letter"
                :name="column.name"
                :samples="column.samples"
                :targets="column.targets"
                :mapped="column.mapped"
                :confidence="column.confidence"
                :skipped="column.skipped"
            />
        </div>

        <Card
            class="wiz-validation"
            :title="t('import.mapping.validationTitle')"
            icon="check"
            :sub="t('import.mapping.validationSub')"
        >
            <ValidationList :validations="validations" />
        </Card>

        <div class="wiz-actions">
            <Button :label="t('import.mapping.back')" :href="backHref" />
            <div style="flex: 1"></div>
            <Button
                variant="primary"
                :label="t('import.mapping.next')"
                :href="nextHref"
            />
        </div>
    </div>
</template>
