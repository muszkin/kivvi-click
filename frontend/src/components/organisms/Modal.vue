<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import Button from "@/components/atoms/Button.vue";

const props = defineProps<{ title: string; size?: number }>();
const emit = defineEmits<{ (e: "close"): void }>();

const panel = ref<HTMLElement | null>(null);
const style = computed(() =>
    props.size ? { maxWidth: `${props.size}px` } : undefined,
);

function close(): void {
    emit("close");
}

function focusables(): HTMLElement[] {
    const el = panel.value;
    if (!el) return [];
    return Array.from(
        el.querySelectorAll<HTMLElement>(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
        ),
    ).filter((node) => !node.hasAttribute("disabled"));
}

function onKeydown(ev: KeyboardEvent): void {
    if (ev.key === "Escape") {
        close();
        return;
    }
    if (ev.key !== "Tab") return;
    const items = focusables();
    const first = items[0];
    const last = items[items.length - 1];
    if (!first || !last) return;
    if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
    } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
    }
}

let previouslyFocused: HTMLElement | null = null;

onMounted(() => {
    previouslyFocused = document.activeElement as HTMLElement | null;
    focusables()[0]?.focus();
    document.addEventListener("keydown", onKeydown);
});

onUnmounted(() => {
    document.removeEventListener("keydown", onKeydown);
    previouslyFocused?.focus();
});
</script>

<template>
    <div
        class="scrim"
        data-action="close-modal"
        data-controller="modal"
        @click="close"
    >
        <div
            ref="panel"
            class="modal"
            role="dialog"
            aria-modal="true"
            :aria-label="title"
            :style="style"
            @click.stop
        >
            <div class="modal-head">
                <h3 class="card-title">{{ title }}</h3>
                <div style="margin-left: auto">
                    <Button
                        variant="ghost"
                        size="sm"
                        icon="x"
                        aria-label="Zamknij"
                        @click="close"
                    />
                </div>
            </div>
            <div class="modal-body">
                <slot name="body" />
            </div>
            <div v-if="$slots.foot" class="modal-foot">
                <slot name="foot" />
            </div>
        </div>
    </div>
</template>
