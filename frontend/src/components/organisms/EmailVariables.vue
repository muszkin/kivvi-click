<script setup lang="ts">
// PARTIAL · e-mail editor / variables — ported from pages/emails/variables.html.twig.
// Copy-to-clipboard list of placeholder tokens, then a note on which A/B variant is shown. The
// "copy-variable" intent (useIntents.ts) copies the button's own data-payload to the clipboard.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";

export interface EditorVariable {
    token: string;
    description: string;
}

defineProps<{ variables: EditorVariable[] }>();

const { t } = useI18n();
</script>

<template>
    <div class="section-title" style="margin-top: 22px">
        {{ t("campaigns.editor.variablesTitle") }}
    </div>
    <div class="col" style="gap: 6px">
        <div
            v-for="(variable, index) in variables"
            :key="index"
            class="row"
            style="
                background: var(--bg);
                border: 1px solid var(--line);
                padding: 6px 8px;
                border-radius: 6px;
                font-family: var(--font-mono);
                font-size: 11.5px;
                justify-content: space-between;
                gap: 8px;
            "
        >
            <span :title="variable.description">{{ variable.token }}</span>
            <Button
                variant="ghost"
                size="sm"
                icon="copy"
                action="copy-variable"
                :payload="variable.token"
            />
        </div>
    </div>

    <div class="section-title" style="margin-top: 22px">
        {{ t("campaigns.editor.abTestsTitle") }}
    </div>
    <div class="muted" style="font-size: 12px">
        {{ t("campaigns.editor.abTestsBody") }}
    </div>
</template>
