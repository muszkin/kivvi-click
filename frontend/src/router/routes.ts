import type { RouteRecordRaw } from "vue-router";
import AppLayout from "@/layouts/AppLayout.vue";
import AuthLayout from "@/layouts/AuthLayout.vue";
import PublicLayout from "@/layouts/PublicLayout.vue";
import AutomationEditorView from "@/views/AutomationEditorView.vue";
import AutomationsView from "@/views/AutomationsView.vue";
import CampaignsView from "@/views/CampaignsView.vue";
import CustomerView from "@/views/CustomerView.vue";
import CustomersView from "@/views/CustomersView.vue";
import DashboardView from "@/views/DashboardView.vue";
import EmailEditorView from "@/views/EmailEditorView.vue";
import EventsView from "@/views/EventsView.vue";
import FeedsView from "@/views/FeedsView.vue";
import ImportView from "@/views/ImportView.vue";
import LandingView from "@/views/LandingView.vue";
import LoginView from "@/views/LoginView.vue";
import PopupEditorView from "@/views/PopupEditorView.vue";
import PopupsView from "@/views/PopupsView.vue";
import PrivacyView from "@/views/PrivacyView.vue";
import SettingsView from "@/views/SettingsView.vue";
import WaitlistConfirmView from "@/views/WaitlistConfirmView.vue";
import WaitlistUnsubscribeView from "@/views/WaitlistUnsubscribeView.vue";

const LOCALE = "pl|en";

/**
 * The full panel/public route table, ported from inventory/routes.json (the 27 Symfony
 * routes minus /_storybook, /collect, /preferences/* and /import/upload). Every route now
 * has its own journey-built view (repair-2: the last two, "popups" and "import", replaced
 * the wave-0 EmptyPageView placeholder, which is why that component and its import were
 * removed here). Navigation between these routes is always a real document request (see
 * useIntents' "navigate" intent), never router.push — the oracle records a full GET for
 * every sidebar/topbar transition, so vue-router here only matches the URL the browser
 * already navigated to.
 */
export const routes: RouteRecordRaw[] = [
    {
        path: `/:locale(${LOCALE})?`,
        name: "home",
        component: LandingView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        // PIO-70. The waitlist form posts here and, when the submission is refused, the server
        // answers with the landing document rather than a redirect — which leaves the browser
        // sitting on this URL. Without a route for it the SPA would have nothing to render and
        // the message would never reach the visitor. It renders the landing page, exactly as
        // /{locale}/login renders LoginView after its own failed POST.
        path: `/:locale(${LOCALE})/waitlist`,
        name: "waitlist",
        component: LandingView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        // PIO-71. The three pages a confirmation mail leads to. The token pattern is the token's
        // own shape, mirroring domain/RouteTable.java: a truncated link is a 404 rather than a
        // page that says it has never heard of a link nobody ever issued.
        path: `/:locale(${LOCALE})/waitlist/confirm/sent`,
        name: "waitlist-confirm-sent",
        component: WaitlistConfirmView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        path: `/:locale(${LOCALE})/waitlist/confirm/:token([0-9a-f]{64})`,
        name: "waitlist-confirm",
        component: WaitlistConfirmView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        path: `/:locale(${LOCALE})/waitlist/unsubscribe/:token([0-9a-f]{64})`,
        name: "waitlist-unsubscribe",
        component: WaitlistUnsubscribeView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        path: `/:locale(${LOCALE})/privacy`,
        name: "privacy",
        component: PrivacyView,
        meta: { layout: PublicLayout, section: "home" },
    },
    {
        path: `/:locale(${LOCALE})/login`,
        name: "login",
        component: LoginView,
        meta: { layout: AuthLayout, section: "login" },
    },
    {
        path: `/:locale(${LOCALE})/dashboard`,
        name: "dashboard",
        component: DashboardView,
        meta: { layout: AppLayout, section: "dashboard" },
    },
    {
        path: `/:locale(${LOCALE})/events`,
        name: "events",
        component: EventsView,
        meta: { layout: AppLayout, section: "events" },
    },
    {
        path: `/:locale(${LOCALE})/customers`,
        name: "customers",
        component: CustomersView,
        meta: { layout: AppLayout, section: "customers" },
    },
    {
        path: `/:locale(${LOCALE})/customers/:id(c_\\d+)`,
        name: "customer_show",
        component: CustomerView,
        meta: { layout: AppLayout, section: "customers" },
    },
    {
        path: `/:locale(${LOCALE})/automations`,
        name: "automations",
        component: AutomationsView,
        meta: { layout: AppLayout, section: "automations" },
    },
    {
        path: `/:locale(${LOCALE})/automations/new`,
        name: "automation_new",
        component: AutomationEditorView,
        meta: { layout: AppLayout, section: "automations" },
    },
    {
        path: `/:locale(${LOCALE})/automations/:id(a\\d+)`,
        name: "automation_edit",
        component: AutomationEditorView,
        meta: { layout: AppLayout, section: "automations" },
    },
    {
        path: `/:locale(${LOCALE})/campaigns`,
        name: "campaigns",
        component: CampaignsView,
        meta: { layout: AppLayout, section: "campaigns" },
    },
    {
        path: `/:locale(${LOCALE})/emails/new`,
        name: "email_new",
        component: EmailEditorView,
        meta: { layout: AppLayout, section: "campaigns" },
    },
    {
        path: `/:locale(${LOCALE})/emails/:id(k\\d+)`,
        name: "email_edit",
        component: EmailEditorView,
        meta: { layout: AppLayout, section: "campaigns" },
    },
    {
        path: `/:locale(${LOCALE})/popups`,
        name: "popups",
        component: PopupsView,
        meta: { layout: AppLayout, section: "popups" },
    },
    {
        path: `/:locale(${LOCALE})/popups/new`,
        name: "popup_new",
        component: PopupEditorView,
        meta: { layout: AppLayout, section: "popups" },
    },
    {
        path: `/:locale(${LOCALE})/popups/:id(p\\d+)`,
        name: "popup_edit",
        component: PopupEditorView,
        meta: { layout: AppLayout, section: "popups" },
    },
    {
        path: `/:locale(${LOCALE})/feeds`,
        name: "feeds",
        component: FeedsView,
        meta: { layout: AppLayout, section: "feeds" },
    },
    {
        path: `/:locale(${LOCALE})/import/:step(1|2|3|4)?`,
        name: "import",
        component: ImportView,
        // defaultParams: mirrors ImportController's `defaults: ['step' => ImportWizard::FIRST_STEP]`
        // — see settings' own defaultParams comment above and router/localeHref.ts for how this
        // generically drops the segment from the locale-toggle link, the way Symfony's own URL
        // generator does (oracle: journeys/import-wizard/steps/1/a11y.json's "PL" link targets
        // "/en/import", never "/en/import/1").
        meta: {
            layout: AppLayout,
            section: "import",
            defaultParams: { step: "1" },
        },
    },
    {
        path: `/:locale(${LOCALE})/settings/:tab?`,
        name: "settings",
        component: SettingsView,
        // defaultParams: mirrors SettingsController's `defaults: ['tab' => 'account']` — see
        // meta.d.ts and router/localeHref.ts for how this generically drops the segment from
        // the locale-toggle link, the way Symfony's own URL generator does.
        meta: {
            layout: AppLayout,
            section: "settings",
            defaultParams: { tab: "account" },
        },
    },
];
