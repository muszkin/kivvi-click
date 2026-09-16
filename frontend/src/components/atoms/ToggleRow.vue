<script setup lang="ts">
// ATOM · ToggleRow — checkbox or radio with an inline label, ported from
// components/atoms/toggle-row.html.twig. `label` is bound with v-html: every settings usage of
// this atom passes developer-authored copy, one of them (the import wizard's "send report to
// <email>" row) carrying an embedded `<span class="mono">` exactly like the old `{{ label|raw }}`
// did.
withDefaults(
    defineProps<{
        kind?: "checkbox" | "radio";
        name: string;
        label: string;
        checked?: boolean;
        value?: string;
    }>(),
    { kind: "checkbox", checked: false },
);
</script>

<template>
    <label :class="kind === 'radio' ? 'radio-row' : 'checkbox-row'">
        <input :type="kind" :name="name" :value="value" :checked="checked" />
        <span v-html="label"></span>
    </label>
</template>
