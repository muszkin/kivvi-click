<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Field from "@/components/atoms/Field.vue";
import Callout from "@/components/molecules/Callout.vue";
import { routeLocale } from "@/router/routeLocale";
import { waitlistFormHref } from "@/router/waitlistForm";

const { t } = useI18n();
const route = useRoute();
const locale = computed(() => routeLocale(route));
const formAction = computed(() => `/${locale.value}/login`);
// PIO-125: "No account?" used to link back to this very form, under a promise of registration
// that does not exist. It leads to the deployment conversation on the landing page instead.
const deploymentHref = computed(() => waitlistFormHref(locale.value));

// Mirrors pages/login.html.twig's `last_username|default('maciej@aureashop.pl')`: a fresh
// GET pre-fills the sample address; a failed POST re-renders the SPA document with
// data-last-username (present even when empty, for the "empty e-mail" case) carrying back
// exactly what was typed.
const DEFAULT_USERNAME = "maciej@aureashop.pl";
const dataset = document.documentElement.dataset;
const lastUsername =
    "lastUsername" in dataset ? (dataset.lastUsername ?? "") : DEFAULT_USERNAME;
const errorMessage = dataset.loginError ?? null;
</script>

<template>
    <h1>{{ t("auth.welcomeBack") }}</h1>
    <p class="sub">{{ t("auth.loginSub") }}</p>

    <form method="post" :action="formAction">
        <Field
            name="_username"
            type="email"
            :label="t('common.email')"
            :value="lastUsername"
        />
        <Field name="_password" type="password" :label="t('common.password')" />

        <Callout
            v-if="errorMessage"
            tone="bad"
            icon="info"
            :text="errorMessage"
        />

        <Button
            variant="primary"
            size="lg"
            :label="t('auth.signIn')"
            type="submit"
            style="width: 100%; justify-content: center; margin-top: 8px"
        />
    </form>

    <div class="row" style="margin: 18px 0; gap: 10px">
        <div style="flex: 1; height: 1px; background: var(--line)"></div>
        <span
            class="muted"
            style="
                font-size: 11px;
                text-transform: uppercase;
                letter-spacing: 0.08em;
            "
            >{{ t("common.or") }}</span
        >
        <div style="flex: 1; height: 1px; background: var(--line)"></div>
    </div>

    <Button
        size="lg"
        :label="t('common.continueWithGoogle')"
        action="sign-in-with-google"
        style="width: 100%; justify-content: center"
    />

    <p
        class="muted"
        style="font-size: 13px; margin-top: 24px; text-align: center"
    >
        {{ t("auth.noAccount") }}
        <a
            class="login-deployment-cta"
            :href="deploymentHref"
            style="color: var(--accent); font-weight: 500"
            >{{ t("auth.deploymentCta") }}</a
        >
    </p>
</template>
