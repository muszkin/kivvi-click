<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";
import Feat from "@/components/molecules/Feat.vue";
import PriceCard from "@/components/molecules/PriceCard.vue";
import HeroPreview from "@/components/organisms/HeroPreview.vue";

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

interface Plan {
    tier: string;
    price: string;
    unit: string;
    items: string[];
    cta: string;
    featured: boolean;
    badge?: string;
}

interface PreviewTile {
    label: string;
    value: string;
    unit?: string;
}

interface LandingData {
    features: Feature[];
    steps: Step[];
    plans: Plan[];
    trustPoints: string[];
    previewTiles: PreviewTile[];
    previewSeries: number[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const loginHref = computed(() => `/${locale.value}/login`);
const demoHref = computed(() => `/${locale.value}/demo`);

// The Twig template's <title> block reads `landing.kicker` too, so this doubles as the page
// title's suffix — set once here rather than in a shared router hook, since every other route
// in this slice keeps the SPA document's static default ("Kivvi-click", set in index.html).
document.title = `kivvi·click — ${t("landingPage.kicker")}`;

const landing = ref<LandingData | null>(null);

onMounted(async () => {
    const response = await fetch(`/api/v1/${locale.value}/landing`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    landing.value = (await response.json()) as LandingData;
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
            <div class="hero-cta">
                <Button
                    variant="primary"
                    size="lg"
                    :label="t('landingPage.ctaPrimary')"
                    :href="loginHref"
                />
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
                    grid-template-columns: repeat(3, minmax(0, 1fr));
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
            id="pricing"
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
                {{ t("landing.pricing") }}
            </div>
            <h2 v-html="t('landingPage.pricingTitle')"></h2>
            <p class="lead">{{ t("landingPage.pricingLead") }}</p>
            <div class="pricing-grid">
                <PriceCard
                    v-for="plan in landing.plans"
                    :key="plan.tier"
                    :tier="plan.tier"
                    :price="plan.price"
                    :unit="plan.unit"
                    :items="plan.items"
                    :cta="plan.cta"
                    :featured="plan.featured"
                    :badge="plan.badge"
                    :href="loginHref"
                />
            </div>
        </section>
    </template>
</template>
