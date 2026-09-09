<script setup lang="ts">
// TAB · Plan i płatności — ported from pages/settings/billing.html.twig (+ plan-summary,
// limit-bars, payment-method). Current plan, limit usage, payment method and invoices.
import { useI18n } from "vue-i18n";
import Bar from "@/components/atoms/Bar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import type { InvoiceData, SettingsBar } from "@/components/settings/types";

defineProps<{ planUsage: SettingsBar[]; invoices: InvoiceData[] }>();

const { t } = useI18n();

const columns: TableColumn[] = [
    { label: t("settings.billing.columnNumber") },
    { label: t("settings.billing.columnDate") },
    { label: t("settings.billing.columnAmount"), align: "right" },
    { label: t("settings.billing.columnStatus") },
    { label: "", align: "right" },
];
</script>

<template>
    <Card class="plan-card">
        <div class="row" style="gap: 18px; align-items: flex-start">
            <div style="flex: 1">
                <div
                    class="mono"
                    style="
                        font-size: 11px;
                        letter-spacing: 0.12em;
                        text-transform: uppercase;
                        color: var(--fg-muted);
                    "
                >
                    {{ t("settings.billing.yourPlan") }}
                </div>
                <div class="row" style="gap: 10px; margin: 6px 0 4px">
                    <span class="h-display" style="font-size: 40px">Pro</span>
                    <Chip
                        :label="t('settings.billing.monthlyBilling')"
                        tone="accent"
                    />
                </div>
                <div class="muted" style="font-size: 13px">
                    {{ t("settings.billing.nextRenewal") }}
                </div>
            </div>
            <div class="row" style="gap: 8px">
                <Button
                    size="sm"
                    :label="t('settings.billing.switchToYearly')"
                    action="switch-billing-period"
                />
                <Button
                    size="sm"
                    :label="t('settings.billing.changePlan')"
                    action="change-plan"
                />
            </div>
        </div>
    </Card>

    <Card
        :title="t('settings.billing.limitsCardTitle')"
        :sub="t('settings.billing.limitsCardSub')"
    >
        <div class="bars">
            <Bar
                v-for="bar in planUsage"
                :key="bar.label"
                :label="bar.label"
                :pct="bar.pct"
                :value="bar.value"
                :tone="bar.tone"
            />
        </div>
    </Card>

    <Card :title="t('settings.billing.paymentMethodCardTitle')">
        <template #headActions>
            <Button
                size="sm"
                icon="edit"
                :label="t('settings.billing.change')"
                action="change-payment-method"
            />
        </template>
        <div class="row" style="gap: 14px">
            <span
                style="
                    width: 44px;
                    height: 30px;
                    border-radius: 6px;
                    background: var(--bg-sunken);
                    border: 1px solid var(--line);
                    display: grid;
                    place-items: center;
                    font-family: var(--font-mono);
                    font-size: 10px;
                    font-weight: 600;
                "
                >VISA</span
            >
            <div style="flex: 1">
                <div class="mono" style="font-size: 13px">
                    •••• •••• •••• 4291
                </div>
                <div class="muted" style="font-size: 12px">
                    {{ t("settings.billing.cardExpiry") }}
                </div>
            </div>
            <Chip
                :label="t('settings.billing.defaultPaymentMethod')"
                tone="good"
            />
        </div>
        <div style="margin-top: 14px">
            <ToggleRow
                name="invoice_recipient"
                :checked="true"
                :label="t('settings.billing.invoiceRecipient')"
            />
        </div>
    </Card>

    <Card :title="t('settings.billing.invoicesCardTitle')">
        <template #headActions>
            <Button
                variant="ghost"
                size="sm"
                icon="download"
                :label="t('settings.billing.downloadAll')"
                action="download-invoices"
            />
        </template>
        <template #raw>
            <Table :columns="columns" :rows="invoices">
                <template #row="{ row }">
                    <td>
                        <span class="mono" style="font-size: 12.5px">{{
                            row.number
                        }}</span>
                    </td>
                    <td>
                        <span class="muted" style="font-size: 12.5px">{{
                            row.date
                        }}</span>
                    </td>
                    <td>
                        <span class="mono">{{ row.amount }}</span>
                    </td>
                    <td><Chip :label="row.status" tone="good" /></td>
                    <td>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="download"
                            action="download-invoice"
                            :payload="row.number"
                        />
                    </td>
                </template>
            </Table>
        </template>
    </Card>
</template>
