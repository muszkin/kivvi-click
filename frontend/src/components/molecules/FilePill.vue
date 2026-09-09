<script setup lang="ts">
// MOLECULE · FilePill — the uploaded-file chip on the import wizard's mapping step, ported from
// step-mapping.html.twig's own inline `.file-pill` markup (never its own Twig molecule). First
// (and only, so far) owner: import-wizard.
import Button from "@/components/atoms/Button.vue";
import Icon from "@/components/atoms/Icon.vue";

defineProps<{
    name: string;
    meta: string;
    changeLabel: string;
    changeHref: string;
}>();
</script>

<template>
    <div class="file-pill">
        <Icon name="upload" />
        <div>
            <div style="font-weight: 500; font-size: 13px">{{ name }}</div>
            <div class="muted mono" style="font-size: 11px">{{ meta }}</div>
        </div>
        <Button
            variant="ghost"
            size="sm"
            :label="changeLabel"
            :href="changeHref"
        />
    </div>
</template>
