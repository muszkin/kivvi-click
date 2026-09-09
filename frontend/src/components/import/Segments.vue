<script setup lang="ts">
// PARTIAL · import step 3 · segments — ported from pages/import/segments.html.twig. Fixed
// segments plus conditional rules evaluated per imported row. Takes no props, exactly like the
// old partial: every value here is static markup, not wizard-payload data.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import CondRule from "@/components/import/CondRule.vue";

const { t } = useI18n();
</script>

<template>
    <div style="display: flex; flex-direction: column; gap: 12px">
        <div class="row" style="gap: 6px; flex-wrap: wrap">
            <Chip label="Newsletter" tone="accent" />
            <Chip label="Import — marzec 2026" tone="brown" />
            <Button
                variant="ghost"
                size="sm"
                icon="plus"
                :label="t('import.rules.addSegment')"
                action="add-segment"
            />
        </div>

        <div
            class="muted"
            style="
                font-size: 11.5px;
                text-transform: uppercase;
                letter-spacing: 0.06em;
                margin-top: 6px;
            "
        >
            {{ t("import.rules.conditionalSegments") }}
        </div>
        <CondRule
            field="lifetime_value"
            :operators="['≥', '>', '=']"
            value="500"
            :suffix="t('import.rules.condSuffixVip')"
            target-label="VIP"
            target-tone="accent"
        />
        <CondRule
            field="orders_count"
            :operators="['=']"
            value="0"
            :suffix="t('import.rules.condSuffixReactivation')"
            :target-label="t('import.rules.segmentReactivation')"
            target-tone="brown"
        />

        <!--
          add-slot.html.twig's own single-use markup, inlined rather than a dedicated component —
          same call made independently by the automations journey's RbStep.vue.
        -->
        <button class="rb-add" data-action="add-cond-rule">
            <Icon name="plus" /> {{ t("import.rules.addCondRule") }}
        </button>
    </div>
</template>
