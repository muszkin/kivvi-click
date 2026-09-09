<script setup lang="ts">
// ORGANISM · Cardiogram — ported from components/organisms/cardiogram.html.twig +
// assets/controllers/cardiogram.ts. The only <canvas> in the system: a rolling 60-bucket ring
// buffer of "events this second", redrawn every 1s, on a `kivvi:event` document event (see
// useEventStream's dispatch) and on a [data-theme]/[style] mutation of <html> — colours are read
// from computed CSS custom properties, so a theme switch must force a fresh draw. DEV-2 masks the
// canvas pixels themselves (driven by randomness); the element/attributes/legend still must match
// the oracle exactly.
import { onMounted, onUnmounted, ref } from "vue";
import Icon from "@/components/atoms/Icon.vue";

export interface CardiogramLegendItem {
    label: string;
    value: string;
}

export interface CardiogramRange {
    label: string;
    active?: boolean;
}

withDefaults(
    defineProps<{
        id?: string;
        title: string;
        sub?: string | null;
        ranges?: CardiogramRange[] | null;
        legend?: CardiogramLegendItem[] | null;
    }>(),
    { id: "cardiogram", sub: null, ranges: null, legend: null },
);

const BUCKETS = 60;

const canvas = ref<HTMLCanvasElement | null>(null);
const buckets = new Array<number>(BUCKETS).fill(0);
let eventsThisSecond = 0;
let intervalId: number | undefined;
let observer: MutationObserver | undefined;

function draw(): void {
    const el = canvas.value;
    if (!el) return;

    const dpr = window.devicePixelRatio || 1;
    const w = el.clientWidth;
    const h = el.clientHeight;
    el.width = w * dpr;
    el.height = h * dpr;

    const ctx = el.getContext("2d");
    if (!ctx) return;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, w, h);

    const cs = getComputedStyle(document.documentElement);
    const accent = cs.getPropertyValue("--accent").trim();
    const accentSoft = cs.getPropertyValue("--accent-soft").trim();
    const line = cs.getPropertyValue("--line").trim();
    const muted = cs.getPropertyValue("--fg-subtle").trim();

    const pad = 12;
    const max = Math.max(8, ...buckets) * 1.2;
    const xStep = (w - pad * 2) / (BUCKETS - 1);
    const yOf = (v: number): number => h - pad - (v / max) * (h - pad * 2);

    ctx.strokeStyle = line;
    ctx.lineWidth = 1;
    for (let i = 0; i < 4; i++) {
        const y = pad + ((h - pad * 2) / 3) * i;
        ctx.beginPath();
        ctx.moveTo(pad, y);
        ctx.lineTo(w - pad, y);
        ctx.stroke();
    }

    ctx.beginPath();
    ctx.moveTo(pad, h - pad);
    buckets.forEach((v, i) => ctx.lineTo(pad + i * xStep, yOf(v)));
    ctx.lineTo(w - pad, h - pad);
    ctx.closePath();
    ctx.fillStyle = accentSoft;
    ctx.fill();

    ctx.beginPath();
    buckets.forEach((v, i) =>
        i === 0 ? ctx.moveTo(pad, yOf(v)) : ctx.lineTo(pad + i * xStep, yOf(v)),
    );
    ctx.strokeStyle = accent;
    ctx.lineWidth = 2;
    ctx.stroke();

    const last = buckets[BUCKETS - 1] ?? 0;
    ctx.fillStyle = accent;
    ctx.beginPath();
    ctx.arc(pad + (BUCKETS - 1) * xStep, yOf(last), 4, 0, Math.PI * 2);
    ctx.fill();

    ctx.font = '500 11px "Geist Mono", monospace';
    ctx.fillStyle = muted;
    ctx.textAlign = "right";
    ctx.fillText(String(last) + " ev/s", w - pad - 8, pad + 14);
}

function onEvent(): void {
    eventsThisSecond++;
}

function tick(): void {
    buckets.push(eventsThisSecond);
    buckets.shift();
    eventsThisSecond = 0;
    draw();
}

onMounted(() => {
    document.addEventListener("kivvi:event", onEvent);

    intervalId = window.setInterval(tick, 1000);

    observer = new MutationObserver(draw);
    observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ["data-theme", "style"],
    });
    window.addEventListener("resize", draw);
    draw();
});

onUnmounted(() => {
    document.removeEventListener("kivvi:event", onEvent);
    window.clearInterval(intervalId);
    observer?.disconnect();
    window.removeEventListener("resize", draw);
});
</script>

<template>
    <div class="cardiogram-wrap">
        <div class="cardiogram-head">
            <h3 class="section-title" style="margin: 0">
                <Icon name="activity" />{{ " " }}{{ title }}
            </h3>
            <span v-if="sub" class="muted" style="font-size: 12px">{{
                sub
            }}</span>
            <div
                v-if="ranges"
                style="margin-left: auto; display: flex; gap: 6px"
            >
                <button
                    v-for="range in ranges"
                    :key="range.label"
                    class="btn ghost sm"
                    :data-active="range.active ? 'true' : 'false'"
                    data-action="set-range"
                    :data-payload="range.label"
                >
                    {{ range.label }}
                </button>
            </div>
        </div>
        <canvas
            :id="id"
            ref="canvas"
            class="cardiogram-canvas"
            width="900"
            height="120"
            role="img"
            :aria-label="title"
        ></canvas>
        <div v-if="legend" class="cardiogram-meta">
            <span v-for="item in legend" :key="item.label">
                {{ item.label }}{{ " "
                }}<span class="mono" style="color: var(--fg)">{{
                    item.value
                }}</span>
            </span>
        </div>
    </div>
</template>
