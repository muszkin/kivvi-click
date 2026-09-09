<script setup lang="ts">
// MOLECULE · RbBlock — ported from components/molecules/rb-block.html.twig. One block inside a
// rule-builder step; `body` carries developer-authored inline HTML (`.pill`/`<strong>` spans from
// the fixture content), never reflected user input, hence the v-html (see eslint.config.js's
// vue/no-v-html exemption).
import Icon from "@/components/atoms/Icon.vue";

defineProps<{ icon: string; title: string; body: string }>();
</script>

<template>
    <div class="rb-block">
        <div class="rb-block__head">
            <span class="ic"><Icon :name="icon" /></span>
            <span>{{ title }}</span>
        </div>
        <div class="rb-block__body" v-html="body"></div>
    </div>
</template>
