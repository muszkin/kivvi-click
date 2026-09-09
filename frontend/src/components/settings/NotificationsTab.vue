<script setup lang="ts">
// TAB · Powiadomienia — ported from pages/settings/notifications.html.twig (+
// notification-toggle, notification-channels, quiet-hours). Event × channel matrix, the connected
// channels, and quiet hours. The matrix table needs the "scroll" (overflow-x) wrapper — see
// ApiTab.vue's comment on the same shared-Table.vue limitation.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Field from "@/components/atoms/Field.vue";
import Icon from "@/components/atoms/Icon.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import type { NotificationRowData } from "@/components/settings/types";

defineProps<{ notificationMatrix: NotificationRowData[] }>();

const { t } = useI18n();

const columns: TableColumn[] = [
    { label: t("settings.notifications.columnEvent") },
    { label: "E-mail" },
    { label: "Slack" },
    { label: "SMS" },
];
</script>

<template>
    <Card
        :title="t('settings.notifications.cardTitle')"
        icon="bell"
        :sub="t('settings.notifications.cardSub')"
    >
        <template #raw>
            <div style="overflow-x: auto">
                <Table :columns="columns" :rows="notificationMatrix">
                    <template #row="{ row }">
                        <td>
                            <span style="font-weight: 500">{{
                                row.label
                            }}</span>
                        </td>
                        <td style="text-align: center">
                            <input
                                type="checkbox"
                                :name="`notify[email][]`"
                                :value="row.label"
                                :checked="row.email"
                                :aria-label="`${row.label} — email`"
                                style="
                                    accent-color: var(--accent);
                                    width: 16px;
                                    height: 16px;
                                "
                            />
                        </td>
                        <td style="text-align: center">
                            <input
                                type="checkbox"
                                :name="`notify[slack][]`"
                                :value="row.label"
                                :checked="row.slack"
                                :aria-label="`${row.label} — slack`"
                                style="
                                    accent-color: var(--accent);
                                    width: 16px;
                                    height: 16px;
                                "
                            />
                        </td>
                        <td style="text-align: center">
                            <input
                                type="checkbox"
                                :name="`notify[sms][]`"
                                :value="row.label"
                                :checked="row.sms"
                                :aria-label="`${row.label} — sms`"
                                style="
                                    accent-color: var(--accent);
                                    width: 16px;
                                    height: 16px;
                                "
                            />
                        </td>
                    </template>
                </Table>
            </div>
        </template>
    </Card>

    <Card :title="t('settings.notifications.channelsCardTitle')">
        <div class="col" style="gap: 12px">
            <div
                class="row"
                style="
                    gap: 14px;
                    padding: 12px 14px;
                    background: var(--bg);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-md);
                "
            >
                <span
                    style="
                        width: 32px;
                        height: 32px;
                        border-radius: 8px;
                        background: var(--accent-soft);
                        color: var(--accent-soft-fg);
                        display: grid;
                        place-items: center;
                    "
                    ><Icon name="mail"
                /></span>
                <div style="flex: 1">
                    <div style="font-weight: 500; font-size: 13.5px">
                        E-mail
                    </div>
                    <div class="muted mono" style="font-size: 12px">
                        maciej@aureashop.pl
                    </div>
                </div>
                <Chip
                    :label="t('settings.notifications.active')"
                    tone="good"
                    icon="check"
                />
            </div>
            <div
                class="row"
                style="
                    gap: 14px;
                    padding: 12px 14px;
                    background: var(--bg);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-md);
                "
            >
                <span
                    style="
                        width: 32px;
                        height: 32px;
                        border-radius: 8px;
                        background: var(--brown-soft);
                        color: var(--brown-soft-fg);
                        display: grid;
                        place-items: center;
                    "
                    ><Icon name="bell"
                /></span>
                <div style="flex: 1">
                    <div style="font-weight: 500; font-size: 13.5px">Slack</div>
                    <div class="muted mono" style="font-size: 12px">
                        #kivvi-alerty · workspace aurea
                    </div>
                </div>
                <Button
                    size="sm"
                    :label="t('settings.notifications.disconnect')"
                    action="disconnect-slack"
                />
            </div>
            <div
                class="row"
                style="
                    gap: 14px;
                    padding: 12px 14px;
                    background: var(--bg);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-md);
                "
            >
                <span
                    style="
                        width: 32px;
                        height: 32px;
                        border-radius: 8px;
                        background: var(--bg-sunken);
                        color: var(--fg-muted);
                        display: grid;
                        place-items: center;
                    "
                    ><Icon name="user"
                /></span>
                <div style="flex: 1">
                    <div style="font-weight: 500; font-size: 13.5px">SMS</div>
                    <div class="muted" style="font-size: 12px">
                        {{ t("settings.notifications.smsOnly") }}
                    </div>
                </div>
                <Button
                    variant="primary"
                    size="sm"
                    :label="t('settings.notifications.connect')"
                    action="connect-sms"
                />
            </div>
        </div>
    </Card>

    <Card :title="t('settings.notifications.quietHoursCardTitle')">
        <div class="form-grid">
            <Field
                name="quiet_from"
                :label="t('settings.notifications.quietFrom')"
                value="22:00"
                mono
            />
            <Field
                name="quiet_to"
                :label="t('settings.notifications.quietTo')"
                value="07:00"
                mono
            />
        </div>
        <div style="margin-top: 12px">
            <ToggleRow
                name="critical_bypass"
                :checked="true"
                :label="t('settings.notifications.criticalBypass')"
            />
        </div>
    </Card>
</template>
