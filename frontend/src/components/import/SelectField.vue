<script setup lang="ts">
// Import-wizard-local select field, matching the `type: 'select'` branch of
// components/atoms/field.html.twig — the shared Field.vue atom only renders a plain <input>
// today (see frontend/src/components/settings/SettingsSelectField.vue's own class comment for
// the same gap, hit independently by the settings journey). Field.vue is a shared component
// this journey does not own, so rather than extend it here, this local component reproduces the
// old select markup exactly; worker-report.md flags the gap again for whoever next needs a
// shared select field.
export interface SelectFieldOption {
    value: string;
    label: string;
    selected?: boolean;
}

defineProps<{
    name: string;
    label?: string;
    options: SelectFieldOption[];
}>();
</script>

<template>
    <div class="field-row">
        <label v-if="label" class="label" :for="`f-${name}`">{{ label }}</label>
        <select class="select" :id="`f-${name}`" :name="name">
            <option
                v-for="option in options"
                :key="option.value"
                :value="option.value"
                :selected="option.selected"
            >
                {{ option.label }}
            </option>
        </select>
    </div>
</template>
