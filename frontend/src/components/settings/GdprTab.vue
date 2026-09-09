<script setup lang="ts">
// TAB · RODO / DPA — ported from pages/settings/gdpr.html.twig (+ dpa, retention, data-export).
// Processing agreement, retention windows, data-subject requests, export and erasure.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import SettingsSelectField from "@/components/settings/SettingsSelectField.vue";
import type {
    DataSubjectRequestData,
    RetentionPolicyData,
} from "@/components/settings/types";

defineProps<{
    retentionPolicies: RetentionPolicyData[];
    dataSubjectRequests: DataSubjectRequestData[];
}>();

const { t } = useI18n();

const dsrColumns: TableColumn[] = [
    { label: "ID" },
    { label: t("settings.gdpr.columnPerson") },
    { label: t("settings.gdpr.columnType") },
    { label: t("settings.gdpr.columnStatus") },
    { label: t("settings.gdpr.columnDue") },
    { label: "", align: "right" },
];
</script>

<template>
    <Card :title="t('settings.gdpr.dpaCardTitle')" icon="info">
        <div class="row" style="gap: 14px; flex-wrap: wrap">
            <Chip
                :label="t('settings.gdpr.dpaSigned')"
                tone="good"
                icon="check"
            />
            <Chip :label="t('settings.gdpr.dpaVersion')" />
            <Chip :label="t('settings.gdpr.dpaProcessor')" />
            <div style="flex: 1"></div>
            <Button
                size="sm"
                icon="download"
                :label="t('settings.gdpr.downloadPdf')"
                action="download-dpa"
            />
            <Button
                size="sm"
                :label="t('settings.gdpr.signNewVersion')"
                action="sign-dpa"
            />
        </div>
        <p class="muted" style="font-size: 13px; margin: 14px 0 0">
            {{ t("settings.gdpr.dpaRegionBefore") }}
            <strong>eu-central-1 (Frankfurt)</strong>.
            {{ t("settings.gdpr.dpaRegionAfter") }}
        </p>
    </Card>

    <Card
        :title="t('settings.gdpr.retentionCardTitle')"
        :sub="t('settings.gdpr.retentionCardSub')"
    >
        <div class="form-grid">
            <SettingsSelectField
                v-for="(policy, index) in retentionPolicies"
                :key="policy.label"
                :name="`retention_${index + 1}`"
                :label="policy.label"
                :options="
                    policy.options.map((option) => ({
                        value: option,
                        label: option,
                        selected: option === policy.selected,
                    }))
                "
            />
        </div>
        <div style="margin-top: 12px">
            <ToggleRow
                name="anonymize_ip"
                :checked="true"
                :label="t('settings.gdpr.anonymizeIp')"
            />
            <ToggleRow
                name="consent_gate"
                :checked="true"
                :label="t('settings.gdpr.consentGate')"
            />
            <ToggleRow
                name="ml_consent"
                :checked="false"
                :label="t('settings.gdpr.mlConsent')"
            />
        </div>
    </Card>

    <Card
        :title="t('settings.gdpr.dsrCardTitle')"
        :sub="t('settings.gdpr.dsrCardSub')"
    >
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.gdpr.newRequest')"
                action="create-dsr"
            />
        </template>
        <template #raw>
            <Table :columns="dsrColumns" :rows="dataSubjectRequests">
                <template #row="{ row }">
                    <td>
                        <span class="mono" style="font-size: 12px">{{
                            row.id
                        }}</span>
                    </td>
                    <td>
                        <span class="mono" style="font-size: 12px">{{
                            row.person
                        }}</span>
                    </td>
                    <td>{{ row.type }}</td>
                    <td>
                        <Chip
                            :label="row.status"
                            :tone="row.done ? 'good' : 'warn'"
                        />
                    </td>
                    <td>
                        <span class="muted" style="font-size: 12.5px">{{
                            row.due
                        }}</span>
                    </td>
                    <td>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="chevron_r"
                            action="open-dsr"
                            :payload="row.id"
                        />
                    </td>
                </template>
            </Table>
        </template>
    </Card>

    <Card :title="t('settings.gdpr.exportCardTitle')">
        <div class="col" style="gap: 12px">
            <div class="row" style="gap: 14px">
                <div style="flex: 1">
                    <div style="font-weight: 500; font-size: 13.5px">
                        {{ t("settings.gdpr.exportAllTitle") }}
                    </div>
                    <div class="muted" style="font-size: 12.5px">
                        {{ t("settings.gdpr.exportAllSub") }}
                    </div>
                </div>
                <Button
                    size="sm"
                    icon="download"
                    :label="t('settings.gdpr.orderExport')"
                    action="export"
                />
            </div>
            <div
                class="row"
                style="
                    gap: 14px;
                    padding-top: 12px;
                    border-top: 1px solid var(--line);
                "
            >
                <div style="flex: 1">
                    <div style="font-weight: 500; font-size: 13.5px">
                        {{ t("settings.gdpr.eraseCustomerTitle") }}
                    </div>
                    <div class="muted" style="font-size: 12.5px">
                        {{ t("settings.gdpr.eraseCustomerSub") }}
                    </div>
                </div>
                <Button
                    variant="danger"
                    size="sm"
                    icon="trash"
                    :label="t('settings.gdpr.eraseCustomer')"
                    action="erase-customer"
                />
            </div>
        </div>
    </Card>
</template>
