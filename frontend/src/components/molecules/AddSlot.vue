<script setup lang="ts">
// MOLECULE · AddSlot — ported from components/molecules/add-slot.html.twig. Dashed "add another"
// affordance, turns accent on hover (04-patterns.css's `.rb-add` rules).
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        label: string;
        action?: string | null;
        payload?: string | null;
    }>(),
    { action: null, payload: null },
);
</script>

<template>
    <button
        class="rb-add"
        :data-action="action ?? undefined"
        :data-payload="action ? (payload ?? undefined) : undefined"
    >
        <Icon name="plus" />{{ " " }}{{ label }}
    </button>
</template>
