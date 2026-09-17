<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";
import { routeLocale } from "@/router/routeLocale";

/**
 * Where the "unsubscribe" link in a message's footer lands (PIO-71).
 *
 * Two states, and no login in either: an unsubscribe link that asks for a password is an
 * unsubscribe link that does not work. As on the confirmation page, the server has already acted
 * by the time this renders and reports the result on <html>.
 */
type UnsubscribeState = "ok" | "unknown";

const { t } = useI18n();
const route = useRoute();

const locale = computed(() => routeLocale(route));
const homeHref = computed(() => `/${locale.value}`);

const state: UnsubscribeState =
    document.documentElement.dataset.waitlistUnsubscribe === "ok"
        ? "ok"
        : "unknown";
</script>

<template>
    <section class="confirm-page" :data-state="state">
        <div class="confirm-mark" :class="`is-${state}`">
            <Icon :name="state === 'ok' ? 'check' : 'info'" :size="26" />
        </div>
        <h1>{{ t(`waitlistPage.unsubscribe.${state}.title`) }}</h1>
        <p class="lead">{{ t(`waitlistPage.unsubscribe.${state}.body`) }}</p>

        <div class="confirm-actions">
            <Button
                size="lg"
                :href="homeHref"
                :label="t('waitlistPage.backHome')"
            />
        </div>
    </section>
</template>
