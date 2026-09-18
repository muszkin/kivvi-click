<script setup lang="ts">
// ORGANISM · FeedCard — connected product-feed panel, ported from
// components/organisms/feed-card.html.twig: source badge, sync status, 4 stats, actions. An
// error state inserts a bad-tone strip above the stats.
//
// `products`/`mapped`/`mappedPercent` are pre-formatted strings, not numbers: feed-card.html.twig
// used to run `products|number_format(0, ',', ' ')` and the mapped/products match-rate
// computation itself; both moved to FeedsViewService on the backend, since the SPA never formats
// a number. `mismatched` stays a raw int — the Twig source never ran it through number_format
// either.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";

type FeedSource = "google" | "facebook" | "xml" | "csv";
type FeedStatus = "synced" | "syncing" | "error" | "paused";

const props = defineProps<{
    name: string;
    url: string;
    source: FeedSource;
    status: FeedStatus;
    error?: string | null;
    products: string;
    mapped: string;
    mappedPercent?: string | null;
    mismatched: number;
    lastSync: string;
    schedule: string;
}>();

// Typed by the exact prop unions (not `Record<string, …>`) so indexing by `props.source` /
// `props.status` is exhaustive and never `| undefined` under noUncheckedIndexedAccess.
const BADGES: Record<
    FeedSource,
    { letter: string; color: string; bg: string }
> = {
    google: {
        letter: "G",
        color: "oklch(0.62 0.16 28)",
        bg: "oklch(0.93 0.04 28)",
    },
    facebook: {
        letter: "f",
        color: "oklch(0.55 0.13 250)",
        bg: "oklch(0.91 0.04 250)",
    },
    xml: {
        letter: "×",
        color: "oklch(0.45 0.07 60)",
        bg: "oklch(0.91 0.04 60)",
    },
    csv: {
        letter: "↧",
        color: "oklch(0.42 0.06 150)",
        bg: "oklch(0.91 0.04 150)",
    },
};

// PIO-129: the four sync-status labels were Polish literals here, outside the translator. Only
// the tone belongs to the component; the word comes from the catalogue like every other label.
const STATES: Record<
    FeedStatus,
    { tone: "good" | "info" | "bad" | "warn"; key: string }
> = {
    synced: { tone: "good", key: "feeds.statusSynced" },
    syncing: { tone: "info", key: "feeds.statusSyncing" },
    error: { tone: "bad", key: "feeds.statusError" },
    paused: { tone: "warn", key: "feeds.statusPaused" },
};

const { t } = useI18n();
const badge = computed(() => BADGES[props.source]);
const state = computed(() => ({
    tone: STATES[props.status].tone,
    label: t(STATES[props.status].key),
}));
const mismatchedColor = computed(() =>
    props.mismatched ? "var(--bad)" : "var(--good)",
);
</script>

<template>
    <div class="feed-card">
        <div class="feed-card__head">
            <span
                class="feed-badge"
                :style="{ background: badge.bg, color: badge.color }"
                >{{ badge.letter }}</span
            >
            <div style="min-width: 0; flex: 1">
                <div class="feed-card__title">{{ name }}</div>
                <div
                    class="muted mono"
                    style="
                        font-size: 11.5px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    "
                >
                    {{ url }}
                </div>
            </div>
            <Chip
                :label="state.label"
                :tone="state.tone"
                :live="status === 'syncing'"
            />
        </div>

        <div v-if="status === 'error'" class="feed-card__err">
            <Icon name="info" />
            {{ error }}
            <span style="margin-left: auto">
                <Button
                    size="sm"
                    icon="play"
                    :label="t('feeds.resume')"
                    action="resume-feed"
                />
            </span>
        </div>

        <div class="feed-stats">
            <div class="feed-stat">
                <div
                    class="muted"
                    style="
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                    "
                >
                    {{ t("feeds.productsLabel") }}
                </div>
                <div
                    class="mono"
                    style="
                        font-size: 20px;
                        font-weight: 500;
                        letter-spacing: -0.02em;
                    "
                >
                    {{ products }}
                </div>
            </div>
            <div class="feed-stat">
                <div
                    class="muted"
                    style="
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                    "
                >
                    Zmapowanych
                </div>
                <div
                    class="mono"
                    style="
                        font-size: 20px;
                        font-weight: 500;
                        letter-spacing: -0.02em;
                    "
                >
                    {{ mapped
                    }}<template v-if="mappedPercent"
                        >{{ " "
                        }}<span
                            class="muted"
                            style="font-size: 11px; font-weight: 400"
                            >({{ mappedPercent }})</span
                        ></template
                    >
                </div>
            </div>
            <div class="feed-stat">
                <div
                    class="muted"
                    style="
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                    "
                >
                    Niedopasowane
                </div>
                <div
                    class="mono"
                    :style="{
                        fontSize: '20px',
                        fontWeight: 500,
                        letterSpacing: '-0.02em',
                        color: mismatchedColor,
                    }"
                >
                    {{ mismatched }}
                </div>
            </div>
            <div class="feed-stat">
                <div
                    class="muted"
                    style="
                        font-size: 11px;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                    "
                >
                    Ostatnia sync.
                </div>
                <div class="mono" style="font-size: 13px; font-weight: 500">
                    {{ lastSync }}
                </div>
                <div class="muted" style="font-size: 11px">
                    harmonogram: {{ schedule }}
                </div>
            </div>
        </div>

        <div class="feed-card__actions">
            <Button
                size="sm"
                icon="play"
                label="Synchronizuj teraz"
                action="sync-feed"
            />
            <Button
                size="sm"
                icon="list"
                :label="t('feeds.fieldMapping')"
                action="map-feed"
            />
            <Button
                size="sm"
                icon="eye"
                :label="t('feeds.preview')"
                action="preview-feed"
            />
            <Button
                variant="ghost"
                size="sm"
                icon="settings"
                label="Ustawienia"
                action="edit-feed"
            />
            <div style="flex: 1"></div>
            <Button
                variant="danger"
                size="sm"
                icon="trash"
                action="remove-feed"
                :aria-label="t('feeds.removeFeed')"
            />
        </div>
    </div>
</template>
