<script setup lang="ts">
// ATOM · CodeBlock — dark snippet panel, ported from components/atoms/code-block.html.twig.
// Highlighting was a server-side Twig filter on the old stack; it is now a client-side port
// (see src/highlight.ts) run on every render — the old stack's own comment on why it lived on
// the server ("so a snippet is one escaped string of markup, identical everywhere") no longer
// applies once there is no template-rendering server at all: the SPA is the only renderer left,
// so highlighting here IS "one place", just a browser-side one.
import { computed } from "vue";
import { highlight } from "@/highlight";

const props = defineProps<{ code: string; lang?: string }>();

const highlighted = computed(() => highlight(props.code));
</script>

<template>
    <pre class="code-block" :data-lang="lang" v-html="highlighted"></pre>
</template>
