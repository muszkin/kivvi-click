<script setup lang="ts">
// PARTIAL · import step 3 · Reguły i segmenty — ported from pages/import/step-rules.html.twig.
// Dedup strategy, segments (fixed and conditional), and the GDPR consent block.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Card from "@/components/molecules/Card.vue";
import Consent from "@/components/import/Consent.vue";
import Dedup, { type DedupStrategy } from "@/components/import/Dedup.vue";
import Segments from "@/components/import/Segments.vue";
import { importStepPath } from "@/components/import/importRoute";

const props = defineProps<{ locale: string; strategies: DedupStrategy[] }>();

const { t } = useI18n();
const backHref = computed(() => importStepPath(props.locale, 2));
const nextHref = computed(() => importStepPath(props.locale, 4));
</script>

<template>
    <div class="wiz-card">
        <h2 class="wiz-title">{{ t("import.rules.title") }}</h2>
        <p class="wiz-sub">{{ t("import.rules.sub") }}</p>

        <div class="rules-grid">
            <Card
                :title="t('import.rules.dedupTitle')"
                icon="users"
                :sub="t('import.rules.dedupSub')"
            >
                <Dedup :strategies="strategies" />
            </Card>

            <Card
                :title="t('import.rules.segmentsTitle')"
                icon="bookmark"
                :sub="t('import.rules.segmentsSub')"
            >
                <Segments />
            </Card>

            <Card
                class="rules-grid__wide"
                :title="t('import.rules.consentTitle')"
                icon="info"
                :sub="t('import.rules.consentSub')"
            >
                <Consent />
            </Card>
        </div>

        <div class="wiz-actions">
            <Button :label="t('import.mapping.back')" :href="backHref" />
            <div style="flex: 1"></div>
            <Button
                variant="primary"
                :label="t('import.rules.next')"
                :href="nextHref"
            />
        </div>
    </div>
</template>
