<script setup lang="ts">
// TAB · Webhooks i API — ported from pages/settings/api.html.twig (+ webhooks, limit-bars). API
// keys with scopes, webhook endpoints with their last HTTP status, and rate limits.
//
// The API-keys table needs the old molecule's "scroll" (overflow-x) wrapper, which the shared
// Table.vue molecule does not implement yet (its own comment: "no page in this journey needs it,
// so it is left for whichever later journey does" — Table.vue is a component this journey
// consumes, not owns, see the packet's parallel-safety touch list). Table.vue's only root element
// is the bare `<table>`, so the wrapper is reproduced here instead, around the component, which
// yields the identical DOM without editing the shared molecule.
import { useI18n } from "vue-i18n";
import Bar from "@/components/atoms/Bar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";
import Card from "@/components/molecules/Card.vue";
import HookRow from "@/components/molecules/HookRow.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import type {
    ApiKeyData,
    SettingsBar,
    WebhookData,
} from "@/components/settings/types";

defineProps<{
    apiKeys: ApiKeyData[];
    webhooks: WebhookData[];
    apiLimits: SettingsBar[];
}>();

const { t } = useI18n();

const columns: TableColumn[] = [
    { label: t("settings.api.columnName") },
    { label: t("settings.api.columnKey") },
    { label: t("settings.api.columnScopes") },
    { label: t("settings.api.columnCreated") },
    { label: t("settings.api.columnLastUsed") },
    { label: "", align: "right" },
];
</script>

<template>
    <Card :title="t('settings.api.keysCardTitle')" icon="code">
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.api.newKey')"
                action="create-api-key"
            />
        </template>
        <template #raw>
            <div style="overflow-x: auto">
                <Table :columns="columns" :rows="apiKeys">
                    <template #row="{ row }">
                        <td>
                            <span style="font-weight: 500">{{ row.name }}</span>
                        </td>
                        <td>
                            <span class="mono" style="font-size: 11.5px"
                                >{{ row.prefix
                                }}<span class="muted">…••••</span></span
                            >
                        </td>
                        <td>
                            <!-- The old markup concatenates chip.html.twig includes with no
                                 separator string, but each chip's own multi-line HTML source
                                 leaves a raw newline before its closing </span>, which the
                                 browser collapses into a single rendered space between
                                 chips — Vue's compiler strips that same whitespace at build
                                 time (no newline survives to be collapsed), so the space is
                                 inserted explicitly here instead. -->
                            <template
                                v-for="(scope, index) in row.scopes"
                                :key="scope"
                                >{{ index > 0 ? " " : ""
                                }}<Chip :label="scope" mono
                            /></template>
                        </td>
                        <td>
                            <span class="muted" style="font-size: 12.5px">{{
                                row.created
                            }}</span>
                        </td>
                        <td>
                            <span class="muted" style="font-size: 12.5px">{{
                                row.last
                            }}</span>
                        </td>
                        <td>
                            <!-- repair-2 (R2-A): api.html.twig concatenates these two button
                                 includes with Twig's `~` and nothing else between them, but
                                 button.html.twig's own file ends with a trailing newline after
                                 </button> — a real, rendered text node the browser collapses to
                                 one space between the two buttons (the td is a plain table
                                 cell, not a flex container, so that whitespace is not stripped
                                 the way it is inside a flex-gapped wrapper). Vue's compiler
                                 drops the newline between these two sibling components
                                 entirely, so the space is inserted explicitly instead — this is
                                 what actually narrowed every other column in this table (auto
                                 table-layout redistributing the ~3px this cell was under-
                                 claiming), not a font or browser-version difference. -->
                            <Button
                                variant="ghost"
                                size="sm"
                                icon="copy"
                                action="copy-api-key"
                                :payload="row.prefix"
                            />{{ " "
                            }}<Button
                                variant="ghost"
                                size="sm"
                                icon="trash"
                                action="revoke-api-key"
                                :payload="row.prefix"
                            />
                        </td>
                    </template>
                </Table>
            </div>
        </template>
    </Card>

    <Card title="Webhooks" :sub="t('settings.api.webhooksCardSub')">
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.api.addWebhook')"
                action="create-webhook"
            />
        </template>
        <div class="col" style="gap: 10px">
            <HookRow
                v-for="hook in webhooks"
                :key="hook.url"
                :url="hook.url"
                :events="hook.events"
                :code="hook.code"
                :last="hook.last"
            />
            <!-- The shared Callout molecule (components/molecules/Callout.vue) has no right-hand
                 action slot yet — components/molecules/callout.html.twig's own `action` param
                 (see webhooks.html.twig) is reproduced directly here instead of extending a
                 component this journey does not own; see worker-report.md. -->
            <div class="callout">
                <Icon name="info" />
                <span>{{ t("settings.api.webhookWarning") }}</span>
                <span style="margin-left: auto">
                    <Button
                        variant="ghost"
                        size="sm"
                        :label="t('settings.api.showLogs')"
                        action="show-webhook-logs"
                    />
                </span>
            </div>
        </div>
    </Card>

    <Card :title="t('settings.api.limitsCardTitle')">
        <div class="bars">
            <Bar
                v-for="bar in apiLimits"
                :key="bar.label"
                :label="bar.label"
                :pct="bar.pct"
                :value="bar.value"
                :tone="bar.tone"
            />
        </div>
    </Card>
</template>
