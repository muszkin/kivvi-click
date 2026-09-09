<script setup lang="ts" generic="Row">
// MOLECULE · Table — data table, ported from components/molecules/table.html.twig. Put it in a
// Card via its "raw" slot so it goes edge to edge. First owner: later journeys reuse this
// component rather than build their own <table>. The old molecule also offered a "scroll"
// wrapper for 8+ column tables — no page in this journey needs it, so it is left for whichever
// later journey does, rather than added here untested.
//
// Generic over the row's own shape so a caller's "row" slot gets a typed row back instead of
// casting `unknown` on every cell.
export interface TableColumn {
    label: string;
    align?: "right";
}

export interface TableRowAction {
    action: string;
    payload: string;
}

defineProps<{
    columns: TableColumn[];
    rows: Row[];
    rowActions?: (TableRowAction | undefined)[];
}>();

defineSlots<{
    row(props: { row: Row; index: number }): unknown;
}>();
</script>

<template>
    <table class="table">
        <thead>
            <tr>
                <th
                    v-for="(column, index) in columns"
                    :key="index"
                    :class="column.align === 'right' ? 'right' : undefined"
                >
                    {{ column.label }}
                </th>
            </tr>
        </thead>
        <tbody>
            <tr
                v-for="(row, index) in rows"
                :key="index"
                :data-action="rowActions?.[index]?.action"
                :data-payload="rowActions?.[index]?.payload"
                :style="rowActions?.[index] ? { cursor: 'pointer' } : undefined"
            >
                <slot name="row" :row="row" :index="index" />
            </tr>
        </tbody>
    </table>
</template>
