<script setup lang="ts">
// MOLECULE · SwatchGrid — ported from the accent-colour picker inlined in
// pages/widgets/inspector.html.twig (the dedicated components/molecules/swatch-grid.html.twig
// partial is dead code on the old stack — never included by any page — so this component mirrors
// the inspector's own bespoke markup instead: a row of literal-`oklch(...)`-background buttons,
// `aria-label="Kolor N"`, the first one `aria-pressed`/bordered as selected).
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const props = withDefaults(
    defineProps<{
        colors: string[];
        selected?: number;
        action?: string | null;
    }>(),
    { selected: 0, action: "set-widget-accent" },
);

const { t } = useI18n();

const swatches = computed(() =>
    props.colors.map((color, index) => ({
        color,
        index,
        active: index === props.selected,
    })),
);
</script>

<template>
    <div class="row" style="gap: 6px; margin-bottom: 12px">
        <button
            v-for="swatch in swatches"
            :key="swatch.index"
            :data-action="action ?? undefined"
            :data-payload="action ? String(swatch.index) : undefined"
            :aria-label="`${t('popups.editor.colorName')} ${swatch.index + 1}`"
            :aria-pressed="swatch.active ? 'true' : 'false'"
            :style="{
                width: '28px',
                height: '28px',
                borderRadius: '6px',
                background: swatch.color,
                border: `2px solid ${swatch.active ? 'var(--fg)' : 'var(--line)'}`,
                cursor: 'pointer',
            }"
        ></button>
    </div>
</template>
