<script setup lang="ts">
// PAGE · Feedy produktów — ported from pages/feeds.html.twig (+ the matching diagnostic partial
// pages/feeds/matching.html.twig, inlined here: it is a page-specific partial in the old stack,
// not a reusable design-system component, so it never earned its own Vue component).
//
// Data arrives from GET /api/v1/{locale}/feeds, pre-formatted by FeedsViewService — this view
// never formats a number itself.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Bar from "@/components/atoms/Bar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import FeedCard from "@/components/organisms/FeedCard.vue";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";
import PageHead from "@/components/organisms/PageHead.vue";

interface FeedsSource {
    id: string;
    letter: string;
    color: string;
    bg: string;
    title: string;
    sub: string;
    recommended: boolean;
}

interface FeedsFeed {
    name: string;
    url: string;
    source: "google" | "facebook" | "xml" | "csv";
    status: "synced" | "syncing" | "error" | "paused";
    error?: string | null;
    products: string;
    mapped: string;
    mappedPercent?: string | null;
    mismatched: number;
    lastSync: string;
    schedule: string;
}

interface FeedsCoverageBar {
    label: string;
    pct: number;
    value: string;
    tone: "accent" | "brown" | "bad";
}

interface FeedsFallbackRule {
    text: string;
    field: string;
}

interface FeedsPayload {
    kpis: KpiTileData[];
    sources: FeedsSource[];
    feeds: FeedsFeed[];
    coverage: FeedsCoverageBar[];
    fallbackRules: FeedsFallbackRule[];
    mismatched: number;
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);

const payload = ref<FeedsPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(`/api/v1/${locale.value}/feeds`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as FeedsPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('feeds.title')" :sub="t('feeds.sub')">
            <template #actions>
                <Button
                    size="sm"
                    icon="book"
                    :label="t('common.documentation')"
                    action="open-docs"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('feeds.connectFeed')"
                    action="connect-feed"
                />
            </template>
        </PageHead>

        <KpiGrid :tiles="payload.kpis" />

        <div class="section-title" style="margin-top: 8px">
            {{ t("feeds.availableSources") }}
        </div>
        <div class="feed-sources">
            <button
                v-for="source in payload.sources"
                :key="source.id"
                class="feed-source"
                data-action="connect-feed"
                :data-payload="source.id"
            >
                <span
                    class="feed-badge"
                    :style="{ background: source.bg, color: source.color }"
                    >{{ source.letter }}</span
                >
                <div>
                    <div class="feed-source__title">{{ source.title }}</div>
                    <div class="muted" style="font-size: 12px">
                        {{ source.sub }}
                    </div>
                </div>
                <span
                    v-if="source.recommended"
                    class="chip accent"
                    style="margin-left: auto"
                    >{{ t("feeds.recommended") }}</span
                >
            </button>
        </div>

        <div class="section-title" style="margin-top: 24px">
            {{ t("feeds.connectedFeeds") }}
        </div>
        <div class="col" style="gap: 14px">
            <FeedCard
                v-for="feed in payload.feeds"
                :key="feed.url"
                :name="feed.name"
                :url="feed.url"
                :source="feed.source"
                :status="feed.status"
                :error="feed.error"
                :products="feed.products"
                :mapped="feed.mapped"
                :mapped-percent="feed.mappedPercent"
                :mismatched="feed.mismatched"
                :last-sync="feed.lastSync"
                :schedule="feed.schedule"
            />
        </div>

        <div style="margin-top: 24px">
            <Card
                :title="t('feeds.matchingTitle')"
                icon="target"
                :sub="t('feeds.matchingSub')"
            >
                <template #headActions>
                    <Button
                        variant="ghost"
                        size="sm"
                        icon="download"
                        :label="t('feeds.report')"
                        action="export"
                    />
                </template>

                <div
                    style="
                        display: grid;
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                        gap: 24px;
                    "
                >
                    <div>
                        <div
                            class="muted"
                            style="
                                font-size: 11.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                                margin-bottom: 8px;
                            "
                        >
                            {{ t("feeds.joinField") }}
                        </div>
                        <div class="row" style="gap: 8px; margin-bottom: 14px">
                            <Chip label="event.product_id" mono />
                            <span style="color: var(--fg-muted)">⇌</span>
                            <Chip label="feed.id" mono />
                            <Button
                                variant="ghost"
                                size="sm"
                                icon="edit"
                                action="edit-join-field"
                            />
                        </div>

                        <div
                            class="muted"
                            style="
                                font-size: 11.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                                margin-bottom: 8px;
                            "
                        >
                            {{ t("feeds.fallbackRules") }}
                        </div>
                        <div class="col" style="gap: 4px">
                            <div
                                v-for="rule in payload.fallbackRules"
                                :key="rule.field"
                                class="row"
                                style="gap: 8px; font-size: 12.5px"
                            >
                                <Icon name="check" />{{ " " }}{{ rule.text }}
                                <Chip :label="rule.field" mono />
                            </div>
                        </div>
                    </div>

                    <div>
                        <div
                            class="muted"
                            style="
                                font-size: 11.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                                margin-bottom: 10px;
                            "
                        >
                            {{ t("feeds.matchRate") }}
                        </div>
                        <div class="bars">
                            <Bar
                                v-for="bar in payload.coverage"
                                :key="bar.label"
                                :label="bar.label"
                                :pct="bar.pct"
                                :value="bar.value"
                                :tone="bar.tone"
                                :label-width="120"
                            />
                        </div>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="list"
                            :label="
                                t('feeds.showMismatched', {
                                    count: payload.mismatched,
                                })
                            "
                            action="show-mismatched"
                            style="margin-top: 10px"
                        />
                    </div>
                </div>
            </Card>
        </div>
    </div>
</template>
