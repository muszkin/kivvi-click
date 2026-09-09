<script setup lang="ts">
// TAB · Zespół — ported from pages/settings/team.html.twig (+ roles). Members with their role and
// 2FA state, then the role catalogue.
import { useI18n } from "vue-i18n";
import Avatar from "@/components/atoms/Avatar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Card from "@/components/molecules/Card.vue";
import Table, { type TableColumn } from "@/components/molecules/Table.vue";
import type { SettingsRole, TeamMember } from "@/components/settings/types";

const props = defineProps<{ team: TeamMember[]; roles: SettingsRole[] }>();

const { t } = useI18n();

const columns: TableColumn[] = [
    { label: t("settings.team.columnPerson") },
    { label: t("settings.team.columnRole") },
    { label: "2FA" },
    { label: t("settings.team.columnLastActivity") },
    { label: t("settings.team.columnActions"), align: "right" },
];

function roleCountLabel(count: number): string {
    const noun = t(
        count === 1
            ? "settings.team.personSingular"
            : "settings.team.personPlural",
    );
    return `${count} ${noun}`;
}
</script>

<template>
    <Card
        :title="t('settings.team.cardTitle')"
        icon="users"
        :sub="t('settings.team.cardSub')"
    >
        <template #headActions>
            <Button
                variant="primary"
                size="sm"
                icon="plus"
                :label="t('settings.team.invite')"
                action="invite-member"
            />
        </template>
        <template #raw>
            <Table :columns="columns" :rows="props.team">
                <template #row="{ row }">
                    <td>
                        <div class="row">
                            <Avatar
                                :name="row.name"
                                :size="28"
                                :text="row.invited ? '?' : undefined"
                            />
                            <div style="min-width: 0">
                                <div style="font-weight: 500">
                                    {{ row.name }}
                                    <Chip
                                        v-if="row.invited"
                                        :label="t('settings.team.invited')"
                                        tone="warn"
                                    />
                                </div>
                                <div
                                    class="muted mono"
                                    style="font-size: 11.5px"
                                >
                                    {{ row.email }}
                                </div>
                            </div>
                        </div>
                    </td>
                    <td>
                        <Chip :label="row.role" :tone="row.roleTone" />
                    </td>
                    <td>
                        <Chip
                            v-if="row.mfa"
                            :label="t('settings.team.mfaOn')"
                            tone="good"
                            icon="check"
                        />
                        <Chip
                            v-else
                            :label="t('settings.team.mfaOff')"
                            tone="bad"
                        />
                    </td>
                    <td>
                        <span class="muted" style="font-size: 12.5px">{{
                            row.last
                        }}</span>
                    </td>
                    <td>
                        <!-- repair-2 (R2-A): same fix as ApiTab.vue's row-action cell — see its
                             comment. team.html.twig concatenates these two button includes with
                             `~` and nothing else, but each carries a trailing newline from its
                             own file that a plain (non-flex) td renders as one space. -->
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="edit"
                            action="edit-member"
                            :payload="row.email"
                        />{{ " "
                        }}<Button
                            variant="ghost"
                            size="sm"
                            icon="trash"
                            action="remove-member"
                            :payload="row.email"
                        />
                    </td>
                </template>
            </Table>
        </template>
    </Card>

    <Card :title="t('settings.team.rolesCardTitle')">
        <template #headActions>
            <Button
                size="sm"
                icon="plus"
                :label="t('settings.team.createRole')"
                action="create-role"
            />
        </template>
        <div class="col" style="gap: 10px">
            <div
                v-for="role in props.roles"
                :key="role.name"
                class="row"
                style="
                    gap: 14px;
                    padding: 12px 14px;
                    background: var(--bg);
                    border: 1px solid var(--line);
                    border-radius: var(--radius-md);
                "
            >
                <div style="flex: 1; min-width: 0">
                    <div style="font-weight: 500; font-size: 13.5px">
                        {{ role.name }}
                    </div>
                    <div class="muted" style="font-size: 12.5px">
                        {{ role.description }}
                    </div>
                </div>
                <Chip :label="roleCountLabel(role.count)" />
                <Button
                    variant="ghost"
                    size="sm"
                    icon="chevron_r"
                    action="open-role"
                    :payload="role.name"
                />
            </div>
        </div>
    </Card>
</template>
