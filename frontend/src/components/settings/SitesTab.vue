<script setup lang="ts">
// TAB · Śledzone strony — ported from pages/settings/sites.html.twig (+ tracker-snippet,
// automatic-events). Tracked domains, the tracker snippet with its detection state, and the
// automatically collected events.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import CodeBlock from "@/components/atoms/CodeBlock.vue";
import Dot from "@/components/atoms/Dot.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import type { TrackedSite } from "@/components/settings/types";

defineProps<{
    trackedSites: TrackedSite[];
    automaticEvents: string[];
    trackerSnippet: string;
}>();

const { t } = useI18n();

const columns: TableColumn[] = [
    { label: t("settings.sites.columnDomain") },
    { label: t("settings.sites.columnTrackingStatus") },
    { label: t("settings.sites.columnEvents24h"), align: "right" },
    { label: t("settings.sites.columnScript"), align: "right" },
    { label: "" },
];
</script>

<template>
    <Card
        :title="t('settings.sites.cardTitle')"
        :sub="t('settings.sites.cardSub')"
    >
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.sites.addSite')"
                action="add-site"
            />
        </template>
        <template #raw>
            <Table :columns="columns" :rows="trackedSites">
                <template #row="{ row }">
                    <td>
                        <div class="row">
                            <Dot :color="row.color" />
                            <span style="font-weight: 500">{{ row.name }}</span>
                        </div>
                    </td>
                    <td>
                        <Chip
                            :label="t('settings.sites.active')"
                            tone="good"
                            live
                        />
                    </td>
                    <td>
                        <span class="mono">{{ row.events }}</span>
                    </td>
                    <td>
                        <span
                            class="mono"
                            style="font-size: 11px; color: var(--fg-muted)"
                            >{{ row.key }}</span
                        >
                    </td>
                    <td>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="settings"
                            action="edit-site"
                            :payload="row.name"
                        />
                    </td>
                </template>
            </Table>
        </template>
    </Card>

    <Card
        :title="t('settings.sites.scriptCardTitle')"
        icon="code"
        sub="aureashop.pl"
    >
        <template #headActions>
            <Button
                size="sm"
                icon="copy"
                :label="t('settings.sites.copySnippet')"
                action="copy-snippet"
            />
        </template>
        <p class="muted" style="margin: 0 0 12px; font-size: 13px">
            {{ t("settings.sites.snippetHelpBefore") }}
            <span class="mono">&lt;/head&gt;</span>
            {{ t("settings.sites.snippetHelpAfter") }}
        </p>
        <CodeBlock :code="trackerSnippet" lang="html" />
        <div class="row" style="margin-top: 14px; gap: 14px; flex-wrap: wrap">
            <Chip
                :label="t('settings.sites.scriptDetected')"
                tone="good"
                icon="check"
            />
            <Chip :label="t('settings.sites.firstEvent')" />
            <Chip label="Wersja k.js: 2.4.1" />
        </div>
    </Card>

    <Card
        :title="t('settings.sites.automaticEventsTitle')"
        icon="bolt"
        :sub="t('settings.sites.automaticEventsSub')"
    >
        <div class="auto-events">
            <div
                v-for="event in automaticEvents"
                :key="event"
                class="row"
                style="
                    gap: 8px;
                    padding: 10px 12px;
                    background: var(--bg);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-sm);
                    font-size: 12.5px;
                "
            >
                <Chip label="ON" tone="good" />
                <span class="mono">{{ event }}</span>
            </div>
        </div>
    </Card>
</template>
