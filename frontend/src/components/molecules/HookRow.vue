<script setup lang="ts">
// MOLECULE · HookRow — webhook endpoint with its last HTTP status, ported from
// components/molecules/hook-row.html.twig. The status pill is a raw `.chip` span rather than the
// Chip atom (the old partial never routed it through the chip include either) so it can carry the
// centred, fixed-width inline style the original markup gives it.
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";

defineProps<{
    url: string;
    events: string[];
    code: number;
    last?: string;
}>();
</script>

<template>
    <div class="hook-row">
        <span
            class="chip"
            :class="code >= 200 && code < 300 ? 'good' : 'bad'"
            style="min-width: 56px; justify-content: center"
            >{{ code }}</span
        >
        <div style="min-width: 0; flex: 1">
            <div
                class="mono"
                style="
                    font-size: 12.5px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                "
            >
                {{ url }}
            </div>
            <div style="margin-top: 4px">
                <!-- The old markup concatenates chip.html.twig includes with no separator
                     string, but each chip's own multi-line HTML source leaves a raw newline
                     before its closing </span>, which the browser collapses into a single
                     rendered space between chips — Vue's compiler strips that same whitespace
                     at build time (no newline survives to be collapsed), so the space is
                     inserted explicitly here instead. -->
                <template v-for="(event, index) in events" :key="event"
                    >{{ index > 0 ? " " : "" }}<Chip :label="event" mono
                /></template>
            </div>
        </div>
        <span
            v-if="last"
            class="muted"
            style="font-size: 12px; white-space: nowrap"
            >{{ last }}</span
        >
        <Button variant="ghost" size="sm" icon="play" action="test-webhook" />
        <Button
            variant="ghost"
            size="sm"
            icon="settings"
            action="edit-webhook"
        />
    </div>
</template>
