<script setup lang="ts">
import { useI18n } from "vue-i18n";

// PIO-70 (decision D3): the consent box on the landing form links here, and the footer's
// "Prywatność" link now points here too instead of href="#". The copy is a static catalogue
// document, not view data from the API — nothing here depends on the session or the account,
// so it needs no fetch.
interface Section {
    heading: string;
    body: string;
}

const { t, tm, rt } = useI18n();

// tm() returns the raw catalogue node so the array of sections can be walked; rt() renders
// each leaf through the same message compiler t() would use.
const sections = tm("privacyPage.sections") as unknown as Section[];
</script>

<template>
    <section class="legal-page">
        <h1>{{ t("privacyPage.title") }}</h1>
        <p class="legal-updated">{{ t("privacyPage.updated") }}</p>
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
