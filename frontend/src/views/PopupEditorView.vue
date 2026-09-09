<script setup lang="ts">
// PAGE · Edytor popupu — ported from pages/popup-editor.html.twig (+ its
// pages/widgets/{type-list,blocks,inspector,stage-meta}.html.twig partials, split into
// PwTypeList/PositionGrid/SwatchGrid/TriggerRow/AddSlot components plus inline markup here for the
// pieces the packet named no dedicated component for — the same balance EmailEditorView.vue struck
// for the e-mail editor).
//
// Shared by GET /{locale}/popups/new and GET /{locale}/popups/{id} (the old controller's own
// `editor()` private method serves both routes too). Data arrives from GET
// /api/v1/{locale}/popups/{id}?type=&device=, pre-formatted by WidgetViewService — this view never
// formats a number itself.
//
// The widget-type switch (PwTypeList) and the device switch (Segmented) are both real navigations
// to THIS SAME route with a different single query parameter — mirrors
// `path(_route, _route_params|merge({type: t.id}))` / `...|merge({device: ...})` in the old
// template exactly: each switch is built from the bare current PATH plus only the ONE query
// parameter it changes, so clicking one drops whatever the other currently holds (the old stack's
// own `_route_params` never carried the other's query value either) — the same "basePath + one
// query param" convention AutomationEditorView.vue's own `segmentedOptions` computed already
// established for its Lista/Diagram switch.
//
// The back link is built inline rather than through PageHead (which has no "back" slot yet) so its
// exact markup — `.page-head a.btn.ghost` — matches page-head.html.twig's own `back` param 1:1,
// the same approach EmailEditorView.vue/CustomerView.vue already use.
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Field from "@/components/atoms/Field.vue";
import Icon from "@/components/atoms/Icon.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import AddSlot from "@/components/molecules/AddSlot.vue";
import PositionGrid from "@/components/molecules/PositionGrid.vue";
import Segmented, {
    type SegmentedOption,
} from "@/components/molecules/Segmented.vue";
import SwatchGrid from "@/components/molecules/SwatchGrid.vue";
import TriggerRow from "@/components/molecules/TriggerRow.vue";
import BlockLibrary, {
    type LibraryBlock,
} from "@/components/organisms/BlockLibrary.vue";
import EditorShell from "@/components/organisms/EditorShell.vue";
import PopupStage from "@/components/organisms/PopupStage.vue";
import PopupWidget from "@/components/organisms/PopupWidget.vue";
import PwTypeList, { type PwType } from "@/components/organisms/PwTypeList.vue";

type Tone = "neutral" | "good" | "warn" | "bad" | "info" | "accent" | "brown";

interface WidgetContent {
    type: string;
    kicker?: string | null;
    title: string;
    body?: string | null;
    placeholder?: string | null;
    cta?: string | null;
    fine?: string | null;
}

interface EditorWidget {
    id: string;
    name: string;
    meta: string;
    type: string;
    content: WidgetContent;
}

interface TypeOption {
    id: string;
    label: string;
    icon: string;
}

interface Trigger {
    label: string;
    tone?: Tone;
    note?: string | null;
    value?: string | null;
    unit?: string | null;
}

interface AudienceRule {
    label: string;
    checked: boolean;
}

interface EditorPayload {
    widget: EditorWidget;
    device: "desktop" | "mobile";
    types: TypeOption[];
    blocks: LibraryBlock[];
    variables: string[];
    triggers: Trigger[];
    audience: AudienceRule[];
    accentColors: string[];
    viewport: string;
}

const { t } = useI18n();
const route = useRoute();
const locale = computed(() =>
    typeof route.params.locale === "string" ? route.params.locale : "pl",
);
const id = computed(() =>
    typeof route.params.id === "string" ? route.params.id : "new",
);
const backHref = computed(() => `/${locale.value}/popups`);

const payload = ref<EditorPayload | null>(null);

function queryParam(name: string): string | undefined {
    const value = route.query[name];
    return typeof value === "string" ? value : undefined;
}

