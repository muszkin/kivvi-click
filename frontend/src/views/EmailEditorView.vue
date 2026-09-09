<script setup lang="ts">
// PAGE · Edytor szablonu email — ported from pages/email-editor.html.twig (+ its
// pages/emails/{envelope,inspector,variables}.html.twig partials, each its own component:
// EmailEnvelope/EmailInspector/EmailVariables).
//
// Data arrives from GET /api/v1/{locale}/emails/{id} ("new" or "k\d+"), pre-formatted by
// CampaignsViewService — this view never formats a number itself. The document route renders 200
// for ANY id shape-matching k\d+ (see CampaignsController's class comment), so this view has no
// "not found" branch: every id resolves to either a known template or the same blank-draft shape.
//
// The back link is built inline rather than through PageHead (which has no "back" slot yet) so
// its exact markup — `.page-head a.btn.ghost` — matches templates/components/organisms/
// page-head.html.twig's own `back` param 1:1 (same approach as CustomerView.vue).
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import BlockLibrary, {
    type LibraryBlock,
} from "@/components/organisms/BlockLibrary.vue";
import EditorShell from "@/components/organisms/EditorShell.vue";
import EmailDocument, {
    type DocumentSection,
} from "@/components/organisms/EmailDocument.vue";
import EmailEnvelope from "@/components/organisms/EmailEnvelope.vue";
import EmailInspector, {
    type SelectedBlock,
} from "@/components/organisms/EmailInspector.vue";
import EmailVariables, {
    type EditorVariable,
} from "@/components/organisms/EmailVariables.vue";

interface EmailTemplate {
    id: string;
    name: string;
    meta: string;
    subject: string;
    sender: string;
}

interface EmailEditorPayload {
    template: EmailTemplate;
    blocks: LibraryBlock[];
    variables: EditorVariable[];
    sections: DocumentSection[];
    selectedBlock: SelectedBlock;
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const id = computed(() =>
    typeof route.params.id === "string" ? route.params.id : "new",
);
const backHref = computed(() => `/${locale.value}/campaigns`);

const payload = ref<EmailEditorPayload | null>(null);

async function load(): Promise<void> {
    const response = await fetch(`/api/v1/${locale.value}/emails/${id.value}`, {
        headers: { Accept: "application/json" },
    });
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as EmailEditorPayload;
}

onMounted(load);
watch(id, load);
</script>

<template>
    <div
        v-if="payload"
        class="page"
        style="max-width: none; padding: 16px 16px 0"
    >
        <div class="page-head">
            <div>
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('campaigns.editor.back')"
                    :href="backHref"
                    style="margin-bottom: 6px"
                />
                <h1 class="page-title">{{ payload.template.name }}</h1>
                <p class="page-sub" v-html="payload.template.meta"></p>
            </div>
            <div class="page-actions">
                <Button
                    size="sm"
                    icon="eye"
                    :label="t('campaigns.editor.previewDesktop')"
                    action="set-email-device"
                    payload="desktop"
                />
                <Button
                    size="sm"
                    icon="eye"
                    :label="t('campaigns.editor.mobile')"
                    action="set-email-device"
                    payload="mobile"
                />
                <Button
                    size="sm"
                    icon="mail"
                    :label="t('campaigns.editor.sendTest')"
                    action="send-test"
                />
                <Button
                    size="sm"
                    :label="t('campaigns.editor.draft')"
                    action="save-draft"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="play"
                    :label="t('campaigns.editor.publish')"
                    action="publish"
                    :payload="payload.template.id"
                />
            </div>
        </div>

        <EditorShell
            :left-title="t('campaigns.editor.blocksTitle')"
            left-icon="grid"
        >
            <template #left>
                <BlockLibrary :blocks="payload.blocks" />
                <EmailVariables :variables="payload.variables" />
            </template>
            <template #canvas>
                <EmailEnvelope
                    :sender="payload.template.sender"
                    :subject="payload.template.subject"
                />
                <EmailDocument :sections="payload.sections" />
            </template>
            <template #right>
                <EmailInspector :block="payload.selectedBlock" />
            </template>
        </EditorShell>
    </div>
</template>
