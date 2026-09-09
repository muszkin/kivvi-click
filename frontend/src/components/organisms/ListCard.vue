<script setup lang="ts" generic="T extends { href: string; columns: string }">
// ORGANISM · ListCard — the dashboard's "recently seen customers" and "best-performing
// automations" rows, ported from pages/dashboard/{recent-customers,top-automations}.html.twig.
// Both old-stack partials wrap each row in a plain `<a class="event-row" href="...">` — reusing
// the live event stream's compact grid styling for an unrelated row shape, not a second
// implementation of the live event row itself — so this is the one other component allowed to
// render `.event-row` (frontend/eslint.config.js has a matching, narrowly-scoped override for this
// exact file; see its comment). `columns` reproduces the old partial's own inline
// `style="grid-template-columns: ..."` override; `href` is a real document link (oracle
// journeys/dashboard steps 2 and 4 both record a full GET, never a client-side route change).
defineProps<{ rows: T[] }>();
defineSlots<{ default(props: { row: T }): unknown }>();
</script>

<template>
    <div>
        <a
            v-for="row in rows"
            :key="row.href"
            class="event-row"
            :href="row.href"
            :style="{
                gridTemplateColumns: row.columns,
                textDecoration: 'none',
                color: 'inherit',
            }"
        >
            <slot :row="row" />
        </a>
    </div>
</template>
