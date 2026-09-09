<script setup lang="ts">
// ORGANISM · BlockLibrary — ported from components/organisms/block-library.html.twig. 2-column
// grid of draggable block sources for the e-mail (and, from wave-4, popup) editor; cursor: grab.
// Drag itself is wired by EditorShell's useEditorDrag, event-delegated off `[data-block-type]` —
// this component only needs to declare that attribute and `draggable`.
import Icon from "@/components/atoms/Icon.vue";

export interface LibraryBlock {
    icon: string;
    label: string;
    type: string;
}

defineProps<{ blocks: LibraryBlock[] }>();
</script>

<template>
    <div class="ee-block-lib">
        <button
            v-for="(block, index) in blocks"
            :key="index"
            draggable="true"
            :data-block-type="block.type"
        >
            <Icon :name="block.icon" />{{ " " }}{{ block.label }}
        </button>
    </div>
</template>
