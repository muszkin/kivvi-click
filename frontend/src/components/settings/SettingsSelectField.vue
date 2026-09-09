<script setup lang="ts">
// Settings-local select field, matching the `type: 'select'` branch of
// components/atoms/field.html.twig. The shared Field.vue atom (frontend/src/components/atoms/
// Field.vue) only renders a plain <input> today — it never grew the select/textarea branches the
// old atom's own contract documents, because no journey before this one needed them. Field.vue is
// a shared component this journey does not own (see the packet's parallel-safety touch list), so
// rather than extend it here, this local component reproduces the old select markup exactly;
// worker-report.md flags the gap for whoever next needs a shared select field.
export interface SettingsSelectOption {
    value: string;
    label: string;
    selected?: boolean;
}

withDefaults(
    defineProps<{
        name: string;
        label?: string;
        options: SettingsSelectOption[];
        mono?: boolean;
    }>(),
    { mono: false },
);
</script>

<template>
    <div class="field-row">
        <label v-if="label" class="label" :for="`f-${name}`">{{ label }}</label>
        <select class="select" :class="{ mono }" :id="`f-${name}`" :name="name">
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
