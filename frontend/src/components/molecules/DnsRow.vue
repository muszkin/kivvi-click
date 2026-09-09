<script setup lang="ts">
// MOLECULE · DnsRow — DNS record verification line (SPF/DKIM/DMARC/BIMI), ported from
// components/molecules/dns-row.html.twig.
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";

defineProps<{ record: string; value: string; ok: boolean }>();
</script>

<template>
    <div class="dns-row">
        <Chip
            :label="record"
            :tone="ok ? 'good' : 'warn'"
            :icon="ok ? 'check' : 'info'"
        />
        <span
            class="mono"
            style="
                font-size: 11.5px;
                color: var(--fg-muted);
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            "
            >{{ value }}</span
        >
        <Button
            variant="ghost"
            size="sm"
            icon="copy"
            action="copy-dns"
            :payload="record"
        />
    </div>
</template>
