<script setup lang="ts">
// PAGE · Edytor automatyzacji — ported from pages/automation-editor.html.twig. Shared by both
// GET /{locale}/automations/new and GET /{locale}/automations/{id} (the old controller's own
// `editor()` private method serves both routes the same page too).
//
// Data arrives from GET /api/v1/{locale}/automations/{id}?view=, pre-formatted by
// AutomationsViewService — this view never formats a number itself. Switching Lista/Diagram is a
// real navigation (see useIntents' "navigate" intent — tests/e2e/specs/automations.spec.ts asserts
// `.seg [data-action="navigate"]` literally), not client-side state: the same rule model rendered
// two ways, and both views stay linkable URLs.
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Card from "@/components/molecules/Card.vue";
import Segmented, {
    type SegmentedOption,
} from "@/components/molecules/Segmented.vue";
import SimulationCard, {
    type SimulationReadout,
} from "@/components/molecules/SimulationCard.vue";
import Tabs, { type TabItem } from "@/components/molecules/Tabs.vue";
import FlowCanvas, {
    type FlowCanvasEdge,
    type FlowCanvasNode,
} from "@/components/organisms/FlowCanvas.vue";
import RulePipeline, {
    type PipelineStep,
} from "@/components/organisms/RulePipeline.vue";

interface AutomationHeader {
    id: string;
    name: string;
    status: string;
    statusLabel: string;
}

interface EditorPayload {
    automation: AutomationHeader;
    view: "list" | "flow";
    tabs: TabItem[];
    steps: PipelineStep[];
    nodes: FlowCanvasNode[];
    edges: FlowCanvasEdge[];
    simulation: SimulationReadout[];
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const automationId = computed(() =>
    typeof route.params.id === "string" ? route.params.id : "new",
);
const backHref = computed(() => `/${locale.value}/automations`);

const payload = ref<EditorPayload | null>(null);

function queryParam(name: string): string | undefined {
    const value = route.query[name];
    return typeof value === "string" ? value : undefined;
}

async function load(): Promise<void> {
    const view = queryParam("view");
    const query = view ? `?view=${encodeURIComponent(view)}` : "";
    const response = await fetch(
        `/api/v1/${locale.value}/automations/${automationId.value}${query}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as EditorPayload;
}

onMounted(load);

const segmentedOptions = computed<SegmentedOption[]>(() => {
    if (!payload.value) {
        return [];
    }
    const basePath = route.path;
    return [
        {
            value: `${basePath}?view=list`,
            label: t("automations.list"),
            icon: "list",
            active: payload.value.view === "list",
        },
        {
            value: `${basePath}?view=flow`,
            label: t("automations.diagram"),
            icon: "flow",
            active: payload.value.view === "flow",
        },
    ];
});
</script>

<template>
    <div v-if="payload" class="page">
        <div class="page-head">
            <div>
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('automations.backToList')"
                    :href="backHref"
                    style="margin-bottom: 6px"
                />
                <h1 class="page-title">{{ payload.automation.name }}</h1>
                <p class="page-sub" v-html="payload.automation.statusLabel"></p>
            </div>
            <div class="page-actions">
                <Segmented action="navigate" :options="segmentedOptions" />
                <Button
                    size="sm"
                    icon="eye"
                    :label="t('automations.preview')"
                    action="preview"
                />
                <Button
                    size="sm"
                    :label="t('automations.draft')"
                    action="save-draft"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="play"
                    :label="t('automations.publish')"
                    action="publish"
                    :payload="payload.automation.id"
                />
            </div>
        </div>

        <Tabs :tabs="payload.tabs" />

        <FlowCanvas
            v-if="payload.view === 'flow'"
            :nodes="payload.nodes"
            :edges="payload.edges"
        />
        <RulePipeline v-else :steps="payload.steps" />

        <div style="margin-top: 20px">
            <Card
                :title="t('automations.ruleTestTitle')"
                icon="target"
                :sub="t('automations.ruleTestSub')"
            >
                <template #headActions>
                    <Button
                        variant="primary"
                        size="sm"
                        icon="play"
                        :label="t('automations.runTest')"
                        action="run-test"
                    />
                </template>
                <SimulationCard :items="payload.simulation" />
            </Card>
        </div>
    </div>
</template>
