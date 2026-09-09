<script setup lang="ts">
import Avatar from "@/components/atoms/Avatar.vue";
import Wordmark from "@/components/atoms/Wordmark.vue";
import NavItem from "@/components/molecules/NavItem.vue";
import WorkspaceCard from "@/components/molecules/WorkspaceCard.vue";
import type { NavGroup } from "@/stores/shell";

defineProps<{
    groups: NavGroup[];
    current: string;
    workspace: { name: string; meta: string; mark: string };
    user: { name: string; email: string };
}>();
</script>

<template>
    <aside class="sidebar">
        <div class="brand-row"><Wordmark /></div>

        <WorkspaceCard
            :name="workspace.name"
            :meta="workspace.meta"
            :mark="workspace.mark"
        />

        <nav class="nav scrollable" aria-label="Nawigacja główna">
            <div v-for="group in groups" :key="group.label" class="nav-group">
                <div class="nav-section-label">{{ group.label }}</div>
                <NavItem
                    v-for="item in group.items"
                    :key="item.route"
                    :label="item.label"
                    :icon="item.icon"
                    :href="item.href"
                    :route="item.route"
                    :active="item.route === current"
                    :badge="item.badge"
                />
            </div>
        </nav>

        <div class="sb-foot">
            <Avatar :name="user.name" />
            <div style="min-width: 0; flex: 1">
                <div
                    style="
                        font-size: 12.5px;
                        font-weight: 500;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    "
                >
                    {{ user.name }}
                </div>
                <div style="font-size: 11px; color: var(--fg-muted)">
                    {{ user.email }}
                </div>
            </div>
        </div>
    </aside>
</template>
