<script setup lang="ts">
// MOLECULE · Pagination — centred prev/next with a mono page readout, ported from
// components/molecules/pagination.html.twig. First owner: later journeys reuse this component.
// "← Poprzednia" / "Strona … z …" / "Następna →" are never run through |trans in the old
// template either — they stay Polish on every locale, reproduced verbatim here.
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
            ← Poprzednia
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
            Następna →
        </button>
    </div>
</template>
