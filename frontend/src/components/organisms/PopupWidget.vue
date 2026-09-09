<script setup lang="ts">
// ORGANISM · PopupWidget — ported from components/organisms/popup-widget.html.twig. The widget
// itself, in five shapes. Like the e-mail document, it uses literal colours (04-patterns.css's
// `.pw*` rules) — it renders on the customer's storefront, outside our theme, so no v-html is
// needed here even though the old template used `|raw` throughout (every field is developer
// fixture text ported by WidgetFixtures, never reflected user input).
import { computed } from "vue";
import Icon from "@/components/atoms/Icon.vue";

const CLASS_BY_TYPE: Record<string, string> = {
    modal: "pw--modal",
    "slide-in": "pw--slidein",
    banner: "pw--banner",
    fullscreen: "pw--fullscreen",
    toast: "pw--toast",
};

const props = defineProps<{
    type: string;
    kicker?: string | null;
    title: string;
    body?: string | null;
    placeholder?: string | null;
    cta?: string | null;
    fine?: string | null;
}>();

const shapeClass = computed(() => CLASS_BY_TYPE[props.type] ?? "");
const showBody = computed(() => !!props.body && props.type !== "slide-in");
</script>

<template>
    <div class="pw" :class="shapeClass" role="dialog" :aria-label="title">
        <button
            class="pw-close"
            data-action="dismiss-widget"
            aria-label="Zamknij"
        >
            <Icon name="x" />
        </button>

        <div v-if="type === 'banner'" class="pw-banner-row">
            <div>
                <div v-if="kicker" class="mono pw-kicker">{{ kicker }}</div>
                <div class="pw-title" style="font-size: 19px; margin: 2px 0 0">
                    {{ title }}
                </div>
            </div>
            <button class="pw-cta" style="width: auto; white-space: nowrap">
                {{ cta }}
            </button>
        </div>

        <div
            v-else-if="type === 'toast'"
            class="row"
            style="gap: 10px; align-items: flex-start"
        >
            <span class="pw-toast-dot"></span>
            <div>
                <div style="font-weight: 600; font-size: 13px">{{ title }}</div>
                <div v-if="body" style="font-size: 12.5px; opacity: 0.75">
                    {{ body }}
                </div>
            </div>
        </div>

        <template v-else>
            <div v-if="kicker" class="mono pw-kicker">{{ kicker }}</div>
            <h3 class="pw-title">{{ title }}</h3>
            <p v-if="showBody" class="pw-sub">{{ body }}</p>
            <form class="pw-form" data-action="submit-widget">
                <input
                    v-if="placeholder"
                    class="pw-input"
                    type="email"
                    name="email"
                    :placeholder="placeholder"
                    required
                />
                <button class="pw-cta" type="submit">{{ cta }}</button>
            </form>
            <div v-if="fine" class="pw-fine">{{ fine }}</div>
        </template>
    </div>
</template>
