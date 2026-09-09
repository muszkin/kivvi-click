<script setup lang="ts">
// PARTIAL · import step 2 · validation list — ported from pages/import/validation-list.html.twig.
import Icon from "@/components/atoms/Icon.vue";

export interface ValidationCheck {
    label: string;
    tone: string;
}

defineProps<{ validations: ValidationCheck[] }>();
</script>

<template>
    <div
        style="
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 8px 24px;
        "
    >
        <div
            v-for="(check, index) in validations"
            :key="index"
            class="row"
            style="font-size: 13px; gap: 8px"
            :style="check.tone === 'info' ? { color: 'var(--fg-muted)' } : {}"
        >
            <Icon :name="check.tone === 'info' ? 'info' : 'check'" />
            <span v-html="check.label"></span>
        </div>
    </div>
</template>
