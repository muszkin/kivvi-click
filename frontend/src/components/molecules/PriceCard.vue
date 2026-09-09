<script setup lang="ts">
import Button from "@/components/atoms/Button.vue";
import Chip from "@/components/atoms/Chip.vue";
import Icon from "@/components/atoms/Icon.vue";

defineProps<{
    tier: string;
    price: string;
    unit: string;
    items: string[];
    cta: string;
    href: string;
    featured?: boolean;
    badge?: string;
}>();
</script>

<template>
    <div class="price-card" :class="{ pro: featured }">
        <div class="row" style="justify-content: space-between">
            <div class="tier">{{ tier }}</div>
            <Chip v-if="badge" :label="badge" tone="accent" />
        </div>
        <div class="price">
            {{ price }}<span class="unit">{{ unit }}</span>
        </div>
        <ul class="price-list">
            <li v-for="item in items" :key="item">
                <Icon name="check" />{{ " " }}{{ item }}
            </li>
        </ul>
        <Button
            :variant="featured ? 'primary' : 'default'"
            size="lg"
            :label="cta"
            :href="href"
            style="width: 100%; justify-content: center"
        />
    </div>
</template>
