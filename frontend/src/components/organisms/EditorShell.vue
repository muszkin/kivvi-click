<script setup lang="ts">
// ORGANISM · EditorShell — ported from components/organisms/editor-shell.html.twig. Three-pane
// WYSIWYG frame shared by the e-mail and popup editors: 280px library · fluid canvas · 280px
// inspector (hidden under 1300px, CSS only — see 04-patterns.css's `.email-right` media query).
// Wires the shared block-library → canvas drag-and-drop (useEditorDrag) on its own root: per
// DEV-7 a dropped block visibly does nothing (see that composable's class comment).
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import Icon from "@/components/atoms/Icon.vue";
import { useEditorDrag } from "@/composables/useEditorDrag";

// PIO-129: the two pane headings defaulted to Polish literals. `rightTitle` is never passed by
// either editor, so "Właściwości" was the heading on an English page as well.
const props = withDefaults(
    defineProps<{
        leftTitle?: string | null;
        leftIcon?: string | null;
        rightTitle?: string | null;
        rightIcon?: string;
    }>(),
    {
        leftTitle: null,
        leftIcon: null,
        rightTitle: null,
        rightIcon: "sliders",
    },
);

const { t } = useI18n();
const leftHeading = computed(() => props.leftTitle ?? t("common.blocks"));
const rightHeading = computed(() => props.rightTitle ?? t("common.properties"));

defineSlots<{
    left(): unknown;
    canvas(): unknown;
    right?(): unknown;
}>();

const root = ref<HTMLElement | null>(null);
useEditorDrag(root);
</script>

<template>
    <div class="email-editor" ref="root">
        <div class="ee-panel">
            <div class="ee-panel-head">
                <Icon v-if="leftIcon" :name="leftIcon" />{{ " "
                }}{{ leftHeading }}
            </div>
            <div class="ee-panel-body"><slot name="left" /></div>
        </div>

        <div class="ee-canvas-wrap scrollable"><slot name="canvas" /></div>

        <div v-if="$slots.right" class="ee-panel email-right">
            <div class="ee-panel-head">
                <Icon :name="rightIcon" />{{ " " }}{{ rightHeading }}
            </div>
            <div class="ee-panel-body"><slot name="right" /></div>
        </div>
    </div>
</template>
