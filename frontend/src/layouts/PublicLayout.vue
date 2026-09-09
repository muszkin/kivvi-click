<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Wordmark from "@/components/atoms/Wordmark.vue";

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const loginHref = computed(() => `/${locale.value}/login`);
const year = new Date().getFullYear();
</script>

<template>
    <div class="landing-wrap">
        <header class="landing-nav">
            <Wordmark :size="22" :font-size="19" />
            <nav class="nav-links">
                <a href="#features">{{ t("landing.features") }}</a>
                <a href="#how">{{ t("landing.how") }}</a>
                <a href="#pricing">{{ t("landing.pricing") }}</a>
                <a href="#docs">{{ t("common.documentation") }}</a>
                <a href="#blog">{{ t("landing.blog") }}</a>
            </nav>
            <div class="spacer"></div>
            <Button
                variant="ghost"
                size="sm"
                :label="t('landing.login')"
                :href="loginHref"
            />
            <Button
                variant="primary"
                size="sm"
                :label="t('landing.register')"
                :href="loginHref"
            />
        </header>

        <slot />

        <footer class="landing-foot">
            <Wordmark :size="16" :font-size="14" />
            <span>·</span><span>© {{ year }}</span
            ><span>·</span> <a href="#">{{ t("landing.terms") }}</a
            ><a href="#">{{ t("landing.privacy") }}</a
            ><a href="#">RODO / DPA</a><a href="#">{{ t("common.status") }}</a>
            <div style="flex: 1"></div>
            <span class="mono" style="font-size: 11px"
                >Zbudowane w Polsce z Symfony, PHP i sporą ilością herbaty</span
            >
        </footer>
    </div>
</template>
