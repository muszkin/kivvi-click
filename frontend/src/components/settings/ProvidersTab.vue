<script setup lang="ts">
// TAB · Dostawcy email — ported from pages/settings/providers.html.twig (+ provider-list,
// dns-records, sender-limits). Sending providers with deliverability, domain authentication and
// sender limits.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Field from "@/components/atoms/Field.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Card from "@/components/molecules/Card.vue";
import DnsRow from "@/components/molecules/DnsRow.vue";
import type { DnsRecordData, EmailProvider } from "@/components/settings/types";

defineProps<{ emailProviders: EmailProvider[]; dnsRecords: DnsRecordData[] }>();

const { t } = useI18n();
</script>

<template>
    <Card
        :title="t('settings.providers.cardTitle')"
        icon="mail"
        :sub="t('settings.providers.cardSub')"
    >
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.providers.addProvider')"
                action="add-provider"
            />
        </template>
        <div class="col" style="gap: 12px">
            <div
                v-for="provider in emailProviders"
                :key="provider.name"
                class="provider-row"
            >
                <div style="min-width: 0; flex: 1">
                    <div class="row" style="gap: 8px; flex-wrap: wrap">
                        <strong style="font-size: 14px">{{
                            provider.name
                        }}</strong>
                        <Chip
                            :label="provider.statusLabel"
                            :tone="provider.statusTone"
                        />
                        <Chip
                            v-if="provider.verified"
                            :label="t('settings.providers.verified')"
                            tone="good"
                            icon="check"
                        />
                        <Chip
                            v-else
                            :label="t('settings.providers.unverified')"
                            tone="warn"
                        />
                    </div>
                    <div
                        class="muted mono"
                        style="font-size: 11.5px; margin-top: 3px"
                    >
                        region: {{ provider.region }}
                    </div>
                </div>
                <div class="provider-metrics">
                    <div>
                        <div
                            class="muted"
                            style="
                                font-size: 10.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                            "
                        >
                            {{ t("settings.providers.sent30d") }}
                        </div>
                        <div class="mono" style="font-weight: 500">
                            {{ provider.sent }}
                        </div>
                    </div>
                    <div>
                        <div
                            class="muted"
                            style="
                                font-size: 10.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                            "
                        >
                            Bounce
                        </div>
                        <div
                            class="mono"
                            style="font-weight: 500"
                            :style="{
                                color: provider.bounceWarn
                                    ? 'var(--warn)'
                                    : 'var(--good)',
                            }"
                        >
                            {{ provider.bounce }}
                        </div>
                    </div>
                    <div>
                        <div
                            class="muted"
                            style="
                                font-size: 10.5px;
                                text-transform: uppercase;
                                letter-spacing: 0.06em;
                            "
                        >
                            {{ t("settings.providers.complaints") }}
                        </div>
                        <div class="mono" style="font-weight: 500">
                            {{ provider.complaint }}
                        </div>
                    </div>
                </div>
                <div class="row" style="gap: 4px">
                    <Button
                        variant="ghost"
                        size="sm"
                        icon="settings"
                        action="edit-provider"
                        :payload="provider.name"
                    />
                    <Button
                        variant="ghost"
                        size="sm"
                        icon="play"
                        :label="t('settings.providers.test')"
                        action="test-provider"
                        :payload="provider.name"
                    />
                </div>
            </div>
        </div>
    </Card>

    <Card :title="t('settings.providers.dnsCardTitle')" sub="aureashop.pl">
        <div class="col" style="gap: 8px">
            <DnsRow
                v-for="record in dnsRecords"
                :key="record.record"
                :record="record.record"
                :value="record.value"
                :ok="record.ok"
            />
        </div>
    </Card>

    <Card :title="t('settings.providers.limitsCardTitle')">
        <div class="form-grid">
            <Field
                name="default_sender"
                :label="t('settings.providers.defaultSender')"
                value="sklep@aureashop.pl"
                mono
            />
            <Field
                name="sender_name"
                :label="t('settings.providers.senderName')"
                value="Aurea Shop"
            />
            <Field
                name="reply_to"
                label="Reply-to"
                value="bok@aureashop.pl"
                mono
            />
            <Field
                name="daily_limit"
                :label="t('settings.providers.dailyLimit')"
                value="50 000"
                mono
            />
        </div>
        <div style="margin-top: 12px">
            <ToggleRow
                name="unsubscribe_hard_bounce"
                :checked="true"
                :label="t('settings.providers.unsubscribeOnHardBounce')"
            />
            <ToggleRow
                name="pause_on_bounce"
                :checked="true"
                :label="t('settings.providers.pauseOnBounceRate')"
            />
        </div>
    </Card>
</template>
