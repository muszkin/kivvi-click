<script setup lang="ts">
// PAGE · Ustawienia — ported from pages/settings.html.twig. Eight tabs, each its own URL
// (/settings/{tab}); the bare /settings route resolves to the "account" default tab client-side,
// exactly like the old stack's route default did server-side.
//
// Data arrives from GET /api/v1/{locale}/settings/{tab}, pre-formatted by SettingsViewService —
// this view (and every tab component under components/settings/) never formats a number itself.
// Every tab component only receives the specific fields it declares as props (never the whole
// payload via a blanket v-bind spread): each tab renders several sibling <Card> root nodes, and
// Vue disables automatic attrs fallthrough for multi-root components, so an unclaimed prop would
// surface only as a console warning — which the frontend's fail-on-warning test policy
// (test/setup.ts) turns into a hard test failure.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import AccountTab from "@/components/settings/AccountTab.vue";
import ApiTab from "@/components/settings/ApiTab.vue";
import BillingTab from "@/components/settings/BillingTab.vue";
import GdprTab from "@/components/settings/GdprTab.vue";
import NotificationsTab from "@/components/settings/NotificationsTab.vue";
import ProvidersTab from "@/components/settings/ProvidersTab.vue";
import SitesTab from "@/components/settings/SitesTab.vue";
import TeamTab from "@/components/settings/TeamTab.vue";
import type { SettingsData } from "@/components/settings/types";
import SettingsNav, {
    type SettingsNavTab,
} from "@/components/organisms/SettingsNav.vue";
import PageHead from "@/components/organisms/PageHead.vue";

interface SettingsPayload {
    tab: string;
    tabs: SettingsNavTab[];
    tabSubtitle: string;
    settings: SettingsData;
    trackerSnippet: string;
}

const DEFAULT_TAB = "account";

const TAB_COMPONENTS: Record<string, unknown> = {
    account: AccountTab,
    sites: SitesTab,
    team: TeamTab,
    providers: ProvidersTab,
    api: ApiTab,
    notifications: NotificationsTab,
    billing: BillingTab,
    gdpr: GdprTab,
};

const { t } = useI18n();
const route = useRoute();

const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const tab = computed(() =>
    typeof route.params.tab === "string" ? route.params.tab : DEFAULT_TAB,
);

const payload = ref<SettingsPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/settings/${tab.value}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as SettingsPayload;
}

onMounted(load);

const currentTabComponent = computed(() =>
    payload.value ? TAB_COMPONENTS[payload.value.tab] : null,
);

const tabProps = computed((): Record<string, unknown> => {
    if (!payload.value) {
        return {};
    }
    const s = payload.value.settings;
    switch (payload.value.tab) {
        case "sites":
            return {
                trackedSites: s.trackedSites,
                automaticEvents: s.automaticEvents,
                trackerSnippet: payload.value.trackerSnippet,
            };
        case "team":
            return { team: s.team, roles: s.roles };
        case "providers":
            return {
                emailProviders: s.emailProviders,
                dnsRecords: s.dnsRecords,
            };
        case "api":
            return {
                apiKeys: s.apiKeys,
                webhooks: s.webhooks,
                apiLimits: s.apiLimits,
            };
        case "notifications":
            return { notificationMatrix: s.notificationMatrix };
        case "billing":
            return { planUsage: s.planUsage, invoices: s.invoices };
        case "gdpr":
            return {
                retentionPolicies: s.retentionPolicies,
                dataSubjectRequests: s.dataSubjectRequests,
            };
        default:
            return {};
    }
});
</script>

<template>
    <div v-if="payload" class="page">
        <PageHead :title="t('settings.title')" :sub="payload.tabSubtitle" />

        <div class="settings-grid">
            <SettingsNav :tabs="payload.tabs" />
            <div class="col" style="gap: 16px">
                <component :is="currentTabComponent" v-bind="tabProps" />
            </div>
        </div>
    </div>
</template>
