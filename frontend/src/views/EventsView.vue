<script setup lang="ts">
// PAGE · Strumień zdarzeń — ported from pages/events.html.twig.
// Route: GET /{locale}/events
// Data arrives from GET /api/v1/{locale}/events, pre-formatted by EventsViewService — this view
// never formats a number itself. The type/site/range query parameters are forwarded to that call
// unchanged (they only mark a filter chip active server-side, per EventsViewService's own doc) —
// see common-journey-rules.md's query-string-state rule.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Card from "@/components/molecules/Card.vue";
import EventStream from "@/components/organisms/EventStream.vue";
import EventsToolbar, {
    type ToolbarFilter,
    type ToolbarRange,
} from "@/components/organisms/EventsToolbar.vue";
import PageHead from "@/components/organisms/PageHead.vue";
import type { StreamEvent } from "@/composables/useEventStream";

interface EventsPayload {
    typeFilters: ToolbarFilter[];
    siteFilters: ToolbarFilter[];
    ranges: ToolbarRange[];
    events: StreamEvent[];
    total: string;
    shown: number;
    mercureTopic: string;
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const webhookHref = computed(() => `/${locale.value}/settings/api`);

const payload = ref<EventsPayload | null>(null);

function queryParam(name: string): string | undefined {
    const value = route.query[name];
    return typeof value === "string" ? value : undefined;
}

async function load(): Promise<void> {
    const params = new URLSearchParams();
    const type = queryParam("type");
    const site = queryParam("site");
    const range = queryParam("range");
    if (type) params.set("type", type);
    if (site) params.set("site", site);
    if (range) params.set("range", range);
    const query = params.size > 0 ? `?${params.toString()}` : "";

    const response = await fetch(`/api/v1/${locale.value}/events${query}`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as EventsPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('events.title')" :sub="t('events.sub')">
            <template #actions>
                <Chip :label="t('events.live')" tone="good" live />
                <Button
                    size="sm"
                    icon="pause"
                    :label="t('events.pause')"
                    action="pause-stream"
                />
                <Button
                    size="sm"
                    icon="download"
                    :label="t('events.exportCsv')"
                    action="export"
                />
                <Button
                    size="sm"
                    icon="code"
                    :label="t('events.webhook')"
                    :href="webhookHref"
                />
            </template>
        </PageHead>

        <EventsToolbar
            :type-filters="payload.typeFilters"
            :site-filters="payload.siteFilters"
            :ranges="payload.ranges"
        />

        <Card
            :title="t('events.log')"
            icon="list"
            :sub="`${payload.total} ${t('events.inWindow')}`"
        >
            <template #headActions>
                <Button
                    variant="ghost"
                    size="sm"
                    icon="search"
                    :label="t('events.searchLog')"
                    action="search-events"
                />
            </template>
            <template #raw>
                <EventStream
                    :events="payload.events"
                    :topic="payload.mercureTopic"
                />
            </template>
            <template #foot>
                <span class="muted" style="font-size: 12px">{{
                    t("events.showingLast", {
                        shown: payload.shown,
                        total: payload.total,
                    })
                }}</span>
                <div style="margin-left: auto">
                    <Button
                        variant="ghost"
                        size="sm"
                        :label="t('events.loadMore')"
                        action="load-more-events"
                    />
                </div>
            </template>
        </Card>
    </div>
</template>
