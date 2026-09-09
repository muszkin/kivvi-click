<script setup lang="ts">
// PAGE · Profil klienta (360) — ported from pages/customer.html.twig (+ its
// pages/customers/{timeline,active-automations}.html.twig partials, inlined here: both are
// page-specific partials in the old stack, not reusable design-system components).
//
// Data arrives from GET /api/v1/{locale}/customers/{id}, pre-formatted by CustomersViewService —
// this view never formats a number or a relative time itself. The document route itself 404s for
// an unknown id server-side (see CustomersController), so this view only ever mounts for a real
// customer — no "not found" branch needed here.
//
// The back link is built inline rather than through PageHead (which has no "back" slot yet) so
// its exact markup — `.page-head a.btn.ghost` — matches templates/components/organisms/
// page-head.html.twig's own `back` param 1:1.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Dot from "@/components/atoms/Dot.vue";
import Card from "@/components/molecules/Card.vue";
import ProfileFact from "@/components/molecules/ProfileFact.vue";
import Tabs, { type TabItem } from "@/components/molecules/Tabs.vue";
import TimelineItem from "@/components/molecules/TimelineItem.vue";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";

interface CustomerTag {
    label: string;
    tone: "accent" | "brown" | null;
}

interface CustomerFact {
    label: string;
    value: string;
    small: boolean;
}

interface ActiveAutomation {
    name: string;
}

interface TimelineEntry {
    time: string;
    title: string;
    detail?: string | null;
    icon: string;
}

interface CustomerDetailPayload {
    customer: {
        id: string;
        name: string;
        initials: string;
        email: string;
        tags: CustomerTag[];
    };
    profileSub: string;
    facts: CustomerFact[];
    automations: ActiveAutomation[];
    tabs: TabItem[];
    scores: KpiTileData[];
    timeline: TimelineEntry[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const customerId = computed(() =>
    typeof route.params.id === "string" ? route.params.id : "",
);
const backHref = computed(() => `/${locale.value}/customers`);

const payload = ref<CustomerDetailPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/customers/${customerId.value}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as CustomerDetailPayload;
}

onMounted(load);
</script>

<template>
    <div v-if="payload" class="page">
        <div class="page-head">
            <div>
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('customers.backToList')"
                    :href="backHref"
                    style="margin-bottom: 6px"
                />
                <h1 class="page-title">{{ payload.customer.name }}</h1>
                <p class="page-sub" v-html="payload.profileSub"></p>
            </div>
            <div class="page-actions">
                <Button
                    size="sm"
                    icon="mail"
                    :label="t('customers.sendEmail')"
                    action="send-email"
                    :payload="payload.customer.id"
                />
                <Button
                    size="sm"
                    icon="coupon"
                    :label="t('customers.grantCoupon')"
                    action="grant-coupon"
                    :payload="payload.customer.id"
                />
                <Button
                    size="sm"
                    icon="bookmark"
                    :label="t('customers.addToSegment')"
                    action="add-to-segment"
                    :payload="payload.customer.id"
                />
            </div>
        </div>

        <div class="customer-grid">
            <div class="col" style="gap: 16px">
                <div class="profile-card">
                    <div class="profile-avatar">
                        {{ payload.customer.initials }}
                    </div>
                    <h2 class="profile-name">{{ payload.customer.name }}</h2>
                    <p
                        class="muted"
                        style="margin: 4px 0 14px; font-size: 13px"
                    >
                        {{ payload.customer.email }}
                    </p>
                    <div class="row" style="gap: 4px">
                        <Chip
                            v-for="(tag, index) in payload.customer.tags"
                            :key="index"
                            :label="tag.label"
                            :tone="tag.tone ?? undefined"
                        />
                    </div>
                    <div style="margin-top: 18px">
                        <ProfileFact
                            v-for="(fact, index) in payload.facts"
                            :key="index"
                            :label="fact.label"
                            :value="fact.value"
                            :small="fact.small"
                        />
                    </div>
                </div>

                <Card :title="t('customers.activeAutomations')">
                    <template #raw>
                        <div style="padding: 8px 0">
                            <div
                                v-for="(
                                    automation, index
                                ) in payload.automations"
                                :key="index"
                                class="row"
                                style="
                                    padding: 8px 14px;
                                    border-bottom: 1px solid var(--line);
                                    font-size: 13px;
                                "
                            >
                                <Dot color="var(--good)" />
                                <span>{{ automation.name }}</span>
                            </div>
                        </div>
                    </template>
                </Card>
            </div>

            <div class="col" style="gap: 16px">
                <Tabs :tabs="payload.tabs" />
                <KpiGrid :tiles="payload.scores" :columns="3" />
                <Card
                    :title="t('customers.timeline')"
                    icon="activity"
                    :sub="t('customers.last24h')"
                >
                    <template #headActions>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="filter"
                            :label="t('customers.filters')"
                            action="open-filters"
                        />
                    </template>
                    <!-- customer.html.twig passes the timeline through the card's "body" param
                         (padded card-body), not "raw" like the active-automations card above —
                         Card's default slot is that same padded body. -->
                    <div class="timeline">
                        <TimelineItem
                            v-for="(item, index) in payload.timeline"
                            :key="index"
                            :time="item.time"
                            :title="item.title"
                            :detail="item.detail ?? undefined"
                            :icon="item.icon"
                        />
                    </div>
                </Card>
            </div>
        </div>
    </div>
</template>