function fetchQuery(): string {
    const type = queryParam("type");
    const device = queryParam("device");
    const params = new URLSearchParams();
    if (type) params.set("type", type);
    if (device) params.set("device", device);
    const query = params.toString();
    return query ? `?${query}` : "";
}

async function load(): Promise<void> {
    const response = await fetch(
        `/api/v1/${locale.value}/popups/${id.value}${fetchQuery()}`,
        { headers: { Accept: "application/json" } },
    );
    if (!response.ok) {
        return;
    }
    payload.value = (await response.json()) as EditorPayload;
}

onMounted(load);
watch([id, () => route.query.type, () => route.query.device], load);

const typeOptions = computed<PwType[]>(() => {
    if (!payload.value) return [];
    const basePath = route.path;
    const currentType = payload.value.widget.type;
    return payload.value.types.map((type) => ({
        id: type.id,
        label: type.label,
        icon: type.icon,
        href: `${basePath}?type=${type.id}`,
        active: type.id === currentType,
    }));
});

const deviceOptions = computed<SegmentedOption[]>(() => {
    if (!payload.value) return [];
    const basePath = route.path;
    const device = payload.value.device;
    return [
        {
            value: `${basePath}?device=desktop`,
            label: t("popups.editor.desktop"),
            icon: "layout",
            active: device === "desktop",
        },
        {
            value: `${basePath}?device=mobile`,
            label: t("popups.editor.mobile"),
            icon: "user",
            active: device === "mobile",
        },
    ];
});

const radiusOptions = computed<SegmentedOption[]>(() => [
    { value: "sharp", label: t("popups.editor.sharp") },
    { value: "soft", label: t("popups.editor.soft"), active: true },
    { value: "full", label: t("popups.editor.full") },
]);

const variantOptions = computed<SegmentedOption[]>(() => [
    { value: "a", label: t("popups.editor.variantA"), active: true },
    { value: "b", label: t("popups.editor.variantB") },
    { value: "new", label: t("popups.editor.newVariant") },
]);
</script>

