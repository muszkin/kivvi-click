<script setup lang="ts">
// ORGANISM · PopupStage — ported from components/organisms/popup-stage.html.twig. Widget preview
// on a mock storefront; min-height (480 desktop / 620 mobile, 04-patterns.css) is load-bearing —
// without it the aspect-ratio box collapses under the widget and overflow:hidden clips it. The
// widget itself arrives through the default slot (PopupWidget), matching the old template's own
// `widget` param (pre-rendered HTML passed in, not built here).
import ShopMock from "./ShopMock.vue";

withDefaults(defineProps<{ type: string; device?: "desktop" | "mobile" }>(), {
    device: "desktop",
});
</script>

<template>
    <div class="pw-stage" :data-device="device" :data-type="type">
        <ShopMock />
        <div class="pw-dim"></div>
        <slot />
    </div>
</template>
