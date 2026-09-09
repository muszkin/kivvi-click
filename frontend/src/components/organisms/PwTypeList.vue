<script setup lang="ts">
// ORGANISM · PwTypeList — ported from pages/widgets/type-list.html.twig. Five widget shapes;
// picking one is a URL change so the preview stays linkable — each option's `href` is built by the
// caller (PopupEditorView), the same convention Segmented's own `options[].value` already uses for
// a link-shaped switch.
import Icon from "@/components/atoms/Icon.vue";

export interface PwType {
    id: string;
    label: string;
    icon: string;
    href: string;
    active: boolean;
}

defineProps<{ types: PwType[] }>();
</script>

<template>
    <div class="pw-type-list">
        <a
            v-for="type in types"
            :key="type.id"
            class="pw-type"
            :data-active="type.active ? 'true' : 'false'"
            :href="type.href"
        >
            <Icon :name="type.icon" />
            <span>{{ type.label }}</span>
        </a>
    </div>
</template>
