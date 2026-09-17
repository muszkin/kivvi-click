<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute, useRouter } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Wordmark from "@/components/atoms/Wordmark.vue";
import { buildLocaleHref } from "@/router/localeHref";
import { otherLocale, routeLocale } from "@/router/routeLocale";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const locale = computed(() => routeLocale(route));
const loginHref = computed(() => `/${locale.value}/login`);
// PIO-70 (decision D3): the footer's privacy link was href="#" until there was a page to point
// it at. The three siblings that are still "#" — terms, DPA, status — are out of scope here.
const privacyHref = computed(() => `/${locale.value}/privacy`);
// PIO-125: once "/" became English, a Polish visitor landed on a page in a language they may not
// read, with nothing on it leading to their own — the panel's topbar had a switch, the public
// pages never did. This is the same page in the other language, built the way the topbar builds
// its own link.
const switchLocale = computed(() => otherLocale(locale.value));
const switchHref = computed(() =>
    buildLocaleHref(router, route, switchLocale.value),
);
// PIO-121: the footer names the stack and links the source, and the header's primary button now
// points here too — it used to offer "Załóż konto →" and lead to the login form, because there is
// no registration behind it. Constant, not locale-dependent.
const repoHref = "https://github.com/muszkin/kivvi-click";
const year = new Date().getFullYear();
</script>

<template>
    <div class="landing-wrap">
        <header class="landing-nav">
            <Wordmark :size="22" :font-size="19" />
            <nav class="nav-links">
                <a href="#features">{{ t("landing.features") }}</a>
                <a href="#how">{{ t("landing.how") }}</a>
                <a href="#open-source">{{ t("landing.openSource") }}</a>
                <a href="#docs">{{ t("common.documentation") }}</a>
                <a href="#blog">{{ t("landing.blog") }}</a>
            </nav>
            <div class="spacer"></div>
            <Button
                class="landing-nav-locale"
                variant="ghost"
                size="sm"
                :label="t('landing.otherLanguage')"
                :href="switchHref"
                :title="t('common.changeLanguage')"
                :hreflang="switchLocale"
                :lang="switchLocale"
            />
            <Button
                variant="ghost"
                size="sm"
                :label="t('landing.login')"
                :href="loginHref"
            />
            <Button
                class="landing-nav-repo"
                variant="primary"
                size="sm"
                :label="t('landing.register')"
                :href="repoHref"
                target="_blank"
                rel="noopener noreferrer"
            />
        </header>

        <slot />

        <footer class="landing-foot">
            <Wordmark :size="16" :font-size="14" />
            <span>·</span><span>© {{ year }}</span
            ><span>·</span> <a href="#">{{ t("landing.terms") }}</a
            ><a :href="privacyHref">{{ t("landing.privacy") }}</a
            ><a href="#">{{ t("landing.dpa") }}</a
            ><a href="#">{{ t("common.status") }}</a>
            <div style="flex: 1"></div>
            <span class="mono" style="font-size: 11px">
                {{ t("landing.builtWith") }}
                <a
                    class="landing-foot-repo"
                    :href="repoHref"
                    target="_blank"
                    rel="noopener noreferrer"
                    >{{ t("landing.repo") }}</a
                >
            </span>
        </footer>
    </div>
</template>
