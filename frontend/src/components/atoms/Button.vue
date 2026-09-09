<script setup lang="ts">
import { computed } from "vue";
import Icon from "./Icon.vue";

const props = withDefaults(
    defineProps<{
        label?: string;
        variant?: "default" | "primary" | "ghost" | "danger";
        size?: "sm" | "md" | "lg";
        icon?: string;
        iconEnd?: string;
        action?: string;
        payload?: string;
        href?: string;
        disabled?: boolean;
        type?: string;
    }>(),
    { variant: "default", size: "md", type: "button" },
);

const classes = computed(() =>
    [
        "btn",
        props.variant !== "default" ? props.variant : "",
        props.size !== "md" ? props.size : "",
    ]
        .filter(Boolean)
        .join(" "),
);
</script>

<template>
    <component
        :is="href ? 'a' : 'button'"
        :class="classes"
        :href="href"
        :type="href ? undefined : type"
        :data-action="action"
        :data-payload="payload"
        :disabled="disabled || undefined"
        :aria-disabled="disabled ? 'true' : undefined"
    >
        <Icon v-if="icon" :name="icon" />
        <span v-if="label">{{ label }}</span>
        <Icon v-if="iconEnd" :name="iconEnd" />
    </component>
</template>
