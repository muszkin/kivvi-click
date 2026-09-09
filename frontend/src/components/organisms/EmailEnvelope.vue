<script setup lang="ts">
// PARTIAL · e-mail editor / envelope — ported from pages/emails/envelope.html.twig. Compact
// sender + subject strip above the 600px document.
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";

defineProps<{ sender: string; subject: string }>();

const { t } = useI18n();
</script>

<template>
    <div
        class="row"
        style="
            max-width: 600px;
            margin: 0 auto 14px;
            background: var(--bg-card);
            border: 1px solid var(--line);
            border-radius: var(--radius-md);
            padding: 10px 14px;
            gap: 14px;
        "
    >
        <div
            style="
                font-size: 11.5px;
                color: var(--fg-muted);
                display: flex;
                gap: 22px;
                flex: 1;
                min-width: 0;
            "
        >
            <div>
                <span class="muted">{{
                    t("campaigns.editor.envelopeFrom")
                }}</span
                >{{ " " }}<span class="mono">{{ sender }}</span>
            </div>
            <div
                style="
                    min-width: 0;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                "
            >
                <span class="muted">{{
                    t("campaigns.editor.envelopeSubject")
                }}</span
                >{{ " " }}<span class="mono">{{ subject }}</span>
            </div>
        </div>
        <Button variant="ghost" size="sm" icon="edit" action="edit-envelope" />
    </div>
</template>
