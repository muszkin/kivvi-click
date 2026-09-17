<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Icon from "@/components/atoms/Icon.vue";
import type { SupportedLocale } from "@/i18n";
import { routeLocale } from "@/router/routeLocale";

// PIO-70 (decision D3): the consent box on the landing form links here, and the footer's
// "Prywatność" link now points here too instead of href="#". The copy is a static catalogue
// document, not view data from the API — nothing here depends on the session or the account,
// so it needs no fetch.
interface Section {
    heading: string;
    body: string;
}

/**
 * The language the policy is written in. The controller is a Polish sole proprietorship answering
 * to the Polish supervisory authority, so the Polish text is the binding one whichever language the
 * interface defaults to (PIO-125); every other language is a translation, and says so.
 */
const BINDING_LOCALE: SupportedLocale = "pl";

const { t, tm, rt } = useI18n();
const route = useRoute();

const isTranslation = computed(() => routeLocale(route) !== BINDING_LOCALE);
const bindingHref = `/${BINDING_LOCALE}/privacy`;

// tm() returns the raw catalogue node so the array of sections can be walked; rt() renders
// each leaf through the same message compiler t() would use.
const sections = tm("privacyPage.sections") as unknown as Section[];
</script>

<template>
    <section class="legal-page">
        <h1>{{ t("privacyPage.title") }}</h1>
        <p class="legal-updated">{{ t("privacyPage.updated") }}</p>
        <p v-if="isTranslation" class="callout legal-translation" role="note">
            <Icon name="info" />
            <span>
                {{ t("privacyPage.translation.notice") }}
                <a :href="bindingHref" :hreflang="BINDING_LOCALE">{{
                    t("privacyPage.translation.link")
                }}</a>
            </span>
        </p>
        <p class="lead">{{ t("privacyPage.intro") }}</p>

        <section
            v-for="section in sections"
            :key="rt(section.heading)"
            class="legal-section"
        >
            <h2>{{ rt(section.heading) }}</h2>
            <p>{{ rt(section.body) }}</p>
        </section>
    </section>
</template>
