<script setup lang="ts">
// PARTIAL · import step 1 · recent imports — ported from pages/import/recent-imports.html.twig.
// Sticky right column: what was imported before, by whom, with what result.
//
// recent-imports.html.twig:14 wraps each row in `class="event-row"` — reusing the live event
// stream's compact grid/hover/border-bottom styling for this unrelated list (this partial has no
// event data at all), not a second implementation of the live event row itself. The visual match
// (:hover, :last-child) is not reproducible via inline styles, so the class is written as the
// same literal the old template uses; frontend/eslint.config.js has a matching, narrowly-scoped
// override for this exact file (see its comment) — the same governed exception ListCard.vue
// already uses for the same reason (its own dashboard partials do the identical reuse).
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import { importStepPath } from "@/components/import/importRoute";

export interface RecentImportItem {
    file: string;
    rows: string;
    date: string;
    who: string;
    ok: boolean;
    note: string;
}

defineProps<{ imports: RecentImportItem[] }>();

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const historyHref = computed(() => importStepPath(locale.value, 1));
</script>

<template>
    <div class="card" style="height: fit-content; position: sticky; top: 24px">
        <div class="card-head">
            <h3 class="card-title">
                <Icon name="list" /> {{ t("import.recent.title") }}
            </h3>
        </div>
        <div>
            <div
                v-for="(item, index) in imports"
                :key="index"
                class="event-row"
                style="grid-template-columns: 1fr auto; padding: 12px 14px"
            >
                <div>
                    <div
                        class="mono"
                        style="font-size: 12.5px; font-weight: 500"
                    >
                        {{ item.file }}
                    </div>
                    <div class="muted" style="font-size: 11px; margin-top: 2px">
                        {{ item.rows }} {{ t("import.recent.rows") }} ·
                        {{ item.date }} ·
                        {{ item.who }}
                    </div>
                </div>
                <Chip :label="item.note" :tone="item.ok ? 'good' : 'warn'" />
            </div>
        </div>
        <div class="card-foot">
            <a
                class="muted"
                style="font-size: 12px"
                :href="historyHref"
                data-action="import-history"
                >{{ t("import.recent.showAll") }}</a
            >
        </div>
    </div>
</template>
