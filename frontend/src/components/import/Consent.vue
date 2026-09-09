<script setup lang="ts">
// PARTIAL · import step 3 · GDPR consent — ported from pages/import/consent.html.twig. Consent
// source, consent date and the default opt-in decision. Takes no props, exactly like the old
// partial: every value here is static markup, not wizard-payload data.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Segmented from "@/components/molecules/Segmented.vue";
import SelectField from "@/components/import/SelectField.vue";

const { t } = useI18n();

const sourceOptions = computed(() => [
    { value: "form", label: t("import.rules.consentSourceForm") },
    {
        value: "migration",
        label: t("import.rules.consentSourceMigration"),
        selected: true,
    },
    { value: "purchase", label: t("import.rules.consentSourcePurchase") },
    { value: "manual", label: t("import.rules.consentSourceManual") },
]);

const dateOptions = computed(() => [
    {
        value: "column",
        label: t("import.rules.consentDateColumn"),
        selected: true,
    },
    { value: "today", label: t("import.rules.consentDateToday") },
    { value: "manual", label: t("import.rules.consentDateManual") },
]);
</script>

<template>
    <div
        style="
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 18px;
        "
    >
        <SelectField
            name="consent_source"
            :label="t('import.rules.consentSource')"
            :options="sourceOptions"
        />
        <SelectField
            name="consent_date"
            :label="t('import.rules.consentDate')"
            :options="dateOptions"
        />

        <div>
            <label class="label">{{ t("import.rules.defaultOptIn") }}</label>
            <Segmented
                action="set-optin"
                :options="[
                    {
                        value: 'file',
                        label: t('import.rules.optInFile'),
                        active: true,
                    },
                    { value: 'yes', label: t('import.rules.optInYes') },
                    { value: 'no', label: t('import.rules.optInNo') },
                ]"
            />
        </div>

        <div style="grid-column: span 3">
            <ToggleRow
                name="gdpr_confirmed"
                checked
                :label="t('import.rules.gdprConfirm')"
            />
        </div>
    </div>
</template>
