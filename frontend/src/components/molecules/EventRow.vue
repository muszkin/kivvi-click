<script setup lang="ts">
// MOLECULE · EventRow — ported from components/molecules/event-row.html.twig. 6-column grid row:
// time · type icon · type · detail · customer · site. `isNew` adds the 800ms flash-in class a row
// pushed by Mercure carries.
import Avatar from "@/components/atoms/Avatar.vue";
import Dot from "@/components/atoms/Dot.vue";
import Icon from "@/components/atoms/Icon.vue";

withDefaults(
    defineProps<{
        time: string;
        typeIcon: string;
        tone?: string;
        type: string;
        detail?: string | null;
        customerName?: string | null;
        customerId?: string | null;
        siteName?: string | null;
        siteColor?: string | null;
        isNew?: boolean;
    }>(),
    {
        tone: "",
        detail: null,
        customerName: null,
        customerId: null,
        siteName: null,
        siteColor: null,
        isNew: false,
    },
);
</script>

<template>
    <div
        class="event-row"
        :class="{ new: isNew }"
        :data-action="customerId ? 'go-customer' : undefined"
        :data-payload="customerId ? customerId : undefined"
    >
        <div class="event-row__time mono">{{ time }}</div>
        <div class="event-row__icon" :class="tone">
            <Icon :name="typeIcon" />
        </div>
        <div class="event-row__type">{{ type }}</div>
        <div class="event-row__detail muted">{{ detail ?? "" }}</div>
        <div class="event-row__customer">
            <template v-if="customerName">
                <Avatar :name="customerName" :size="22" />{{ " "
                }}<span>{{ customerName }}</span>
            </template>
        </div>
        <div class="event-row__site">
            <template v-if="siteName">
                <Dot :color="siteColor ?? 'var(--fg-muted)'" />{{ " "
                }}<span class="muted mono" style="font-size: 11px">{{
                    siteName
                }}</span>
            </template>
        </div>
    </div>
</template>
