<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import Icon from "@/components/atoms/Icon.vue";
import Kbd from "@/components/atoms/Kbd.vue";
import { buildLocaleHref } from "@/router/localeHref";

const props = defineProps<{
    siteName: string;
    crumb: string;
    locale: string;
    theme: string;
}>();

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

// The theme button's data-payload is computed once, from the theme this component mounted
// with — mirroring the Twig button's server-rendered payload, which only changes on the
// next full page load, never reactively after a click (see useIntents' set-theme intent).
const initialTheme = props.theme;
const themePayload = initialTheme === "light" ? "dark" : "light";
const themeIcon = initialTheme === "light" ? "moon" : "sun";

const otherLocale = computed(() => (props.locale === "pl" ? "en" : "pl"));
// Generic port of Symfony's path($route, $routeParams) — see router/localeHref.ts. Repair-1
// (R1-B): replaces a settings-only special case with a route-agnostic helper driven by each
// route's own meta.defaultParams, so a later journey with the same shape (e.g. import-wizard's
// `defaults: ['step' => ImportWizard::FIRST_STEP]`) only adds a routes.ts meta entry, never a
// second Topbar branch.
const localeHref = computed(() =>
    buildLocaleHref(router, route, otherLocale.value),
);
</script>

<template>
    <header class="topbar">
        <button
            class="btn ghost sm"
            data-action="toggle-sidebar"
            :title="t('common.collapseSidebar')"
            :aria-label="t('common.collapseSidebar')"
        >
            <Icon name="sidebar" />
        </button>

        <nav class="crumbs" :aria-label="t('common.breadcrumb')">
            <span>{{ siteName }}</span>
            <span class="sep">/</span>
            <span class="now">{{ crumb }}</span>
        </nav>

        <div class="topbar-spacer"></div>

        <button
            class="kbar"
            data-action="open-command-bar"
            :aria-label="t('common.openSearch')"
        >
            <Icon name="search" />
            <span>{{ t("common.search") }}</span>
            <Kbd keys="⌘K" />
        </button>

        <a class="tb-btn" :href="localeHref" :title="t('common.changeLanguage')"
            ><span class="mono" style="font-size: 11px">{{
                locale.toUpperCase()
            }}</span></a
        >

        <button
            class="tb-btn"
            data-action="set-theme"
            :data-payload="themePayload"
            :title="t('common.theme')"
        >
            <Icon :name="themeIcon" />
        </button>

        <button
            class="tb-btn"
            data-action="open-notifications"
            :title="t('common.notifications')"
        >
            <Icon name="bell" />
        </button>
    </header>
</template>
