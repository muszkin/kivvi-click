<script setup lang="ts">
// MOLECULE · Stepper — ported from components/molecules/stepper.html.twig. Wizard progress
// header; the whole item is a jump target. First (and only, so far) owner: import-wizard.
//
// Every step-to-step transition here is a real document navigation, not router.push — the
// oracle's own http.jsonl for a stepper click records a "document" kind GET (steps 6-8), the
// same full reload assets/app.ts's own "set-import-step" intent performed via
// `window.location.href`. This component reproduces that click directly (the shared
// `useIntents` composable is outside this journey's touch scope — see worker-report.md) rather
// than relying on a delegated document-level dispatcher; `data-action`/`data-payload` are still
// rendered for markup parity and because Playwright's own `.step[data-payload="3"]` selector
// depends on them.
import { computed } from "vue";
import Icon from "@/components/atoms/Icon.vue";
import { importStepPath } from "@/components/import/importRoute";

export interface StepperStep {
    n: number;
    label: string;
}

type StepState = "done" | "cur" | "todo";

const props = defineProps<{
    steps: StepperStep[];
    current: number;
    locale: string;
}>();

function stateOf(n: number): StepState {
    if (n < props.current) return "done";
    if (n === props.current) return "cur";
    return "todo";
}

const stepCount = computed(() => props.steps.length);

function go(n: number): void {
    window.location.href = importStepPath(props.locale, n as 1 | 2 | 3 | 4);
}
</script>

<template>
    <ol class="stepper">
        <li
            v-for="s in steps"
            :key="s.n"
            class="step"
            :data-state="stateOf(s.n)"
            data-action="set-import-step"
            :data-payload="s.n"
            @click="go(s.n)"
        >
            <span class="step__num">
                <Icon v-if="stateOf(s.n) === 'done'" name="check" />
                <template v-else>{{ s.n }}</template>
            </span>
            <span class="step__label">
                <span class="step__kicker mono"
                    >KROK {{ s.n }} / {{ stepCount }}</span
                >
                <span class="step__title">{{ s.label }}</span>
            </span>
        </li>
    </ol>
</template>
