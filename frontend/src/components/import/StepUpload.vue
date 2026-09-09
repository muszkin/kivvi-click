<script setup lang="ts">
// PARTIAL · import step 1 · Plik — ported from pages/import/step-upload.html.twig. Dropzone plus
// the two escape hatches: a ready-made CSV template and the streaming API. Takes no props,
// exactly like the old partial: nothing on this step depends on the wizard payload.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";
import Dropzone from "@/components/molecules/Dropzone.vue";

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const apiSettingsHref = computed(() => `/${locale.value}/settings/api`);
</script>

<template>
    <div class="wiz-card">
        <h2 class="wiz-title">{{ t("import.upload.title") }}</h2>
        <p class="wiz-sub">{{ t("import.upload.sub") }}</p>

        <Dropzone
            :title="t('import.upload.dropzoneTitle')"
            :sub="t('import.upload.dropzoneSub')"
            hint="np. newsletter-2026-mar.csv"
            accept=".csv,.xml,.xlsx"
        />

        <div
            class="row"
            style="gap: 14px; margin-top: 18px; align-items: stretch"
        >
            <div class="card" style="flex: 1; padding: 14px">
                <div class="row" style="gap: 10px">
                    <span
                        style="
                            background: var(--accent-soft);
                            color: var(--accent-soft-fg);
                            width: 32px;
                            height: 32px;
                            border-radius: 8px;
                            display: grid;
                            place-items: center;
                        "
                    >
                        <Icon name="book" />
                    </span>
                    <div>
                        <div style="font-weight: 500; font-size: 13.5px">
                            {{ t("import.upload.templateTitle") }}
                        </div>
                        <div class="muted" style="font-size: 12px">
                            {{ t("import.upload.templateSub") }}
                        </div>
                    </div>
                    <span style="margin-left: auto">
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="download"
                            action="download-import-template"
                        />
                    </span>
                </div>
            </div>
            <div class="card" style="flex: 1; padding: 14px">
                <div class="row" style="gap: 10px">
                    <span
                        style="
                            background: var(--brown-soft);
                            color: var(--brown-soft-fg);
                            width: 32px;
                            height: 32px;
                            border-radius: 8px;
                            display: grid;
                            place-items: center;
                        "
                    >
                        <Icon name="code" />
                    </span>
                    <div>
                        <div style="font-weight: 500; font-size: 13.5px">
                            {{ t("import.upload.apiTitle") }}
                        </div>
                        <div class="muted" style="font-size: 12px">
                            {{ t("import.upload.apiSub") }}
                        </div>
                    </div>
                    <span style="margin-left: auto">
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="arrow_right"
                            :href="apiSettingsHref"
                        />
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>
