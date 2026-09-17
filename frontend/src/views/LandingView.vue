<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Field from "@/components/atoms/Field.vue";
import Icon from "@/components/atoms/Icon.vue";
import Feat from "@/components/molecules/Feat.vue";
import HeroPreview from "@/components/organisms/HeroPreview.vue";
import { routeLocale } from "@/router/routeLocale";
import { WAITLIST_FORM_ANCHOR } from "@/router/waitlistForm";

interface Feature {
    icon: string;
    title: string;
    body: string;
}

interface Step {
    number: string;
    title: string;
    body: string;
}

interface PreviewTile {
    label: string;
    value: string;
    unit?: string;
}

interface LandingData {
    features: Feature[];
    steps: Step[];
    trustPoints: string[];
    previewTiles: PreviewTile[];
    previewSeries: number[];
}

const { t } = useI18n();
const route = useRoute();
// PIO-125: "/" has no locale segment and is the English landing page now.
const locale = computed(() => routeLocale(route));
// PIO-121 replaced the price cards — the only readers of loginHref here — with the open-source
// section, which links the repository instead.
const repoHref = "https://github.com/muszkin/kivvi-click";
const demoHref = computed(() => `/${locale.value}/demo`);
const waitlistAction = computed(() => `/${locale.value}/waitlist`);
const privacyHref = computed(() => `/${locale.value}/privacy`);

// PIO-70. The waitlist form is a native document POST, not a fetch — the same pattern
// LoginView.vue uses. That means both of its outcomes arrive as a fresh document:
//   success  → the server redirects to /{locale}?waitlist=ok (post/redirect/get, so a reload
//              cannot submit twice), and the query is what swaps the form for the thank-you;
//   refusal  → the server re-renders this very document with data-waitlist-* on <html>,
//              carrying the message, the address as typed and whether the box was ticked, so
//              nothing the visitor entered is lost.
const dataset = document.documentElement.dataset;
const waitlistError = dataset.waitlistError ?? null;
const submittedEmail = dataset.waitlistEmail ?? "";
const submittedConsent = dataset.waitlistConsent === "true";
const signedUp = computed(() => route.query.waitlist === "ok");

// The Twig template's <title> block reads `landing.kicker` too, so this doubles as the page
// title's suffix — set once here rather than in a shared router hook, since every other route
// in this slice keeps the SPA document's static default ("Kivvi-click", set in index.html).
document.title = `kivvi·click — ${t("landingPage.kicker")}`;

const landing = ref<LandingData | null>(null);
const waitlistForm = ref<HTMLFormElement | null>(null);

/**
 * PIO-125: the login page's "No account?" line links to `/{locale}#waitlist`. The browser jumps to
 * a fragment while the document loads — before this page has fetched its content and rendered the
 * form — so it finds nothing to jump to, and the router's scrollBehavior then starts every fresh
 * navigation at the top. Once the form exists, bring it into view the way the jump would have.
 */
function revealLinkedForm(): void {
    if (route.hash !== `#${WAITLIST_FORM_ANCHOR}`) {
        return;
    }
    waitlistForm.value?.scrollIntoView({ block: "center" });
}

