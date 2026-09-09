<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import Sidebar from "@/components/organisms/Sidebar.vue";
import Topbar from "@/components/organisms/Topbar.vue";
import { useShellStore } from "@/stores/shell";

const shell = useShellStore();
const app = ref<HTMLElement | null>(null);

// Canvas-based children (the dashboard cardiogram, later waves) do not reflow on their own
// when the sidebar's grid column animates — dispatch a resize once the transition settles,
// mirroring assets/controllers/shell.ts.
function onTransitionEnd(ev: TransitionEvent): void {
    if (ev.propertyName !== "grid-template-columns") return;
    window.dispatchEvent(new Event("resize"));
}

onMounted(() => app.value?.addEventListener("transitionend", onTransitionEnd));
onUnmounted(() =>
    app.value?.removeEventListener("transitionend", onTransitionEnd),
);
</script>

<template>
    <div
        ref="app"
        class="app"
        :data-sidebar="shell.sidebar"
        data-controller="shell"
    >
        <Sidebar
            :groups="shell.navGroups"
            :current="shell.currentSection"
            :workspace="shell.workspace"
            :user="shell.user"
        />
        <main class="main">
            <Topbar
                :site-name="shell.workspace.name"
                :crumb="shell.crumb"
                :locale="shell.locale"
                :theme="shell.theme"
            />
            <div class="main-scroll scrollable">
                <slot />
            </div>
        </main>
    </div>
</template>
