<script setup lang="ts">
// MOLECULE · Dropzone — ported from components/molecules/dropzone.html.twig + assets/controllers/
// upload.ts. File drop target for the import wizard's step 1.
//
// Repair-1: reproduces assets/controllers/upload.ts's own `send()` exactly — `fetch(...)` then
// `if (r.redirected) window.location.href = r.url`, a full document navigation. The oracle's own
// step-9 http.jsonl records three entries: the POST's own 302, the browser fetch()'s own
// auto-followed GET (kind "xhr"), and a THIRD "document" GET from this very
// `window.location.href` assignment — a real page load, not an SPA route change. An earlier
// version of this component swapped the third entry for `router.push` (a cheaper, SPA-native
// transition) as a deliberate breaking change; the orchestrator rejected that (repair-1: workers
// may not introduce an unapproved deviation, and the zero-change rule wins over "cheaper") and
// ruled that this component must match the old stack byte-for-byte instead. Request shape:
// `fetch("/import/upload", { method: "POST", body })`, same `file` field name, default
// credentials.
//
// PIO-125 added one field, `locale`: the language of the page the upload is posted from. The
// upload route has no locale segment, so without it the server could only redirect into the
// default locale — which, once English became the default, sent every Polish user to English.
// A prop rather than useRoute(), like Stepper's own `locale`: the parent step already knows it.
import { ref } from "vue";
import Icon from "@/components/atoms/Icon.vue";

const LOCALE_FIELD = "locale";

const props = withDefaults(
    defineProps<{
        title: string;
        locale: string;
        sub?: string;
        hint?: string;
        accept?: string;
    }>(),
    {},
);

const dragging = ref(false);
const input = ref<HTMLInputElement | null>(null);

async function send(file: File): Promise<void> {
    const body = new FormData();
    body.append("file", file);
    body.append(LOCALE_FIELD, props.locale);
    const response = await fetch("/import/upload", { method: "POST", body });
    if (response.redirected) {
        window.location.href = response.url;
    }
}

function onDragEnter(ev: DragEvent): void {
    ev.preventDefault();
    dragging.value = true;
}

function onDragOver(ev: DragEvent): void {
    ev.preventDefault();
    dragging.value = true;
}

function onDragLeave(): void {
    dragging.value = false;
}

function onDrop(ev: DragEvent): void {
    ev.preventDefault();
    dragging.value = false;
    const file = ev.dataTransfer?.files?.[0];
    if (file) void send(file);
}

function onChange(): void {
    const file = input.value?.files?.[0];
    if (file) void send(file);
}
</script>

<template>
    <label
        class="dropzone"
        data-controller="upload"
        :data-dragging="dragging ? 'true' : undefined"
        @dragenter="onDragEnter"
        @dragover="onDragOver"
        @dragleave="onDragLeave"
        @drop="onDrop"
    >
        <Icon name="upload" />
        <div class="dropzone__title">{{ title }}</div>
        <div v-if="sub" class="dropzone__sub">{{ sub }}</div>
        <div v-if="hint" class="dropzone__sub mono">{{ hint }}</div>
        <input
            ref="input"
            type="file"
            hidden
            :accept="accept"
            data-upload-target="input"
            @change="onChange"
        />
    </label>
</template>
