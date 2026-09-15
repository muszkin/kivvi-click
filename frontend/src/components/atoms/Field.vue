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
        // PIO-70: lets a native form POST rely on the browser's own required-field check, the
        // way the login form already relies on type="email". Absent unless asked for, so no
        // existing Field starts refusing to submit.
        required?: boolean;
        autocomplete?: string;
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
            :required="required || undefined"
            :autocomplete="autocomplete"
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
