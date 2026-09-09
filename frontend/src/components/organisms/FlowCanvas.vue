<script setup lang="ts">
// ORGANISM · FlowCanvas — ported from components/organisms/flow-canvas.html.twig. Node-graph
// variant of the rule builder: edges are dashed bezier paths in one SVG behind the nodes; the
// dotted background is a repeating radial-gradient at 24px (CSS only, already byte-identical).
//
// "Węzeł" / "Auto-układ" and the node/edge count footer are NOT `|trans`'d in the old template
// either (rule-pipeline's add-slot label is a fixture param, these two are template-literal Polish
// left as-is) — reproduced verbatim rather than through vue-i18n.
import { computed } from "vue";
import Button from "@/components/atoms/Button.vue";
import FlowNode from "@/components/molecules/FlowNode.vue";

export interface FlowCanvasNode {
    x: number;
    y: number;
    kind: "trigger" | "cond" | "action";
    kicker: string;
    title: string;
    sub?: string | null;
    icon?: string | null;
}

export interface FlowCanvasEdge {
    from: number;
    to: number;
}

const props = withDefaults(
    defineProps<{
        nodes: FlowCanvasNode[];
        edges: FlowCanvasEdge[];
        nodeWidth?: number;
    }>(),
    { nodeWidth: 220 },
);

interface EdgePath {
    d: string;
    endX: number;
    endY: number;
}

const edgePaths = computed<EdgePath[]>(() => {
    const paths: EdgePath[] = [];
    for (const edge of props.edges) {
        const from = props.nodes[edge.from];
        const to = props.nodes[edge.to];
        if (!from || !to) continue;
        const startX = from.x + props.nodeWidth;
        const startY = from.y + 32;
        const endX = to.x;
        const endY = to.y + 32;
        paths.push({
            d: `M ${startX} ${startY} C ${startX + 60} ${startY} ${endX - 60} ${endY} ${endX} ${endY}`,
            endX,
            endY,
        });
    }
    return paths;
});
</script>

<template>
    <div class="flow-canvas" data-controller="flow-canvas">
        <svg
            class="flow-svg"
            viewBox="0 0 900 540"
            preserveAspectRatio="none"
            aria-hidden="true"
        >
            <template v-for="(path, index) in edgePaths" :key="index">
                <path
                    :d="path.d"
                    stroke="var(--line-strong)"
                    stroke-width="1.5"
                    fill="none"
                    stroke-dasharray="4 4"
                />
                <circle
                    :cx="path.endX"
                    :cy="path.endY"
                    r="3"
                    fill="var(--accent)"
                />
            </template>
        </svg>
        <FlowNode
            v-for="(node, index) in nodes"
            :key="index"
            :x="node.x"
            :y="node.y"
            :kind="node.kind"
            :kicker="node.kicker"
            :title="node.title"
            :sub="node.sub"
            :icon="node.icon"
        />
        <div
            style="
                position: absolute;
                left: 24px;
                bottom: 24px;
                display: flex;
                gap: 6px;
            "
        >
            <Button size="sm" icon="plus" label="Węzeł" action="add-node" />
            <Button
                size="sm"
                icon="move"
                label="Auto-układ"
                action="autolayout"
            />
        </div>
        <div
            style="
                position: absolute;
                right: 24px;
                bottom: 24px;
                font-family: var(--font-mono);
                font-size: 11px;
                color: var(--fg-muted);
            "
        >
            {{ nodes.length }} węzłów · {{ edges.length }} połączeń
        </div>
    </div>
</template>
