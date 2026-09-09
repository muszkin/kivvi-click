<script setup lang="ts">
withDefaults(
    defineProps<{
        type?: string;
        name: string;
        label?: string;
        value?: string;
        placeholder?: string;
        mono?: boolean;
        help?: string;
        error?: string;
    }>(),
    { type: "text", value: "", placeholder: "" },
);
</script>

<template>
    <div class="field-row">
        <label v-if="label" class="label" :for="`f-${name}`">{{ label }}</label>
        <input
            class="input"
            :class="{ mono }"
            :id="`f-${name}`"
            :name="name"
            :type="type"
            :value="value"
            :placeholder="placeholder"
            :aria-invalid="error ? 'true' : undefined"
        />
        <div v-if="help" class="muted" style="font-size: 12px; margin-top: 4px">
            {{ help }}
        </div>
        <div
            v-if="error"
            style="color: var(--bad); font-size: 12px; margin-top: 4px"
        >
            {{ error }}
        </div>
    </div>
</template>
