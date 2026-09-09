<script setup lang="ts">
// PARTIAL · e-mail editor / inspector — ported from pages/emails/inspector.html.twig. Properties
// of the selected block: copy, section background, alignment, padding and the condition that
// decides whether the block renders at all.
//
// The "Wsparcie placeholder" <select> is hand-built here rather than through the shared Field
// atom: Field.vue (ported from wave-1's own needs) only renders an <input> — it never gained
// field.html.twig's select/textarea branches because no earlier journey needed one. Extending a
// shared, read-only atom is out of scope for this slice (see worker-report.md); this reproduces
// field.html.twig's select markup 1:1 instead, the same way CustomerView.vue builds its own
// back-link markup rather than extending PageHead.
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Field from "@/components/atoms/Field.vue";
import Segmented, {
    type SegmentedOption,
} from "@/components/molecules/Segmented.vue";

export interface SelectedBlock {
    blockName: string;
    blockId: string;
    title: string;
    placeholders: string[];
    backgrounds: string[];
    alignment: string;
    padding: string;
    visibility: string;
}

const props = defineProps<{ block: SelectedBlock }>();

const { t } = useI18n();

const alignmentOptions = computed<SegmentedOption[]>(() => [
    {
        value: "left",
        label: t("campaigns.editor.alignLeft"),
        active: props.block.alignment === "left",
    },
    {
        value: "center",
        label: t("campaigns.editor.alignCenter"),
        active: props.block.alignment === "center",
    },
    {
        value: "right",
        label: t("campaigns.editor.alignRight"),
        active: props.block.alignment === "right",
    },
]);
</script>

<template>
    <div
        class="muted"
        style="
            font-size: 11.5px;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            margin-bottom: 6px;
        "
    >
        {{ t("campaigns.editor.selectedBlock") }}
    </div>
    <div
        class="card"
        style="padding: 10px; border-radius: 8px; margin-bottom: 16px"
    >
        <div style="font-weight: 500; font-size: 13px">
            {{ block.blockName }}
        </div>
        <div class="muted mono" style="font-size: 11.5px">
            block_id: {{ block.blockId }}
        </div>
    </div>

    <Field
        name="hero_title"
        :label="t('campaigns.editor.titleField')"
        :value="block.title"
    />

    <div class="field-row">
        <label class="label" for="f-hero_placeholder">{{
            t("campaigns.editor.placeholderSupport")
        }}</label>
        <select class="select" id="f-hero_placeholder" name="hero_placeholder">
            <option
                v-for="(placeholder, index) in block.placeholders"
                :key="index"
                :value="placeholder"
                :selected="index === 0"
            >
                {{ placeholder }}
            </option>
        </select>
    </div>

    <label class="label">{{ t("campaigns.editor.sectionBackground") }}</label>
    <div class="row" style="gap: 6px; margin-bottom: 12px">
        <button
            v-for="(background, index) in block.backgrounds"
            :key="index"
            data-action="set-section-background"
            :data-payload="index"
            :aria-label="`${t('campaigns.editor.backgroundAlt')} ${index + 1}`"
            :aria-pressed="index === 0 ? 'true' : 'false'"
            :style="{
                width: '28px',
                height: '28px',
                borderRadius: '6px',
                background: background,
                border: `2px solid ${index === 0 ? 'var(--fg)' : 'var(--line)'}`,
                cursor: 'pointer',
            }"
        ></button>
    </div>

    <label class="label">{{ t("campaigns.editor.alignment") }}</label>
    <Segmented action="set-section-align" :options="alignmentOptions" />

    <div style="margin-top: 12px">
        <Field
            name="section_padding"
            :label="t('campaigns.editor.sectionPadding')"
            :value="block.padding"
            mono
        />
    </div>

    <div class="section-title" style="margin-top: 8px">
        {{ t("campaigns.editor.visibilityTitle") }}
    </div>
    <div class="muted" style="font-size: 12px; margin-bottom: 8px">
        {{ t("campaigns.editor.visibilityLead") }}
    </div>
    <div class="row" style="gap: 4px">
        <Chip :label="block.visibility" mono />
    </div>
    <div style="margin-top: 8px">
        <Button
            variant="ghost"
            size="sm"
            icon="plus"
            :label="t('campaigns.editor.addVisibilityRule')"
            action="add-visibility-rule"
        />
    </div>
</template>
