<script setup lang="ts">
// PARTIAL · import step 4 · final options — ported from pages/import/final-options.html.twig.
// What happens after the import finishes, and whether the mapping is saved as a template. Takes
// no props: every value here is static markup or the signed-in identity, not wizard-payload data.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Field from "@/components/atoms/Field.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Segmented from "@/components/molecules/Segmented.vue";
import { useShellStore } from "@/stores/shell";

const { t } = useI18n();
const shell = useShellStore();

const sendReportLabel = computed(
    () =>
        `<strong>${t("import.run.sendReport")}</strong> ${t("import.run.sendReportOn")} <span class="mono">${shell.user.email}</span>`,
);
const saveMappingLabel = computed(
    () =>
        `<strong>${t("import.run.saveMapping")}</strong> ${t("import.run.saveMappingNote")}`,
);
const runWelcomeLabel = computed(
    () =>
        `<strong>${t("import.run.runWelcome")}</strong> ${t("import.run.runWelcomeNote")}`,
);
</script>

<template>
    <div
        style="
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 24px;
        "
    >
        <div>
            <ToggleRow name="send_report" checked :label="sendReportLabel" />
            <ToggleRow name="save_mapping" checked :label="saveMappingLabel" />
            <ToggleRow name="run_welcome" :label="runWelcomeLabel" />
        </div>
        <div>
            <Field
                name="mapping_template"
                :label="t('import.run.mappingTemplateName')"
                value="Import z Shoper · marzec 2026"
            />
            <label class="label" style="margin-top: 12px">{{
                t("import.run.schedule")
            }}</label>
            <Segmented
                action="set-import-schedule"
                :options="[
                    {
                        value: 'now',
                        label: t('import.run.runNow'),
                        active: true,
                    },
                    { value: 'later', label: t('import.run.scheduleLater') },
                ]"
            />
        </div>
    </div>
</template>
