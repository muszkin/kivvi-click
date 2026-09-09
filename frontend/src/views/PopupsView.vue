<script setup lang="ts">
// PAGE · Popupy i widgety — ported from pages/popups.html.twig.
// Route: GET /{locale}/popups
// Data arrives from GET /api/v1/{locale}/popups?preview=, pre-formatted by WidgetViewService —
// this view never formats a number itself. `?preview=` is a real navigation (see the "navigate"
// intent): clicking a card reloads this same route with a different query value, exactly like the
// oracle's own http.jsonl records for that step.
//
// WidgetViewService.list()/the API's own `cards[].action`/`payload` mirror WidgetCatalog::cards()
// verbatim ("go-popup"/id) — pages/popups.html.twig itself overrode that to a "navigate" action
// plus a `?preview=` URL only at the template layer (`p|merge({action: 'navigate', payload: ...})`
// in the Twig source), so this view performs that same override here, the same layering
// AutomationsView.vue/useIntents.ts's own "go-automation" precedent already established for
// list-card.html.twig's row-click override (see that composable's class comment) — the difference
// here is the destination itself (a `?preview=` query on this same route, not another page), so no
// new shared intent is registered for it.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import AutoCard, {
    type AutoCardChip,
    type AutoCardMetric,
} from "@/components/organisms/AutoCard.vue";
import PageHead from "@/components/organisms/PageHead.vue";
import PopupStage from "@/components/organisms/PopupStage.vue";
import PopupWidget from "@/components/organisms/PopupWidget.vue";

interface WidgetCard {
    title: string;
    chips: AutoCardChip[];
    metrics: AutoCardMetric[];
    action: string;
    payload: string;
}

interface WidgetContent {
    type: string;
    kicker?: string | null;
    title: string;
    body?: string | null;
    placeholder?: string | null;
    cta?: string | null;
    fine?: string | null;
}

interface SelectedWidget {
    id: string;
    name: string;
    type: string;
    content: WidgetContent;
}

interface PopupsPayload {
    cards: WidgetCard[];
    selected: SelectedWidget;
    types: { id: string; label: string; icon: string }[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const newPopupHref = computed(() => `/${locale.value}/popups/new`);

const payload = ref<PopupsPayload | null>(null);

function previewParam(): string | undefined {
    const value = route.query.preview;
    return typeof value === "string" ? value : undefined;
}

function previewHref(id: string): string {
    return `/${locale.value}/popups?preview=${id}`;
}

function editHref(id: string): string {
    return `/${locale.value}/popups/${id}`;
}

async function load(): Promise<void> {
    const preview = previewParam();
    const query = preview ? `?preview=${encodeURIComponent(preview)}` : "";
    const response = await fetch(`/api/v1/${locale.value}/popups${query}`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as PopupsPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('popups.title')" :sub="t('popups.sub')">
            <template #actions>
                <Button
                    size="sm"
                    icon="book"
                    :label="t('popups.templates')"
                    action="open-templates"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('popups.newPopup')"
                    :href="newPopupHref"
                />
            </template>
        </PageHead>

        <div class="split">
            <div class="col" style="gap: 12px">
                <AutoCard
                    v-for="(card, index) in payload.cards"
                    :key="index"
                    :title="card.title"
                    :chips="card.chips"
                    :metrics="card.metrics"
                    action="navigate"
                    :payload="previewHref(card.payload)"
                >
                    <template #actions>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="eye"
                            :href="previewHref(card.payload)"
                        />
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="edit"
                            :href="editHref(card.payload)"
                        />
                    </template>
                </AutoCard>
            </div>

            <div>
                <div class="section-title">
                    <Icon name="eye" />{{ " " }}{{ t("popups.preview") }}:
                    {{ payload.selected.name }}
                    <span style="margin-left: auto">
                        <Button
                            size="sm"
                            icon="edit"
                            :label="t('popups.edit')"
                            :href="editHref(payload.selected.id)"
                        />
                    </span>
                </div>
                <PopupStage :type="payload.selected.type" device="desktop">
                    <PopupWidget
                        :type="payload.selected.content.type"
                        :kicker="payload.selected.content.kicker"
                        :title="payload.selected.content.title"
                        :body="payload.selected.content.body"
                        :placeholder="payload.selected.content.placeholder"
                        :cta="payload.selected.content.cta"
                        :fine="payload.selected.content.fine"
                    />
                </PopupStage>
                <div style="margin-top: 14px">
                    <Card>
                        <div
                            class="muted"
                            style="
                                font-size: 11.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                                margin-bottom: 6px;
                            "
                        >
                            {{ t("popups.showsWhen") }}
                        </div>
                        <div
                            style="font-size: 13px"
                            v-html="t('popups.triggerSummary')"
                        ></div>
                    </Card>
                </div>
            </div>
        </div>
    </div>
</template>