onMounted(async () => {
    const response = await fetch(`/api/v1/${locale.value}/landing`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    landing.value = (await response.json()) as LandingData;
    await nextTick();
    revealLinkedForm();
});
</script>

<template>
    <template v-if="landing">
        <section class="hero">
            <div
                class="mono"
                style="
                    font-size: 11px;
                    letter-spacing: 0.15em;
                    text-transform: uppercase;
                    color: var(--accent);
                    margin-bottom: 18px;
                "
            >
                <Icon name="activity" />{{ " " }}{{ t("landingPage.kicker") }}
            </div>
            <h1 v-html="t('landingPage.headline')"></h1>
            <p class="lead">{{ t("landingPage.lead") }}</p>
            <p v-if="signedUp" class="waitlist-thanks" role="status">
                <Icon name="check" />{{ " "
                }}{{ t("landingPage.waitlist.thanks") }}
            </p>
            <form
                v-else
                :id="WAITLIST_FORM_ANCHOR"
                ref="waitlistForm"
                class="waitlist-form"
                method="post"
                :action="waitlistAction"
            >
                <div class="waitlist-row">
                    <Field
                        name="email"
                        type="email"
                        required
                        autocomplete="email"
                        :label="t('landingPage.waitlist.label')"
                        :placeholder="t('landingPage.waitlist.placeholder')"
                        :value="submittedEmail"
                        :error="waitlistError ?? undefined"
                    />
                    <Button
                        variant="primary"
                        size="lg"
                        type="submit"
                        :label="t('landingPage.waitlist.submit')"
                    />
                </div>

                <label class="waitlist-consent">
                    <!-- Deliberately not `required`: the browser would then block the submit
                         and the server-side consent check - the one that actually decides
                         whether a row is written - would never be exercised in a real
                         browser. The e-mail field keeps `required` because its server-side
                         check is reachable by any other client anyway. -->
                    <input
                        type="checkbox"
                        name="consent"
                        value="1"
                        :checked="submittedConsent"
                    />
                    <span>
                        {{ t("landingPage.waitlist.consent") }}
                        <a :href="privacyHref">{{
                            t("landingPage.waitlist.privacyLink")
                        }}</a>
                    </span>
                </label>

                <!-- Honeypot: no human ever sees or tabs into this, so anything in it means a
                     script filled the form. aria-hidden keeps it out of the accessibility tree
                     as well, so a screen-reader user is not asked to skip a phantom field. -->
                <div class="visually-hidden" aria-hidden="true">
                    <label for="f-website">{{
                        t("landingPage.waitlist.honeypotLabel")
                    }}</label>
                    <input
                        id="f-website"
                        name="website"
                        type="text"
                        tabindex="-1"
                        autocomplete="off"
                    />
                </div>

                <p class="waitlist-note">
                    {{ t("landingPage.waitlist.note") }}
                </p>
            </form>

            <div class="hero-cta">
                <Button
                    size="lg"
                    :label="t('landingPage.ctaSecondary')"
                    :href="demoHref"
                />
            </div>
            <div
                class="row"
                style="
                    justify-content: center;
                    gap: 18px;
                    margin-top: 28px;
                    font-size: 12.5px;
                    color: var(--fg-muted);
                "
            >
                <span
                    class="row"
                    style="gap: 4px"
                    v-for="point in landing.trustPoints"
                    :key="point"
                    ><Icon name="check" />{{ " " }}{{ point }}</span
                >
            </div>
        </section>

        <HeroPreview
            :tiles="landing.previewTiles"
            :series="landing.previewSeries"
        />

        <section class="landing-section" id="features">
            <div
                class="mono"
                style="
                    font-size: 11px;
                    letter-spacing: 0.15em;
                    text-transform: uppercase;
                    color: var(--fg-muted);
                    margin-bottom: 8px;
                "
            >
                {{ t("landing.features") }}
            </div>
            <h2 v-html="t('landingPage.featuresTitle')"></h2>
            <p class="lead">{{ t("landingPage.featuresLead") }}</p>
            <div class="feat-grid">
                <Feat
                    v-for="feature in landing.features"
                    :key="feature.title"
                    :icon="feature.icon"
                    :title="feature.title"
                    :body="feature.body"
                />
            </div>
        </section>

        <section
            class="landing-section"
            id="how"
            style="border-top: 1px solid var(--line)"
        >
            <div
                class="mono"
                style="
                    font-size: 11px;
                    letter-spacing: 0.15em;
                    text-transform: uppercase;
                    color: var(--fg-muted);
                    margin-bottom: 8px;
                "
            >
                {{ t("landing.how") }}
            </div>
            <h2 v-html="t('landingPage.howTitle')"></h2>
            <div
                style="
                    display: grid;
                    grid-template-columns: repeat(4, minmax(0, 1fr));
                    gap: 18px;
                    margin-top: 36px;
                "
            >
                <div
                    class="feat"
                    v-for="step in landing.steps"
                    :key="step.number"
                >
                    <div
                        class="mono"
                        style="
                            font-size: 36px;
                            color: var(--accent);
                            font-weight: 500;
                            letter-spacing: -0.04em;
                        "
                    >
                        {{ step.number }}
                    </div>
                    <h3>{{ step.title }}</h3>
                    <p>{{ step.body }}</p>
                </div>
            </div>
        </section>

        <section
            class="landing-section"
            id="open-source"
            style="border-top: 1px solid var(--line)"
        >
            <div
                class="mono"
                style="
                    font-size: 11px;
                    letter-spacing: 0.15em;
                    text-transform: uppercase;
                    color: var(--fg-muted);
                    margin-bottom: 8px;
                "
            >
                {{ t("landing.openSource") }}
            </div>
            <h2 v-html="t('landingPage.openSourceTitle')"></h2>
            <p class="lead">{{ t("landingPage.openSourceLead") }}</p>
            <p class="open-source-repo">
                <a :href="repoHref" target="_blank" rel="noopener noreferrer">{{
                    t("landingPage.openSourceRepo")
                }}</a>
            </p>
        </section>
    </template>
</template>