<template>
    <div
        v-if="payload"
        class="page"
        style="max-width: none; padding: 16px 16px 0"
    >
        <div class="page-head">
            <div>
                <Button
                    variant="ghost"
                    size="sm"
                    :label="t('popups.editor.backToList')"
                    :href="backHref"
                    style="margin-bottom: 6px"
                />
                <h1 class="page-title">{{ payload.widget.name }}</h1>
                <p class="page-sub" v-html="payload.widget.meta"></p>
            </div>
            <div class="page-actions">
                <Segmented action="navigate" :options="deviceOptions" />
                <Button
                    size="sm"
                    icon="play"
                    :label="t('popups.editor.testOnSite')"
                    action="test-on-site"
                />
                <Button
                    size="sm"
                    :label="t('popups.editor.draft')"
                    action="save-draft"
                />
                <Button
                    variant="primary"
                    size="sm"
                    icon="play"
                    :label="t('popups.editor.publish')"
                    action="publish"
                    :payload="payload.widget.id"
                />
            </div>
        </div>

        <EditorShell
            :left-title="t('popups.editor.widgetType')"
            left-icon="layout"
        >
            <template #left>
                <PwTypeList :types="typeOptions" />

                <div class="section-title" style="margin-top: 20px">
                    {{ t("popups.editor.blocksTitle") }}
                </div>
                <BlockLibrary :blocks="payload.blocks" />

                <div class="section-title" style="margin-top: 20px">
                    {{ t("popups.editor.variablesTitle") }}
                </div>
                <div class="col" style="gap: 6px">
                    <div
                        v-for="(variable, index) in payload.variables"
                        :key="index"
                        class="row"
                        style="
                            background: var(--bg);
                            border: 1px solid var(--line);
                            padding: 6px 8px;
                            border-radius: 6px;
                            font-family: var(--font-mono);
                            font-size: 11.5px;
                            justify-content: space-between;
                            gap: 8px;
                        "
                    >
                        <span>{{ variable }}</span>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon="copy"
                            action="copy-variable"
                            :payload="variable"
                        />
                    </div>
                </div>
            </template>

            <template #canvas>
                <PopupStage
                    :type="payload.widget.type"
                    :device="payload.device"
                >
                    <PopupWidget
                        :type="payload.widget.content.type"
                        :kicker="payload.widget.content.kicker"
                        :title="payload.widget.content.title"
                        :body="payload.widget.content.body"
                        :placeholder="payload.widget.content.placeholder"
                        :cta="payload.widget.content.cta"
                        :fine="payload.widget.content.fine"
                    />
                </PopupStage>
                <div
                    class="row"
                    style="justify-content: center; gap: 8px; margin-top: 14px"
                >
                    <Chip :label="payload.widget.type" mono />
                    <Chip :label="payload.viewport" mono />
                    <Chip :label="t('popups.editor.animation')" />
                </div>
            </template>

            <template #right>
                <div
                    class="muted"
                    style="
                        font-size: 11.5px;
                        text-transform: uppercase;
                        letter-spacing: 0.06em;
                        margin-bottom: 6px;
                    "
                >
                    {{ t("popups.editor.selectedBlock") }}
                </div>
                <div
                    class="card"
                    style="
                        padding: 10px;
                        border-radius: 8px;
                        margin-bottom: 16px;
                    "
                >
                    <div style="font-weight: 500; font-size: 13px">
                        {{ t("popups.editor.selectedBlockName") }}
                    </div>
                    <div class="muted mono" style="font-size: 11.5px">
                        block_id: heading_1
                    </div>
                </div>

                <Field
                    name="widget_title"
                    :label="t('popups.editor.headingLabel')"
                    :value="payload.widget.content.title ?? ''"
                />
                <div class="field-row">
                    <label class="label" for="f-widget_body">{{
                        t("popups.editor.descriptionLabel")
                    }}</label>
                    <textarea
                        class="textarea"
                        id="f-widget_body"
                        name="widget_body"
                        rows="3"
                        placeholder=""
                        :value="payload.widget.content.body ?? ''"
                    ></textarea>
                </div>
                <Field
                    name="widget_cta"
                    :label="t('popups.editor.buttonTextLabel')"
                    :value="payload.widget.content.cta ?? ''"
                />

                <label class="label">{{
                    t("popups.editor.accentColorLabel")
                }}</label>
                <SwatchGrid :colors="payload.accentColors" />

                <label class="label">{{
                    t("popups.editor.positionLabel")
                }}</label>
                <PositionGrid />

                <label class="label" style="margin-top: 12px">{{
                    t("popups.editor.cornersLabel")
                }}</label>
                <Segmented
                    action="set-widget-radius"
                    :options="radiusOptions"
                />

                <div class="section-title" style="margin-top: 18px">
                    <Icon name="bolt" />{{ " "
                    }}{{ t("popups.editor.triggersTitle") }}
                </div>
                <div class="col" style="gap: 8px">
                    <TriggerRow
                        v-for="(trigger, index) in payload.triggers"
                        :key="index"
                        :label="trigger.label"
                        :tone="trigger.tone"
                        :note="trigger.note"
                        :value="trigger.value"
                        :unit="trigger.unit"
                    />
                    <AddSlot
                        :label="t('popups.editor.addTrigger')"
                        action="add-trigger"
                    />
                </div>

                <div class="section-title" style="margin-top: 20px">
                    <Icon name="filter" />{{ " "
                    }}{{ t("popups.editor.audienceTitle") }}
                </div>
                <div class="col" style="gap: 6px">
                    <ToggleRow
                        v-for="(rule, index) in payload.audience"
                        :key="index"
                        name="audience[]"
                        :label="rule.label"
                        :checked="rule.checked"
                        :value="String(index)"
                    />
                </div>

                <div class="section-title" style="margin-top: 20px">
                    <Icon name="target" />{{ " "
                    }}{{ t("popups.editor.abTestTitle") }}
                </div>
                <div class="muted" style="font-size: 12px; margin-bottom: 8px">
                    {{ t("popups.editor.abTestBody") }}
                </div>
                <Segmented action="set-variant" :options="variantOptions" />
            </template>
        </EditorShell>
    </div>
</template>
