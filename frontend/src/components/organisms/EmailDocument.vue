<script setup lang="ts">
// ORGANISM · EmailDocument — ported from components/organisms/email-document.html.twig. The
// 600px e-mail canvas. Deliberately uses hard-coded oklch()/white literals, never a theme token,
// in both this markup's inline styles and 04-patterns.css's `.ee-doc` rule — e-mail clients don't
// inherit the app theme, so the document must never render dark (B28). `body` fields carry
// developer-authored HTML ported from Twig `|raw` (e.g. the hero's `<strong>` emphasis, the
// footer's links) — v-html is required, never bound to user input (see eslint.config.js's
// no-v-html exemption).
import CouponCode from "@/components/molecules/CouponCode.vue";

export interface DocumentProduct {
    name: string;
    price: string;
}

export interface DocumentSection {
    type: "hero" | "coupon" | "products" | "footer";
    kicker?: string | null;
    title?: string | null;
    body?: string | null;
    code?: string | null;
    note?: string | null;
    cta?: string | null;
    href?: string | null;
    items?: DocumentProduct[] | null;
}

defineProps<{ sections: DocumentSection[] }>();
</script>

<template>
    <div class="ee-doc">
        <template v-for="(section, index) in sections" :key="index">
            <div v-if="section.type === 'hero'" class="doc-hero">
                <div
                    v-if="section.kicker"
                    class="mono"
                    style="
                        font-size: 11px;
                        letter-spacing: 0.15em;
                        color: oklch(0.5 0.03 90);
                    "
                >
                    {{ section.kicker }}
                </div>
                <div class="doc-h">{{ section.title }}</div>
                <div
                    v-if="section.body"
                    style="color: oklch(0.45 0.025 150); font-size: 14px"
                    v-html="section.body"
                ></div>
            </div>

            <div
                v-else-if="section.type === 'coupon'"
                class="doc-section"
                style="text-align: center"
            >
                <CouponCode :code="section.code ?? ''" :note="section.note" />
                <a class="doc-cta" :href="section.href ?? '#'">{{
                    section.cta
                }}</a>
            </div>

            <div v-else-if="section.type === 'products'" class="doc-section">
                <div
                    v-if="section.kicker"
                    class="mono"
                    style="
                        font-size: 11px;
                        letter-spacing: 0.15em;
                        color: oklch(0.5 0.03 90);
                    "
                >
                    {{ section.kicker }}
                </div>
                <div class="doc-prod">
                    <div
                        v-for="(item, itemIndex) in section.items"
                        :key="itemIndex"
                        class="item"
                    >
                        <div class="ph"></div>
                        <div style="font-weight: 500; font-size: 13px">
                            {{ item.name }}
                        </div>
                        <div
                            style="color: oklch(0.5 0.025 150); font-size: 12px"
                        >
                            {{ item.price }}
                        </div>
                    </div>
                </div>
            </div>

            <div
                v-else-if="section.type === 'footer'"
                class="doc-foot"
                v-html="section.body"
            ></div>
        </template>
    </div>
</template>
