<script setup lang="ts">
// MOLECULE · PositionGrid — ported from components/molecules/position-grid.html.twig. 3×3
// placement picker for popups/widgets. `selected` is a 0-8 index, row-major; the inspector always
// passes 4 (centre) today, matching the old template's own hard-coded param.
import { computed } from "vue";

const GLYPHS = ["↖", "↑", "↗", "←", "●", "→", "↙", "↓", "↘"];

const props = withDefaults(
    defineProps<{ selected?: number; action?: string | null }>(),
    { selected: 4, action: "set-popup-position" },
);

const cells = computed(() =>
    GLYPHS.map((glyph, index) => ({
        glyph,
        index,
        active: index === props.selected,
    })),
);
</script>

<template>
    <div class="pos-grid" role="radiogroup" aria-label="Pozycja widgetu">
        <button
            v-for="cell in cells"
            :key="cell.index"
            class="pos-cell"
            role="radio"
            :aria-checked="cell.active ? 'true' : 'false'"
            :data-active="cell.active ? 'true' : 'false'"
            :data-action="action ?? undefined"
            :data-payload="action ? String(cell.index) : undefined"
        >
            {{ cell.glyph }}
        </button>
    </div>
</template>
