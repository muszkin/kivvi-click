<script setup lang="ts">
// PARTIAL · import step 3 · deduplication — ported from pages/import/dedup.html.twig.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Icon from "@/components/atoms/Icon.vue";
import Segmented from "@/components/molecules/Segmented.vue";

export interface DedupStrategy {
    value: string;
    label: string;
    checked: boolean;
}

defineProps<{ strategies: DedupStrategy[] }>();

const { t } = useI18n();
</script>

<template>
    <div style="display: flex; flex-direction: column; gap: 12px">
        <div
            class="muted"
            style="
                font-size: 11.5px;
                text-transform: uppercase;
                letter-spacing: 0.06em;
            "
        >
            {{ t("import.rules.matchKey") }}
        </div>
        <Segmented
            action="set-dedup-key"
            :options="[
                {
                    value: 'email',
                    label: t('import.rules.matchEmail'),
                    active: true,
                },
                {
                    value: 'email_phone',
                    label: t('import.rules.matchEmailPhone'),
                },
                { value: 'customer_id', label: 'customer_id' },
            ]"
        />

        <div
            class="muted"
            style="
                font-size: 11.5px;
                text-transform: uppercase;
                letter-spacing: 0.06em;
                margin-top: 6px;
            "
        >
            {{ t("import.rules.whatIfExists") }}
        </div>
        <div class="col" style="gap: 6px">
            <ToggleRow
                v-for="strategy in strategies"
                :key="strategy.value"
                kind="radio"
                name="dedup"
                :value="strategy.value"
                :label="strategy.label"
                :checked="strategy.checked"
            />
        </div>

        <!--
          Callout.vue (shared molecule) has no right-aligned `action` slot yet — only `text`,
          `icon`, `tone` (callout.html.twig's own `action` param has no Vue counterpart). It is
          outside this journey's touch scope, so this reproduces callout.html.twig's markup
          directly rather than editing it; worker-report.md flags the gap.
        -->
        <div class="callout">
            <Icon name="info" />
            <span v-html="t('import.rules.callout')"></span>
            <span style="margin-left: auto">
                <Button
                    variant="ghost"
                    size="sm"
                    icon="edit"
                    action="edit-consent-rule"
                />
            </span>
        </div>
    </div>
</template>
