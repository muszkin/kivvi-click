<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";

/**
 * Where a confirmation link lands (PIO-71).
 *
 * The server has already done the work by the time this renders — following the link is a GET that
 * confirms — and hands the result down on <html> as data-waitlist-confirm, the same way a refused
 * login and a refused signup hand theirs down. There is no fetch here and deliberately no state
 * machine: the page reports one outcome that was decided before the document was written.
 *
 * "sent" is the exception, and it comes from the route rather than the attribute: it is the page
 * the resend redirects to (post/redirect/get), so a reload cannot queue a second message.
 */
type ConfirmState = "ok" | "already" | "expired" | "unknown" | "sent";

const STATES: readonly ConfirmState[] = [
    "ok",
    "already",
    "expired",
    "unknown",
    "sent",
];

const ICONS: Record<ConfirmState, string> = {
    ok: "check",
    already: "check",
    expired: "info",
    unknown: "info",
    sent: "mail",
};

const { t } = useI18n();
const route = useRoute();

const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const homeHref = computed(() => `/${locale.value}`);
const resendAction = computed(() => `/${locale.value}/waitlist/confirm/resend`);

const dataset = document.documentElement.dataset;
// An attribute this page does not recognise is treated as an unknown link rather than rendered
// blank: a document with nothing on it tells the visitor less than the wrong-but-honest message.
const state = computed<ConfirmState>(() => {
    if (route.name === "waitlist-confirm-sent") {
        return "sent";
    }
    const reported = dataset.waitlistConfirm;
    return STATES.includes(reported as ConfirmState)
        ? (reported as ConfirmState)
        : "unknown";
});
const token = dataset.waitlistToken ?? "";
</script>

<template>
    <section class="confirm-page" :data-state="state">
        <div class="confirm-mark" :class="`is-${state}`">
            <Icon :name="ICONS[state]" :size="26" />
        </div>
        <h1>{{ t(`waitlistPage.confirm.${state}.title`) }}</h1>
        <p class="lead">{{ t(`waitlistPage.confirm.${state}.body`) }}</p>

        <form
            v-if="state === 'expired'"
            class="confirm-actions"
            method="post"
            :action="resendAction"
        >
            <!-- The expired token is what identifies the address, so the visitor never has to
                 remember which one they used. It is spent either way: the server issues a new
                 one and this one stops working. -->
            <input type="hidden" name="token" :value="token" />
            <Button
                variant="primary"
                size="lg"
                type="submit"
                :label="t('waitlistPage.confirm.expired.action')"
            />
        </form>
        <div v-else class="confirm-actions">
            <Button
                size="lg"
                :href="homeHref"
                :label="t('waitlistPage.backHome')"
            />
        </div>
    </section>
</template>
