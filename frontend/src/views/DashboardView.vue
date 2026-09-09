<script setup lang="ts">
// PAGE · Pulpit — ported from pages/dashboard.html.twig (+ pages/dashboard/{recent-customers,
// top-automations}.html.twig).
// Route: GET /{locale}/dashboard
// Data arrives from GET /api/v1/{locale}/dashboard, pre-formatted by DashboardViewService — this
// view never formats a number itself. The cardiogram's three range-button labels are the one
// exception: dashboard.html.twig passed them to the cardiogram include as an inline literal, not
// through DashboardController's view-model, so they stay a literal here too (see
// DashboardFixtures's class comment) — never translated, exactly like the old template.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Avatar from "@/components/atoms/Avatar.vue";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Card from "@/components/molecules/Card.vue";
import Cardiogram, {
    type CardiogramLegendItem,
    type CardiogramRange,
} from "@/components/organisms/Cardiogram.vue";
import EventStream from "@/components/organisms/EventStream.vue";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";
import ListCard from "@/components/organisms/ListCard.vue";
import PageHead from "@/components/organisms/PageHead.vue";
import type { StreamEvent } from "@/composables/useEventStream";

interface DashboardCustomer {
    id: string;
    name: string;
    email: string;
    orders: number;
    lastSeen: string;
}

interface DashboardAutomation {
    id: string;
    name: string;
    channels: string[];
    runs: string;
    conversion: string;
    revenue: string;
}

interface DashboardPayload {
    kpis: KpiTileData[];
    legend: CardiogramLegendItem[];
    events: StreamEvent[];
    mercureTopic: string;
    recentCustomers: DashboardCustomer[];
    topAutomations: DashboardAutomation[];
}

const CARDIOGRAM_RANGES: CardiogramRange[] = [
    { label: "5 min", active: true },
    { label: "1 godz." },
    { label: "24 godz." },
];

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const eventsHref = computed(() => `/${locale.value}/events`);
const customersHref = computed(() => `/${locale.value}/customers`);
const automationsHref = computed(() => `/${locale.value}/automations`);
const newAutomationHref = computed(() => `/${locale.value}/automations/new`);

const payload = ref<DashboardPayload | null>(null);

const customerRows = computed(() =>
    (payload.value?.recentCustomers ?? []).map((customer) => ({
        ...customer,
        href: `/${locale.value}/customers/${customer.id}`,
        columns: "auto 1fr auto auto",
    })),
);

const automationRows = computed(() =>
    (payload.value?.topAutomations ?? []).map((automation) => ({
        ...automation,
        href: `/${locale.value}/automations/${automation.id}`,
        columns: "1fr auto auto auto",
    })),
);

async function load(): Promise<void> {
    const response = await fetch(`/api/v1/${locale.value}/dashboard`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as DashboardPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('dashboard.title')" :sub="t('dashboard.sub')">
            <template #actions>
                <Chip :label="t('dashboard.live')" tone="good" live />
                <Button
                    size="sm"
                    icon="download"
                    :label="t('dashboard.export')"
                    action="export"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="plus"
                    :label="t('dashboard.newAutomation')"
                    :href="newAutomationHref"
                />
            </template>
        </PageHead>

        <KpiGrid :tiles="payload.kpis" />

        <div style="margin-bottom: 20px">
            <Cardiogram
                id="cg-main"
                :title="t('dashboard.cardiogram')"
                :sub="t('dashboard.cardiogramSub')"
                :ranges="CARDIOGRAM_RANGES"
                :legend="payload.legend"
            />
        </div>

        <div class="dash-grid">
            <Card
                :title="t('dashboard.liveFeed')"
                :sub="t('dashboard.liveFeedSub')"
                live
            >
                <template #headActions>
                    <Button
                        variant="ghost"
                        size="sm"
                        icon="pause"
                        :label="t('dashboard.pause')"
                        action="pause-stream"
                    />
                    <Button
                        variant="ghost"
                        size="sm"
                        :label="t('dashboard.fullLog')"
                        :href="eventsHref"
                    />
                </template>
                <template #raw>
                    <EventStream
                        :events="payload.events"
                        :topic="payload.mercureTopic"
                    />
                </template>
            </Card>

            <Card :title="t('dashboard.recentCustomers')">
                <template #headActions>
                    <Button
                        variant="ghost"
                        size="sm"
                        :label="t('dashboard.allCustomers')"
                        :href="customersHref"
                    />
                </template>
                <template #raw>
                    <ListCard :rows="customerRows">
                        <template #default="{ row }">
                            <Avatar :name="row.name" :size="28" />
                            <div style="min-width: 0">
                                <div style="font-weight: 500; font-size: 13px">
                                    {{ row.name }}
                                </div>
                                <div class="muted mono" style="font-size: 11px">
                                    {{ row.email }}
                                </div>
                            </div>
                            <div class="mono" style="font-size: 12px">
                                {{ row.orders }} {{ t("dashboard.orders") }}
                            </div>
                            <div class="muted" style="font-size: 11px">
                                {{ row.lastSeen }}
                            </div>
                        </template>
                    </ListCard>
                </template>
            </Card>
        </div>

        <div style="margin-top: 20px">
            <Card
                :title="t('dashboard.topRules')"
                :sub="t('dashboard.topRulesSub')"
            >
                <template #headActions>
                    <Button
                        variant="ghost"
                        size="sm"
                        :label="t('dashboard.allAutomations')"
                        :href="automationsHref"
                    />
                </template>
                <template #raw>
                    <ListCard :rows="automationRows">
                        <template #default="{ row }">
                            <div>
                                <div
                                    style="font-weight: 500; font-size: 13.5px"
                                >
                                    {{ row.name }}
                                </div>
                                <div
                                    class="muted"
                                    style="font-size: 11.5px; margin-top: 2px"
                                >
                                    <template
                                        v-for="(channel, index) in row.channels"
                                        :key="channel"
                                        ><template v-if="index > 0">{{
                                            " "
                                        }}</template
                                        ><Chip :label="channel"
                                    /></template>
                                </div>
                            </div>
                            <div class="mono right" style="min-width: 80px">
                                <div style="font-weight: 500">
                                    {{ row.runs }}
                                </div>
                                <div class="muted" style="font-size: 10.5px">
                                    {{ t("dashboard.runs") }}
                                </div>
                            </div>
                            <div class="mono right" style="min-width: 80px">
                                <div
                                    style="font-weight: 500; color: var(--good)"
                                >
                                    {{ row.conversion }}
                                </div>
                                <div class="muted" style="font-size: 10.5px">
                                    {{ t("dashboard.conversion") }}
                                </div>
                            </div>
                            <div class="mono right" style="min-width: 100px">
                                <div style="font-weight: 500">
                                    {{ row.revenue }}
                                </div>
                                <div class="muted" style="font-size: 10.5px">
                                    {{ t("dashboard.revenue") }}
                                </div>
                            </div>
                        </template>
                    </ListCard>
                </template>
            </Card>
        </div>
    </div>
</template>
