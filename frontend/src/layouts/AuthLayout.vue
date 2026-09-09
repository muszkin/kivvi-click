<script setup lang="ts">
import { useI18n } from "vue-i18n";
import Wordmark from "@/components/atoms/Wordmark.vue";

const { t } = useI18n();
const year = new Date().getFullYear();
</script>

<template>
    <div class="auth-wrap">
        <div class="auth-side">
            <Wordmark :size="28" :font-size="24" />
            <div>
                <div
                    class="mono"
                    style="
                        font-size: 11px;
                        letter-spacing: 0.12em;
                        text-transform: uppercase;
                        color: var(--fg-muted);
                        margin-bottom: 12px;
                    "
                >
                    {{ t("common.marketingAutomation") }}
                </div>
                <h1
                    style="
                        font-family: var(--font-display);
                        font-style: italic;
                        font-size: 56px;
                        line-height: 1.12;
                        letter-spacing: -0.025em;
                        margin: 0;
                        padding-bottom: 0.15em;
                    "
                    v-html="t('auth.headline')"
                ></h1>
                <p
                    class="muted"
                    style="font-size: 16px; max-width: 360px; margin-top: 18px"
                >
                    {{ t("auth.lead") }}
                </p>
            </div>
            <div
                class="row"
                style="gap: 14px; font-size: 13px; color: var(--fg-muted)"
            >
                <span>© {{ year }} kivvi-click</span><span>·</span
                ><a href="#">{{ t("common.status") }}</a
                ><span>·</span><a href="#">{{ t("common.documentation") }}</a>
            </div>
        </div>
        <div class="auth-form">
            <div class="form">
                <slot />
            </div>
        </div>
    </div>
</template>
