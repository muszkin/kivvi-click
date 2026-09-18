<script setup lang="ts">
// MOLECULE · Pagination — centred prev/next with a mono page readout, ported from
// components/molecules/pagination.html.twig. First owner: later journeys reuse this component.
// PIO-129: "← Poprzednia" and "Następna →" were never run through |trans in the old template
// either, so the port reproduced them verbatim and they stayed Polish on every locale. They go
// through vue-i18n now; the mono page readout between them is built by the caller.
import { useI18n } from "vue-i18n";

const { t } = useI18n();

withDefaults(
    defineProps<{
        page: number;
        pages: number;
        action?: string;
    }>(),
    { action: "go-page" },
);
</script>

<template>
    <div
        class="row"
        style="margin-top: 14px; justify-content: center; gap: 8px"
    >
        <button
            class="btn sm"
            :data-action="action"
            :data-payload="String(page - 1)"
            :disabled="page <= 1"
        >
            {{ t("common.previousPage") }}
        </button>
        <span class="mono muted" style="padding: 0 12px"
            >Strona {{ page }} z {{ pages }}</span
        >
        <button
            class="btn sm"
            :data-action="action"
            :data-payload="String(page + 1)"
            :disabled="page >= pages"
        >
            {{ t("common.nextPage") }}
        </button>
    </div>
</template>
