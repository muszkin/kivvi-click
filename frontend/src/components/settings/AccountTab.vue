<script setup lang="ts">
// TAB · Konto — ported from pages/settings/account.html.twig (+ account-form, account-owner,
// account-danger). Company data, account owner, and the irreversible zone. This tab's fields are
// display-only fixture text (no persistence exists today — see the packet's "Out of scope").
import { useI18n } from "vue-i18n";
import Avatar from "@/components/atoms/Avatar.vue";
import Button from "@/components/atoms/Button.vue";
import Field from "@/components/atoms/Field.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import Card from "@/components/molecules/Card.vue";
import SettingsSelectField from "@/components/settings/SettingsSelectField.vue";

const { t } = useI18n();
</script>

<template>
    <Card :title="t('settings.account.cardTitle')">
        <div class="form-grid">
            <Field
                name="company"
                :label="t('settings.account.companyName')"
                value="Aurea Shop sp. z o.o."
            />
            <Field
                name="vat_id"
                :label="t('settings.account.vatId')"
                value="527-289-11-04"
                mono
            />
            <Field
                name="street"
                :label="t('settings.account.street')"
                value="ul. Świętokrzyska 18/22"
            />
            <Field
                name="city"
                :label="t('settings.account.cityAndPostcode')"
                value="00-052 Warszawa"
            />
            <SettingsSelectField
                name="country"
                :label="t('settings.account.country')"
                :options="[
                    {
                        value: 'pl',
                        label: t('settings.account.countryPoland'),
                        selected: true,
                    },
                    {
                        value: 'de',
                        label: t('settings.account.countryGermany'),
                    },
                    {
                        value: 'cz',
                        label: t('settings.account.countryCzechia'),
                    },
                ]"
            />
            <SettingsSelectField
                name="currency"
                :label="t('settings.account.currency')"
                :options="[
                    { value: 'PLN', label: 'PLN', selected: true },
                    { value: 'EUR', label: 'EUR' },
                ]"
            />
        </div>
        <template #foot>
            <div style="margin-left: auto; display: flex; gap: 8px">
                <Button
                    size="sm"
                    :label="t('settings.account.cancel')"
                    action="cancel-account-form"
                />
                <Button
                    variant="primary"
                    size="sm"
                    :label="t('settings.account.save')"
                    action="save-account"
                />
            </div>
        </template>
    </Card>

    <Card :title="t('settings.account.ownerCardTitle')">
        <div class="row" style="gap: 14px">
            <Avatar name="Maciej Kowalczyk" :size="48" />
            <div style="flex: 1">
                <div style="font-weight: 500">Maciej Kowalczyk</div>
                <div class="muted mono" style="font-size: 12px">
                    maciej@aureashop.pl
                </div>
            </div>
            <Button
                size="sm"
                icon="edit"
                :label="t('settings.account.transferOwnership')"
                action="transfer-ownership"
            />
        </div>
        <div
            style="
                margin-top: 18px;
                padding-top: 16px;
                border-top: 1px solid var(--line);
            "
        >
            <ToggleRow
                name="require_mfa"
                :checked="true"
                :label="t('settings.account.require2fa')"
            />
            <ToggleRow
                name="session_expiry"
                :checked="true"
                :label="t('settings.account.sessionExpiry')"
            />
        </div>
    </Card>

    <Card class="danger-zone" :title="t('settings.account.dangerZoneTitle')">
        <div class="row" style="gap: 14px">
            <div style="flex: 1">
                <div style="font-weight: 500; font-size: 13.5px">
                    {{ t("settings.account.deleteAccountTitle") }}
                </div>
                <div class="muted" style="font-size: 12.5px">
                    {{ t("settings.account.deleteAccountBody") }}
                </div>
            </div>
            <Button
                variant="danger"
                size="sm"
                icon="trash"
                :label="t('settings.account.deleteAccount')"
                action="delete-account"
            />
        </div>
    </Card>
</template>
